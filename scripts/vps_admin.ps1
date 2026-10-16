# SonyVegas administration helper for the standalone VPS backend.
# Keep SONYVEGAS_ADMIN_TOKEN in the current PowerShell process only.

[CmdletBinding()]
param(
    [Parameter(Position = 0, Mandatory = $true)]
    [ValidateSet("dashboard", "status", "generate-license", "create-license", "list-licenses", "revoke-license", "upload-dll", "upload-aux", "list-users", "list-admins", "create-admin", "revoke-admin", "copy-admin-token", "cleanup", "export")]
    [string]$Action,

    [Parameter(Position = 1)]
    [string]$Argument,

    [Parameter(Position = 2)]
    [ValidateSet("user", "developer")]
    [string]$Role = "user"
)

$ErrorActionPreference = "Stop"
$Base = if ([string]::IsNullOrWhiteSpace($env:SONYVEGAS_API_BASE)) {
    "https://54.94.145.87"
} else {
    $env:SONYVEGAS_API_BASE.TrimEnd("/")
}
$Token = $env:SONYVEGAS_ADMIN_TOKEN
$ChunkSize = 5MB
$MaximumPayloadSize = 40MB
$CredentialFile = Join-Path ([Environment]::GetFolderPath("UserProfile")) ".sonyvegas\admin-token.clixml"

function Get-PlainTextCredential {
    param([Parameter(Mandatory = $true)][string]$Path)
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw "Encrypted credential file not found: $Path"
    }
    $secureToken = Import-Clixml -LiteralPath $Path
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureToken)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
}

if ([string]::IsNullOrWhiteSpace($Token) -and (Test-Path -LiteralPath $CredentialFile -PathType Leaf)) {
    $Token = Get-PlainTextCredential $CredentialFile
}

if ([string]::IsNullOrWhiteSpace($Token)) {
    throw "SONYVEGAS_ADMIN_TOKEN is not set and the encrypted local credential file is missing."
}

$Headers = @{}
if ($Action -ne "copy-admin-token") {
    $totpCode = if ($env:SONYVEGAS_ADMIN_TOTP) { $env:SONYVEGAS_ADMIN_TOTP } else { Read-Host "Google Authenticator ou codigo de recuperacao" }
    if ($totpCode -notmatch '^([0-9]{6}|[A-Za-z2-7]{4}-?[A-Za-z2-7]{4}-?[A-Za-z2-7]{5})$') {
        throw "Informe o codigo de 6 digitos ou um codigo de recuperacao."
    }
    $loginBody = @{ adminToken = $Token; totp = $totpCode } | ConvertTo-Json -Compress
    $login = Invoke-RestMethod -Uri "$Base/v1/admin/login" -Method POST -Headers @{ "X-SonyVegas-CLI" = "1" } -ContentType "application/json" -Body $loginBody
    Remove-Variable totpCode,loginBody -ErrorAction SilentlyContinue
    if ($login.mfaSetupRequired) { throw "Abra $Base/admin com a chave e conclua o cadastro MFA antes de usar o helper." }
    if (-not $login.sessionToken) { throw "Codigo MFA invalido ou acesso indisponivel." }
    $Headers = @{ Authorization = "Bearer $($login.sessionToken)" }
}

function Get-LicenseId {
    param([Parameter(Mandatory = $true)][string]$LicenseKey)
    $sha = [System.Security.Cryptography.SHA256]::Create()
    try {
        $hash = $sha.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($LicenseKey))
        return "lc_" + (($hash | ForEach-Object { $_.ToString("x2") }) -join "").Substring(0, 32)
    } finally {
        $sha.Dispose()
    }
}

function Invoke-ControlApi {
    param([Parameter(Mandatory = $true)][string]$Method, [Parameter(Mandatory = $true)][string]$Path, $Body)
    $parameters = @{ Uri = "$Base$Path"; Method = $Method; Headers = $Headers; ErrorAction = "Stop" }
    if ($null -ne $Body) {
        $parameters.ContentType = "application/json"
        $parameters.Body = $Body | ConvertTo-Json -Depth 20 -Compress
    }
    Invoke-RestMethod @parameters
}

switch ($Action) {
    "dashboard" {
        $access = Invoke-ControlApi POST "/v1/admin/browser-tickets" $null
        $ticket = [Uri]::EscapeDataString([string]$access.ticket)
        Start-Process -FilePath "$Base/admin#ticket=$ticket"
        Write-Host "Dashboard opened with a one-time sign-in." -ForegroundColor Green
    }
    "status" {
        Invoke-ControlApi GET "/v1/admin/status" $null | ConvertTo-Json -Depth 10
    }
    "generate-license" {
        $created = Invoke-ControlApi POST "/v1/admin/licenses" @{ role = $Role }
        Write-Host "License created. Copy it now; it is not stored in plaintext:" -ForegroundColor Green
        Write-Host $created.licenseKey -ForegroundColor Yellow
        Write-Host "Role: $($created.role)  ID: $($created.id)"
    }
    "create-license" {
        if ([string]::IsNullOrWhiteSpace($Argument) -or $Argument.Length -lt 20) {
            throw "Provide a random license key with at least 20 characters."
        }
        Invoke-ControlApi POST "/v1/admin/licenses" @{ role = $Role; licenseKey = $Argument } | ConvertTo-Json
    }
    "list-licenses" {
        (Invoke-ControlApi GET "/v1/admin/licenses" $null).licenses | Format-Table id,status,role,bound_device_id,expires_at
    }
    "revoke-license" {
        if ([string]::IsNullOrWhiteSpace($Argument)) { throw "Provide the original license key." }
        $id = Get-LicenseId $Argument
        Invoke-ControlApi POST "/v1/admin/licenses/$id/revoke" @{} | ConvertTo-Json
    }
    "upload-dll" {
        $payloadTrack = if ($Role -eq "user") { "release" } else { "developer" }
        $resolved = (Resolve-Path -LiteralPath $Argument).Path
        $file = Get-Item -LiteralPath $resolved
        if ($file.Extension -ine ".dll" -or $file.Length -lt 1KB -or $file.Length -gt $MaximumPayloadSize) {
            throw "Payload must be a DLL between 1 KiB and 40 MiB."
        }
        $stream = [System.IO.File]::OpenRead($resolved)
        try {
            if ($stream.ReadByte() -ne 0x4D -or $stream.ReadByte() -ne 0x5A) { throw "Payload is not a PE/MZ file." }
        } finally {
            $stream.Dispose()
        }
        $stream = [System.IO.File]::OpenRead($resolved)
        try {
            $offset = 0L
            $response = $null
            while ($offset -lt $file.Length) {
                $length = [int][Math]::Min($ChunkSize, $file.Length - $offset)
                $chunk = [byte[]]::new($length)
                $read = 0
                while ($read -lt $length) {
                    $count = $stream.Read($chunk, $read, $length - $read)
                    if ($count -le 0) { throw "Unexpected end of payload." }
                    $read += $count
                }
                $end = $offset + $length - 1
                $chunkHeaders = @{
                    Authorization = $Headers.Authorization
                    "Content-Range" = "bytes $offset-$end/$($file.Length)"
                }
                $response = Invoke-RestMethod -Uri "$Base/v1/admin/payloads/$payloadTrack" -Method PUT `
                    -Headers $chunkHeaders -ContentType "application/octet-stream" -Body $chunk
                $percent = [Math]::Floor((($end + 1) / $file.Length) * 100)
                Write-Host "Uploaded $percent%" -ForegroundColor Cyan
                $offset += $length
            }
            $response | ConvertTo-Json
        } finally {
            $stream.Dispose()
        }
    }
    "upload-aux" {
        $auxTrack = if ($Role -eq "user") { "release" } else { "developer" }
        $resolved = (Resolve-Path -LiteralPath $Argument).Path
        $file = Get-Item -LiteralPath $resolved
        if ($file.Extension -ine ".exe" -or $file.Length -lt 1KB -or $file.Length -gt 32MB) {
            throw "Auxiliary app must be an EXE between 1 KiB and 32 MiB."
        }
        $stream = [System.IO.File]::OpenRead($resolved)
        try {
            if ($stream.ReadByte() -ne 0x4D -or $stream.ReadByte() -ne 0x5A) { throw "Auxiliary app is not a PE/MZ file." }
        } finally {
            $stream.Dispose()
        }
        $stream = [System.IO.File]::OpenRead($resolved)
        try {
            $offset = 0L
            $response = $null
            while ($offset -lt $file.Length) {
                $length = [int][Math]::Min($ChunkSize, $file.Length - $offset)
                $chunk = [byte[]]::new($length)
                $read = 0
                while ($read -lt $length) {
                    $count = $stream.Read($chunk, $read, $length - $read)
                    if ($count -le 0) { throw "Unexpected end of auxiliary app." }
                    $read += $count
                }
                $end = $offset + $length - 1
                $chunkHeaders = @{
                    Authorization = $Headers.Authorization
                    "Content-Range" = "bytes $offset-$end/$($file.Length)"
                    "X-File-Name" = $file.Name
                }
                $response = Invoke-RestMethod -Uri "$Base/v1/admin/auxiliary/$auxTrack" -Method PUT `
                    -Headers $chunkHeaders -ContentType "application/octet-stream" -Body $chunk
                $percent = [Math]::Floor((($end + 1) / $file.Length) * 100)
                Write-Host "Uploaded $percent%" -ForegroundColor Cyan
                $offset += $length
            }
            $response | ConvertTo-Json
        } finally {
            $stream.Dispose()
        }
    }
    "list-users" {
        (Invoke-ControlApi GET "/v1/admin/devices" $null).devices | Format-Table username,id,status,role,last_seen_at
    }
    "list-admins" {
        (Invoke-ControlApi GET "/v1/admin/admins" $null).admins | Format-Table name,id,status,last_login_at,created_at
    }
    "create-admin" {
        if ([string]::IsNullOrWhiteSpace($Argument) -or $Argument.Trim().Length -gt 32) {
            throw "Provide the administrator display name (1-32 characters)."
        }
        $created = Invoke-ControlApi POST "/v1/admin/admins" @{ name = $Argument.Trim() }
        $normalized = $Argument.Normalize([Text.NormalizationForm]::FormD)
        $profile = -join ($normalized.ToCharArray() | Where-Object {
            [Globalization.CharUnicodeInfo]::GetUnicodeCategory($_) -ne [Globalization.UnicodeCategory]::NonSpacingMark
        })
        $profile = ($profile.ToLowerInvariant() -replace '[^a-z0-9]+', '-').Trim('-')
        if ([string]::IsNullOrWhiteSpace($profile)) { $profile = $created.admin.id.Substring(3, 8) }
        $credentialDirectory = Split-Path -Parent $CredentialFile
        New-Item -ItemType Directory -Force -Path $credentialDirectory | Out-Null
        $savedPath = Join-Path $credentialDirectory "admin-token-$profile.clixml"
        ConvertTo-SecureString ([string]$created.adminToken) -AsPlainText -Force | Export-Clixml -LiteralPath $savedPath
        Set-Clipboard -Value ([string]$created.adminToken)
        Write-Host "Administrator '$($created.admin.name)' created. Token copied to the clipboard and encrypted at $savedPath" -ForegroundColor Green
    }
    "revoke-admin" {
        if ($Argument -notmatch '^ad_[0-9a-f]{32}$') { throw "Provide the administrator ID from list-admins." }
        Invoke-ControlApi POST "/v1/admin/admins/$Argument/revoke" @{} | ConvertTo-Json
    }
    "copy-admin-token" {
        $profile = if ([string]::IsNullOrWhiteSpace($Argument) -or $Argument -eq "owner") { "owner" } else { $Argument }
        if ($profile -notmatch '^[a-z0-9][a-z0-9-]{0,31}$') { throw "Use profile 'owner' or the saved profile name." }
        $source = if ($profile -eq "owner") {
            $CredentialFile
        } else {
            Join-Path (Split-Path -Parent $CredentialFile) "admin-token-$profile.clixml"
        }
        Set-Clipboard -Value (Get-PlainTextCredential $source)
        Write-Host "Administrative token for profile '$profile' copied to the clipboard." -ForegroundColor Green
    }
    "cleanup" {
        Invoke-ControlApi POST "/v1/admin/cleanup" @{} | ConvertTo-Json -Depth 10
    }
    "export" {
        if ([string]::IsNullOrWhiteSpace($Argument)) { throw "Provide the backup JSON path." }
        $backup = Invoke-ControlApi GET "/v1/admin/export" $null | ConvertTo-Json -Depth 30
        [System.IO.File]::WriteAllText((Join-Path (Get-Location) $Argument), $backup, [System.Text.UTF8Encoding]::new($false))
        Write-Host "Backup written to $Argument" -ForegroundColor Green
    }
}

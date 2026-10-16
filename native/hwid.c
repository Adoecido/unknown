#ifndef WIN32_LEAN_AND_MEAN
#define WIN32_LEAN_AND_MEAN
#endif
#include <windows.h>
#include <wincrypt.h>
#include <dpapi.h>
#include <stdio.h>
#include <string.h>

#include "hwid.h"
#include "obf.h"

#pragma comment(lib, "advapi32.lib")

int sonyvegas_generate_hwid(char *output, size_t capacity) {
    HKEY key = NULL;
    char machine_guid[128] = {0};
    DWORD guid_size = sizeof(machine_guid);
    DWORD volume_serial = 0;
    char computer_name[MAX_COMPUTERNAME_LENGTH + 1] = {0};
    DWORD comp_size = sizeof(computer_name);
    char combined[512];
    HCRYPTPROV prov = 0;
    HCRYPTHASH hash = 0;
    BYTE hash_bytes[32];
    DWORD hash_size = 32;
    int i;

    if (capacity < 36) return 0;

    if (RegOpenKeyExA(HKEY_LOCAL_MACHINE,
            SV_STR(SV_DC379A5B43B341B7, "SOFTWARE\\Microsoft\\Cryptography"),
            0, KEY_READ | KEY_WOW64_64KEY, &key) == ERROR_SUCCESS) {
        RegQueryValueExA(key, SV_STR(SV_1B9CEC7A4B6B0114, "MachineGuid"),
                NULL, NULL, (LPBYTE)machine_guid, &guid_size);
        RegCloseKey(key);
    }

    GetVolumeInformationA(SV_STR(SV_2E8956133D4B5BDB, "C:\\"), NULL, 0, &volume_serial,
            NULL, NULL, NULL, 0);
    GetComputerNameA(computer_name, &comp_size);

    _snprintf(combined, sizeof(combined), SV_STR(SV_A179B2287125C284, "%s|%lu|%s"),
            machine_guid, (unsigned long)volume_serial, computer_name);

    if (!CryptAcquireContextA(&prov, NULL, NULL, PROV_RSA_AES,
            CRYPT_VERIFYCONTEXT)) {
        return 0;
    }
    if (!CryptCreateHash(prov, CALG_SHA_256, 0, 0, &hash)) {
        CryptReleaseContext(prov, 0);
        return 0;
    }
    CryptHashData(hash, (const BYTE *)combined, (DWORD)strlen(combined), 0);
    CryptGetHashParam(hash, HP_HASHVAL, hash_bytes, &hash_size, 0);
    CryptDestroyHash(hash);
    CryptReleaseContext(prov, 0);

    output[0] = 'h';
    output[1] = 'w';
    output[2] = '_';
    for (i = 0; i < 16; i++) {
        _snprintf(output + 3 + i * 2, 3, SV_STR(SV_033A986E4D3C6DB9, "%02x"), hash_bytes[i]);
    }
    output[35] = '\0';
    return 1;
}

int sonyvegas_load_device_secret(char *output, size_t capacity) {
    HKEY key = NULL;
    BYTE encrypted[1024];
    DWORD encrypted_size = sizeof(encrypted);
    DWORD type = 0;
    DATA_BLOB input;
    DATA_BLOB plain;
    int result = 0;

    if (output == NULL || capacity == 0) return 0;
    output[0] = '\0';
    if (RegOpenKeyExA(HKEY_CURRENT_USER,
            SV_STR(SV_2510706B34C60C3E, "Software\\Crimson\\SonyVegas"), 0, KEY_READ, &key)
            != ERROR_SUCCESS) return 0;
    if (RegQueryValueExA(key, SV_STR(SV_11B596BF99113A7F, "DeviceSecret"), NULL, &type, encrypted,
            &encrypted_size) != ERROR_SUCCESS || type != REG_BINARY) {
        RegCloseKey(key);
        return 0;
    }
    RegCloseKey(key);

    input.pbData = encrypted;
    input.cbData = encrypted_size;
    memset(&plain, 0, sizeof(plain));
    if (CryptUnprotectData(&input, NULL, NULL, NULL, NULL,
            CRYPTPROTECT_UI_FORBIDDEN, &plain)) {
        if (plain.cbData > 1 && plain.cbData <= capacity
                && plain.pbData[plain.cbData - 1] == '\0') {
            memcpy(output, plain.pbData, plain.cbData);
            result = 1;
        }
        SecureZeroMemory(plain.pbData, plain.cbData);
        LocalFree(plain.pbData);
    }
    SecureZeroMemory(encrypted, sizeof(encrypted));
    return result;
}

int sonyvegas_store_device_secret(const char *secret) {
    HKEY key = NULL;
    DWORD disposition;
    DATA_BLOB input;
    DATA_BLOB encrypted;
    size_t length;
    LONG status;
    int result = 0;

    if (secret == NULL || secret[0] == '\0') return 0;
    length = strlen(secret) + 1;
    if (length > 256) return 0;
    input.pbData = (BYTE *)secret;
    input.cbData = (DWORD)length;
    memset(&encrypted, 0, sizeof(encrypted));
    if (!CryptProtectData(&input, SV_WSTR(SV_7F5657F4897978CF, L"Crimson SonyVegas device secret"),
            NULL, NULL, NULL, CRYPTPROTECT_UI_FORBIDDEN, &encrypted)) {
        return 0;
    }
    status = RegCreateKeyExA(HKEY_CURRENT_USER,
            SV_STR(SV_2510706B34C60C3E, "Software\\Crimson\\SonyVegas"), 0, NULL, 0,
            KEY_SET_VALUE, NULL, &key, &disposition);
    if (status == ERROR_SUCCESS) {
        status = RegSetValueExA(key, SV_STR(SV_11B596BF99113A7F, "DeviceSecret"), 0, REG_BINARY,
                encrypted.pbData, encrypted.cbData);
        RegCloseKey(key);
        result = status == ERROR_SUCCESS;
    }
    SecureZeroMemory(encrypted.pbData, encrypted.cbData);
    LocalFree(encrypted.pbData);
    return result;
}

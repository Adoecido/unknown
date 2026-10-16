#define IMGUI_DEFINE_MATH_OPERATORS
#ifndef WIN32_LEAN_AND_MEAN
#define WIN32_LEAN_AND_MEAN
#endif
#include <windows.h>
#include <wincrypt.h>
#include <tlhelp32.h>
#include <dwmapi.h>
#include <d3d11.h>
#include <dxgi.h>
#include <shellapi.h>

#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <ctime>
#include <cmath>
#include <atomic>
#include <thread>

#include "imgui/imgui.h"
#include "imgui/imgui_impl_dx11.h"
#include "imgui/imgui_impl_win32.h"
#include "imgui/imgui_custom.h"
#include "imgui/imspinner.h"
#include "imgui/RawFonts.hpp"
#include "imgui/FontAwesome.hpp"
#include "assets/crimson_logo_bytes.h"

extern "C" {
#include "obf.h"
#include "hwid.h"
#include "control_api_client.h"
#include "remote_session.h"
#include "manual_map.h"
#include "cleanup.h"
#include "anti_debug.h"
}

#pragma comment(lib, "d3d11.lib")
#pragma comment(lib, "dxgi.lib")
#pragma comment(lib, "dwmapi.lib")
#pragma comment(lib, "winhttp.lib")
#pragma comment(lib, "advapi32.lib")
#pragma comment(lib, "crypt32.lib")
#pragma comment(lib, "user32.lib")
#pragma comment(lib, "shell32.lib")

/* ── forward declarations ────────────────────────────────────────── */
extern IMGUI_IMPL_API LRESULT ImGui_ImplWin32_WndProcHandler(
        HWND hWnd, UINT msg, WPARAM wParam, LPARAM lParam);
static LRESULT WINAPI WndProc(HWND hWnd, UINT msg, WPARAM wParam,
        LPARAM lParam);

/* ── DX11 globals ────────────────────────────────────────────────── */
static ID3D11Device            *g_device            = nullptr;
static ID3D11DeviceContext     *g_device_context     = nullptr;
static IDXGISwapChain          *g_swap_chain         = nullptr;
static ID3D11RenderTargetView  *g_render_target_view = nullptr;
static ID3D11ShaderResourceView *g_logo_texture       = nullptr;
static HWND                     g_hwnd               = nullptr;
static bool                     g_stream_mode        = true;

#ifndef WDA_EXCLUDEFROMCAPTURE
#define WDA_EXCLUDEFROMCAPTURE 0x00000011
#endif

static const int WIN_W = 640;
static const int WIN_H = 360;

static bool ApplyStreamMode(bool enabled) {
    if (!g_hwnd) return false;
    DWORD affinity = enabled ? WDA_EXCLUDEFROMCAPTURE : WDA_NONE;
    if (SetWindowDisplayAffinity(g_hwnd, affinity)) return true;
    control_api_debug_log(L"SetWindowDisplayAffinity failed mode=%lu error=%lu",
            (unsigned long)affinity, (unsigned long)GetLastError());
    return false;
}

/* ── ImGui fonts ─────────────────────────────────────────────────── */
static ImFont *g_font_regular    = nullptr;
static ImFont *g_font_black      = nullptr;
static ImFont *g_font_bold       = nullptr;
static ImFont *g_font_extra_bold = nullptr;
static ImFont *g_font_extra_light = nullptr;
static ImFont *g_font_light      = nullptr;
static ImFont *g_font_medium     = nullptr;
static ImFont *g_font_semi_bold  = nullptr;
static ImFont *g_font_thin       = nullptr;
static ImFont *g_font_awesome    = nullptr;

/* ── application state ───────────────────────────────────────────── */
enum UIState {
    UI_INIT,
    UI_LOGIN,
    UI_AUTH_WORKING,
    UI_DOWNLOAD,
    UI_PROCESS_SELECT,
    UI_INJECTING,
    UI_SUCCESS,
    UI_ERROR,
    UI_SELF_DESTRUCT,
    UI_CLOSING
};

static std::atomic<UIState> g_state{UI_INIT};
static std::atomic<int>     g_progress{0};
static char                 g_status[256]      = "";
static char                 g_detail[256]      = "";
static char                 g_error_msg[512]   = "";
static char                 g_username[64]     = "";
static char                 g_license_key[160] = "";
static std::atomic<bool>     g_require_license{true};
static HANDLE               g_auth_event       = nullptr;
static HANDLE               g_select_event     = nullptr;

static unsigned char *g_dll_data  = nullptr;
static size_t         g_dll_size  = 0;
static DWORD          g_target_pid = 0;
static char           g_target_client[32] = "Minecraft";
static int            g_remote_mode = 1;
static int            g_injection_result = 0;

/* ── process list ────────────────────────────────────────────────── */
#define MAX_CANDIDATES 256
#define WINDOW_TITLE_CAPACITY 256

struct process_candidate {
    DWORD    process_id;
    wchar_t  executable[MAX_PATH];
    wchar_t  title[WINDOW_TITLE_CAPACITY];
    char     display[320];
    char     client_name[32];
};

static process_candidate g_candidates[MAX_CANDIDATES];
static int               g_candidate_count = 0;
static int               g_selected_candidate = 0;

/* ── helper: wide → utf8 ────────────────────────────────────────── */
static void wchar_to_utf8(char *dst, size_t cap, const wchar_t *src) {
    if (!dst || cap == 0) return;
    if (!src) { dst[0] = '\0'; return; }
    WideCharToMultiByte(CP_UTF8, 0, src, -1, dst, (int)cap, nullptr, nullptr);
    dst[cap - 1] = '\0';
}

/* ═══════════════════════════════════════════════════════════════════
 *  BUSINESS LOGIC — ported from injector.c (raw strings, no SV_WSTR)
 * ═══════════════════════════════════════════════════════════════════ */

static int is_java_process(const wchar_t *executable) {
    return _wcsicmp(executable, L"java.exe") == 0
            || _wcsicmp(executable, L"javaw.exe") == 0;
}

struct window_search_context {
    process_candidate *candidates;
    size_t count;
};

static BOOL CALLBACK capture_window_title(HWND window, LPARAM parameter) {
    auto *ctx = (window_search_context *)parameter;
    wchar_t title[WINDOW_TITLE_CAPACITY];
    DWORD pid = 0;
    if (!IsWindowVisible(window) || GetWindowTextLengthW(window) == 0)
        return TRUE;
    GetWindowThreadProcessId(window, &pid);
    if (pid == 0 || GetWindowTextW(window, title, WINDOW_TITLE_CAPACITY) == 0)
        return TRUE;
    for (size_t i = 0; i < ctx->count; ++i) {
        if (ctx->candidates[i].process_id == pid
                && ctx->candidates[i].title[0] == L'\0') {
            wcscpy(ctx->candidates[i].title, title);
            break;
        }
    }
    return TRUE;
}

static int compare_candidates(const void *a, const void *b) {
    auto *ca = (const process_candidate *)a;
    auto *cb = (const process_candidate *)b;
    if (ca->process_id < cb->process_id) return -1;
    if (ca->process_id > cb->process_id) return 1;
    return 0;
}

static bool wide_contains_ci(const wchar_t *value, const wchar_t *needle) {
    if (!value || !needle || !needle[0]) return false;
    size_t needle_length = wcslen(needle);
    for (const wchar_t *cursor = value; *cursor; ++cursor) {
        if (_wcsnicmp(cursor, needle, needle_length) == 0) return true;
    }
    return false;
}

struct process_unicode_string {
    USHORT Length;
    USHORT MaximumLength;
    PWSTR Buffer;
};

static bool read_process_command_line(DWORD process_id,
        wchar_t *output, size_t capacity) {
    typedef LONG (WINAPI *nt_query_information_process_fn)(
            HANDLE, ULONG, PVOID, ULONG, PULONG);
    if (!output || capacity == 0) return false;
    output[0] = L'\0';
    HMODULE ntdll = GetModuleHandleW(L"ntdll.dll");
    auto query = ntdll ? (nt_query_information_process_fn)GetProcAddress(
            ntdll, "NtQueryInformationProcess") : nullptr;
    if (!query) return false;
    HANDLE process = OpenProcess(PROCESS_QUERY_LIMITED_INFORMATION,
            FALSE, process_id);
    if (!process) return false;
    ULONG required = 0;
    query(process, 60 /* ProcessCommandLineInformation */,
            nullptr, 0, &required);
    if (required < sizeof(process_unicode_string)
            || required > 1024 * 1024) {
        CloseHandle(process);
        return false;
    }
    auto *buffer = (unsigned char *)malloc(required + sizeof(wchar_t));
    if (!buffer) {
        CloseHandle(process);
        return false;
    }
    LONG status = query(process, 60, buffer, required, &required);
    if (status >= 0) {
        auto *command_line = (process_unicode_string *)buffer;
        size_t characters = command_line->Length / sizeof(wchar_t);
        uintptr_t data_begin = (uintptr_t)buffer;
        uintptr_t data_end = data_begin + required;
        uintptr_t text_begin = (uintptr_t)command_line->Buffer;
        uintptr_t text_end = text_begin + command_line->Length;
        if (command_line->Buffer && text_begin >= data_begin
                && text_end >= text_begin && text_end <= data_end) {
            if (characters >= capacity) characters = capacity - 1;
            memcpy(output, command_line->Buffer,
                    characters * sizeof(wchar_t));
            output[characters] = L'\0';
        } else {
            status = -1;
        }
    }
    free(buffer);
    CloseHandle(process);
    return status >= 0;
}

static const char *detect_client_name(process_candidate *candidate) {
    bool cheatbreaker = wide_contains_ci(candidate->title, L"cheatbreaker");
    bool badlion = wide_contains_ci(candidate->title, L"badlion");
    bool lunar = wide_contains_ci(candidate->title, L"lunar");
    HANDLE snap = CreateToolhelp32Snapshot(
            TH32CS_SNAPMODULE | TH32CS_SNAPMODULE32, candidate->process_id);
    if (snap != INVALID_HANDLE_VALUE) {
        MODULEENTRY32W module{};
        module.dwSize = sizeof(module);
        if (Module32FirstW(snap, &module)) {
            do {
                cheatbreaker = cheatbreaker
                        || wide_contains_ci(module.szExePath, L"cheatbreaker");
                badlion = badlion
                        || wide_contains_ci(module.szExePath, L"badlion");
                lunar = lunar
                        || wide_contains_ci(module.szExePath, L"lunarclient")
                        || wide_contains_ci(module.szExePath, L"lunar client");
            } while (!cheatbreaker && !badlion && !lunar
                    && Module32NextW(snap, &module));
        }
        CloseHandle(snap);
    }
    wchar_t command_line[8192];
    if (!cheatbreaker && !badlion && !lunar
            && read_process_command_line(candidate->process_id,
                    command_line, _countof(command_line))) {
        cheatbreaker = wide_contains_ci(command_line, L"cheatbreaker")
                || wide_contains_ci(command_line, L"1.8.9.patch");
        badlion = wide_contains_ci(command_line, L"badlion");
        lunar = wide_contains_ci(command_line, L"lunarclient")
                || wide_contains_ci(command_line, L"lunar client");
        SecureZeroMemory(command_line, sizeof(command_line));
    }
    if (cheatbreaker) return "CheatBreaker";
    if (badlion) return "Badlion";
    if (lunar) return "Lunar";
    return "Minecraft";
}

static int enumerate_candidates_list(process_candidate *out, int capacity) {
    HANDLE snap = CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0);
    if (snap == INVALID_HANDLE_VALUE) return 0;
    PROCESSENTRY32W entry{};
    entry.dwSize = sizeof(entry);
    int count = 0;
    if (Process32FirstW(snap, &entry)) {
        do {
            if (count < capacity && is_java_process(entry.szExeFile)) {
                auto *c = &out[count++];
                memset(c, 0, sizeof(*c));
                c->process_id = entry.th32ProcessID;
                wcsncpy(c->executable, entry.szExeFile, MAX_PATH - 1);
            }
        } while (Process32NextW(snap, &entry));
    }
    CloseHandle(snap);

    window_search_context ctx{out, (size_t)count};
    EnumWindows(capture_window_title, (LPARAM)&ctx);

    int write = 0;
    for (int i = 0; i < count; ++i) {
        if (out[i].title[0] != L'\0') {
            if (write != i) out[write] = out[i];
            ++write;
        }
    }
    qsort(out, write, sizeof(*out), compare_candidates);

    for (int i = 0; i < write; ++i) {
        const char *client_name = detect_client_name(&out[i]);
        strncpy_s(out[i].client_name, client_name, _TRUNCATE);
        _snprintf_s(out[i].display, sizeof(out[i].display), _TRUNCATE,
                "%s - (%lu)", client_name,
                (unsigned long)out[i].process_id);
    }
    return write;
}

static uintptr_t remote_module_base(DWORD pid, const wchar_t *name) {
    HANDLE snap = CreateToolhelp32Snapshot(
            TH32CS_SNAPMODULE | TH32CS_SNAPMODULE32, pid);
    if (snap == INVALID_HANDLE_VALUE) return 0;
    MODULEENTRY32W entry{};
    entry.dwSize = sizeof(entry);
    uintptr_t result = 0;
    if (Module32FirstW(snap, &entry)) {
        do {
            if (_wcsicmp(entry.szModule, name) == 0) {
                result = (uintptr_t)entry.modBaseAddr;
                break;
            }
        } while (Module32NextW(snap, &entry));
    }
    CloseHandle(snap);
    return result;
}

static uintptr_t remote_module_by_path(DWORD pid, const wchar_t *path) {
    HANDLE snap = CreateToolhelp32Snapshot(
            TH32CS_SNAPMODULE | TH32CS_SNAPMODULE32, pid);
    if (snap == INVALID_HANDLE_VALUE) return 0;
    MODULEENTRY32W entry{};
    entry.dwSize = sizeof(entry);
    uintptr_t result = 0;
    if (Module32FirstW(snap, &entry)) {
        do {
            if (_wcsicmp(entry.szExePath, path) == 0) {
                result = (uintptr_t)entry.modBaseAddr;
                break;
            }
        } while (Module32NextW(snap, &entry));
    }
    CloseHandle(snap);
    return result;
}

static int require_x64_target(HANDLE process) {
    typedef BOOL (WINAPI *fn)(HANDLE, USHORT *, USHORT *);
    auto fp = (fn)GetProcAddress(GetModuleHandleW(L"kernel32.dll"),
            "IsWow64Process2");
    if (fp) {
        USHORT pm = IMAGE_FILE_MACHINE_UNKNOWN;
        USHORT nm = IMAGE_FILE_MACHINE_UNKNOWN;
        if (!fp(process, &pm, &nm)) return 0;
        return pm == IMAGE_FILE_MACHINE_UNKNOWN
                && nm == IMAGE_FILE_MACHINE_AMD64;
    }
    BOOL wow64 = FALSE;
    if (!IsWow64Process(process, &wow64)) return 0;
    return !wow64 && sizeof(void *) == 8;
}

static int inject_library(DWORD pid, const wchar_t *dll_path) {
    if (remote_module_by_path(pid, dll_path) != 0) return 2;
    HANDLE proc = OpenProcess(PROCESS_CREATE_THREAD | PROCESS_QUERY_INFORMATION
            | PROCESS_VM_OPERATION | PROCESS_VM_WRITE | PROCESS_VM_READ,
            FALSE, pid);
    if (!proc) return 0;
    if (!require_x64_target(proc)) { CloseHandle(proc); return 0; }
    SIZE_T path_bytes = (wcslen(dll_path) + 1) * sizeof(wchar_t);
    LPVOID remote_path = VirtualAllocEx(proc, nullptr, path_bytes,
            MEM_COMMIT | MEM_RESERVE, PAGE_READWRITE);
    if (!remote_path) { CloseHandle(proc); return 0; }
    SIZE_T written = 0;
    if (!WriteProcessMemory(proc, remote_path, dll_path, path_bytes, &written)
            || written != path_bytes) {
        VirtualFreeEx(proc, remote_path, 0, MEM_RELEASE);
        CloseHandle(proc);
        return 0;
    }
    HMODULE k32 = GetModuleHandleW(L"kernel32.dll");
    FARPROC local_ll = GetProcAddress(k32, "LoadLibraryW");
    uintptr_t rk32 = remote_module_base(pid, L"kernel32.dll");
    if (!k32 || !local_ll || !rk32) {
        VirtualFreeEx(proc, remote_path, 0, MEM_RELEASE);
        CloseHandle(proc);
        return 0;
    }
    auto remote_ll = (LPTHREAD_START_ROUTINE)(
            rk32 + ((uintptr_t)local_ll - (uintptr_t)k32));
    HANDLE thread = CreateRemoteThread(proc, nullptr, 0, remote_ll,
            remote_path, 0, nullptr);
    int result = 0;
    if (thread) {
        if (WaitForSingleObject(thread, 30000) == WAIT_OBJECT_0) {
            for (int a = 0; a < 100; ++a) {
                if (remote_module_by_path(pid, dll_path)) { result = 1; break; }
                Sleep(50);
            }
        }
        CloseHandle(thread);
    }
    VirtualFreeEx(proc, remote_path, 0, MEM_RELEASE);
    CloseHandle(proc);
    return result;
}

static int validate_downloaded_payload(const unsigned char *data, size_t size) {
    if (!data || size < sizeof(IMAGE_DOS_HEADER) || size > 40u * 1024u * 1024u)
        return 0;
    auto *dos = (const IMAGE_DOS_HEADER *)data;
    if (dos->e_magic != IMAGE_DOS_SIGNATURE || dos->e_lfanew <= 0) return 0;
    size_t nt_off = (size_t)dos->e_lfanew;
    if (nt_off > size || size - nt_off < sizeof(IMAGE_NT_HEADERS64)) return 0;
    auto *nt = (const IMAGE_NT_HEADERS64 *)(data + nt_off);
    return nt->Signature == IMAGE_NT_SIGNATURE
            && nt->FileHeader.Machine == IMAGE_FILE_MACHINE_AMD64
            && nt->FileHeader.NumberOfSections > 0
            && nt->FileHeader.NumberOfSections <= 96
            && nt->OptionalHeader.Magic == IMAGE_NT_OPTIONAL_HDR64_MAGIC
            && nt->OptionalHeader.SizeOfImage > 0;
}

static int protect_remote_username(const char *username, const char *token,
        unsigned char *out, DWORD out_cap, DWORD *out_size) {
    if (!username || !token || !out || !out_size
            || username[0] == '\0' || token[0] == '\0') return 0;
    size_t ulen = strlen(username) + 1;
    size_t tlen = strlen(token);
    if (ulen > 96 || tlen > 255) return 0;
    DATA_BLOB plain{(DWORD)ulen, (BYTE *)username};
    DATA_BLOB entropy{(DWORD)tlen, (BYTE *)token};
    DATA_BLOB prot{};
    int result = 0;
    if (CryptProtectData(&plain, nullptr, &entropy, nullptr, nullptr,
            CRYPTPROTECT_UI_FORBIDDEN, &prot)) {
        if (prot.cbData > 0 && prot.cbData <= out_cap) {
            memcpy(out, prot.pbData, prot.cbData);
            *out_size = prot.cbData;
            result = 1;
        }
        if (prot.pbData) {
            SecureZeroMemory(prot.pbData, prot.cbData);
            LocalFree(prot.pbData);
        }
    }
    return result;
}

struct remote_session_handoff {
    HANDLE mapping;
    HANDLE ack_event;
    SonyVegas421RemoteSessionV2 *block;
};

static void remote_session_name(wchar_t *out, size_t cap,
        const wchar_t *kind, DWORD pid) {
    _snwprintf_s(out, cap, _TRUNCATE, L"Local\\IllIIlIl.%ls.%lu", kind, pid);
}

static int begin_remote_session_handoff(DWORD pid, const char *token,
        const char *username, const char *role, remote_session_handoff *h) {
    wchar_t mn[96], en[96];
    char identity[96]{};
    unsigned char prot[512]{};
    DWORD prot_size = 0;
    memset(h, 0, sizeof(*h));
    _snprintf_s(identity, sizeof(identity), _TRUNCATE, "%s\t%s", username,
            role != nullptr && _stricmp(role, "developer") == 0
                    ? "developer" : "user");
    if (!token || !token[0] || strlen(token) >= sizeof(h->block->control_token)
            || !protect_remote_username(identity, token, prot, sizeof(prot),
                    &prot_size)) {
        SecureZeroMemory(identity, sizeof(identity));
        return 0;
    }
    SecureZeroMemory(identity, sizeof(identity));
    remote_session_name(mn, _countof(mn), L"RemoteSession", pid);
    remote_session_name(en, _countof(en), L"RemoteSessionAck", pid);
    h->mapping = CreateFileMappingW(INVALID_HANDLE_VALUE, nullptr,
            PAGE_READWRITE, 0, sizeof(SonyVegas421RemoteSessionV2), mn);
    if (!h->mapping) {
        SecureZeroMemory(prot, sizeof(prot)); return 0;
    }
    h->block = (SonyVegas421RemoteSessionV2 *)MapViewOfFile(
            h->mapping, FILE_MAP_READ | FILE_MAP_WRITE, 0, 0,
            sizeof(SonyVegas421RemoteSessionV2));
    if (!h->block) {
        SecureZeroMemory(prot, sizeof(prot));
        CloseHandle(h->mapping); memset(h, 0, sizeof(*h)); return 0;
    }
    h->ack_event = CreateEventW(nullptr, FALSE, FALSE, en);
    if (!h->ack_event) {
        SecureZeroMemory(prot, sizeof(prot));
        UnmapViewOfFile(h->block); CloseHandle(h->mapping);
        memset(h, 0, sizeof(*h)); return 0;
    }
    memset(h->block, 0, sizeof(*h->block));
    h->block->magic = SONYVEGAS421_REMOTE_SESSION_MAGIC;
    h->block->version = SONYVEGAS421_REMOTE_SESSION_VERSION;
    h->block->structure_size = sizeof(*h->block);
    h->block->target_pid = pid;
    h->block->status = SONYVEGAS421_REMOTE_SESSION_CREATED;
    strcpy_s(h->block->control_token,
            sizeof(h->block->control_token), token);
    h->block->protected_username_size = prot_size;
    memcpy(h->block->protected_username, prot, prot_size);
    SecureZeroMemory(prot, sizeof(prot));
    return 1;
}

static int finish_remote_session_handoff(remote_session_handoff *h) {
    int consumed = 0;
    if (h->ack_event) WaitForSingleObject(h->ack_event, 5000);
    if (h->block) {
        consumed = h->block->status == SONYVEGAS421_REMOTE_SESSION_CONSUMED;
        SecureZeroMemory(h->block->control_token,
                sizeof(h->block->control_token));
        SecureZeroMemory(h->block->protected_username,
                sizeof(h->block->protected_username));
        h->block->protected_username_size = 0;
        UnmapViewOfFile(h->block);
    }
    if (h->ack_event) CloseHandle(h->ack_event);
    if (h->mapping)   CloseHandle(h->mapping);
    memset(h, 0, sizeof(*h));
    return consumed;
}

/* ═══════════════════════════════════════════════════════════════════
 *  WORKER THREAD — runs the auth / download / injection pipeline
 * ═══════════════════════════════════════════════════════════════════ */

static void set_status(const char *status, const char *detail) {
    if (status) strncpy_s(g_status, status, _TRUNCATE);
    if (detail) strncpy_s(g_detail, detail, _TRUNCATE);
}

static void worker_thread_func() {
    char hwid[64]{};
    char device_secret[256]{};
    char error_buf[256]{};
    char username_buf[64]{};
    char authorized_username[64]{};
    control_api_debug_log(L"=== Crimson loader diagnostic session started ===");
    control_api_debug_log(L"Remote worker started");

    /* ── 1. generate HWID ─────────────────────────────────────── */
    g_progress = 10;
    set_status("GENERATING DEVICE ID",
            "Creating a secure fingerprint for this device");
    if (!sonyvegas_generate_hwid(hwid, sizeof(hwid))) {
        control_api_debug_log(L"HWID generation failed error=%lu", GetLastError());
        strncpy_s(g_error_msg, "Could not generate a secure device fingerprint.",
                _TRUNCATE);
        g_state = UI_ERROR;
        return;
    }

    /* ── 2. check existing HWID ───────────────────────────────── */
    sonyvegas_load_device_secret(device_secret, sizeof(device_secret));
    control_api_debug_log(L"Device secret present=%d", device_secret[0] != '\0');
    g_progress = 24;
    set_status("Verificando licenca",
            "Conferindo sua licenca com o servidor");

    if (control_api_check_hwid(hwid, device_secret, nullptr,
            username_buf, sizeof(username_buf))) {
        control_api_debug_log(L"Authorization state=existing-device");
        strncpy_s(authorized_username, username_buf, _TRUNCATE);
        strncpy_s(g_username, username_buf, _TRUNCATE);
        g_license_key[0] = '\0';
        g_require_license = false;
        g_state = UI_LOGIN;
        WaitForSingleObject(g_auth_event, INFINITE);
        if (g_state == UI_CLOSING) return;
        g_state = UI_AUTH_WORKING;
        g_progress = 38;
        set_status("LICENSE VERIFIED", "Saving your username");
        if (_stricmp(g_username, authorized_username) != 0) {
            strncpy_s(g_error_msg, "Authentication failed.", _TRUNCATE);
            g_state = UI_ERROR;
            return;
        }
        if (!control_api_check_hwid(hwid, device_secret, g_username,
                username_buf, sizeof(username_buf))) {
            control_api_debug_log(L"Username update authorization failed");
            strncpy_s(g_error_msg, "Could not save the username for this device.",
                    _TRUNCATE);
            g_state = UI_ERROR;
            return;
        }
        strncpy_s(g_username, username_buf, _TRUNCATE);
    } else if (control_api_claim_required()) {
        control_api_debug_log(L"Authorization state=claim-required");
        g_state = UI_LOGIN;
        g_username[0] = '\0';
        g_require_license = true;
        WaitForSingleObject(g_auth_event, INFINITE);
        if (g_state == UI_CLOSING) return;
        g_state = UI_AUTH_WORKING;
        g_progress = 32;
        set_status("SECURING DEVICE",
                "Migrating your Crimson license to protected storage");
        if (!control_api_claim_device(g_license_key, hwid, g_username,
                username_buf, sizeof(username_buf),
                error_buf, sizeof(error_buf))) {
            control_api_debug_log(L"Device claim failed");
            strncpy_s(g_error_msg, error_buf[0] ? error_buf
                    : "Device claim failed.", _TRUNCATE);
            g_state = UI_ERROR;
            return;
        }
        strncpy_s(g_username, username_buf, _TRUNCATE);
        g_progress = 38;
        set_status("DEVICE SECURED",
                "Your Crimson license is now protected on this device");
        Sleep(600);
    } else {
        control_api_debug_log(L"Authorization state=new-or-rejected-device");
        g_state = UI_LOGIN;
        g_require_license = true;
        WaitForSingleObject(g_auth_event, INFINITE);
        if (g_state == UI_CLOSING) return;
        g_state = UI_AUTH_WORKING;
        g_progress = 32;
        set_status("ACTIVATING LICENSE",
                "Registering this device with Crimson");
        if (!control_api_activate_license(g_license_key, hwid, g_username,
                error_buf, sizeof(error_buf))) {
            control_api_debug_log(L"License activation failed");
            strncpy_s(g_error_msg, error_buf[0] ? error_buf
                    : "License activation failed.", _TRUNCATE);
            g_state = UI_ERROR;
            return;
        }
        g_progress = 38;
        set_status("LICENSE ACTIVATED",
                "Welcome to Crimson. Your device is ready.");
        Sleep(600);
    }

    /* ── 3. store new device secret ───────────────────────────── */
    if (control_api_new_device_secret()[0] != '\0'
            && !sonyvegas_store_device_secret(
                    control_api_new_device_secret())) {
        strncpy_s(g_error_msg,
                "Could not store the protected device session.", _TRUNCATE);
        g_state = UI_ERROR;
        return;
    }
    SecureZeroMemory(device_secret, sizeof(device_secret));
    SecureZeroMemory(g_license_key, sizeof(g_license_key));

    /* ── 4. download payload ──────────────────────────────────── */
    g_state = UI_DOWNLOAD;
    g_progress = 52;
    set_status("Baixando modulos",
            "Recebendo os modulos mais recentes");
    if (!control_api_download_payload(hwid, &g_dll_data, &g_dll_size)) {
        control_api_debug_log(L"Payload download failed");
        strncpy_s(g_error_msg, "The Crimson payload download failed.",
                _TRUNCATE);
        g_state = UI_ERROR;
        return;
    }
    if (!validate_downloaded_payload(g_dll_data, g_dll_size)) {
        control_api_debug_log(L"Payload validation failed bytes=%zu", g_dll_size);
        strncpy_s(g_error_msg, "The downloaded Crimson payload is invalid.",
                _TRUNCATE);
        free(g_dll_data);
        g_dll_data = nullptr;
        g_state = UI_ERROR;
        return;
    }
    control_api_debug_log(L"Payload validated bytes=%zu", g_dll_size);
    g_progress = 68;
    set_status("PAYLOAD VERIFIED",
            "Crimson is signed, validated and ready to inject");
    Sleep(600);

    /* ── 5. process selection ─────────────────────────────────── */
    g_state = UI_PROCESS_SELECT;
    WaitForSingleObject(g_select_event, INFINITE);
    if (g_state == UI_CLOSING) {
        if (g_dll_data) {
            SecureZeroMemory(g_dll_data, g_dll_size);
            free(g_dll_data);
            g_dll_data = nullptr;
        }
        return;
    }

    /* ── 6. inject ────────────────────────────────────────────── */
    g_state = UI_INJECTING;
    g_progress = 76;
    if (_stricmp(g_target_client, "CheatBreaker") == 0) {
        control_api_debug_log(L"CB mapping synchronization started pid=%lu",
                (unsigned long)g_target_pid);
        set_status("UPDATING MAPPINGS",
                "Checking the installed CheatBreaker version");
        error_buf[0] = '\0';
        if (!control_api_prepare_cheatbreaker_mappings(
                error_buf, sizeof(error_buf))) {
            strncpy_s(g_error_msg, error_buf[0] ? error_buf
                    : "CheatBreaker mappings are unavailable.", _TRUNCATE);
            SecureZeroMemory(g_dll_data, g_dll_size);
            free(g_dll_data);
            g_dll_data = nullptr;
            g_state = UI_ERROR;
            return;
        }
        control_api_debug_log(L"CB mapping synchronization completed");
    }
    set_status("Buscando Minecraft",
            "Procurando o processo do jogo");

    remote_session_handoff handoff{};
    int handoff_started = begin_remote_session_handoff(g_target_pid,
            control_api_session_token(), control_api_session_username(),
            control_api_session_role(),
            &handoff);
    control_api_debug_log(L"Remote session handoff started=%d", handoff_started);

    HANDLE proc = OpenProcess(
            PROCESS_CREATE_THREAD | PROCESS_QUERY_INFORMATION
            | PROCESS_VM_OPERATION | PROCESS_VM_WRITE | PROCESS_VM_READ,
            FALSE, g_target_pid);
    if (!proc) {
        control_api_debug_log(L"OpenProcess failed pid=%lu error=%lu",
                (unsigned long)g_target_pid, GetLastError());
        if (handoff_started) finish_remote_session_handoff(&handoff);
        SecureZeroMemory(g_dll_data, g_dll_size);
        free(g_dll_data);
        g_dll_data = nullptr;
        strncpy_s(g_error_msg,
                "Could not open the selected game process.", _TRUNCATE);
        g_state = UI_ERROR;
        return;
    }
    if (!require_x64_target(proc)) {
        control_api_debug_log(L"Target architecture rejected pid=%lu error=%lu",
                (unsigned long)g_target_pid, GetLastError());
        CloseHandle(proc);
        if (handoff_started) finish_remote_session_handoff(&handoff);
        SecureZeroMemory(g_dll_data, g_dll_size);
        free(g_dll_data);
        g_dll_data = nullptr;
        strncpy_s(g_error_msg,
                "Crimson requires a 64-bit Java game process.", _TRUNCATE);
        g_state = UI_ERROR;
        return;
    }
    g_progress = 88;
    set_status("INJECTING CRIMSON",
            "Mapping the client safely into the Java game process");
    g_injection_result = manual_map_inject(g_target_pid, proc,
            g_dll_data, g_dll_size);
    control_api_debug_log(L"manual_map_inject returned=%d pid=%lu",
            g_injection_result, (unsigned long)g_target_pid);
    CloseHandle(proc);
    SecureZeroMemory(g_dll_data, g_dll_size);
    free(g_dll_data);
    g_dll_data = nullptr;

    if (handoff_started) finish_remote_session_handoff(&handoff);

    if (g_injection_result == 0) {
        int stage = manual_map_last_stage();
        const char *mod = manual_map_last_module();
        const char *func = manual_map_last_function();
        const char *detail = (func && func[0]) ? func
                : (mod && mod[0] ? mod : nullptr);
        if (stage == 5 && detail) {
            _snprintf_s(g_error_msg, sizeof(g_error_msg), _TRUNCATE,
                    "Injection failed at stage 5 (%.160s).", detail);
        } else {
            _snprintf_s(g_error_msg, sizeof(g_error_msg), _TRUNCATE,
                    "Injection failed at stage %d.", stage);
        }
        g_state = UI_ERROR;
        return;
    }
    g_progress = 100;
    set_status(g_injection_result == 2 ? "CRIMSON IS ALREADY ACTIVE"
                                       : "CRIMSON INJECTED",
            "Crimson is ready");
    control_api_update_last_injection();
    g_state = UI_SUCCESS;
}

/* worker for local mode — simpler flow */
static void worker_local_func(const wchar_t *dll_path) {
    g_progress = 68;
    set_status("LOCAL PAYLOAD READY",
            "The local Crimson payload was found and validated");
    Sleep(600);

    g_state = UI_PROCESS_SELECT;
    WaitForSingleObject(g_select_event, INFINITE);
    if (g_state == UI_CLOSING) return;

    g_state = UI_INJECTING;
    g_progress = 88;
    set_status("INJECTING CRIMSON",
            "Loading the local client into the Java game process");
    g_injection_result = inject_library(g_target_pid, dll_path);

    if (g_injection_result == 0) {
        strncpy_s(g_error_msg,
                "Injection failed. Check the native log.", _TRUNCATE);
        g_state = UI_ERROR;
        return;
    }
    g_progress = 100;
    set_status(g_injection_result == 2 ? "CRIMSON IS ALREADY ACTIVE"
                                       : "CRIMSON INJECTED",
            "Crimson is ready");
    control_api_update_last_injection();
    g_state = UI_SUCCESS;
}

/* ═══════════════════════════════════════════════════════════════════
 *  DX11 SETUP
 * ═══════════════════════════════════════════════════════════════════ */

static void CreateRenderTarget() {
    ID3D11Texture2D *buf = nullptr;
    g_swap_chain->GetBuffer(0, IID_PPV_ARGS(&buf));
    if (buf) {
        g_device->CreateRenderTargetView(buf, nullptr, &g_render_target_view);
        buf->Release();
    }
}

static void CleanupRenderTarget() {
    if (g_render_target_view) {
        g_render_target_view->Release();
        g_render_target_view = nullptr;
    }
}

static bool CreateDeviceD3D(HWND hWnd) {
    DXGI_SWAP_CHAIN_DESC sd{};
    sd.BufferCount = 2;
    sd.BufferDesc.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
    sd.BufferDesc.RefreshRate = {60, 1};
    sd.Flags = DXGI_SWAP_CHAIN_FLAG_ALLOW_MODE_SWITCH;
    sd.BufferUsage = DXGI_USAGE_RENDER_TARGET_OUTPUT;
    sd.OutputWindow = hWnd;
    sd.SampleDesc = {1, 0};
    sd.Windowed = TRUE;
    sd.SwapEffect = DXGI_SWAP_EFFECT_DISCARD;
    D3D_FEATURE_LEVEL fl;
    D3D_FEATURE_LEVEL levels[] = {D3D_FEATURE_LEVEL_11_0,
                                   D3D_FEATURE_LEVEL_10_0};
    if (D3D11CreateDeviceAndSwapChain(nullptr, D3D_DRIVER_TYPE_HARDWARE,
            nullptr, 0, levels, 2, D3D11_SDK_VERSION, &sd,
            &g_swap_chain, &g_device, &fl, &g_device_context) != S_OK)
        return false;
    CreateRenderTarget();
    return true;
}

static void CleanupDeviceD3D() {
    CleanupRenderTarget();
    if (g_logo_texture)    { g_logo_texture->Release();    g_logo_texture = nullptr; }
    if (g_swap_chain)      { g_swap_chain->Release();      g_swap_chain = nullptr; }
    if (g_device_context)  { g_device_context->Release();  g_device_context = nullptr; }
    if (g_device)          { g_device->Release();          g_device = nullptr; }
}

static bool CreateLogoTexture() {
    D3D11_TEXTURE2D_DESC desc{};
    desc.Width = CRIMSON_LOGO_WIDTH;
    desc.Height = CRIMSON_LOGO_HEIGHT;
    desc.MipLevels = 1;
    desc.ArraySize = 1;
    desc.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
    desc.SampleDesc.Count = 1;
    desc.Usage = D3D11_USAGE_IMMUTABLE;
    desc.BindFlags = D3D11_BIND_SHADER_RESOURCE;

    D3D11_SUBRESOURCE_DATA pixels{};
    pixels.pSysMem = CRIMSON_LOGO_RGBA;
    pixels.SysMemPitch = CRIMSON_LOGO_WIDTH * 4;

    ID3D11Texture2D *texture = nullptr;
    HRESULT result = g_device->CreateTexture2D(&desc, &pixels, &texture);
    if (FAILED(result) || !texture) return false;
    result = g_device->CreateShaderResourceView(texture, nullptr,
            &g_logo_texture);
    texture->Release();
    return SUCCEEDED(result) && g_logo_texture != nullptr;
}

/* ═══════════════════════════════════════════════════════════════════
 *  IMGUI THEME — CRIMSON
 * ═══════════════════════════════════════════════════════════════════ */

static void InitCrimsonStyle() {
    ImGui::StyleColorsDark();
    auto &io = ImGui::GetIO();
    io.IniFilename = nullptr;
    io.LogFilename = nullptr;

    g_font_regular = io.Fonts->AddFontFromMemoryCompressedTTF(
            InterRegular_compressed_data, InterRegular_compressed_size, 16);
    g_font_black = io.Fonts->AddFontFromMemoryCompressedTTF(
            InterBlack_compressed_data, InterBlack_compressed_size, 30);
    g_font_bold = io.Fonts->AddFontFromMemoryCompressedTTF(
            InterBold_compressed_data, InterBold_compressed_size, 20);
    g_font_extra_bold = io.Fonts->AddFontFromMemoryCompressedTTF(
            InterExtraBold_compressed_data, InterExtraBold_compressed_size, 14);
    g_font_extra_light = io.Fonts->AddFontFromMemoryCompressedTTF(
            InterExtraLight_compressed_data, InterExtraLight_compressed_size, 14);
    g_font_light = io.Fonts->AddFontFromMemoryCompressedTTF(
            InterLight_compressed_data, InterLight_compressed_size, 16);
    g_font_medium = io.Fonts->AddFontFromMemoryCompressedTTF(
            InterMedium_compressed_data, InterMedium_compressed_size, 20);
    g_font_semi_bold = io.Fonts->AddFontFromMemoryCompressedTTF(
            InterSemiBold_compressed_data, InterSemiBold_compressed_size, 18);
    g_font_thin = io.Fonts->AddFontFromMemoryCompressedTTF(
            InterThin_compressed_data, InterThin_compressed_size, 14);

    static const ImWchar icon_ranges[] = {0xe000, 0xf8ff, 0};
    ImFontConfig icon_cfg;
    icon_cfg.MergeMode = true;
    icon_cfg.PixelSnapH = true;
    g_font_awesome = io.Fonts->AddFontFromMemoryCompressedTTF(
            FontAwesomeData, FontAwesomeSize, 17.f, &icon_cfg, icon_ranges);

    /* Keep the original Monesy ImGui styling byte-for-byte in values. */
    auto *s = &ImGui::GetStyle();
    s->WindowBorderSize = 0.f;
    s->WindowPadding = ImVec2(0, 0);
    s->WindowRounding = 10;
    s->ChildRounding = 6;
    s->FrameRounding = 6;

    s->Colors[ImGuiCol_Text] = ImColor(126, 125, 126);
    s->Colors[ImGuiCol_WindowBg] = ImColor(3, 2, 3);
    s->Colors[ImGuiCol_HeaderBg] = ImColor(7, 5, 7);
    s->Colors[ImGuiCol_MenuChildBg] = ImColor(6, 4, 6);
    s->Colors[ImGuiCol_MenuChildHeaderBg] = ImColor(9, 7, 9);
    s->Colors[ImGuiCol_Border] = ImColor(10, 9, 10);
    s->Colors[ImGuiCol_Color] = ImColor(205, 18, 52);
    s->Colors[ImGuiCol_Button] = ImColor(205, 18, 52, 200);
    s->Colors[ImGuiCol_ButtonHovered] = ImColor(245, 28, 67, 255);
    s->Colors[ImGuiCol_ButtonActive] = s->Colors[ImGuiCol_ButtonHovered];
    s->Colors[ImGuiCol_FrameBg] = ImColor(10, 9, 10);

    /* Defaults not customized by the reference UI stay dark and neutral. */
    s->Colors[ImGuiCol_TextDisabled] = ImColor(75, 74, 76);
    s->Colors[ImGuiCol_FrameBgHovered] = ImColor(18, 16, 20);
    s->Colors[ImGuiCol_FrameBgActive] = ImColor(24, 20, 28);
    s->Colors[ImGuiCol_Header] = ImColor(16, 13, 20);
    s->Colors[ImGuiCol_HeaderHovered] = ImColor(30, 25, 42);
    s->Colors[ImGuiCol_HeaderActive] = ImColor(40, 33, 56);
    s->Colors[ImGuiCol_CheckMark] = ImColor(225, 20, 58);
    s->Colors[ImGuiCol_SliderGrab] = ImColor(205, 18, 52);
    s->Colors[ImGuiCol_SliderGrabActive] = ImColor(245, 28, 67);

    CreateLogoTexture();
}

/* ═══════════════════════════════════════════════════════════════════
 *  RENDER HELPERS
 * ═══════════════════════════════════════════════════════════════════ */

static void CenterText(const char *text, ImFont *font,
        ImU32 color, float y_offset) {
    ImGui::PushFont(font);
    ImVec2 sz = ImGui::CalcTextSize(text);
    ImGui::SetCursorPos(ImVec2((WIN_W - sz.x) / 2, y_offset));
    ImGui::TextColored(ImGui::ColorConvertU32ToFloat4(color), "%s", text);
    ImGui::PopFont();
}

static void RenderHeader() {
    if (ImGui::Header("")) {
        g_state = UI_CLOSING;
        if (g_auth_event)   SetEvent(g_auth_event);
        if (g_select_event) SetEvent(g_select_event);
        PostQuitMessage(0);
    }

    if (g_logo_texture) {
        ImVec2 wp = ImGui::GetWindowPos();
        ImGui::GetWindowDrawList()->AddImage(g_logo_texture,
                wp + ImVec2(12, 7), wp + ImVec2(48, 43));
    }

    ImVec2 mouse = ImGui::GetMousePos();
    ImVec2 wp = ImGui::GetWindowPos();
    bool over_stream_mode = mouse.x >= wp.x + 425
            && mouse.x < wp.x + WIN_W - 95
            && mouse.y >= wp.y + 8 && mouse.y < wp.y + 42;
    bool over_drag_area = mouse.x >= wp.x && mouse.x < wp.x + WIN_W - 95
            && mouse.y >= wp.y && mouse.y < wp.y + 50
            && !over_stream_mode;
    if (over_drag_area && ImGui::IsMouseClicked(ImGuiMouseButton_Left)) {
        ReleaseCapture();
        SendMessageW(g_hwnd, WM_NCLBUTTONDOWN, HTCAPTION, 0);
    }
}

/* ═══════════════════════════════════════════════════════════════════
 *  RENDER PAGES — original Monesy ImGui layout, existing loader logic
 * ═══════════════════════════════════════════════════════════════════ */

static void RenderLogin() {
    RenderHeader();
    ImGui::SetCursorPos(ImVec2(155, 95));
    ImGui::MenuChild("Sign In", ImVec2(340, 220));
    ImGui::SetCursorPos(ImVec2(20, 20));
    ImGui::BeginGroup();
    ImGui::PushFont(g_font_semi_bold);

    ImGui::Text("Username");
    ImGui::SetNextItemWidth(300);
    ImGui::InputText("##Username", g_username, IM_ARRAYSIZE(g_username));
    ImGui::Spacing();
    ImGui::Spacing();

    if (g_require_license.load()) {
        ImGui::Text("License Key");
        ImGui::SetNextItemWidth(300);
        ImGui::InputText("##LicenseKey", g_license_key,
                IM_ARRAYSIZE(g_license_key), ImGuiInputTextFlags_Password);
        ImGui::Spacing();
        ImGui::Spacing();
    }

    bool can_login = g_username[0] != '\0'
            && (!g_require_license.load() || g_license_key[0] != '\0');
    if (!can_login) ImGui::PushStyleVar(ImGuiStyleVar_Alpha, 0.4f);
    if (ImGui::Button("Log In", ImVec2(100, 30)) && can_login)
        SetEvent(g_auth_event);
    if (!can_login) ImGui::PopStyleVar();
    ImGui::PopFont();

    ImGui::SameLine();
    ImGui::SetCursorPosY(ImGui::GetCursorPosY() + 5);
    ImGui::Text("|");
    ImGui::SameLine();
    ImGui::SetCursorPosY(ImGui::GetCursorPosY() + 5);
    ImColor color = ImGui::GetColorU32(ImGuiCol_Text);
    ImGui::TextColored(ImGui::ColorConvertU32ToFloat4(
            ImColor(color.Value.x, color.Value.y, color.Value.z,
                    ImGui::GetStyle().Alpha)), "Authorized accounts only");

    ImGui::EndGroup();
    ImGui::EndMenuChild();
}

static void RenderWorking() {
    RenderHeader();
    static float tab_alpha = 0.f;
    tab_alpha = ImClamp(tab_alpha + 0.5f * ImGui::GetIO().DeltaTime,
            0.f, 1.f);
    ImGui::PushStyleVar(ImGuiStyleVar_Alpha,
            tab_alpha * ImGui::GetStyle().Alpha);
    ImGui::PushFont(g_font_medium);

    const char *status = g_status[0] ? g_status : "Loading";
    ImVec2 text_size = ImGui::CalcTextSize(status);
    ImVec2 text_position((ImGui::GetWindowSize().x - text_size.x) * 0.5f,
            (ImGui::GetWindowSize().y - text_size.y) * 0.5f - 20.f);
    ImGui::SetCursorPos(text_position);
    ImGui::BeginGroup();
    ImColor accent = ImGui::GetColorU32(ImGuiCol_Color);
    ImGui::TextColored(ImGui::ColorConvertU32ToFloat4(
            ImColor(accent.Value.x, accent.Value.y, accent.Value.z,
                    ImGui::GetStyle().Alpha)), "%s", status);
    UIState state = g_state.load();
    float spinner_radius = state == UI_DOWNLOAD ? 26.f : 20.f;
    ImGui::SetCursorPosX(ImGui::GetCursorPosX() + text_size.x * 0.5f
            - spinner_radius);
    ImGui::SetCursorPosY(ImGui::GetCursorPosY() + 20.f);
    ImColor spinner = ImGui::GetColorU32(ImGuiCol_Text);
    if (state == UI_DOWNLOAD) {
        ImColor crimson = ImColor(225, 20, 58,
                (int)(255.f * ImGui::GetStyle().Alpha));
        ImSpinner::SpinnerSpiral("##module_download_spiral", 26.f, 3.f,
                crimson, 3.6f, 5);
    } else {
        ImSpinner::SpinnerPatternRings("##loading", 20.f, 4.f,
                ImGui::ColorConvertU32ToFloat4(ImColor(spinner.Value.x,
                        spinner.Value.y, spinner.Value.z,
                        ImGui::GetStyle().Alpha)), 3.5f);
    }
    ImGui::EndGroup();
    ImGui::PopFont();

    if (g_detail[0]) {
        CenterText(g_detail, g_font_light, ImGui::GetColorU32(ImGuiCol_Text),
                292.f);
    }
    ImGui::PopStyleVar();
}

static void RenderProcessSelect() {
    static ULONGLONG next_refresh = 0;
    ULONGLONG now = GetTickCount64();
    if (now >= next_refresh) {
        g_candidate_count = enumerate_candidates_list(
                g_candidates, MAX_CANDIDATES);
        next_refresh = now + 750;
    }

    if (g_selected_candidate >= g_candidate_count)
        g_selected_candidate = 0;

    RenderHeader();
    ImGui::SetCursorPos(ImVec2(25, 75));
    ImGui::PushStyleColor(ImGuiCol_ChildBg,
            ImGui::GetColorU32(ImGuiCol_MenuChildBg));
    ImGui::BeginChild("MenuMinecraft", ImVec2(285, 245),
            ImGuiChildFlags_Border);
    ImGui::PushStyleVar(ImGuiStyleVar_ItemSpacing, ImVec2(0, 0));
    if (g_candidate_count == 0) {
        ImGui::SetCursorPos(ImVec2(20, 25));
        ImGui::PushFont(g_font_semi_bold);
        ImGui::PushTextWrapPos(260.f);
        ImGui::TextWrapped("Waiting for a visible Minecraft Java window...");
        ImGui::PopTextWrapPos();
        ImGui::PopFont();
    } else {
        for (int i = 0; i < g_candidate_count; ++i) {
            if (ImGui::InsertCheat(ICON_FA_GHOST,
                    g_candidates[i].display, "",
                    i == g_selected_candidate)) {
                g_selected_candidate = i;
            }
        }
    }
    ImGui::PopStyleVar();
    ImGui::EndChild();
    ImGui::PopStyleColor();

    ImGui::SetCursorPos(ImVec2(330, 75));
    ImGui::PushStyleColor(ImGuiCol_ChildBg,
            ImGui::GetColorU32(ImGuiCol_MenuChildBg));
    ImGui::BeginChild("MenuInfo", ImVec2(285, 245),
            ImGuiChildFlags_Border);
    ImGui::SetCursorPos(ImVec2(20, 20));
    ImGui::BeginGroup();
    ImGui::PushFont(g_font_bold);
    ImGui::Text("Informations");
    ImGui::PopFont();
    ImColor muted = ImColor(80, 80, 80);
    ImGui::TextColored(ImGui::ColorConvertU32ToFloat4(
            ImColor(muted.Value.x, muted.Value.y, muted.Value.z,
                    ImGui::GetStyle().Alpha)), "Select the game process");
    ImGui::SetCursorPosY(ImGui::GetCursorPosY() + 8);
    const process_candidate *selected = g_candidate_count > 0
            ? &g_candidates[g_selected_candidate] : nullptr;
    char selected_pid[32] = "-";
    if (selected) {
        _snprintf_s(selected_pid, sizeof(selected_pid), _TRUNCATE, "%lu",
                (unsigned long)selected->process_id);
    }
    ImGui::CheatInformation(ICON_FA_GHOST, "Client",
            selected ? selected->client_name : "Waiting", Information_Plain);
    ImGui::CheatInformation(ICON_FA_BARS, "PID", selected_pid,
            Information_Plain);
    ImGui::Spacing();
    bool has_selection = g_candidate_count > 0;
    if (!has_selection) ImGui::PushStyleVar(ImGuiStyleVar_Alpha, 0.4f);
    if (ImGui::Button("Load Client", ImVec2(245, 40)) && has_selection) {
        g_target_pid = g_candidates[g_selected_candidate].process_id;
        strncpy_s(g_target_client,
                g_candidates[g_selected_candidate].client_name, _TRUNCATE);
        control_api_debug_log(L"Process selected pid=%lu client=%S title=%ls",
                (unsigned long)g_target_pid, g_target_client,
                g_candidates[g_selected_candidate].title);
        SetEvent(g_select_event);
    }
    if (!has_selection) ImGui::PopStyleVar();
    ImGui::EndGroup();
    ImGui::EndChild();
    ImGui::PopStyleColor();

    char welcome[128];
    _snprintf_s(welcome, sizeof(welcome), _TRUNCATE, "Welcome Back, %s",
            g_username[0] ? g_username : "User");
    ImGui::PushFont(g_font_semi_bold);
    ImGui::GetWindowDrawList()->AddText(
            ImGui::GetWindowPos() + ImVec2(25, WIN_H - 30),
            ImGui::GetColorU32(ImGuiCol_Text), welcome);
    ImGui::PopFont();
}

static void RenderSuccess() {
    RenderHeader();
    ImGui::SetCursorPos(ImVec2(25, 75));
    ImGui::PushStyleColor(ImGuiCol_ChildBg,
            ImGui::GetColorU32(ImGuiCol_MenuChildBg));
    ImGui::BeginChild("MenuUserInfo", ImVec2(285, 245),
            ImGuiChildFlags_Border);
    ImGui::PushStyleVar(ImGuiStyleVar_ItemSpacing, ImVec2(0, 0));
    ImGui::SetCursorPos(ImVec2(20, 20));
    ImGui::BeginGroup();
    ImGui::PushFont(g_font_awesome);
    ImGui::Text(ICON_FA_USER);
    ImGui::PopFont();
    ImGui::SameLine();
    ImGui::SetCursorPos(ImVec2(ImGui::GetCursorPosX() + 6,
            ImGui::GetCursorPosY() - 5));
    ImGui::PushFont(g_font_bold);
    ImGui::Text("User Info");
    ImGui::PopFont();
    ImGui::SetCursorPosY(ImGui::GetCursorPosY() + 15);
    {
        const char *role = control_api_session_role();
        const char *time_left = control_api_session_time_left();
        const char *last_inj = control_api_session_last_injection();
        ImGui::CheatInformation(ICON_FA_USER, "User",
                g_username[0] ? g_username : "Unknown",
                Information_Plain);
        ImGui::CheatInformation(ICON_FA_SHIELD_ALT, "Role",
                role[0] ? role : "user",
                Information_Plain);
        ImGui::CheatInformation(ICON_FA_CLOCK, "TimeLeft",
                time_left[0] ? time_left : "Lifetime",
                Information_Plain);
        ImGui::CheatInformation(ICON_FA_HISTORY, "Last Injection",
                last_inj[0] ? last_inj : "First time",
                Information_Plain);
    }
    ImGui::EndGroup();
    ImGui::PopStyleVar();
    ImGui::EndChild();
    ImGui::PopStyleColor();

    ImGui::SetCursorPos(ImVec2(330, 75));
    ImGui::PushStyleColor(ImGuiCol_ChildBg,
            ImGui::GetColorU32(ImGuiCol_MenuChildBg));
    ImGui::BeginChild("MenuInjectionInfo", ImVec2(285, 245),
            ImGuiChildFlags_Border);
    ImGui::SetCursorPos(ImVec2(20, 20));
    ImGui::BeginGroup();
    ImGui::PushFont(g_font_bold);
    ImGui::Text("Informations");
    ImGui::PopFont();
    char pid_text[48];
    _snprintf_s(pid_text, sizeof(pid_text), _TRUNCATE, "PID %lu",
            (unsigned long)g_target_pid);
    ImGui::TextColored(ImColor(80, 80, 80), "%s", pid_text);
    ImGui::SetCursorPosY(ImGui::GetCursorPosY() + 8);
    ImGui::CheatInformation(ICON_FA_BARS, "Status",
            g_injection_result == 2 ? "Active" : "Injected",
            Information_Plain);
#if defined(_DEBUG)
    const char *build_channel = "Developer";
#else
    const char *build_channel = "Release";
#endif
    ImGui::CheatInformation(ICON_FA_CLOCK, "Version", build_channel,
            Information_Plain);
    ImGui::Spacing();
    ImGui::PushStyleColor(ImGuiCol_Text,
            ImVec4(248.f / 255.f, 248.f / 255.f, 250.f / 255.f, 1.f));
    if (ImGui::Button("Self Destruct", ImVec2(245, 40)))
        g_state = UI_SELF_DESTRUCT;
    ImGui::PopStyleColor();
    ImGui::EndGroup();
    ImGui::EndChild();
    ImGui::PopStyleColor();

    if ((GetAsyncKeyState(VK_F6) & 1) != 0)
        g_state = UI_SELF_DESTRUCT;
}

static void RenderError() {
    RenderHeader();
    ImGui::SetCursorPos(ImVec2(155, 85));
    ImGui::MenuChild("Startup Failed", ImVec2(340, 230));
    ImGui::SetCursorPos(ImVec2(20, 20));
    ImGui::BeginGroup();
    ImGui::PushFont(g_font_semi_bold);
    ImGui::PushTextWrapPos(300.f);
    ImGui::TextWrapped("%s", g_error_msg);
    ImGui::PopTextWrapPos();
    ImGui::SetCursorPosY(123.f);
    if (ImGui::Button("Close", ImVec2(300, 36))) {
        g_state = UI_CLOSING;
        PostQuitMessage(0);
    }
    ImGui::PopFont();
    ImGui::EndGroup();
    ImGui::EndMenuChild();
}

static void RenderSelfDestruct() {
    static bool started = false;
    if (!started) {
        started = true;
        std::thread([]() {
            cleanup_report cr;
            g_progress = 0;
            set_status("SELF DESTRUCT", "Removing the loader footprint");
            for (int i = 0; i <= 20; ++i) {
                g_progress = (int)(i * 4.6f);
                Sleep(25);
            }
            cleanup_run(&cr);
            g_progress = 100;
            set_status("SELF DESTRUCT COMPLETE",
                    "The loader footprint was removed");
            Sleep(1200);
            g_state = UI_CLOSING;
            PostMessage(g_hwnd, WM_CLOSE, 0, 0);
        }).detach();
    }
    RenderWorking();
}

/* ═══════════════════════════════════════════════════════════════════
 *  MAIN RENDER
 * ═══════════════════════════════════════════════════════════════════ */

static void Render() {
    ImGui::SetNextWindowSize(ImVec2((float)WIN_W, (float)WIN_H));
    ImGui::SetNextWindowPos(ImVec2(0, 0));
    ImGui::Begin("##crimson", nullptr,
            ImGuiWindowFlags_NoDecoration | ImGuiWindowFlags_NoMove);

    UIState state = g_state.load();
    switch (state) {
        case UI_INIT:
        case UI_AUTH_WORKING:
        case UI_DOWNLOAD:
        case UI_INJECTING:
            RenderWorking();
            break;
        case UI_LOGIN:
            RenderLogin();
            break;
        case UI_PROCESS_SELECT:
            RenderProcessSelect();
            break;
        case UI_SUCCESS:
            RenderSuccess();
            break;
        case UI_ERROR:
            RenderError();
            break;
        case UI_SELF_DESTRUCT:
            RenderSelfDestruct();
            break;
        case UI_CLOSING:
            break;
    }

    if (state != UI_CLOSING) {
        ImGui::SetCursorPos(ImVec2(430.f, 14.f));
        ImGui::PushFont(g_font_medium);
        bool requested_stream_mode = g_stream_mode;
        if (ImGui::Checkbox("Stream Mode", &requested_stream_mode)) {
            if (ApplyStreamMode(requested_stream_mode))
                g_stream_mode = requested_stream_mode;
        }
        ImGui::PopFont();
    }

    ImGui::End();
}

/* ═══════════════════════════════════════════════════════════════════
 *  WNDPROC
 * ═══════════════════════════════════════════════════════════════════ */

static LRESULT WINAPI WndProc(HWND hWnd, UINT msg, WPARAM wParam,
        LPARAM lParam) {
    if (ImGui_ImplWin32_WndProcHandler(hWnd, msg, wParam, lParam))
        return 0;
    switch (msg) {
        case WM_SIZE:
            if (g_device && wParam != SIZE_MINIMIZED) {
                CleanupRenderTarget();
                g_swap_chain->ResizeBuffers(0, (UINT)LOWORD(lParam),
                        (UINT)HIWORD(lParam), DXGI_FORMAT_UNKNOWN, 0);
                CreateRenderTarget();
            }
            return 0;
        case WM_SYSCOMMAND:
            if ((wParam & 0xfff0) == SC_KEYMENU) return 0;
            break;
        case WM_DESTROY:
            PostQuitMessage(0);
            return 0;
    }
    return DefWindowProcW(hWnd, msg, wParam, lParam);
}

/* ═══════════════════════════════════════════════════════════════════
 *  ENTRY POINT
 * ═══════════════════════════════════════════════════════════════════ */

int APIENTRY wWinMain(HINSTANCE hInstance, HINSTANCE, LPWSTR lpCmdLine,
        int) {
    if (!anti_debug_init()) return 0;
    /* parse command line */
    int argc = 0;
    LPWSTR *argv = CommandLineToArgvW(GetCommandLineW(), &argc);
    wchar_t dll_path[MAX_PATH]{};
    bool mode_explicit = false;

    if (argc > 1) {
        wchar_t *end = nullptr;
        if (argc == 3) {
            g_target_pid = (DWORD)wcstoul(argv[1], &end, 10);
            if (g_target_pid == 0 || end == argv[1] || *end != L'\0')
                g_target_pid = 0;
        }
        if (argc >= 2 && wcscmp(argv[argc - 1], L"--local") == 0) {
            g_remote_mode = 0;
            mode_explicit = true;
        } else if (argc >= 2 && wcscmp(argv[argc - 1], L"--remote") == 0) {
            g_remote_mode = 1;
            mode_explicit = true;
        }
    }
    if (argv) LocalFree(argv);

    /* Prefer the freshly built companion DLL when the complete local bundle
     * is launched directly.  A standalone loader keeps the authenticated VPS
     * flow, and --remote remains available as an explicit override. */
    DWORD dll_path_len = GetModuleFileNameW(nullptr, dll_path, MAX_PATH);
    if (dll_path_len == 0 || dll_path_len >= MAX_PATH) return 2;
    wchar_t *dll_name = wcsrchr(dll_path, L'\\');
    dll_name = dll_name ? dll_name + 1 : dll_path;
    wcscpy(dll_name, L"IllIIlIl.dll");
    bool companion_dll_exists =
            GetFileAttributesW(dll_path) != INVALID_FILE_ATTRIBUTES;
    if (!mode_explicit && companion_dll_exists)
        g_remote_mode = 0;

    if (!g_remote_mode) {
        if (!companion_dll_exists) {
            MessageBoxW(nullptr,
                    L"Place IllIIlIl.dll beside the Crimson loader.",
                    L"Crimson", MB_ICONERROR);
            return 2;
        }
    }

    /* create synchronization events */
    g_auth_event   = CreateEventW(nullptr, FALSE, FALSE, nullptr);
    g_select_event = CreateEventW(nullptr, FALSE, FALSE, nullptr);

    /* create window */
    WNDCLASSEXW wc{sizeof(WNDCLASSEXW), CS_CLASSDC, WndProc,
            0, 0, hInstance, nullptr, nullptr, nullptr, nullptr,
            L"CrimsonLoader", nullptr};
    RegisterClassExW(&wc);

    int sx = GetSystemMetrics(SM_CXSCREEN);
    int sy = GetSystemMetrics(SM_CYSCREEN);
    g_hwnd = CreateWindowExW(0, wc.lpszClassName, L"Crimson",
            WS_POPUP, (sx - WIN_W) / 2, (sy - WIN_H) / 2,
            WIN_W, WIN_H, nullptr, nullptr, wc.hInstance, nullptr);

    ApplyStreamMode(g_stream_mode);

    MARGINS margins{-1};
    DwmExtendFrameIntoClientArea(g_hwnd, &margins);

    if (!CreateDeviceD3D(g_hwnd)) {
        CleanupDeviceD3D();
        UnregisterClassW(wc.lpszClassName, wc.hInstance);
        return 1;
    }

    ShowWindow(g_hwnd, SW_SHOWDEFAULT);
    UpdateWindow(g_hwnd);
    anti_debug_start_sentinel();

    DeleteFileW(L"imgui.ini");
    ImGui::CreateContext();
    InitCrimsonStyle();
    ImGui_ImplWin32_Init(g_hwnd);
    ImGui_ImplDX11_Init(g_device, g_device_context);

    /* start worker thread */
    set_status("INITIALIZING", "Starting Crimson loader...");
    std::thread worker;
    if (g_remote_mode) {
        worker = std::thread(worker_thread_func);
    } else {
        worker = std::thread(worker_local_func, dll_path);
    }
    worker.detach();

    /* main loop */
    bool running = true;
    while (running) {
        MSG msg;
        while (PeekMessageW(&msg, nullptr, 0, 0, PM_REMOVE)) {
            TranslateMessage(&msg);
            DispatchMessageW(&msg);
            if (msg.message == WM_QUIT) running = false;
        }
        if (!running) break;

        ImGui_ImplDX11_NewFrame();
        ImGui_ImplWin32_NewFrame();
        ImGui::NewFrame();
        Render();
        ImGui::Render();

        float clear[4] = {0.f, 0.f, 0.f, 0.f};
        g_device_context->OMSetRenderTargets(1, &g_render_target_view,
                nullptr);
        g_device_context->ClearRenderTargetView(g_render_target_view, clear);
        ImGui_ImplDX11_RenderDrawData(ImGui::GetDrawData());
        g_swap_chain->Present(1, 0);
    }

    /* cleanup */
    anti_debug_stop();
    g_state = UI_CLOSING;
    ApplyStreamMode(false);
    if (g_auth_event)   { SetEvent(g_auth_event);   CloseHandle(g_auth_event); }
    if (g_select_event) { SetEvent(g_select_event); CloseHandle(g_select_event); }
    Sleep(200);

    ImGui_ImplDX11_Shutdown();
    ImGui_ImplWin32_Shutdown();
    ImGui::DestroyContext();
    CleanupDeviceD3D();
    DestroyWindow(g_hwnd);
    UnregisterClassW(wc.lpszClassName, wc.hInstance);
    return 0;
}

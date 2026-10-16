#ifndef WIN32_LEAN_AND_MEAN
#define WIN32_LEAN_AND_MEAN
#endif
#include <winsock2.h>
#include <ws2tcpip.h>
#include <windows.h>
#include <wincrypt.h>
#include <tlhelp32.h>

#include "obf.h"
#include "hwid.h"
#include "control_api_client.h"
#include "mapping_handoff.h"
#include "remote_session.h"
#include "loader_bootstrap.h"
#include "manual_map.h"
#include "cleanup.h"

#include <conio.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <wchar.h>

#define MAX_CANDIDATES 256
#define WINDOW_TITLE_CAPACITY 256
#define REFRESH_INTERVAL_MS 750
#define CONSOLE_WIDTH 112
#define CONSOLE_HEIGHT 31
#define CONTENT_BOX_WIDTH 58

#ifndef ENABLE_VIRTUAL_TERMINAL_PROCESSING
#define ENABLE_VIRTUAL_TERMINAL_PROCESSING 0x0004
#endif

#define CONSOLE_FOREGROUND (FOREGROUND_RED | FOREGROUND_GREEN \
        | FOREGROUND_BLUE | FOREGROUND_INTENSITY)
#define COLOR_DEFAULT CONSOLE_FOREGROUND
#define COLOR_DIM (FOREGROUND_INTENSITY)
#define COLOR_CRIMSON (FOREGROUND_RED | FOREGROUND_INTENSITY)
#define COLOR_CYAN (FOREGROUND_GREEN | FOREGROUND_BLUE | FOREGROUND_INTENSITY)
#define COLOR_MAGENTA (FOREGROUND_RED | FOREGROUND_BLUE | FOREGROUND_INTENSITY)
#define COLOR_RED (FOREGROUND_RED)
#define COLOR_GREEN (FOREGROUND_GREEN | FOREGROUND_INTENSITY)
#define COLOR_YELLOW (FOREGROUND_RED | FOREGROUND_GREEN | FOREGROUND_INTENSITY)

typedef struct process_candidate {
    DWORD process_id;
    wchar_t executable[MAX_PATH];
    wchar_t title[WINDOW_TITLE_CAPACITY];
    char client_name[32];
} process_candidate;

typedef struct window_search_context {
    process_candidate *candidates;
    size_t count;
} window_search_context;

typedef struct remote_session_handoff {
    HANDLE mapping;
    HANDLE ack_event;
    SonyVegas421RemoteSessionV2 *block;
} remote_session_handoff;

typedef struct mapping_bundle_handoff {
    HANDLE mapping;
    HANDLE ack_event;
    SonyVegas421MappingBundleV1 *block;
    size_t size;
} mapping_bundle_handoff;

typedef struct bootstrap_handoff {
    HANDLE mapping;
    HANDLE ack_event;
    SonyVegas421BootstrapV2 *block;
    SOCKET listener;
    int winsock_started;
} bootstrap_handoff;

static void set_console_color(WORD color);
static void reset_console_color(void);
static void print_banner(void);
static void print_auth_banner(void);
static void configure_console(void);
static void console_set_cursor_visible(int visible);
static void console_move(short column, short row);
static void console_write_at(short column, short row, const wchar_t *text,
        WORD color);
static void console_write_centered(short row, const wchar_t *text, WORD color);
static void ansi_write_at(short column, short row, const wchar_t *text,
        int r, int g, int b);
static void ansi_write_centered(short row, const wchar_t *text,
        int r, int g, int b);
static void console_draw_box(short top, short height, short width,
        const wchar_t *title);
static int console_prompt_auth(char *username, size_t username_capacity,
        char *license_key, size_t license_capacity, int license_only);
static void console_animate_progress(int target, const wchar_t *status,
        const wchar_t *detail);
static void console_render_result(int success, const wchar_t *title,
        const wchar_t *detail);
static int begin_bootstrap_handoff(DWORD process_id, const char *access_token,
        bootstrap_handoff *handoff);
static int finish_bootstrap_handoff(bootstrap_handoff *handoff,
        char *error_out, size_t error_capacity);
static void abandon_bootstrap_handoff(bootstrap_handoff *handoff);

static int console_progress = 0;
static wchar_t console_last_error[256];
static int vt_enabled = 0;
static char selected_client_name[32];

static void print_last_error(const wchar_t *operation) {
    DWORD error = GetLastError();
    wchar_t *message = NULL;
    FormatMessageW(FORMAT_MESSAGE_ALLOCATE_BUFFER
                    | FORMAT_MESSAGE_FROM_SYSTEM
                    | FORMAT_MESSAGE_IGNORE_INSERTS,
            NULL, error, 0, (wchar_t *)&message, 0, NULL);
    fwprintf(stderr, SV_WSTR(SV_43D4299B2CD80454, L"%ls failed (%lu): %ls\n"), operation,
            (unsigned long)error, message == NULL ? SV_WSTR(SV_475F727DB9428149, L"unknown error") : message);
    if (message != NULL) {
        LocalFree(message);
    }
}

static int is_java_process(const wchar_t *executable) {
    return _wcsicmp(executable,
                    SV_WSTR(SV_8D1BDFC4467A8CDA, L"java.exe")) == 0
            || _wcsicmp(executable,
                    SV_WSTR(SV_5FAA4D0123D44073, L"javaw.exe")) == 0;
}

static BOOL CALLBACK capture_window_title(HWND window, LPARAM parameter) {
    window_search_context *context = (window_search_context *)parameter;
    wchar_t title[WINDOW_TITLE_CAPACITY];
    DWORD process_id = 0;
    size_t index;
    if (!IsWindowVisible(window) || GetWindowTextLengthW(window) == 0) {
        return TRUE;
    }
    GetWindowThreadProcessId(window, &process_id);
    if (process_id == 0
            || GetWindowTextW(window, title, WINDOW_TITLE_CAPACITY) == 0) {
        return TRUE;
    }
    for (index = 0; index < context->count; ++index) {
        process_candidate *candidate = &context->candidates[index];
        if (candidate->process_id == process_id && candidate->title[0] == L'\0') {
            wcscpy(candidate->title, title);
            break;
        }
    }
    return TRUE;
}

static int compare_candidates(const void *left, const void *right) {
    const process_candidate *a = (const process_candidate *)left;
    const process_candidate *b = (const process_candidate *)right;
    if (a->process_id < b->process_id) return -1;
    if (a->process_id > b->process_id) return 1;
    return 0;
}

static int wide_contains_ci(const wchar_t *value, const wchar_t *needle) {
    size_t needle_length;
    const wchar_t *cursor;
    if (value == NULL || needle == NULL || needle[0] == L'\0') return 0;
    needle_length = wcslen(needle);
    for (cursor = value; *cursor != L'\0'; ++cursor) {
        if (_wcsnicmp(cursor, needle, needle_length) == 0) return 1;
    }
    return 0;
}

typedef struct process_unicode_string {
    USHORT Length;
    USHORT MaximumLength;
    PWSTR Buffer;
} process_unicode_string;

static int read_process_command_line(DWORD process_id,
        wchar_t *output, size_t capacity) {
    typedef LONG (WINAPI *nt_query_information_process_fn)(
            HANDLE, ULONG, PVOID, ULONG, PULONG);
    HMODULE ntdll;
    nt_query_information_process_fn query;
    HANDLE process;
    ULONG required = 0;
    unsigned char *buffer;
    LONG status;
    process_unicode_string *command_line;
    size_t characters;
    uintptr_t data_begin;
    uintptr_t data_end;
    uintptr_t text_begin;
    uintptr_t text_end;
    if (output == NULL || capacity == 0) return 0;
    output[0] = L'\0';
    ntdll = GetModuleHandleW(SV_WSTR(SV_7AD35ECC23F3D5FA, L"ntdll.dll"));
    query = ntdll == NULL ? NULL
            : (nt_query_information_process_fn)GetProcAddress(
                    ntdll, SV_STR(SV_80C80CF8454E2C1B, "NtQueryInformationProcess"));
    if (query == NULL) return 0;
    process = OpenProcess(PROCESS_QUERY_LIMITED_INFORMATION,
            FALSE, process_id);
    if (process == NULL) return 0;
    query(process, 60 /* ProcessCommandLineInformation */,
            NULL, 0, &required);
    if (required < sizeof(process_unicode_string)
            || required > 1024u * 1024u) {
        CloseHandle(process);
        return 0;
    }
    buffer = (unsigned char *)malloc(required + sizeof(wchar_t));
    if (buffer == NULL) {
        CloseHandle(process);
        return 0;
    }
    status = query(process, 60, buffer, required, &required);
    if (status >= 0) {
        command_line = (process_unicode_string *)buffer;
        characters = command_line->Length / sizeof(wchar_t);
        data_begin = (uintptr_t)buffer;
        data_end = data_begin + required;
        text_begin = (uintptr_t)command_line->Buffer;
        text_end = text_begin + command_line->Length;
        if (command_line->Buffer != NULL && text_begin >= data_begin
                && text_end >= text_begin && text_end <= data_end) {
            if (characters >= capacity) characters = capacity - 1;
            memcpy(output, command_line->Buffer,
                    characters * sizeof(wchar_t));
            output[characters] = L'\0';
        } else {
            status = -1;
        }
    }
    SecureZeroMemory(buffer, required);
    free(buffer);
    CloseHandle(process);
    return status >= 0;
}

static const char *detect_client_name(process_candidate *candidate) {
    int cheatbreaker = wide_contains_ci(candidate->title, SV_WSTR(SV_2A4073313BFB1439, L"cheatbreaker"));
    int badlion = wide_contains_ci(candidate->title, SV_WSTR(SV_8A1713E06062D357, L"badlion"));
    int lunar = wide_contains_ci(candidate->title, SV_WSTR(SV_F6D09644C157CD64, L"lunar"));
    HANDLE snapshot = CreateToolhelp32Snapshot(
            TH32CS_SNAPMODULE | TH32CS_SNAPMODULE32,
            candidate->process_id);
    if (snapshot != INVALID_HANDLE_VALUE) {
        MODULEENTRY32W module;
        memset(&module, 0, sizeof(module));
        module.dwSize = sizeof(module);
        if (Module32FirstW(snapshot, &module)) {
            do {
                cheatbreaker = cheatbreaker
                        || wide_contains_ci(module.szExePath, SV_WSTR(SV_2A4073313BFB1439, L"cheatbreaker"));
                badlion = badlion
                        || wide_contains_ci(module.szExePath, SV_WSTR(SV_8A1713E06062D357, L"badlion"));
                lunar = lunar
                        || wide_contains_ci(module.szExePath, SV_WSTR(SV_1EBF04C999339432, L"lunarclient"))
                        || wide_contains_ci(module.szExePath, SV_WSTR(SV_61DA071567A8D1B2, L"lunar client"));
            } while (!cheatbreaker && !badlion && !lunar
                    && Module32NextW(snapshot, &module));
        }
        CloseHandle(snapshot);
    }
    if (!cheatbreaker && !badlion && !lunar) {
        wchar_t command_line[8192];
        if (read_process_command_line(candidate->process_id,
                command_line, sizeof(command_line) / sizeof(command_line[0]))) {
            cheatbreaker = wide_contains_ci(command_line, SV_WSTR(SV_2A4073313BFB1439, L"cheatbreaker"))
                    || wide_contains_ci(command_line, SV_WSTR(SV_4F17D10CAF7C7DF9, L"1.8.9.patch"));
            badlion = wide_contains_ci(command_line, SV_WSTR(SV_8A1713E06062D357, L"badlion"));
            lunar = wide_contains_ci(command_line, SV_WSTR(SV_1EBF04C999339432, L"lunarclient"))
                    || wide_contains_ci(command_line, SV_WSTR(SV_61DA071567A8D1B2, L"lunar client"));
            SecureZeroMemory(command_line, sizeof(command_line));
        }
    }
    if (cheatbreaker) return SV_STR(SV_646D52AFC3FAEF45, "CheatBreaker");
    if (badlion) return SV_STR(SV_41F1EBE24EDC8356, "Badlion");
    if (lunar) return SV_STR(SV_A24C3BF42A150217, "Lunar");
    return SV_STR(SV_954DA354CDD00B30, "Minecraft");
}

static size_t enumerate_candidates(
        process_candidate *candidates, size_t capacity) {
    HANDLE snapshot;
    PROCESSENTRY32W entry;
    size_t count = 0;
    size_t read_index;
    size_t write_index = 0;
    window_search_context context;

    snapshot = CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0);
    if (snapshot == INVALID_HANDLE_VALUE) {
        return 0;
    }
    memset(&entry, 0, sizeof(entry));
    entry.dwSize = sizeof(entry);
    if (Process32FirstW(snapshot, &entry)) {
        do {
            if (count < capacity && is_java_process(entry.szExeFile)) {
                process_candidate *candidate = &candidates[count++];
                memset(candidate, 0, sizeof(*candidate));
                candidate->process_id = entry.th32ProcessID;
                wcsncpy(candidate->executable, entry.szExeFile, MAX_PATH - 1);
            }
        } while (Process32NextW(snapshot, &entry));
    }
    CloseHandle(snapshot);

    context.candidates = candidates;
    context.count = count;
    EnumWindows(capture_window_title, (LPARAM)&context);

    for (read_index = 0; read_index < count; ++read_index) {
        if (candidates[read_index].title[0] != L'\0') {
            if (write_index != read_index) {
                candidates[write_index] = candidates[read_index];
            }
            ++write_index;
        }
    }
    qsort(candidates, write_index, sizeof(*candidates), compare_candidates);
    for (read_index = 0; read_index < write_index; ++read_index) {
        const char *client = detect_client_name(&candidates[read_index]);
        strncpy_s(candidates[read_index].client_name,
                sizeof(candidates[read_index].client_name), client, _TRUNCATE);
    }
    return write_index;
}

static void clear_console_screen(HANDLE output) {
    CONSOLE_SCREEN_BUFFER_INFO info;
    COORD start = {0, 0};
    DWORD cells;
    DWORD written;
    if (!GetConsoleScreenBufferInfo(output, &info)) return;
    cells = (DWORD)info.dwSize.X * (DWORD)info.dwSize.Y;
    FillConsoleOutputCharacterW(output, L' ', cells, start, &written);
    FillConsoleOutputAttribute(output, info.wAttributes, cells, start, &written);
    SetConsoleCursorPosition(output, start);
}

static uint64_t candidate_list_signature(
        const process_candidate *candidates, size_t count) {
    uint64_t signature = UINT64_C(1469598103934665603);
    size_t index;
    for (index = 0; index < count; ++index) {
        const unsigned char *pid =
                (const unsigned char *)&candidates[index].process_id;
        const wchar_t *text;
        size_t byte;
        for (byte = 0; byte < sizeof(candidates[index].process_id); ++byte) {
            signature = (signature ^ pid[byte]) * UINT64_C(1099511628211);
        }
        for (text = candidates[index].executable; *text != L'\0'; ++text) {
            signature = (signature ^ (uint64_t)*text)
                    * UINT64_C(1099511628211);
        }
        for (text = candidates[index].title; *text != L'\0'; ++text) {
            signature = (signature ^ (uint64_t)*text)
                    * UINT64_C(1099511628211);
        }
        {
            const unsigned char *client =
                    (const unsigned char *)candidates[index].client_name;
            while (*client != '\0') {
                signature = (signature ^ *client++) * UINT64_C(1099511628211);
            }
        }
    }
    return (signature ^ (uint64_t)count) * UINT64_C(1099511628211);
}

static void render_selector(const process_candidate *candidates, size_t count,
        size_t selected) {
    HANDLE output = GetStdHandle(STD_OUTPUT_HANDLE);
    const short box_width = 86;
    const short left = (short)((CONSOLE_WIDTH - box_width) / 2);
    const size_t maximum_rows = 6;
    size_t first = 0;
    size_t visible_count = count > maximum_rows ? maximum_rows : count;
    size_t row;
    clear_console_screen(output);
    print_banner();
    console_draw_box(10, 17, box_width,
            SV_WSTR(SV_9316B2B4A43FFF7A, L"SELECT MINECRAFT"));
    console_write_centered(14,
            SV_WSTR(SV_3D9441ACAAC7F80E, L"UP/DOWN choose  |  ENTER inject  |  ESC cancel"),
            COLOR_DIM);
    if (count == 0) {
        console_write_centered(18,
                SV_WSTR(SV_B8C92126511BCB9F, L"Waiting for a visible Minecraft Java window..."),
                COLOR_YELLOW);
    } else {
        if (selected >= maximum_rows) first = selected - maximum_rows + 1;
        if (first + visible_count > count) first = count - visible_count;
        for (row = 0; row < visible_count; ++row) {
            size_t index = first + row;
            int active = index == selected;
            wchar_t entry[82];
            wchar_t marker[2];
            short line_row = (short)(16 + row);
            _snwprintf_s(entry,
                    sizeof(entry) / sizeof(entry[0]), _TRUNCATE,
                    SV_WSTR(SV_5ED6D12375067FE4, L"[%5lu] %-12.12S  %-50.50ls"),
                    (unsigned long)candidates[index].process_id,
                    candidates[index].client_name, candidates[index].title);
            marker[0] = active ? L'\x25B8' : L' ';
            marker[1] = L'\0';
            if (vt_enabled) {
                if (active) {
                    ansi_write_at((short)(left + 3), line_row, marker, 110, 231, 183);
                    ansi_write_at((short)(left + 5), line_row, entry, 152, 240, 200);
                } else {
                    ansi_write_at((short)(left + 3), line_row, marker, 120, 128, 140);
                    ansi_write_at((short)(left + 5), line_row, entry, 120, 128, 140);
                }
            } else {
                console_write_at((short)(left + 3), line_row, marker,
                        active ? COLOR_GREEN : COLOR_DEFAULT);
                console_write_at((short)(left + 5), line_row, entry,
                        active ? COLOR_GREEN : COLOR_DEFAULT);
            }
        }
    }
    console_write_centered(24,
            SV_WSTR(SV_CEEF1C222920AF8D, L"List refreshes automatically"),
            COLOR_DIM);
    console_set_cursor_visible(0);
    reset_console_color();
    fflush(stdout);
}

static DWORD select_process(void) {
    process_candidate candidates[MAX_CANDIDATES];
    size_t count = 0;
    size_t selected = 0;
    DWORD selected_process_id = 0;
    ULONGLONG next_refresh = 0;
    HANDLE output = GetStdHandle(STD_OUTPUT_HANDLE);
    CONSOLE_CURSOR_INFO original_cursor;
    CONSOLE_CURSOR_INFO hidden_cursor;
    int cursor_changed = 0;
    uint64_t rendered_signature = UINT64_MAX;

    if (GetConsoleCursorInfo(output, &original_cursor)) {
        hidden_cursor = original_cursor;
        hidden_cursor.bVisible = FALSE;
        cursor_changed = SetConsoleCursorInfo(output, &hidden_cursor);
    }
    for (;;) {
        ULONGLONG now = GetTickCount64();
        if (now >= next_refresh) {
            DWORD previous_id = count == 0 ? 0 : candidates[selected].process_id;
            uint64_t signature;
            size_t index;
            count = enumerate_candidates(candidates, MAX_CANDIDATES);
            selected = 0;
            if (count == 1) {
                selected_process_id = candidates[0].process_id;
                strncpy_s(selected_client_name,
                        sizeof(selected_client_name),
                        candidates[0].client_name, _TRUNCATE);
                break;
            }
            for (index = 0; index < count; ++index) {
                if (candidates[index].process_id == previous_id) {
                    selected = index;
                    break;
                }
            }
            signature = candidate_list_signature(candidates, count);
            if (signature != rendered_signature) {
                render_selector(candidates, count, selected);
                rendered_signature = signature;
            }
            next_refresh = now + REFRESH_INTERVAL_MS;
        }
        if (_kbhit()) {
            int key = _getwch();
            if (key == 0 || key == 0xe0) {
                key = _getwch();
                if (key == 72 && count != 0) {
                    selected = selected == 0 ? count - 1 : selected - 1;
                    render_selector(candidates, count, selected);
                } else if (key == 80 && count != 0) {
                    selected = (selected + 1) % count;
                    render_selector(candidates, count, selected);
                }
            } else if (key == 13 && count != 0) {
                selected_process_id = candidates[selected].process_id;
                strncpy_s(selected_client_name,
                        sizeof(selected_client_name),
                        candidates[selected].client_name, _TRUNCATE);
                break;
            } else if (key == 27) {
                break;
            }
        }
        Sleep(25);
    }
    if (cursor_changed) SetConsoleCursorInfo(output, &original_cursor);
    wprintf(SV_WSTR(SV_4F186F9A34928DB8, L"\n"));
    return selected_process_id;
}

static uintptr_t remote_module_base(DWORD process_id, const wchar_t *module_name) {
    HANDLE snapshot;
    MODULEENTRY32W entry;
    uintptr_t result = 0;
    snapshot = CreateToolhelp32Snapshot(
            TH32CS_SNAPMODULE | TH32CS_SNAPMODULE32, process_id);
    if (snapshot == INVALID_HANDLE_VALUE) {
        return 0;
    }
    memset(&entry, 0, sizeof(entry));
    entry.dwSize = sizeof(entry);
    if (Module32FirstW(snapshot, &entry)) {
        do {
            if (_wcsicmp(entry.szModule, module_name) == 0) {
                result = (uintptr_t)entry.modBaseAddr;
                break;
            }
        } while (Module32NextW(snapshot, &entry));
    }
    CloseHandle(snapshot);
    return result;
}

static uintptr_t remote_module_by_path(
        DWORD process_id, const wchar_t *module_path) {
    HANDLE snapshot;
    MODULEENTRY32W entry;
    uintptr_t result = 0;
    snapshot = CreateToolhelp32Snapshot(
            TH32CS_SNAPMODULE | TH32CS_SNAPMODULE32, process_id);
    if (snapshot == INVALID_HANDLE_VALUE) {
        return 0;
    }
    memset(&entry, 0, sizeof(entry));
    entry.dwSize = sizeof(entry);
    if (Module32FirstW(snapshot, &entry)) {
        do {
            if (_wcsicmp(entry.szExePath, module_path) == 0) {
                result = (uintptr_t)entry.modBaseAddr;
                break;
            }
        } while (Module32NextW(snapshot, &entry));
    }
    CloseHandle(snapshot);
    return result;
}

static int require_x64_target(HANDLE process) {
    typedef BOOL (WINAPI *is_wow64_process2_fn)(HANDLE, USHORT *, USHORT *);
    is_wow64_process2_fn is_wow64_process2 =
            (is_wow64_process2_fn)GetProcAddress(
                    GetModuleHandleW(SV_WSTR(SV_CAB8C0D74F95EA36, L"kernel32.dll")), SV_STR(SV_C90E0596D49615F1, "IsWow64Process2"));
    if (is_wow64_process2 != NULL) {
        USHORT process_machine = IMAGE_FILE_MACHINE_UNKNOWN;
        USHORT native_machine = IMAGE_FILE_MACHINE_UNKNOWN;
        if (!is_wow64_process2(process, &process_machine, &native_machine)) {
            return 0;
        }
        return process_machine == IMAGE_FILE_MACHINE_UNKNOWN
                && native_machine == IMAGE_FILE_MACHINE_AMD64;
    }
    {
        BOOL wow64 = FALSE;
        if (!IsWow64Process(process, &wow64)) {
            return 0;
        }
        return !wow64 && sizeof(void *) == 8;
    }
}

static int inject_library(DWORD process_id, const wchar_t *dll_path) {
    HANDLE process = NULL;
    HANDLE thread = NULL;
    LPVOID remote_path = NULL;
    HMODULE local_kernel;
    FARPROC local_load_library;
    uintptr_t remote_kernel;
    uintptr_t load_library_offset;
    LPTHREAD_START_ROUTINE remote_load_library;
    SIZE_T path_bytes = (wcslen(dll_path) + 1) * sizeof(wchar_t);
    SIZE_T written = 0;
    DWORD wait_result;
    int attempt;
    int result = 0;

    if (remote_module_by_path(process_id, dll_path) != 0) {
        return 2;
    }

    process = OpenProcess(PROCESS_CREATE_THREAD | PROCESS_QUERY_INFORMATION
                    | PROCESS_VM_OPERATION | PROCESS_VM_WRITE | PROCESS_VM_READ,
            FALSE, process_id);
    if (process == NULL) {
        print_last_error(SV_WSTR(SV_28CF9F9BB7B7FE5D, L"OpenProcess"));
        goto cleanup;
    }
    if (!require_x64_target(process)) {
        fwprintf(stderr, SV_WSTR(SV_A930A7E219FC3BFF, L"Target process is not x64; injection refused.\n"));
        goto cleanup;
    }
    remote_path = VirtualAllocEx(process, NULL, path_bytes,
            MEM_COMMIT | MEM_RESERVE, PAGE_READWRITE);
    if (remote_path == NULL) {
        print_last_error(SV_WSTR(SV_9210E4BE7E3F18CA, L"VirtualAllocEx"));
        goto cleanup;
    }
    if (!WriteProcessMemory(process, remote_path, dll_path,
            path_bytes, &written) || written != path_bytes) {
        print_last_error(SV_WSTR(SV_3198AE430590694C, L"WriteProcessMemory"));
        goto cleanup;
    }

    local_kernel = GetModuleHandleW(SV_WSTR(SV_CAB8C0D74F95EA36, L"kernel32.dll"));
    local_load_library = GetProcAddress(local_kernel, SV_STR(SV_6FF2E25F4013B44F, "LoadLibraryW"));
    remote_kernel = remote_module_base(process_id, SV_WSTR(SV_CAB8C0D74F95EA36, L"kernel32.dll"));
    if (local_kernel == NULL || local_load_library == NULL || remote_kernel == 0) {
        fwprintf(stderr, SV_WSTR(SV_464FC5EBEFEA84B2, L"Could not resolve remote kernel32!LoadLibraryW.\n"));
        goto cleanup;
    }
    load_library_offset = (uintptr_t)local_load_library - (uintptr_t)local_kernel;
    remote_load_library = (LPTHREAD_START_ROUTINE)(
            remote_kernel + load_library_offset);
    thread = CreateRemoteThread(process, NULL, 0, remote_load_library,
            remote_path, 0, NULL);
    if (thread == NULL) {
        print_last_error(SV_WSTR(SV_65F5B68654BA61F8, L"CreateRemoteThread"));
        goto cleanup;
    }
    wait_result = WaitForSingleObject(thread, 30000);
    if (wait_result != WAIT_OBJECT_0) {
        fwprintf(stderr, SV_WSTR(SV_302AE08BA3FC3B43, L"Remote LoadLibraryW did not finish within 30 seconds.\n"));
        goto cleanup;
    }
    for (attempt = 0; attempt < 100; ++attempt) {
        if (remote_module_by_path(process_id, dll_path) != 0) {
            result = 1;
            break;
        }
        Sleep(50);
    }
    if (result == 0) {
        fwprintf(stderr, SV_WSTR(SV_1B05C14829514AEC, L"%ls\n"),
                SV_WSTR(SV_182358A9DC198C0E, L"LoadLibraryW returned, but the DLL is not mapped. Inspect native log for bootstrap failure."));
        goto cleanup;
    }

cleanup:
    if (thread != NULL) CloseHandle(thread);
    if (remote_path != NULL && process != NULL) {
        VirtualFreeEx(process, remote_path, 0, MEM_RELEASE);
    }
    if (process != NULL) CloseHandle(process);
    return result;
}

static void set_console_color(WORD color) {
    HANDLE output = GetStdHandle(STD_OUTPUT_HANDLE);
    SetConsoleTextAttribute(output, color);
}

static void reset_console_color(void) {
    set_console_color(COLOR_DEFAULT);
}

static void console_set_cursor_visible(int visible) {
    HANDLE output = GetStdHandle(STD_OUTPUT_HANDLE);
    CONSOLE_CURSOR_INFO cursor;
    if (GetConsoleCursorInfo(output, &cursor)) {
        cursor.bVisible = visible ? TRUE : FALSE;
        cursor.dwSize = 18;
        SetConsoleCursorInfo(output, &cursor);
    }
}

static void console_move(short column, short row) {
    COORD position;
    position.X = column;
    position.Y = row;
    SetConsoleCursorPosition(GetStdHandle(STD_OUTPUT_HANDLE), position);
}

static void console_write_at(short column, short row, const wchar_t *text,
        WORD color) {
    HANDLE output = GetStdHandle(STD_OUTPUT_HANDLE);
    DWORD written = 0;
    if (text == NULL) return;
    console_move(column, row);
    set_console_color(color);
    WriteConsoleW(output, text, (DWORD)wcslen(text), &written, NULL);
}

static void console_write_centered(short row, const wchar_t *text,
        WORD color) {
    size_t length;
    short column;
    if (text == NULL) return;
    length = wcslen(text);
    column = length >= CONSOLE_WIDTH
            ? 0 : (short)((CONSOLE_WIDTH - length) / 2);
    console_write_at(column, row, text, color);
}

/* Snapshot an SV_WSTR pool pointer into a caller-owned buffer. Writes an empty
 * string via a char terminator (no wide literal) so the obfuscator stays happy
 * when the source is NULL. */
static void console_copy_text(wchar_t *dest, size_t capacity,
        const wchar_t *src) {
    if (dest == NULL || capacity == 0) return;
    if (src == NULL) {
        dest[0] = L'\0';
        return;
    }
    lstrcpynW(dest, src, (int)capacity);
}

static void ansi_fg_rgb(HANDLE output, int r, int g, int b) {
    wchar_t seq[32];
    DWORD written;
    _snwprintf_s(seq, sizeof(seq) / sizeof(seq[0]), _TRUNCATE,
            SV_WSTR(SV_08A29CE486C9E15C, L"\033[38;2;%d;%d;%dm"), r, g, b);
    WriteConsoleW(output, seq, (DWORD)wcslen(seq), &written, NULL);
}

static void ansi_write_at(short column, short row, const wchar_t *text,
        int r, int g, int b) {
    HANDLE output = GetStdHandle(STD_OUTPUT_HANDLE);
    DWORD written;
    if (text == NULL) return;
    console_move(column, row);
    ansi_fg_rgb(output, r, g, b);
    WriteConsoleW(output, text, (DWORD)wcslen(text), &written, NULL);
}

static void ansi_write_centered(short row, const wchar_t *text,
        int r, int g, int b) {
    size_t length;
    short column;
    if (text == NULL) return;
    length = wcslen(text);
    column = length >= CONSOLE_WIDTH
            ? 0 : (short)((CONSOLE_WIDTH - length) / 2);
    ansi_write_at(column, row, text, r, g, b);
}

static int get_monitor_refresh_rate(void) {
    DEVMODEW dm;
    memset(&dm, 0, sizeof(dm));
    dm.dmSize = sizeof(dm);
    if (EnumDisplaySettingsW(NULL, ENUM_CURRENT_SETTINGS, &dm)
            && dm.dmDisplayFrequency > 0)
        return (int)dm.dmDisplayFrequency;
    return 60;
}

static void typewrite_status(short row, const wchar_t *text) {
    HANDLE output = GetStdHandle(STD_OUTPUT_HANDLE);
    DWORD written;
    size_t length;
    size_t index;
    short column;
    if (text == NULL) return;
    length = wcslen(text);
    column = length >= CONSOLE_WIDTH
            ? 0 : (short)((CONSOLE_WIDTH - length) / 2);
    for (index = 0; index < length; ++index) {
        int factor = length > 1
                ? (int)index * 100 / ((int)length - 1) : 100;
        int r = 80 + (160 - 80) * factor / 100;
        int g = 200 - (200 - 160) * factor / 100;
        int b = 120 + (160 - 120) * factor / 100;
        console_move((short)(column + (short)index), row);
        ansi_fg_rgb(output, r, g, b);
        WriteConsoleW(output, &text[index], 1, &written, NULL);
        fflush(stdout);
        Sleep(18);
    }
    reset_console_color();
}

static void configure_console(void) {
    HANDLE output = GetStdHandle(STD_OUTPUT_HANDLE);
    HANDLE input = GetStdHandle(STD_INPUT_HANDLE);
    CONSOLE_FONT_INFOEX font;
    SMALL_RECT minimal = {0, 0, 1, 1};
    SMALL_RECT window = {0, 0, CONSOLE_WIDTH - 1, CONSOLE_HEIGHT - 1};
    COORD buffer = {CONSOLE_WIDTH, CONSOLE_HEIGHT};
    DWORD input_mode;
    HWND console_window;

    SetConsoleOutputCP(CP_UTF8);
    SetConsoleCP(CP_UTF8);

    /* Without this the whole truecolor path below is dead and every screen
     * falls back to flat white. Enabling it lights up the green banner, the
     * progress bar and the typewriter status. */
    {
        DWORD output_mode = 0;
        if (GetConsoleMode(output, &output_mode)
                && SetConsoleMode(output, output_mode
                        | ENABLE_VIRTUAL_TERMINAL_PROCESSING)) {
            vt_enabled = 1;
        }
    }

    reset_console_color();

    memset(&font, 0, sizeof(font));
    font.cbSize = sizeof(font);
    if (GetCurrentConsoleFontEx(output, FALSE, &font)) {
        font.dwFontSize.X = 0;
        font.dwFontSize.Y = 18;
        font.FontFamily = FF_DONTCARE;
        font.FontWeight = FW_NORMAL;
        wcscpy_s(font.FaceName, LF_FACESIZE,
                SV_WSTR(SV_1DF4FFEB604D6EC5, L"Consolas"));
        SetCurrentConsoleFontEx(output, FALSE, &font);
    }

    SetConsoleWindowInfo(output, TRUE, &minimal);
    SetConsoleScreenBufferSize(output, buffer);
    SetConsoleWindowInfo(output, TRUE, &window);
    clear_console_screen(output);

    if (GetConsoleMode(input, &input_mode)) {
        input_mode |= ENABLE_EXTENDED_FLAGS;
        input_mode &= ~ENABLE_QUICK_EDIT_MODE;
        SetConsoleMode(input, input_mode);
    }

    console_window = GetConsoleWindow();
    if (console_window != NULL) {
        RECT console_rect;
        MONITORINFO monitor;
        HMONITOR monitor_handle = MonitorFromWindow(
                console_window, MONITOR_DEFAULTTONEAREST);
        memset(&monitor, 0, sizeof(monitor));
        monitor.cbSize = sizeof(monitor);
        if (GetWindowRect(console_window, &console_rect)
                && GetMonitorInfoW(monitor_handle, &monitor)) {
            int width = console_rect.right - console_rect.left;
            int height = console_rect.bottom - console_rect.top;
            int x = monitor.rcWork.left
                    + (monitor.rcWork.right - monitor.rcWork.left - width) / 2;
            int y = monitor.rcWork.top
                    + (monitor.rcWork.bottom - monitor.rcWork.top - height) / 2;
            SetWindowPos(console_window, NULL, x, y, 0, 0,
                    SWP_NOACTIVATE | SWP_NOSIZE | SWP_NOZORDER);
        }
        ShowScrollBar(console_window, SB_BOTH, FALSE);
        {
            LONG style = GetWindowLongW(console_window, GWL_STYLE);
            style &= ~(WS_MAXIMIZEBOX | WS_SIZEBOX);
            SetWindowLongW(console_window, GWL_STYLE, style);
            SetWindowPos(console_window, NULL, 0, 0, 0, 0,
                    SWP_NOACTIVATE | SWP_NOMOVE | SWP_NOSIZE
                    | SWP_NOZORDER | SWP_FRAMECHANGED);
        }
    }

    console_set_cursor_visible(0);
    clear_console_screen(output);
}

/* Slate border, drawn truecolor when the terminal supports it. Box-drawing
 * glyphs are character constants, so they need no obfuscated string pool. */
static void box_line(short column, short row, const wchar_t *text) {
    if (vt_enabled) {
        ansi_write_at(column, row, text, 70, 84, 110);
    } else {
        console_write_at(column, row, text, COLOR_DIM);
    }
}

static void console_draw_box(short top, short height, short width,
        const wchar_t *title) {
    wchar_t line[CONSOLE_WIDTH + 1];
    wchar_t bar[2];
    wchar_t title_copy[128];
    short left = (short)((CONSOLE_WIDTH - width) / 2);
    short row;
    short index;

    if (width < 4 || width > CONSOLE_WIDTH || height < 3) return;
    console_copy_text(title_copy, sizeof(title_copy) / sizeof(title_copy[0]), title);

    line[0] = L'\x256D';
    for (index = 1; index < width - 1; ++index) line[index] = L'\x2500';
    line[width - 1] = L'\x256E';
    line[width] = L'\0';
    box_line(left, top, line);
    line[0] = L'\x2570';
    line[width - 1] = L'\x256F';
    box_line(left, (short)(top + height - 1), line);

    bar[0] = L'\x2502';
    bar[1] = L'\0';
    for (row = (short)(top + 1); row < top + height - 1; ++row) {
        box_line(left, row, bar);
        box_line((short)(left + width - 1), row, bar);
    }
    if (title_copy[0] != L'\0') {
        if (vt_enabled) {
            ansi_write_centered((short)(top + 2), title_copy, 110, 231, 183);
        } else {
            console_write_centered((short)(top + 2), title_copy, COLOR_CRIMSON);
        }
    }
    reset_console_color();
}

/* Emerald gradient down the six banner rows, dimmed by brightness (0..100).
 * The fade-in ramps brightness; a static draw passes 100. */
static void banner_row_color(int index, int brightness,
        int *r, int *g, int *b) {
    static const int top[3] = {110, 231, 183};
    static const int bottom[3] = {16, 185, 129};
    int fraction = index * 100 / 7;
    if (brightness < 0) brightness = 0;
    if (brightness > 100) brightness = 100;
    *r = (top[0] + (bottom[0] - top[0]) * fraction / 100) * brightness / 100;
    *g = (top[1] + (bottom[1] - top[1]) * fraction / 100) * brightness / 100;
    *b = (top[2] + (bottom[2] - top[2]) * fraction / 100) * brightness / 100;
}

static void print_banner_at(int brightness) {
    const wchar_t *raw[8];
    wchar_t copies[8][128];
    int index;

    raw[0] = SV_WSTR(SV_7A85DB7F26781920, L" \u2588\u2588\u2588\u2588\u2588  \u2588\u2588\u2588\u2588\u2588 \u2588\u2588\u2588\u2588\u2588\u2588   \u2588\u2588\u2588\u2588\u2588 \u2588\u2588\u2588\u2588\u2588   \u2588\u2588\u2588\u2588 \u2588\u2588\u2588\u2588\u2588\u2588   \u2588\u2588\u2588\u2588\u2588    \u2588\u2588\u2588\u2588\u2588\u2588\u2588    \u2588\u2588\u2588\u2588\u2588   \u2588\u2588\u2588   \u2588\u2588\u2588\u2588\u2588 \u2588\u2588\u2588\u2588\u2588\u2588   \u2588\u2588\u2588\u2588\u2588");
    raw[1] = SV_WSTR(SV_20DA9808C51BE614, L"\u2592\u2592\u2588\u2588\u2588  \u2592\u2592\u2588\u2588\u2588 \u2592\u2592\u2588\u2588\u2588\u2588\u2588\u2588 \u2592\u2592\u2588\u2588\u2588 \u2592\u2592\u2588\u2588\u2588   \u2588\u2588\u2588\u2592 \u2592\u2592\u2588\u2588\u2588\u2588\u2588\u2588 \u2592\u2592\u2588\u2588\u2588   \u2588\u2588\u2588\u2592\u2592\u2592\u2592\u2592\u2588\u2588\u2588 \u2592\u2592\u2588\u2588\u2588   \u2592\u2588\u2588\u2588  \u2592\u2592\u2588\u2588\u2588 \u2592\u2592\u2588\u2588\u2588\u2588\u2588\u2588 \u2592\u2592\u2588\u2588\u2588 ");
    raw[2] = SV_WSTR(SV_25B6917A19764104, L" \u2592\u2588\u2588\u2588   \u2592\u2588\u2588\u2588  \u2592\u2588\u2588\u2588\u2592\u2588\u2588\u2588 \u2592\u2588\u2588\u2588  \u2592\u2588\u2588\u2588  \u2588\u2588\u2588    \u2592\u2588\u2588\u2588\u2592\u2588\u2588\u2588 \u2592\u2588\u2588\u2588  \u2588\u2588\u2588     \u2592\u2592\u2588\u2588\u2588 \u2592\u2588\u2588\u2588   \u2592\u2588\u2588\u2588   \u2592\u2588\u2588\u2588  \u2592\u2588\u2588\u2588\u2592\u2588\u2588\u2588 \u2592\u2588\u2588\u2588 ");
    raw[3] = SV_WSTR(SV_CD1B901671F247B7, L" \u2592\u2588\u2588\u2588   \u2592\u2588\u2588\u2588  \u2592\u2588\u2588\u2588\u2592\u2592\u2588\u2588\u2588\u2592\u2588\u2588\u2588  \u2592\u2588\u2588\u2588\u2588\u2588\u2588\u2588     \u2592\u2588\u2588\u2588\u2592\u2592\u2588\u2588\u2588\u2592\u2588\u2588\u2588 \u2592\u2588\u2588\u2588      \u2592\u2588\u2588\u2588 \u2592\u2588\u2588\u2588   \u2592\u2588\u2588\u2588   \u2592\u2588\u2588\u2588  \u2592\u2588\u2588\u2588\u2592\u2592\u2588\u2588\u2588\u2592\u2588\u2588\u2588 ");
    raw[4] = SV_WSTR(SV_0B637B1823D177F1, L" \u2592\u2588\u2588\u2588   \u2592\u2588\u2588\u2588  \u2592\u2588\u2588\u2588 \u2592\u2592\u2588\u2588\u2588\u2588\u2588\u2588  \u2592\u2588\u2588\u2588\u2592\u2592\u2588\u2588\u2588    \u2592\u2588\u2588\u2588 \u2592\u2592\u2588\u2588\u2588\u2588\u2588\u2588 \u2592\u2588\u2588\u2588      \u2592\u2588\u2588\u2588 \u2592\u2592\u2588\u2588\u2588  \u2588\u2588\u2588\u2588\u2588  \u2588\u2588\u2588   \u2592\u2588\u2588\u2588 \u2592\u2592\u2588\u2588\u2588\u2588\u2588\u2588 ");
    raw[5] = SV_WSTR(SV_D4F271600A7AC45F, L" \u2592\u2588\u2588\u2588   \u2592\u2588\u2588\u2588  \u2592\u2588\u2588\u2588  \u2592\u2592\u2588\u2588\u2588\u2588\u2588  \u2592\u2588\u2588\u2588 \u2592\u2592\u2588\u2588\u2588   \u2592\u2588\u2588\u2588  \u2592\u2592\u2588\u2588\u2588\u2588\u2588 \u2592\u2592\u2588\u2588\u2588     \u2588\u2588\u2588   \u2592\u2592\u2592\u2588\u2588\u2588\u2588\u2588\u2592\u2588\u2588\u2588\u2588\u2588\u2592    \u2592\u2588\u2588\u2588  \u2592\u2592\u2588\u2588\u2588\u2588\u2588 ");
    raw[6] = SV_WSTR(SV_1C2B793E513F88ED, L" \u2592\u2592\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588   \u2588\u2588\u2588\u2588\u2588  \u2592\u2592\u2588\u2588\u2588\u2588\u2588 \u2588\u2588\u2588\u2588\u2588 \u2592\u2592\u2588\u2588\u2588\u2588 \u2588\u2588\u2588\u2588\u2588  \u2592\u2592\u2588\u2588\u2588\u2588\u2588 \u2592\u2592\u2592\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2592      \u2592\u2592\u2588\u2588\u2588 \u2592\u2592\u2588\u2588\u2588      \u2588\u2588\u2588\u2588\u2588  \u2592\u2592\u2588\u2588\u2588\u2588\u2588");
    raw[7] = SV_WSTR(SV_23F3EC2791576815, L"  \u2592\u2592\u2592\u2592\u2592\u2592\u2592\u2592   \u2592\u2592\u2592\u2592\u2592    \u2592\u2592\u2592\u2592\u2592 \u2592\u2592\u2592\u2592\u2592   \u2592\u2592\u2592\u2592 \u2592\u2592\u2592\u2592\u2592    \u2592\u2592\u2592\u2592\u2592    \u2592\u2592\u2592\u2592\u2592\u2592\u2592         \u2592\u2592\u2592   \u2592\u2592\u2592      \u2592\u2592\u2592\u2592\u2592    \u2592\u2592\u2592\u2592\u2592 ");
    for (index = 0; index < 8; ++index) {
        console_copy_text(copies[index], 128, raw[index]);
        if (vt_enabled) {
            int r, g, b;
            banner_row_color(index, brightness, &r, &g, &b);
            ansi_write_centered((short)(1 + index), copies[index], r, g, b);
        } else {
            console_write_centered((short)(1 + index), copies[index],
                    COLOR_DEFAULT);
        }
    }
    reset_console_color();
}

static void print_banner(void) {
    print_banner_at(100);
}

static void print_auth_banner(void) {
    print_banner();
}

static void animate_banner_fade_in(void) {
    int refresh;
    int frames;
    int frame;
    if (!vt_enabled) {
        print_banner();
        fflush(stdout);
        return;
    }
    refresh = get_monitor_refresh_rate();
    frames = refresh * 45 / 100;
    if (frames < 12) frames = 12;
    for (frame = 0; frame <= frames; ++frame) {
        int position = frame * 100 / frames;
        int eased = 100 - (100 - position) * (100 - position) / 100;
        print_banner_at(eased);
        fflush(stdout);
        if (frame != frames) Sleep((DWORD)(1000 / refresh));
    }
}

static void console_render_input(short column, short row,
        const char *buffer, size_t length, int secret, short width) {
    wchar_t visible[64];
    wchar_t spaces[64];
    size_t start = length > (size_t)width ? length - (size_t)width : 0;
    size_t count = length - start;
    size_t index;

    if (width >= (short)(sizeof(visible) / sizeof(visible[0]))) {
        width = (short)(sizeof(visible) / sizeof(visible[0]) - 1);
    }
    for (index = 0; index < (size_t)width; ++index) spaces[index] = L' ';
    spaces[width] = L'\0';
    console_write_at(column, row, spaces, COLOR_DEFAULT);

    for (index = 0; index < count; ++index) {
        visible[index] = secret ? L'*' : (wchar_t)(unsigned char)buffer[start + index];
    }
    visible[count] = L'\0';
    console_write_at(column, row, visible, COLOR_GREEN);
    console_move((short)(column + count), row);
}

static int console_paste_field(char *buffer, size_t capacity,
        size_t *length) {
    HANDLE clipboard_data = NULL;
    const wchar_t *clipboard_text = NULL;
    const wchar_t *cursor;
    size_t original_length;
    int pasted = 0;

    if (buffer == NULL || length == NULL || *length + 1 >= capacity
            || !OpenClipboard(GetConsoleWindow())) {
        return 0;
    }
    clipboard_data = GetClipboardData(CF_UNICODETEXT);
    if (clipboard_data != NULL) {
        clipboard_text = (const wchar_t *)GlobalLock(clipboard_data);
    }
    if (clipboard_text == NULL) {
        CloseClipboard();
        return 0;
    }

    original_length = *length;
    cursor = clipboard_text;
    while (*cursor == L' ' || *cursor == L'\t'
            || *cursor == L'\r' || *cursor == L'\n') {
        ++cursor;
    }
    while (*cursor != L'\0' && *cursor != L'\r' && *cursor != L'\n'
            && *length + 1 < capacity) {
        if (*cursor >= 32 && *cursor <= 126) {
            buffer[(*length)++] = (char)*cursor;
            pasted = 1;
        }
        ++cursor;
    }
    while (*length > original_length && buffer[*length - 1] == ' ') {
        --(*length);
    }
    buffer[*length] = '\0';
    GlobalUnlock(clipboard_data);
    CloseClipboard();
    return pasted && *length > original_length;
}

static int console_read_field(short column, short row, char *buffer,
        size_t capacity, int secret, short visible_width) {
    size_t length = 0;
    int key;
    buffer[0] = '\0';
    console_set_cursor_visible(1);
    console_render_input(column, row, buffer, length, secret, visible_width);
    for (;;) {
        key = _getwch();
        if (key == 0 || key == 0xe0) {
            int extended_key = _getwch();
            if (extended_key == 82 && (GetKeyState(VK_SHIFT) & 0x8000)
                    && console_paste_field(buffer, capacity, &length)) {
                console_render_input(column, row, buffer, length, secret,
                        visible_width);
            }
            continue;
        }
        if (key == 22) {
            if (console_paste_field(buffer, capacity, &length)) {
                console_render_input(column, row, buffer, length, secret,
                        visible_width);
            }
            continue;
        }
        if (key == 27) {
            buffer[0] = '\0';
            console_set_cursor_visible(0);
            return 0;
        }
        if (key == '\r' || key == '\n') {
            if (length != 0) {
                console_set_cursor_visible(0);
                return 1;
            }
            continue;
        }
        if ((key == '\b' || key == 127) && length > 0) {
            buffer[--length] = '\0';
        } else if (key >= 32 && key <= 126 && length + 1 < capacity) {
            buffer[length++] = (char)key;
            buffer[length] = '\0';
        } else {
            continue;
        }
        console_render_input(column, row, buffer, length, secret, visible_width);
    }
}

static int console_prompt_auth(char *username, size_t username_capacity,
        char *license_key, size_t license_capacity, int mode) {
    HANDLE output = GetStdHandle(STD_OUTPUT_HANDLE);
    short left = (short)((CONSOLE_WIDTH - CONTENT_BOX_WIDTH) / 2);
    short value_column = (short)(left + 12);
    short visible_width = (short)(CONTENT_BOX_WIDTH - 16);

    clear_console_screen(output);
    print_auth_banner();
    console_write_at((short)(left + 6), 20,
            SV_WSTR(SV_A3706B352CE27640, L"user:"), COLOR_DEFAULT);
    if (mode != 2) {
        console_write_at((short)(left + 6), 22,
                SV_WSTR(SV_9B096AAB3A4913AB, L"license:"), COLOR_DEFAULT);
    }
    console_write_centered(26,
            SV_WSTR(SV_6EEE849B9181923C, L"ENTER confirm  |  CTRL+V paste  |  ESC cancel"),
            COLOR_DIM);

    if (mode != 1) {
        console_write_at((short)(left + 3), 20,
                SV_WSTR(SV_8D798BBE6FF1FE12, L">"), COLOR_GREEN);
        if (!console_read_field(value_column, 20, username,
                username_capacity, 0, visible_width)) return 0;
    } else if (username != NULL && username[0] != '\0') {
        console_render_input(value_column, 20, username, strlen(username),
                0, visible_width);
    }

    console_write_at((short)(left + 3), 20,
            SV_WSTR(SV_0396013420940B12, L" "), COLOR_DEFAULT);
    if (mode == 2) return 1;
    console_write_at((short)(left + 3), 22,
            SV_WSTR(SV_8D798BBE6FF1FE12, L">"), COLOR_GREEN);
    if (!console_read_field(value_column, 22, license_key,
            license_capacity, 1, visible_width)) return 0;
    console_write_at((short)(left + 3), 22,
            SV_WSTR(SV_0396013420940B12, L" "), COLOR_DEFAULT);
    return 1;
}

static void console_draw_progress_bar(int percent) {
    HANDLE output = GetStdHandle(STD_OUTPUT_HANDLE);
    const short width = 50;
    const short left = (short)((CONSOLE_WIDTH - (width + 7)) / 2);
    int filled = (percent * width + 99) / 100;
    int index;
    wchar_t percent_text[8];
    DWORD written;
    wchar_t bracket_open = L'[';
    wchar_t bracket_close = L']';

    console_move(left, 18);
    if (vt_enabled) {
        ansi_fg_rgb(output, 140, 140, 150);
        WriteConsoleW(output, &bracket_open, 1, &written, NULL);
        for (index = 0; index < width; ++index) {
            wchar_t ch = index < filled ? L'\x2588' : L'\x2500';
            if (index < filled)
                ansi_fg_rgb(output, 80, 200, 120);
            else
                ansi_fg_rgb(output, 50, 50, 60);
            WriteConsoleW(output, &ch, 1, &written, NULL);
        }
        ansi_fg_rgb(output, 140, 140, 150);
        WriteConsoleW(output, &bracket_close, 1, &written, NULL);
    } else {
        console_write_at(left, 18, SV_WSTR(SV_BCA6FA71A490EA05, L"["),
                COLOR_DEFAULT);
        for (index = 0; index < width; ++index) {
            wchar_t character = index < filled ? L'=' : L'-';
            WORD color = index < filled ? COLOR_CRIMSON : COLOR_DIM;
            console_move((short)(left + 1 + index), 18);
            set_console_color(color);
            WriteConsoleW(output, &character, 1, &written, NULL);
        }
        console_write_at((short)(left + width + 1), 18,
                SV_WSTR(SV_B3FFB663FB6F0CEA, L"]"), COLOR_DEFAULT);
    }
    _snwprintf_s(percent_text,
            sizeof(percent_text) / sizeof(percent_text[0]), _TRUNCATE,
            SV_WSTR(SV_351B7145BE9EF8E6, L" %3d%%"), percent);
    if (vt_enabled) {
        ansi_fg_rgb(output, 200, 200, 210);
        WriteConsoleW(output, percent_text, (DWORD)wcslen(percent_text),
                &written, NULL);
    } else {
        console_write_at((short)(left + width + 2), 18,
                percent_text, COLOR_DEFAULT);
    }
    reset_console_color();
    fflush(stdout);
}

static void console_animate_progress(int target, const wchar_t *status,
        const wchar_t *detail) {
    HANDLE output = GetStdHandle(STD_OUTPUT_HANDLE);
    int start = console_progress;
    int frame;
    int total_frames;
    int refresh;
    double frame_ms;
    wchar_t status_copy[128];
    wchar_t detail_copy[256];

    console_copy_text(status_copy, sizeof(status_copy) / sizeof(status_copy[0]), status);
    console_copy_text(detail_copy, sizeof(detail_copy) / sizeof(detail_copy[0]), detail);

    if (target < 0) target = 0;
    if (target > 100) target = 100;
    if (target < start) start = 0;

    console_set_cursor_visible(0);
    clear_console_screen(output);
    print_banner();

    if (vt_enabled) {
        typewrite_status(15, status_copy);
        console_write_centered(16, detail_copy, COLOR_DIM);

        refresh = get_monitor_refresh_rate();
        frame_ms = 1000.0 / (double)refresh;
        total_frames = (int)(500.0 / frame_ms);
        if (total_frames < 8) total_frames = 8;

        for (frame = 0; frame <= total_frames; ++frame) {
            double position = (double)frame / (double)total_frames;
            double eased = 1.0 - (1.0 - position) * (1.0 - position)
                    * (1.0 - position);
            int percent = start + (int)((target - start) * eased + 0.5);
            console_draw_progress_bar(percent);
            if (frame != total_frames) Sleep((DWORD)frame_ms);
        }
    } else {
        console_write_centered(15, status_copy, COLOR_DEFAULT);
        console_write_centered(16, detail_copy, COLOR_DIM);

        total_frames = 18;
        for (frame = 0; frame <= total_frames; ++frame) {
            double position = (double)frame / (double)total_frames;
            double eased = 1.0 - (1.0 - position) * (1.0 - position)
                    * (1.0 - position);
            int percent = start + (int)((target - start) * eased + 0.5);
            console_draw_progress_bar(percent);
            if (frame != total_frames) Sleep(16);
        }
    }
    console_progress = target;
    reset_console_color();
    Sleep(300);
}

static void console_render_result(int success, const wchar_t *title,
        const wchar_t *detail) {
    HANDLE output = GetStdHandle(STD_OUTPUT_HANDLE);
    wchar_t title_copy[128];
    wchar_t detail_copy[256];
    wchar_t hint_copy[128];

    console_copy_text(title_copy, sizeof(title_copy) / sizeof(title_copy[0]), title);
    console_copy_text(detail_copy, sizeof(detail_copy) / sizeof(detail_copy[0]), detail);
    console_copy_text(hint_copy, sizeof(hint_copy) / sizeof(hint_copy[0]),
            success
                    ? SV_WSTR(SV_365DEB2820E1CDFE, L"F6 - self destruct")
                    : SV_WSTR(SV_478EBC14E04629BA, L"Press any key to close"));

    clear_console_screen(output);
    print_banner();

    if (vt_enabled) {
        if (success)
            ansi_write_centered(15, title_copy, 80, 200, 120);
        else
            ansi_write_centered(15, title_copy, 220, 20, 60);
        console_write_centered(16, detail_copy, COLOR_DEFAULT);
        console_write_centered(18, hint_copy, COLOR_DIM);
    } else {
        console_write_centered(15, title_copy,
                success ? COLOR_GREEN : COLOR_CRIMSON);
        console_write_centered(16, detail_copy, COLOR_DEFAULT);
        console_write_centered(18, hint_copy, COLOR_DIM);
    }
    console_set_cursor_visible(0);
    fflush(stdout);
}

static void console_set_error_utf8(const char *message) {
    int converted;
    console_last_error[0] = L'\0';
    if (message == NULL || message[0] == '\0') return;
    converted = MultiByteToWideChar(CP_UTF8, 0, message, -1,
            console_last_error,
            (int)(sizeof(console_last_error) / sizeof(console_last_error[0])));
    if (converted == 0) {
        MultiByteToWideChar(CP_ACP, 0, message, -1,
                console_last_error,
                (int)(sizeof(console_last_error) / sizeof(console_last_error[0])));
    }
}

static void console_set_error(const wchar_t *message) {
    wcsncpy_s(console_last_error,
            sizeof(console_last_error) / sizeof(console_last_error[0]),
            message == NULL ? SV_WSTR(SV_0E35A61F15F724AD, L"Unknown error") : message,
            _TRUNCATE);
}

static void console_set_api_connection_error(void) {
    wchar_t message[128];
    unsigned long transport_error = control_api_last_transport_error();
    unsigned long http_status = control_api_last_http_status();
    if (transport_error != 0) {
        swprintf_s(message, sizeof(message) / sizeof(message[0]),
                SV_WSTR(SV_8EBC482733C09015, L"API: erro de transporte 0x%08lX"), transport_error);
    } else if (http_status != 0) {
        swprintf_s(message, sizeof(message) / sizeof(message[0]),
                SV_WSTR(SV_DF94919490C1DA20, L"API: resposta HTTP %lu"), http_status);
    } else {
        swprintf_s(message, sizeof(message) / sizeof(message[0]),
                SV_WSTR(SV_87CAC69A44AE235F, L"API: resposta invalida ou vazia"));
    }
    console_set_error(message);
}

static void render_injected_console(DWORD process_id, int already_active) {
    HANDLE output = GetStdHandle(STD_OUTPUT_HANDLE);
    (void)process_id;
    (void)already_active;
    clear_console_screen(output);
    print_banner();
    console_write_centered(17,
            SV_WSTR(SV_23FD027C4A3F3B10, L"press f6 for self destruct"), COLOR_DEFAULT);
    console_write_centered(28, SV_WSTR(SV_D56778D12B3345A1, L"made by lazer"), COLOR_DEFAULT);
    console_set_cursor_visible(0);
    reset_console_color();
    fflush(stdout);
}

static int wait_for_self_destruct_key(void) {
    HANDLE input = GetStdHandle(STD_INPUT_HANDLE);
    DWORD mode;
    INPUT_RECORD record;
    DWORD read;

    if (input != INVALID_HANDLE_VALUE && GetConsoleMode(input, &mode)) {
        FlushConsoleInputBuffer(input);
        for (;;) {
            if (!ReadConsoleInputW(input, &record, 1, &read)) break;
            if (read == 1 && record.EventType == KEY_EVENT
                    && record.Event.KeyEvent.bKeyDown
                    && record.Event.KeyEvent.wVirtualKeyCode == VK_F6) {
                return 1;
            }
        }
    }

    /* Fallback for hosts that do not expose a classic console input buffer. */
    for (;;) {
        if ((GetAsyncKeyState(VK_F6) & 1) != 0) return 1;
        Sleep(25);
    }
}

static void remote_session_object_name(wchar_t *output, size_t capacity,
        const wchar_t *kind, DWORD process_id) {
    _snwprintf_s(output, capacity, _TRUNCATE,
            SV_WSTR(SV_2857277F3A8D050C, L"Local\\IllIIlIl.%ls.%lu"), kind, process_id);
}

static int protect_remote_username(const char *username, const char *token,
        unsigned char *output, DWORD output_capacity, DWORD *output_size) {
    DATA_BLOB plain;
    DATA_BLOB entropy;
    DATA_BLOB protected_blob;
    size_t username_length;
    size_t token_length;
    int result = 0;
    if (username == NULL || token == NULL || output == NULL
            || output_size == NULL || username[0] == '\0' || token[0] == '\0') {
        return 0;
    }
    username_length = strlen(username) + 1;
    token_length = strlen(token);
    if (username_length > 96 || token_length > 255) return 0;
    plain.cbData = (DWORD)username_length;
    plain.pbData = (BYTE *)username;
    entropy.cbData = (DWORD)token_length;
    entropy.pbData = (BYTE *)token;
    memset(&protected_blob, 0, sizeof(protected_blob));
    if (CryptProtectData(&plain, NULL, &entropy, NULL, NULL,
            CRYPTPROTECT_UI_FORBIDDEN, &protected_blob)) {
        if (protected_blob.cbData > 0
                && protected_blob.cbData <= output_capacity) {
            memcpy(output, protected_blob.pbData, protected_blob.cbData);
            *output_size = protected_blob.cbData;
            result = 1;
        }
        if (protected_blob.pbData != NULL) {
            SecureZeroMemory(protected_blob.pbData, protected_blob.cbData);
            LocalFree(protected_blob.pbData);
        }
    }
    return result;
}

static int begin_remote_session_handoff(DWORD process_id, const char *token,
        const char *username, const char *role,
        remote_session_handoff *handoff) {
    wchar_t mapping_name[96];
    wchar_t event_name[96];
    char identity[96];
    unsigned char protected_username[512];
    DWORD protected_username_size = 0;
    memset(handoff, 0, sizeof(*handoff));
    memset(identity, 0, sizeof(identity));
    memset(protected_username, 0, sizeof(protected_username));
    _snprintf_s(identity, sizeof(identity), _TRUNCATE, SV_STR(SV_1B69400343565B7E, "%s\t%s"), username,
            role != NULL && _stricmp(role, SV_STR(SV_F6CECEC39741D5A6, "developer")) == 0
                    ? SV_STR(SV_F6CECEC39741D5A6, "developer") : SV_STR(SV_0797C8FD1A0B831A, "user"));
    if (token == NULL || token[0] == '\0'
            || strlen(token) >= sizeof(handoff->block->control_token)
            || !protect_remote_username(identity, token, protected_username,
                    sizeof(protected_username), &protected_username_size)) {
        SecureZeroMemory(identity, sizeof(identity));
        return 0;
    }
    SecureZeroMemory(identity, sizeof(identity));
    remote_session_object_name(mapping_name,
            sizeof(mapping_name) / sizeof(mapping_name[0]),
            SV_WSTR(SV_4F515C6A19269377, L"RemoteSession"), process_id);
    remote_session_object_name(event_name,
            sizeof(event_name) / sizeof(event_name[0]),
            SV_WSTR(SV_CCD49B5F8B3DC864, L"RemoteSessionAck"), process_id);
    handoff->mapping = CreateFileMappingW(INVALID_HANDLE_VALUE, NULL,
            PAGE_READWRITE, 0, sizeof(SonyVegas421RemoteSessionV2),
            mapping_name);
    if (handoff->mapping == NULL) {
        SecureZeroMemory(protected_username, sizeof(protected_username));
        return 0;
    }
    handoff->block = (SonyVegas421RemoteSessionV2 *)MapViewOfFile(
            handoff->mapping, FILE_MAP_READ | FILE_MAP_WRITE,
            0, 0, sizeof(SonyVegas421RemoteSessionV2));
    if (handoff->block == NULL) {
        SecureZeroMemory(protected_username, sizeof(protected_username));
        CloseHandle(handoff->mapping);
        memset(handoff, 0, sizeof(*handoff));
        return 0;
    }
    handoff->ack_event = CreateEventW(NULL, FALSE, FALSE, event_name);
    if (handoff->ack_event == NULL) {
        SecureZeroMemory(protected_username, sizeof(protected_username));
        UnmapViewOfFile(handoff->block);
        CloseHandle(handoff->mapping);
        memset(handoff, 0, sizeof(*handoff));
        return 0;
    }
    memset(handoff->block, 0, sizeof(*handoff->block));
    handoff->block->magic = SONYVEGAS421_REMOTE_SESSION_MAGIC;
    handoff->block->version = SONYVEGAS421_REMOTE_SESSION_VERSION;
    handoff->block->structure_size = sizeof(*handoff->block);
    handoff->block->target_pid = process_id;
    handoff->block->status = SONYVEGAS421_REMOTE_SESSION_CREATED;
    strcpy_s(handoff->block->control_token,
            sizeof(handoff->block->control_token), token);
    handoff->block->protected_username_size = protected_username_size;
    memcpy(handoff->block->protected_username, protected_username,
            protected_username_size);
    SecureZeroMemory(protected_username, sizeof(protected_username));
    return 1;
}

static int finish_remote_session_handoff(remote_session_handoff *handoff) {
    int consumed = 0;
    if (handoff->ack_event != NULL) {
        WaitForSingleObject(handoff->ack_event, 5000);
    }
    if (handoff->block != NULL) {
        consumed = handoff->block->status
                == SONYVEGAS421_REMOTE_SESSION_CONSUMED;
        SecureZeroMemory(handoff->block->control_token,
                sizeof(handoff->block->control_token));
        SecureZeroMemory(handoff->block->protected_username,
                sizeof(handoff->block->protected_username));
        handoff->block->protected_username_size = 0;
        UnmapViewOfFile(handoff->block);
    }
    if (handoff->ack_event != NULL) CloseHandle(handoff->ack_event);
    if (handoff->mapping != NULL) CloseHandle(handoff->mapping);
    memset(handoff, 0, sizeof(*handoff));
    return consumed;
}

static int begin_mapping_bundle_handoff(DWORD process_id,
        mapping_bundle_handoff *handoff) {
    wchar_t mapping_name[96];
    wchar_t event_name[96];
    const unsigned char *bundle_data;
    size_t bundle_size = 0;
    const SonyVegas421MappingBundleV1 *source;
    memset(handoff, 0, sizeof(*handoff));
    bundle_data = control_api_cheatbreaker_mapping_bundle(&bundle_size);
    if (bundle_data == NULL || bundle_size < sizeof(*source)
            || bundle_size > SONYVEGAS421_MAPPING_BUNDLE_MAX_SIZE) {
        return 0;
    }
    source = (const SonyVegas421MappingBundleV1 *)bundle_data;
    if (source->magic != SONYVEGAS421_MAPPING_BUNDLE_MAGIC
            || source->version != SONYVEGAS421_MAPPING_BUNDLE_VERSION
            || source->header_size != sizeof(*source)
            || source->total_size != bundle_size) {
        return 0;
    }
    remote_session_object_name(mapping_name,
            sizeof(mapping_name) / sizeof(mapping_name[0]),
            SV_WSTR(SV_8760DF297DF76B44, L"MappingBundle"), process_id);
    remote_session_object_name(event_name,
            sizeof(event_name) / sizeof(event_name[0]),
            SV_WSTR(SV_439AC3929B29C9F2, L"MappingBundleAck"), process_id);
    handoff->mapping = CreateFileMappingW(INVALID_HANDLE_VALUE, NULL,
            PAGE_READWRITE, 0, (DWORD)bundle_size, mapping_name);
    if (handoff->mapping == NULL) return 0;
    handoff->block = (SonyVegas421MappingBundleV1 *)MapViewOfFile(
            handoff->mapping, FILE_MAP_READ | FILE_MAP_WRITE,
            0, 0, bundle_size);
    if (handoff->block == NULL) {
        CloseHandle(handoff->mapping);
        memset(handoff, 0, sizeof(*handoff));
        return 0;
    }
    handoff->ack_event = CreateEventW(NULL, FALSE, FALSE, event_name);
    if (handoff->ack_event == NULL) {
        UnmapViewOfFile(handoff->block);
        CloseHandle(handoff->mapping);
        memset(handoff, 0, sizeof(*handoff));
        return 0;
    }
    memcpy(handoff->block, bundle_data, bundle_size);
    handoff->block->target_pid = process_id;
    handoff->block->status = SONYVEGAS421_MAPPING_BUNDLE_CREATED;
    handoff->size = bundle_size;
    return 1;
}

static int finish_mapping_bundle_handoff(mapping_bundle_handoff *handoff) {
    int consumed = 0;
    if (handoff->ack_event != NULL) {
        WaitForSingleObject(handoff->ack_event, 10000);
    }
    if (handoff->block != NULL) {
        consumed = handoff->block->status
                == SONYVEGAS421_MAPPING_BUNDLE_CONSUMED;
        SecureZeroMemory(handoff->block, handoff->size);
        UnmapViewOfFile(handoff->block);
    }
    if (handoff->ack_event != NULL) CloseHandle(handoff->ack_event);
    if (handoff->mapping != NULL) CloseHandle(handoff->mapping);
    memset(handoff, 0, sizeof(*handoff));
    control_api_clear_cheatbreaker_mapping_bundle();
    return consumed;
}

static int socket_wait_readable(SOCKET socket_value, DWORD timeout_ms) {
    fd_set read_set;
    struct timeval timeout;
    int selected;
    FD_ZERO(&read_set);
    FD_SET(socket_value, &read_set);
    timeout.tv_sec = (long)(timeout_ms / 1000u);
    timeout.tv_usec = (long)((timeout_ms % 1000u) * 1000u);
    selected = select(0, &read_set, NULL, NULL, &timeout);
    return selected > 0 && FD_ISSET(socket_value, &read_set);
}

static int controller_receive_line(SOCKET socket_value, char *output,
        size_t capacity) {
    size_t length = 0;
    if (output == NULL || capacity == 0) return 0;
    while (length + 1 < capacity) {
        char character;
        int received = recv(socket_value, &character, 1, 0);
        if (received != 1) return 0;
        if (character == '\n') {
            if (length != 0 && output[length - 1] == '\r') --length;
            output[length] = '\0';
            return 1;
        }
        output[length++] = character;
    }
    output[0] = '\0';
    return 0;
}

static int controller_send_all(SOCKET socket_value, const char *data,
        size_t size) {
    size_t offset = 0;
    while (offset < size) {
        int sent = send(socket_value, data + offset, (int)(size - offset), 0);
        if (sent <= 0) return 0;
        offset += (size_t)sent;
    }
    return 1;
}

static int controller_send_token(SOCKET socket_value, const char *token) {
    return token != NULL && token[0] != '\0'
            && controller_send_all(socket_value, token, strlen(token))
            && controller_send_all(socket_value, SV_STR(SV_8181E3BA16810C79, "\n"), 1);
}

static void controller_error(char *output, size_t capacity,
        const char *message) {
    if (output == NULL || capacity == 0) return;
    strncpy_s(output, capacity, message == NULL ? SV_STR(SV_317F40F70479C4AE, "Client startup failed.")
            : message, _TRUNCATE);
}

static void bootstrap_object_name(wchar_t *output, size_t capacity,
        const wchar_t *kind, DWORD process_id) {
    _snwprintf_s(output, capacity, _TRUNCATE,
            SV_WSTR(SV_2857277F3A8D050C, L"Local\\IllIIlIl.%ls.%lu"), kind, process_id);
}

static void abandon_bootstrap_handoff(bootstrap_handoff *handoff) {
    if (handoff == NULL) return;
    if (handoff->block != NULL) {
        SecureZeroMemory(handoff->block, sizeof(*handoff->block));
        UnmapViewOfFile(handoff->block);
    }
    if (handoff->ack_event != NULL) CloseHandle(handoff->ack_event);
    if (handoff->mapping != NULL) CloseHandle(handoff->mapping);
    if (handoff->listener != INVALID_SOCKET) closesocket(handoff->listener);
    if (handoff->winsock_started) WSACleanup();
    memset(handoff, 0, sizeof(*handoff));
    handoff->listener = INVALID_SOCKET;
}

static int begin_bootstrap_handoff(DWORD process_id, const char *access_token,
        bootstrap_handoff *handoff) {
    WSADATA winsock_data;
    struct sockaddr_in address;
    int address_size = sizeof(address);
    wchar_t mapping_name[96];
    wchar_t event_name[96];
    if (handoff == NULL || access_token == NULL || access_token[0] == '\0'
            || strlen(access_token) >= 512) return 0;
    memset(handoff, 0, sizeof(*handoff));
    handoff->listener = INVALID_SOCKET;
    if (WSAStartup(MAKEWORD(2, 2), &winsock_data) != 0) return 0;
    handoff->winsock_started = 1;
    handoff->listener = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
    memset(&address, 0, sizeof(address));
    address.sin_family = AF_INET;
    address.sin_addr.s_addr = htonl(INADDR_LOOPBACK);
    address.sin_port = 0;
    if (handoff->listener == INVALID_SOCKET
            || bind(handoff->listener, (const struct sockaddr *)&address,
                    sizeof(address)) == SOCKET_ERROR
            || listen(handoff->listener, 1) == SOCKET_ERROR
            || getsockname(handoff->listener,
                    (const struct sockaddr *)&address, &address_size)
                    == SOCKET_ERROR) {
        abandon_bootstrap_handoff(handoff);
        return 0;
    }
    bootstrap_object_name(mapping_name,
            sizeof(mapping_name) / sizeof(mapping_name[0]),
            SV_WSTR(SV_F6AAAE70F63C1E06, L"Bootstrap"), process_id);
    bootstrap_object_name(event_name,
            sizeof(event_name) / sizeof(event_name[0]),
            SV_WSTR(SV_08784B7BF2FCD8C3, L"BootstrapAck"), process_id);
    handoff->mapping = CreateFileMappingW(INVALID_HANDLE_VALUE, NULL,
            PAGE_READWRITE, 0, sizeof(*handoff->block), mapping_name);
    handoff->ack_event = CreateEventW(NULL, TRUE, FALSE, event_name);
    handoff->block = handoff->mapping == NULL ? NULL
            : (SonyVegas421BootstrapV2 *)MapViewOfFile(handoff->mapping,
                    FILE_MAP_READ | FILE_MAP_WRITE, 0, 0,
                    sizeof(*handoff->block));
    if (handoff->mapping == NULL || handoff->ack_event == NULL
            || handoff->block == NULL) {
        abandon_bootstrap_handoff(handoff);
        return 0;
    }
    SecureZeroMemory(handoff->block, sizeof(*handoff->block));
    handoff->block->magic = SONYVEGAS421_BOOTSTRAP_MAGIC;
    handoff->block->version = SONYVEGAS421_BOOTSTRAP_VERSION;
    handoff->block->structure_size = (uint16_t)sizeof(*handoff->block);
    handoff->block->target_pid = process_id;
    handoff->block->mode = SONYVEGAS421_BOOTSTRAP_MODE_ONLINE;
    handoff->block->controller_port = ntohs(address.sin_port);
    handoff->block->service_zeus_port = 8091;
    handoff->block->status = SONYVEGAS421_BOOTSTRAP_STATUS_CREATED;
    return 1;
}

static int finish_bootstrap_handoff(bootstrap_handoff *handoff,
        char *error_out, size_t error_capacity) {
    SOCKET client = INVALID_SOCKET;
    char line[512];
    int success = 0;
    if (error_out != NULL && error_capacity != 0) error_out[0] = '\0';
    if (handoff == NULL || handoff->block == NULL
            || handoff->listener == INVALID_SOCKET) return 0;
    if (!socket_wait_readable(handoff->listener, 30000)) {
        controller_error(error_out, error_capacity,
                SV_STR(SV_239221359F6E1772, "The injected client did not connect to the startup controller."));
        goto cleanup;
    }
    client = accept(handoff->listener, NULL, NULL);
    if (client == INVALID_SOCKET
            || !controller_receive_line(client, line, sizeof(line))
            || strcmp(line, SV_STR(SV_1C6C3DA2689E3A2C, "617")) != 0
            || !controller_receive_line(client, line, sizeof(line))
            || strcmp(line, SV_STR(SV_815576FEE9CA32C7, "200")) != 0
            || !controller_send_token(client, control_api_session_token())) {
        controller_error(error_out, error_capacity,
                SV_STR(SV_B740BC946E3A1980, "The injected client could not establish its startup session."));
        goto cleanup;
    }
    if (WaitForSingleObject(handoff->ack_event, 15000) != WAIT_OBJECT_0
            || handoff->block->status != SONYVEGAS421_BOOTSTRAP_STATUS_CONSUMED) {
        controller_error(error_out, error_capacity,
                SV_STR(SV_8760772043C5D52A, "The injected client did not accept its startup session."));
        goto cleanup;
    }
    for (;;) {
        if (!socket_wait_readable(client, 120000)
                || !controller_receive_line(client, line, sizeof(line))) {
            controller_error(error_out, error_capacity,
                    SV_STR(SV_F8429C94DCD02AB5, "The injected client did not finish starting."));
            break;
        }
        if (strcmp(line, SV_STR(SV_BD9EA8F384546881, "604")) == 0) {
            if (!controller_receive_line(client, line, sizeof(line))
                    || !controller_receive_line(client, line, sizeof(line))) {
                controller_error(error_out, error_capacity,
                        SV_STR(SV_6C727025D1F6A4DE, "The startup progress channel was interrupted."));
                break;
            }
            continue;
        }
        if (strcmp(line, SV_STR(SV_24794EC1CDB3FC83, "606")) == 0
                && controller_receive_line(client, line, sizeof(line))
                && strcmp(line, SV_STR(SV_815576FEE9CA32C7, "200")) == 0) {
            success = 1;
            break;
        }
        if (strcmp(line, SV_STR(SV_2BDFA90B8F3E30D8, "618")) == 0
                && controller_receive_line(client, line, sizeof(line))) {
            char *end = NULL;
            unsigned long message_size = strtoul(line, &end, 10);
            if (end != line && *end == '\0' && message_size > 0
                    && message_size < error_capacity) {
                size_t received = 0;
                while (received < message_size) {
                    int count = recv(client, error_out + received,
                            (int)(message_size - received), 0);
                    if (count <= 0) break;
                    received += (size_t)count;
                }
                if (received == message_size) {
                    error_out[received] = '\0';
                } else {
                    controller_error(error_out, error_capacity,
                            SV_STR(SV_97CB755D3F9C187D, "The injected client reported an incomplete startup error."));
                }
            } else {
                controller_error(error_out, error_capacity,
                        SV_STR(SV_44751464D10CFEEF, "The injected client reported a startup failure."));
            }
            break;
        }
        controller_error(error_out, error_capacity,
                SV_STR(SV_51908630008D78AC, "The startup controller received an invalid response."));
        break;
    }
cleanup:
    if (client != INVALID_SOCKET) closesocket(client);
    abandon_bootstrap_handoff(handoff);
    return success;
}

static int validate_downloaded_payload(const unsigned char *data, size_t size) {
    const IMAGE_DOS_HEADER *dos_header;
    const IMAGE_NT_HEADERS64 *nt_headers;
    size_t nt_offset;

    if (data == NULL || size < sizeof(IMAGE_DOS_HEADER)
            || size > 40u * 1024u * 1024u) {
        return 0;
    }
    dos_header = (const IMAGE_DOS_HEADER *)data;
    if (dos_header->e_magic != IMAGE_DOS_SIGNATURE || dos_header->e_lfanew <= 0) {
        return 0;
    }
    nt_offset = (size_t)dos_header->e_lfanew;
    if (nt_offset > size || size - nt_offset < sizeof(IMAGE_NT_HEADERS64)) {
        return 0;
    }
    nt_headers = (const IMAGE_NT_HEADERS64 *)(data + nt_offset);
    return nt_headers->Signature == IMAGE_NT_SIGNATURE
            && nt_headers->FileHeader.Machine == IMAGE_FILE_MACHINE_AMD64
            && nt_headers->FileHeader.NumberOfSections > 0
            && nt_headers->FileHeader.NumberOfSections <= 96
            && nt_headers->OptionalHeader.Magic == IMAGE_NT_OPTIONAL_HDR64_MAGIC
            && nt_headers->OptionalHeader.SizeOfImage > 0;
}

static void loader_hash_hex(const unsigned char digest[32], char output[65]) {
    const char *digits = SV_STR(SV_ED299E91459544C4, "0123456789abcdef");
    size_t index;
    for (index = 0; index < 32; ++index) {
        output[index * 2] = digits[digest[index] >> 4];
        output[index * 2 + 1] = digits[digest[index] & 15];
    }
    output[64] = '\0';
}

static int loader_sha256_file(const wchar_t *path, char output[65]) {
    HANDLE file = INVALID_HANDLE_VALUE;
    HCRYPTPROV provider = 0;
    HCRYPTHASH hash = 0;
    unsigned char buffer[65536];
    unsigned char digest[32];
    DWORD read = 0;
    DWORD digest_size = sizeof(digest);
    int success = 0;
    file = CreateFileW(path, GENERIC_READ,
            FILE_SHARE_READ | FILE_SHARE_WRITE | FILE_SHARE_DELETE,
            NULL, OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL);
    if (file == INVALID_HANDLE_VALUE
            || !CryptAcquireContextW(&provider, NULL, NULL, PROV_RSA_AES,
                    CRYPT_VERIFYCONTEXT)
            || !CryptCreateHash(provider, CALG_SHA_256, 0, 0, &hash))
        goto cleanup;
    for (;;) {
        if (!ReadFile(file, buffer, sizeof(buffer), &read, NULL)) goto cleanup;
        if (read == 0) break;
        if (!CryptHashData(hash, buffer, read, 0)) goto cleanup;
    }
    if (!CryptGetHashParam(hash, HP_HASHVAL, digest, &digest_size, 0)
            || digest_size != 32) goto cleanup;
    loader_hash_hex(digest, output);
    success = 1;
cleanup:
    SecureZeroMemory(buffer, sizeof(buffer));
    SecureZeroMemory(digest, sizeof(digest));
    if (hash) CryptDestroyHash(hash);
    if (provider) CryptReleaseContext(provider, 0);
    if (file != INVALID_HANDLE_VALUE) CloseHandle(file);
    return success;
}

static int loader_sha256_bytes(const unsigned char *data, size_t size,
        char output[65]) {
    HCRYPTPROV provider = 0;
    HCRYPTHASH hash = 0;
    unsigned char digest[32];
    DWORD digest_size = sizeof(digest);
    int success = 0;
    if (size > 32u * 1024u * 1024u
            || !CryptAcquireContextW(&provider, NULL, NULL, PROV_RSA_AES,
                    CRYPT_VERIFYCONTEXT)
            || !CryptCreateHash(provider, CALG_SHA_256, 0, 0, &hash)
            || !CryptHashData(hash, data, (DWORD)size, 0)
            || !CryptGetHashParam(hash, HP_HASHVAL, digest, &digest_size, 0)
            || digest_size != 32) goto cleanup;
    loader_hash_hex(digest, output);
    success = 1;
cleanup:
    SecureZeroMemory(digest, sizeof(digest));
    if (hash) CryptDestroyHash(hash);
    if (provider) CryptReleaseContext(provider, 0);
    return success;
}

static int valid_loader_image(const unsigned char *data, size_t size) {
    const IMAGE_DOS_HEADER *dos;
    const IMAGE_NT_HEADERS64 *nt;
    if (!data || size < sizeof(IMAGE_DOS_HEADER)
            || size < sizeof(IMAGE_NT_HEADERS64)) return 0;
    dos = (const IMAGE_DOS_HEADER *)data;
    if (dos->e_magic != IMAGE_DOS_SIGNATURE || dos->e_lfanew < 0
            || (size_t)dos->e_lfanew > size - sizeof(IMAGE_NT_HEADERS64))
        return 0;
    nt = (const IMAGE_NT_HEADERS64 *)(data + dos->e_lfanew);
    return nt->Signature == IMAGE_NT_SIGNATURE
            && nt->FileHeader.Machine == IMAGE_FILE_MACHINE_AMD64
            && (nt->FileHeader.Characteristics & IMAGE_FILE_EXECUTABLE_IMAGE) != 0
            && (nt->FileHeader.Characteristics & IMAGE_FILE_DLL) == 0
            && nt->OptionalHeader.Magic == IMAGE_NT_OPTIONAL_HDR64_MAGIC;
}

static int loader_update_safe_path(const wchar_t *path) {
    const wchar_t *cursor;
    if (!path || !path[0]) return 0;
    for (cursor = path; *cursor; ++cursor) {
        if (*cursor == L'%' || *cursor == L'!' || *cursor == L'^'
                || *cursor == L'"' || *cursor == L'&' || *cursor == L'|'
                || *cursor == L'<' || *cursor == L'>') return 0;
    }
    return 1;
}

static int loader_update_write(const wchar_t *path,
        const unsigned char *data, size_t size) {
    HANDLE file;
    DWORD written = 0;
    int success = 0;
    file = CreateFileW(path, GENERIC_WRITE, 0, NULL,
            CREATE_ALWAYS, FILE_ATTRIBUTE_NORMAL, NULL);
    if (file == INVALID_HANDLE_VALUE) return 0;
    if (size <= MAXDWORD && WriteFile(file, data, (DWORD)size, &written, NULL)
            && written == (DWORD)size && FlushFileBuffers(file)) success = 1;
    CloseHandle(file);
    return success;
}

static int loader_update_schedule(const wchar_t *self_path,
        const unsigned char *data, size_t size) {
    wchar_t directory[MAX_PATH];
    wchar_t stage[MAX_PATH] = {0};
    wchar_t script[MAX_PATH] = {0};
    wchar_t command[MAX_PATH * 2 + 128];
    wchar_t cmd_path[MAX_PATH];
    wchar_t *last_separator;
    STARTUPINFOW startup;
    PROCESS_INFORMATION process;
    const char *script_body;
    int success = 0;
    if (!loader_update_safe_path(self_path)
            || wcscpy_s(directory, MAX_PATH, self_path) != 0) return 0;
    last_separator = wcsrchr(directory, L'\\');
    if (!last_separator) return 0;
    last_separator[1] = L'\0';
    if (!GetTempFileNameW(directory, SV_WSTR(SV_10BE24574CC3AA57, L"unk"),
            0, stage)) return 0;
    if (!loader_update_write(stage, data, size)) goto cleanup;
    if (_snwprintf_s(script, MAX_PATH, _TRUNCATE,
            SV_WSTR(SV_A9B353C498DDBFCD, L"%ls.cmd"), stage) < 0)
        goto cleanup;
    script_body = SV_STR(SV_9E91AF6C66E6BDFC, "@echo off\r\nsetlocal DisableDelayedExpansion\r\nset /a tries=0\r\n:retry\r\nmove /Y \"%SV_LOADER_UPDATE_SOURCE%\" \"%SV_LOADER_UPDATE_TARGET%\" >nul 2>nul\r\nif not exist \"%SV_LOADER_UPDATE_SOURCE%\" goto updated\r\nset /a tries+=1\r\nif %tries% GEQ 30 goto failed\r\ntimeout /t 1 /nobreak >nul\r\ngoto retry\r\n:updated\r\nstart \"\" \"%SV_LOADER_UPDATE_TARGET%\"\r\ndel /f /q \"%~f0\" >nul 2>nul\r\nexit /b 0\r\n:failed\r\ndel /f /q \"%SV_LOADER_UPDATE_SOURCE%\" >nul 2>nul\r\ndel /f /q \"%~f0\" >nul 2>nul\r\nexit /b 1\r\n");
    if (!loader_update_write(script,
            (const unsigned char *)script_body, strlen(script_body))) goto cleanup;
    if (!loader_update_safe_path(stage) || !loader_update_safe_path(script)
            || !GetSystemDirectoryW(cmd_path, MAX_PATH)
            || wcscat_s(cmd_path, MAX_PATH,
                    SV_WSTR(SV_B9FC930040B200DB, L"\\cmd.exe")) != 0)
        goto cleanup;
    _snwprintf_s(command, _countof(command), _TRUNCATE,
            SV_WSTR(SV_DFD5FD3205E434F2, L"\"%ls\" /D /V:OFF /C call \"%ls\""),
            cmd_path, script);
    if (!SetEnvironmentVariableW(
            SV_WSTR(SV_892DB63D38A48C98, L"SV_LOADER_UPDATE_SOURCE"), stage))
        goto cleanup;
    if (!SetEnvironmentVariableW(
            SV_WSTR(SV_D1B38C9694177D61, L"SV_LOADER_UPDATE_TARGET"), self_path)) {
        SetEnvironmentVariableW(SV_WSTR(SV_892DB63D38A48C98, L"SV_LOADER_UPDATE_SOURCE"), NULL);
        goto cleanup;
    }
    ZeroMemory(&startup, sizeof(startup));
    startup.cb = sizeof(startup);
    startup.dwFlags = STARTF_USESHOWWINDOW;
    startup.wShowWindow = SW_HIDE;
    ZeroMemory(&process, sizeof(process));
    if (CreateProcessW(cmd_path, command, NULL, NULL, FALSE,
            CREATE_NO_WINDOW, NULL, NULL, &startup, &process)) {
        CloseHandle(process.hThread);
        CloseHandle(process.hProcess);
        success = 1;
    }
    SetEnvironmentVariableW(SV_WSTR(SV_892DB63D38A48C98, L"SV_LOADER_UPDATE_SOURCE"), NULL);
    SetEnvironmentVariableW(SV_WSTR(SV_D1B38C9694177D61, L"SV_LOADER_UPDATE_TARGET"), NULL);
cleanup:
    if (!success) {
        DeleteFileW(stage);
        DeleteFileW(script);
    }
    return success;
}

/* 0: already current/no published loader; 1: replacement scheduled; -1: check failed. */
static int loader_update_if_needed(void) {
    wchar_t self_path[MAX_PATH];
    char current_sha[65] = {0};
    char expected_sha[65] = {0};
    char downloaded_sha[65] = {0};
    unsigned char *replacement = NULL;
    size_t replacement_size = 0;
    DWORD path_length = GetModuleFileNameW(NULL, self_path, MAX_PATH);
    int result;
    if (!path_length || path_length >= MAX_PATH
            || !loader_sha256_file(self_path, current_sha)) return -1;
    result = control_api_download_loader_update(current_sha,
            &replacement, &replacement_size, expected_sha);
    if (result != 1) return result;
    console_animate_progress(44,
            SV_WSTR(SV_D0CB83673F880FF5, L"ATUALIZANDO LOADER"),
            SV_WSTR(SV_E17B61C43BCCFDFB, L"Substituindo a versao antiga no mesmo arquivo"));
    if (!valid_loader_image(replacement, replacement_size)
            || !loader_sha256_bytes(replacement, replacement_size, downloaded_sha)
            || strcmp(downloaded_sha, expected_sha) != 0
            || !loader_update_schedule(self_path, replacement, replacement_size))
        result = -1;
    SecureZeroMemory(replacement, replacement_size);
    free(replacement);
    SecureZeroMemory(current_sha, sizeof(current_sha));
    SecureZeroMemory(expected_sha, sizeof(expected_sha));
    SecureZeroMemory(downloaded_sha, sizeof(downloaded_sha));
    return result;
}

static int authenticate_and_download(unsigned char **out_data,
        size_t *out_size) {
    char hwid[64] = {0};
    char username[64] = {0};
    char authorized_username[64] = {0};
    char license_key[160] = {0};
    char device_secret[256] = {0};
    char error_msg[256] = {0};
    unsigned char *dll_data = NULL;
    size_t dll_size = 0;

    console_last_error[0] = L'\0';
    console_animate_progress(10,
            SV_WSTR(SV_8C5C4511711BC9A6, L"GENERATING DEVICE ID"),
            SV_WSTR(SV_1CE45047D7CDF86F, L"Creating a secure fingerprint for this device"));

    if (!sonyvegas_generate_hwid(hwid, sizeof(hwid))) {
        console_set_error(SV_WSTR(SV_953D657DF3473B59, L"Could not generate a secure device fingerprint."));
        return 0;
    }

    sonyvegas_load_device_secret(device_secret, sizeof(device_secret));
    console_animate_progress(24,
            SV_WSTR(SV_28A6F45EF0081890, L"Verificando licen\347a"),
            SV_WSTR(SV_1A8E2E9642FC9D97, L"Conferindo sua licen\347a com o servidor"));
    if (control_api_check_hwid(hwid, device_secret, NULL,
            username, sizeof(username))) {
        strncpy_s(authorized_username, sizeof(authorized_username),
                username, _TRUNCATE);
        if (!console_prompt_auth(username, sizeof(username),
                license_key, sizeof(license_key), 2)) {
            console_set_error(SV_WSTR(SV_952E6ED37AE0F564, L"Authentication cancelled."));
            return 0;
        }
        if (_stricmp(username, authorized_username) != 0) {
            console_set_error(SV_WSTR(SV_7C8632F65E020D8F, L"Authentication failed."));
            return 0;
        }
        if (!control_api_check_hwid(hwid, device_secret, username,
                username, sizeof(username))) {
            if (control_api_last_auth_status() == CONTROL_AUTH_UNAVAILABLE) {
                console_set_api_connection_error();
            } else {
                console_set_error(SV_WSTR(SV_7C8632F65E020D8F, L"Authentication failed."));
            }
            return 0;
        }
        console_animate_progress(38,
                SV_WSTR(SV_0AB9096F56729B9B, L"LICENSE VERIFIED"),
                username[0] == '\0'
                        ? SV_WSTR(SV_1F01D4BC42F3E454, L"Welcome back")
                        : SV_WSTR(SV_F9FA57F4E73EB785, L"Welcome back. Your device is authorized."));
    } else if (control_api_claim_required()) {
        if (!console_prompt_auth(username, sizeof(username),
                license_key, sizeof(license_key), 0)) {
            console_set_error(SV_WSTR(SV_952E6ED37AE0F564, L"Authentication cancelled."));
            return 0;
        }
        console_animate_progress(32,
                SV_WSTR(SV_E078E1454FD220F8, L"SECURING DEVICE"),
                SV_WSTR(SV_E4E92CFB05099251, L"Moving your license to protected storage"));
        if (!control_api_claim_device(license_key, hwid, username,
                        username, sizeof(username), error_msg,
                        sizeof(error_msg))) {
            console_set_error_utf8(error_msg);
            return 0;
        }
        console_animate_progress(38,
                SV_WSTR(SV_D37AA9E98B3C2AFB, L"DEVICE SECURED"),
                SV_WSTR(SV_542F6C164809D2BA, L"Your license is now protected on this device"));
    } else if (control_api_last_auth_status() == CONTROL_AUTH_UNAVAILABLE) {
        console_set_api_connection_error();
        return 0;
    } else if (control_api_last_auth_status() == CONTROL_AUTH_DEVICE_DISABLED) {
        console_set_error(SV_WSTR(SV_4BCB46B9275D5A86, L"Dispositivo desativado no painel."));
        return 0;
    } else if (control_api_last_auth_status() == CONTROL_AUTH_LICENSE_INACTIVE) {
        console_set_error(SV_WSTR(SV_FAAC4704A35289BB, L"Licenca desativada, expirada ou removida."));
        return 0;
    } else if (control_api_last_auth_status() == CONTROL_AUTH_DEVICE_SECRET_INVALID) {
        console_set_error(SV_WSTR(SV_0AC91A3BEC122717, L"A sessao salva nao confere com o servidor."));
        return 0;
    } else if (control_api_last_auth_status() != CONTROL_AUTH_NOT_FOUND) {
        console_set_error(SV_WSTR(SV_28E1E94621A71808, L"Falha na autorizacao do dispositivo."));
        return 0;
    } else {
        if (!console_prompt_auth(username, sizeof(username),
                license_key, sizeof(license_key), 0)) {
            console_set_error(SV_WSTR(SV_952E6ED37AE0F564, L"Authentication cancelled."));
            return 0;
        }
        console_animate_progress(32,
                SV_WSTR(SV_D36CB55B86C8FF32, L"ACTIVATING LICENSE"),
                SV_WSTR(SV_976346E74EB15294, L"Registering this device"));
        if (!control_api_activate_license(license_key, hwid, username,
                error_msg, sizeof(error_msg))) {
            console_set_error_utf8(error_msg);
            return 0;
        }
        console_animate_progress(38,
                SV_WSTR(SV_F2FC3471AE695E81, L"LICENSE ACTIVATED"),
                SV_WSTR(SV_839355EA98FBCB2A, L"Your device is ready."));
    }

    if (control_api_new_device_secret()[0] != '\0'
            && !sonyvegas_store_device_secret(
                    control_api_new_device_secret())) {
        console_set_error(SV_WSTR(SV_AFF759DBF7B5C1D6, L"Could not store the protected device session."));
        return 0;
    }
    SecureZeroMemory(device_secret, sizeof(device_secret));
    SecureZeroMemory(license_key, sizeof(license_key));
    SecureZeroMemory(authorized_username, sizeof(authorized_username));

    console_animate_progress(43,
            SV_WSTR(SV_31C1625328ADEDF2, L"VERIFICANDO LOADER"),
            SV_WSTR(SV_D9E3EF15E9532469, L"Conferindo a versao mais recente"));
    {
        int update_result = loader_update_if_needed();
        if (update_result == 1) return 2;
        if (update_result < 0) {
            console_animate_progress(46,
                    SV_WSTR(SV_D8FA8E874D0A8E2A, L"ATUALIZACAO INDISPONIVEL"),
                    SV_WSTR(SV_44E58F63171D648A, L"Usando o loader atual nesta sessao"));
        }
    }

    console_animate_progress(52,
            SV_WSTR(SV_32B89C6439E521F7, L"Baixando m\363dulos"),
            SV_WSTR(SV_7E769636CEE42205, L"Recebendo os m\363dulos mais recentes"));
    if (!control_api_download_payload(hwid, &dll_data, &dll_size)) {
        console_set_error(SV_WSTR(SV_112C9B08AA60A395, L"The client download failed."));
        return 0;
    }
    if (!validate_downloaded_payload(dll_data, dll_size)) {
        console_set_error(SV_WSTR(SV_5C1A0A6B6395C69F, L"The downloaded client is invalid."));
        free(dll_data);
        return 0;
    }
    console_animate_progress(68,
            SV_WSTR(SV_918E2F315F8DDECC, L"PAYLOAD VERIFIED"),
            SV_WSTR(SV_DEF0B3A8E15693F8, L"The client is validated and ready to inject"));

    *out_data = dll_data;
    *out_size = dll_size;
    return 1;
}

int wmain(int argc, wchar_t **argv) {
    wchar_t dll_path[MAX_PATH];
    unsigned char *dll_data = NULL;
    size_t dll_size = 0;
    DWORD process_id = 0;
    int injection_result;
    int remote_mode = 1;
    int handoff_started = 0;
    int mapping_handoff_started = 0;
    int bootstrap_handoff_started = 0;
    remote_session_handoff handoff;
    mapping_bundle_handoff mapping_handoff;
    bootstrap_handoff startup_handoff;
    memset(&handoff, 0, sizeof(handoff));
    memset(&mapping_handoff, 0, sizeof(mapping_handoff));
    memset(&startup_handoff, 0, sizeof(startup_handoff));
    startup_handoff.listener = INVALID_SOCKET;

    SetConsoleTitleW(SV_WSTR(SV_F27E7737247731F3, L"unknown"));
    configure_console();
    animate_banner_fade_in();
    Sleep(300);

    if (argc > 1) {
        wchar_t *end = NULL;
        if (argc == 3) {
            process_id = (DWORD)wcstoul(argv[1], &end, 10);
            if (process_id == 0 || end == argv[1] || *end != L'\0') {
                console_render_result(0,
                        SV_WSTR(SV_5C4C21EF3CC5CA9C, L"INVALID PROCESS"),
                        SV_WSTR(SV_185AEC96186B6840, L"The supplied process identifier is not valid."));
                _getwch();
                return 2;
            }
        }
        if (argc >= 2 && wcscmp(argv[argc - 1], SV_WSTR(SV_5E62F5E32FB99ECB, L"--local")) == 0) {
            remote_mode = 0;
        }
    }

    if (!remote_mode) {
        DWORD length = GetModuleFileNameW(NULL, dll_path, MAX_PATH);
        wchar_t *file_name;
        DWORD attributes;
        if (length == 0 || length >= MAX_PATH) {
            console_render_result(0,
                    SV_WSTR(SV_F353E9032225EAB5, L"LOCAL STARTUP FAILED"),
                    SV_WSTR(SV_2FE8E818BCD56CDD, L"Could not resolve the loader path."));
            _getwch();
            return 2;
        }
        file_name = wcsrchr(dll_path, L'\\');
        file_name = file_name == NULL ? dll_path : file_name + 1;
        wcscpy(file_name, SV_WSTR(SV_64846C40304318A5, L"IllIIlIl.dll"));
        attributes = GetFileAttributesW(dll_path);
        if (attributes == INVALID_FILE_ATTRIBUTES
                || (attributes & FILE_ATTRIBUTE_DIRECTORY) != 0) {
            console_render_result(0,
                    SV_WSTR(SV_0C88916489632132, L"PAYLOAD NOT FOUND"),
                    SV_WSTR(SV_0CABE821868E210E, L"Place IllIIlIl.dll beside unknown.exe and retry."));
            _getwch();
            return 2;
        }
        console_animate_progress(68,
                SV_WSTR(SV_793357FE922FC486, L"LOCAL PAYLOAD READY"),
                SV_WSTR(SV_CE479B5F13ECEBD3, L"The local client was found and validated"));
    } else {
        int auth_result = authenticate_and_download(&dll_data, &dll_size);
        if (auth_result == 2) return 0;
        if (!auth_result) {
            console_render_result(0,
                    SV_WSTR(SV_643F79BFF9BE3F36, L"STARTUP FAILED"),
                    console_last_error[0] == L'\0'
                            ? SV_WSTR(SV_00391D4DFBC6D640, L"Authentication or payload download could not be completed.")
                            : console_last_error);
            _getwch();
            return 1;
        }
    }

    if (process_id == 0) {
        process_id = select_process();
        if (process_id == 0) {
            if (dll_data) {
                SecureZeroMemory(dll_data, dll_size);
                free(dll_data);
            }
            return 1;
        }
    }

    if (selected_client_name[0] == '\0') {
        process_candidate selected_candidate;
        const char *detected_client;
        memset(&selected_candidate, 0, sizeof(selected_candidate));
        selected_candidate.process_id = process_id;
        detected_client = detect_client_name(&selected_candidate);
        strncpy_s(selected_client_name, sizeof(selected_client_name),
                detected_client, _TRUNCATE);
    }

    if (remote_mode
            && _stricmp(selected_client_name,
                    SV_STR(SV_646D52AFC3FAEF45, "CheatBreaker")) == 0) {
        char mapping_error[256] = {0};
        console_animate_progress(72,
                SV_WSTR(SV_DF92FD33CD4E88D0, L"UPDATING MAPPINGS"),
                SV_WSTR(SV_F10BE81905054517, L"Checking the installed CheatBreaker version"));
        if (!control_api_prepare_cheatbreaker_mappings(mapping_error,
                sizeof(mapping_error))) {
            console_set_error_utf8(mapping_error[0] != '\0'
                    ? mapping_error : SV_STR(SV_85BE675F368EEBE4, "CheatBreaker mappings are unavailable."));
            if (dll_data != NULL) {
                SecureZeroMemory(dll_data, dll_size);
                free(dll_data);
            }
            console_render_result(0, SV_WSTR(SV_EE2DDAEE924BE568, L"MAPPING UPDATE FAILED"),
                    console_last_error);
            _getwch();
            return 1;
        }
    }

    console_animate_progress(76,
            SV_WSTR(SV_0354AB89235D0F05, L"Buscando Minecraft"),
            SV_WSTR(SV_F8D312E9C35DCC56, L"Procurando o processo do jogo"));

    if (remote_mode) {
        handoff_started = begin_remote_session_handoff(process_id,
                control_api_session_token(), control_api_session_username(),
                control_api_session_role(),
                &handoff);
        if (!handoff_started) {
            fwprintf(stderr, SV_WSTR(SV_7F13E0C5CF38596F, L"Could not create remote control session handoff.\n"));
        }
        if (_stricmp(selected_client_name,
                SV_STR(SV_646D52AFC3FAEF45, "CheatBreaker")) == 0) {
            mapping_handoff_started = begin_mapping_bundle_handoff(
                    process_id, &mapping_handoff);
            if (!mapping_handoff_started) {
                if (handoff_started) finish_remote_session_handoff(&handoff);
                SecureZeroMemory(dll_data, dll_size);
                free(dll_data);
                control_api_clear_cheatbreaker_mapping_bundle();
                console_render_result(0,
                        SV_WSTR(SV_EE2DDAEE924BE568, L"MAPPING UPDATE FAILED"),
                        SV_WSTR(SV_8C662BDA1C4BF58A, L"Could not prepare the in-memory mapping handoff."));
                _getwch();
                return 1;
            }
        }
        bootstrap_handoff_started = begin_bootstrap_handoff(process_id,
                control_api_session_token(), &startup_handoff);
        if (!bootstrap_handoff_started) {
            if (handoff_started) finish_remote_session_handoff(&handoff);
            if (mapping_handoff_started)
                finish_mapping_bundle_handoff(&mapping_handoff);
            SecureZeroMemory(dll_data, dll_size);
            free(dll_data);
            console_render_result(0,
                    SV_WSTR(SV_643F79BFF9BE3F36, L"STARTUP FAILED"),
                    SV_WSTR(SV_378DC002B3AF1139, L"Could not prepare the client startup session."));
            _getwch();
            return 1;
        }
    }

    if (remote_mode) {
        HANDLE process = OpenProcess(
                PROCESS_CREATE_THREAD | PROCESS_QUERY_INFORMATION
                | PROCESS_VM_OPERATION | PROCESS_VM_WRITE | PROCESS_VM_READ,
                FALSE, process_id);
        if (process == NULL) {
            print_last_error(SV_WSTR(SV_28CF9F9BB7B7FE5D, L"OpenProcess"));
            SecureZeroMemory(dll_data, dll_size);
            free(dll_data);
            if (handoff_started) finish_remote_session_handoff(&handoff);
            if (mapping_handoff_started)
                finish_mapping_bundle_handoff(&mapping_handoff);
            if (bootstrap_handoff_started)
                abandon_bootstrap_handoff(&startup_handoff);
            console_render_result(0,
                    SV_WSTR(SV_59F8704BFD3A2B9C, L"PROCESS ACCESS FAILED"),
                    SV_WSTR(SV_94C1EEBAF1B29124, L"Could not open the selected game process."));
            _getwch();
            return 3;
        }
        if (!require_x64_target(process)) {
            fwprintf(stderr, SV_WSTR(SV_A930A7E219FC3BFF, L"Target process is not x64; injection refused.\n"));
            CloseHandle(process);
            SecureZeroMemory(dll_data, dll_size);
            free(dll_data);
            if (handoff_started) finish_remote_session_handoff(&handoff);
            if (mapping_handoff_started)
                finish_mapping_bundle_handoff(&mapping_handoff);
            if (bootstrap_handoff_started)
                abandon_bootstrap_handoff(&startup_handoff);
            console_render_result(0,
                    SV_WSTR(SV_D7CF6779A75E990A, L"UNSUPPORTED PROCESS"),
                    SV_WSTR(SV_F6A5B083355F479C, L"A 64-bit Java game process is required."));
            _getwch();
            return 3;
        }
        console_animate_progress(88,
                SV_WSTR(SV_DE86DE6AD43FCCCE, L"INJECTING"),
                SV_WSTR(SV_3CC546234D5FB3E2, L"Mapping the client safely into the Java game process"));
        injection_result = manual_map_inject(process_id, process,
                dll_data, dll_size);
        CloseHandle(process);
        SecureZeroMemory(dll_data, dll_size);
        free(dll_data);
        dll_data = NULL;
    } else {
        console_animate_progress(88,
                SV_WSTR(SV_DE86DE6AD43FCCCE, L"INJECTING"),
                SV_WSTR(SV_8B7B05AA56D7E052, L"Loading the local client into the Java game process"));
        injection_result = inject_library(process_id, dll_path);
    }

    if (injection_result == 0) {
        if (bootstrap_handoff_started)
            abandon_bootstrap_handoff(&startup_handoff);
    } else if (bootstrap_handoff_started && injection_result != 2) {
        char startup_error[512];
        if (!finish_bootstrap_handoff(&startup_handoff, startup_error,
                sizeof(startup_error))) {
            console_set_error_utf8(startup_error);
            if (handoff_started) finish_remote_session_handoff(&handoff);
            if (mapping_handoff_started)
                finish_mapping_bundle_handoff(&mapping_handoff);
            console_render_result(0,
                    SV_WSTR(SV_643F79BFF9BE3F36, L"STARTUP FAILED"),
                    console_last_error);
            _getwch();
            return 3;
        }
    } else if (bootstrap_handoff_started) {
        abandon_bootstrap_handoff(&startup_handoff);
    }

    if (handoff_started && !finish_remote_session_handoff(&handoff)) {
        fwprintf(stderr, SV_WSTR(SV_E579C165EE2DE3A3, L"Remote control session was not consumed by the DLL.\n"));
    }
    if (mapping_handoff_started
            && !finish_mapping_bundle_handoff(&mapping_handoff)) {
        fwprintf(stderr, SV_WSTR(SV_73819A7713A9FEF4,
                L"The in-memory mapping profile was not consumed by the DLL.\n"));
    }

    if (injection_result == 0) {
        wchar_t fail_detail[256];
        int stage = manual_map_last_stage();
        const char *mod = manual_map_last_module();
        const char *func = manual_map_last_function();
        const char *detail = (func != NULL && func[0] != '\0') ? func
                : ((mod != NULL && mod[0] != '\0') ? mod : NULL);
        if (stage == 5 && detail != NULL) {
            _snwprintf_s(fail_detail,
                    sizeof(fail_detail) / sizeof(fail_detail[0]), _TRUNCATE,
                    SV_WSTR(SV_8FC593944CB13B35, L"Stage 5 import failed: %S"), detail);
        } else {
            _snwprintf_s(fail_detail,
                    sizeof(fail_detail) / sizeof(fail_detail[0]), _TRUNCATE,
                    SV_WSTR(SV_06D37684F2CB76EC, L"Stage %d failed. Check the native log for details."),
                    stage);
        }
        console_render_result(0,
                SV_WSTR(SV_61F0161F77D13941, L"INJECTION FAILED"),
                fail_detail);
        _getwch();
        return 3;
    }
    console_animate_progress(100,
            injection_result == 2
                    ? SV_WSTR(SV_7A05326C5F63159F, L"already active")
                    : SV_WSTR(SV_F27E7737247731F3, L"unknown"),
            SV_WSTR(SV_665A7A6CD7B5EF97, L"ready"));
    control_api_update_last_injection();
    render_injected_console(process_id, injection_result == 2);

    /* Self destruct is deliberately user-triggered and never runs on inject. */
    if (wait_for_self_destruct_key()) {
        cleanup_report cr;
        console_progress = 0;
        console_animate_progress(92,
                SV_WSTR(SV_7320EFE7653570EF, L"SELF DESTRUCT"),
                SV_WSTR(SV_EE1D03CD14A655D9, L"Removing local loader data"));
        cleanup_run(&cr);
        console_animate_progress(100,
                SV_WSTR(SV_670A00F1AE19332E, L"SELF DESTRUCT COMPLETE"),
                SV_WSTR(SV_FE1203854937BAE4, L"Local loader data was removed"));
    }

    Sleep(1200);
    return 0;
}

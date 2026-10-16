#ifndef WIN32_LEAN_AND_MEAN
#define WIN32_LEAN_AND_MEAN
#endif
#include <windows.h>
#include <winternl.h>
#include <tlhelp32.h>
#include <aclapi.h>
#include <psapi.h>
#include <intrin.h>
#include <stdlib.h>
#include "anti_debug.h"

#pragma comment(lib, "ntdll.lib")
#pragma comment(lib, "advapi32.lib")

/* ── NT internals ──────────────────────────────────────────────────── */

#ifndef STATUS_SUCCESS
#define STATUS_SUCCESS ((NTSTATUS)0)
#endif
#ifndef ThreadHideFromDebugger
#define ThreadHideFromDebugger 0x11
#endif
#ifndef ProcessDebugPort
#define ProcessDebugPort 7
#endif
#ifndef ProcessDebugObjectHandle
#define ProcessDebugObjectHandle 30
#endif
#ifndef ProcessDebugFlags
#define ProcessDebugFlags 31
#endif
#ifndef SystemKernelDebuggerInformation
#define SystemKernelDebuggerInformation 35
#endif

typedef struct {
    BOOLEAN DebuggerEnabled;
    BOOLEAN DebuggerNotPresent;
} SV_KERNEL_DEBUGGER_INFO;

typedef NTSTATUS (NTAPI *fn_NtQIP)(HANDLE, ULONG, PVOID, ULONG, PULONG);
typedef NTSTATUS (NTAPI *fn_NtSIT)(HANDLE, ULONG, PVOID, ULONG);
typedef NTSTATUS (NTAPI *fn_NtQSI)(ULONG, PVOID, ULONG, PULONG);

static fn_NtQIP pNtQIP;
static fn_NtSIT pNtSIT;
static fn_NtQSI pNtQSI;

static volatile LONG g_sentinel_running;
static HANDLE        g_sentinel_thread;
static DWORD         g_init_module_count;

/* ── resolve NT functions ──────────────────────────────────────────── */

static void resolve_nt(void) {
    HMODULE ntdll = GetModuleHandleW(L"ntdll.dll");
    if (!ntdll) return;
    pNtQIP = (fn_NtQIP)GetProcAddress(ntdll, "NtQueryInformationProcess");
    pNtSIT = (fn_NtSIT)GetProcAddress(ntdll, "NtSetInformationThread");
    pNtQSI = (fn_NtQSI)GetProcAddress(ntdll, "NtQuerySystemInformation");
}

/* ═══════════════════════════════════════════════════════════════════
 *  DETECTION CHECKS
 * ═══════════════════════════════════════════════════════════════════ */

#pragma optimize("", off)

/* 1. PEB.BeingDebugged */
static int check_debugger_present(void) {
    if (IsDebuggerPresent()) return 1;
#ifdef _M_X64
    return *(unsigned char *)(__readgsqword(0x60) + 2) != 0;
#else
    return *(unsigned char *)(__readfsdword(0x30) + 2) != 0;
#endif
}

/* 2. Debug port */
static int check_debug_port(void) {
    if (!pNtQIP) return 0;
    DWORD_PTR port = 0;
    return pNtQIP(GetCurrentProcess(), ProcessDebugPort,
            &port, sizeof(port), NULL) == STATUS_SUCCESS && port != 0;
}

/* 3. Debug object handle */
static int check_debug_object(void) {
    if (!pNtQIP) return 0;
    HANDLE obj = NULL;
    NTSTATUS s = pNtQIP(GetCurrentProcess(), ProcessDebugObjectHandle,
            &obj, sizeof(obj), NULL);
    if (s == STATUS_SUCCESS && obj) { CloseHandle(obj); return 1; }
    return 0;
}

/* 4. Debug flags */
static int check_debug_flags(void) {
    if (!pNtQIP) return 0;
    DWORD flags = 1;
    return pNtQIP(GetCurrentProcess(), ProcessDebugFlags,
            &flags, sizeof(flags), NULL) == STATUS_SUCCESS && flags == 0;
}

/* 5. Remote debugger */
static int check_remote_debugger(void) {
    BOOL p = FALSE;
    CheckRemoteDebuggerPresent(GetCurrentProcess(), &p);
    return p != FALSE;
}

/* 6. Kernel debugger */
static int check_kernel_debugger(void) {
    if (!pNtQSI) return 0;
    SV_KERNEL_DEBUGGER_INFO info = {0};
    return pNtQSI(SystemKernelDebuggerInformation,
            &info, sizeof(info), NULL) == STATUS_SUCCESS
            && info.DebuggerEnabled && !info.DebuggerNotPresent;
}

/* 7. Hardware breakpoints */
static int check_hardware_breakpoints(void) {
    CONTEXT ctx;
    memset(&ctx, 0, sizeof(ctx));
    ctx.ContextFlags = CONTEXT_DEBUG_REGISTERS;
    if (!GetThreadContext(GetCurrentThread(), &ctx)) return 0;
    return (ctx.Dr0 | ctx.Dr1 | ctx.Dr2 | ctx.Dr3) != 0;
}

/* 8. RDTSC timing */
static int check_timing(void) {
    unsigned __int64 t1 = __rdtsc();
    volatile int acc = 0;
    for (int i = 0; i < 100; ++i) acc += i;
    (void)acc;
    return (__rdtsc() - t1) > 10000000;
}

/* 9. NTDLL hook detection */
static int check_ntdll_hooks(void) {
    HMODULE ntdll = GetModuleHandleW(L"ntdll.dll");
    if (!ntdll) return 0;
    const char *fns[] = {
        "NtQueryInformationProcess", "NtSetInformationThread",
        "NtQuerySystemInformation",  "NtReadVirtualMemory",
        "NtWriteVirtualMemory",      "NtCreateThreadEx",
        "NtProtectVirtualMemory",    "NtOpenProcess"
    };
    for (int i = 0; i < _countof(fns); ++i) {
        unsigned char *p = (unsigned char *)GetProcAddress(ntdll, fns[i]);
        if (p && (p[0] == 0xE9 || p[0] == 0xCC || p[0] == 0xEB))
            return 1;
    }
    return 0;
}

/* 10. NtGlobalFlag */
static int check_nt_global_flag(void) {
#ifdef _M_X64
    return (*(DWORD *)(__readgsqword(0x60) + 0xBC) & 0x70) != 0;
#else
    return (*(DWORD *)(__readfsdword(0x30) + 0x68) & 0x70) != 0;
#endif
}

/* 11. Heap flags */
static int check_heap_flags(void) {
#ifdef _M_X64
    PVOID peb  = (PVOID)__readgsqword(0x60);
    PVOID heap = *(PVOID *)((BYTE *)peb + 0x30);
    DWORD f  = *(DWORD *)((BYTE *)heap + 0x70);
    DWORD ff = *(DWORD *)((BYTE *)heap + 0x74);
#else
    PVOID peb  = (PVOID)__readfsdword(0x30);
    PVOID heap = *(PVOID *)((BYTE *)peb + 0x18);
    DWORD f  = *(DWORD *)((BYTE *)heap + 0x40);
    DWORD ff = *(DWORD *)((BYTE *)heap + 0x44);
#endif
    return ((f & ~0x2u) != 0 || ff != 0);
}

/* 12. RE tool process scan via case-insensitive name fragments */
static const wchar_t *RE_FRAGMENTS[] = {
    L"x64dbg",      L"x32dbg",      L"ollydbg",
    L"windbg",      L"ida64",       L"ida.exe",
    L"ghidra",      L"wireshark",   L"cheatengine",
    L"dnspy",       L"de4dot",      L"scylla",
    L"pestudio",    L"hxd.exe",     L"procmon",
    L"processhacker", L"fiddler",   L"httpdebugger",
    L"apimonitor",  L"regmon",      L"procexp",
    L"resourcehacker", L"idaq",     L"immunitydebugger",
    L"megadumper",  L"strongod",    L"titanhide",
};

static int check_re_tools(void) {
    HANDLE snap = CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0);
    if (snap == INVALID_HANDLE_VALUE) return 0;
    PROCESSENTRY32W pe;
    pe.dwSize = sizeof(pe);
    int found = 0;
    if (Process32FirstW(snap, &pe)) {
        do {
            wchar_t lower[MAX_PATH];
            wcsncpy_s(lower, MAX_PATH, pe.szExeFile, _TRUNCATE);
            _wcslwr_s(lower, MAX_PATH);
            for (int i = 0; i < _countof(RE_FRAGMENTS); ++i) {
                if (wcsstr(lower, RE_FRAGMENTS[i])) { found = 1; break; }
            }
            if (found) break;
        } while (Process32NextW(snap, &pe));
    }
    CloseHandle(snap);
    return found;
}

/* 13. Debugger window class scan */
static int check_debugger_windows(void) {
    const wchar_t *classes[] = {
        L"OLLYDBG",  L"WinDbgFrameClass", L"ID",
        L"Zeta Debugger", L"Rock Debugger", L"ObsidianGUI"
    };
    for (int i = 0; i < _countof(classes); ++i)
        if (FindWindowW(classes[i], NULL)) return 1;
    if (FindWindowW(NULL, L"x64dbg")) return 1;
    if (FindWindowW(NULL, L"x32dbg")) return 1;
    return 0;
}

/* 14. Parent process check */
static int check_parent_process(void) {
    typedef struct {
        PVOID Reserved1;
        PVOID PebBaseAddress;
        PVOID Reserved2[2];
        ULONG_PTR UniqueProcessId;
        ULONG_PTR InheritedFromUniqueProcessId;
    } PROCESS_BASIC_INFO_EX;

    if (!pNtQIP) return 0;
    PROCESS_BASIC_INFO_EX pbi;
    memset(&pbi, 0, sizeof(pbi));
    if (pNtQIP(GetCurrentProcess(), 0,
            &pbi, sizeof(pbi), NULL) != STATUS_SUCCESS)
        return 0;

    DWORD parent_pid = (DWORD)pbi.InheritedFromUniqueProcessId;
    if (parent_pid == 0) return 0;

    HANDLE snap = CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0);
    if (snap == INVALID_HANDLE_VALUE) return 0;
    PROCESSENTRY32W pe;
    pe.dwSize = sizeof(pe);
    int found = 0;
    if (Process32FirstW(snap, &pe)) {
        do {
            if (pe.th32ProcessID != parent_pid) continue;
            wchar_t lower[MAX_PATH];
            wcsncpy_s(lower, MAX_PATH, pe.szExeFile, _TRUNCATE);
            _wcslwr_s(lower, MAX_PATH);
            for (int i = 0; i < _countof(RE_FRAGMENTS); ++i) {
                if (wcsstr(lower, RE_FRAGMENTS[i])) { found = 1; break; }
            }
            break;
        } while (Process32NextW(snap, &pe));
    }
    CloseHandle(snap);
    return found;
}

/* 15. Suspicious DLL detection (injected debugger/instrumentation) */
static int check_suspicious_dlls(void) {
    const wchar_t *bad_dlls[] = {
        L"ScyllaHide",    L"HookLibraryx64",
        L"x64bridge",     L"x32bridge",
        L"vehdebug",      L"TitanHide",
        L"SharpOD",       L"StrongOD",
        L"PhantOm",       L"cmdvrt",
        L"SbieDll",
    };
    HMODULE mods[512];
    DWORD needed;
    if (!EnumProcessModules(GetCurrentProcess(),
            mods, sizeof(mods), &needed))
        return 0;
    DWORD count = needed / sizeof(HMODULE);
    for (DWORD m = 0; m < count; ++m) {
        wchar_t name[MAX_PATH];
        if (!GetModuleFileNameW(mods[m], name, MAX_PATH)) continue;
        wchar_t *base = wcsrchr(name, L'\\');
        base = base ? base + 1 : name;
        for (int i = 0; i < _countof(bad_dlls); ++i) {
            if (_wcsnicmp(base, bad_dlls[i], wcslen(bad_dlls[i])) == 0)
                return 1;
        }
    }
    return 0;
}

/* 16. INT3 scan on our own code */
static int check_int3_scan(void) {
    HMODULE self = GetModuleHandleW(NULL);
    if (!self) return 0;
    IMAGE_DOS_HEADER *dos = (IMAGE_DOS_HEADER *)self;
    IMAGE_NT_HEADERS *nt =
            (IMAGE_NT_HEADERS *)((BYTE *)self + dos->e_lfanew);
    IMAGE_SECTION_HEADER *sec = IMAGE_FIRST_SECTION(nt);
    for (WORD i = 0; i < nt->FileHeader.NumberOfSections; ++i) {
        if (!(sec[i].Characteristics & IMAGE_SCN_MEM_EXECUTE)) continue;
        unsigned char *start = (unsigned char *)self + sec[i].VirtualAddress;
        DWORD size = sec[i].Misc.VirtualSize;
        int consecutive = 0;
        for (DWORD j = 0; j < size; ++j) {
            if (start[j] == 0xCC) {
                if (++consecutive >= 4) return 1;
            } else {
                consecutive = 0;
            }
        }
    }
    return 0;
}

/* 17. Module injection detection */
static DWORD count_modules(void) {
    HANDLE snap = CreateToolhelp32Snapshot(
            TH32CS_SNAPMODULE, GetCurrentProcessId());
    if (snap == INVALID_HANDLE_VALUE) return 0;
    MODULEENTRY32W me;
    me.dwSize = sizeof(me);
    DWORD count = 0;
    if (Module32FirstW(snap, &me)) {
        do { ++count; } while (Module32NextW(snap, &me));
    }
    CloseHandle(snap);
    return count;
}

static int check_module_injection(void) {
    if (g_init_module_count == 0) return 0;
    DWORD current = count_modules();
    return (current > g_init_module_count + 3);
}

/* 18. NtClose invalid handle trap */
static int check_ntclose_trap(void) {
    __try {
        CloseHandle((HANDLE)0xDEADBEEFull);
    } __except (EXCEPTION_EXECUTE_HANDLER) {
        return 1;
    }
    return 0;
}

/* 19. Anti-dump: erase PE header from memory */
static void erase_pe_header(void) {
    HMODULE self = GetModuleHandleW(NULL);
    if (!self) return;
    IMAGE_DOS_HEADER *dos = (IMAGE_DOS_HEADER *)self;
    DWORD header_size = dos->e_lfanew
            + ((IMAGE_NT_HEADERS *)((BYTE *)self + dos->e_lfanew))
                    ->OptionalHeader.SizeOfHeaders;
    DWORD old;
    if (VirtualProtect(self, header_size, PAGE_READWRITE, &old)) {
        SecureZeroMemory(self, header_size);
        VirtualProtect(self, header_size, old, &old);
    }
}

/* 20. Anti-attach: patch DbgUiRemoteBreakin to return immediately */
static void patch_dbg_remote_breakin(void) {
    HMODULE ntdll = GetModuleHandleW(L"ntdll.dll");
    if (!ntdll) return;
    unsigned char *fn = (unsigned char *)
            GetProcAddress(ntdll, "DbgUiRemoteBreakin");
    if (!fn) return;
    DWORD old;
    if (VirtualProtect(fn, 1, PAGE_EXECUTE_READWRITE, &old)) {
        fn[0] = 0xC3; /* RET */
        VirtualProtect(fn, 1, old, &old);
    }
}

#pragma optimize("", on)

/* ═══════════════════════════════════════════════════════════════════
 *  PROCESS SELF-PROTECTION (DACL)
 * ═══════════════════════════════════════════════════════════════════ */

static void harden_process_dacl(void) {
    HANDLE token;
    if (OpenProcessToken(GetCurrentProcess(),
            TOKEN_ADJUST_PRIVILEGES | TOKEN_QUERY, &token)) {
        TOKEN_PRIVILEGES tp;
        tp.PrivilegeCount = 1;
        tp.Privileges[0].Attributes = SE_PRIVILEGE_ENABLED;
        if (LookupPrivilegeValueW(NULL, L"SeDebugPrivilege",
                &tp.Privileges[0].Luid))
            AdjustTokenPrivileges(token, FALSE, &tp, 0, NULL, NULL);
        CloseHandle(token);
    }

    SID_IDENTIFIER_AUTHORITY auth = SECURITY_WORLD_SID_AUTHORITY;
    PSID everyone = NULL;
    if (!AllocateAndInitializeSid(&auth, 1, SECURITY_WORLD_RID,
            0, 0, 0, 0, 0, 0, 0, &everyone))
        return;

    EXPLICIT_ACCESSW ea;
    memset(&ea, 0, sizeof(ea));
    ea.grfAccessPermissions = PROCESS_VM_READ | PROCESS_VM_WRITE
            | PROCESS_VM_OPERATION | PROCESS_TERMINATE
            | PROCESS_SUSPEND_RESUME | PROCESS_CREATE_THREAD;
    ea.grfAccessMode = DENY_ACCESS;
    ea.grfInheritance = NO_INHERITANCE;
    ea.Trustee.TrusteeForm = TRUSTEE_IS_SID;
    ea.Trustee.TrusteeType = TRUSTEE_IS_WELL_KNOWN_GROUP;
    ea.Trustee.ptstrName = (LPWSTR)everyone;

    PACL dacl = NULL;
    if (SetEntriesInAclW(1, &ea, NULL, &dacl) == ERROR_SUCCESS) {
        SetSecurityInfo(GetCurrentProcess(), SE_KERNEL_OBJECT,
                DACL_SECURITY_INFORMATION, NULL, NULL, dacl, NULL);
        LocalFree(dacl);
    }
    FreeSid(everyone);
}

static void hide_thread(void) {
    if (pNtSIT)
        pNtSIT(GetCurrentThread(), ThreadHideFromDebugger, NULL, 0);
}

/* ═══════════════════════════════════════════════════════════════════
 *  SENTINEL BACKGROUND THREAD
 * ═══════════════════════════════════════════════════════════════════ */

static void __declspec(noreturn) detected_exit(void) {
    TerminateProcess(GetCurrentProcess(), 0);
    ExitProcess(0);
    for (;;) {}
}

static DWORD WINAPI sentinel_proc(LPVOID param) {
    (void)param;
    Sleep(3000);
    hide_thread();

    while (InterlockedCompareExchange(&g_sentinel_running, 1, 1) == 1) {
#ifdef SONYVEGAS421_AD_DIAG
        struct { const char *name; int(*fn)(void); } sc[] = {
            {"S:debugger_present",  check_debugger_present},
            {"S:debug_port",        check_debug_port},
            {"S:debug_object",      check_debug_object},
            {"S:debug_flags",       check_debug_flags},
            {"S:remote_debugger",   check_remote_debugger},
            {"S:hw_breakpoints",    check_hardware_breakpoints},
            {"S:timing",            check_timing},
            {"S:ntdll_hooks",       check_ntdll_hooks},
            {"S:re_tools",          check_re_tools},
            {"S:nt_global_flag",    check_nt_global_flag},
            {"S:heap_flags",        check_heap_flags},
            {"S:kernel_debugger",   check_kernel_debugger},
            {"S:debugger_windows",  check_debugger_windows},
            {"S:parent_process",    check_parent_process},
            {"S:suspicious_dlls",   check_suspicious_dlls},
            {"S:ntclose_trap",      check_ntclose_trap},
        };
        for (int i = 0; i < _countof(sc); ++i) {
            if (sc[i].fn()) {
                MessageBoxA(NULL, sc[i].name, "Sentinel failed", MB_OK);
                detected_exit();
            }
        }
#else
        if (check_debugger_present()
                || check_debug_port()
                || check_debug_object()
                || check_debug_flags()
                || check_remote_debugger()
                || check_hardware_breakpoints()
                || check_timing()
                || check_ntdll_hooks()
                || check_re_tools()
                || check_nt_global_flag()
                || check_heap_flags()
                || check_kernel_debugger()
                || check_debugger_windows()
                || check_parent_process()
                || check_suspicious_dlls()
                || check_ntclose_trap())
            detected_exit();
#endif

        LARGE_INTEGER ctr;
        QueryPerformanceCounter(&ctr);
        Sleep(8000 + (DWORD)(ctr.QuadPart % 7000));
    }
    return 0;
}

/* ═══════════════════════════════════════════════════════════════════
 *  PUBLIC API
 * ═══════════════════════════════════════════════════════════════════ */

#pragma optimize("", off)

int anti_debug_init(void) {
    resolve_nt();
    harden_process_dacl();
    hide_thread();
    patch_dbg_remote_breakin();
    g_init_module_count = count_modules();

#ifdef SONYVEGAS421_AD_DIAG
    struct { const char *name; int(*fn)(void); } checks[] = {
        {"debugger_present",    check_debugger_present},
        {"debug_port",          check_debug_port},
        {"debug_object",        check_debug_object},
        {"debug_flags",         check_debug_flags},
        {"remote_debugger",     check_remote_debugger},
        {"kernel_debugger",     check_kernel_debugger},
        {"hw_breakpoints",      check_hardware_breakpoints},
        {"nt_global_flag",      check_nt_global_flag},
        {"heap_flags",          check_heap_flags},
        {"re_tools",            check_re_tools},
        {"debugger_windows",    check_debugger_windows},
        {"parent_process",      check_parent_process},
        {"ntclose_trap",        check_ntclose_trap},
    };
    for (int i = 0; i < _countof(checks); ++i) {
        if (checks[i].fn()) {
            MessageBoxA(NULL, checks[i].name, "AD check failed", MB_OK);
            return 0;
        }
    }
#else
    if (check_debugger_present()
            || check_debug_port()
            || check_debug_object()
            || check_debug_flags()
            || check_remote_debugger()
            || check_kernel_debugger()
            || check_hardware_breakpoints()
            || check_nt_global_flag()
            || check_heap_flags()
            || check_re_tools()
            || check_debugger_windows()
            || check_parent_process()
            || check_ntclose_trap())
        return 0;
#endif

    return 1;
}

#pragma optimize("", on)

void anti_debug_start_sentinel(void) {
    InterlockedExchange(&g_sentinel_running, 1);
    g_sentinel_thread = CreateThread(NULL, 0, sentinel_proc,
            NULL, 0, NULL);
}

void anti_debug_stop(void) {
    InterlockedExchange(&g_sentinel_running, 0);
    if (g_sentinel_thread) {
        WaitForSingleObject(g_sentinel_thread, 3000);
        CloseHandle(g_sentinel_thread);
        g_sentinel_thread = NULL;
    }
}

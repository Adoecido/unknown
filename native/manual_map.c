#ifndef WIN32_LEAN_AND_MEAN
#define WIN32_LEAN_AND_MEAN
#endif
#include <windows.h>
#include <tlhelp32.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>

#include "manual_map.h"
#include "obf.h"

/* The mapper stays silent in release builds, but the phase it died in is
 * cheap to keep and is the only clue the loader can show the user. */
static volatile long g_mm_stage = 0;

/* Dotted name of the dependency (and "mod!func") that broke stage 5. */
static char g_mm_module[64];
static char g_mm_function[160];

int manual_map_last_stage(void) {
    return (int)g_mm_stage;
}

const char *manual_map_last_module(void) {
    return g_mm_module;
}

const char *manual_map_last_function(void) {
    return g_mm_function;
}

static void mm_fail_import(const char *module, const char *function) {
    if (module != NULL) {
        strncpy_s(g_mm_module, sizeof(g_mm_module), module, _TRUNCATE);
    } else {
        g_mm_module[0] = '\0';
    }
    if (function != NULL) {
        strncpy_s(g_mm_function, sizeof(g_mm_function), function, _TRUNCATE);
    } else {
        g_mm_function[0] = '\0';
    }
}

/* Loader diagnostics are intentionally absent from release builds. */
#define mm_log_open() ((void)0)
#define mm_log_close() ((void)0)
#define mm_log(...) ((void)0)
#define mm_error(...) ((void)0)

/* ---- helpers ----------------------------------------------------------- */

static int mm_write(HANDLE process, void *remote, const void *local,
        SIZE_T size) {
    SIZE_T written = 0;
    return WriteProcessMemory(process, remote, local, size, &written)
            && written == size;
}

static int mm_read(HANDLE process, const void *remote, void *local,
        SIZE_T size) {
    SIZE_T bytes_read = 0;
    return ReadProcessMemory(process, remote, local, size, &bytes_read)
            && bytes_read == size;
}

/* Convert an RVA to a raw file offset. Returns (DWORD)-1 on failure. */
static DWORD rva_to_offset(DWORD rva,
        const IMAGE_SECTION_HEADER *sections, WORD num_sections,
        DWORD size_of_headers) {
    WORD i;
    if (rva < size_of_headers) return rva;
    for (i = 0; i < num_sections; ++i) {
        DWORD va = sections[i].VirtualAddress;
        DWORD vs = sections[i].Misc.VirtualSize;
        if (vs == 0) vs = sections[i].SizeOfRawData;
        if (rva >= va && rva < va + vs) {
            return sections[i].PointerToRawData + (rva - va);
        }
    }
    return (DWORD)-1;
}

static uintptr_t mm_find_remote_module(DWORD pid, const char *name) {
    HANDLE snapshot;
    MODULEENTRY32W entry;
    uintptr_t result = 0;
    wchar_t wide_name[MAX_PATH];

    MultiByteToWideChar(CP_ACP, 0, name, -1, wide_name, MAX_PATH);
    snapshot = CreateToolhelp32Snapshot(
            TH32CS_SNAPMODULE | TH32CS_SNAPMODULE32, pid);
    if (snapshot == INVALID_HANDLE_VALUE) return 0;
    memset(&entry, 0, sizeof(entry));
    entry.dwSize = sizeof(entry);
    if (Module32FirstW(snapshot, &entry)) {
        do {
            if (_wcsicmp(entry.szModule, wide_name) == 0) {
                result = (uintptr_t)entry.modBaseAddr;
                break;
            }
        } while (Module32NextW(snapshot, &entry));
    }
    CloseHandle(snapshot);
    return result;
}

/* ---- remote export resolution ------------------------------------------ */

static uintptr_t mm_resolve_forward(HANDLE process, DWORD pid,
        const char *forward_str, int depth);

/* Forward declaration: defined below, needed by the LoadLibrary helper. */
static uintptr_t mm_resolve_remote_export(HANDLE process, DWORD pid,
        uintptr_t module_base, const char *func_name, WORD ordinal,
        int by_ordinal, int depth);

/* Load a dependency inside the target and return its remote base.
 *
 * The remote thread exit code (LoadLibraryA's HMODULE) is a DWORD and
 * truncates 64-bit base addresses, so it can only be used as a hint.
 * The authoritative answer always comes from a module snapshot, which
 * carries full-width addresses. Resolution order:
 *   1. exact name match after the load (normal DLLs, e.g. WINHTTP.dll);
 *   2. low-32-bit match of the exit code (API sets whose host, e.g.
 *      ucrtbase.dll, was already resident before the load);
 *   3. newly appeared module (API sets whose host got pulled in).
 * Returns 0 when nothing resolved. */
static uintptr_t mm_load_remote_library(HANDLE process, DWORD pid,
        const char *dll_name) {
    uintptr_t k32;
    uintptr_t load_library_a;
    LPVOID name_buf;
    HANDLE thread;
    DWORD exit_code = 0;
    size_t name_len;
    wchar_t wide_name[MAX_PATH];
    HANDLE snapshot;
    MODULEENTRY32W entry;
    uintptr_t *before_bases = NULL;
    wchar_t (*before_names)[MAX_PATH] = NULL;
    int before_count = 0;
    int before_capacity = 0;
    uintptr_t result = 0;
    int j;
    if (dll_name == NULL || dll_name[0] == '\0') return 0;
    name_len = strlen(dll_name) + 1;
    if (name_len > 260) return 0;
    MultiByteToWideChar(CP_ACP, 0, dll_name, -1, wide_name, MAX_PATH);
    k32 = mm_find_remote_module(pid,
            SV_STR(SV_7BEA49B86068A953, "kernel32.dll"));
    load_library_a = k32 ? mm_resolve_remote_export(process, pid,
            k32, SV_STR(SV_E05F658880BBCB93, "LoadLibraryA"),
            0, 0, 0) : 0;
    if (load_library_a == 0) return 0;

    /* Snapshot the target's modules before the load for the diff below. */
    snapshot = CreateToolhelp32Snapshot(
            TH32CS_SNAPMODULE | TH32CS_SNAPMODULE32, pid);
    if (snapshot != INVALID_HANDLE_VALUE) {
        memset(&entry, 0, sizeof(entry));
        entry.dwSize = sizeof(entry);
        if (Module32FirstW(snapshot, &entry)) {
            do {
                if (before_count >= before_capacity) {
                    int new_capacity = before_capacity == 0
                            ? 256 : before_capacity * 2;
                    uintptr_t *new_bases = (uintptr_t *)realloc(
                            before_bases,
                            (size_t)new_capacity * sizeof(uintptr_t));
                    wchar_t (*new_names)[MAX_PATH] = (wchar_t(*)[MAX_PATH])
                            realloc(before_names,
                            (size_t)new_capacity * sizeof(*new_names));
                    if (new_bases == NULL || new_names == NULL) break;
                    before_bases = new_bases;
                    before_names = new_names;
                    before_capacity = new_capacity;
                }
                if (before_count < before_capacity) {
                    before_bases[before_count] =
                            (uintptr_t)entry.modBaseAddr;
                    wcsncpy_s(before_names[before_count], MAX_PATH,
                            entry.szModule, _TRUNCATE);
                    ++before_count;
                }
            } while (Module32NextW(snapshot, &entry));
        }
        CloseHandle(snapshot);
    }

    name_buf = VirtualAllocEx(process, NULL, name_len,
            MEM_COMMIT | MEM_RESERVE, PAGE_READWRITE);
    if (name_buf == NULL) goto done;
    if (!mm_write(process, name_buf, dll_name, name_len)) {
        VirtualFreeEx(process, name_buf, 0, MEM_RELEASE);
        goto done;
    }
    thread = CreateRemoteThread(process, NULL, 0,
            (LPTHREAD_START_ROUTINE)load_library_a, name_buf, 0, NULL);
    if (thread == NULL) {
        VirtualFreeEx(process, name_buf, 0, MEM_RELEASE);
        goto done;
    }
    WaitForSingleObject(thread, 10000);
    GetExitCodeThread(thread, &exit_code);
    CloseHandle(thread);
    VirtualFreeEx(process, name_buf, 0, MEM_RELEASE);

    /* Re-scan with full-width addresses. */
    snapshot = CreateToolhelp32Snapshot(
            TH32CS_SNAPMODULE | TH32CS_SNAPMODULE32, pid);
    if (snapshot == INVALID_HANDLE_VALUE) goto done;
    memset(&entry, 0, sizeof(entry));
    entry.dwSize = sizeof(entry);
    if (Module32FirstW(snapshot, &entry)) {
        do {
            if (result == 0 && _wcsicmp(entry.szModule, wide_name) == 0) {
                /* Step 1: exact name match. Keep scanning so steps 2-3
                 * still see the full list. */
                result = (uintptr_t)entry.modBaseAddr;
            }
        } while (Module32NextW(snapshot, &entry));
    }
    if (result == 0 && exit_code != 0) {
        /* Step 2: exit-code low-32-bit match (API-set host already
         * resident). Rewind and compare full entries. */
        MODULEENTRY32W scan;
        memset(&scan, 0, sizeof(scan));
        scan.dwSize = sizeof(scan);
        if (Module32FirstW(snapshot, &scan)) {
            do {
                if (((uintptr_t)scan.modBaseAddr & 0xFFFFFFFFu)
                        == (uintptr_t)exit_code) {
                    result = (uintptr_t)scan.modBaseAddr;
                    break;
                }
            } while (Module32NextW(snapshot, &scan));
        }
    }
    if (result == 0) {
        /* Step 3: first module that was not there before the load. */
        MODULEENTRY32W scan;
        memset(&scan, 0, sizeof(scan));
        scan.dwSize = sizeof(scan);
        if (Module32FirstW(snapshot, &scan)) {
            do {
                int known = 0;
                for (j = 0; j < before_count; ++j) {
                    if (_wcsicmp(scan.szModule, before_names[j]) == 0) {
                        known = 1;
                        break;
                    }
                }
                if (!known) {
                    result = (uintptr_t)scan.modBaseAddr;
                    break;
                }
            } while (Module32NextW(snapshot, &scan));
        }
    }
    CloseHandle(snapshot);

done:
    free(before_bases);
    free(before_names);
    return result;
}

static uintptr_t mm_resolve_remote_export(HANDLE process, DWORD pid,
        uintptr_t module_base, const char *func_name, WORD ordinal,
        int by_ordinal, int depth) {
    IMAGE_DOS_HEADER dos;
    IMAGE_NT_HEADERS64 nt;
    IMAGE_EXPORT_DIRECTORY exports;
    DWORD export_rva, export_size;
    DWORD *names_rva = NULL, *functions_rva = NULL;
    WORD *ordinals_arr = NULL;
    char remote_name[256];
    uintptr_t result = 0;
    DWORD i;

    if (depth > 8) return 0;
    if (!mm_read(process, (void *)module_base, &dos, sizeof(dos))) return 0;
    if (dos.e_magic != IMAGE_DOS_SIGNATURE) return 0;
    if (!mm_read(process, (void *)(module_base + dos.e_lfanew),
            &nt, sizeof(nt))) return 0;
    if (nt.Signature != IMAGE_NT_SIGNATURE) return 0;

    if (nt.OptionalHeader.NumberOfRvaAndSizes <= IMAGE_DIRECTORY_ENTRY_EXPORT)
        return 0;
    export_rva = nt.OptionalHeader.DataDirectory[IMAGE_DIRECTORY_ENTRY_EXPORT]
            .VirtualAddress;
    export_size = nt.OptionalHeader.DataDirectory[IMAGE_DIRECTORY_ENTRY_EXPORT]
            .Size;
    if (export_rva == 0) return 0;

    if (!mm_read(process, (void *)(module_base + export_rva),
            &exports, sizeof(exports))) return 0;

    functions_rva = (DWORD *)malloc(exports.NumberOfFunctions * sizeof(DWORD));
    if (!functions_rva) return 0;
    if (!mm_read(process,
            (void *)(module_base + exports.AddressOfFunctions),
            functions_rva, exports.NumberOfFunctions * sizeof(DWORD))) {
        free(functions_rva);
        return 0;
    }

    if (by_ordinal) {
        DWORD index = ordinal - (WORD)exports.Base;
        if (index < exports.NumberOfFunctions) {
            DWORD func_rva = functions_rva[index];
            if (func_rva >= export_rva
                    && func_rva < export_rva + export_size) {
                char fwd[256];
                if (mm_read(process,
                        (void *)(module_base + func_rva),
                        fwd, sizeof(fwd))) {
                    fwd[sizeof(fwd) - 1] = '\0';
                    result = mm_resolve_forward(process, pid,
                            fwd, depth + 1);
                }
            } else {
                result = module_base + func_rva;
            }
        }
        free(functions_rva);
        return result;
    }

    names_rva = (DWORD *)malloc(exports.NumberOfNames * sizeof(DWORD));
    ordinals_arr = (WORD *)malloc(exports.NumberOfNames * sizeof(WORD));
    if (!names_rva || !ordinals_arr) goto export_cleanup;
    if (!mm_read(process, (void *)(module_base + exports.AddressOfNames),
            names_rva, exports.NumberOfNames * sizeof(DWORD))) {
        goto export_cleanup;
    }
    if (!mm_read(process,
            (void *)(module_base + exports.AddressOfNameOrdinals),
            ordinals_arr, exports.NumberOfNames * sizeof(WORD))) {
        goto export_cleanup;
    }

    for (i = 0; i < exports.NumberOfNames; ++i) {
        if (!mm_read(process, (void *)(module_base + names_rva[i]),
                remote_name, sizeof(remote_name))) continue;
        remote_name[sizeof(remote_name) - 1] = '\0';
        if (strcmp(remote_name, func_name) == 0) {
            DWORD func_index = ordinals_arr[i];
            if (func_index < exports.NumberOfFunctions) {
                DWORD func_rva = functions_rva[func_index];
                if (func_rva >= export_rva
                        && func_rva < export_rva + export_size) {
                    char fwd[256];
                    if (mm_read(process,
                            (void *)(module_base + func_rva),
                            fwd, sizeof(fwd))) {
                        fwd[sizeof(fwd) - 1] = '\0';
                        result = mm_resolve_forward(process, pid,
                                fwd, depth + 1);
                    }
                } else {
                    result = module_base + func_rva;
                }
            }
            break;
        }
    }

export_cleanup:
    free(functions_rva);
    free(names_rva);
    free(ordinals_arr);
    return result;
}

static uintptr_t mm_resolve_forward(HANDLE process, DWORD pid,
        const char *forward_str, int depth) {
    char dll_name[256];
    const char *dot;
    const char *func_part;
    uintptr_t fwd_base;
    size_t dll_len;

    dot = strchr(forward_str, '.');
    if (dot == NULL) return 0;
    dll_len = (size_t)(dot - forward_str);
    if (dll_len == 0 || dll_len >= sizeof(dll_name) - 5) return 0;

    memcpy(dll_name, forward_str, dll_len);
    dll_name[dll_len] = '\0';
    strcat_s(dll_name, sizeof(dll_name), SV_STR(SV_F21FFA15534C55B0, ".dll"));

    func_part = dot + 1;
    fwd_base = mm_find_remote_module(pid, dll_name);

    if (fwd_base == 0) {
        fwd_base = mm_load_remote_library(process, pid, dll_name);
    }
    if (fwd_base == 0) {
        mm_fail_import(dll_name, NULL);
        return 0;
    }

    if (func_part[0] == '#') {
        WORD ord = (WORD)atoi(func_part + 1);
        return mm_resolve_remote_export(process, pid, fwd_base,
                NULL, ord, 1, depth);
    }
    return mm_resolve_remote_export(process, pid, fwd_base,
            func_part, 0, 0, depth);
}

/* ---- PE section protections -------------------------------------------- */

static DWORD mm_section_protect(DWORD characteristics) {
    int r = (characteristics & IMAGE_SCN_MEM_READ) != 0;
    int w = (characteristics & IMAGE_SCN_MEM_WRITE) != 0;
    int x = (characteristics & IMAGE_SCN_MEM_EXECUTE) != 0;
    if (x && w) return PAGE_EXECUTE_READWRITE;
    if (x && r) return PAGE_EXECUTE_READ;
    if (x)      return PAGE_EXECUTE;
    if (w)      return PAGE_READWRITE;
    if (r)      return PAGE_READONLY;
    return PAGE_NOACCESS;
}

/* ---- API-set resolution ------------------------------------------------ */

static uintptr_t mm_resolve_apiset_module(DWORD pid, const char *dll_name) {
    if (_strnicmp(dll_name, SV_STR(SV_B5DE14709E4E0251, "api-ms-win-crt-"), 15) == 0)
        return mm_find_remote_module(pid, SV_STR(SV_71C168E71F8A6DAD, "ucrtbase.dll"));
    if (_strnicmp(dll_name, SV_STR(SV_60BC66FE878CD9BF, "api-ms-win-core-"), 16) == 0)
        return mm_find_remote_module(pid, SV_STR(SV_B993CC2CC384C807, "kernelbase.dll"));
    return 0;
}

/* ---- x64 DllMain-caller stub ------------------------------------------ */

#pragma pack(push, 1)
typedef struct {
    UINT64 dll_base;            /* +0x00 */
    UINT64 dll_main;            /* +0x08 */
    UINT64 fn_result;           /* +0x10  output */
    UINT64 fn_add_func_table;   /* +0x18  RtlAddFunctionTable (0=skip) */
    UINT64 pdata_addr;          /* +0x20  .pdata in remote */
    UINT32 pdata_count;         /* +0x28  RUNTIME_FUNCTION count */
    UINT32 _pad;                /* +0x2C */
    /* code at +0x30 */
    BYTE   code[64];
} mm_stub;
#pragma pack(pop)

/*
 * push rbx                 ; 53
 * sub  rsp, 0x20           ; 48 83 EC 20
 * mov  rbx, rcx            ; 48 89 CB
 *
 * ; RtlAddFunctionTable(pdata_addr, pdata_count, dll_base)
 * mov  rax, [rbx+0x18]     ; 48 8B 43 18   fn_add_func_table
 * test rax, rax             ; 48 85 C0
 * jz   skip (+0x0C)         ; 74 0C
 * mov  rcx, [rbx+0x20]     ; 48 8B 4B 20   pdata_addr
 * mov  edx, [rbx+0x28]     ; 8B 53 28      pdata_count
 * mov  r8,  [rbx]           ; 4C 8B 03      dll_base
 * call rax                  ; FF D0
 *
 * skip:
 * ; DllMain(dll_base, DLL_PROCESS_ATTACH, NULL)
 * mov  rcx, [rbx]           ; 48 8B 0B
 * mov  edx, 1               ; BA 01 00 00 00
 * xor  r8d, r8d             ; 45 31 C0
 * call [rbx+0x08]           ; FF 53 08
 *
 * mov  [rbx+0x10], rax      ; 48 89 43 10
 * add  rsp, 0x20            ; 48 83 C4 20
 * pop  rbx                  ; 5B
 * xor  eax, eax             ; 31 C0
 * ret                       ; C3
 */
static const BYTE mm_stub_code[] = {
    0x53,
    0x48, 0x83, 0xEC, 0x20,
    0x48, 0x89, 0xCB,
    0x48, 0x8B, 0x43, 0x18,
    0x48, 0x85, 0xC0,
    0x74, 0x0C,
    0x48, 0x8B, 0x4B, 0x20,
    0x8B, 0x53, 0x28,
    0x4C, 0x8B, 0x03,
    0xFF, 0xD0,
    0x48, 0x8B, 0x0B,
    0xBA, 0x01, 0x00, 0x00, 0x00,
    0x45, 0x31, 0xC0,
    0xFF, 0x53, 0x08,
    0x48, 0x89, 0x43, 0x10,
    0x48, 0x83, 0xC4, 0x20,
    0x5B,
    0x31, 0xC0,
    0xC3
};

/* ---- main entry -------------------------------------------------------- */

int manual_map_inject(DWORD process_id, HANDLE process,
        const unsigned char *dll_data, size_t dll_size) {
    const IMAGE_DOS_HEADER *dos;
    const IMAGE_NT_HEADERS64 *nt;
    const IMAGE_SECTION_HEADER *sections;
    WORD num_sections;
    DWORD hdr_size;
    LPVOID remote_base = NULL;
    LPVOID stub_mem = NULL;
    HANDLE thread = NULL;
    uintptr_t preferred_base;
    ptrdiff_t delta;
    DWORD i, old_prot;
    int result = 0;

    mm_log_open();
    g_mm_stage = 0;
    g_mm_module[0] = '\0';
    g_mm_function[0] = '\0';
    mm_log(SV_WSTR(SV_164C26A9881E6E3F, L"[manual-map] === START pid=%lu dll_size=%zu ===\n"),
            (unsigned long)process_id, dll_size);

    /* ---- 1. parse PE --------------------------------------------------- */
    g_mm_stage = 1;
    dos = (const IMAGE_DOS_HEADER *)dll_data;
    if (dll_size < sizeof(IMAGE_DOS_HEADER)
            || dos->e_magic != IMAGE_DOS_SIGNATURE) {
        mm_error(SV_WSTR(SV_0525B7BD5C4C9FFF, L"Invalid DOS header")); return 0;
    }
    nt = (const IMAGE_NT_HEADERS64 *)(dll_data + dos->e_lfanew);
    if ((size_t)dos->e_lfanew + sizeof(IMAGE_NT_HEADERS64) > dll_size
            || nt->Signature != IMAGE_NT_SIGNATURE
            || nt->FileHeader.Machine != IMAGE_FILE_MACHINE_AMD64
            || nt->OptionalHeader.Magic != IMAGE_NT_OPTIONAL_HDR64_MAGIC) {
        mm_error(SV_WSTR(SV_C9E538E6BAD7F981, L"Not a valid PE64 image")); return 0;
    }
    sections = (const IMAGE_SECTION_HEADER *)(
            (const BYTE *)&nt->OptionalHeader
            + nt->FileHeader.SizeOfOptionalHeader);
    num_sections = nt->FileHeader.NumberOfSections;
    hdr_size = nt->OptionalHeader.SizeOfHeaders;
    preferred_base = (uintptr_t)nt->OptionalHeader.ImageBase;
    mm_log(SV_WSTR(SV_15A7532577F0D98A, L"[manual-map] PE parsed: %u sections, ImageBase=0x%llx, SizeOfImage=0x%lx, EntryPoint=0x%lx\n"),
            num_sections, (unsigned long long)preferred_base,
            (unsigned long)nt->OptionalHeader.SizeOfImage,
            (unsigned long)nt->OptionalHeader.AddressOfEntryPoint);

    /* ---- 2. allocate in target ----------------------------------------- */
    g_mm_stage = 2;
    remote_base = VirtualAllocEx(process,
            (LPVOID)preferred_base,
            nt->OptionalHeader.SizeOfImage,
            MEM_COMMIT | MEM_RESERVE, PAGE_EXECUTE_READWRITE);
    if (!remote_base) {
        remote_base = VirtualAllocEx(process, NULL,
                nt->OptionalHeader.SizeOfImage,
                MEM_COMMIT | MEM_RESERVE, PAGE_EXECUTE_READWRITE);
    }
    if (!remote_base) {
        mm_error(SV_WSTR(SV_99503A1CADAF85E7, L"VirtualAllocEx failed for image")); return 0;
    }
    delta = (ptrdiff_t)((uintptr_t)remote_base - preferred_base);
    mm_log(SV_WSTR(SV_BB27AD0D9698BD27, L"[manual-map] Allocated at 0x%llx (delta=0x%llx)\n"),
            (unsigned long long)(uintptr_t)remote_base,
            (unsigned long long)delta);

    /* ---- 3. copy headers + sections ------------------------------------ */
    g_mm_stage = 3;
    if (!mm_write(process, remote_base, dll_data, hdr_size)) {
        mm_error(SV_WSTR(SV_8389561A1B80B21C, L"Failed to write PE headers")); goto cleanup;
    }
    for (i = 0; i < num_sections; ++i) {
        const IMAGE_SECTION_HEADER *sec = &sections[i];
        DWORD raw_size = sec->SizeOfRawData;
        if (raw_size == 0) continue;
        if (sec->PointerToRawData + raw_size > dll_size) {
            mm_error(SV_WSTR(SV_407256A4D5C33DFA, L"Section raw data out of bounds")); goto cleanup;
        }
        if (!mm_write(process,
                (BYTE *)remote_base + sec->VirtualAddress,
                dll_data + sec->PointerToRawData, raw_size)) {
            mm_error(SV_WSTR(SV_6D75AD1FB93BC56B, L"Failed to write section")); goto cleanup;
        }
    }

    mm_log(SV_WSTR(SV_418F394BA76B101A, L"[manual-map] Headers + %u sections copied OK\n"),
            num_sections);

    /* ---- 4. relocations ------------------------------------------------ */
    g_mm_stage = 4;
    if (delta != 0) {
        DWORD reloc_rva = 0, reloc_size = 0;
        if (nt->OptionalHeader.NumberOfRvaAndSizes
                > IMAGE_DIRECTORY_ENTRY_BASERELOC) {
            reloc_rva = nt->OptionalHeader
                    .DataDirectory[IMAGE_DIRECTORY_ENTRY_BASERELOC]
                    .VirtualAddress;
            reloc_size = nt->OptionalHeader
                    .DataDirectory[IMAGE_DIRECTORY_ENTRY_BASERELOC].Size;
        }
        if (reloc_rva == 0 || reloc_size == 0) {
            mm_error(SV_WSTR(SV_31447DCC956BB174, L"Image needs relocation but has no reloc table"));
            goto cleanup;
        } else {
            DWORD reloc_file_offset = rva_to_offset(reloc_rva,
                    sections, num_sections, hdr_size);
            DWORD offset = 0;
            if (reloc_file_offset == (DWORD)-1) {
                mm_error(SV_WSTR(SV_F92559727A0010E6, L"Cannot map relocation RVA to file offset"));
                goto cleanup;
            }
            while (offset < reloc_size) {
                const IMAGE_BASE_RELOCATION *block =
                        (const IMAGE_BASE_RELOCATION *)
                        (dll_data + reloc_file_offset + offset);
                DWORD entry_count;
                const WORD *entries;
                DWORD e;

                if (block->SizeOfBlock < sizeof(IMAGE_BASE_RELOCATION))
                    break;
                entry_count = (block->SizeOfBlock
                        - sizeof(IMAGE_BASE_RELOCATION)) / sizeof(WORD);
                entries = (const WORD *)((const BYTE *)block
                        + sizeof(IMAGE_BASE_RELOCATION));

                for (e = 0; e < entry_count; ++e) {
                    WORD type = entries[e] >> 12;
                    WORD rel_offset = entries[e] & 0xFFF;
                    uintptr_t patch_addr = (uintptr_t)remote_base
                            + block->VirtualAddress + rel_offset;

                    if (type == IMAGE_REL_BASED_DIR64) {
                        UINT64 value = 0;
                        if (!mm_read(process, (void *)patch_addr,
                                &value, sizeof(value))) {
                            mm_error(SV_WSTR(SV_0219189967650BEE, L"Failed to read reloc target"));
                            goto cleanup;
                        }
                        value = (UINT64)((ptrdiff_t)value + delta);
                        if (!mm_write(process, (void *)patch_addr,
                                &value, sizeof(value))) {
                            mm_error(SV_WSTR(SV_F1C8FE4AF69BF5CB, L"Failed to write reloc target"));
                            goto cleanup;
                        }
                    } else if (type == IMAGE_REL_BASED_HIGHLOW) {
                        DWORD value = 0;
                        if (!mm_read(process, (void *)patch_addr,
                                &value, sizeof(value))) goto cleanup;
                        value = (DWORD)((ptrdiff_t)value + delta);
                        if (!mm_write(process, (void *)patch_addr,
                                &value, sizeof(value))) goto cleanup;
                    } else if (type == IMAGE_REL_BASED_ABSOLUTE) {
                        /* padding, skip */
                    } else {
                        mm_log(
                                SV_WSTR(SV_74BD7909CC57B0AD, L"[manual-map] Unsupported reloc type %u\n"),
                                type);
                    }
                }
                offset += block->SizeOfBlock;
            }
        }
    }

    mm_log(SV_WSTR(SV_8BD8B9EDBB5577BD, L"[manual-map] Relocations %s\n"),
            delta != 0 ? SV_WSTR(SV_092707CE479B6BFD, L"applied") : SV_WSTR(SV_6C6017653929E1E4, L"skipped (delta=0)"));

    /* ---- 5. resolve imports -------------------------------------------- */
    g_mm_stage = 5;
    {
        DWORD import_rva = 0;
        DWORD import_file_offset;
        if (nt->OptionalHeader.NumberOfRvaAndSizes
                > IMAGE_DIRECTORY_ENTRY_IMPORT) {
            import_rva = nt->OptionalHeader
                    .DataDirectory[IMAGE_DIRECTORY_ENTRY_IMPORT]
                    .VirtualAddress;
        }
        if (import_rva != 0) {
            const IMAGE_IMPORT_DESCRIPTOR *imp;
            import_file_offset = rva_to_offset(import_rva,
                    sections, num_sections, hdr_size);
            if (import_file_offset == (DWORD)-1) {
                mm_fail_import(NULL, NULL);
                mm_error(SV_WSTR(SV_690D1FF241548C7D, L"Cannot map import RVA to file offset"));
                goto cleanup;
            }
            imp = (const IMAGE_IMPORT_DESCRIPTOR *)
                    (dll_data + import_file_offset);
            while (imp->Name != 0) {
                DWORD name_offset = rva_to_offset(imp->Name,
                        sections, num_sections, hdr_size);
                const char *dll_name;
                uintptr_t mod_base;
                const IMAGE_THUNK_DATA64 *orig_thunk;
                uintptr_t orig_rva, iat_rva;
                DWORD orig_file_offset;

                if (name_offset == (DWORD)-1) {
                    mm_fail_import(NULL, NULL);
                    mm_error(SV_WSTR(SV_2C7FADAACFF0B8F0, L"Cannot map import name RVA"));
                    goto cleanup;
                }
                dll_name = (const char *)(dll_data + name_offset);
                mod_base = mm_find_remote_module(process_id, dll_name);
                mm_log(SV_WSTR(SV_387C558A18E3ABB3, L"[manual-map] Import: %S -> remote=0x%llx\n"),
                        dll_name, (unsigned long long)mod_base);

                if (mod_base == 0) {
                    mod_base = mm_load_remote_library(process,
                            process_id, dll_name);
                }
                if (mod_base == 0) {
                    mod_base = mm_resolve_apiset_module(
                            process_id, dll_name);
                    if (mod_base != 0)
                        mm_log(SV_WSTR(SV_A8C68EFE1FB24FA0, L"[manual-map]   -> API set resolved to 0x%llx\n"),
                                (unsigned long long)mod_base);
                }
                if (mod_base == 0) {
                    mm_fail_import(dll_name, NULL);
                    mm_log(SV_WSTR(SV_B5E65ABA841B100E, L"[manual-map] Cannot find module: %S\n"),
                            dll_name);
                    goto cleanup;
                }

                orig_rva = imp->OriginalFirstThunk
                        ? imp->OriginalFirstThunk
                        : imp->FirstThunk;
                iat_rva = imp->FirstThunk;

                orig_file_offset = rva_to_offset((DWORD)orig_rva,
                        sections, num_sections, hdr_size);
                if (orig_file_offset == (DWORD)-1) {
                    mm_fail_import(dll_name, NULL);
                    mm_error(SV_WSTR(SV_2796703C3B53A35E, L"Cannot map thunk RVA to file offset"));
                    goto cleanup;
                }
                orig_thunk = (const IMAGE_THUNK_DATA64 *)
                        (dll_data + orig_file_offset);

                {
                    size_t iat_index = 0;
                    while (orig_thunk->u1.AddressOfData != 0) {
                        uintptr_t func_addr = 0;
                        uintptr_t iat_entry_addr = (uintptr_t)remote_base
                                + iat_rva
                                + iat_index * sizeof(IMAGE_THUNK_DATA64);
                        if (orig_thunk->u1.Ordinal
                                & IMAGE_ORDINAL_FLAG64) {
                            WORD ordinal = (WORD)(orig_thunk->u1.Ordinal
                                    & 0xFFFF);
                            func_addr = mm_resolve_remote_export(
                                    process, process_id,
                                    mod_base, NULL, ordinal, 1, 0);
                        } else {
                            DWORD hint_offset = rva_to_offset(
                                    (DWORD)orig_thunk->u1.AddressOfData,
                                    sections, num_sections, hdr_size);
                            const IMAGE_IMPORT_BY_NAME *hint;
                            if (hint_offset == (DWORD)-1) {
                                mm_fail_import(dll_name, NULL);
                                mm_error(SV_WSTR(SV_CB1B980E4A5AC04F, L"Cannot map hint RVA"));
                                goto cleanup;
                            }
                            hint = (const IMAGE_IMPORT_BY_NAME *)
                                    (dll_data + hint_offset);
                            func_addr = mm_resolve_remote_export(
                                    process, process_id,
                                    mod_base, hint->Name, 0, 0, 0);
                        }
                        if (func_addr == 0) {
                            if (orig_thunk->u1.Ordinal
                                    & IMAGE_ORDINAL_FLAG64) {
                                char ord_detail[96];
                                _snprintf_s(ord_detail, sizeof(ord_detail),
                                        _TRUNCATE, SV_STR(SV_094975365647E854, "%s!#%u"), dll_name,
                                        (unsigned)(orig_thunk->u1.Ordinal
                                        & 0xFFFF));
                                mm_fail_import(dll_name, ord_detail);
                                mm_log(
                                        SV_WSTR(SV_796803FA15BDACB1, L"[manual-map] Unresolved ordinal #%u in %S\n"),
                                        (unsigned)(orig_thunk->u1.Ordinal
                                        & 0xFFFF), dll_name);
                            } else {
                                DWORD h = rva_to_offset(
                                        (DWORD)orig_thunk->u1
                                        .AddressOfData,
                                        sections, num_sections,
                                        hdr_size);
                                const char *fn = (h != (DWORD)-1)
                                        ? ((const IMAGE_IMPORT_BY_NAME *)
                                        (dll_data + h))->Name
                                        : SV_STR(SV_81147249542A7E0A, "???");
                                char func_detail[160];
                                _snprintf_s(func_detail,
                                        sizeof(func_detail), _TRUNCATE,
                                        SV_STR(SV_B6B4916ED248C66F, "%s!%s"), dll_name, fn);
                                mm_fail_import(dll_name, func_detail);
                                mm_log(
                                        SV_WSTR(SV_CD2D3CB40EFF2D77, L"[manual-map] Unresolved import %S!%S\n"),
                                        dll_name, fn);
                            }
                            goto cleanup;
                        }
                        if (!mm_write(process,
                                (void *)iat_entry_addr,
                                &func_addr, sizeof(func_addr))) {
                            mm_error(SV_WSTR(SV_751A151435FEE741, L"Failed to write IAT entry"));
                            goto cleanup;
                        }
                        ++orig_thunk;
                        ++iat_index;
                    }
                }
                ++imp;
            }
        }
    }

    mm_log(SV_WSTR(SV_C481A9FB257CED04, L"[manual-map] All imports resolved OK\n"));

    /* ---- 6. set section protections ------------------------------------ */
    g_mm_stage = 6;
    for (i = 0; i < num_sections; ++i) {
        const IMAGE_SECTION_HEADER *sec = &sections[i];
        DWORD sec_size = sec->Misc.VirtualSize;
        DWORD prot = mm_section_protect(sec->Characteristics);
        if (sec_size == 0) continue;
        if (!VirtualProtectEx(process,
                (BYTE *)remote_base + sec->VirtualAddress,
                sec_size, prot, &old_prot)) {
            mm_log(SV_WSTR(SV_8652C23D5EC35B5E, L"[manual-map] VirtualProtectEx failed for section %.8S (prot=0x%lx, error %lu)\n"),
                    sec->Name, (unsigned long)prot,
                    (unsigned long)GetLastError());
        }
    }
    if (!VirtualProtectEx(process, remote_base, hdr_size,
            PAGE_READONLY, &old_prot)) {
        mm_log(SV_WSTR(SV_4F941FF8366270CE, L"[manual-map] VirtualProtectEx failed for headers\n"));
    }

    /* ---- 7. flush instruction cache ------------------------------------ */
    g_mm_stage = 7;
    FlushInstructionCache(process, remote_base,
            nt->OptionalHeader.SizeOfImage);
    mm_log(SV_WSTR(SV_374D9A98A2A61870, L"[manual-map] FlushInstructionCache done\n"));

    /* ---- 8. register .pdata for x64 exception handling ----------------- */
    g_mm_stage = 8;
    {
        UINT64 fn_add_func_table = 0;
        UINT64 pdata_remote = 0;
        DWORD pdata_count = 0;
        DWORD pdata_rva = 0, pdata_size = 0;

        if (nt->OptionalHeader.NumberOfRvaAndSizes
                > IMAGE_DIRECTORY_ENTRY_EXCEPTION) {
            pdata_rva = nt->OptionalHeader
                    .DataDirectory[IMAGE_DIRECTORY_ENTRY_EXCEPTION]
                    .VirtualAddress;
            pdata_size = nt->OptionalHeader
                    .DataDirectory[IMAGE_DIRECTORY_ENTRY_EXCEPTION].Size;
        }
        if (pdata_rva != 0 && pdata_size != 0) {
            uintptr_t ntdll_base = mm_find_remote_module(
                    process_id, SV_STR(SV_6837D120B7627608, "ntdll.dll"));
            pdata_remote = (UINT64)(uintptr_t)remote_base + pdata_rva;
            pdata_count = pdata_size / 12;
            if (ntdll_base != 0) {
                fn_add_func_table = (UINT64)mm_resolve_remote_export(
                        process, process_id, ntdll_base,
                        SV_STR(SV_B79FD2CE8732CAAE, "RtlAddFunctionTable"), 0, 0, 0);
            }
            mm_log(SV_WSTR(SV_389F44D201CD7C33, L"[manual-map] .pdata: %lu entries, RtlAddFunctionTable=0x%llx\n"),
                    (unsigned long)pdata_count,
                    (unsigned long long)fn_add_func_table);
        }

    /* ---- 9. call DllMain via stub -------------------------------------- */
    g_mm_stage = 9;
        {
            mm_stub stub;
            DWORD wait_result;
            uintptr_t entry_point = (uintptr_t)remote_base
                    + nt->OptionalHeader.AddressOfEntryPoint;

            memset(&stub, 0, sizeof(stub));
            stub.dll_base = (UINT64)(uintptr_t)remote_base;
            stub.dll_main = (UINT64)entry_point;
            stub.fn_result = 0;
            stub.fn_add_func_table = fn_add_func_table;
            stub.pdata_addr = pdata_remote;
            stub.pdata_count = pdata_count;
            memcpy(stub.code, mm_stub_code, sizeof(mm_stub_code));

            stub_mem = VirtualAllocEx(process, NULL, sizeof(stub),
                    MEM_COMMIT | MEM_RESERVE, PAGE_EXECUTE_READWRITE);
            if (!stub_mem) {
                mm_error(SV_WSTR(SV_A055313F9DF3080C, L"VirtualAllocEx failed for stub")); goto cleanup;
            }
            if (!mm_write(process, stub_mem, &stub, sizeof(stub))) {
                mm_error(SV_WSTR(SV_5F9B51464894C4BA, L"Failed to write stub")); goto cleanup;
            }

            mm_log(SV_WSTR(SV_B6F4FFCEAE1F7714, L"[manual-map] Stub at 0x%llx, DllMain=0x%llx, base=0x%llx\n"),
                    (unsigned long long)(uintptr_t)stub_mem,
                    (unsigned long long)entry_point,
                    (unsigned long long)(uintptr_t)remote_base);

            thread = CreateRemoteThread(process, NULL, 0,
                    (LPTHREAD_START_ROUTINE)((BYTE *)stub_mem
                            + offsetof(mm_stub, code)),
                    stub_mem, 0, NULL);
            if (!thread) {
                mm_error(SV_WSTR(SV_E1A6C83AAEEF25CE, L"CreateRemoteThread failed for DllMain"));
                goto cleanup;
            }
            mm_log(SV_WSTR(SV_EB982FAD0B6180D7, L"[manual-map] DllMain thread started, waiting...\n"));
            wait_result = WaitForSingleObject(thread, 30000);
            if (wait_result != WAIT_OBJECT_0) {
                mm_error(SV_WSTR(SV_3F47A61B421C57F0, L"DllMain did not return within 30 seconds"));
                goto cleanup;
            }

            {
                mm_stub result_check;
                if (mm_read(process, stub_mem, &result_check,
                        sizeof(result_check))) {
                    mm_log(SV_WSTR(SV_12D082C67EC14352, L"[manual-map] DllMain returned: %lld\n"),
                            (long long)result_check.fn_result);
                }
            }
        }
    }

    result = 1;
    mm_log(SV_WSTR(SV_4B661F678710C780, L"[manual-map] === SUCCESS ===\n"));

cleanup:
    if (!result) mm_log(SV_WSTR(SV_9C3A1F412A84D602, L"[manual-map] === FAILED ===\n"));
    mm_log_close();
    if (thread) CloseHandle(thread);
    if (stub_mem && process) {
        VirtualFreeEx(process, stub_mem, 0, MEM_RELEASE);
    }
    if (!result && remote_base && process) {
        VirtualFreeEx(process, remote_base, 0, MEM_RELEASE);
    }
    return result;
}

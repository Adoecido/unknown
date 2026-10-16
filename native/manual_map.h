#ifndef SONYVEGAS421_MANUAL_MAP_H
#define SONYVEGAS421_MANUAL_MAP_H

#include <windows.h>
#include <stddef.h>

/* Map a PE DLL into a remote process entirely from memory.
   No file is written to disk. DllMain is called with DLL_PROCESS_ATTACH.
   Returns 1 on success. */
int manual_map_inject(DWORD process_id, HANDLE process,
        const unsigned char *dll_data, size_t dll_size);

/* Phase the last manual_map_inject call reached: 1 parse, 2 allocate,
 * 3 sections, 4 relocations, 5 imports, 6 protections, 7 cache flush,
 * 8 exception table, 9 DllMain. Zero when nothing ran. */
int manual_map_last_stage(void);

/* Dotted module name (e.g. "MSVCP140.dll") and import name ("mod!func"
 * or "mod!#ordinal") that made stage 5 fail. Empty strings when the last
 * failure was not an import problem. */
const char *manual_map_last_module(void);
const char *manual_map_last_function(void);

#endif

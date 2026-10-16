#ifndef SONYVEGAS421_OBF_H
#define SONYVEGAS421_OBF_H

#include <stdint.h>
#include <stddef.h>
#include <wchar.h>

#ifdef __cplusplus
extern "C" {
#endif

const char *sonyvegas_obf_dec(
        const uint64_t *data, const uint64_t *keys,
        size_t block_count, size_t len);
const wchar_t *sonyvegas_obf_dec_wide(
        const uint64_t *data, const uint64_t *keys,
        size_t block_count, size_t len);

/* Wipes every per-thread decrypt pool slot. Call after network or IPC work
 * that asked SV_STR/SV_WSTR for protocol/host material: decoded plaintext
 * otherwise lingers in the TLS slots until the slots rotate. */
void sonyvegas_obf_scrub(void);

/* Generated before native compilation from SV_STR/SV_WSTR call sites. */
#include "obf_generated.h"

#ifdef __cplusplus
}
#endif

#endif

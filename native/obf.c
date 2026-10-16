#include "obf.h"
#include <windows.h>
#include <string.h>

#define SV_DECRYPT_POOL_SIZE 16
#define SV_MAX_DECRYPT_BYTES 1024

typedef struct {
    char pool[SV_DECRYPT_POOL_SIZE][SV_MAX_DECRYPT_BYTES + 1];
    size_t pool_index;
    wchar_t wide_pool[SV_DECRYPT_POOL_SIZE][SV_MAX_DECRYPT_BYTES / 2 + 1];
    size_t wide_pool_index;
} SvDecryptState;

static volatile LONG g_tls_init_done = 0;
static DWORD g_tls_index = TLS_OUT_OF_INDEXES;

static SvDecryptState *get_state(void) {
    SvDecryptState *state;
    LONG prev = InterlockedCompareExchange(&g_tls_init_done, 1, 0);
    if (prev == 0) {
        g_tls_index = TlsAlloc();
        InterlockedExchange(&g_tls_init_done, 2);
    } else {
        while (InterlockedCompareExchange(&g_tls_init_done, 2, 2) != 2)
            Sleep(1);
    }
    if (g_tls_index == TLS_OUT_OF_INDEXES) return NULL;
    state = (SvDecryptState *)TlsGetValue(g_tls_index);
    if (state == NULL) {
        state = (SvDecryptState *)HeapAlloc(
                GetProcessHeap(), HEAP_ZERO_MEMORY, sizeof(*state));
        if (state != NULL) TlsSetValue(g_tls_index, state);
    }
    return state;
}

const char *sonyvegas_obf_dec(
        const uint64_t *data, const uint64_t *keys,
        size_t block_count, size_t len) {
    SvDecryptState *state = get_state();
    char *buffer;
    size_t block;
    if (state == NULL) return "";
    if (len > SV_MAX_DECRYPT_BYTES) {
        len = SV_MAX_DECRYPT_BYTES;
    }
    buffer = state->pool[state->pool_index];
    state->pool_index = (state->pool_index + 1) % SV_DECRYPT_POOL_SIZE;
    SecureZeroMemory(buffer, SV_MAX_DECRYPT_BYTES + 1);
    for (block = 0; block < block_count && block * 8 < len; ++block) {
        uint64_t plain = (~data[block]) ^ keys[block];
        size_t offset = block * 8;
        size_t remaining = len - offset;
        size_t copy = remaining < sizeof(plain) ? remaining : sizeof(plain);
        memcpy(buffer + offset, &plain, copy);
    }
    buffer[len] = '\0';
    return buffer;
}

void sonyvegas_obf_scrub(void) {
    SvDecryptState *state = get_state();
    if (state == NULL) return;
    SecureZeroMemory(state->pool, sizeof(state->pool));
    SecureZeroMemory(state->wide_pool, sizeof(state->wide_pool));
}

const wchar_t *sonyvegas_obf_dec_wide(
        const uint64_t *data, const uint64_t *keys,
        size_t block_count, size_t len) {
    SvDecryptState *state = get_state();
    wchar_t *buffer;
    unsigned char *bytes;
    size_t byte_count;
    size_t block;
    size_t char_count;
    if (state == NULL) return L"";
    if (len > SV_MAX_DECRYPT_BYTES) {
        len = SV_MAX_DECRYPT_BYTES;
    }
    byte_count = len & ~(size_t)1;
    buffer = state->wide_pool[state->wide_pool_index];
    state->wide_pool_index =
            (state->wide_pool_index + 1) % SV_DECRYPT_POOL_SIZE;
    SecureZeroMemory(buffer, sizeof(state->wide_pool[0]));
    bytes = (unsigned char *)buffer;
    for (block = 0; block < block_count && block * 8 < byte_count; ++block) {
        uint64_t plain = (~data[block]) ^ keys[block];
        size_t offset = block * 8;
        size_t remaining = byte_count - offset;
        size_t copy = remaining < sizeof(plain) ? remaining : sizeof(plain);
        memcpy(bytes + offset, &plain, copy);
    }
    char_count = byte_count / sizeof(wchar_t);
    buffer[char_count] = L'\0';
    return buffer;
}

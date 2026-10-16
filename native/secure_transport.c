#ifndef WIN32_LEAN_AND_MEAN
#define WIN32_LEAN_AND_MEAN
#endif
#include <windows.h>
#ifndef SECURITY_WIN32
#define SECURITY_WIN32
#endif
#include <winsock2.h>
#include <ws2tcpip.h>
#include <security.h>
#include <schannel.h>
#include <wincrypt.h>
#include <bcrypt.h>

#include "secure_transport.h"
#include "obf.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#pragma comment(lib, "ws2_32.lib")
#pragma comment(lib, "secur32.lib")
#pragma comment(lib, "crypt32.lib")
#pragma comment(lib, "bcrypt.lib")

/* Custom error codes outside the Winsock/SSPI ranges. */
#define SV_ERR_PIN_MISMATCH 0xA0000001uL
#define SV_ERR_NO_CERT      0xA0000002uL
#define SV_ERR_HEAD_TOO_BIG 0xA0000003uL
#define SV_ERR_WSA_INIT     0xA0000005uL
#define SV_ERR_CONNECT      0xA0000006uL
#define SV_ERR_SOCKET       0xA0000007uL

#define SV_WIRE_BUFFER 24576
#define SV_HEAD_SCAN_MAX 16384
#define SV_HEAD_BUFFER (SV_HEAD_SCAN_MAX + 4096)
#define SV_TLS_PLAIN_CHUNK 16384
#define SV_DNS_NAME_MAX 256

typedef struct sv_stream {
    SOCKET socket;
    int use_tls;
    int context_valid;
    SecPkgContext_StreamSizes sizes;
    CtxtHandle context;
    unsigned char *tls_record;
    size_t tls_record_cap;
    unsigned char *plain;
    size_t plain_len;
    size_t plain_off;
    unsigned char *wire;
    size_t wire_len;
    size_t wire_off;
} sv_stream;

/* ── Winsock bootstrap ─────────────────────────────────────────────── */

static volatile LONG g_wsa_state = 0; /* 0 = idle, 1 = initializing, 2 = ready */

static int ensure_winsock(void) {
    WSADATA data;
    LONG previous = InterlockedCompareExchange(&g_wsa_state, 1, 0);
    while (previous == 1) {
        Sleep(1);
        previous = InterlockedCompareExchange(&g_wsa_state, 1, 0);
    }
    if (previous == 2) return 1;
    if (WSAStartup(MAKEWORD(2, 2), &data) != 0) {
        InterlockedExchange(&g_wsa_state, 0);
        return 0;
    }
    InterlockedExchange(&g_wsa_state, 2);
    return 1;
}

/* ── credential cache (Schannel) ───────────────────────────────────── */

/* Two outbound credentials are cached for the process lifetime: a manual
 * one for SPKI-pinned endpoints (no chain/name validation, no host text)
 * and an automatic one for public hosts validated by Schannel itself. */
typedef struct sv_credentials {
    SecHandle handle;
    volatile LONG state; /* 0 = idle, 1 = initializing, 2 = ready */
} sv_credentials;

static sv_credentials g_manual_creds;
static sv_credentials g_auto_creds;
static CRITICAL_SECTION g_cred_lock;
static volatile LONG g_cred_lock_state = 0;

static void cred_lock_init(void) {
    LONG previous = InterlockedCompareExchange(&g_cred_lock_state, 1, 0);
    if (previous == 0) {
        InitializeCriticalSection(&g_cred_lock);
        InterlockedExchange(&g_cred_lock_state, 2);
    } else {
        while (InterlockedCompareExchange(&g_cred_lock_state, 2, 2) != 2) {
            Sleep(1);
        }
    }
}

static int acquire_credential(sv_credentials *slot, int manual_validation) {
    SCHANNEL_CRED credential;
    TimeStamp expiry;
    SECURITY_STATUS status;
    cred_lock_init();
    EnterCriticalSection(&g_cred_lock);
    if (slot->state == 2) {
        LeaveCriticalSection(&g_cred_lock);
        return 1;
    }
    SecureZeroMemory(&credential, sizeof(credential));
    credential.dwVersion = SCHANNEL_CRED_VERSION;
    credential.dwFlags = SCH_CRED_NO_DEFAULT_CREDS
            | (manual_validation ? SCH_CRED_MANUAL_CRED_VALIDATION : 0);
    credential.grbitEnabledProtocols = SP_PROT_TLS1_2_CLIENT
            | SP_PROT_TLS1_3_CLIENT;
    status = AcquireCredentialsHandleA(NULL, UNISP_NAME_A,
            SECPKG_CRED_OUTBOUND, NULL, &credential, NULL, NULL,
            &slot->handle, &expiry);
    if (status != SEC_E_OK) {
        LeaveCriticalSection(&g_cred_lock);
        return 0;
    }
    InterlockedExchange(&slot->state, 2);
    LeaveCriticalSection(&g_cred_lock);
    return 1;
}

/* ── SPKI pin check ────────────────────────────────────────────────── */

static int sha256_bytes(const unsigned char *data, size_t size,
        unsigned char out[32]) {
    BCRYPT_ALG_HANDLE algorithm = NULL;
    BCRYPT_HASH_HANDLE hash = NULL;
    NTSTATUS status;
    int ok = 0;
    status = BCryptOpenAlgorithmProvider(&algorithm, BCRYPT_SHA256_ALGORITHM,
            NULL, 0);
    if (!BCRYPT_SUCCESS(status)) return 0;
    status = BCryptCreateHash(algorithm, &hash, NULL, 0, NULL, 0, 0);
    if (BCRYPT_SUCCESS(status)) {
        status = BCryptHashData(hash, (PUCHAR)data, (ULONG)size, 0);
        if (BCRYPT_SUCCESS(status)) {
            status = BCryptFinishHash(hash, out, 32, 0);
            ok = BCRYPT_SUCCESS(status) != 0;
        }
        BCryptDestroyHash(hash);
    }
    BCryptCloseAlgorithmProvider(algorithm, 0);
    return ok;
}

static int pins_match(const unsigned char *pins, int pin_count,
        const unsigned char digest[32]) {
    int pin;
    int result = 0;
    for (pin = 0; pin < pin_count; ++pin) {
        const unsigned char *expected = pins + (size_t)pin * 32;
        int index;
        int same = 1;
        int zero = 1;
        for (index = 0; index < 32; ++index) {
            same &= expected[index] == digest[index];
            zero &= expected[index] == 0;
        }
        if (!zero && same) result = 1;
    }
    return result;
}

/* Validates a pinned server: the certificate's SubjectPublicKeyInfo
 * SHA-256 must equal one of the caller's pins. No name, chain or expiry
 * check runs, so a private CA works without ever naming the host. */
static unsigned long validate_pinned_spki(sv_stream *stream,
        const unsigned char *pins, int pin_count) {
    PCCERT_CONTEXT certificate = NULL;
    unsigned char *encoded = NULL;
    DWORD encoded_size = 0;
    unsigned char digest[32];
    unsigned long result = SV_ERR_PIN_MISMATCH;
    if (QueryContextAttributesA(&stream->context,
            SECPKG_ATTR_REMOTE_CERT_CONTEXT, &certificate) != SEC_E_OK
            || certificate == NULL) {
        return SV_ERR_NO_CERT;
    }
    if (CryptEncodeObjectEx(X509_ASN_ENCODING | PKCS_7_ASN_ENCODING,
            X509_PUBLIC_KEY_INFO, &certificate->pCertInfo->SubjectPublicKeyInfo,
            CRYPT_ENCODE_ALLOC_FLAG, NULL, &encoded, &encoded_size)
            && encoded != NULL && encoded_size > 0 && encoded_size <= 65536) {
        if (sha256_bytes(encoded, encoded_size, digest)
                && pins_match(pins, pin_count, digest)) {
            result = 0;
        }
    }
    SecureZeroMemory(digest, sizeof(digest));
    if (encoded != NULL) {
        SecureZeroMemory(encoded, encoded_size);
        LocalFree(encoded);
    }
    CertFreeCertificateContext(certificate);
    return result;
}

/* ── socket helpers ────────────────────────────────────────────────── */

static int send_all(SOCKET socket, const unsigned char *data, size_t size) {
    size_t sent = 0;
    while (sent < size) {
        int produced = send(socket, (const char *)data + sent,
                (int)(size - sent > 65536 ? 65536 : size - sent), 0);
        if (produced == SOCKET_ERROR || produced <= 0) return 0;
        sent += (size_t)produced;
    }
    return 1;
}

/* Reads more ciphertext/raw bytes into the wire buffer. Returns 1 when at
 * least one byte arrived, 0 on EOF, and -1 on error/timeout. */
static int wire_refill(sv_stream *stream) {
    int produced;
    if (stream->wire_off > 0) {
        memmove(stream->wire, stream->wire + stream->wire_off,
                stream->wire_len - stream->wire_off);
        stream->wire_len -= stream->wire_off;
        stream->wire_off = 0;
    }
    if (stream->wire_len >= SV_WIRE_BUFFER) {
        WSASetLastError(WSAEMSGSIZE);
        return -1;
    }
    produced = recv(stream->socket, (char *)stream->wire + stream->wire_len,
            (int)(SV_WIRE_BUFFER - stream->wire_len), 0);
    if (produced == SOCKET_ERROR) {
        int error = WSAGetLastError();
        if (error == WSAECONNRESET) return 0;
        return -1;
    }
    if (produced == 0) return 0;
    stream->wire_len += (size_t)produced;
    return 1;
}

static int connect_with_timeout(SOCKET socket, const struct sockaddr *address,
        int address_size, unsigned int timeout_ms) {
    unsigned long non_blocking = 1;
    unsigned long blocking = 0;
    fd_set write_set;
    fd_set error_set;
    struct timeval timeout;
    int select_result;
    int pending_error = 0;
    int pending_size = sizeof(pending_error);
    ioctlsocket(socket, FIONBIO, &non_blocking);
    if (connect(socket, address, address_size) == 0) {
        ioctlsocket(socket, FIONBIO, &blocking);
        return 1;
    }
    if (WSAGetLastError() != WSAEWOULDBLOCK) {
        ioctlsocket(socket, FIONBIO, &blocking);
        return 0;
    }
    FD_ZERO(&write_set);
    FD_ZERO(&error_set);
    FD_SET(socket, &write_set);
    FD_SET(socket, &error_set);
    timeout.tv_sec = (long)(timeout_ms / 1000u);
    timeout.tv_usec = (long)((timeout_ms % 1000u) * 1000u);
    select_result = select((int)socket + 1, NULL, &write_set, &error_set,
            &timeout);
    ioctlsocket(socket, FIONBIO, &blocking);
    if (select_result <= 0) return 0;
    if (getsockopt(socket, SOL_SOCKET, SO_ERROR, (char *)&pending_error,
            &pending_size) != 0 || pending_error != 0) {
        return 0;
    }
    return 1;
}

/* ── stream lifecycle ──────────────────────────────────────────────── */

static void stream_init(sv_stream *stream) {
    SecureZeroMemory(stream, sizeof(*stream));
    stream->socket = INVALID_SOCKET;
}

static void stream_close(sv_stream *stream) {
    if (stream->socket != INVALID_SOCKET) {
        shutdown(stream->socket, SD_BOTH);
        closesocket(stream->socket);
        stream->socket = INVALID_SOCKET;
    }
    if (stream->context_valid) {
        DeleteSecurityContext(&stream->context);
        stream->context_valid = 0;
    }
    if (stream->wire != NULL) {
        SecureZeroMemory(stream->wire, SV_WIRE_BUFFER);
        free(stream->wire);
        stream->wire = NULL;
    }
    if (stream->plain != NULL) {
        SecureZeroMemory(stream->plain, SV_TLS_PLAIN_CHUNK);
        free(stream->plain);
        stream->plain = NULL;
    }
    if (stream->tls_record != NULL) {
        SecureZeroMemory(stream->tls_record, stream->tls_record_cap);
        free(stream->tls_record);
        stream->tls_record = NULL;
    }
}

/* ── TLS handshake ─────────────────────────────────────────────────── */

static unsigned long tls_handshake(sv_stream *stream, SecHandle *credential,
        const char *target_name, int manual_validation) {
    SecBufferDesc input;
    SecBufferDesc output;
    SecBuffer input_buffers[2];
    SecBuffer output_buffers[1];
    SECURITY_STATUS status;
    unsigned long attributes = 0;
    TimeStamp expiry;
    int first_leg = 1;
    int have_input = 0;
    for (;;) {
        size_t previous_wire_off = stream->wire_off;
        size_t previous_wire_len = stream->wire_len;
        output_buffers[0].BufferType = SECBUFFER_TOKEN;
        output_buffers[0].cbBuffer = 0;
        output_buffers[0].pvBuffer = NULL;
        output.pBuffers = output_buffers;
        output.cBuffers = 1;
        output.ulVersion = SECBUFFER_VERSION;
        if (have_input) {
            input_buffers[0].BufferType = SECBUFFER_TOKEN;
            input_buffers[0].cbBuffer =
                    (ULONG)(stream->wire_len - stream->wire_off);
            input_buffers[0].pvBuffer = stream->wire + stream->wire_off;
            input_buffers[1].BufferType = SECBUFFER_EMPTY;
            input_buffers[1].cbBuffer = 0;
            input_buffers[1].pvBuffer = NULL;
            input.pBuffers = input_buffers;
            input.cBuffers = 2;
            input.ulVersion = SECBUFFER_VERSION;
        }
        status = InitializeSecurityContextA(credential,
                first_leg ? NULL : &stream->context, (SEC_CHAR *)target_name,
                ISC_REQ_ALLOCATE_MEMORY | ISC_REQ_CONFIDENTIALITY
                        | ISC_REQ_REPLAY_DETECT | ISC_REQ_SEQUENCE_DETECT
                        | ISC_REQ_STREAM
                        | (manual_validation
                                ? ISC_REQ_MANUAL_CRED_VALIDATION : 0),
                0, SECURITY_NATIVE_DREP, have_input ? &input : NULL, 0,
                &stream->context, &output, &attributes, &expiry);
        first_leg = 0;
        if (output_buffers[0].pvBuffer != NULL
                && output_buffers[0].cbBuffer > 0) {
            if (!send_all(stream->socket,
                        (const unsigned char *)output_buffers[0].pvBuffer,
                        output_buffers[0].cbBuffer)) {
                FreeContextBuffer(output_buffers[0].pvBuffer);
                return SV_ERR_SOCKET;
            }
            FreeContextBuffer(output_buffers[0].pvBuffer);
        }
        if (status == SEC_E_OK) {
            if (have_input) {
                if (input_buffers[1].BufferType == SECBUFFER_EXTRA) {
                    stream->wire_off = stream->wire_len
                            - input_buffers[1].cbBuffer;
                } else {
                    stream->wire_off = stream->wire_len;
                }
            }
            break;
        }
        if (status == SEC_I_CONTINUE_NEEDED
                || status == SEC_E_INCOMPLETE_MESSAGE) {
            int produced;
            if (status == SEC_I_CONTINUE_NEEDED && have_input
                    && input_buffers[1].BufferType == SECBUFFER_EXTRA) {
                stream->wire_off = stream->wire_len
                        - input_buffers[1].cbBuffer;
            } else if (status == SEC_I_CONTINUE_NEEDED) {
                stream->wire_off = stream->wire_len;
            }
            have_input = 1;
            if (stream->wire_off >= stream->wire_len
                    || (stream->wire_off == previous_wire_off
                            && stream->wire_len == previous_wire_len)) {
                produced = wire_refill(stream);
                if (produced != 1) {
                    return produced == 0 ? WSAECONNABORTED
                            : WSAGetLastError();
                }
            }
            continue;
        }
        return (unsigned long)status;
    }
    if (QueryContextAttributesA(&stream->context, SECPKG_ATTR_STREAM_SIZES,
                &stream->sizes) != SEC_E_OK) {
        return SV_ERR_SOCKET;
    }
    stream->context_valid = 1;
    return 0;
}

/* ── TLS app data ──────────────────────────────────────────────────── */

static unsigned long tls_send(sv_stream *stream, const unsigned char *data,
        size_t size) {
    size_t offset = 0;
    if (stream->tls_record == NULL) {
        size_t capacity = stream->sizes.cbHeader + SV_TLS_PLAIN_CHUNK
                + stream->sizes.cbTrailer;
        stream->tls_record = (unsigned char *)malloc(capacity);
        if (stream->tls_record == NULL) return SV_ERR_SOCKET;
        stream->tls_record_cap = capacity;
    }
    while (offset < size) {
        SecBuffer buffers[4];
        SecBufferDesc message;
        SECURITY_STATUS status;
        size_t chunk = size - offset;
        unsigned char *data_slot;
        unsigned long total;
        if (chunk > SV_TLS_PLAIN_CHUNK) chunk = SV_TLS_PLAIN_CHUNK;
        data_slot = stream->tls_record + stream->sizes.cbHeader;
        memcpy(data_slot, data + offset, chunk);
        buffers[0].BufferType = SECBUFFER_STREAM_HEADER;
        buffers[0].cbBuffer = stream->sizes.cbHeader;
        buffers[0].pvBuffer = stream->tls_record;
        buffers[1].BufferType = SECBUFFER_DATA;
        buffers[1].cbBuffer = (ULONG)chunk;
        buffers[1].pvBuffer = data_slot;
        buffers[2].BufferType = SECBUFFER_STREAM_TRAILER;
        buffers[2].cbBuffer = stream->sizes.cbTrailer;
        buffers[2].pvBuffer = data_slot + chunk;
        buffers[3].BufferType = SECBUFFER_EMPTY;
        buffers[3].cbBuffer = 0;
        buffers[3].pvBuffer = NULL;
        message.ulVersion = SECBUFFER_VERSION;
        message.cBuffers = 4;
        message.pBuffers = buffers;
        status = EncryptMessage(&stream->context, 0, &message, 0);
        if (status != SEC_E_OK) return (unsigned long)status;
        total = (unsigned long)buffers[0].cbBuffer
                + (unsigned long)buffers[1].cbBuffer
                + (unsigned long)buffers[2].cbBuffer;
        if (!send_all(stream->socket, stream->tls_record, total)) {
            return SV_ERR_SOCKET;
        }
        offset += chunk;
    }
    return 0;
}

/* Decrypts until plaintext is available. 1 = data ready, 0 = graceful
 * close, -1 = error. Zero-length records (TLS 1.3 tickets) are consumed. */
static int tls_fill_plain(sv_stream *stream) {
    for (;;) {
        SecBuffer buffers[4];
        SecBufferDesc message;
        SECURITY_STATUS status;
        int index;
        int data_index = -1;
        int extra_index = -1;
        size_t previous_wire_off = stream->wire_off;
        size_t previous_wire_len = stream->wire_len;
        if (stream->wire_off >= stream->wire_len) {
            int produced = wire_refill(stream);
            if (produced != 1) return produced;
            continue;
        }
        buffers[0].BufferType = SECBUFFER_DATA;
        buffers[0].cbBuffer = (ULONG)(stream->wire_len - stream->wire_off);
        buffers[0].pvBuffer = stream->wire + stream->wire_off;
        for (index = 1; index < 4; ++index) {
            buffers[index].BufferType = SECBUFFER_EMPTY;
            buffers[index].cbBuffer = 0;
            buffers[index].pvBuffer = NULL;
        }
        message.ulVersion = SECBUFFER_VERSION;
        message.cBuffers = 4;
        message.pBuffers = buffers;
        status = DecryptMessage(&stream->context, &message, 0, NULL);
        if (status == SEC_E_OK) {
            size_t plain_size = 0;
            for (index = 0; index < 4; ++index) {
                if (buffers[index].BufferType == SECBUFFER_DATA
                        && data_index < 0) {
                    data_index = index;
                } else if (buffers[index].BufferType == SECBUFFER_EXTRA
                        && extra_index < 0) {
                    extra_index = index;
                }
            }
            if (extra_index >= 0) {
                stream->wire_off =
                        stream->wire_len - buffers[extra_index].cbBuffer;
            } else {
                stream->wire_off = stream->wire_len;
            }
            if (data_index >= 0) plain_size = buffers[data_index].cbBuffer;
            if (plain_size > 0) {
                if (stream->plain == NULL) {
                    stream->plain =
                            (unsigned char *)malloc(SV_TLS_PLAIN_CHUNK);
                    if (stream->plain == NULL) return -1;
                }
                if (plain_size > SV_TLS_PLAIN_CHUNK) {
                    plain_size = SV_TLS_PLAIN_CHUNK;
                }
                memcpy(stream->plain,
                        (const unsigned char *)buffers[data_index].pvBuffer,
                        plain_size);
                stream->plain_len = plain_size;
                stream->plain_off = 0;
                return 1;
            }
            if (stream->wire_off == previous_wire_off
                    && stream->wire_len == previous_wire_len) {
                int produced = wire_refill(stream);
                if (produced != 1) return produced;
            }
            continue;
        }
        if (status == SEC_E_INCOMPLETE_MESSAGE) {
            int produced = wire_refill(stream);
            if (produced != 1) return produced;
            continue;
        }
        if (status == SEC_I_CONTEXT_EXPIRED) return 0;
        return -1;
    }
}

/* Reads up to `want` bytes; returns the number of bytes actually read.
 * Short reads happen at end of stream or on receive timeout. */
static size_t sv_read(sv_stream *stream, unsigned char *destination,
        size_t want) {
    size_t filled = 0;
    if (want == 0) return 0;
    if (!stream->use_tls) {
        while (filled < want) {
            int produced = recv(stream->socket,
                    (char *)destination + filled, (int)(want - filled), 0);
            if (produced == SOCKET_ERROR || produced == 0) break;
            filled += (size_t)produced;
        }
        return filled;
    }
    while (filled < want) {
        size_t available;
        size_t copy;
        int produced;
        if (stream->plain == NULL || stream->plain_off >= stream->plain_len) {
            produced = tls_fill_plain(stream);
            if (produced != 1) break;
        }
        available = stream->plain_len - stream->plain_off;
        copy = want - filled < available ? want - filled : available;
        memcpy(destination + filled, stream->plain + stream->plain_off, copy);
        stream->plain_off += copy;
        filled += copy;
    }
    return filled;
}

/* ── HTTP helpers ──────────────────────────────────────────────────── */

static int ascii_equal_ci(const unsigned char *left, size_t left_len,
        const unsigned char *right, size_t right_len) {
    size_t index;
    if (left_len != right_len) return 0;
    for (index = 0; index < left_len; ++index) {
        unsigned char a = left[index];
        unsigned char b = right[index];
        if (a >= 'A' && a <= 'Z') a = (unsigned char)(a + 32);
        if (b >= 'A' && b <= 'Z') b = (unsigned char)(b + 32);
        if (a != b) return 0;
    }
    return 1;
}

/* Locates "name: value" (case-insensitive) inside the raw head and
 * returns the value span. Returns 0 when absent. */
static int header_span(const unsigned char *head, size_t head_len,
        const unsigned char *name, size_t name_len, size_t *value_offset,
        size_t *value_len) {
    size_t index = 0;
    while (index < head_len) {
        size_t line_end = index;
        size_t cursor;
        while (line_end < head_len && head[line_end] != '\n') ++line_end;
        cursor = index;
        while (cursor < head_len && head[cursor] != ':'
                && head[cursor] != '\r' && head[cursor] != '\n') {
            ++cursor;
        }
        if (cursor < head_len && head[cursor] == ':'
                && ascii_equal_ci(head + index, cursor - index, name,
                        name_len)) {
            size_t value_start = cursor + 1;
            size_t value_end = line_end < head_len ? line_end : head_len;
            while (value_start < head_len
                    && (head[value_start] == ' '
                        || head[value_start] == '\t')) {
                ++value_start;
            }
            if (value_end > value_start && head[value_end - 1] == '\r') {
                --value_end;
            }
            while (value_end > value_start
                    && (head[value_end - 1] == ' '
                        || head[value_end - 1] == '\t')) {
                --value_end;
            }
            *value_offset = value_start;
            *value_len = value_end - value_start;
            return 1;
        }
        if (line_end >= head_len) break;
        index = line_end + 1;
    }
    return 0;
}

static int hex_digit_value(unsigned char character) {
    if (character >= '0' && character <= '9') return character - '0';
    if (character >= 'a' && character <= 'f') return character - 'a' + 10;
    if (character >= 'A' && character <= 'F') return character - 'A' + 10;
    return -1;
}

typedef struct sv_reader {
    sv_stream *stream;
    unsigned char buffer[4096];
    size_t buffer_len;
    size_t buffer_off;
} sv_reader;

/* Pulls more bytes into the reader scratch. 1 = bytes available. */
static int reader_fill(sv_reader *reader) {
    size_t produced;
    if (reader->buffer_off > 0) {
        memmove(reader->buffer, reader->buffer + reader->buffer_off,
                reader->buffer_len - reader->buffer_off);
        reader->buffer_len -= reader->buffer_off;
        reader->buffer_off = 0;
    }
    if (reader->buffer_len >= sizeof(reader->buffer)) return 1;
    produced = sv_read(reader->stream,
            reader->buffer + reader->buffer_len,
            sizeof(reader->buffer) - reader->buffer_len);
    reader->buffer_len += produced;
    return reader->buffer_len > reader->buffer_off ? 1 : 0;
}

/* Copies up to `want` bytes; returns the number of bytes copied. */
static size_t reader_take(sv_reader *reader, unsigned char *destination,
        size_t want) {
    size_t filled = 0;
    while (filled < want) {
        size_t available;
        size_t copy;
        if (reader->buffer_off >= reader->buffer_len) {
            if (!reader_fill(reader)) break;
            if (reader->buffer_off >= reader->buffer_len) break;
        }
        available = reader->buffer_len - reader->buffer_off;
        copy = want - filled < available ? want - filled : available;
        memcpy(destination + filled, reader->buffer + reader->buffer_off,
                copy);
        reader->buffer_off += copy;
        filled += copy;
    }
    return filled;
}

/* Reads one CRLF (or LF) terminated line into destination (without the
 * terminator). Returns the line length, -1 on EOF or overflow. */
static int reader_line(sv_reader *reader, unsigned char *destination,
        size_t capacity) {
    size_t filled = 0;
    for (;;) {
        unsigned char byte;
        if (reader_take(reader, &byte, 1) == 0) {
            return filled > 0 ? (int)filled : -1;
        }
        if (byte == '\n') {
            if (filled > 0 && destination[filled - 1] == '\r') {
                return (int)filled - 1;
            }
            return (int)filled;
        }
        if (filled >= capacity) return -1;
        destination[filled++] = byte;
    }
}

int sv_request_perform(const sv_request *request, sv_response *response) {
    sv_stream stream;
    sv_reader reader;
    char host_name[SV_DNS_NAME_MAX];
    unsigned char head[SV_HEAD_BUFFER];
    unsigned char *body_buffer = NULL;
    size_t body_capacity = 0;
    size_t body_total = 0;
    size_t head_len = 0;
    unsigned long error = 0;
    int use_post;
    const unsigned char *method = NULL;
    size_t method_len = 0;
    int ok = 0;

    SecureZeroMemory(response, sizeof(*response));
    SecureZeroMemory(host_name, sizeof(host_name));
    SecureZeroMemory(head, sizeof(head));
    stream_init(&stream);
    SecureZeroMemory(&reader, sizeof(reader));
    reader.stream = &stream;

    if (request == NULL || request->target == NULL || request->port == 0) {
        response->error = ERROR_INVALID_PARAMETER;
        return 0;
    }
    if (request->target_len == 0 || request->target_len > 2048) {
        response->error = ERROR_INVALID_PARAMETER;
        return 0;
    }
    if (request->body == NULL && request->body_len > 0) {
        response->error = ERROR_INVALID_PARAMETER;
        return 0;
    }
    if (!ensure_winsock()) {
        response->error = SV_ERR_WSA_INIT;
        return 0;
    }
    use_post = request->body != NULL && request->body_len > 0;
    if (request->body_len > 64u * 1024u * 1024u) {
        response->error = ERROR_INVALID_PARAMETER;
        return 0;
    }
    if (request->method == NULL || request->method_len == 0
            || request->method_len > 16) {
        method = NULL;
        method_len = 0;
    } else {
        method = request->method;
        method_len = request->method_len;
    }
    if (method == NULL) {
        method = use_post
                ? (const unsigned char *)SV_STR(SV_8C5FE0AFBC11BBC2, "POST")
                : (const unsigned char *)SV_STR(SV_5317F41AA1903D00, "GET");
        method_len = strlen((const char *)method);
    }

    /* Resolve and connect. Binary hosts never touch the resolver. */
    {
        struct addrinfo hints;
        struct addrinfo *result_chain = NULL;
        if (request->host != NULL && request->host_len > 0) {
            struct addrinfo *candidate;
            size_t copy_len = request->host_len;
            if (copy_len > SV_DNS_NAME_MAX - 1) {
                response->error = ERROR_INVALID_PARAMETER;
                return 0;
            }
            memcpy(host_name, request->host, copy_len);
            host_name[copy_len] = '\0';
            SecureZeroMemory(&hints, sizeof(hints));
            hints.ai_family = AF_INET;
            hints.ai_socktype = SOCK_STREAM;
            hints.ai_protocol = IPPROTO_TCP;
            if (getaddrinfo(host_name, NULL, &hints, &result_chain) != 0
                    || result_chain == NULL) {
                response->error = WSAHOST_NOT_FOUND;
                SecureZeroMemory(host_name, sizeof(host_name));
                return 0;
            }
            for (candidate = result_chain; candidate != NULL;
                    candidate = candidate->ai_next) {
                SOCKET attempt = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
                struct sockaddr_in *sin;
                if (attempt == INVALID_SOCKET) continue;
                sin = (struct sockaddr_in *)candidate->ai_addr;
                sin->sin_port = htons(request->port);
                if (connect_with_timeout(attempt, candidate->ai_addr,
                            (int)candidate->ai_addrlen,
                            request->connect_timeout_ms)) {
                    stream.socket = attempt;
                    break;
                }
                closesocket(attempt);
            }
            freeaddrinfo(result_chain);
        } else {
            SOCKET attempt = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
            if (attempt != INVALID_SOCKET) {
                struct sockaddr_in address;
                SecureZeroMemory(&address, sizeof(address));
                address.sin_family = AF_INET;
                address.sin_addr.s_addr = htonl(request->host_ip);
                address.sin_port = htons(request->port);
                if (connect_with_timeout(attempt,
                            (const struct sockaddr *)&address,
                            sizeof(address), request->connect_timeout_ms)) {
                    stream.socket = attempt;
                } else {
                    closesocket(attempt);
                }
            }
        }
        if (stream.socket == INVALID_SOCKET) {
            response->error = SV_ERR_CONNECT;
            SecureZeroMemory(host_name, sizeof(host_name));
            return 0;
        }
        {
            DWORD send_timeout = request->send_timeout_ms;
            DWORD recv_timeout = request->recv_timeout_ms;
            setsockopt(stream.socket, SOL_SOCKET, SO_SNDTIMEO,
                    (const char *)&send_timeout, sizeof(send_timeout));
            setsockopt(stream.socket, SOL_SOCKET, SO_RCVTIMEO,
                    (const char *)&recv_timeout, sizeof(recv_timeout));
        }
    }

    stream.wire = (unsigned char *)malloc(SV_WIRE_BUFFER);
    if (stream.wire == NULL) {
        response->error = ERROR_NOT_ENOUGH_MEMORY;
        goto cleanup;
    }
    SecureZeroMemory(stream.wire, SV_WIRE_BUFFER);

    if (request->use_tls) {
        SecHandle *credential;
        const char *target_name;
        int manual_validation = request->pins != NULL
                && request->pin_count > 0;
        if (!acquire_credential(
                    manual_validation ? &g_manual_creds : &g_auto_creds,
                    manual_validation)) {
            response->error = SV_ERR_NO_CERT;
            goto cleanup;
        }
        credential = manual_validation ? &g_manual_creds.handle
                : &g_auto_creds.handle;
        /* Pinned endpoints use a fixed dummy name so no host text ever
         * enters the TLS layer; public hosts validate against their name. */
        target_name = manual_validation ? SV_STR(SV_7711D9454ED49AEA, "a") : host_name;
        error = tls_handshake(&stream, credential, target_name,
                manual_validation);
        if (error != 0) {
            response->error = error;
            goto cleanup;
        }
        if (manual_validation) {
            error = validate_pinned_spki(&stream, request->pins,
                    request->pin_count);
            if (error != 0) {
                response->error = error;
                goto cleanup;
            }
        }
        stream.use_tls = 1;
    }

    /* External HTTP/1.1 requests need Host. The in-game loopback server
     * identifies native requests by the absence of Host and rejects the
     * generic value used for pinned TLS requests. */
    {
        head_len = (size_t)snprintf((char *)head, SV_HEAD_BUFFER,
                SV_STR(SV_5CDF9F148C1570DA, "%s %.*s HTTP/1.1\r\n"),
                (const char *)method, (int)request->target_len,
                (const char *)request->target);
        if (request->host != NULL && request->host_len > 0) {
            head_len += (size_t)snprintf((char *)head + head_len,
                    SV_HEAD_BUFFER - head_len, SV_STR(SV_C213D5660C32549B, "Host: %s:%u\r\n"), host_name,
                    request->port);
        } else if (request->use_tls) {
            head_len += (size_t)snprintf((char *)head + head_len,
                    SV_HEAD_BUFFER - head_len,
                    SV_STR(SV_AD15BD52D961D64B, "Host: a\r\n"));
        }
        if (use_post) {
            head_len += (size_t)snprintf((char *)head + head_len,
                    SV_HEAD_BUFFER - head_len, SV_STR(SV_201225F2BD6CEF1B, "Content-Length: %zu\r\n"),
                    request->body_len);
            if (request->content_type != NULL
                    && request->content_type_len > 0) {
                head_len += (size_t)snprintf((char *)head + head_len,
                        SV_HEAD_BUFFER - head_len, SV_STR(SV_EFFB91014F7B7D1F, "Content-Type: %.*s\r\n"),
                        (int)(request->content_type_len > 256
                                ? 256 : request->content_type_len),
                        (const char *)request->content_type);
            }
        }
        head_len += (size_t)snprintf((char *)head + head_len,
                SV_HEAD_BUFFER - head_len, SV_STR(SV_4C4E3E8A021652AE, "Connection: close\r\n\r\n"));
        if (head_len >= SV_HEAD_BUFFER - 64) {
            response->error = SV_ERR_HEAD_TOO_BIG;
            goto cleanup;
        }
    }

    if (stream.use_tls) {
        error = tls_send(&stream, head, head_len);
        if (error == 0 && use_post) {
            error = tls_send(&stream, request->body, request->body_len);
        }
        if (error != 0) {
            response->error = error;
            goto cleanup;
        }
    } else {
        if (!send_all(stream.socket, head, head_len)) {
            response->error = SV_ERR_SOCKET;
            goto cleanup;
        }
        if (use_post && !send_all(stream.socket, request->body,
                    request->body_len)) {
            response->error = SV_ERR_SOCKET;
            goto cleanup;
        }
    }

    /* Read the status line and headers, one byte at a time so the head
     * stops exactly at the CRLFCRLF terminator. */
    head_len = 0;
    {
        static const unsigned char terminator[4] = {'\r', '\n', '\r', '\n'};
        int found = 0;
        while (head_len < SV_HEAD_SCAN_MAX) {
            unsigned char byte;
            if (sv_read(&stream, &byte, 1) == 0) break;
            head[head_len++] = byte;
            if (head_len >= 4
                    && memcmp(head + head_len - 4, terminator, 4) == 0) {
                found = 1;
                break;
            }
        }
        if (!found) {
            response->error = SV_ERR_HEAD_TOO_BIG;
            goto cleanup;
        }
    }
    {
        int status_parsed = 0;
        size_t cursor = 0;
        while (cursor < head_len && head[cursor] != ' '
                && head[cursor] != '\r' && head[cursor] != '\n') {
            ++cursor;
        }
        while (cursor < head_len && head[cursor] == ' ') ++cursor;
        while (cursor < head_len && head[cursor] >= '0'
                && head[cursor] <= '9') {
            response->status = response->status * 10
                    + (long)(head[cursor] - '0');
            status_parsed = 1;
            ++cursor;
        }
        if (!status_parsed) {
            response->error = ERROR_INVALID_DATA;
            goto cleanup;
        }
    }

    /* Body framing: Content-Length, chunked, or read until close. */
    {
        size_t value_offset = 0;
        size_t value_len = 0;
        int chunked = 0;
        body_capacity = 65536;
        body_buffer = (unsigned char *)malloc(body_capacity);
        if (body_buffer == NULL) {
            response->error = ERROR_NOT_ENOUGH_MEMORY;
            goto cleanup;
        }
        if (header_span(head, head_len, (const unsigned char *)SV_STR(SV_8A89E0829669EB4F, "Content-Length"),
                    14, &value_offset, &value_len)) {
            size_t content_length = 0;
            size_t got = 0;
            size_t digit_index;
            for (digit_index = 0; digit_index < value_len; ++digit_index) {
                unsigned char byte = head[value_offset + digit_index];
                if (byte < '0' || byte > '9') {
                    response->error = ERROR_INVALID_DATA;
                    goto cleanup;
                }
                content_length = content_length * 10
                        + (size_t)(byte - '0');
                if (content_length > request->max_response_bytes) {
                    response->error = ERROR_INSUFFICIENT_BUFFER;
                    goto cleanup;
                }
            }
            while (body_capacity < content_length + 1) {
                unsigned char *resized;
                body_capacity = content_length + 1;
                resized = (unsigned char *)realloc(body_buffer, body_capacity);
                if (resized == NULL) {
                    response->error = ERROR_NOT_ENOUGH_MEMORY;
                    goto cleanup;
                }
                body_buffer = resized;
            }
            while (got < content_length) {
                size_t produced = reader_take(&reader,
                        body_buffer + got, content_length - got);
                if (produced == 0) break;
                got += produced;
            }
            body_total = got;
        } else {
            if (header_span(head, head_len,
                        (const unsigned char *)SV_STR(SV_D16E30F88423DC85, "Transfer-Encoding"), 17,
                        &value_offset, &value_len)
                    && ascii_equal_ci(head + value_offset, value_len,
                            (const unsigned char *)SV_STR(SV_9B9D9815C1D85539, "chunked"), 7)) {
                chunked = 1;
            }
            if (chunked) {
                unsigned char line[80];
                for (;;) {
                    int line_length = reader_line(&reader, line,
                            sizeof(line) - 1);
                    size_t chunk_size = 0;
                    int digit;
                    if (line_length < 0) goto body_done;
                    for (value_offset = 0;
                            value_offset < (size_t)line_length;
                            ++value_offset) {
                        digit = hex_digit_value(line[value_offset]);
                        if (digit < 0) break;
                        chunk_size = chunk_size * 16 + (size_t)digit;
                        if (chunk_size > request->max_response_bytes) {
                            response->error = ERROR_INSUFFICIENT_BUFFER;
                            goto cleanup;
                        }
                    }
                    if (chunk_size == 0) {
                        for (;;) {
                            int trailer = reader_line(&reader, line,
                                    sizeof(line) - 1);
                            if (trailer <= 0) break;
                        }
                        goto body_done;
                    }
                    if (body_total + chunk_size
                            > request->max_response_bytes) {
                        response->error = ERROR_INSUFFICIENT_BUFFER;
                        goto cleanup;
                    }
                    while (body_total + chunk_size + 1 > body_capacity) {
                        unsigned char *resized;
                        body_capacity *= 2;
                        resized = (unsigned char *)realloc(body_buffer,
                                body_capacity);
                        if (resized == NULL) {
                            response->error = ERROR_NOT_ENOUGH_MEMORY;
                            goto cleanup;
                        }
                        body_buffer = resized;
                    }
                    if (reader_take(&reader, body_buffer + body_total,
                                chunk_size) < chunk_size) {
                        goto body_done;
                    }
                    body_total += chunk_size;
                    {
                        unsigned char separator[2];
                        reader_take(&reader, separator, 2);
                    }
                }
            } else {
                for (;;) {
                    size_t produced;
                    if (body_total >= body_capacity - 1) {
                        unsigned char *resized;
                        if (body_total >= request->max_response_bytes) {
                            response->error = ERROR_INSUFFICIENT_BUFFER;
                            goto cleanup;
                        }
                        body_capacity *= 2;
                        resized = (unsigned char *)realloc(body_buffer,
                                body_capacity);
                        if (resized == NULL) {
                            response->error = ERROR_NOT_ENOUGH_MEMORY;
                            goto cleanup;
                        }
                        body_buffer = resized;
                    }
                    produced = sv_read(&stream,
                            body_buffer + body_total,
                            body_capacity - body_total - 1);
                    if (produced == 0) break;
                    body_total += produced;
                }
                if (body_total > request->max_response_bytes) {
                    response->error = ERROR_INSUFFICIENT_BUFFER;
                    goto cleanup;
                }
            }
        }
body_done:
        body_buffer[body_total] = '\0';
        response->data = body_buffer;
        response->size = body_total;
        body_buffer = NULL;
        ok = 1;
    }

cleanup:
    if (body_buffer != NULL) {
        SecureZeroMemory(body_buffer, body_capacity);
        free(body_buffer);
    }
    SecureZeroMemory(head, sizeof(head));
    SecureZeroMemory(host_name, sizeof(host_name));
    SecureZeroMemory(&reader, sizeof(reader));
    stream_close(&stream);
    return ok;
}

void sv_response_discard(sv_response *response) {
    if (response == NULL) return;
    if (response->data != NULL) {
        if (response->size > 0) {
            SecureZeroMemory(response->data, response->size);
        }
        free(response->data);
        response->data = NULL;
    }
    response->size = 0;
}

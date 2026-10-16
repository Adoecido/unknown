#include "loader_bootstrap.h"
#include "remote_session.h"
#include "mapping_handoff.h"
#include "obf.h"

#include <winsock2.h>
#include <ws2tcpip.h>
#include <windows.h>
#include <wincrypt.h>
#include <bcrypt.h>

#include <stdio.h>
#include <string.h>


enum {
    SONYVEGAS421_TOKEN_UNINITIALIZED = 0,
    SONYVEGAS421_TOKEN_STANDALONE = 1,
    SONYVEGAS421_TOKEN_ONLINE = 2,
    SONYVEGAS421_TOKEN_FAILED = 3
};

static volatile LONG g_bootstrap_init_done = 0;
static SRWLOCK g_controller_lock = SRWLOCK_INIT;
static volatile LONG g_token_state = SONYVEGAS421_TOKEN_UNINITIALIZED;
static SOCKET g_controller_socket = INVALID_SOCKET;
static int g_winsock_started = 0;
/* sv-raw-begin: C requires a constant initializer for array storage. */
static char g_access_token[512] = "0";
/* sv-raw-end */
static char g_remote_control_token[256];
static char g_remote_username[96];
static char g_remote_identity_envelope[256];
static char g_diagnostic[384];
static unsigned short g_zeus_port;
static SRWLOCK g_mapping_bundle_lock = SRWLOCK_INIT;
static unsigned char *g_mapping_bundle;
static size_t g_mapping_bundle_size;

static int unprotect_remote_username(
        const SonyVegas421RemoteSessionV2 *block, const char *token) {
    DATA_BLOB protected_blob;
    DATA_BLOB entropy;
    DATA_BLOB plain;
    int result = 0;
    if (block == NULL || token == NULL || token[0] == '\0'
            || block->protected_username_size == 0
            || block->protected_username_size
                    > sizeof(block->protected_username)) {
        return 0;
    }
    protected_blob.cbData = block->protected_username_size;
    protected_blob.pbData = (BYTE *)block->protected_username;
    entropy.cbData = (DWORD)strlen(token);
    entropy.pbData = (BYTE *)token;
    memset(&plain, 0, sizeof(plain));
    if (CryptUnprotectData(&protected_blob, NULL, &entropy, NULL, NULL,
            CRYPTPROTECT_UI_FORBIDDEN, &plain)) {
        if (plain.cbData > 1 && plain.cbData <= sizeof(g_remote_username)
                && plain.pbData[plain.cbData - 1] == '\0'
                && memchr(plain.pbData, '\0', plain.cbData - 1) == NULL) {
            memcpy(g_remote_username, plain.pbData, plain.cbData);
            result = 1;
        }
        if (plain.pbData != NULL) {
            SecureZeroMemory(plain.pbData, plain.cbData);
            LocalFree(plain.pbData);
        }
    }
    return result;
}

static int derive_identity_key(const char *token, unsigned char key[32]) {
    static const unsigned char label[] = {
        'c','r','i','m','s','o','n','.','i','d','e','n','t','i','t','y',
        '.','k','e','y','.','v','1'
    };
    BCRYPT_ALG_HANDLE algorithm = NULL;
    unsigned char material[sizeof(label) + 256];
    size_t token_length = token == NULL ? 0 : strlen(token);
    NTSTATUS status;
    if (token_length == 0 || token_length > 255) return 0;
    memcpy(material, label, sizeof(label));
    memcpy(material + sizeof(label), token, token_length);
    status = BCryptOpenAlgorithmProvider(&algorithm,
            BCRYPT_SHA256_ALGORITHM, NULL, 0);
    if (BCRYPT_SUCCESS(status)) {
        status = BCryptHash(algorithm, NULL, 0, material,
                (ULONG)(sizeof(label) + token_length), key, 32);
    }
    if (algorithm != NULL) BCryptCloseAlgorithmProvider(algorithm, 0);
    SecureZeroMemory(material, sizeof(material));
    return BCRYPT_SUCCESS(status);
}

static int build_remote_identity_envelope(void) {
    static const unsigned char associated_data[] = {
        'c','r','i','m','s','o','n','.','i','d','e','n','t','i','t','y',
        '.','v','1'
    };
    BCRYPT_ALG_HANDLE algorithm = NULL;
    BCRYPT_KEY_HANDLE key_handle = NULL;
    BCRYPT_AUTHENTICATED_CIPHER_MODE_INFO authentication;
    unsigned char key[32];
    unsigned char nonce[12];
    unsigned char tag[16];
    unsigned char ciphertext[96];
    unsigned char packet[1 + sizeof(nonce) + sizeof(tag) + sizeof(ciphertext)];
    unsigned char *key_object = NULL;
    ULONG key_object_size = 0;
    ULONG property_size = 0;
    ULONG ciphertext_size = 0;
    DWORD encoded_size;
    DWORD packet_size;
    NTSTATUS status = (NTSTATUS)-1;
    int result = 0;
    size_t username_length = strlen(g_remote_username);

    memset(key, 0, sizeof(key));
    memset(nonce, 0, sizeof(nonce));
    memset(tag, 0, sizeof(tag));
    memset(ciphertext, 0, sizeof(ciphertext));
    memset(packet, 0, sizeof(packet));
    if (username_length == 0 || username_length > sizeof(ciphertext)
            || !derive_identity_key(g_remote_control_token, key)) {
        goto cleanup;
    }
    status = BCryptOpenAlgorithmProvider(&algorithm, BCRYPT_AES_ALGORITHM,
            NULL, 0);
    if (!BCRYPT_SUCCESS(status)) goto cleanup;
    status = BCryptSetProperty(algorithm, BCRYPT_CHAINING_MODE,
            (PUCHAR)BCRYPT_CHAIN_MODE_GCM,
            (ULONG)sizeof(BCRYPT_CHAIN_MODE_GCM), 0);
    if (!BCRYPT_SUCCESS(status)) goto cleanup;
    status = BCryptGetProperty(algorithm, BCRYPT_OBJECT_LENGTH,
            (PUCHAR)&key_object_size, sizeof(key_object_size),
            &property_size, 0);
    if (!BCRYPT_SUCCESS(status) || key_object_size == 0) goto cleanup;
    key_object = (unsigned char *)HeapAlloc(
            GetProcessHeap(), HEAP_ZERO_MEMORY, key_object_size);
    if (key_object == NULL) goto cleanup;
    status = BCryptGenerateSymmetricKey(algorithm, &key_handle,
            key_object, key_object_size, key, sizeof(key), 0);
    if (!BCRYPT_SUCCESS(status)) goto cleanup;
    status = BCryptGenRandom(NULL, nonce, sizeof(nonce),
            BCRYPT_USE_SYSTEM_PREFERRED_RNG);
    if (!BCRYPT_SUCCESS(status)) goto cleanup;

    BCRYPT_INIT_AUTH_MODE_INFO(authentication);
    authentication.pbNonce = nonce;
    authentication.cbNonce = sizeof(nonce);
    authentication.pbTag = tag;
    authentication.cbTag = sizeof(tag);
    authentication.pbAuthData = (PUCHAR)associated_data;
    authentication.cbAuthData = sizeof(associated_data);
    status = BCryptEncrypt(key_handle, (PUCHAR)g_remote_username,
            (ULONG)username_length, &authentication, NULL, 0,
            ciphertext, sizeof(ciphertext), &ciphertext_size, 0);
    if (!BCRYPT_SUCCESS(status) || ciphertext_size != username_length) {
        goto cleanup;
    }

    packet[0] = 1;
    memcpy(packet + 1, nonce, sizeof(nonce));
    memcpy(packet + 1 + sizeof(nonce), tag, sizeof(tag));
    memcpy(packet + 1 + sizeof(nonce) + sizeof(tag), ciphertext,
            ciphertext_size);
    packet_size = (DWORD)(1 + sizeof(nonce) + sizeof(tag) + ciphertext_size);
    g_remote_identity_envelope[0] = 'v';
    g_remote_identity_envelope[1] = '1';
    g_remote_identity_envelope[2] = '.';
    encoded_size = (DWORD)(sizeof(g_remote_identity_envelope) - 3);
    if (CryptBinaryToStringA(packet, packet_size,
            CRYPT_STRING_BASE64 | CRYPT_STRING_NOCRLF,
            g_remote_identity_envelope + 3, &encoded_size)) {
        result = 1;
    }

cleanup:
    if (!result) SecureZeroMemory(g_remote_identity_envelope,
            sizeof(g_remote_identity_envelope));
    if (key_handle != NULL) BCryptDestroyKey(key_handle);
    if (key_object != NULL) {
        SecureZeroMemory(key_object, key_object_size);
        HeapFree(GetProcessHeap(), 0, key_object);
    }
    if (algorithm != NULL) BCryptCloseAlgorithmProvider(algorithm, 0);
    SecureZeroMemory(key, sizeof(key));
    SecureZeroMemory(nonce, sizeof(nonce));
    SecureZeroMemory(tag, sizeof(tag));
    SecureZeroMemory(ciphertext, sizeof(ciphertext));
    SecureZeroMemory(packet, sizeof(packet));
    return result;
}

static void object_name(wchar_t *output, size_t capacity,
        const wchar_t *kind, DWORD process_id) {
    _snwprintf_s(output, capacity, _TRUNCATE,
            SV_WSTR(SV_8A666D13C00587D4, L"%ls%ls.%ls.%lu"), SV_WSTR(SV_CB353A6557138554, L"Local\\"), SV_WSTR(SV_3985D77BD0D70F17, L"IllIIlIl"),
            kind, process_id);
}

static void signal_ack(DWORD process_id) {
    wchar_t name[96];
    HANDLE event;
    object_name(name, sizeof(name) / sizeof(name[0]),
            SV_WSTR(SV_08784B7BF2FCD8C3, L"BootstrapAck"), process_id);
    event = OpenEventW(EVENT_MODIFY_STATE, FALSE, name);
    if (event != NULL) {
        SetEvent(event);
        CloseHandle(event);
    }
}

static void remote_session_object_name(wchar_t *output, size_t capacity,
        const wchar_t *kind, DWORD process_id) {
    _snwprintf_s(output, capacity, _TRUNCATE,
            SV_WSTR(SV_2857277F3A8D050C, L"Local\\IllIIlIl.%ls.%lu"), kind, process_id);
}

static int mapping_bundle_is_valid(
        const SonyVegas421MappingBundleV1 *bundle, DWORD process_id) {
    uint32_t index;
    if (bundle == NULL
            || bundle->magic != SONYVEGAS421_MAPPING_BUNDLE_MAGIC
            || bundle->version != SONYVEGAS421_MAPPING_BUNDLE_VERSION
            || bundle->header_size != sizeof(*bundle)
            || bundle->target_pid != process_id
            || bundle->status != SONYVEGAS421_MAPPING_BUNDLE_CREATED
            || bundle->resource_count != SONYVEGAS421_MAPPING_RESOURCE_COUNT
            || bundle->total_size < sizeof(*bundle)
            || bundle->total_size > SONYVEGAS421_MAPPING_BUNDLE_MAX_SIZE) {
        return 0;
    }
    for (index = 0; index < bundle->resource_count; ++index) {
        const SonyVegas421MappingResourceV1 *resource =
                &bundle->resources[index];
        if (memchr(resource->name, '\0', sizeof(resource->name)) == NULL
                || resource->name[0] == '\0'
                || resource->offset < sizeof(*bundle)
                || resource->offset > bundle->total_size
                || resource->size > bundle->total_size - resource->offset) {
            return 0;
        }
    }
    return 1;
}

static void load_mapping_bundle(DWORD process_id) {
    wchar_t mapping_name[96];
    wchar_t event_name[96];
    HANDLE mapping;
    HANDLE event;
    SonyVegas421MappingBundleV1 *shared;
    unsigned char *copy = NULL;
    size_t copy_size = 0;
    remote_session_object_name(mapping_name,
            sizeof(mapping_name) / sizeof(mapping_name[0]),
            SV_WSTR(SV_8760DF297DF76B44, L"MappingBundle"), process_id);
    mapping = OpenFileMappingW(FILE_MAP_READ | FILE_MAP_WRITE,
            FALSE, mapping_name);
    if (mapping == NULL) return;
    shared = (SonyVegas421MappingBundleV1 *)MapViewOfFile(mapping,
            FILE_MAP_READ | FILE_MAP_WRITE, 0, 0, 0);
    if (shared != NULL) {
        MEMORY_BASIC_INFORMATION region;
        SIZE_T queried = VirtualQuery(shared, &region, sizeof(region));
        if (queried == sizeof(region)
                && mapping_bundle_is_valid(shared, process_id)
                && shared->total_size <= region.RegionSize) {
            copy_size = shared->total_size;
            copy = (unsigned char *)HeapAlloc(GetProcessHeap(), 0,
                    copy_size);
            if (copy != NULL) {
                memcpy(copy, shared, copy_size);
                ((SonyVegas421MappingBundleV1 *)copy)->status =
                        SONYVEGAS421_MAPPING_BUNDLE_CONSUMED;
                InterlockedExchange((volatile LONG *)&shared->status,
                        SONYVEGAS421_MAPPING_BUNDLE_CONSUMED);
            } else {
                InterlockedExchange((volatile LONG *)&shared->status,
                        SONYVEGAS421_MAPPING_BUNDLE_FAILED);
            }
        } else {
            InterlockedExchange((volatile LONG *)&shared->status,
                    SONYVEGAS421_MAPPING_BUNDLE_FAILED);
        }
        UnmapViewOfFile(shared);
    }
    remote_session_object_name(event_name,
            sizeof(event_name) / sizeof(event_name[0]),
            SV_WSTR(SV_439AC3929B29C9F2, L"MappingBundleAck"), process_id);
    event = OpenEventW(EVENT_MODIFY_STATE, FALSE, event_name);
    if (event != NULL) {
        SetEvent(event);
        CloseHandle(event);
    }
    CloseHandle(mapping);
    if (copy != NULL) {
        AcquireSRWLockExclusive(&g_mapping_bundle_lock);
        if (g_mapping_bundle != NULL) {
            SecureZeroMemory(g_mapping_bundle, g_mapping_bundle_size);
            HeapFree(GetProcessHeap(), 0, g_mapping_bundle);
        }
        g_mapping_bundle = copy;
        g_mapping_bundle_size = copy_size;
        ReleaseSRWLockExclusive(&g_mapping_bundle_lock);
    }
}

static void load_remote_control_session(DWORD process_id) {
    wchar_t mapping_name[96];
    wchar_t event_name[96];
    HANDLE mapping;
    HANDLE event;
    SonyVegas421RemoteSessionV2 *block;

    remote_session_object_name(mapping_name,
            sizeof(mapping_name) / sizeof(mapping_name[0]),
            SV_WSTR(SV_4F515C6A19269377, L"RemoteSession"), process_id);
    mapping = OpenFileMappingW(FILE_MAP_READ | FILE_MAP_WRITE,
            FALSE, mapping_name);
    if (mapping == NULL) return;
    block = (SonyVegas421RemoteSessionV2 *)MapViewOfFile(mapping,
            FILE_MAP_READ | FILE_MAP_WRITE, 0, 0, sizeof(*block));
    if (block != NULL) {
        if (block->magic == SONYVEGAS421_REMOTE_SESSION_MAGIC
                && block->version == SONYVEGAS421_REMOTE_SESSION_VERSION
                && block->structure_size == sizeof(*block)
                && block->target_pid == process_id
                && block->status == SONYVEGAS421_REMOTE_SESSION_CREATED
                && memchr(block->control_token, '\0',
                        sizeof(block->control_token)) != NULL
                && block->control_token[0] != '\0'
                && block->protected_username_size > 0
                && block->protected_username_size
                        <= sizeof(block->protected_username)) {
            strcpy_s(g_remote_control_token,
                    sizeof(g_remote_control_token), block->control_token);
            if (unprotect_remote_username(block, g_remote_control_token)) {
                SecureZeroMemory(block->control_token,
                        sizeof(block->control_token));
                SecureZeroMemory(block->protected_username,
                        sizeof(block->protected_username));
                block->protected_username_size = 0;
                InterlockedExchange((volatile LONG *)&block->status,
                        SONYVEGAS421_REMOTE_SESSION_CONSUMED);
            } else {
                SecureZeroMemory(g_remote_control_token,
                        sizeof(g_remote_control_token));
                InterlockedExchange((volatile LONG *)&block->status,
                        SONYVEGAS421_REMOTE_SESSION_FAILED);
            }
        } else {
            InterlockedExchange((volatile LONG *)&block->status,
                    SONYVEGAS421_REMOTE_SESSION_FAILED);
        }
        UnmapViewOfFile(block);
    }
    remote_session_object_name(event_name,
            sizeof(event_name) / sizeof(event_name[0]),
            SV_WSTR(SV_CCD49B5F8B3DC864, L"RemoteSessionAck"), process_id);
    event = OpenEventW(EVENT_MODIFY_STATE, FALSE, event_name);
    if (event != NULL) {
        SetEvent(event);
        CloseHandle(event);
    }
    CloseHandle(mapping);
}

static int send_bytes(SOCKET socket_value, const char *bytes, size_t length) {
    size_t offset = 0;
    while (offset < length) {
        int sent = send(socket_value, bytes + offset,
                (int)(length - offset), 0);
        if (sent <= 0) return 0;
        offset += (size_t)sent;
    }
    return 1;
}

static int send_line(SOCKET socket_value, const char *value) {
    return send_bytes(socket_value, value, strlen(value))
            && send_bytes(socket_value, SV_STR(SV_8181E3BA16810C79, "\n"), 1);
}

static int receive_line(SOCKET socket_value, char *output, size_t capacity) {
    size_t length = 0;
    if (capacity == 0) return 0;
    while (length + 1 < capacity) {
        char character;
        int received = recv(socket_value, &character, 1, 0);
        if (received != 1) return 0;
        if (character == '\n') {
            if (length > 0 && output[length - 1] == '\r') --length;
            output[length] = '\0';
            return 1;
        }
        output[length++] = character;
    }
    output[0] = '\0';
    return 0;
}

static void close_controller_locked(void) {
    if (g_controller_socket != INVALID_SOCKET) {
        shutdown(g_controller_socket, SD_BOTH);
        closesocket(g_controller_socket);
        g_controller_socket = INVALID_SOCKET;
    }
    if (g_winsock_started) {
        WSACleanup();
        g_winsock_started = 0;
    }
}

static int connect_controller(uint16_t port) {
    WSADATA data;
    struct sockaddr_in address;
    DWORD timeout = 500;
    SOCKET socket_value;
    if (WSAStartup(MAKEWORD(2, 2), &data) != 0) return 0;
    g_winsock_started = 1;
    socket_value = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
    if (socket_value == INVALID_SOCKET) {
        close_controller_locked();
        return 0;
    }
    setsockopt(socket_value, SOL_SOCKET, SO_SNDTIMEO,
            (const char *)&timeout, sizeof(timeout));
    setsockopt(socket_value, SOL_SOCKET, SO_RCVTIMEO,
            (const char *)&timeout, sizeof(timeout));
    memset(&address, 0, sizeof(address));
    address.sin_family = AF_INET;
    address.sin_addr.s_addr = htonl(INADDR_LOOPBACK);
    address.sin_port = htons(port);
    if (connect(socket_value, (const struct sockaddr *)&address,
            sizeof(address)) == SOCKET_ERROR) {
        closesocket(socket_value);
        close_controller_locked();
        return 0;
    }
    g_controller_socket = socket_value;
    return 1;
}

static int request_access_token(void) {
    if (g_controller_socket == INVALID_SOCKET
            || !send_line(g_controller_socket, SV_STR(SV_1C6C3DA2689E3A2C, "617"))
            || !send_line(g_controller_socket, SV_STR(SV_815576FEE9CA32C7, "200"))
            || !receive_line(g_controller_socket, g_access_token,
                    sizeof(g_access_token))
            || g_access_token[0] == '\0') {
        SecureZeroMemory(g_access_token, sizeof(g_access_token));
        return 0;
    }
    return 1;
}


int sonyvegas_loader_bootstrap_initialize(void) {
    LONG prev;
    prev = InterlockedCompareExchange(&g_bootstrap_init_done, 1, 0);
    if (prev == 0) {
        {
            DWORD process_id = GetCurrentProcessId();
            DWORD bs_mapping_error;
            wchar_t bs_name[96];
            HANDLE bs_mapping;
            SonyVegas421BootstrapV2 *block;
            LONG state = SONYVEGAS421_TOKEN_FAILED;
            load_remote_control_session(process_id);
            load_mapping_bundle(process_id);
            object_name(bs_name, sizeof(bs_name) / sizeof(bs_name[0]),
                    SV_WSTR(SV_F6AAAE70F63C1E06, L"Bootstrap"), process_id);
            bs_mapping = OpenFileMappingW(FILE_MAP_READ | FILE_MAP_WRITE,
                    FALSE, bs_name);
            if (bs_mapping == NULL) {
                bs_mapping_error = GetLastError();
                if (bs_mapping_error == ERROR_FILE_NOT_FOUND) {
                    strcpy_s(g_access_token, sizeof(g_access_token), SV_STR(SV_3DB37F28E0C46CEC, "0"));
                    state = SONYVEGAS421_TOKEN_STANDALONE;
                }
                InterlockedExchange(&g_token_state, state);
            } else {
                block = (SonyVegas421BootstrapV2 *)MapViewOfFile(bs_mapping,
                        FILE_MAP_READ | FILE_MAP_WRITE, 0, 0, sizeof(*block));
                if (block != NULL) {
                    if (block->magic == SONYVEGAS421_BOOTSTRAP_MAGIC
                            && block->version == SONYVEGAS421_BOOTSTRAP_VERSION
                            && block->structure_size == sizeof(*block)
                            && block->target_pid == process_id
                            && block->mode == SONYVEGAS421_BOOTSTRAP_MODE_ONLINE
                            && block->status
                                    == SONYVEGAS421_BOOTSTRAP_STATUS_CREATED
                            && block->controller_port != 0
                            && block->service_zeus_port != 0) {
                        g_zeus_port = block->service_zeus_port;
                        if (connect_controller(block->controller_port)
                                && request_access_token()) {
                            state = SONYVEGAS421_TOKEN_ONLINE;
                            InterlockedExchange(
                                    (volatile LONG *)&block->status,
                                    SONYVEGAS421_BOOTSTRAP_STATUS_CONSUMED);
                        } else {
                            close_controller_locked();
                            SecureZeroMemory(g_access_token,
                                    sizeof(g_access_token));
                            InterlockedExchange(
                                    (volatile LONG *)&block->status,
                                    SONYVEGAS421_BOOTSTRAP_STATUS_FAILED);
                        }
                    } else {
                        InterlockedExchange(
                                (volatile LONG *)&block->status,
                                SONYVEGAS421_BOOTSTRAP_STATUS_FAILED);
                    }
                    /* The injector still reads block->status from its own
                     * view after the ack, so the block must stay intact;
                     * V3 carries no secret material to scrub anyway. */
                    signal_ack(process_id);
                    UnmapViewOfFile(block);
                } else {
                    signal_ack(process_id);
                }
                CloseHandle(bs_mapping);
                InterlockedExchange(&g_token_state, state);
            }
        }
        InterlockedExchange(&g_bootstrap_init_done, 2);
    } else {
        while (InterlockedCompareExchange(&g_bootstrap_init_done, 2, 2) != 2)
            Sleep(1);
    }
    return InterlockedCompareExchange(&g_token_state, 0, 0)
            != SONYVEGAS421_TOKEN_FAILED;
}

const char *sonyvegas_loader_access_token(void) {
    return g_access_token;
}

unsigned short sonyvegas_loader_zeus_port(void) {
    return g_zeus_port;
}

const char *sonyvegas_remote_control_token(void) {
    return g_remote_control_token;
}

const char *sonyvegas_remote_identity(void) {
    return g_remote_username;
}

const char *sonyvegas_remote_identity_envelope(void) {
    const char *result;
    AcquireSRWLockExclusive(&g_controller_lock);
    if (g_remote_identity_envelope[0] == '\0'
            && g_remote_username[0] != '\0'
            && g_remote_control_token[0] != '\0') {
        build_remote_identity_envelope();
    }
    result = g_remote_identity_envelope;
    ReleaseSRWLockExclusive(&g_controller_lock);
    return result;
}

const unsigned char *sonyvegas_loader_mapping_resource(
        const char *resource_name, size_t *size_out) {
    const char *prefix = SV_STR(SV_74B823E1211F2C54,
            "mappings/cheatbreaker189/");
    size_t prefix_length = strlen(prefix);
    const char *name = resource_name;
    const unsigned char *result = NULL;
    uint32_t index;
    if (size_out != NULL) *size_out = 0;
    if (name == NULL) return NULL;
    if (name[0] == '/') ++name;
    if (strncmp(name, prefix, prefix_length) != 0) return NULL;
    name += prefix_length;
    AcquireSRWLockShared(&g_mapping_bundle_lock);
    if (g_mapping_bundle != NULL
            && g_mapping_bundle_size >= sizeof(SonyVegas421MappingBundleV1)) {
        const SonyVegas421MappingBundleV1 *bundle =
                (const SonyVegas421MappingBundleV1 *)g_mapping_bundle;
        for (index = 0; index < bundle->resource_count; ++index) {
            const SonyVegas421MappingResourceV1 *resource =
                    &bundle->resources[index];
            if (strcmp(resource->name, name) == 0) {
                result = g_mapping_bundle + resource->offset;
                if (size_out != NULL) *size_out = resource->size;
                break;
            }
        }
    }
    ReleaseSRWLockShared(&g_mapping_bundle_lock);
    return result;
}

int sonyvegas_loader_bootstrap_failed(void) {
    return InterlockedCompareExchange(&g_token_state, 0, 0)
            == SONYVEGAS421_TOKEN_FAILED;
}

void sonyvegas_loader_report_progress(int step) {
    char value[32];
    AcquireSRWLockExclusive(&g_controller_lock);
    if (g_controller_socket != INVALID_SOCKET) {
        _snprintf_s(value, sizeof(value), _TRUNCATE, SV_STR(SV_B2C9E7759FDDFBD4, "%d"), step);
        if (!send_line(g_controller_socket, SV_STR(SV_BD9EA8F384546881, "604"))
                || !send_line(g_controller_socket, value)
                || !send_line(g_controller_socket, SV_STR(SV_815576FEE9CA32C7, "200"))) {
            close_controller_locked();
        }
    }
    ReleaseSRWLockExclusive(&g_controller_lock);
}

void sonyvegas_loader_report_completed(void) {
    AcquireSRWLockExclusive(&g_controller_lock);
    if (g_controller_socket != INVALID_SOCKET) {
        send_line(g_controller_socket, SV_STR(SV_24794EC1CDB3FC83, "606"));
        send_line(g_controller_socket, SV_STR(SV_815576FEE9CA32C7, "200"));
        close_controller_locked();
    }
    ReleaseSRWLockExclusive(&g_controller_lock);
}

void sonyvegas_loader_report_failure(const char *message) {
    char length[32];
    char combined[512];
    size_t message_length;
    combined[0] = '\0';
    if (message != NULL && message[0] != '\0') {
        strncpy_s(combined, sizeof(combined), message, _TRUNCATE);
    }
    AcquireSRWLockExclusive(&g_controller_lock);
    if (g_diagnostic[0] != '\0') {
        if (combined[0] != '\0') {
            strncat_s(combined, sizeof(combined), SV_STR(SV_FF73373DB16F5584, " | "), _TRUNCATE);
        }
        strncat_s(combined, sizeof(combined), g_diagnostic, _TRUNCATE);
    }
    message_length = strlen(combined);
    if (g_controller_socket != INVALID_SOCKET) {
        _snprintf_s(length, sizeof(length), _TRUNCATE, SV_STR(SV_4129DBD552840CED, "%zu"), message_length);
        send_line(g_controller_socket, SV_STR(SV_2BDFA90B8F3E30D8, "618"));
        send_line(g_controller_socket, length);
        if (message_length != 0) {
            send_bytes(g_controller_socket, combined, message_length);
        }
        close_controller_locked();
    }
    ReleaseSRWLockExclusive(&g_controller_lock);
}

void sonyvegas_loader_record_diagnostic(const char *message) {
    if (message == NULL || message[0] == '\0') return;
    AcquireSRWLockExclusive(&g_controller_lock);
    strncpy_s(g_diagnostic, sizeof(g_diagnostic), message, _TRUNCATE);
    ReleaseSRWLockExclusive(&g_controller_lock);
}

void sonyvegas_loader_bootstrap_clear(void) {
    AcquireSRWLockExclusive(&g_controller_lock);
    close_controller_locked();
    SecureZeroMemory(g_access_token, sizeof(g_access_token));
    SecureZeroMemory(g_remote_control_token, sizeof(g_remote_control_token));
    SecureZeroMemory(g_remote_username, sizeof(g_remote_username));
    SecureZeroMemory(g_remote_identity_envelope,
            sizeof(g_remote_identity_envelope));
    SecureZeroMemory(g_diagnostic, sizeof(g_diagnostic));
    InterlockedExchange(&g_token_state, SONYVEGAS421_TOKEN_UNINITIALIZED);
    ReleaseSRWLockExclusive(&g_controller_lock);
    AcquireSRWLockExclusive(&g_mapping_bundle_lock);
    if (g_mapping_bundle != NULL) {
        SecureZeroMemory(g_mapping_bundle, g_mapping_bundle_size);
        HeapFree(GetProcessHeap(), 0, g_mapping_bundle);
        g_mapping_bundle = NULL;
        g_mapping_bundle_size = 0;
    }
    ReleaseSRWLockExclusive(&g_mapping_bundle_lock);
}

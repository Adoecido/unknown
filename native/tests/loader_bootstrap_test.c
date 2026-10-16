#include "loader_bootstrap.h"
#include "mapping_handoff.h"
#include "remote_session.h"

#include <winsock2.h>
#include <ws2tcpip.h>
#include <windows.h>
#include <wincrypt.h>

#include <stdio.h>
#include <string.h>

typedef struct TestServer {
    SOCKET listener;
    volatile LONG succeeded;
} TestServer;

static int protect_username(const char *username, const char *token,
        unsigned char *output, DWORD capacity, DWORD *output_size) {
    DATA_BLOB plain;
    DATA_BLOB entropy;
    DATA_BLOB protected_blob;
    int result = 0;
    plain.cbData = (DWORD)strlen(username) + 1;
    plain.pbData = (BYTE *)username;
    entropy.cbData = (DWORD)strlen(token);
    entropy.pbData = (BYTE *)token;
    memset(&protected_blob, 0, sizeof(protected_blob));
    if (CryptProtectData(&plain, L"SonyVegas identity", &entropy,
            NULL, NULL, CRYPTPROTECT_UI_FORBIDDEN, &protected_blob)) {
        if (protected_blob.cbData <= capacity) {
            memcpy(output, protected_blob.pbData, protected_blob.cbData);
            *output_size = protected_blob.cbData;
            result = 1;
        }
        SecureZeroMemory(protected_blob.pbData, protected_blob.cbData);
        LocalFree(protected_blob.pbData);
    }
    return result;
}

static int receive_line(SOCKET socket_value, char *output, size_t capacity) {
    size_t length = 0;
    while (length + 1 < capacity) {
        char character;
        if (recv(socket_value, &character, 1, 0) != 1) return 0;
        if (character == '\n') {
            output[length] = '\0';
            return 1;
        }
        output[length++] = character;
    }
    return 0;
}

static int expect_line(SOCKET socket_value, const char *expected) {
    char actual[64];
    return receive_line(socket_value, actual, sizeof(actual))
            && strcmp(actual, expected) == 0;
}

static DWORD WINAPI serve_controller(LPVOID parameter) {
    TestServer *server = (TestServer *)parameter;
    SOCKET client = accept(server->listener, NULL, NULL);
    const char token[] = "persistent-test-token\n";
    if (client != INVALID_SOCKET
            && expect_line(client, "617")
            && expect_line(client, "200")
            && send(client, token, (int)strlen(token), 0) == strlen(token)
            && expect_line(client, "604")
            && expect_line(client, "23")
            && expect_line(client, "200")
            && expect_line(client, "606")
            && expect_line(client, "200")) {
        InterlockedExchange(&server->succeeded, 1);
    }
    if (client != INVALID_SOCKET) closesocket(client);
    return 0;
}

int main(int argc, char **argv) {
    DWORD process_id = GetCurrentProcessId();
    wchar_t mapping_name[96];
    wchar_t ack_name[96];
    wchar_t remote_mapping_name[96];
    wchar_t remote_ack_name[96];
    wchar_t profile_mapping_name[96];
    wchar_t profile_ack_name[96];
    wchar_t object_namespace[16];
    wchar_t object_prefix[32];
    HANDLE mapping = NULL;
    HANDLE ack = NULL;
    HANDLE server_thread = NULL;
    HANDLE remote_mapping = NULL;
    HANDLE remote_ack = NULL;
    HANDLE profile_mapping = NULL;
    HANDLE profile_ack = NULL;
    SonyVegas421BootstrapV2 *block = NULL;
    SonyVegas421RemoteSessionV2 *remote_block = NULL;
    SonyVegas421MappingBundleV1 *profile_block = NULL;
    DWORD protected_username_size = 0;
    TestServer server = {INVALID_SOCKET, 0};
    WSADATA winsock_data;
    struct sockaddr_in address;
    int address_length = sizeof(address);
    int invalid = argc > 1 && strcmp(argv[1], "invalid") == 0;
    int winsock_started = 0;
    int result = 1;

    if (argc > 1 && strcmp(argv[1], "standalone") == 0) {
        if (!sonyvegas_loader_bootstrap_initialize()
                || strcmp(sonyvegas_loader_access_token(), "0") != 0) {
            fprintf(stderr, "standalone bootstrap did not return sentinel token\n");
            return 1;
        }
        sonyvegas_loader_bootstrap_clear();
        return 0;
    }

    memset(&address, 0, sizeof(address));
    if (!invalid) {
        if (WSAStartup(MAKEWORD(2, 2), &winsock_data) != 0) goto cleanup;
        winsock_started = 1;
        server.listener = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
        address.sin_family = AF_INET;
        address.sin_addr.s_addr = htonl(INADDR_LOOPBACK);
        address.sin_port = 0;
        if (server.listener == INVALID_SOCKET
                || bind(server.listener, (const struct sockaddr *)&address,
                        sizeof(address)) == SOCKET_ERROR
                || listen(server.listener, 1) == SOCKET_ERROR
                || getsockname(server.listener, (struct sockaddr *)&address,
                        &address_length) == SOCKET_ERROR) {
            goto cleanup;
        }
        server_thread = CreateThread(NULL, 0, serve_controller, &server, 0, NULL);
        if (server_thread == NULL) goto cleanup;

        _snwprintf_s(remote_mapping_name,
                sizeof(remote_mapping_name) / sizeof(remote_mapping_name[0]),
                _TRUNCATE, L"Local\\IllIIlIl.RemoteSession.%lu", process_id);
        _snwprintf_s(remote_ack_name,
                sizeof(remote_ack_name) / sizeof(remote_ack_name[0]),
                _TRUNCATE, L"Local\\IllIIlIl.RemoteSessionAck.%lu", process_id);
        remote_mapping = CreateFileMappingW(INVALID_HANDLE_VALUE, NULL,
                PAGE_READWRITE, 0, sizeof(*remote_block), remote_mapping_name);
        remote_ack = CreateEventW(NULL, TRUE, FALSE, remote_ack_name);
        remote_block = remote_mapping == NULL ? NULL
                : (SonyVegas421RemoteSessionV2 *)MapViewOfFile(remote_mapping,
                        FILE_MAP_ALL_ACCESS, 0, 0, sizeof(*remote_block));
        if (remote_mapping == NULL || remote_ack == NULL
                || remote_block == NULL) {
            fprintf(stderr, "failed to create remote identity test objects\n");
            goto cleanup;
        }
        SecureZeroMemory(remote_block, sizeof(*remote_block));
        remote_block->magic = SONYVEGAS421_REMOTE_SESSION_MAGIC;
        remote_block->version = SONYVEGAS421_REMOTE_SESSION_VERSION;
        remote_block->structure_size = (uint16_t)sizeof(*remote_block);
        remote_block->target_pid = process_id;
        remote_block->status = SONYVEGAS421_REMOTE_SESSION_CREATED;
        strcpy_s(remote_block->control_token,
                sizeof(remote_block->control_token), "remote-session-token");
        if (!protect_username("Fixture User", remote_block->control_token,
                remote_block->protected_username,
                sizeof(remote_block->protected_username),
                &protected_username_size)) {
            fprintf(stderr, "failed to protect remote username\n");
            goto cleanup;
        }
        remote_block->protected_username_size = protected_username_size;

        _snwprintf_s(profile_mapping_name,
                sizeof(profile_mapping_name) / sizeof(profile_mapping_name[0]),
                _TRUNCATE, L"Local\\IllIIlIl.MappingBundle.%lu", process_id);
        _snwprintf_s(profile_ack_name,
                sizeof(profile_ack_name) / sizeof(profile_ack_name[0]),
                _TRUNCATE, L"Local\\IllIIlIl.MappingBundleAck.%lu", process_id);
        profile_mapping = CreateFileMappingW(INVALID_HANDLE_VALUE, NULL,
                PAGE_READWRITE, 0, sizeof(*profile_block) + 7,
                profile_mapping_name);
        profile_ack = CreateEventW(NULL, TRUE, FALSE, profile_ack_name);
        profile_block = profile_mapping == NULL ? NULL
                : (SonyVegas421MappingBundleV1 *)MapViewOfFile(profile_mapping,
                        FILE_MAP_ALL_ACCESS, 0, 0,
                        sizeof(*profile_block) + 7);
        if (profile_mapping == NULL || profile_ack == NULL
                || profile_block == NULL) {
            fprintf(stderr, "failed to create mapping profile test objects\n");
            goto cleanup;
        }
        SecureZeroMemory(profile_block, sizeof(*profile_block) + 7);
        profile_block->magic = SONYVEGAS421_MAPPING_BUNDLE_MAGIC;
        profile_block->version = SONYVEGAS421_MAPPING_BUNDLE_VERSION;
        profile_block->header_size = (uint16_t)sizeof(*profile_block);
        profile_block->target_pid = process_id;
        profile_block->status = SONYVEGAS421_MAPPING_BUNDLE_CREATED;
        profile_block->resource_count = SONYVEGAS421_MAPPING_RESOURCE_COUNT;
        profile_block->total_size = (uint32_t)(sizeof(*profile_block) + 7);
        strcpy_s(profile_block->patch_sha256,
                sizeof(profile_block->patch_sha256),
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        {
            static const char *names[SONYVEGAS421_MAPPING_RESOURCE_COUNT] = {
                "classes.tsv", "anchors.properties", "fields.tsv",
                "fields_fwd.tsv", "methods.tsv", "methods_fwd.tsv",
                "mapping-update.json"
            };
            uint32_t index;
            for (index = 0; index < SONYVEGAS421_MAPPING_RESOURCE_COUNT;
                    ++index) {
                strcpy_s(profile_block->resources[index].name,
                        sizeof(profile_block->resources[index].name),
                        names[index]);
                profile_block->resources[index].offset =
                        (uint32_t)sizeof(*profile_block);
            }
        }
        profile_block->resources[0].size = 7;
        memcpy((unsigned char *)profile_block + sizeof(*profile_block),
                "fixture", 7);
    }

    wcscpy_s(object_namespace, sizeof(object_namespace) / sizeof(object_namespace[0]),
            L"Local\\");
    wcscpy_s(object_prefix, sizeof(object_prefix) / sizeof(object_prefix[0]),
            L"IllIIlIl");
    _snwprintf_s(mapping_name, sizeof(mapping_name) / sizeof(mapping_name[0]),
            _TRUNCATE, L"%ls%ls.Bootstrap.%lu", object_namespace, object_prefix, process_id);
    _snwprintf_s(ack_name, sizeof(ack_name) / sizeof(ack_name[0]),
            _TRUNCATE, L"%ls%ls.BootstrapAck.%lu", object_namespace, object_prefix, process_id);
    mapping = CreateFileMappingW(INVALID_HANDLE_VALUE, NULL, PAGE_READWRITE,
            0, sizeof(*block), mapping_name);
    ack = CreateEventW(NULL, TRUE, FALSE, ack_name);
    block = mapping == NULL ? NULL : (SonyVegas421BootstrapV2 *)MapViewOfFile(
            mapping, FILE_MAP_ALL_ACCESS, 0, 0, sizeof(*block));
    if (mapping == NULL || ack == NULL || block == NULL) {
        fprintf(stderr, "failed to create bootstrap test objects\n");
        goto cleanup;
    }

    SecureZeroMemory(block, sizeof(*block));
    block->magic = invalid ? 0 : SONYVEGAS421_BOOTSTRAP_MAGIC;
    block->version = SONYVEGAS421_BOOTSTRAP_VERSION;
    block->structure_size = (uint16_t)sizeof(*block);
    block->target_pid = process_id;
    block->mode = SONYVEGAS421_BOOTSTRAP_MODE_ONLINE;
    block->controller_port = invalid ? 1 : ntohs(address.sin_port);
    block->service_zeus_port = 8091;
    block->status = SONYVEGAS421_BOOTSTRAP_STATUS_CREATED;

    if (invalid) {
        if (sonyvegas_loader_bootstrap_initialize()
                || WaitForSingleObject(ack, 1000) != WAIT_OBJECT_0
                || block->status != SONYVEGAS421_BOOTSTRAP_STATUS_FAILED) {
            fprintf(stderr, "invalid bootstrap was not rejected\n");
            goto cleanup;
        }
    } else {
        const unsigned char *mapping_resource;
        size_t mapping_resource_size = 0;
        if (!sonyvegas_loader_bootstrap_initialize()
                || WaitForSingleObject(ack, 1000) != WAIT_OBJECT_0
                || block->status != SONYVEGAS421_BOOTSTRAP_STATUS_CONSUMED
                || strcmp(sonyvegas_loader_access_token(), "persistent-test-token") != 0
                || WaitForSingleObject(remote_ack, 1000) != WAIT_OBJECT_0
                || remote_block->status != SONYVEGAS421_REMOTE_SESSION_CONSUMED
                || WaitForSingleObject(profile_ack, 1000) != WAIT_OBJECT_0
                || profile_block->status
                        != SONYVEGAS421_MAPPING_BUNDLE_CONSUMED
                || strcmp(sonyvegas_remote_control_token(),
                        "remote-session-token") != 0
                || strncmp(sonyvegas_remote_identity_envelope(), "v1.", 3) != 0) {
            fprintf(stderr, "socket token bootstrap failed\n");
            goto cleanup;
        }
        mapping_resource = sonyvegas_loader_mapping_resource(
                "/mappings/cheatbreaker189/classes.tsv",
                &mapping_resource_size);
        if (mapping_resource == NULL || mapping_resource_size != 7
                || memcmp(mapping_resource, "fixture", 7) != 0) {
            fprintf(stderr, "mapping profile memory handoff failed\n");
            goto cleanup;
        }
        sonyvegas_loader_report_progress(23);
        sonyvegas_loader_report_completed();
        if (WaitForSingleObject(server_thread, 1000) != WAIT_OBJECT_0
                || InterlockedCompareExchange(&server.succeeded, 0, 0) == 0) {
            fprintf(stderr, "progress protocol sequence failed\n");
            goto cleanup;
        }
    }
    result = 0;

cleanup:
    sonyvegas_loader_bootstrap_clear();
    if (block != NULL) {
        SecureZeroMemory(block, sizeof(*block));
        UnmapViewOfFile(block);
    }
    if (ack != NULL) CloseHandle(ack);
    if (mapping != NULL) CloseHandle(mapping);
    if (remote_block != NULL) {
        SecureZeroMemory(remote_block, sizeof(*remote_block));
        UnmapViewOfFile(remote_block);
    }
    if (remote_ack != NULL) CloseHandle(remote_ack);
    if (remote_mapping != NULL) CloseHandle(remote_mapping);
    if (profile_block != NULL) {
        SecureZeroMemory(profile_block, sizeof(*profile_block) + 7);
        UnmapViewOfFile(profile_block);
    }
    if (profile_ack != NULL) CloseHandle(profile_ack);
    if (profile_mapping != NULL) CloseHandle(profile_mapping);
    if (server.listener != INVALID_SOCKET) closesocket(server.listener);
    if (server_thread != NULL) {
        WaitForSingleObject(server_thread, 1000);
        CloseHandle(server_thread);
    }
    if (winsock_started) WSACleanup();
    return result;
}

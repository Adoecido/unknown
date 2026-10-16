#ifndef WIN32_LEAN_AND_MEAN
#define WIN32_LEAN_AND_MEAN
#endif
#include <windows.h>
#include <wincrypt.h>
#include <shlobj.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "control_api_client.h"
#include "mapping_handoff.h"
#include "obf.h"
#include "secure_transport.h"

#define CONTROL_PORT 443u
#define CONTROL_MAX_RESPONSE (32 * 1024 * 1024)

/* Binary control endpoint, stored XOR-masked so no dotted quad exists in
 * the image. Unmask with 0x5A at request time; the address never
 * materializes as text in the process. */
#define CONTROL_HOST_MASK 0x5Au
static const unsigned char control_host_masked[4] = {0x6Cu, 0x04u, 0xCBu, 0x0Du};

/* Accepted SubjectPublicKeyInfo SHA-256 pins for the control endpoint.
 * Slot 1 holds the live HTTPS certificate's key; slot 2 is reserved for
 * rotation and is ignored while zero. Certificate renewal must reuse this
 * key, because the transport validates the pin instead of the host name. */
static const unsigned char control_spki_pins[2][32] = {
    {0x52u, 0x55u, 0xADu, 0x53u, 0x4Cu, 0x3Du, 0x06u, 0xF2u, 0x11u, 0xC0u,
     0xF1u, 0x9Eu, 0x6Du, 0x11u, 0x7Bu, 0xAAu, 0xD0u, 0x99u, 0x46u, 0xABu,
     0xC4u, 0x8Fu, 0x62u, 0xD0u, 0xDFu, 0x93u, 0x96u, 0x6Au, 0x2Fu, 0x4Du,
     0xB9u, 0xCBu},
    {0}
};

typedef struct {
    char *data;
    size_t size;
    DWORD status_code;
    DWORD transport_error;
} control_response;

static char cached_file_id[64];
static char cached_file_token[2048];
static char cached_control_token[256];
static char cached_username[64];
static char cached_new_device_secret[256];
static char cached_role[32];
static char cached_time_left[64];
static char cached_last_injection[64];
static int cached_claim_required;
static control_auth_status cached_auth_status = CONTROL_AUTH_UNAVAILABLE;
static DWORD cached_transport_error;
static DWORD cached_http_status;
static unsigned char *cached_mapping_bundle;
static size_t cached_mapping_bundle_size;

static void sanitized_request_path(const char *path,
        char *output, size_t capacity) {
    size_t index = 0;
    if (!output || capacity == 0) return;
    output[0] = '\0';
    if (path == NULL) return;
    if (strncmp(path, SV_STR(SV_0FE6DCF86C552B42, "/api/v1/"), 8) == 0) {
        strncpy(output, SV_STR(SV_F9CD3E54A7D051F1, "/api/v1/<redacted>"), capacity - 1);
        output[capacity - 1] = '\0';
        return;
    }
    while (path[index] != '\0' && path[index] != '?'
            && index + 1 < capacity) {
        output[index] = path[index];
        ++index;
    }
    output[index] = '\0';
}

static void control_response_free(control_response *response) {
    if (response->data) {
        if (response->size > 0) {
            SecureZeroMemory(response->data, response->size);
        }
        free(response->data);
        response->data = NULL;
    }
    response->size = 0;
}

static int control_request_ex(const char *method, const char *path,
        const char *body, size_t body_len, control_response *response,
        int binary_response, int binary_request, int startup_request) {
    sv_request request;
    sv_response transport_response;
    unsigned char host_ip[4];
    char safe_path[1024];
    size_t index;
    int ok = 0;

    (void)binary_response;
    memset(response, 0, sizeof(*response));
    if (method == NULL || method[0] == '\0' || path == NULL
            || path[0] == '\0') {
        response->transport_error = ERROR_INVALID_PARAMETER;
        return 0;
    }
    for (index = 0; index < 4; ++index) {
        host_ip[index] =
                (unsigned char)(control_host_masked[index] ^ CONTROL_HOST_MASK);
    }
    SecureZeroMemory(&request, sizeof(request));
    SecureZeroMemory(&transport_response, sizeof(transport_response));
    /* s_addr is the on-the-wire order: first octet in the low address. */
    request.host_ip = ((uint32_t)host_ip[0] << 24)
            | ((uint32_t)host_ip[1] << 16)
            | ((uint32_t)host_ip[2] << 8) | (uint32_t)host_ip[3];
    SecureZeroMemory(host_ip, sizeof(host_ip));
    request.port = CONTROL_PORT;
    request.use_tls = 1;
    request.pins = &control_spki_pins[0][0];
    request.pin_count = (int)(sizeof(control_spki_pins) / 32);
    request.method = (const unsigned char *)method;
    request.method_len = strlen(method);
    request.target = (const unsigned char *)path;
    request.target_len = strlen(path);
    if (body != NULL && body_len > 0) {
        const char *content_type = binary_request
                ? SV_STR(SV_0A0F75D0198BA863, "application/octet-stream")
                : SV_STR(SV_4D632048A03BD311, "application/json");
        request.body = (const unsigned char *)body;
        request.body_len = body_len;
        request.content_type = (const unsigned char *)content_type;
        request.content_type_len = strlen(content_type);
    }
    request.connect_timeout_ms = 5000u;
    request.send_timeout_ms = startup_request ? 10000u : 90000u;
    request.recv_timeout_ms = startup_request ? 10000u : 30000u;
    request.max_response_bytes =
            startup_request ? 65536u : (size_t)CONTROL_MAX_RESPONSE;

    sanitized_request_path(path, safe_path, sizeof(safe_path));
    if (!sv_request_perform(&request, &transport_response)) {
        response->transport_error = transport_response.error;
        sv_response_discard(&transport_response);
        SecureZeroMemory(safe_path, sizeof(safe_path));
        sonyvegas_obf_scrub();
        return 0;
    }
    response->status_code = (DWORD)transport_response.status;
    response->data = (char *)transport_response.data;
    response->size = transport_response.size;
    ok = 1;
    SecureZeroMemory(safe_path, sizeof(safe_path));
    sonyvegas_obf_scrub();
    return ok;
}

static int control_request(const char *method, const char *path,
        const char *body, size_t body_len, control_response *response,
        int binary_response, int binary_request) {
    return control_request_ex(method, path, body, body_len, response,
            binary_response, binary_request, 0);
}

char *control_api_get_account_info(const char *token,
        unsigned long *error_out, unsigned long *status_out) {
    char path[640];
    control_response response;
    size_t token_size;
    size_t index;
    char *result;
    int sent;
    *error_out = ERROR_INVALID_PARAMETER;
    *status_out = 0;
    if (!token || !token[0]) return NULL;
    token_size = strlen(token);
    if (token_size >= 512 || strcmp(token, SV_STR(SV_3DB37F28E0C46CEC, "0")) == 0) return NULL;
    /* Only an opaque URL-safe session may occupy this single path segment. */
    for (index = 0; index < token_size; ++index) {
        unsigned char ch = (unsigned char)token[index];
        if (!((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')
                || (ch >= '0' && ch <= '9') || ch == '_' || ch == '-'
                || ch == '.')) return NULL;
    }
    _snprintf(path, sizeof(path), SV_STR(SV_9F02997DE88D4D48, "/api/v1/%s/authenticated"), token);
    sent = control_request_ex(SV_STR(SV_5317F41AA1903D00, "GET"), path, NULL, 0, &response, 0, 0, 1);
    SecureZeroMemory(path, sizeof(path));
    *status_out = response.status_code;
    *error_out = response.transport_error;
    if (!sent || response.status_code != 200
            || !response.data || response.size == 0) {
        if (!*error_out && (!sent || response.status_code == 200)) {
            *error_out = ERROR_INVALID_DATA;
        }
        control_response_free(&response);
        return NULL;
    }
    result = response.data;
    response.data = NULL;
    return result;
}

static int json_escape(const char *input, char *output, size_t capacity) {
    size_t read_index;
    size_t write_index = 0;
    for (read_index = 0; input[read_index] != '\0'; ++read_index) {
        unsigned char value = (unsigned char)input[read_index];
        const char *escape = NULL;
        if (value == '"') escape = SV_STR(SV_B825EDA9C3819A38, "\\\"");
        else if (value == '\\') escape = SV_STR(SV_A4EADACF307CFAB8, "\\\\");
        else if (value == '\n') escape = SV_STR(SV_7F859CC2571D62A9, "\\n");
        else if (value == '\r') escape = SV_STR(SV_C01169E7C3743D97, "\\r");
        else if (value == '\t') escape = SV_STR(SV_2A943401D31A26B2, "\\t");
        if (escape) {
            if (write_index + 2 >= capacity) return 0;
            output[write_index++] = escape[0];
            output[write_index++] = escape[1];
        } else {
            if (value < 32 || write_index + 1 >= capacity) return 0;
            output[write_index++] = (char)value;
        }
    }
    output[write_index] = '\0';
    return 1;
}

static int execute_control(const char *request_body,
        char *control_response_body, size_t response_capacity) {
    control_response response;
    int copied = 0;
    int sent = control_request(SV_STR(SV_8C5FE0AFBC11BBC2, "POST"),
            SV_STR(SV_4C4E695F1914F95A, "/v1/control"),
            request_body, strlen(request_body), &response, 0, 0);
    cached_transport_error = response.transport_error;
    cached_http_status = response.status_code;
    if (!sent) return 0;
    if (response.status_code >= 200 && response.status_code < 300
            && response.data && response.size < response_capacity) {
        memcpy(control_response_body, response.data, response.size + 1);
        copied = 1;
    }
    control_response_free(&response);
    return copied;
}

static int cache_authorization(char *response,
        char *username_out, size_t username_capacity) {
    char *username;
    char *file_id;
    char *token;
    char *control_token;
    char *device_secret;
    char *role;
    char *separator;
    if (strncmp(response, SV_STR(SV_6B822D5AE4EF85F3, "OK\t"), 3) != 0) return 0;
    username = response + 3;
    separator = strchr(username, '\t');
    if (!separator) return 0;
    *separator = '\0';
    file_id = separator + 1;
    separator = strchr(file_id, '\t');
    if (!separator) return 0;
    *separator = '\0';
    token = separator + 1;
    separator = strchr(token, '\t');
    if (!separator) return 0;
    *separator = '\0';
    control_token = separator + 1;
    separator = strchr(control_token, '\t');
    if (!separator) return 0;
    *separator = '\0';
    device_secret = separator + 1;
    separator = strchr(device_secret, '\t');
    if (!separator) return 0;
    *separator = '\0';
    role = separator + 1;
    {
        char *time_left = NULL;
        char *last_inj = NULL;
        separator = strchr(role, '\t');
        if (separator) {
            *separator = '\0';
            time_left = separator + 1;
            separator = strchr(time_left, '\t');
            if (separator) {
                *separator = '\0';
                last_inj = separator + 1;
                separator = strchr(last_inj, '\t');
                if (separator) *separator = '\0';
            }
        }
        if (time_left && time_left[0] && strlen(time_left) < sizeof(cached_time_left)) {
            strncpy(cached_time_left, time_left, sizeof(cached_time_left) - 1);
            cached_time_left[sizeof(cached_time_left) - 1] = '\0';
        } else {
            cached_time_left[0] = '\0';
        }
        if (last_inj && last_inj[0] && strlen(last_inj) < sizeof(cached_last_injection)) {
            strncpy(cached_last_injection, last_inj, sizeof(cached_last_injection) - 1);
            cached_last_injection[sizeof(cached_last_injection) - 1] = '\0';
        } else {
            cached_last_injection[0] = '\0';
        }
    }
    if (!file_id[0] || !token[0] || !control_token[0] || !role[0]
            || strlen(file_id) >= sizeof(cached_file_id)
            || strlen(token) >= sizeof(cached_file_token)
            || strlen(control_token) >= sizeof(cached_control_token)
            || strlen(device_secret) >= sizeof(cached_new_device_secret)) return 0;
    strncpy(cached_file_id, file_id, sizeof(cached_file_id) - 1);
    cached_file_id[sizeof(cached_file_id) - 1] = '\0';
    strncpy(cached_file_token, token, sizeof(cached_file_token) - 1);
    cached_file_token[sizeof(cached_file_token) - 1] = '\0';
    strncpy(cached_control_token, control_token,
            sizeof(cached_control_token) - 1);
    cached_control_token[sizeof(cached_control_token) - 1] = '\0';
    if (strcmp(device_secret, SV_STR(SV_588BEDF6E5D25BB0, "-")) != 0) {
        strncpy(cached_new_device_secret, device_secret,
                sizeof(cached_new_device_secret) - 1);
        cached_new_device_secret[sizeof(cached_new_device_secret) - 1] = '\0';
    } else {
        cached_new_device_secret[0] = '\0';
    }
    cached_claim_required = 0;
    if (username_capacity > 0) {
        strncpy(username_out, username, username_capacity - 1);
        username_out[username_capacity - 1] = '\0';
    }
    strncpy(cached_username, username, sizeof(cached_username) - 1);
    cached_username[sizeof(cached_username) - 1] = '\0';
    if (role[0] && strlen(role) < sizeof(cached_role)) {
        strncpy(cached_role, role, sizeof(cached_role) - 1);
        cached_role[sizeof(cached_role) - 1] = '\0';
    }
    return 1;
}

int control_api_check_hwid(const char *user_id, const char *device_secret,
        const char *username, char *username_out, size_t username_capacity) {
    char escaped_secret[320];
    char escaped_username[96];
    char inner[768];
    char response[4096];
    cached_claim_required = 0;
    cached_auth_status = CONTROL_AUTH_UNAVAILABLE;
    if (!json_escape(device_secret == NULL ? SV_STR(SV_8C4D649A44E15777, "") : device_secret,
            escaped_secret, sizeof(escaped_secret))
            || !json_escape(username == NULL ? SV_STR(SV_8C4D649A44E15777, "") : username,
                    escaped_username, sizeof(escaped_username))) return 0;
    if (username != NULL && username[0] != '\0') {
        _snprintf(inner, sizeof(inner),
                SV_STR(SV_B9DB500F9D648357, "{\"action\":\"auth.check\",\"hwid\":\"%s\",\"deviceSecret\":\"%s\",\"username\":\"%s\"}"),
                user_id, escaped_secret, escaped_username);
    } else {
        _snprintf(inner, sizeof(inner),
                SV_STR(SV_1E5C7CD6132F0D92, "{\"action\":\"auth.check\",\"hwid\":\"%s\",\"deviceSecret\":\"%s\"}"), user_id, escaped_secret);
    }
    if (!execute_control(inner, response, sizeof(response))) return 0;
    if (strcmp(response, SV_STR(SV_2351B27FA8ACF722, "CLAIM_REQUIRED")) == 0) {
        cached_claim_required = 1;
        cached_auth_status = CONTROL_AUTH_CLAIM_REQUIRED;
        return 0;
    }
    if (strcmp(response, SV_STR(SV_9012DB97028A9F2B, "NOT_FOUND")) == 0) {
        cached_auth_status = CONTROL_AUTH_NOT_FOUND;
        return 0;
    }
    if (strcmp(response, SV_STR(SV_9062E1B60E60C686, "ERROR\tdevice_disabled")) == 0) {
        cached_auth_status = CONTROL_AUTH_DEVICE_DISABLED;
        return 0;
    }
    if (strcmp(response, SV_STR(SV_F3FEDBDFFB733784, "ERROR\tlicense_disabled_or_expired")) == 0) {
        cached_auth_status = CONTROL_AUTH_LICENSE_INACTIVE;
        return 0;
    }
    if (strcmp(response, SV_STR(SV_159921817F861F3C, "ERROR\tdevice_secret_invalid")) == 0) {
        cached_auth_status = CONTROL_AUTH_DEVICE_SECRET_INVALID;
        return 0;
    }
    if (cache_authorization(response, username_out, username_capacity)) {
        cached_auth_status = CONTROL_AUTH_AUTHORIZED;
        return 1;
    }
    cached_auth_status = CONTROL_AUTH_REJECTED;
    return 0;
}

control_auth_status control_api_last_auth_status(void) {
    return cached_auth_status;
}

unsigned long control_api_last_transport_error(void) {
    return cached_transport_error;
}

unsigned long control_api_last_http_status(void) {
    return cached_http_status;
}

int control_api_claim_required(void) {
    return cached_claim_required;
}

int control_api_claim_device(const char *license_key, const char *user_id,
        const char *username, char *username_out, size_t username_capacity,
        char *error_out, size_t error_capacity) {
    char escaped_license[320];
    char escaped_username[96];
    char inner[768];
    char response[4096];
    if (!json_escape(license_key, escaped_license, sizeof(escaped_license))
            || !json_escape(username, escaped_username,
                    sizeof(escaped_username))) {
        _snprintf(error_out, error_capacity, SV_STR(SV_9EBBBAC4EDFC5AA0, "Entrada invalida"));
        return 0;
    }
    _snprintf(inner, sizeof(inner),
            SV_STR(SV_7DAD102C055DBFAF, "{\"action\":\"auth.claim\",\"hwid\":\"%s\",\"licenseKey\":\"%s\",\"username\":\"%s\"}"),
            user_id, escaped_license, escaped_username);
    if (!execute_control(inner, response, sizeof(response))) {
        _snprintf(error_out, error_capacity, SV_STR(SV_69330FE67AFEC7B2, "Erro de conexao"));
        return 0;
    }
    if (cache_authorization(response, username_out, username_capacity)) return 1;
    if (strncmp(response, SV_STR(SV_35904ED778D83E31, "ERROR\t"), 6) == 0) response[5] = ' ';
    _snprintf(error_out, error_capacity, SV_STR(SV_84693D00E97C295D, "%s"), response);
    return 0;
}

int control_api_activate_license(const char *license_key,
        const char *user_id, const char *username,
        char *error_out, size_t error_capacity) {
    char escaped_license[160];
    char escaped_username[96];
    char inner[1024];
    char response[4096];
    char ignored_username[64];
    if (!json_escape(license_key, escaped_license, sizeof(escaped_license))
            || !json_escape(username, escaped_username,
                    sizeof(escaped_username))) {
        _snprintf(error_out, error_capacity, SV_STR(SV_9EBBBAC4EDFC5AA0, "Entrada invalida"));
        return 0;
    }
    _snprintf(inner, sizeof(inner),
            SV_STR(SV_1EEB08D6719A0859, "{\"action\":\"activate\",\"hwid\":\"%s\",\"licenseKey\":\"%s\",\"username\":\"%s\"}"),
            user_id, escaped_license, escaped_username);
    if (!execute_control(inner, response, sizeof(response))) {
        _snprintf(error_out, error_capacity, SV_STR(SV_69330FE67AFEC7B2, "Erro de conexao"));
        return 0;
    }
    if (cache_authorization(response, ignored_username,
            sizeof(ignored_username))) return 1;
    if (strncmp(response, SV_STR(SV_35904ED778D83E31, "ERROR\t"), 6) == 0) response[5] = ' ';
    _snprintf(error_out, error_capacity, SV_STR(SV_84693D00E97C295D, "%s"), response);
    return 0;
}

int control_api_download_payload(const char *user_id,
        unsigned char **buffer_out, size_t *size_out) {
    char path[4096];
    control_response response;
    (void)user_id;
    if (!cached_file_id[0] || !cached_file_token[0]) return 0;
    _snprintf(path, sizeof(path), SV_STR(SV_6B6BD06547120858, "/v1/payloads/%s/download?token=%s"),
            cached_file_id, cached_file_token);
    if (!control_request(SV_STR(SV_5317F41AA1903D00, "GET"), path, NULL, 0, &response, 1, 0)
            || response.status_code != 200 || !response.data
            || response.size == 0) {
        control_response_free(&response);
        SecureZeroMemory(path, sizeof(path));
        return 0;
    }
    SecureZeroMemory(path, sizeof(path));
    *buffer_out = (unsigned char *)response.data;
    *size_out = response.size;
    response.data = NULL;
    cached_file_token[0] = '\0';
    return 1;
}

static int hash_file_sha256(const wchar_t *path, char output[65],
        unsigned char **data_out, size_t *size_out) {
    HANDLE file = INVALID_HANDLE_VALUE;
    LARGE_INTEGER size = {0};
    HCRYPTPROV provider = 0;
    HCRYPTHASH hash = 0;
    unsigned char *data = NULL;
    DWORD read = 0;
    DWORD hash_size = 32;
    unsigned char digest_bytes[32];
    const char *hex = SV_STR(SV_ED299E91459544C4, "0123456789abcdef");
    int result = 0;
    file = CreateFileW(path, GENERIC_READ, FILE_SHARE_READ | FILE_SHARE_WRITE,
            NULL, OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL);
    if (file == INVALID_HANDLE_VALUE || !GetFileSizeEx(file, &size)
            || size.QuadPart < 1024 || size.QuadPart > 16 * 1024 * 1024) goto cleanup;
    data = (unsigned char *)malloc((size_t)size.QuadPart);
    if (!data || !ReadFile(file, data, (DWORD)size.QuadPart, &read, NULL)
            || read != (DWORD)size.QuadPart) goto cleanup;
    if (!CryptAcquireContextW(&provider, NULL, NULL, PROV_RSA_AES,
            CRYPT_VERIFYCONTEXT) || !CryptCreateHash(provider, CALG_SHA_256, 0, 0, &hash)
            || !CryptHashData(hash, data, read, 0)
            || !CryptGetHashParam(hash, HP_HASHVAL, digest_bytes, &hash_size, 0)) goto cleanup;
    for (DWORD index = 0; index < hash_size; ++index) {
        output[index * 2] = hex[digest_bytes[index] >> 4];
        output[index * 2 + 1] = hex[digest_bytes[index] & 15];
    }
    output[64] = '\0';
    *data_out = data;
    *size_out = (size_t)size.QuadPart;
    data = NULL;
    result = 1;
cleanup:
    if (data) { SecureZeroMemory(data, (size_t)(size.QuadPart > 0 ? size.QuadPart : 0)); free(data); }
    if (hash) CryptDestroyHash(hash);
    if (provider) CryptReleaseContext(provider, 0);
    if (file != INVALID_HANDLE_VALUE) CloseHandle(file);
    return result;
}

enum mapping_status_result {
    MAPPING_STATUS_PENDING = 0,
    MAPPING_STATUS_READY = 1,
    MAPPING_STATUS_FAILED = 2,
    MAPPING_STATUS_UNAVAILABLE = 3
};

static void mapping_status_error(const char *response, char *output,
        size_t output_capacity) {
    const char *cursor;
    size_t length = 0;
    if (output == NULL || output_capacity == 0) return;
    output[0] = '\0';
    if (response == NULL) return;
    cursor = strstr(response, SV_STR(SV_49141997539E3CC2, "\"error\":\""));
    if (cursor == NULL) return;
    cursor += strlen(SV_STR(SV_49141997539E3CC2, "\"error\":\""));
    while (*cursor != '\0' && *cursor != '"' && length + 1 < output_capacity) {
        unsigned char character = (unsigned char)*cursor++;
        if (character == '\\' && *cursor != '\0') character = (unsigned char)*cursor++;
        if (character < 32 || character > 126) continue;
        output[length++] = (char)character;
    }
    output[length] = '\0';
}

static int mapping_status(const char *sha256, char *error_out,
        size_t error_capacity) {
    char path[1024];
    control_response response;
    _snprintf(path, sizeof(path),
            SV_STR(SV_8E6594876976DF6D, "/v1/mappings/cheatbreaker/status?sha256=%s&token=%s"),
            sha256, cached_control_token);
    if (error_out != NULL && error_capacity != 0) error_out[0] = '\0';
    if (!control_request(SV_STR(SV_5317F41AA1903D00, "GET"), path, NULL, 0, &response, 0, 0)) {
        SecureZeroMemory(path, sizeof(path));
        return MAPPING_STATUS_UNAVAILABLE;
    }
    SecureZeroMemory(path, sizeof(path));
    if (response.status_code != 200 || response.data == NULL) {
        control_response_free(&response);
        return MAPPING_STATUS_UNAVAILABLE;
    }
    int ready = response.status_code == 200 && response.data
            && strstr(response.data, SV_STR(SV_81B9F83949453302, "\"ready\":true")) != NULL;
    int failed = response.status_code == 200 && response.data
            && strstr(response.data, SV_STR(SV_2E5DBE1A49C01017, "\"state\":\"failed\"")) != NULL;
    if (failed) mapping_status_error(response.data, error_out, error_capacity);
    control_api_debug_log(SV_WSTR(SV_401E114695D016D8, L"CB mapping status hash=%S http=%lu ready=%d"),
            sha256, response.status_code, ready);
    control_response_free(&response);
    if (ready) return MAPPING_STATUS_READY;
    if (failed) return MAPPING_STATUS_FAILED;
    return MAPPING_STATUS_PENDING;
}

static int download_mapping_resource(const char *sha256, const char *name,
        unsigned char **data_out, size_t *size_out,
        char *error_out, size_t error_capacity) {
    char request_path[1024];
    control_response response;
    unsigned char *copy;
    if (data_out == NULL || size_out == NULL) return 0;
    *data_out = NULL;
    *size_out = 0;
    _snprintf(request_path, sizeof(request_path),
            SV_STR(SV_E5F2B9A9C3B2919A, "/v1/mappings/cheatbreaker/%s/%s?token=%s"),
            sha256, name, cached_control_token);
    if (!control_request(SV_STR(SV_5317F41AA1903D00, "GET"), request_path, NULL, 0, &response, 1, 0)
            || response.status_code != 200 || !response.data || response.size == 0) {
        if (error_out && error_capacity) {
            _snprintf(error_out, error_capacity,
                    SV_STR(SV_36BB66D4C7536EFD, "Mapping %s: HTTP %lu, transport 0x%08lX"),
                    name, response.status_code, response.transport_error);
        }
        control_response_free(&response);
        SecureZeroMemory(request_path, sizeof(request_path));
        return 0;
    }
    SecureZeroMemory(request_path, sizeof(request_path));
    control_api_debug_log(SV_WSTR(SV_69CC6C1D3D90F2A1, L"CB mapping download file=%S bytes=%zu"),
            name, response.size);
    if (response.size > 4u * 1024u * 1024u) {
        if (error_out && error_capacity) {
            _snprintf(error_out, error_capacity,
                    SV_STR(SV_3AF137542515522E, "Mapping %s exceeds 4 MiB"), name);
        }
        control_response_free(&response);
        return 0;
    }
    copy = (unsigned char *)malloc(response.size);
    if (copy == NULL) {
        if (error_out && error_capacity) {
            _snprintf(error_out, error_capacity,
                    SV_STR(SV_A067576C28EED1B8, "Mapping %s: out of memory"), name);
        }
        control_response_free(&response);
        return 0;
    }
    memcpy(copy, response.data, response.size);
    *data_out = copy;
    *size_out = response.size;
    control_response_free(&response);
    return 1;
}

int control_api_prepare_cheatbreaker_mappings(
        char *error_out, size_t error_capacity) {
    wchar_t patch_path[MAX_PATH];
    char sha256[65];
    unsigned char *patch_data = NULL;
    size_t patch_size = 0;
    unsigned char *resource_data[SONYVEGAS421_MAPPING_RESOURCE_COUNT] = {0};
    size_t resource_sizes[SONYVEGAS421_MAPPING_RESOURCE_COUNT] = {0};
    unsigned char *bundle = NULL;
    size_t bundle_size = sizeof(SonyVegas421MappingBundleV1);
    control_response response;
    const wchar_t *appdata = _wgetenv(SV_WSTR(SV_232256B0C32BF09A, L"APPDATA"));
    char files[SONYVEGAS421_MAPPING_RESOURCE_COUNT][32] = {{0}};
    size_t index;
    control_api_clear_cheatbreaker_mapping_bundle();
    if (!appdata || !cached_control_token[0]) goto failure;
    swprintf(patch_path, MAX_PATH,
            SV_WSTR(SV_E77B0105249E5769, L"%s\\CheatBreaker\\downloads\\versions\\1.8.9\\1.8.9.patch"), appdata);
    if (!hash_file_sha256(patch_path, sha256, &patch_data, &patch_size)) {
        control_api_debug_log(SV_WSTR(SV_704290AABD462A14, L"CB patch unavailable path=%ls error=%lu"),
                patch_path, GetLastError());
        _snprintf(error_out, error_capacity, SV_STR(SV_19420C6EE7D8D109, "CheatBreaker 1.8.9.patch was not found."));
        return 0;
    }
    control_api_debug_log(SV_WSTR(SV_DB34DA8DCB2B9534, L"CB patch path=%ls hash=%S bytes=%zu"),
            patch_path, sha256, patch_size);
    {
        char mapping_failure_detail[192] = {0};
        int initial_state = mapping_status(sha256, mapping_failure_detail,
                sizeof(mapping_failure_detail));
        if (initial_state == MAPPING_STATUS_UNAVAILABLE) {
            _snprintf(error_out, error_capacity,
                    SV_STR(SV_9E6D01D7FA63CCFC, "CheatBreaker mapping service is unavailable."));
            goto failure;
        }
        if (initial_state != MAPPING_STATUS_READY) {
        char upload_path[1024];
        _snprintf(upload_path, sizeof(upload_path),
                SV_STR(SV_710653FEE70B926B, "/v1/mappings/cheatbreaker/%s?token=%s"), sha256,
                cached_control_token);
        if (!control_request(SV_STR(SV_68D9A9DC1760A01F, "PUT"), upload_path, (const char *)patch_data,
                patch_size, &response, 0, 1)
                || (response.status_code != 200 && response.status_code != 202)) {
            control_response_free(&response);
            SecureZeroMemory(upload_path, sizeof(upload_path));
            goto failure;
        }
        SecureZeroMemory(upload_path, sizeof(upload_path));
        control_api_debug_log(SV_WSTR(SV_D3751040F8FE65C0, L"CB mapping upload hash=%S http=%lu"),
                sha256, response.status_code);
        control_response_free(&response);
        int mapping_state = MAPPING_STATUS_PENDING;
        int unavailable_attempts = 0;
        ULONGLONG wait_started = GetTickCount64();
        for (int attempt = 0; attempt < 450
                && GetTickCount64() - wait_started < 15ULL * 60ULL * 1000ULL;
                ++attempt) {
            mapping_state = mapping_status(sha256, mapping_failure_detail,
                    sizeof(mapping_failure_detail));
            if (mapping_state == MAPPING_STATUS_READY) break;
            if (mapping_state == MAPPING_STATUS_FAILED) {
                _snprintf(error_out, error_capacity,
                        SV_STR(SV_39662DB478EAFD98, "CheatBreaker mapping worker failed.%s%s"),
                        mapping_failure_detail[0] ? SV_STR(SV_AFF368124CD76E60, " ") : SV_STR(SV_8C4D649A44E15777, ""),
                        mapping_failure_detail[0] ? mapping_failure_detail : SV_STR(SV_8C4D649A44E15777, ""));
                goto failure;
            }
            if (mapping_state == MAPPING_STATUS_UNAVAILABLE) {
                if (++unavailable_attempts >= 3) {
                    _snprintf(error_out, error_capacity,
                            SV_STR(SV_9E6D01D7FA63CCFC, "CheatBreaker mapping service is unavailable."));
                    goto failure;
                }
            } else {
                unavailable_attempts = 0;
            }
            Sleep(2000);
        }
        if (mapping_state != MAPPING_STATUS_READY) {
            _snprintf(error_out, error_capacity,
                    SV_STR(SV_8A965AA6F884EEFD, "CheatBreaker mappings could not be updated on the server."));
            goto failure;
        }
        }
    }
    SecureZeroMemory(patch_data, patch_size);
    free(patch_data);
    patch_data = NULL;
    for (index = 0; index < SONYVEGAS421_MAPPING_RESOURCE_COUNT; ++index) {
        const char *resource_name;
        switch (index) {
            case 0: resource_name = SV_STR(SV_859A105819DDB9BB, "anchors.properties"); break;
            case 1: resource_name = SV_STR(SV_0594E42B26B6885C, "classes.tsv"); break;
            case 2: resource_name = SV_STR(SV_66AA43E39C6A37D3, "fields.tsv"); break;
            case 3: resource_name = SV_STR(SV_3852C6B0BF83E80F, "fields_fwd.tsv"); break;
            case 4: resource_name = SV_STR(SV_02A9EE8BAE28A91C, "methods.tsv"); break;
            case 5: resource_name = SV_STR(SV_7705F8BDA78EF716, "methods_fwd.tsv"); break;
            case 6: resource_name = SV_STR(SV_88423F95F7F09838, "mapping-update.json"); break;
            default: goto failure;
        }
        strcpy_s(files[index], sizeof(files[index]), resource_name);
        if (!download_mapping_resource(sha256, files[index],
                &resource_data[index], &resource_sizes[index],
                error_out, error_capacity)
                || resource_sizes[index] > UINT32_MAX
                || bundle_size > SONYVEGAS421_MAPPING_BUNDLE_MAX_SIZE
                        - resource_sizes[index]) {
            if (error_out && error_capacity && error_out[0] == '\0') {
                _snprintf(error_out, error_capacity,
                        SV_STR(SV_A1F713D6D4F11029, "Mapping bundle is too large"));
            }
            goto failure;
        }
        bundle_size += resource_sizes[index];
    }
    bundle = (unsigned char *)calloc(1, bundle_size);
    if (bundle == NULL) goto failure;
    {
        SonyVegas421MappingBundleV1 *header =
                (SonyVegas421MappingBundleV1 *)bundle;
        size_t offset = sizeof(*header);
        header->magic = SONYVEGAS421_MAPPING_BUNDLE_MAGIC;
        header->version = SONYVEGAS421_MAPPING_BUNDLE_VERSION;
        header->header_size = (uint16_t)sizeof(*header);
        header->resource_count = SONYVEGAS421_MAPPING_RESOURCE_COUNT;
        header->total_size = (uint32_t)bundle_size;
        memcpy(header->patch_sha256, sha256, 64);
        header->patch_sha256[64] = '\0';
        for (index = 0; index < SONYVEGAS421_MAPPING_RESOURCE_COUNT; ++index) {
            strcpy_s(header->resources[index].name,
                    sizeof(header->resources[index].name), files[index]);
            header->resources[index].offset = (uint32_t)offset;
            header->resources[index].size = (uint32_t)resource_sizes[index];
            memcpy(bundle + offset, resource_data[index],
                    resource_sizes[index]);
            offset += resource_sizes[index];
        }
    }
    control_api_clear_cheatbreaker_mapping_bundle();
    cached_mapping_bundle = bundle;
    cached_mapping_bundle_size = bundle_size;
    bundle = NULL;
    for (index = 0; index < SONYVEGAS421_MAPPING_RESOURCE_COUNT; ++index) {
        SecureZeroMemory(resource_data[index], resource_sizes[index]);
        free(resource_data[index]);
        resource_data[index] = NULL;
    }
    control_api_debug_log(SV_WSTR(SV_E022B30065F4F61E, L"CB mappings ready hash=%S memoryBytes=%zu"),
            sha256, cached_mapping_bundle_size);
    SecureZeroMemory(files, sizeof(files));
    return 1;
failure:
    SecureZeroMemory(files, sizeof(files));
    if (patch_data) { SecureZeroMemory(patch_data, patch_size); free(patch_data); }
    if (bundle) { SecureZeroMemory(bundle, bundle_size); free(bundle); }
    for (index = 0; index < SONYVEGAS421_MAPPING_RESOURCE_COUNT; ++index) {
        if (resource_data[index]) {
            SecureZeroMemory(resource_data[index], resource_sizes[index]);
            free(resource_data[index]);
        }
    }
    if (error_out && error_capacity && error_out[0] == '\0')
        _snprintf(error_out, error_capacity, SV_STR(SV_5E52A2D176F52F32, "Could not synchronize CheatBreaker mappings."));
    control_api_debug_log(SV_WSTR(SV_ED0BBD314C693CAC, L"CB mapping synchronization failed"));
    return 0;
}

const unsigned char *control_api_cheatbreaker_mapping_bundle(
        size_t *size_out) {
    if (size_out != NULL) *size_out = cached_mapping_bundle_size;
    return cached_mapping_bundle;
}

void control_api_clear_cheatbreaker_mapping_bundle(void) {
    if (cached_mapping_bundle != NULL) {
        SecureZeroMemory(cached_mapping_bundle, cached_mapping_bundle_size);
        free(cached_mapping_bundle);
        cached_mapping_bundle = NULL;
        cached_mapping_bundle_size = 0;
    }
}

const char *control_api_session_token(void) {
    return cached_control_token;
}

const char *control_api_session_username(void) {
    return cached_username;
}

const char *control_api_new_device_secret(void) {
    return cached_new_device_secret;
}

const char *control_api_session_role(void) {
    return cached_role;
}

const char *control_api_session_time_left(void) {
    return cached_time_left;
}

const char *control_api_session_last_injection(void) {
    return cached_last_injection;
}

void control_api_update_last_injection(void) {
    SYSTEMTIME now;
    GetLocalTime(&now);
    _snprintf(cached_last_injection, sizeof(cached_last_injection),
            SV_STR(SV_14B3952165FB2088, "%02u-%02u %02u:%02u"),
            now.wMonth, now.wDay, now.wHour, now.wMinute);
}

/* Generic account-API request for the Java bridge: path-only transport so
 * the JAR never assembles a URL. Accepts only /api/v1/ paths, returns the
 * heap body (caller wipes and frees) and reports the HTTP status; the
 * status stays 0 on transport failure. */
char *control_api_account_perform(const char *method, const char *path,
        const char *body, unsigned long *status_out) {
    control_response response;
    char *result = NULL;
    size_t method_size;
    if (status_out == NULL) return NULL;
    *status_out = 0;
    if (method == NULL || path == NULL || path[0] != '/'
            || strncmp(path, SV_STR(SV_0FE6DCF86C552B42, "/api/v1/"), 8) != 0) return NULL;
    method_size = strlen(method);
    if (method_size == 0 || method_size > 8) return NULL;
    if (!control_request_ex(method, path, body,
                body == NULL ? 0 : strlen(body), &response, 0, 0, 0)) {
        return NULL;
    }
    *status_out = response.status_code;
    if (response.status_code >= 200 && response.status_code < 300
            && response.data != NULL) {
        result = response.data;
        response.data = NULL;
    }
    control_response_free(&response);
    return result;
}

char *control_api_request_generic(const char *action,
        const char *arguments_json, const char *token) {
    if (!action || !token || token[0] == '\0') return NULL;

    size_t action_size = strlen(action);
    size_t token_size = strlen(token);
    size_t arguments_size = arguments_json ? strlen(arguments_json) : 0;
    size_t body_capacity;
    char *body;
    int body_len;
    int sent;
    if (action_size > 1024 * 1024 - 64
            || token_size > 1024 * 1024 - 64 - action_size
            || arguments_size > 1024 * 1024 - 64 - action_size - token_size) {
        return NULL;
    }
    body_capacity = action_size + token_size + arguments_size + 64;
    body = (char *)malloc(body_capacity);
    if (!body) return NULL;
    if (arguments_json && arguments_json[0]) {
        body_len = _snprintf(body, body_capacity,
                SV_STR(SV_B341391DE16276A4, "{\"action\":\"%s\",\"arguments\":%s,\"token\":\"%s\"}"),
                action, arguments_json, token);
    } else {
        body_len = _snprintf(body, body_capacity,
                SV_STR(SV_193561B6A99707C2, "{\"action\":\"%s\",\"token\":\"%s\"}"), action, token);
    }
    if (body_len <= 0 || body_len >= (int)body_capacity) {
        SecureZeroMemory(body, body_capacity);
        free(body);
        return NULL;
    }

    control_response response;
    sent = control_request(SV_STR(SV_8C5FE0AFBC11BBC2, "POST"), SV_STR(SV_4C4E695F1914F95A, "/v1/control"),
            body, body_len, &response, 0, 0);
    SecureZeroMemory(body, body_capacity);
    free(body);
    if (!sent) return NULL;

    if (response.status_code < 200 || response.status_code >= 300
            || !response.data || response.size == 0) {
        control_response_free(&response);
        return NULL;
    }

    char *result = (char *)malloc(response.size + 1);
    if (!result) {
        control_response_free(&response);
        return NULL;
    }
    memcpy(result, response.data, response.size);
    result[response.size] = '\0';
    control_response_free(&response);
    return result;
}

int control_api_download_loader_update(const char *current_sha256,
        unsigned char **buffer_out, size_t *size_out,
        char expected_sha256[65]) {
    char arguments[96];
    char *reply = NULL;
    char *hash;
    char *size_text;
    char *path;
    char *separator;
    char path_prefix[32] = {0};
    char path_suffix[32] = {0};
    size_t prefix_len;
    size_t suffix_len;
    size_t reply_len = 0;
    unsigned __int64 declared_size;
    char *size_end;
    control_response response;
    size_t index;
    int result = -1;

    if (!current_sha256 || !buffer_out || !size_out || !expected_sha256
            || strlen(current_sha256) != 64 || !cached_control_token[0]) return -1;
    *buffer_out = NULL;
    *size_out = 0;
    expected_sha256[0] = '\0';
    for (index = 0; index < 64; ++index) {
        char ch = current_sha256[index];
        if (!((ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f'))) return -1;
    }
    _snprintf(arguments, sizeof(arguments),
            SV_STR(SV_B6D9C73C79C1162A, "{\"sha256\":\"%s\"}"), current_sha256);
    reply = control_api_request_generic(
            SV_STR(SV_F8E089E8DAF65D92, "loader.latest"),
            arguments, cached_control_token);
    SecureZeroMemory(arguments, sizeof(arguments));
    if (!reply) return -1;
    reply_len = strlen(reply);
    if (strcmp(reply, SV_STR(SV_D262E1AB51569631, "CURRENT")) == 0
            || strcmp(reply, SV_STR(SV_F2B86E5557FC8018, "NONE")) == 0) {
        result = 0;
        goto cleanup;
    }
    if (strncmp(reply, SV_STR(SV_2D4EAA1D3FF01D19, "UPDATE\t"), 7) != 0)
        goto cleanup;
    hash = reply + 7;
    separator = strchr(hash, '\t');
    if (!separator || separator - hash != 64) goto cleanup;
    *separator = '\0';
    for (index = 0; index < 64; ++index) {
        char ch = hash[index];
        if (!((ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f'))) goto cleanup;
    }
    size_text = separator + 1;
    separator = strchr(size_text, '\t');
    if (!separator) goto cleanup;
    *separator = '\0';
    declared_size = _strtoui64(size_text, &size_end, 10);
    if (size_end == size_text || *size_end != '\0'
            || declared_size < 1024 || declared_size > CONTROL_MAX_RESPONSE)
        goto cleanup;
    path = separator + 1;
    strcpy_s(path_prefix, sizeof(path_prefix),
            SV_STR(SV_E02B126F3C8EF355, "/v1/downloads/dl_"));
    strcpy_s(path_suffix, sizeof(path_suffix),
            SV_STR(SV_3202460658735558, "/file?token="));
    prefix_len = strlen(path_prefix);
    suffix_len = strlen(path_suffix);
    if (strlen(path) > 256 || strncmp(path, path_prefix, prefix_len) != 0
            || strlen(path) < prefix_len + 32 + suffix_len + 16
            || strncmp(path + prefix_len + 32, path_suffix, suffix_len) != 0)
        goto cleanup;
    for (index = prefix_len; index < prefix_len + 32; ++index) {
        char ch = path[index];
        if (!((ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f'))) goto cleanup;
    }
    for (index = prefix_len + 32 + suffix_len; path[index] != '\0'; ++index) {
        char ch = path[index];
        if (!((ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'z')
                || (ch >= 'A' && ch <= 'Z') || ch == '-' || ch == '_'))
            goto cleanup;
    }
    if (!control_request(SV_STR(SV_5317F41AA1903D00, "GET"), path,
            NULL, 0, &response, 1, 0)) goto cleanup;
    if (response.status_code != 200 || !response.data
            || response.size != (size_t)declared_size) {
        control_response_free(&response);
        goto cleanup;
    }
    memcpy(expected_sha256, hash, 64);
    expected_sha256[64] = '\0';
    *buffer_out = (unsigned char *)response.data;
    *size_out = response.size;
    response.data = NULL;
    control_response_free(&response);
    result = 1;
cleanup:
    SecureZeroMemory(reply, reply_len);
    free(reply);
    SecureZeroMemory(path_prefix, sizeof(path_prefix));
    SecureZeroMemory(path_suffix, sizeof(path_suffix));
    return result;
}

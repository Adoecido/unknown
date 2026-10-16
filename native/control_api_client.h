#ifndef SONYVEGAS421_CONTROL_API_CLIENT_H
#define SONYVEGAS421_CONTROL_API_CLIENT_H

#include <stddef.h>
#include <wchar.h>

/* Loader diagnostics are compiled out in release builds. */
#define control_api_debug_log(...) ((void)0)

/* Ask the remote control API to authorize this HWID and issue a short-lived
   payload token. No administrative key is present in the client. */
int control_api_check_hwid(const char *user_id, const char *device_secret,
        const char *username, char *username_out, size_t username_cap);

typedef enum {
    CONTROL_AUTH_UNAVAILABLE = 0,
    CONTROL_AUTH_AUTHORIZED,
    CONTROL_AUTH_NOT_FOUND,
    CONTROL_AUTH_CLAIM_REQUIRED,
    CONTROL_AUTH_DEVICE_DISABLED,
    CONTROL_AUTH_LICENSE_INACTIVE,
    CONTROL_AUTH_DEVICE_SECRET_INVALID,
    CONTROL_AUTH_REJECTED
} control_auth_status;
control_auth_status control_api_last_auth_status(void);
unsigned long control_api_last_transport_error(void);
unsigned long control_api_last_http_status(void);

/* True when the backend found a migrated device which must confirm its
   original license once before a device secret can be issued. */
int control_api_claim_required(void);

int control_api_claim_device(const char *license_key, const char *user_id,
        const char *username, char *username_out, size_t username_cap,
        char *error_out, size_t error_cap);

/* Activate a license key, binding the chosen display name to an HWID.
   Username is used by the JAR for party members and audit events; it is not
   a VPS credential and there is no account password.
   Returns 1 on success, 0 on failure (invalid key, already used, etc.).
   On failure, writes error message to error_out. */
int control_api_activate_license(const char *license_key,
        const char *user_id, const char *username,
        char *error_out, size_t error_cap);

/* Download the DLL payload into a memory buffer.
   Caller must free *buffer_out with free().
   Returns 1 on success. */
int control_api_download_payload(const char *user_id,
        unsigned char **buffer_out, size_t *size_out);

/* Compare the installed loader hash with the newest visible catalog entry.
   Returns 0 when current/no loader is published, 1 with verified download
   metadata and owned bytes, or -1 when the update check/download fails. */
int control_api_download_loader_update(const char *current_sha256,
        unsigned char **buffer_out, size_t *size_out,
        char expected_sha256[65]);

/* Synchronize the selected CheatBreaker patch with the mapping service and
   retain its validated runtime profile in loader memory. */
int control_api_prepare_cheatbreaker_mappings(
        char *error_out, size_t error_cap);

/* Returns the packed mapping profile owned by this module. The caller may
   copy it into the target-process handoff and must not free the pointer. */
const unsigned char *control_api_cheatbreaker_mapping_bundle(
        size_t *size_out);
void control_api_clear_cheatbreaker_mapping_bundle(void);

/* Session and newly issued device secret cached from the last successful
   authorization. Pointers remain owned by this module. */
const char *control_api_session_token(void);
const char *control_api_session_username(void);
const char *control_api_new_device_secret(void);
const char *control_api_session_role(void);
const char *control_api_session_time_left(void);
const char *control_api_session_last_injection(void);
void control_api_update_last_injection(void);

/* One bounded HTTPS account request using the same Windows transport as the
   loader. The JSON still requires Java's signed-license verification.
   Caller must erase and free the returned string. Error/status contain no
   token, request URL or response content. */
char *control_api_get_account_info(const char *token,
        unsigned long *error_out, unsigned long *status_out);

/* Generic control API request for Java bridge.
   Caller must free returned string with free().
   Returns allocated JSON response string or NULL on failure. */
char *control_api_request_generic(const char *action,
        const char *arguments_json, const char *token);

/* Generic account-API request for the Java bridge: the JAR supplies only a
 * method and an /api/v1/... path, never a URL, so no host text reaches the
 * JVM. Returns the heap body (caller wipes and frees); *status_out carries
 * the HTTP status, 0 on transport failure. */
char *control_api_account_perform(const char *method, const char *path,
        const char *body, unsigned long *status_out);

#endif

#ifndef SONYVEGAS421_SECURE_TRANSPORT_H
#define SONYVEGAS421_SECURE_TRANSPORT_H

#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Raw HTTP/1.1 transport with optional Schannel TLS.
 *
 * Replaces WinHTTP on every code path that runs inside the game process.
 * WinHTTP leaks wide copies of URLs, host names, user agents and raw request
 * blobs into the target heap; this transport owns every buffer it touches
 * and the caller wipes request inputs, so nothing identifying survives a
 * request:
 *
 *  - request/target/body bytes live in caller-provided buffers that the
 *    caller wipes after sv_request_perform returns;
 *  - response payloads are owned by the transport and wiped by
 *    sv_response_discard();
 *  - the host may be given as a name (resolved with getaddrinfo) or as a
 *    binary IPv4 word, so control endpoints never exist as text;
 *  - pinned endpoints validate a caller-supplied SPKI SHA-256 pin instead
 *    of a host name, and public CDNs use Schannel chain validation with
 *    the (wiped) DNS name only as a transient validation input.
 */

typedef struct sv_request {
    /* DNS name bytes (NUL-terminated not required). Used when host != NULL. */
    const unsigned char *host;
    size_t host_len;
    /* Binary IPv4 host word in host byte order (htonl applied internally);
     * used when host == NULL. */
    uint32_t host_ip;
    unsigned short port;
    int use_tls;
    /* pins != NULL: 32-byte SPKI SHA-256 values, pin_count of them; one must
     * match the server key and no chain/name validation runs. pins == NULL:
     * Schannel automatic chain + host name validation. */
    const unsigned char *pins;
    int pin_count;
    /* Request target bytes, query string included. */
    const unsigned char *target;
    size_t target_len;
    /* Request method bytes (e.g. "PUT"); NULL or empty selects POST when a
     * body is present and GET otherwise. */
    const unsigned char *method;
    size_t method_len;
    /* POST body; NULL or empty for GET. */
    const unsigned char *body;
    size_t body_len;
    /* Optional Content-Type value bytes. */
    const unsigned char *content_type;
    size_t content_type_len;
    unsigned int connect_timeout_ms;
    unsigned int send_timeout_ms;
    unsigned int recv_timeout_ms;
    size_t max_response_bytes;
} sv_request;

typedef struct sv_response {
    unsigned char *data;
    size_t size;
    long status;          /* HTTP status code; 0 on transport failure. */
    unsigned long error;  /* Winsock / SSPI / transport error code. */
} sv_response;

/* Performs the request. Returns 1 on success (HTTP exchange completed, any
 * status code), 0 on transport failure with response->error set. */
int sv_request_perform(const sv_request *request, sv_response *response);

/* Wipes and frees response->data. Safe on zeroed responses. */
void sv_response_discard(sv_response *response);

#ifdef __cplusplus
}
#endif

#endif

#ifndef SONYVEGAS421_LOADER_BOOTSTRAP_H
#define SONYVEGAS421_LOADER_BOOTSTRAP_H

#include <stdint.h>
#include <stddef.h>

#define SONYVEGAS421_BOOTSTRAP_MAGIC 0x54423456u
#define SONYVEGAS421_BOOTSTRAP_VERSION 3u
#define SONYVEGAS421_BOOTSTRAP_MODE_ONLINE 1u
#define SONYVEGAS421_BOOTSTRAP_STATUS_CREATED 1u
#define SONYVEGAS421_BOOTSTRAP_STATUS_CONSUMED 2u
#define SONYVEGAS421_BOOTSTRAP_STATUS_FAILED 3u

#pragma pack(push, 1)
/* V3 drops the plaintext service_http_base/service_zeus_host fields: the
 * mapped block used to carry "https://<ip>" and "127.0.0.1" inside the game
 * process. Both consumers now reach their endpoints through binary
 * addresses compiled into the DLL, so the handoff only transports a port. */
typedef struct SonyVegas421BootstrapV3 {
    uint32_t magic;
    uint16_t version;
    uint16_t structure_size;
    uint32_t target_pid;
    uint32_t mode;
    uint16_t controller_port;
    uint16_t service_zeus_port;
    uint8_t reserved[40];
    uint32_t status;
} SonyVegas421BootstrapV3;
#pragma pack(pop)

typedef char SonyVegas421BootstrapV3_size_must_be_64[
        sizeof(SonyVegas421BootstrapV3) == 64 ? 1 : -1];

/* Deprecated alias kept for older loader sources. */
typedef SonyVegas421BootstrapV3 SonyVegas421BootstrapV2;

int sonyvegas_loader_bootstrap_initialize(void);
unsigned short sonyvegas_loader_zeus_port(void);
const char *sonyvegas_loader_access_token(void);
const char *sonyvegas_remote_control_token(void);
const char *sonyvegas_remote_identity(void);
const char *sonyvegas_remote_identity_envelope(void);
const unsigned char *sonyvegas_loader_mapping_resource(
        const char *resource_name, size_t *size_out);
int sonyvegas_loader_bootstrap_failed(void);
void sonyvegas_loader_report_progress(int step);
void sonyvegas_loader_report_completed(void);
void sonyvegas_loader_report_failure(const char *message);
void sonyvegas_loader_record_diagnostic(const char *message);
void sonyvegas_loader_bootstrap_clear(void);

#endif

#ifndef SONYVEGAS421_REMOTE_SESSION_H
#define SONYVEGAS421_REMOTE_SESSION_H

#include <stdint.h>

#define SONYVEGAS421_REMOTE_SESSION_MAGIC 0x53565243u
#define SONYVEGAS421_REMOTE_SESSION_VERSION 2u
#define SONYVEGAS421_REMOTE_SESSION_CREATED 1u
#define SONYVEGAS421_REMOTE_SESSION_CONSUMED 2u
#define SONYVEGAS421_REMOTE_SESSION_FAILED 3u

#pragma pack(push, 1)
typedef struct SonyVegas421RemoteSessionV2 {
    uint32_t magic;
    uint16_t version;
    uint16_t structure_size;
    uint32_t target_pid;
    uint32_t status;
    char control_token[256];
    uint32_t protected_username_size;
    uint8_t protected_username[512];
} SonyVegas421RemoteSessionV2;
#pragma pack(pop)

typedef char SonyVegas421RemoteSessionV2_size_must_be_788[
        sizeof(SonyVegas421RemoteSessionV2) == 788 ? 1 : -1];

#endif

#ifndef SONYVEGAS421_MAPPING_HANDOFF_H
#define SONYVEGAS421_MAPPING_HANDOFF_H

#include <stdint.h>

#define SONYVEGAS421_MAPPING_BUNDLE_MAGIC 0x50414D53u
#define SONYVEGAS421_MAPPING_BUNDLE_VERSION 1u
#define SONYVEGAS421_MAPPING_BUNDLE_CREATED 1u
#define SONYVEGAS421_MAPPING_BUNDLE_CONSUMED 2u
#define SONYVEGAS421_MAPPING_BUNDLE_FAILED 3u
#define SONYVEGAS421_MAPPING_RESOURCE_COUNT 7u
#define SONYVEGAS421_MAPPING_RESOURCE_NAME_CAPACITY 64u
#define SONYVEGAS421_MAPPING_BUNDLE_MAX_SIZE (32u * 1024u * 1024u)

#pragma pack(push, 1)
typedef struct SonyVegas421MappingResourceV1 {
    char name[SONYVEGAS421_MAPPING_RESOURCE_NAME_CAPACITY];
    uint32_t offset;
    uint32_t size;
} SonyVegas421MappingResourceV1;

typedef struct SonyVegas421MappingBundleV1 {
    uint32_t magic;
    uint16_t version;
    uint16_t header_size;
    uint32_t target_pid;
    uint32_t status;
    uint32_t resource_count;
    uint32_t total_size;
    char patch_sha256[65];
    uint8_t reserved[7];
    SonyVegas421MappingResourceV1 resources[
            SONYVEGAS421_MAPPING_RESOURCE_COUNT];
} SonyVegas421MappingBundleV1;
#pragma pack(pop)

typedef char SonyVegas421MappingResourceV1_size_must_be_72[
        sizeof(SonyVegas421MappingResourceV1) == 72 ? 1 : -1];
typedef char SonyVegas421MappingBundleV1_size_must_be_600[
        sizeof(SonyVegas421MappingBundleV1) == 600 ? 1 : -1];

#endif

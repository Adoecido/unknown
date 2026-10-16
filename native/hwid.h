#ifndef SONYVEGAS421_HWID_H
#define SONYVEGAS421_HWID_H

#include <stddef.h>

/* Generates a stable hardware ID from machine identifiers.
   Writes a backend-safe user ID (hw_ + 32 hex chars) into output. */
int sonyvegas_generate_hwid(char *output, size_t capacity);

/* Per-Windows-user DPAPI storage for the server-issued device secret. */
int sonyvegas_load_device_secret(char *output, size_t capacity);
int sonyvegas_store_device_secret(const char *secret);

#endif

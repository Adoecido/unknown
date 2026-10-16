#ifndef SONYVEGAS421_CLEANUP_H
#define SONYVEGAS421_CLEANUP_H

#include <windows.h>

typedef struct cleanup_report {
    int prefetch_found;
    int prefetch_cleaned;
    int timestomp_ok;
    int replace_ok;
    int self_delete_ok;
    wchar_t details[8192];
    size_t details_len;
} cleanup_report;

void cleanup_run(cleanup_report *report);

#endif

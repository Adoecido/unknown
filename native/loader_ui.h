#ifndef SONYVEGAS421_LOADER_UI_H
#define SONYVEGAS421_LOADER_UI_H

#ifndef WIN32_LEAN_AND_MEAN
#define WIN32_LEAN_AND_MEAN
#endif
#include <windows.h>

#include <stddef.h>

#define LOADER_UI_MAX_PROCESSES 64
#define LOADER_UI_EXECUTABLE_CAPACITY 64
#define LOADER_UI_TITLE_CAPACITY 256

typedef struct loader_ui_process {
    DWORD process_id;
    wchar_t executable[LOADER_UI_EXECUTABLE_CAPACITY];
    wchar_t title[LOADER_UI_TITLE_CAPACITY];
} loader_ui_process;

int loader_ui_start(void);
void loader_ui_shutdown(void);
int loader_ui_is_running(void);
int loader_ui_is_closed(void);

void loader_ui_set_status(const wchar_t *status, const wchar_t *detail,
        float progress, int indeterminate);
void loader_ui_show_result(int success, const wchar_t *status,
        const wchar_t *detail);
void loader_ui_wait(DWORD milliseconds);

int loader_ui_prompt_utf8(const wchar_t *prompt, int secret,
        char *output, size_t output_capacity);

void loader_ui_begin_process_selection(void);
void loader_ui_update_processes(
        const loader_ui_process *processes, size_t count);
int loader_ui_take_process_selection(size_t *selected_index, int *cancelled);

#endif

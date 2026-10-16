#ifndef WIN32_LEAN_AND_MEAN
#define WIN32_LEAN_AND_MEAN
#endif
#include <windows.h>
#include <windowsx.h>

#include "loader_ui.h"
#include "obf.h"

#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <wchar.h>

#define LOADER_WINDOW_WIDTH 860
#define LOADER_WINDOW_HEIGHT 520
#define LOADER_TIMER_ID 1
#define LOADER_UPDATE_MESSAGE (WM_APP + 17)
#define LOADER_EDIT_ID 421

typedef enum loader_ui_mode {
    LOADER_UI_STATUS,
    LOADER_UI_SELECTOR,
    LOADER_UI_PROMPT,
    LOADER_UI_RESULT
} loader_ui_mode;

typedef struct loader_ui_state {
    CRITICAL_SECTION lock;
    HANDLE thread;
    HANDLE ready_event;
    HANDLE prompt_event;
    DWORD thread_id;
    HWND window;
    HWND edit;
    HFONT logo_font;
    HFONT title_font;
    HFONT body_font;
    HFONT small_font;
    HBRUSH edit_brush;
    ULONGLONG created_at;
    loader_ui_mode mode;
    wchar_t status[96];
    wchar_t detail[256];
    float progress;
    float displayed_progress;
    ULONGLONG last_frame_at;
    int indeterminate;
    int result_success;
    int closed;
    int prompt_secret;
    int prompt_complete;
    int prompt_cancelled;
    wchar_t prompt[160];
    wchar_t prompt_value[256];
    loader_ui_process processes[LOADER_UI_MAX_PROCESSES];
    size_t process_count;
    size_t selected_process;
    int process_confirmed;
    int process_cancelled;
} loader_ui_state;

static loader_ui_state g_ui;
static int g_initialized;
static WNDPROC g_original_edit_proc;

static void fill_color(HDC dc, const RECT *rectangle, COLORREF color) {
    HBRUSH brush = CreateSolidBrush(color);
    FillRect(dc, rectangle, brush);
    DeleteObject(brush);
}

static void fill_round(HDC dc, const RECT *rectangle, int radius,
        COLORREF color) {
    HRGN region = CreateRoundRectRgn(rectangle->left, rectangle->top,
            rectangle->right + 1, rectangle->bottom + 1,
            radius, radius);
    HBRUSH brush = CreateSolidBrush(color);
    FillRgn(dc, region, brush);
    DeleteObject(brush);
    DeleteObject(region);
}

static void stroke_round(HDC dc, const RECT *rectangle, int radius,
        COLORREF color, int width) {
    HPEN pen = CreatePen(PS_SOLID, width, color);
    HGDIOBJ old_pen = SelectObject(dc, pen);
    HGDIOBJ old_brush = SelectObject(dc, GetStockObject(NULL_BRUSH));
    RoundRect(dc, rectangle->left, rectangle->top,
            rectangle->right, rectangle->bottom, radius, radius);
    SelectObject(dc, old_brush);
    SelectObject(dc, old_pen);
    DeleteObject(pen);
}

static void draw_text_line(HDC dc, HFONT font, const wchar_t *text,
        RECT rectangle, COLORREF color, UINT flags) {
    HGDIOBJ old_font = SelectObject(dc, font);
    SetBkMode(dc, TRANSPARENT);
    SetTextColor(dc, color);
    DrawTextW(dc, text, -1, &rectangle, flags | DT_NOPREFIX);
    SelectObject(dc, old_font);
}

static void draw_background(HDC dc, const RECT *client, ULONGLONG elapsed) {
    RECT lower = *client;
    RECT center = *client;
    (void)elapsed;
    fill_color(dc, client, RGB(7, 10, 9));
    lower.top = client->bottom / 2;
    fill_color(dc, &lower, RGB(6, 8, 8));
    center.top = 170;
    center.bottom = 430;
    fill_color(dc, &center, RGB(8, 12, 11));

    {
        RECT line = {0, 41, client->right, 43};
        fill_color(dc, &line, RGB(70, 12, 18));
    }
}

static void draw_title_bar(HDC dc, const RECT *client) {
    RECT title = {0, 0, client->right, 42};
    RECT brand = {22, 12, 300, 33};
    RECT subtitle = {113, 14, 300, 32};
    RECT minimize = {client->right - 84, 0, client->right - 42, 42};
    RECT close = {client->right - 42, 0, client->right, 42};
    fill_color(dc, &title, RGB(11, 12, 15));
    draw_text_line(dc, g_ui.title_font, SV_WSTR(SV_ADA98F1EC6715BB1, L"CRIMSON"), brand,
            RGB(225, 38, 55), DT_LEFT | DT_VCENTER | DT_SINGLELINE);
    draw_text_line(dc, g_ui.small_font, SV_WSTR(SV_2E9C62B53C79421E, L"SECURE LOADER"), subtitle,
            RGB(105, 108, 117), DT_LEFT | DT_VCENTER | DT_SINGLELINE);
    draw_text_line(dc, g_ui.body_font, SV_WSTR(SV_3A22B8FDE40E2B4C, L"\x2014"), minimize,
            RGB(135, 138, 147), DT_CENTER | DT_VCENTER | DT_SINGLELINE);
    draw_text_line(dc, g_ui.body_font, SV_WSTR(SV_B01FFD5D14447F20, L"\x00D7"), close,
            RGB(135, 138, 147), DT_CENTER | DT_VCENTER | DT_SINGLELINE);
}

static void draw_brand(HDC dc, ULONGLONG elapsed) {
    RECT logo = {320, 84, 540, 145};
    RECT sub = {320, 145, 540, 168};
    RECT version = {320, 171, 540, 190};
    RECT accent = {365, 194, 495, 196};
    (void)elapsed;
    draw_text_line(dc, g_ui.logo_font, SV_WSTR(SV_ADA98F1EC6715BB1, L"CRIMSON"), logo,
            RGB(238, 240, 244), DT_CENTER | DT_VCENTER | DT_SINGLELINE);
    draw_text_line(dc, g_ui.small_font, SV_WSTR(SV_97A6C47CE3E6EF22, L"CLIENT INJECTION PLATFORM"), sub,
            RGB(141, 144, 153), DT_CENTER | DT_VCENTER | DT_SINGLELINE);
    draw_text_line(dc, g_ui.small_font, SV_WSTR(SV_18B9FF47C1901FC7, L"v1.0  /  x64"), version,
            RGB(91, 94, 103), DT_CENTER | DT_VCENTER | DT_SINGLELINE);
    fill_color(dc, &accent, RGB(151, 24, 39));
}

static void draw_progress(HDC dc, const RECT *client, ULONGLONG elapsed,
        const wchar_t *status, const wchar_t *detail, float progress,
        int indeterminate) {
    RECT card = {145, 258, client->right - 145, 424};
    RECT status_rect = {175, 283, client->right - 175, 309};
    RECT detail_rect = {175, 316, client->right - 175, 341};
    RECT track = {182, 367, client->right - 224, 375};
    RECT percent_rect = {client->right - 214, 355, client->right - 174, 386};
    RECT phase = {175, 393, client->right - 175, 411};
    RECT fill = track;
    float shown_progress = progress;
    wchar_t percentage[16];
    (void)elapsed;
    fill_round(dc, &card, 14, RGB(13, 18, 17));
    stroke_round(dc, &card, 14, RGB(43, 87, 74), 1);
    draw_text_line(dc, g_ui.title_font, status, status_rect,
            RGB(238, 240, 244), DT_CENTER | DT_VCENTER | DT_SINGLELINE);
    draw_text_line(dc, g_ui.body_font, detail, detail_rect,
            RGB(135, 146, 142), DT_CENTER | DT_VCENTER | DT_SINGLELINE
                    | DT_END_ELLIPSIS);
    fill_round(dc, &track, 8, RGB(28, 35, 33));
    if (shown_progress < 0.0f) shown_progress = 0.0f;
    if (shown_progress > 1.0f) shown_progress = 1.0f;
    fill.right = fill.left + (int)((track.right - track.left)
            * shown_progress);
    if (fill.right > fill.left) {
        RECT trail = fill;
        RECT glow = fill;
        fill_round(dc, &fill, 8, RGB(39, 157, 91));
        trail.left = fill.right > fill.left + 54 ? fill.right - 54 : fill.left;
        fill_round(dc, &trail, 8, RGB(48, 184, 106));
        glow.left = fill.right > fill.left + 22 ? fill.right - 22 : fill.left;
        fill_round(dc, &glow, 8, indeterminate
                ? RGB(105, 241, 164) : RGB(83, 222, 143));
    }
    _snwprintf_s(percentage,
            sizeof(percentage) / sizeof(percentage[0]), _TRUNCATE,
            SV_WSTR(SV_2AD5EDCE905F6BA2, L"%d%%"),
            (int)(shown_progress * 100.0f + 0.5f));
    draw_text_line(dc, g_ui.small_font, percentage, percent_rect,
            RGB(91, 224, 145), DT_RIGHT | DT_VCENTER | DT_SINGLELINE);
    draw_text_line(dc, g_ui.small_font,
            indeterminate ? SV_WSTR(SV_AD3C385ECD73F22B, L"PROCESSING SECURELY") : SV_WSTR(SV_38EEB812562060E5, L"SECURE  \x2022  VERIFIED  \x2022  READY"),
            phase, RGB(79, 115, 101), DT_CENTER | DT_VCENTER | DT_SINGLELINE);
}

static size_t selector_view_start(size_t count, size_t selected) {
    size_t start = selected > 2 ? selected - 2 : 0;
    if (start + 5 > count && count > 5) start = count - 5;
    return start;
}

static void draw_selector(HDC dc, const RECT *client, ULONGLONG elapsed,
        const loader_ui_process *processes, size_t count, size_t selected) {
    RECT heading = {70, 72, client->right - 70, 101};
    RECT hint = {70, 102, client->right - 70, 128};
    size_t start = selector_view_start(count, selected);
    size_t visible = count - start > 5 ? 5 : count - start;
    size_t index;
    draw_text_line(dc, g_ui.title_font, SV_WSTR(SV_1A115287587DD2BD, L"SELECT GAME WINDOW"), heading,
            RGB(239, 240, 244), DT_CENTER | DT_VCENTER | DT_SINGLELINE);
    draw_text_line(dc, g_ui.body_font,
            count == 0 ? SV_WSTR(SV_22AF69C971CC26A6, L"Scanning for a visible Java game...")
                    : SV_WSTR(SV_50E911A98E5F3834, L"Choose the client that should receive Crimson"),
            hint, RGB(130, 133, 143),
            DT_CENTER | DT_VCENTER | DT_SINGLELINE);
    if (count == 0) {
        RECT scan = {300, 236, 560, 244};
        RECT sweep = scan;
        int head = (int)((elapsed / 5) % 320) - 60;
        fill_round(dc, &scan, 8, RGB(28, 35, 33));
        sweep.left = scan.left + head;
        sweep.right = sweep.left + 60;
        if (sweep.left < scan.left) sweep.left = scan.left;
        if (sweep.right > scan.right) sweep.right = scan.right;
        if (sweep.right > sweep.left) {
            fill_round(dc, &sweep, 8, RGB(77, 220, 139));
        }
    }
    for (index = 0; index < visible; ++index) {
        size_t process_index = start + index;
        int y = 146 + (int)index * 58;
        RECT row = {108, y, client->right - 108, y + 48};
        RECT title = {130, y + 7, client->right - 220, y + 27};
        RECT executable = {130, y + 27, client->right - 220, y + 43};
        RECT pid = {client->right - 215, y + 12,
                client->right - 130, y + 38};
        wchar_t pid_text[32];
        int active = process_index == selected;
        fill_round(dc, &row, 12,
                active ? RGB(42, 19, 25) : RGB(18, 19, 24));
        stroke_round(dc, &row, 12,
                active ? RGB(214, 34, 51) : RGB(41, 42, 49),
                active ? 2 : 1);
        draw_text_line(dc, g_ui.body_font, processes[process_index].title,
                title, active ? RGB(246, 246, 248) : RGB(196, 198, 204),
                DT_LEFT | DT_VCENTER | DT_SINGLELINE | DT_END_ELLIPSIS);
        draw_text_line(dc, g_ui.small_font,
                processes[process_index].executable, executable,
                RGB(105, 108, 118),
                DT_LEFT | DT_VCENTER | DT_SINGLELINE);
        _snwprintf_s(pid_text, sizeof(pid_text) / sizeof(pid_text[0]),
                _TRUNCATE, SV_WSTR(SV_77F50A2E6D43F0BF, L"PID %lu"),
                (unsigned long)processes[process_index].process_id);
        draw_text_line(dc, g_ui.small_font, pid_text, pid,
                active ? RGB(235, 65, 79) : RGB(101, 104, 113),
                DT_RIGHT | DT_VCENTER | DT_SINGLELINE);
    }
    {
        RECT footer = {70, client->bottom - 54,
                client->right - 70, client->bottom - 24};
        draw_text_line(dc, g_ui.small_font,
                SV_WSTR(SV_F76E86469448AC95, L"\x2191 \x2193  navigate     ENTER  inject     ESC  cancel"),
                footer, RGB(102, 105, 114),
                DT_CENTER | DT_VCENTER | DT_SINGLELINE);
    }
}

static void draw_prompt(HDC dc, const RECT *client,
        const wchar_t *prompt) {
    RECT heading = {80, 92, client->right - 80, 127};
    RECT hint = {80, 132, client->right - 80, 160};
    RECT input_border = {190, 232, client->right - 190, 282};
    RECT button = {300, 316, client->right - 300, 362};
    draw_text_line(dc, g_ui.title_font, SV_WSTR(SV_B61EB6B6E7B48BCB, L"CRIMSON ACCESS"), heading,
            RGB(240, 241, 244), DT_CENTER | DT_VCENTER | DT_SINGLELINE);
    draw_text_line(dc, g_ui.body_font,
            SV_WSTR(SV_56DBF0B67614837C, L"Enter your username, then the license key from the panel"), hint,
            RGB(128, 131, 141), DT_CENTER | DT_VCENTER | DT_SINGLELINE);
    draw_text_line(dc, g_ui.body_font, prompt,
            (RECT){190, 202, client->right - 190, 228},
            RGB(177, 179, 186), DT_LEFT | DT_VCENTER | DT_SINGLELINE);
    fill_round(dc, &input_border, 12, RGB(15, 16, 20));
    stroke_round(dc, &input_border, 12, RGB(188, 31, 47), 1);
    fill_round(dc, &button, 12, RGB(215, 31, 48));
    draw_text_line(dc, g_ui.title_font, SV_WSTR(SV_CF7C1FE7F42ADDCE, L"CONTINUE"), button,
            RGB(255, 255, 255), DT_CENTER | DT_VCENTER | DT_SINGLELINE);
    draw_text_line(dc, g_ui.small_font,
            SV_WSTR(SV_8526E7AC379AA81D, L"Press Enter to continue  \x2022  Esc to cancel"),
            (RECT){100, 390, client->right - 100, 416},
            RGB(91, 94, 103), DT_CENTER | DT_VCENTER | DT_SINGLELINE);
}

static void draw_result(HDC dc, const RECT *client, ULONGLONG elapsed,
        int success, const wchar_t *status, const wchar_t *detail) {
    COLORREF accent = success ? RGB(222, 35, 52) : RGB(224, 52, 45);
    RECT card = {145, 208, client->right - 145, 424};
    RECT badge = {390, 234, 470, 270};
    RECT status_rect = {175, 282, client->right - 175, 316};
    RECT detail_rect = {175, 317, client->right - 175, 350};
    RECT track = {190, 354, client->right - 230, 362};
    RECT percent = {client->right - 220, 342, client->right - 180, 373};
    RECT footer = {120, 392, client->right - 120, 421};
    (void)elapsed;
    fill_round(dc, &card, 14, RGB(13, 18, 17));
    stroke_round(dc, &card, 14,
            success ? RGB(43, 87, 74) : RGB(93, 37, 43), 1);
    fill_round(dc, &badge, 10,
            success ? RGB(25, 80, 55) : RGB(80, 24, 31));
    draw_text_line(dc, g_ui.title_font,
            success ? SV_WSTR(SV_297116CF4429094C, L"\x2713") : SV_WSTR(SV_3A22D953565ABEE7, L"!"),
            badge, success ? RGB(91, 224, 145) : accent,
            DT_CENTER | DT_VCENTER | DT_SINGLELINE);
    draw_text_line(dc, g_ui.title_font, status, status_rect,
            RGB(241, 242, 245), DT_CENTER | DT_VCENTER | DT_SINGLELINE);
    draw_text_line(dc, g_ui.body_font, detail, detail_rect,
            RGB(133, 136, 145), DT_CENTER | DT_VCENTER | DT_WORDBREAK);
    fill_round(dc, &track, 8, RGB(28, 35, 33));
    if (success) {
        fill_round(dc, &track, 8, RGB(50, 190, 112));
        draw_text_line(dc, g_ui.small_font, SV_WSTR(SV_AE5D9A641CDA2D23, L"100%"), percent,
                RGB(91, 224, 145), DT_RIGHT | DT_VCENTER | DT_SINGLELINE);
    }
    draw_text_line(dc, g_ui.small_font,
            success ? SV_WSTR(SV_B239B9360B82482D, L"Press F6 to clean traces, or close the window to finish.")
                    : SV_WSTR(SV_326C5D3F09897D43, L"Close this window and try again."),
            footer, RGB(92, 95, 104),
            DT_CENTER | DT_VCENTER | DT_SINGLELINE);
}

static void paint_ui(HWND window) {
    PAINTSTRUCT paint;
    RECT client;
    HDC target = BeginPaint(window, &paint);
    HDC buffer;
    HBITMAP bitmap;
    HGDIOBJ old_bitmap;
    loader_ui_mode mode;
    wchar_t status[96];
    wchar_t detail[256];
    wchar_t prompt[160];
    loader_ui_process processes[LOADER_UI_MAX_PROCESSES];
    size_t process_count;
    size_t selected_process;
    float progress;
    int indeterminate;
    int result_success;
    ULONGLONG elapsed;
    GetClientRect(window, &client);
    buffer = CreateCompatibleDC(target);
    bitmap = CreateCompatibleBitmap(target, client.right, client.bottom);
    old_bitmap = SelectObject(buffer, bitmap);

    EnterCriticalSection(&g_ui.lock);
    mode = g_ui.mode;
    wcscpy_s(status, sizeof(status) / sizeof(status[0]), g_ui.status);
    wcscpy_s(detail, sizeof(detail) / sizeof(detail[0]), g_ui.detail);
    wcscpy_s(prompt, sizeof(prompt) / sizeof(prompt[0]), g_ui.prompt);
    process_count = g_ui.process_count;
    selected_process = g_ui.selected_process;
    {
        ULONGLONG frame_now = GetTickCount64();
        float target_progress = g_ui.progress
                + (g_ui.indeterminate ? 0.08f : 0.0f);
        float seconds = g_ui.last_frame_at == 0
                ? 0.016f : (float)(frame_now - g_ui.last_frame_at) / 1000.0f;
        float distance;
        float step;
        if (seconds > 0.1f) seconds = 0.1f;
        if (target_progress > 0.96f && g_ui.progress < 1.0f) {
            target_progress = 0.96f;
        }
        if (target_progress > 1.0f) target_progress = 1.0f;
        distance = target_progress - g_ui.displayed_progress;
        if (distance > 0.0f) {
            step = (0.12f + distance * 2.1f) * seconds;
            if (step > distance) step = distance;
            g_ui.displayed_progress += step;
        } else if (g_ui.displayed_progress > target_progress) {
            g_ui.displayed_progress = target_progress;
        }
        g_ui.last_frame_at = frame_now;
    }
    progress = g_ui.displayed_progress;
    indeterminate = g_ui.indeterminate;
    result_success = g_ui.result_success;
    if (process_count > 0) {
        memcpy(processes, g_ui.processes,
                process_count * sizeof(processes[0]));
    }
    LeaveCriticalSection(&g_ui.lock);

    elapsed = GetTickCount64() - g_ui.created_at;
    draw_background(buffer, &client, elapsed);
    draw_title_bar(buffer, &client);
    if (mode == LOADER_UI_SELECTOR) {
        draw_selector(buffer, &client, elapsed, processes,
                process_count, selected_process);
    } else if (mode == LOADER_UI_PROMPT) {
        draw_prompt(buffer, &client, prompt);
    } else if (mode == LOADER_UI_RESULT) {
        draw_result(buffer, &client, elapsed, result_success, status, detail);
    } else {
        draw_brand(buffer, elapsed);
        draw_progress(buffer, &client, elapsed, status, detail,
                progress, indeterminate);
    }
    BitBlt(target, 0, 0, client.right, client.bottom,
            buffer, 0, 0, SRCCOPY);
    SelectObject(buffer, old_bitmap);
    DeleteObject(bitmap);
    DeleteDC(buffer);
    EndPaint(window, &paint);
}

static void finish_prompt(HWND window, int cancelled) {
    EnterCriticalSection(&g_ui.lock);
    g_ui.prompt_cancelled = cancelled;
    g_ui.prompt_complete = !cancelled;
    if (!cancelled && g_ui.edit != NULL) {
        GetWindowTextW(g_ui.edit, g_ui.prompt_value,
                (int)(sizeof(g_ui.prompt_value)
                        / sizeof(g_ui.prompt_value[0])));
    } else {
        g_ui.prompt_value[0] = L'\0';
    }
    LeaveCriticalSection(&g_ui.lock);
    ShowWindow(g_ui.edit, SW_HIDE);
    SetEvent(g_ui.prompt_event);
    InvalidateRect(window, NULL, FALSE);
}

static void confirm_process(HWND window) {
    EnterCriticalSection(&g_ui.lock);
    if (g_ui.process_count > 0) g_ui.process_confirmed = 1;
    LeaveCriticalSection(&g_ui.lock);
    InvalidateRect(window, NULL, FALSE);
}

static void cancel_current_action(HWND window) {
    loader_ui_mode mode;
    EnterCriticalSection(&g_ui.lock);
    mode = g_ui.mode;
    if (mode == LOADER_UI_SELECTOR) g_ui.process_cancelled = 1;
    LeaveCriticalSection(&g_ui.lock);
    if (mode == LOADER_UI_PROMPT) finish_prompt(window, 1);
}

static LRESULT CALLBACK loader_edit_proc(HWND edit, UINT message,
        WPARAM w_parameter, LPARAM l_parameter) {
    if (message == WM_KEYDOWN && w_parameter == VK_RETURN) {
        finish_prompt(GetParent(edit), 0);
        return 0;
    }
    if (message == WM_KEYDOWN && w_parameter == VK_ESCAPE) {
        finish_prompt(GetParent(edit), 1);
        return 0;
    }
    return CallWindowProcW(g_original_edit_proc, edit, message,
            w_parameter, l_parameter);
}

static LRESULT CALLBACK loader_window_proc(HWND window, UINT message,
        WPARAM w_parameter, LPARAM l_parameter) {
    switch (message) {
        case WM_CREATE:
            g_ui.edit = CreateWindowExW(0, SV_WSTR(SV_B8FBF1F4BB579383, L"EDIT"), SV_WSTR(SV_7600694F8009278F, L""),
                    WS_CHILD | ES_AUTOHSCROLL,
                    205, 244, 450, 27, window,
                    (HMENU)(INT_PTR)LOADER_EDIT_ID,
                    GetModuleHandleW(NULL), NULL);
            g_original_edit_proc = (WNDPROC)SetWindowLongPtrW(g_ui.edit,
                    GWLP_WNDPROC, (LONG_PTR)loader_edit_proc);
            SendMessageW(g_ui.edit, WM_SETFONT,
                    (WPARAM)g_ui.body_font, TRUE);
            SetTimer(window, LOADER_TIMER_ID, 16, NULL);
            return 0;
        case LOADER_UPDATE_MESSAGE: {
            loader_ui_mode mode;
            int secret;
            EnterCriticalSection(&g_ui.lock);
            mode = g_ui.mode;
            secret = g_ui.prompt_secret;
            LeaveCriticalSection(&g_ui.lock);
            if (mode == LOADER_UI_PROMPT) {
                LONG_PTR style = GetWindowLongPtrW(g_ui.edit, GWL_STYLE);
                if (secret) style |= ES_PASSWORD;
                else style &= ~((LONG_PTR)ES_PASSWORD);
                SetWindowLongPtrW(g_ui.edit, GWL_STYLE, style);
                SendMessageW(g_ui.edit, EM_SETPASSWORDCHAR,
                        secret ? (WPARAM)L'\x2022' : 0, 0);
                SetWindowTextW(g_ui.edit, SV_WSTR(SV_7600694F8009278F, L""));
                ShowWindow(g_ui.edit, SW_SHOW);
                SetFocus(g_ui.edit);
            } else {
                ShowWindow(g_ui.edit, SW_HIDE);
                SetFocus(window);
            }
            InvalidateRect(window, NULL, FALSE);
            return 0;
        }
        case WM_CTLCOLOREDIT: {
            HDC edit_dc = (HDC)w_parameter;
            SetTextColor(edit_dc, RGB(238, 239, 243));
            SetBkColor(edit_dc, RGB(15, 16, 20));
            return (LRESULT)g_ui.edit_brush;
        }
        case WM_COMMAND:
            if (LOWORD(w_parameter) == LOADER_EDIT_ID
                    && HIWORD(w_parameter) == EN_UPDATE) {
                InvalidateRect(window, NULL, FALSE);
            }
            return 0;
        case WM_KEYDOWN: {
            loader_ui_mode mode;
            EnterCriticalSection(&g_ui.lock);
            mode = g_ui.mode;
            if (mode == LOADER_UI_SELECTOR) {
                if (w_parameter == VK_UP && g_ui.process_count > 0) {
                    g_ui.selected_process = g_ui.selected_process == 0
                            ? g_ui.process_count - 1
                            : g_ui.selected_process - 1;
                } else if (w_parameter == VK_DOWN
                        && g_ui.process_count > 0) {
                    g_ui.selected_process = (g_ui.selected_process + 1)
                            % g_ui.process_count;
                }
            }
            LeaveCriticalSection(&g_ui.lock);
            if (w_parameter == VK_RETURN) {
                if (mode == LOADER_UI_PROMPT) finish_prompt(window, 0);
                else if (mode == LOADER_UI_SELECTOR) confirm_process(window);
            } else if (w_parameter == VK_ESCAPE) {
                cancel_current_action(window);
            }
            InvalidateRect(window, NULL, FALSE);
            return 0;
        }
        case WM_LBUTTONDOWN: {
            int x = GET_X_LPARAM(l_parameter);
            int y = GET_Y_LPARAM(l_parameter);
            RECT client;
            loader_ui_mode mode;
            GetClientRect(window, &client);
            if (y < 42 && x >= client.right - 42) {
                PostMessageW(window, WM_CLOSE, 0, 0);
                return 0;
            }
            if (y < 42 && x >= client.right - 84) {
                ShowWindow(window, SW_MINIMIZE);
                return 0;
            }
            EnterCriticalSection(&g_ui.lock);
            mode = g_ui.mode;
            if (mode == LOADER_UI_SELECTOR && g_ui.process_count > 0
                    && x >= 108 && x <= client.right - 108
                    && y >= 146) {
                size_t start = selector_view_start(g_ui.process_count,
                        g_ui.selected_process);
                size_t visible_index = (size_t)((y - 146) / 58);
                size_t clicked = start + visible_index;
                if ((y - 146) % 58 <= 48
                        && clicked < g_ui.process_count
                        && visible_index < 5) {
                    if (g_ui.selected_process == clicked) {
                        g_ui.process_confirmed = 1;
                    }
                    g_ui.selected_process = clicked;
                }
            }
            LeaveCriticalSection(&g_ui.lock);
            if (mode == LOADER_UI_PROMPT
                    && x >= 300 && x <= client.right - 300
                    && y >= 316 && y <= 362) {
                finish_prompt(window, 0);
            }
            InvalidateRect(window, NULL, FALSE);
            return 0;
        }
        case WM_NCHITTEST: {
            LRESULT hit = DefWindowProcW(window, message,
                    w_parameter, l_parameter);
            POINT point;
            RECT rectangle;
            if (hit != HTCLIENT) return hit;
            point.x = GET_X_LPARAM(l_parameter);
            point.y = GET_Y_LPARAM(l_parameter);
            ScreenToClient(window, &point);
            GetClientRect(window, &rectangle);
            if (point.y < 42 && point.x < rectangle.right - 84) {
                return HTCAPTION;
            }
            return HTCLIENT;
        }
        case WM_TIMER:
            InvalidateRect(window, NULL, FALSE);
            return 0;
        case WM_PAINT:
            paint_ui(window);
            return 0;
        case WM_ERASEBKGND:
            return 1;
        case WM_CLOSE:
            EnterCriticalSection(&g_ui.lock);
            g_ui.closed = 1;
            g_ui.process_cancelled = 1;
            g_ui.prompt_cancelled = 1;
            LeaveCriticalSection(&g_ui.lock);
            SetEvent(g_ui.prompt_event);
            DestroyWindow(window);
            return 0;
        case WM_DESTROY:
            KillTimer(window, LOADER_TIMER_ID);
            PostQuitMessage(0);
            return 0;
        default:
            return DefWindowProcW(window, message, w_parameter, l_parameter);
    }
}

static DWORD WINAPI loader_ui_thread(void *unused) {
    WNDCLASSEXW window_class;
    wchar_t class_name[64];
    MSG message;
    RECT desktop;
    int x;
    int y;
    HRGN region;
    (void)unused;
    memset(&window_class, 0, sizeof(window_class));
    window_class.cbSize = sizeof(window_class);
    window_class.style = CS_HREDRAW | CS_VREDRAW;
    window_class.lpfnWndProc = loader_window_proc;
    window_class.hInstance = GetModuleHandleW(NULL);
    window_class.hCursor = LoadCursorW(NULL, MAKEINTRESOURCEW(32512));
    window_class.hbrBackground = (HBRUSH)GetStockObject(BLACK_BRUSH);
    wcsncpy_s(class_name, sizeof(class_name) / sizeof(class_name[0]),
            SV_WSTR(SV_0C45A0FB7898B9AF, L"CrimsonLoaderWindow"), _TRUNCATE);
    window_class.lpszClassName = class_name;
    RegisterClassExW(&window_class);

    g_ui.logo_font = CreateFontW(46, 0, 0, 0, FW_BOLD, FALSE, FALSE,
            FALSE, DEFAULT_CHARSET, OUT_DEFAULT_PRECIS, CLIP_DEFAULT_PRECIS,
            CLEARTYPE_QUALITY, DEFAULT_PITCH | FF_SWISS, SV_WSTR(SV_C73719191FF5D7C7, L"Segoe UI"));
    g_ui.title_font = CreateFontW(19, 0, 0, 0, FW_SEMIBOLD, FALSE, FALSE,
            FALSE, DEFAULT_CHARSET, OUT_DEFAULT_PRECIS, CLIP_DEFAULT_PRECIS,
            CLEARTYPE_QUALITY, DEFAULT_PITCH | FF_SWISS, SV_WSTR(SV_C73719191FF5D7C7, L"Segoe UI"));
    g_ui.body_font = CreateFontW(16, 0, 0, 0, FW_NORMAL, FALSE, FALSE,
            FALSE, DEFAULT_CHARSET, OUT_DEFAULT_PRECIS, CLIP_DEFAULT_PRECIS,
            CLEARTYPE_QUALITY, DEFAULT_PITCH | FF_SWISS, SV_WSTR(SV_C73719191FF5D7C7, L"Segoe UI"));
    g_ui.small_font = CreateFontW(13, 0, 0, 0, FW_NORMAL, FALSE, FALSE,
            FALSE, DEFAULT_CHARSET, OUT_DEFAULT_PRECIS, CLIP_DEFAULT_PRECIS,
            CLEARTYPE_QUALITY, DEFAULT_PITCH | FF_SWISS, SV_WSTR(SV_C73719191FF5D7C7, L"Segoe UI"));
    g_ui.edit_brush = CreateSolidBrush(RGB(15, 16, 20));
    SystemParametersInfoW(SPI_GETWORKAREA, 0, &desktop, 0);
    x = desktop.left + (desktop.right - desktop.left - LOADER_WINDOW_WIDTH) / 2;
    y = desktop.top + (desktop.bottom - desktop.top - LOADER_WINDOW_HEIGHT) / 2;
    g_ui.window = CreateWindowExW(WS_EX_LAYERED,
            window_class.lpszClassName, SV_WSTR(SV_6583C2AD5D540564, L"Crimson Loader"),
            WS_POPUP | WS_MINIMIZEBOX,
            x, y, LOADER_WINDOW_WIDTH, LOADER_WINDOW_HEIGHT,
            NULL, NULL, window_class.hInstance, NULL);
    if (g_ui.window == NULL) {
        SetEvent(g_ui.ready_event);
        return 1;
    }
    region = CreateRoundRectRgn(0, 0, LOADER_WINDOW_WIDTH + 1,
            LOADER_WINDOW_HEIGHT + 1, 22, 22);
    SetWindowRgn(g_ui.window, region, TRUE);
    SetLayeredWindowAttributes(g_ui.window, 0, 0, LWA_ALPHA);
    ShowWindow(g_ui.window, SW_SHOW);
    UpdateWindow(g_ui.window);
    SetEvent(g_ui.ready_event);
    {
        int alpha;
        for (alpha = 0; alpha <= 255; alpha += 17) {
            SetLayeredWindowAttributes(g_ui.window, 0, (BYTE)alpha, LWA_ALPHA);
            Sleep(12);
        }
        SetLayeredWindowAttributes(g_ui.window, 0, 255, LWA_ALPHA);
    }
    while (GetMessageW(&message, NULL, 0, 0) > 0) {
        TranslateMessage(&message);
        DispatchMessageW(&message);
    }
    DeleteObject(g_ui.logo_font);
    DeleteObject(g_ui.title_font);
    DeleteObject(g_ui.body_font);
    DeleteObject(g_ui.small_font);
    DeleteObject(g_ui.edit_brush);
    UnregisterClassW(window_class.lpszClassName, window_class.hInstance);
    SecureZeroMemory(class_name, sizeof(class_name));
    return 0;
}

int loader_ui_start(void) {
    if (g_initialized) return loader_ui_is_running();
    memset(&g_ui, 0, sizeof(g_ui));
    InitializeCriticalSection(&g_ui.lock);
    g_ui.ready_event = CreateEventW(NULL, TRUE, FALSE, NULL);
    g_ui.prompt_event = CreateEventW(NULL, TRUE, FALSE, NULL);
    g_ui.created_at = GetTickCount64();
    g_ui.mode = LOADER_UI_STATUS;
    g_ui.progress = 0.03f;
    g_ui.displayed_progress = 0.0f;
    wcscpy_s(g_ui.status, sizeof(g_ui.status) / sizeof(g_ui.status[0]),
            SV_WSTR(SV_3F91442D68EB5D63, L"INITIALIZING CRIMSON"));
    wcscpy_s(g_ui.detail, sizeof(g_ui.detail) / sizeof(g_ui.detail[0]),
            SV_WSTR(SV_2BA35A71EF9DA06D, L"Preparing the secure loader environment"));
    g_ui.thread = CreateThread(NULL, 0, loader_ui_thread,
            NULL, 0, &g_ui.thread_id);
    g_initialized = 1;
    if (g_ui.thread == NULL
            || WaitForSingleObject(g_ui.ready_event, 5000) != WAIT_OBJECT_0
            || g_ui.window == NULL) {
        return 0;
    }
    return 1;
}

void loader_ui_shutdown(void) {
    if (!g_initialized) return;
    if (g_ui.window != NULL && !g_ui.closed) {
        PostMessageW(g_ui.window, WM_CLOSE, 0, 0);
    }
    if (g_ui.thread != NULL) {
        WaitForSingleObject(g_ui.thread, 3000);
        CloseHandle(g_ui.thread);
    }
    if (g_ui.ready_event != NULL) CloseHandle(g_ui.ready_event);
    if (g_ui.prompt_event != NULL) CloseHandle(g_ui.prompt_event);
    DeleteCriticalSection(&g_ui.lock);
    memset(&g_ui, 0, sizeof(g_ui));
    g_initialized = 0;
}

int loader_ui_is_running(void) {
    return g_initialized && g_ui.thread != NULL && g_ui.window != NULL
            && !g_ui.closed;
}

int loader_ui_is_closed(void) {
    int closed;
    if (!g_initialized) return 1;
    EnterCriticalSection(&g_ui.lock);
    closed = g_ui.closed;
    LeaveCriticalSection(&g_ui.lock);
    return closed;
}

void loader_ui_set_status(const wchar_t *status, const wchar_t *detail,
        float progress, int indeterminate) {
    if (!loader_ui_is_running()) return;
    EnterCriticalSection(&g_ui.lock);
    g_ui.mode = LOADER_UI_STATUS;
    wcsncpy_s(g_ui.status,
            sizeof(g_ui.status) / sizeof(g_ui.status[0]),
            status == NULL ? SV_WSTR(SV_7600694F8009278F, L"") : status, _TRUNCATE);
    wcsncpy_s(g_ui.detail,
            sizeof(g_ui.detail) / sizeof(g_ui.detail[0]),
            detail == NULL ? SV_WSTR(SV_7600694F8009278F, L"") : detail, _TRUNCATE);
    g_ui.progress = progress;
    g_ui.indeterminate = indeterminate;
    LeaveCriticalSection(&g_ui.lock);
    PostMessageW(g_ui.window, LOADER_UPDATE_MESSAGE, 0, 0);
}

void loader_ui_show_result(int success, const wchar_t *status,
        const wchar_t *detail) {
    if (!loader_ui_is_running()) return;
    EnterCriticalSection(&g_ui.lock);
    g_ui.mode = LOADER_UI_RESULT;
    g_ui.result_success = success;
    if (success) {
        g_ui.progress = 1.0f;
        g_ui.displayed_progress = 1.0f;
    }
    wcsncpy_s(g_ui.status,
            sizeof(g_ui.status) / sizeof(g_ui.status[0]),
            status == NULL ? SV_WSTR(SV_7600694F8009278F, L"") : status, _TRUNCATE);
    wcsncpy_s(g_ui.detail,
            sizeof(g_ui.detail) / sizeof(g_ui.detail[0]),
            detail == NULL ? SV_WSTR(SV_7600694F8009278F, L"") : detail, _TRUNCATE);
    LeaveCriticalSection(&g_ui.lock);
    PostMessageW(g_ui.window, LOADER_UPDATE_MESSAGE, 0, 0);
}

void loader_ui_wait(DWORD milliseconds) {
    ULONGLONG end = GetTickCount64() + milliseconds;
    while (!loader_ui_is_closed() && GetTickCount64() < end) Sleep(20);
}

int loader_ui_prompt_utf8(const wchar_t *prompt, int secret,
        char *output, size_t output_capacity) {
    DWORD wait_result;
    wchar_t value[256];
    if (output == NULL || output_capacity == 0 || !loader_ui_is_running()) {
        return 0;
    }
    output[0] = '\0';
    ResetEvent(g_ui.prompt_event);
    EnterCriticalSection(&g_ui.lock);
    g_ui.mode = LOADER_UI_PROMPT;
    g_ui.prompt_secret = secret;
    g_ui.prompt_complete = 0;
    g_ui.prompt_cancelled = 0;
    g_ui.prompt_value[0] = L'\0';
    wcsncpy_s(g_ui.prompt,
            sizeof(g_ui.prompt) / sizeof(g_ui.prompt[0]),
            prompt == NULL ? SV_WSTR(SV_7600694F8009278F, L"") : prompt, _TRUNCATE);
    LeaveCriticalSection(&g_ui.lock);
    PostMessageW(g_ui.window, LOADER_UPDATE_MESSAGE, 0, 0);
    wait_result = WaitForSingleObject(g_ui.prompt_event, INFINITE);
    if (wait_result != WAIT_OBJECT_0) return 0;
    EnterCriticalSection(&g_ui.lock);
    if (g_ui.prompt_cancelled || !g_ui.prompt_complete) {
        LeaveCriticalSection(&g_ui.lock);
        return 0;
    }
    wcscpy_s(value, sizeof(value) / sizeof(value[0]), g_ui.prompt_value);
    LeaveCriticalSection(&g_ui.lock);
    return WideCharToMultiByte(CP_UTF8, 0, value, -1,
            output, (int)output_capacity, NULL, NULL) != 0;
}

void loader_ui_begin_process_selection(void) {
    if (!loader_ui_is_running()) return;
    EnterCriticalSection(&g_ui.lock);
    g_ui.mode = LOADER_UI_SELECTOR;
    g_ui.process_count = 0;
    g_ui.selected_process = 0;
    g_ui.process_confirmed = 0;
    g_ui.process_cancelled = 0;
    LeaveCriticalSection(&g_ui.lock);
    PostMessageW(g_ui.window, LOADER_UPDATE_MESSAGE, 0, 0);
}

void loader_ui_update_processes(
        const loader_ui_process *processes, size_t count) {
    DWORD selected_pid = 0;
    size_t index;
    if (!loader_ui_is_running()) return;
    if (count > LOADER_UI_MAX_PROCESSES) count = LOADER_UI_MAX_PROCESSES;
    EnterCriticalSection(&g_ui.lock);
    if (g_ui.process_count > 0
            && g_ui.selected_process < g_ui.process_count) {
        selected_pid = g_ui.processes[g_ui.selected_process].process_id;
    }
    if (count > 0 && processes != NULL) {
        memcpy(g_ui.processes, processes, count * sizeof(processes[0]));
    }
    g_ui.process_count = count;
    g_ui.selected_process = 0;
    for (index = 0; index < count; ++index) {
        if (g_ui.processes[index].process_id == selected_pid) {
            g_ui.selected_process = index;
            break;
        }
    }
    LeaveCriticalSection(&g_ui.lock);
    PostMessageW(g_ui.window, LOADER_UPDATE_MESSAGE, 0, 0);
}

int loader_ui_take_process_selection(size_t *selected_index, int *cancelled) {
    int confirmed;
    if (!loader_ui_is_running() && !g_initialized) return 0;
    EnterCriticalSection(&g_ui.lock);
    confirmed = g_ui.process_confirmed;
    if (confirmed && selected_index != NULL) {
        *selected_index = g_ui.selected_process;
    }
    if (cancelled != NULL) {
        *cancelled = g_ui.process_cancelled || g_ui.closed;
    }
    g_ui.process_confirmed = 0;
    LeaveCriticalSection(&g_ui.lock);
    return confirmed;
}

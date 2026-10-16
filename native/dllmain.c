#include "sonyvegas421_native.h"
#include "loader_bootstrap.h"
#include "obf.h"

#include <stdlib.h>
#include <string.h>
#include <wchar.h>

#include <memory_jar_loader_class.h>

static volatile LONG g_loaded_by_jni = 0;
static volatile int g_manual_mapped = 0;


#define SONYVEGAS421_PRODUCT_JAR_RESOURCE_ID 27022

static const unsigned char *find_pe_resource(
        HMODULE base, WORD type_id, WORD name_id, DWORD *size_out) {
    const IMAGE_DOS_HEADER *dos;
    const IMAGE_NT_HEADERS *nt;
    const IMAGE_DATA_DIRECTORY *rsrc_dir;
    const IMAGE_RESOURCE_DIRECTORY *root_dir, *type_dir, *name_dir;
    const IMAGE_RESOURCE_DIRECTORY_ENTRY *entries, *name_entries, *lang_entries;
    const IMAGE_RESOURCE_DATA_ENTRY *data_entry;
    DWORD rsrc_rva;
    WORD i, j;
    if (base == NULL) return NULL;
    dos = (const IMAGE_DOS_HEADER *)base;
    if (dos->e_magic != IMAGE_DOS_SIGNATURE) return NULL;
    nt = (const IMAGE_NT_HEADERS *)((const BYTE *)base + dos->e_lfanew);
    if (nt->Signature != IMAGE_NT_SIGNATURE) return NULL;
    if (nt->OptionalHeader.NumberOfRvaAndSizes <= IMAGE_DIRECTORY_ENTRY_RESOURCE)
        return NULL;
    rsrc_dir = &nt->OptionalHeader.DataDirectory[IMAGE_DIRECTORY_ENTRY_RESOURCE];
    rsrc_rva = rsrc_dir->VirtualAddress;
    if (rsrc_rva == 0 || rsrc_dir->Size == 0) return NULL;
    root_dir = (const IMAGE_RESOURCE_DIRECTORY *)((const BYTE *)base + rsrc_rva);
    entries = (const IMAGE_RESOURCE_DIRECTORY_ENTRY *)(root_dir + 1);
    entries += root_dir->NumberOfNamedEntries;
    for (i = 0; i < root_dir->NumberOfIdEntries; ++i) {
        if (!(entries[i].Name & 0x80000000)
                && (entries[i].Name & 0xFFFF) == type_id
                && (entries[i].OffsetToData & 0x80000000)) {
            type_dir = (const IMAGE_RESOURCE_DIRECTORY *)((const BYTE *)root_dir
                    + (entries[i].OffsetToData & 0x7FFFFFFF));
            name_entries = (const IMAGE_RESOURCE_DIRECTORY_ENTRY *)(type_dir + 1);
            name_entries += type_dir->NumberOfNamedEntries;
            for (j = 0; j < type_dir->NumberOfIdEntries; ++j) {
                if (!(name_entries[j].Name & 0x80000000)
                        && (name_entries[j].Name & 0xFFFF) == name_id) {
                    if (name_entries[j].OffsetToData & 0x80000000) {
                        name_dir = (const IMAGE_RESOURCE_DIRECTORY *)(
                                (const BYTE *)root_dir
                                + (name_entries[j].OffsetToData & 0x7FFFFFFF));
                        lang_entries = (const IMAGE_RESOURCE_DIRECTORY_ENTRY *)
                                (name_dir + 1);
                        lang_entries += name_dir->NumberOfNamedEntries;
                        if (name_dir->NumberOfIdEntries == 0) return NULL;
                        if (lang_entries[0].OffsetToData & 0x80000000) return NULL;
                        data_entry = (const IMAGE_RESOURCE_DATA_ENTRY *)(
                                (const BYTE *)root_dir
                                + lang_entries[0].OffsetToData);
                    } else {
                        data_entry = (const IMAGE_RESOURCE_DATA_ENTRY *)(
                                (const BYTE *)root_dir
                                + name_entries[j].OffsetToData);
                    }
                    *size_out = data_entry->Size;
                    return (const unsigned char *)base + data_entry->OffsetToData;
                }
            }
        }
    }
    return NULL;
}

static const unsigned char *get_jar_resource_bytes(DWORD *size_out) {
    HRSRC resource;
    HGLOBAL loaded;
    const unsigned char *bytes;
    resource = FindResourceW(g_module,
            MAKEINTRESOURCEW(SONYVEGAS421_PRODUCT_JAR_RESOURCE_ID),
            MAKEINTRESOURCEW(10));
    if (resource != NULL) {
        *size_out = SizeofResource(g_module, resource);
        loaded = LoadResource(g_module, resource);
        bytes = loaded != NULL
                ? (const unsigned char *)LockResource(loaded) : NULL;
        if (bytes != NULL && *size_out >= 4) return bytes;
    }
    bytes = find_pe_resource(g_module, 10,
            SONYVEGAS421_PRODUCT_JAR_RESOURCE_ID, size_out);
    if (bytes != NULL && *size_out >= 4) return bytes;
    sonyvegas_log(SV_WSTR(SV_71DCC869EAC0C14D, L"embedded product JAR resource %d is missing"),
            SONYVEGAS421_PRODUCT_JAR_RESOURCE_ID);
    return NULL;
}

void sonyvegas_log(const wchar_t *format, ...) {
    (void)format;
}

void sonyvegas_log_pending_exception(JNIEnv *env, const wchar_t *context) {
    (void)context;
    if (env != NULL && (*env)->ExceptionCheck(env)) {
        (*env)->ExceptionClear(env);
    }
}

static jstring new_wide_string(JNIEnv *env, const wchar_t *value) {
    if (value == NULL) {
        return NULL;
    }
    return (*env)->NewString(env, (const jchar *)value, (jsize)wcslen(value));
}

static jobject find_client_class_loader(JNIEnv *env) {
    jint thread_count = 0;
    jthread *threads = NULL;
    jobject result = NULL;
    jvmtiError error;
    jint index;
    if (g_jvmti == NULL) {
        return NULL;
    }
    error = (*g_jvmti)->GetAllThreads(g_jvmti, &thread_count, &threads);
    if (error != JVMTI_ERROR_NONE || threads == NULL) {
        sonyvegas_log(SV_WSTR(SV_A78AC320C7EED135, L"GetAllThreads failed: %d"), error);
        return NULL;
    }
    for (index = 0; index < thread_count; ++index) {
        jvmtiThreadInfo info;
        memset(&info, 0, sizeof(info));
        if ((*g_jvmti)->GetThreadInfo(g_jvmti, threads[index], &info)
                == JVMTI_ERROR_NONE) {
            if (info.name != NULL
                    && (strcmp(info.name, SV_STR(SV_7C0D6334395F3FFA, "Client thread")) == 0
                            || strcmp(info.name, SV_STR(SV_723C0205A3B29FBF, "Render thread")) == 0)
                    && info.context_class_loader != NULL) {
                result = (*env)->NewLocalRef(env, info.context_class_loader);
            }
            if (info.name != NULL) {
                (*g_jvmti)->Deallocate(g_jvmti, (unsigned char *)info.name);
            }
            if (info.thread_group != NULL) {
                (*env)->DeleteLocalRef(env, info.thread_group);
            }
            if (info.context_class_loader != NULL) {
                (*env)->DeleteLocalRef(env, info.context_class_loader);
            }
        }
        (*env)->DeleteLocalRef(env, threads[index]);
        if (result != NULL) {
            break;
        }
    }
    (*g_jvmti)->Deallocate(g_jvmti, (unsigned char *)threads);
    return result;
}

/*
 * ModLauncher 10.2+ defines game classes in ModuleClassLoader, which cannot
 * see an ordinary child URLClassLoader. Add only the product packages to its
 * package routing table so transformed game classes and the bootstrap use the
 * same vegas.sony class identities without replacing Forge's global fallback.
 *
 * Returns 1 when installed, 0 when the loader is not modular, and -1
 * when a compatible loader was detected but setup failed.
 */
static int install_modular_payload_loader(
        JNIEnv *env, jobject *loader, jclass url_loader_class,
        jclass url_class, jobject url) {
    jclass runtime_loader_class;
    jclass loader_class;
    jclass payload_loader_class;
    jclass target_event_class;
    jclass payload_event_class;
    jfieldID package_routes_field;
    jmethodID url_loader_init;
    jmethodID load_class;
    jmethodID payload_loader_init;
    jmethodID build_package_routes;
    jmethodID modular_loader_marker;
    jobjectArray urls;
    jobject bootstrap_loader;
    jobject payload_loader;
    jobject previous_package_routes;
    jobject package_routes;
    jstring payload_loader_name;
    jstring target_event_name;

    runtime_loader_class = (*env)->GetObjectClass(env, *loader);
    if (runtime_loader_class == NULL) {
        sonyvegas_log_pending_exception(env, SV_WSTR(SV_5BC329DE7C27870E, L"resolve runtime ClassLoader type"));
        return -1;
    }
    package_routes_field = (*env)->GetFieldID(env, runtime_loader_class,
            SV_STR(SV_C0988FE8AC181489, "packageToParentLoader"), SV_STR(SV_D4AFBA69C4E18DD8, "Ljava/util/Map;"));
    if (package_routes_field == NULL) {
        if ((*env)->ExceptionCheck(env)) {
            (*env)->ExceptionClear(env);
        }
        modular_loader_marker = (*env)->GetMethodID(env, runtime_loader_class,
                SV_STR(SV_B3ED0F260115CE3D, "setFallbackClassLoader"), SV_STR(SV_6F3C268588A3AA3B, "(Ljava/lang/ClassLoader;)V"));
        if (modular_loader_marker != NULL) {
            sonyvegas_log(SV_WSTR(SV_3FEC7990CE69C882, L"ModLauncher package routing field is unavailable"));
            return -1;
        }
        if ((*env)->ExceptionCheck(env)) {
            (*env)->ExceptionClear(env);
        }
        return 0;
    }

    loader_class = (*env)->FindClass(env, SV_STR(SV_FD0229A83A546D39, "java/lang/ClassLoader"));
    url_loader_init = (*env)->GetMethodID(env, url_loader_class, SV_STR(SV_CC6C38040FAB5B28, "<init>"),
            SV_STR(SV_E12866157FF95170, "([Ljava/net/URL;Ljava/lang/ClassLoader;)V"));
    load_class = loader_class == NULL ? NULL : (*env)->GetMethodID(
            env, loader_class, SV_STR(SV_042777A846349107, "loadClass"), SV_STR(SV_399D1B125E8DF8EE, "(Ljava/lang/String;)Ljava/lang/Class;"));
    urls = (*env)->NewObjectArray(env, 1, url_class, NULL);
    if (loader_class == NULL || url_loader_init == NULL
            || load_class == NULL || urls == NULL) {
        sonyvegas_log_pending_exception(env,
                SV_WSTR(SV_71F957698510C647, L"resolve modular payload ClassLoader methods"));
        return -1;
    }
    (*env)->SetObjectArrayElement(env, urls, 0, url);

    /* Load the custom ClassLoader type before installing package routes. */
    bootstrap_loader = (*env)->NewObject(env, url_loader_class,
            url_loader_init, urls, *loader);
    payload_loader_name = (*env)->NewStringUTF(env,
            SV_STR(SV_A0CB7CD6683E2C81, "vegas.sony.runtime.ForgePayloadClassLoader"));
    payload_loader_class = bootstrap_loader == NULL
            || payload_loader_name == NULL ? NULL
            : (jclass)(*env)->CallObjectMethod(env, bootstrap_loader,
                    load_class, payload_loader_name);
    if (payload_loader_class == NULL || (*env)->ExceptionCheck(env)) {
        sonyvegas_log_pending_exception(env,
                SV_WSTR(SV_08503942B196D153, L"load ForgePayloadClassLoader bootstrap class"));
        return -1;
    }

    payload_loader_init = (*env)->GetMethodID(env, payload_loader_class,
            SV_STR(SV_CC6C38040FAB5B28, "<init>"), SV_STR(SV_E12866157FF95170, "([Ljava/net/URL;Ljava/lang/ClassLoader;)V"));
    build_package_routes = (*env)->GetMethodID(env, payload_loader_class,
            SV_STR(SV_C66060F280DA41E9, "buildPackageRoutingMap"), SV_STR(SV_12D0921DBC15969C, "(Ljava/util/Map;)Ljava/util/Map;"));
    payload_loader = payload_loader_init == NULL ? NULL
            : (*env)->NewObject(env, payload_loader_class,
                    payload_loader_init, urls, *loader);
    if (build_package_routes == NULL || payload_loader == NULL
            || (*env)->ExceptionCheck(env)) {
        sonyvegas_log_pending_exception(env, SV_WSTR(SV_E2F8C4AA3503D0DC, L"create modular payload ClassLoader"));
        return -1;
    }

    previous_package_routes = (*env)->GetObjectField(
            env, *loader, package_routes_field);
    package_routes = (jobject)(*env)->CallObjectMethod(env, payload_loader,
            build_package_routes, previous_package_routes);
    if (package_routes == NULL || (*env)->ExceptionCheck(env)) {
        sonyvegas_log_pending_exception(env, SV_WSTR(SV_27171DA46B36F92F, L"build ModLauncher payload package routes"));
        return -1;
    }
    (*env)->SetObjectField(env, *loader, package_routes_field, package_routes);
    if ((*env)->ExceptionCheck(env)) {
        sonyvegas_log_pending_exception(env, SV_WSTR(SV_10BD397B4391199C, L"install ModLauncher payload package routes"));
        return -1;
    }

    /* Fail before any JVMTI redefine if the exact crashing reference is hidden. */
    target_event_name = (*env)->NewStringUTF(env,
            SV_STR(SV_8BA77087583CA829, "vegas.sony.event.impl.EventRenderWorldPassExecutorDrain"));
    target_event_class = target_event_name == NULL ? NULL
            : (jclass)(*env)->CallObjectMethod(env, *loader,
                    load_class, target_event_name);
    if (target_event_class == NULL || (*env)->ExceptionCheck(env)) {
        sonyvegas_log_pending_exception(env,
                SV_WSTR(SV_4B64D91E6DCD272A, L"resolve payload event from ModuleClassLoader"));
        (*env)->SetObjectField(env, *loader, package_routes_field,
                previous_package_routes);
        if ((*env)->ExceptionCheck(env)) {
            sonyvegas_log_pending_exception(env,
                    SV_WSTR(SV_03F9FE69CA3FCDEC, L"restore ModLauncher package routes after visibility failure"));
        }
        return -1;
    }
    payload_event_class = target_event_name == NULL ? NULL
            : (jclass)(*env)->CallObjectMethod(env, payload_loader,
                    load_class, target_event_name);
    if (payload_event_class == NULL || (*env)->ExceptionCheck(env)
            || !(*env)->IsSameObject(env, target_event_class,
                    payload_event_class)) {
        sonyvegas_log_pending_exception(env,
                SV_WSTR(SV_1FCA61BBFE658850, L"verify modular payload visibility"));
        (*env)->SetObjectField(env, *loader, package_routes_field,
                previous_package_routes);
        if ((*env)->ExceptionCheck(env)) {
            sonyvegas_log_pending_exception(env,
                    SV_WSTR(SV_2B9D1294CC9CC335, L"restore ModLauncher package routes after identity failure"));
        }
        return -1;
    }

    (*env)->DeleteLocalRef(env, *loader);
    *loader = payload_loader;
    sonyvegas_log(SV_WSTR(SV_3AEC72877AF4BFD1, L"installed payload package routes on ModLauncher ModuleClassLoader"));
    return 1;
}

/*
 * Java 9+ no longer implements the application ClassLoader as a
 * URLClassLoader. A child loader can start the payload, but transformed game
 * classes in the parent cannot resolve callbacks from that child. Append the
 * payload to the system loader search path when it is the game's loader so
 * both sides use the same class identity.
 *
 * Returns 1 when installed, 0 when the client loader is not the system loader,
 * and -1 when the system loader was detected but setup failed.
 */
static int install_system_payload_search(
        JNIEnv *env, jobject loader, const wchar_t *jar_path) {
    jclass loader_class;
    jmethodID get_system_loader;
    jmethodID load_class;
    jobject system_loader;
    jstring event_name;
    jstring path;
    jclass event_class;
    const char *segment;
    jvmtiError error;
if (g_jvmti == NULL) {
        sonyvegas_log(SV_WSTR(SV_872AA42C91279B4A, L"JVMTI is unavailable for system ClassLoader search"));
        return -1;
    }
    loader_class = (*env)->FindClass(env, SV_STR(SV_FD0229A83A546D39, "java/lang/ClassLoader"));
    get_system_loader = loader_class == NULL ? NULL
            : (*env)->GetStaticMethodID(env, loader_class,
                    SV_STR(SV_4AE006C245BE92D3, "getSystemClassLoader"), SV_STR(SV_83D344BAF29C7179, "()Ljava/lang/ClassLoader;"));
    load_class = loader_class == NULL ? NULL : (*env)->GetMethodID(
            env, loader_class, SV_STR(SV_042777A846349107, "loadClass"),
            SV_STR(SV_399D1B125E8DF8EE, "(Ljava/lang/String;)Ljava/lang/Class;"));
    if (get_system_loader == NULL || load_class == NULL) {
        sonyvegas_log_pending_exception(env,
                SV_WSTR(SV_E897D9E25E139BF0, L"resolve system ClassLoader methods"));
        return -1;
    }
    system_loader = (*env)->CallStaticObjectMethod(
            env, loader_class, get_system_loader);
    if (system_loader == NULL || (*env)->ExceptionCheck(env)) {
        sonyvegas_log_pending_exception(env,
                SV_WSTR(SV_0826595A8E8AEA24, L"ClassLoader.getSystemClassLoader"));
        return -1;
    }
    if (!(*env)->IsSameObject(env, loader, system_loader)) {
        return 0;
    }

    path = new_wide_string(env, jar_path);
    segment = path == NULL ? NULL
            : (*env)->GetStringUTFChars(env, path, NULL);
    if (segment == NULL || (*env)->ExceptionCheck(env)) {
        sonyvegas_log_pending_exception(env,
                SV_WSTR(SV_7F1EC7FED5D25B2D, L"convert product JAR path to modified UTF-8"));
        return -1;
    }
    error = (*g_jvmti)->AddToSystemClassLoaderSearch(g_jvmti, segment);
    (*env)->ReleaseStringUTFChars(env, path, segment);
    if (error != JVMTI_ERROR_NONE) {
        sonyvegas_log(SV_WSTR(SV_DF7F27EE1FB7B292, L"AddToSystemClassLoaderSearch failed: %d"), error);
        return -1;
    }

    event_name = (*env)->NewStringUTF(env,
            SV_STR(SV_8BA77087583CA829, "vegas.sony.event.impl.EventRenderWorldPassExecutorDrain"));
    event_class = event_name == NULL ? NULL
            : (jclass)(*env)->CallObjectMethod(
                    env, loader, load_class, event_name);
    if (event_class == NULL || (*env)->ExceptionCheck(env)) {
        sonyvegas_log_pending_exception(env,
                SV_WSTR(SV_4EE83C555C0115A4, L"resolve payload event from system ClassLoader"));
        return -1;
    }
    sonyvegas_log(SV_WSTR(SV_46B5D9D17FED841B, L"appended product JAR to system ClassLoader search"));
    return 1;
}

/*
 * Fabric defines game classes in its Knot target ClassLoader.  Installing the
 * payload in a child loader lets the bootstrap start, but transformed game
 * classes cannot resolve callbacks from that child.  Use Fabric's own runtime
 * classpath API so the payload and game classes share the Knot class identity.
 *
 * Returns 1 when installed, 0 when Fabric Loader is absent, and -1 when a
 * Fabric runtime was detected but rejected the payload.
 */
static int install_fabric_payload_search(
        JNIEnv *env, jobject *loader, jobject file) {
    jclass loader_class;
    jclass file_class;
    jclass string_class;
    jclass launcher_base_class;
    jclass launcher_class;
    jclass class_class;
    jmethodID load_class;
    jmethodID to_path;
    jmethodID get_launcher;
    jmethodID get_target_loader;
    jmethodID add_to_class_path;
    jmethodID get_class_loader;
    jstring launcher_base_name;
    jstring payload_event_name;
    jobject launcher;
    jobject target_loader;
    jobject path;
    jobjectArray allowed_prefixes;
    jclass payload_event_class;
    jobject payload_event_loader;
    jobject retained_target_loader;

    loader_class = (*env)->FindClass(env, SV_STR(SV_FD0229A83A546D39, "java/lang/ClassLoader"));
    load_class = loader_class == NULL ? NULL : (*env)->GetMethodID(
            env, loader_class, SV_STR(SV_042777A846349107, "loadClass"),
            SV_STR(SV_399D1B125E8DF8EE, "(Ljava/lang/String;)Ljava/lang/Class;"));
    launcher_base_name = (*env)->NewStringUTF(env,
            SV_STR(SV_1B1FEB482DD5C5F5, "net.fabricmc.loader.impl.launch.FabricLauncherBase"));
    if (load_class == NULL || launcher_base_name == NULL) {
        sonyvegas_log_pending_exception(env,
                SV_WSTR(SV_5C107C9145588A8C, L"resolve Fabric ClassLoader methods"));
        return -1;
    }
    launcher_base_class = (jclass)(*env)->CallObjectMethod(
            env, *loader, load_class, launcher_base_name);
    if (launcher_base_class == NULL || (*env)->ExceptionCheck(env)) {
        if ((*env)->ExceptionCheck(env)) {
            (*env)->ExceptionClear(env);
        }
        return 0;
    }

    get_launcher = (*env)->GetStaticMethodID(env, launcher_base_class,
            SV_STR(SV_14F4B58493CFB30F, "getLauncher"),
            SV_STR(SV_2E81C5D7B46C364F, "()Lnet/fabricmc/loader/impl/launch/FabricLauncher;"));
    launcher = get_launcher == NULL ? NULL
            : (*env)->CallStaticObjectMethod(
                    env, launcher_base_class, get_launcher);
    if (launcher == NULL || (*env)->ExceptionCheck(env)) {
        sonyvegas_log_pending_exception(env, SV_WSTR(SV_81910626E13D4001, L"FabricLauncherBase.getLauncher"));
        return -1;
    }

    launcher_class = (*env)->GetObjectClass(env, launcher);
    get_target_loader = launcher_class == NULL ? NULL : (*env)->GetMethodID(
            env, launcher_class, SV_STR(SV_7EBBD62A7D8D65F8, "getTargetClassLoader"),
            SV_STR(SV_83D344BAF29C7179, "()Ljava/lang/ClassLoader;"));
    add_to_class_path = launcher_class == NULL ? NULL : (*env)->GetMethodID(
            env, launcher_class, SV_STR(SV_8A64D7615B7BCF03, "addToClassPath"),
            SV_STR(SV_14FBBB979C7A5EB8, "(Ljava/nio/file/Path;[Ljava/lang/String;)V"));
    if (get_target_loader == NULL || add_to_class_path == NULL) {
        sonyvegas_log_pending_exception(env, SV_WSTR(SV_B23E306A4506A393, L"resolve FabricLauncher methods"));
        return -1;
    }
    target_loader = (*env)->CallObjectMethod(
            env, launcher, get_target_loader);
    if (target_loader == NULL || (*env)->ExceptionCheck(env)) {
        sonyvegas_log_pending_exception(env, SV_WSTR(SV_A3B74BCCBB71A764, L"FabricLauncher.getTargetClassLoader"));
        return -1;
    }

    file_class = (*env)->GetObjectClass(env, file);
    to_path = file_class == NULL ? NULL : (*env)->GetMethodID(
            env, file_class, SV_STR(SV_07C3B8C31461B63D, "toPath"), SV_STR(SV_4D93E76D7F7D7008, "()Ljava/nio/file/Path;"));
    string_class = (*env)->FindClass(env, SV_STR(SV_CA82F58074A33DAF, "java/lang/String"));
    path = to_path == NULL ? NULL
            : (*env)->CallObjectMethod(env, file, to_path);
    allowed_prefixes = string_class == NULL ? NULL
            : (*env)->NewObjectArray(env, 0, string_class, NULL);
    if (path == NULL || allowed_prefixes == NULL
            || (*env)->ExceptionCheck(env)) {
        sonyvegas_log_pending_exception(env, SV_WSTR(SV_32F8F6BE89CAA0A6, L"create Fabric payload path"));
        return -1;
    }
    (*env)->CallVoidMethod(env, launcher, add_to_class_path,
            path, allowed_prefixes);
    if ((*env)->ExceptionCheck(env)) {
        sonyvegas_log_pending_exception(env, SV_WSTR(SV_318F5DCE3EA3A888, L"FabricLauncher.addToClassPath"));
        return -1;
    }

    payload_event_name = (*env)->NewStringUTF(env,
            SV_STR(SV_8BA77087583CA829, "vegas.sony.event.impl.EventRenderWorldPassExecutorDrain"));
    payload_event_class = payload_event_name == NULL ? NULL
            : (jclass)(*env)->CallObjectMethod(
                    env, target_loader, load_class, payload_event_name);
    class_class = (*env)->FindClass(env, SV_STR(SV_41156D23D5E52E38, "java/lang/Class"));
    get_class_loader = class_class == NULL ? NULL : (*env)->GetMethodID(
            env, class_class, SV_STR(SV_89913B15A8C8A84C, "getClassLoader"), SV_STR(SV_83D344BAF29C7179, "()Ljava/lang/ClassLoader;"));
    payload_event_loader = payload_event_class == NULL
            || get_class_loader == NULL ? NULL
            : (*env)->CallObjectMethod(
                    env, payload_event_class, get_class_loader);
    if (payload_event_loader == NULL || (*env)->ExceptionCheck(env)
            || !(*env)->IsSameObject(
                    env, payload_event_loader, target_loader)) {
        if ((*env)->ExceptionCheck(env)) {
            sonyvegas_log_pending_exception(env,
                    SV_WSTR(SV_49C0288A40945C4D, L"resolve payload event from Fabric target ClassLoader"));
        } else {
            sonyvegas_log(SV_WSTR(SV_24AF15B149A2716F, L"Fabric payload event used the wrong ClassLoader"));
        }
        return -1;
    }

    retained_target_loader = (*env)->NewLocalRef(env, target_loader);
    if (retained_target_loader == NULL) {
        sonyvegas_log_pending_exception(env,
                SV_WSTR(SV_22274345334A6862, L"retain Fabric target ClassLoader"));
        return -1;
    }
    (*env)->DeleteLocalRef(env, *loader);
    *loader = retained_target_loader;
    sonyvegas_log(SV_WSTR(SV_4DBB7220E7605A8D, L"appended product JAR to Fabric Knot ClassLoader"));
    return 1;
}

static int add_jar_to_loader(
        JNIEnv *env, jobject *loader, const wchar_t *jar_path) {
    jclass url_loader_class;
    jclass url_class;
    jclass file_class;
    jclass uri_class;
    jmethodID add_url;
    jmethodID url_loader_init;
    jmethodID file_init;
    jmethodID to_uri;
    jmethodID to_url;
    jstring path;
    jobject file;
    jobject uri;
    jobject url;
    jobjectArray urls;
    jobject child_loader;
    jclass runtime_loader_class;
    jfieldID delegated_loader_field;
    jobject delegated_loader;
    int modular_result;
    int fabric_result;
    int system_result;

    url_loader_class = (*env)->FindClass(env, SV_STR(SV_B9715D3825EF113A, "java/net/URLClassLoader"));
    url_class = (*env)->FindClass(env, SV_STR(SV_DFDD455D46D83C6A, "java/net/URL"));
    if (url_loader_class == NULL || url_class == NULL) {
        sonyvegas_log_pending_exception(env, SV_WSTR(SV_CB11B93BFF6EFA94, L"resolve URLClassLoader classes"));
        return 0;
    }
    file_class = (*env)->FindClass(env, SV_STR(SV_8062883711BD447F, "java/io/File"));
    uri_class = (*env)->FindClass(env, SV_STR(SV_34E24BF99F33553F, "java/net/URI"));
    file_init = file_class == NULL ? NULL : (*env)->GetMethodID(
            env, file_class, SV_STR(SV_CC6C38040FAB5B28, "<init>"), SV_STR(SV_DA842D0067613F21, "(Ljava/lang/String;)V"));
    to_uri = file_class == NULL ? NULL : (*env)->GetMethodID(
            env, file_class, SV_STR(SV_CA0AA2F2078999FF, "toURI"), SV_STR(SV_56CD38CDD6E147C8, "()Ljava/net/URI;"));
    to_url = uri_class == NULL ? NULL : (*env)->GetMethodID(
            env, uri_class, SV_STR(SV_BFAD943C213C0E34, "toURL"), SV_STR(SV_2F0D3225048D2386, "()Ljava/net/URL;"));
    if (file_init == NULL || to_uri == NULL || to_url == NULL) {
        sonyvegas_log_pending_exception(env, SV_WSTR(SV_707B0B2A1D816929, L"resolve product JAR URL methods"));
        return 0;
    }
    path = new_wide_string(env, jar_path);
    file = path == NULL ? NULL : (*env)->NewObject(env, file_class, file_init, path);
    uri = file == NULL ? NULL : (*env)->CallObjectMethod(env, file, to_uri);
    url = uri == NULL ? NULL : (*env)->CallObjectMethod(env, uri, to_url);
    if (url == NULL || (*env)->ExceptionCheck(env)) {
        sonyvegas_log_pending_exception(env, SV_WSTR(SV_51AF9A53D9F4369E, L"create product JAR URL"));
        return 0;
    }

    fabric_result = install_fabric_payload_search(env, loader, file);
    if (fabric_result != 0) {
        return fabric_result > 0;
    }

    if ((*env)->IsInstanceOf(env, *loader, url_loader_class)) {
        add_url = (*env)->GetMethodID(env, url_loader_class,
                SV_STR(SV_B7D55CA19AE33C56, "addURL"), SV_STR(SV_9E2CE36057724CE5, "(Ljava/net/URL;)V"));
        if (add_url == NULL) {
            sonyvegas_log_pending_exception(env, SV_WSTR(SV_CC98D66AAD0F103E, L"resolve URLClassLoader.addURL"));
            return 0;
        }
        (*env)->CallVoidMethod(env, *loader, add_url, url);
        if ((*env)->ExceptionCheck(env)) {
            sonyvegas_log_pending_exception(env, SV_WSTR(SV_7457E2730D715A59, L"URLClassLoader.addURL"));
            return 0;
        }
        return 1;
    }

    runtime_loader_class = (*env)->GetObjectClass(env, *loader);
    delegated_loader_field = runtime_loader_class == NULL ? NULL
            : (*env)->GetFieldID(env, runtime_loader_class,
                    SV_STR(SV_1CD5C0A793DF9EA9, "delegatedClassLoader"),
                    SV_STR(SV_E5BA105493103BC7, "Lcpw/mods/modlauncher/TransformingClassLoader$DelegatedClassLoader;"));
    if (delegated_loader_field != NULL) {
        delegated_loader = (*env)->GetObjectField(
                env, *loader, delegated_loader_field);
        if (delegated_loader != NULL
                && (*env)->IsInstanceOf(env, delegated_loader,
                        url_loader_class)) {
            add_url = (*env)->GetMethodID(env, url_loader_class,
                    SV_STR(SV_B7D55CA19AE33C56, "addURL"), SV_STR(SV_9E2CE36057724CE5, "(Ljava/net/URL;)V"));
            if (add_url == NULL) {
                sonyvegas_log_pending_exception(env,
                        SV_WSTR(SV_722E28D2B2B90E8F, L"resolve delegated URLClassLoader.addURL"));
                return 0;
            }
            (*env)->CallVoidMethod(env, delegated_loader, add_url, url);
            if ((*env)->ExceptionCheck(env)) {
                sonyvegas_log_pending_exception(env,
                        SV_WSTR(SV_58F77BBD6143609A, L"delegated URLClassLoader.addURL"));
                return 0;
            }
            sonyvegas_log(SV_WSTR(SV_96941B03E08F55DF, L"appended product JAR to ModLauncher delegated ClassLoader"));
            return 1;
        }
    } else if ((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionClear(env);
    }

    modular_result = install_modular_payload_loader(
            env, loader, url_loader_class, url_class, url);
    if (modular_result != 0) {
        return modular_result > 0;
    }

    system_result = install_system_payload_search(env, *loader, jar_path);
    if (system_result != 0) {
        return system_result > 0;
    }

    url_loader_init = (*env)->GetMethodID(env, url_loader_class, SV_STR(SV_CC6C38040FAB5B28, "<init>"),
            SV_STR(SV_E12866157FF95170, "([Ljava/net/URL;Ljava/lang/ClassLoader;)V"));
    urls = (*env)->NewObjectArray(env, 1, url_class, NULL);
    if (url_loader_init == NULL || urls == NULL) {
        sonyvegas_log_pending_exception(env, SV_WSTR(SV_A973853F34D5C596, L"resolve child URLClassLoader constructor"));
        return 0;
    }
    (*env)->SetObjectArrayElement(env, urls, 0, url);
    child_loader = (*env)->NewObject(env, url_loader_class, url_loader_init,
            urls, *loader);
    if (child_loader == NULL || (*env)->ExceptionCheck(env)) {
        sonyvegas_log_pending_exception(env, SV_WSTR(SV_3227C5A127444191, L"create child product URLClassLoader"));
        return 0;
    }
    (*env)->DeleteLocalRef(env, *loader);
    *loader = child_loader;
    sonyvegas_log(SV_WSTR(SV_5F9DD93EA8FEF133, L"created child URLClassLoader for non-URL Minecraft ClassLoader"));
    return 1;
}

static int define_memory_loader_helpers(JNIEnv *env, jobject parent_loader,
        jmethodID find_loaded_class) {
    unsigned int index;
    for (index = 0; index < g_mem_loader_helper_class_count; ++index) {
        const sonyvegas_embedded_loader_class *helper =
                &g_mem_loader_helper_classes[index];
        jstring class_name = (*env)->NewStringUTF(env, helper->binary_name);
        jclass loaded_class;
        if (class_name == NULL) {
            sonyvegas_log_pending_exception(env,
                    SV_WSTR(SV_E1AE373D8978D515,
                            L"DefineClass MemoryJarLoader"));
            return 0;
        }
        loaded_class = (jclass)(*env)->CallObjectMethod(env, parent_loader,
                find_loaded_class, class_name);
        if ((*env)->ExceptionCheck(env)) {
            (*env)->DeleteLocalRef(env, class_name);
            sonyvegas_log_pending_exception(env,
                    SV_WSTR(SV_E1AE373D8978D515,
                            L"DefineClass MemoryJarLoader"));
            return 0;
        }
        if (loaded_class == NULL) {
            loaded_class = (*env)->DefineClass(env, helper->binary_name,
                    parent_loader, (const jbyte *)helper->bytes,
                    (jsize)helper->size);
            if (loaded_class == NULL || (*env)->ExceptionCheck(env)) {
                (*env)->DeleteLocalRef(env, class_name);
                sonyvegas_log_pending_exception(env,
                        SV_WSTR(SV_E1AE373D8978D515,
                                L"DefineClass MemoryJarLoader"));
                return 0;
            }
        }
        (*env)->DeleteLocalRef(env, loaded_class);
        (*env)->DeleteLocalRef(env, class_name);
    }
    return 1;
}

static jobject create_memory_class_loader(
        JNIEnv *env, jobject parent_loader,
        const unsigned char *jar_data, DWORD jar_size) {
    jclass loader_class;
    jclass mem_loader_class;
    jmethodID find_loaded_class;
    jmethodID init;
    jstring mem_loader_name;
    jbyteArray jar_bytes;
    jobject loader;
    loader_class = (*env)->FindClass(env, SV_STR(SV_FD0229A83A546D39, "java/lang/ClassLoader"));
    find_loaded_class = loader_class == NULL ? NULL : (*env)->GetMethodID(
            env, loader_class, SV_STR(SV_B69E0A4F06EAEC59, "findLoadedClass"),
            SV_STR(SV_399D1B125E8DF8EE, "(Ljava/lang/String;)Ljava/lang/Class;"));
    mem_loader_name = (*env)->NewStringUTF(env, SV_STR(SV_B657FF12E88C164D, "MemoryJarLoader"));
    if (find_loaded_class == NULL || mem_loader_name == NULL) {
        sonyvegas_log_pending_exception(env,
                SV_WSTR(SV_0738BC331931CFC6, L"resolve MemoryJarLoader bootstrap methods"));
        return NULL;
    }
    mem_loader_class = (jclass)(*env)->CallObjectMethod(env, parent_loader,
            find_loaded_class, mem_loader_name);
    if ((*env)->ExceptionCheck(env)) {
        sonyvegas_log_pending_exception(env, SV_WSTR(SV_25A3273006DB8477, L"find loaded MemoryJarLoader"));
        return NULL;
    }
    if (mem_loader_class == NULL) {
        mem_loader_class = (*env)->DefineClass(env, SV_STR(SV_B657FF12E88C164D, "MemoryJarLoader"),
                parent_loader, (const jbyte *)g_mem_loader_class,
                (jsize)g_mem_loader_class_size);
    }
    if (mem_loader_class == NULL || (*env)->ExceptionCheck(env)) {
        sonyvegas_log_pending_exception(env, SV_WSTR(SV_E1AE373D8978D515, L"DefineClass MemoryJarLoader"));
        return NULL;
    }
    if (!define_memory_loader_helpers(env, parent_loader,
            find_loaded_class)) {
        return NULL;
    }
    init = (*env)->GetMethodID(env, mem_loader_class, SV_STR(SV_CC6C38040FAB5B28, "<init>"),
            SV_STR(SV_DEA716C74EFDC478, "([BLjava/lang/ClassLoader;)V"));
    if (init == NULL) {
        sonyvegas_log_pending_exception(env,
                SV_WSTR(SV_5C6D3D0C1B10B2D9, L"resolve MemoryJarLoader constructor"));
        return NULL;
    }
    jar_bytes = (*env)->NewByteArray(env, (jsize)jar_size);
    if (jar_bytes == NULL) {
        sonyvegas_log(SV_WSTR(SV_E5225BAECFA5F84E, L"failed to allocate %lu byte array for JAR"), jar_size);
        return NULL;
    }
    (*env)->SetByteArrayRegion(env, jar_bytes, 0, (jsize)jar_size,
            (const jbyte *)jar_data);
    loader = (*env)->NewObject(env, mem_loader_class, init,
            jar_bytes, parent_loader);
    (*env)->DeleteLocalRef(env, jar_bytes);
    if (loader == NULL || (*env)->ExceptionCheck(env)) {
        sonyvegas_log_pending_exception(env,
                SV_WSTR(SV_13AD2C0F6BE7DB79, L"create MemoryJarLoader instance"));
        return NULL;
    }
    sonyvegas_log(SV_WSTR(SV_00AFDC158C5F173C, L"created in-memory ClassLoader for %lu byte JAR"), jar_size);
    return loader;
}

static int jstring_starts_with(JNIEnv *env, jstring value,
        const char *prefix) {
    const char *text;
    size_t prefix_length;
    int matches;
    if (value == NULL || prefix == NULL) return 0;
    text = (*env)->GetStringUTFChars(env, value, NULL);
    if (text == NULL) {
        if ((*env)->ExceptionCheck(env)) (*env)->ExceptionClear(env);
        return 0;
    }
    prefix_length = strlen(prefix);
    matches = strncmp(text, prefix, prefix_length) == 0;
    (*env)->ReleaseStringUTFChars(env, value, text);
    return matches;
}

static int define_memory_jar_classes(JNIEnv *env, jobject memory_loader,
        jobject target_loader, int badlion_runtime) {
    jclass memory_loader_class;
    jclass loader_class;
    jmethodID get_class_names;
    jmethodID get_class_bytes;
    jmethodID find_loaded_class;
    jmethodID load_class;
    jobjectArray class_names;
    unsigned char *completed;
    jsize class_count;
    jsize remaining;
    jsize pass;
    jsize defined_count = 0;
    jsize already_loaded_count = 0;
    jsize skipped_count = 0;
    jsize yield_counter = 0;

    memory_loader_class = (*env)->GetObjectClass(env, memory_loader);
    loader_class = (*env)->FindClass(env, SV_STR(SV_FD0229A83A546D39, "java/lang/ClassLoader"));
    get_class_names = memory_loader_class == NULL ? NULL : (*env)->GetMethodID(
            env, memory_loader_class, SV_STR(SV_D361EE41AC3B6E62, "getClassNames"),
            SV_STR(SV_B68D2B79E6A8BF37, "()[Ljava/lang/String;"));
    get_class_bytes = memory_loader_class == NULL ? NULL : (*env)->GetMethodID(
            env, memory_loader_class, SV_STR(SV_1EA31C2C750AE651, "getClassBytes"),
            SV_STR(SV_29DFD95CCC73010B, "(Ljava/lang/String;)[B"));
    find_loaded_class = loader_class == NULL ? NULL : (*env)->GetMethodID(
            env, loader_class, SV_STR(SV_B69E0A4F06EAEC59, "findLoadedClass"),
            SV_STR(SV_399D1B125E8DF8EE, "(Ljava/lang/String;)Ljava/lang/Class;"));
    load_class = loader_class == NULL ? NULL : (*env)->GetMethodID(
            env, loader_class, SV_STR(SV_042777A846349107, "loadClass"),
            SV_STR(SV_399D1B125E8DF8EE, "(Ljava/lang/String;)Ljava/lang/Class;"));
    if (get_class_names == NULL || get_class_bytes == NULL
            || find_loaded_class == NULL || load_class == NULL) {
        sonyvegas_log_pending_exception(env,
                SV_WSTR(SV_DE680BC4E1D328FF, L"resolve in-memory class definition methods"));
        return 0;
    }
    class_names = (jobjectArray)(*env)->CallObjectMethod(env, memory_loader,
            get_class_names);
    if (class_names == NULL || (*env)->ExceptionCheck(env)) {
        sonyvegas_log_pending_exception(env,
                SV_WSTR(SV_87F8C15C21D8353F, L"enumerate in-memory JAR classes"));
        return 0;
    }
    class_count = (*env)->GetArrayLength(env, class_names);
    if (class_count <= 0) {
        sonyvegas_log(SV_WSTR(SV_5C916C95F8220FBD, L"in-memory JAR contains no loadable classes"));
        return 0;
    }
    completed = (unsigned char *)calloc((size_t)class_count, 1);
    if (completed == NULL) {
        sonyvegas_log(SV_WSTR(SV_E34E2369B8C506BC, L"failed to allocate class definition state for %d classes"),
                (int)class_count);
        return 0;
    }
    remaining = class_count;
    for (pass = 0; pass <= class_count && remaining > 0; ++pass) {
        jsize index;
        jsize progress = 0;
        for (index = 0; index < class_count; ++index) {
            jstring class_name;
            jobject loaded_class;
            jbyteArray class_bytes;
            jbyte *bytes;
            jsize byte_count;
            jclass defined_class;
            if (completed[index]) continue;
            class_name = (jstring)(*env)->GetObjectArrayElement(
                    env, class_names, index);
            if (class_name == NULL || (*env)->ExceptionCheck(env)) {
                (*env)->ExceptionClear(env);
                continue;
            }
            loaded_class = (*env)->CallObjectMethod(env, target_loader,
                    find_loaded_class, class_name);
            if ((*env)->ExceptionCheck(env)) {
                (*env)->ExceptionClear(env);
                loaded_class = NULL;
            }
            if (loaded_class != NULL) {
                completed[index] = 1;
                --remaining;
                ++progress;
                ++already_loaded_count;
                (*env)->DeleteLocalRef(env, loaded_class);
                (*env)->DeleteLocalRef(env, class_name);
                continue;
            }
            if (badlion_runtime
                    && (jstring_starts_with(env, class_name,
                            SV_STR(SV_5BC5D35996F4BEE4, "org.lwjgl."))
                        || jstring_starts_with(env, class_name,
                            SV_STR(SV_2FC14ABCFD6FDBF8, "com.google.gson."))
                        || jstring_starts_with(env, class_name,
                            SV_STR(SV_AF4FBC1655E3DBF8, "com.google.zxing."))
                        || jstring_starts_with(env, class_name,
                            SV_STR(SV_43CEC3639143CCA2, "org.apache.commons.io."))
                        || jstring_starts_with(env, class_name,
                            SV_STR(SV_E3B74E6D7CDC4D19, "net.java.games.input.")))) {
                /* A fat payload JAR contains copies of libraries already
                 * owned by Badlion. Direct DefineClass bypasses delegation
                 * and creates a second Display/Gson universe; hooks then bind
                 * to the copy instead of the classes used by the game. */
                loaded_class = (*env)->CallObjectMethod(env, target_loader,
                        load_class, class_name);
                if ((*env)->ExceptionCheck(env)) {
                    (*env)->ExceptionClear(env);
                    loaded_class = NULL;
                }
                if (loaded_class != NULL) {
                    completed[index] = 1;
                    --remaining;
                    ++progress;
                    ++already_loaded_count;
                    (*env)->DeleteLocalRef(env, loaded_class);
                    (*env)->DeleteLocalRef(env, class_name);
                    continue;
                }
                /* Never create a private LWJGL copy in Badlion. Missing
                 * optional entries can be skipped; required payload classes
                 * are checked explicitly after the definition pass. */
                if (jstring_starts_with(env, class_name,
                        SV_STR(SV_5BC5D35996F4BEE4, "org.lwjgl."))) {
                    completed[index] = 1;
                    --remaining;
                    ++progress;
                    ++skipped_count;
                    (*env)->DeleteLocalRef(env, class_name);
                    continue;
                }
            }
            class_bytes = (jbyteArray)(*env)->CallObjectMethod(
                    env, memory_loader, get_class_bytes, class_name);
            if (class_bytes == NULL || (*env)->ExceptionCheck(env)) {
                (*env)->ExceptionClear(env);
                (*env)->DeleteLocalRef(env, class_name);
                continue;
            }
            byte_count = (*env)->GetArrayLength(env, class_bytes);
            bytes = (*env)->GetByteArrayElements(env, class_bytes, NULL);
            if (bytes == NULL || (*env)->ExceptionCheck(env)) {
                (*env)->ExceptionClear(env);
                (*env)->DeleteLocalRef(env, class_bytes);
                (*env)->DeleteLocalRef(env, class_name);
                continue;
            }
            defined_class = (*env)->DefineClass(env, NULL, target_loader,
                    bytes, byte_count);
            (*env)->ReleaseByteArrayElements(env, class_bytes, bytes, JNI_ABORT);
            if (defined_class != NULL && !(*env)->ExceptionCheck(env)) {
                completed[index] = 1;
                --remaining;
                ++progress;
                ++defined_count;
                (*env)->DeleteLocalRef(env, defined_class);
                // Defining thousands of classes back-to-back serializes the
                // game thread behind the loader lock (frozen window, "not
                // responding"). Yield regularly so the game keeps pumping
                // messages while the payload streams in; total work is
                // unchanged, only interleaved.
                if ((++yield_counter & 63) == 0) {
                    SwitchToThread();
                    if ((yield_counter & 511) == 0) {
                        Sleep(1);
                    }
                }
            } else {
                (*env)->ExceptionClear(env);
                loaded_class = (*env)->CallObjectMethod(env, target_loader,
                        find_loaded_class, class_name);
                if ((*env)->ExceptionCheck(env)) {
                    (*env)->ExceptionClear(env);
                    loaded_class = NULL;
                }
                if (loaded_class != NULL) {
                    completed[index] = 1;
                    --remaining;
                    ++progress;
                    ++already_loaded_count;
                    (*env)->DeleteLocalRef(env, loaded_class);
                }
            }
            (*env)->DeleteLocalRef(env, class_bytes);
            (*env)->DeleteLocalRef(env, class_name);
        }
        if (remaining > 0 && progress == 0) {
            for (index = 0; index < class_count; ++index) {
                jstring class_name;
                const char *name_utf;
                if (completed[index]) continue;
                class_name = (jstring)(*env)->GetObjectArrayElement(
                        env, class_names, index);
                name_utf = (*env)->GetStringUTFChars(env, class_name, NULL);
                if (name_utf != NULL) {
                    sonyvegas_log(SV_WSTR(SV_863DE41CF0D8510A, L"could not define payload class %hs"),
                            name_utf);
                    (*env)->ReleaseStringUTFChars(env, class_name, name_utf);
                }
                (*env)->DeleteLocalRef(env, class_name);
                skipped_count = remaining;
                sonyvegas_log(SV_WSTR(SV_90FC82E38A98AE49, L"skipping %d JAR classes whose optional hierarchy dependencies are unavailable"),
                        (int)skipped_count);
                remaining = 0;
                break;
            }
        }
    }
    free(completed);
    if (remaining != 0) {
        sonyvegas_log(SV_WSTR(SV_6A51E24930D3A03F, L"failed to define %d of %d in-memory JAR classes"),
                (int)remaining, (int)class_count);
        return 0;
    }
    sonyvegas_log(SV_WSTR(SV_10AC73E230F8F346, L"defined %d JAR classes in the game ClassLoader; %d were already loaded there; %d optional classes were skipped"),
            (int)defined_count, (int)already_loaded_count,
            (int)skipped_count);
    return 1;
}

static int verify_payload_class(JNIEnv *env, jobject loader,
        jmethodID load_class, const char *binary_name) {
    jstring name = (*env)->NewStringUTF(env, binary_name);
    jclass loaded;
    if (name == NULL) return 0;
    loaded = (jclass)(*env)->CallObjectMethod(env, loader, load_class, name);
    (*env)->DeleteLocalRef(env, name);
    if (loaded == NULL || (*env)->ExceptionCheck(env)) {
        sonyvegas_log_pending_exception(env,
                SV_WSTR(SV_42F6C7035C55CC8E, L"required in-memory payload class is unavailable"));
        return 0;
    }
    (*env)->DeleteLocalRef(env, loaded);
    return 1;
}

static int verify_required_payload_classes(JNIEnv *env, jobject loader,
        int badlion_runtime) {
    jclass loader_class = (*env)->FindClass(env,
            SV_STR(SV_FD0229A83A546D39, "java/lang/ClassLoader"));
    jmethodID load_class = loader_class == NULL ? NULL : (*env)->GetMethodID(
            env, loader_class, SV_STR(SV_042777A846349107, "loadClass"),
            SV_STR(SV_399D1B125E8DF8EE, "(Ljava/lang/String;)Ljava/lang/Class;"));
    if (load_class == NULL) return 0;
    if (!verify_payload_class(env, loader, load_class,
            SV_STR(SV_696F6FCC1D10199F, "vegas.sony.runtime.NativeBridge"))
            || !verify_payload_class(env, loader, load_class,
            SV_STR(SV_B7E45A0054368AE5, "vegas.sony.SonyVegas"))
            || !verify_payload_class(env, loader, load_class,
            SV_STR(SV_8DA3DC959FF99DE5, "vegas.sony.mapping.PrimaryMappingTaskSet"))) {
        return 0;
    }
    if (badlion_runtime
            && (!verify_payload_class(env, loader, load_class,
            SV_STR(SV_61A05723834F52B9, "vegas.sony.event.impl.EventRender2D"))
            || !verify_payload_class(env, loader, load_class,
            SV_STR(SV_D21F3A4143619683, "vegas.sony.mapping.Badlion189Render2DMappingTask")))) {
        return 0;
    }
    return 1;
}

static jclass load_bridge_class(JNIEnv *env, jobject loader) {
    jclass loader_class = (*env)->FindClass(env, SV_STR(SV_FD0229A83A546D39, "java/lang/ClassLoader"));
    jmethodID load_class = loader_class == NULL ? NULL : (*env)->GetMethodID(
            env, loader_class, SV_STR(SV_042777A846349107, "loadClass"), SV_STR(SV_399D1B125E8DF8EE, "(Ljava/lang/String;)Ljava/lang/Class;"));
    jstring name = (*env)->NewStringUTF(env, SV_STR(SV_696F6FCC1D10199F, "vegas.sony.runtime.NativeBridge"));
    jclass result;
    if (load_class == NULL || name == NULL) {
        return NULL;
    }
    result = (jclass)(*env)->CallObjectMethod(env, loader, load_class, name);
    if (result == NULL || (*env)->ExceptionCheck(env)) {
        sonyvegas_log_pending_exception(env,
                SV_WSTR(SV_297DD63CFE71E341, L"load NativeBridge class"));
        return NULL;
    }
    return result;
}

static int set_current_context_class_loader(JNIEnv *env, jobject loader) {
    jclass thread_class = (*env)->FindClass(env, SV_STR(SV_C777B094C81CAB52, "java/lang/Thread"));
    jmethodID current_thread;
    jmethodID set_context_loader;
    jobject thread;
    if (thread_class == NULL) {
        sonyvegas_log_pending_exception(env, SV_WSTR(SV_221E5ACDA25BEEC8, L"resolve java.lang.Thread"));
        return 0;
    }
    current_thread = (*env)->GetStaticMethodID(env, thread_class,
            SV_STR(SV_97612740801F68C2, "currentThread"), SV_STR(SV_ADE8F25FF5411F05, "()Ljava/lang/Thread;"));
    set_context_loader = (*env)->GetMethodID(env, thread_class,
            SV_STR(SV_D2F99AF27C8A55C9, "setContextClassLoader"), SV_STR(SV_6F3C268588A3AA3B, "(Ljava/lang/ClassLoader;)V"));
    if (current_thread == NULL || set_context_loader == NULL) {
        sonyvegas_log_pending_exception(env, SV_WSTR(SV_5268E2F0A9BF519C, L"resolve Thread context ClassLoader methods"));
        return 0;
    }
    thread = (*env)->CallStaticObjectMethod(env, thread_class, current_thread);
    if (thread == NULL || (*env)->ExceptionCheck(env)) {
        sonyvegas_log_pending_exception(env, SV_WSTR(SV_6E7B748B784EBFE2, L"Thread.currentThread"));
        return 0;
    }
    (*env)->CallVoidMethod(env, thread, set_context_loader, loader);
    if ((*env)->ExceptionCheck(env)) {
        sonyvegas_log_pending_exception(env, SV_WSTR(SV_9D632E3C15FCC76C, L"Thread.setContextClassLoader"));
        return 0;
    }
    return 1;
}

static int pin_native_module(void) {
    HMODULE pinned = NULL;
    if (g_manual_mapped) return 1;
    if (!GetModuleHandleExW(GET_MODULE_HANDLE_EX_FLAG_FROM_ADDRESS
                    | GET_MODULE_HANDLE_EX_FLAG_PIN,
            (LPCWSTR)(const void *)&g_module, &pinned)) {
        sonyvegas_log(SV_WSTR(SV_2B4D856C768A85C4, L"GetModuleHandleExW(PIN) failed: %lu"), GetLastError());
        return 0;
    }
    return 1;
}

static int call_bridge_start(JNIEnv *env, jclass bridge_class) {
    jmethodID start = (*env)->GetStaticMethodID(env, bridge_class, SV_STR(SV_6E1F4F385A595F74, "start"), SV_STR(SV_C52E12541A990AAC, "()V"));
    if (start == NULL) {
        sonyvegas_log_pending_exception(env, SV_WSTR(SV_F44E4F6DF74E1463, L"resolve NativeBridge.start"));
        return 0;
    }
    (*env)->CallStaticVoidMethod(env, bridge_class, start);
    if ((*env)->ExceptionCheck(env)) {
        sonyvegas_log_pending_exception(env, SV_WSTR(SV_DBEA61294E310CD1, L"NativeBridge.start"));
        return 0;
    }
    return sonyvegas_bridge_start_completed(env, bridge_class);
}

static DWORD WINAPI bootstrap_thread(LPVOID parameter) {
    HMODULE jvm_module;
    FARPROC created_vms_address;
    typedef jint (JNICALL *get_created_vms_fn)(JavaVM **, jsize, jsize *);
    get_created_vms_fn get_created_vms;
    JavaVM *vm = NULL;
    JNIEnv *env = NULL;
    jsize vm_count = 0;
    jobject loader = NULL;
    jobject mem_loader = NULL;
    jclass bridge_class = NULL;
    const unsigned char *jar_data = NULL;
    DWORD jar_size = 0;
    int badlion_runtime = 0;
    int attached = 0;
    int registered = 0;
    int completed = 0;
    int attempt;
    HMODULE worker_module = (HMODULE)parameter;
    DWORD exit_code = 1;
    if (!sonyvegas_loader_bootstrap_initialize()) {
        sonyvegas_log(SV_WSTR(SV_81969C9D04B3FEDE, L"Loader token bootstrap is invalid"));
        exit_code = 6;
        goto cleanup;
    }
    Sleep(150);
    if (InterlockedCompareExchange(&g_loaded_by_jni, 0, 0) != 0) {
        return 0;
    }
    for (attempt = 0; attempt < 600; ++attempt) {
        jvm_module = GetModuleHandleW(
                SV_WSTR(SV_687A375AB2595325, L"jvm.dll"));
        if (jvm_module != NULL) break;
        Sleep(100);
    }
    if (jvm_module == NULL) {
        sonyvegas_log(SV_WSTR(SV_55A9C44FEF20B0DA, L"jvm.dll is not loaded"));
        exit_code = 2;
        goto cleanup;
    }
    created_vms_address = GetProcAddress(jvm_module,
            SV_STR(SV_B002C8CEF5CCA07D, "JNI_GetCreatedJavaVMs"));
    if (created_vms_address == NULL) {
        sonyvegas_log(SV_WSTR(SV_1B39B01B52EE45D8, L"JNI_GetCreatedJavaVMs export is unavailable"));
        exit_code = 3;
        goto cleanup;
    }
    get_created_vms = (get_created_vms_fn)created_vms_address;
    for (attempt = 0; attempt < 600; ++attempt) {
        if (get_created_vms(&vm, 1, &vm_count) == JNI_OK
                && vm != NULL && vm_count >= 1) {
            break;
        }
        vm = NULL;
        vm_count = 0;
        Sleep(100);
    }
    if (vm == NULL || vm_count < 1) {
        sonyvegas_log(SV_WSTR(SV_B98DF1B3673AB5A0, L"JNI_GetCreatedJavaVMs returned no VM"));
        exit_code = 4;
        goto cleanup;
    }
    if ((*vm)->AttachCurrentThreadAsDaemon(vm, (void **)&env, NULL) != JNI_OK
            || env == NULL) {
        sonyvegas_log(SV_WSTR(SV_557E78C717953E09, L"AttachCurrentThreadAsDaemon failed"));
        exit_code = 5;
        goto cleanup;
    }
    attached = 1;
    sonyvegas_log(SV_WSTR(SV_96547657384ECEB9,
            L"bootstrap build: badlion-frame-defer-2026-09-11"));
    if (sonyvegas_initialize_jvmti(vm) != JNI_OK) {
        goto cleanup;
    }
    badlion_runtime = sonyvegas_is_badlion_189_runtime();
    sonyvegas_log(SV_WSTR(SV_1D2F89B211F86ADB, L"runtime profile: Badlion 1.8.9=%d"), badlion_runtime);
    jar_data = get_jar_resource_bytes(&jar_size);
    if (jar_data == NULL || jar_size < 4
            || jar_data[0] != 'P' || jar_data[1] != 'K') {
        if (jar_data != NULL)
            sonyvegas_log(SV_WSTR(SV_6BF670D7B38D89BD, L"embedded product JAR resource is invalid"));
        exit_code = 7;
        goto cleanup;
    }
    sonyvegas_log(SV_WSTR(SV_20B958FAC928CDC4, L"found embedded JAR resource: %lu bytes"), jar_size);
    for (attempt = 0; attempt < 600 && loader == NULL; ++attempt) {
        loader = find_client_class_loader(env);
        if (loader == NULL) {
            Sleep(100);
        }
    }
    if (loader == NULL) {
        sonyvegas_log(SV_WSTR(SV_C0339187FBF2D04A, L"Minecraft client/render thread was not found within 60 seconds"));
        goto cleanup;
    }
    mem_loader = create_memory_class_loader(env, loader, jar_data, jar_size);
    if (mem_loader == NULL) {
        exit_code = 81;
        goto cleanup;
    }
    if (!define_memory_jar_classes(env, mem_loader, loader,
            badlion_runtime)) {
        exit_code = 82;
        goto cleanup;
    }
    if (!verify_required_payload_classes(env, loader, badlion_runtime)) {
        exit_code = 83;
        goto cleanup;
    }
    if (!set_current_context_class_loader(env, mem_loader)) {
        exit_code = 84;
        goto cleanup;
    }
    bridge_class = load_bridge_class(env, loader);
    if (bridge_class == NULL) {
        exit_code = 9;
        goto cleanup;
    }
    if (sonyvegas_register_native_bridge(env, bridge_class) != JNI_OK) {
        exit_code = 10;
        goto cleanup;
    }
    registered = 1;
    {
        jmethodID set_resource_loader = (*env)->GetStaticMethodID(env,
                bridge_class, SV_STR(SV_82C50AFD653F6C58, "srl"), SV_STR(SV_6F3C268588A3AA3B, "(Ljava/lang/ClassLoader;)V"));
        if (set_resource_loader == NULL || (*env)->ExceptionCheck(env)) {
            sonyvegas_log_pending_exception(env,
                    SV_WSTR(SV_B2ABD273F2F660BE, L"resolve NativeBridge resource-loader handoff"));
            exit_code = 11;
            goto cleanup;
        }
        (*env)->CallStaticVoidMethod(env, bridge_class,
                set_resource_loader, mem_loader);
        if ((*env)->ExceptionCheck(env)) {
            sonyvegas_log_pending_exception(env,
                    SV_WSTR(SV_CA1F182BBA88DEB3, L"install MemoryJarLoader resource handoff"));
            exit_code = 11;
            goto cleanup;
        }
        sonyvegas_log(SV_WSTR(SV_3F8A74049069F3F4, L"installed MemoryJarLoader for payload resources"));
    }
    if (!pin_native_module()) {
        exit_code = 12;
        goto cleanup;
    }
    sonyvegas_log(SV_WSTR(SV_2D2647EACF6CC22C, L"NativeBridge loaded in the game ClassLoader from in-memory JAR (%lu bytes); MemoryJarLoader serves resources"),
            jar_size);
    if (badlion_runtime) {
        /* The reference starts on this attached worker. Badlion's own final
         * frame callback moves GUI initialization/rendering onto the client
         * path without replacing WindowsDisplay.nUpdate. */
        if (!call_bridge_start(env, bridge_class)) {
            exit_code = 13;
            goto cleanup;
        }
    } else {
        int scheduled = sonyvegas_schedule_bridge_start_on_lwjgl2(env);
        if (scheduled < 0) {
            exit_code = 13;
            goto cleanup;
        }
        if (scheduled > 0) {
            int start_status = 0;
            for (attempt = 0; attempt < 1200; ++attempt) {
                start_status = sonyvegas_bridge_start_status();
                if (start_status == 3 || start_status == 4) {
                    break;
                }
                Sleep(100);
            }
            if (start_status != 3) {
                sonyvegas_log(SV_WSTR(SV_D58A836696E3E6AE, L"render-thread NativeBridge.start failed or timed out: status=%d"),
                        start_status);
                exit_code = 13;
                goto cleanup;
            }
        } else if (!call_bridge_start(env, bridge_class)) {
            exit_code = 13;
            goto cleanup;
        }
    }
    sonyvegas_loader_report_completed();
    sonyvegas_log(SV_WSTR(SV_0F8E9DB602C7AE00, L"NativeBridge.start completed; injection is active"));
    completed = 1;
    exit_code = 0;

cleanup:
    if (!completed) {
        char failure[96];
        _snprintf_s(failure, sizeof(failure), _TRUNCATE,
                SV_STR(SV_F44931702BCEBA82, "Native bootstrap failed with code %lu"), exit_code);
        sonyvegas_loader_report_failure(failure);
    }
    if (attached) {
        (*vm)->DetachCurrentThread(vm);
    }
    if (!registered && worker_module != NULL) {
        sonyvegas_log(SV_WSTR(SV_C949FC5E47AE7AE5, L"bootstrap failed before native registration; unloading DLL"));
        if (!g_manual_mapped) {
            FreeLibraryAndExitThread(worker_module, exit_code);
        }
        ExitThread(exit_code);
    }
    if (!completed) {
        sonyvegas_log(SV_WSTR(SV_822A6EF2B9450CB1, L"bootstrap did not complete; DLL remains loaded because natives may be registered"));
    }
    return exit_code;
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env = NULL;
    jclass bridge_class;
    (void)reserved;
    InterlockedExchange(&g_loaded_by_jni, 1);
    if (!sonyvegas_loader_bootstrap_initialize()) {
        return JNI_ERR;
    }
    if ((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_8) != JNI_OK
            || env == NULL) {
        return JNI_ERR;
    }
    if (sonyvegas_initialize_jvmti(vm) != JNI_OK) {
        return JNI_ERR;
    }
    bridge_class = (*env)->FindClass(env, SV_STR(SV_2C90BA66DAEAE5B1, "vegas/sony/runtime/NativeBridge"));
    if (bridge_class == NULL
            || sonyvegas_register_native_bridge(env, bridge_class) != JNI_OK) {
        sonyvegas_log_pending_exception(env, SV_WSTR(SV_95C267A718A56EB7, L"JNI_OnLoad NativeBridge registration"));
        return JNI_ERR;
    }
    sonyvegas_log(SV_WSTR(SV_0B35006F7ED320E7, L"JNI_OnLoad completed"));
    return JNI_VERSION_1_8;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved) {
    JNIEnv *env = NULL;
    (void)reserved;
    if ((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_8) == JNI_OK) {
        sonyvegas_release_native_bridge(env);
    }
    sonyvegas_loader_bootstrap_clear();
}

BOOL WINAPI DllMain(HINSTANCE instance, DWORD reason, LPVOID reserved) {
    HANDLE thread;
    HMODULE test_handle = NULL;
    (void)reserved;
    if (reason == DLL_PROCESS_ATTACH) {
        g_module = instance;
        if (!GetModuleHandleExW(
                GET_MODULE_HANDLE_EX_FLAG_FROM_ADDRESS
                        | GET_MODULE_HANDLE_EX_FLAG_UNCHANGED_REFCOUNT,
                (LPCWSTR)instance, &test_handle)) {
            g_manual_mapped = 1;
        } else {
        }
        if (!g_manual_mapped) {
            DisableThreadLibraryCalls(instance);
        }
        thread = CreateThread(NULL, 0, bootstrap_thread, instance, 0, NULL);
        if (thread != NULL) {
            CloseHandle(thread);
        }
    } else if (reason == DLL_PROCESS_DETACH) {
        sonyvegas_loader_bootstrap_clear();
    }
    return TRUE;
}

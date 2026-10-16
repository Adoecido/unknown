#ifndef SONYVEGAS421_NATIVE_H
#define SONYVEGAS421_NATIVE_H

#ifndef WIN32_LEAN_AND_MEAN
#define WIN32_LEAN_AND_MEAN
#endif
#include <windows.h>
#include <jni.h>
#include <jvmti.h>

#ifdef __cplusplus
extern "C" {
#endif

extern JavaVM *g_vm;
extern jvmtiEnv *g_jvmti;
extern HMODULE g_module;

void sonyvegas_log(const wchar_t *format, ...);
void sonyvegas_log_pending_exception(JNIEnv *env, const wchar_t *context);
jint sonyvegas_initialize_jvmti(JavaVM *vm);
int sonyvegas_is_badlion_189_runtime(void);
jint sonyvegas_register_native_bridge(JNIEnv *env, jclass bridge_class);
int sonyvegas_schedule_bridge_start_on_lwjgl2(JNIEnv *env);
int sonyvegas_bridge_start_status(void);
int sonyvegas_bridge_start_completed(JNIEnv *env, jclass bridge_class);
void sonyvegas_release_native_bridge(JNIEnv *env);

#ifdef __cplusplus
}
#endif

#endif

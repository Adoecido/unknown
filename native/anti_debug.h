#ifndef SONYVEGAS421_ANTI_DEBUG_H
#define SONYVEGAS421_ANTI_DEBUG_H

#ifdef __cplusplus
extern "C" {
#endif

/* Run all one-shot startup protections (DACL hardening, hide thread,
   initial debugger checks).  Returns 0 if a debugger was detected. */
int anti_debug_init(void);

/* Start the background sentinel thread that periodically scans for
   RE tools, hardware breakpoints, timing anomalies, NTDLL hooks,
   and external process handles.  Calls TerminateProcess on detection. */
void anti_debug_start_sentinel(void);

/* Stop the sentinel thread (called during normal shutdown). */
void anti_debug_stop(void);

#ifdef __cplusplus
}
#endif

#endif

#ifndef SONYVEGAS421_MODULE_MENU_H
#define SONYVEGAS421_MODULE_MENU_H

#include <stddef.h>
#include <windows.h>

#ifdef __cplusplus
extern "C" {
#endif

/* Called by NativeBridge.ss after the Java-side loopback control service is
 * ready. The binary payload carries the port, the XOR-masked session root,
 * the CSRF token, per-session route aliases and the GUI bind; no URL or
 * host text exists on either side. */
void sonyvegas_module_menu_accept_bootstrap(const unsigned char *data,
        size_t length);

/* Stops the independent overlay and its loopback transport worker. */
void sonyvegas_module_menu_shutdown(void);

/* NativeBridge installs this after the Java bridge is registered. The return
 * value is true when opening the menu moved Minecraft out of exclusive
 * fullscreen and the overlay should provide a temporary borderless surface. */
typedef BOOL (*sonyvegas_module_menu_visibility_callback)(BOOL visible);
void sonyvegas_module_menu_set_visibility_callback(
        sonyvegas_module_menu_visibility_callback callback);
BOOL sonyvegas_module_menu_is_visible(void);

/* Embedded renderer used from Minecraft's own OpenGL render thread. */
BOOL sonyvegas_module_menu_set_visible(BOOL visible);
BOOL sonyvegas_module_menu_render_opengl(HWND window);
/* Frame-local normalized HUD positions: three x/y pairs, bits 0-2 indicate
 * drafts and bit 3 indicates the position editor. No HTTP request is needed. */
unsigned sonyvegas_module_menu_hud_positions(float positions[6]);
BOOL sonyvegas_module_menu_handle_message(
        HWND window, UINT message, WPARAM wparam, LPARAM lparam);

#ifdef __cplusplus
}
#endif

#endif

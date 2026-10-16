package vegas.sony.input;

import java.lang.reflect.Method;

/**
 * Authoritative focus state of the game window.
 *
 * <p>{@link InputFocusState} is fed by window messages, and on LWJGL 2 those
 * never arrive: Windows <em>sends</em> WM_SETFOCUS/WM_KILLFOCUS straight to the
 * window procedure instead of posting them to the queue the client drains, so
 * the tracked flag stays "focused" forever and modules keep clicking and aiming
 * while the player is in another window - or in a second game instance.
 * {@code Display.isActive()} answers that reliably on LWJGL 2; on LWJGL 3 the
 * class does not exist and the message driven flag is already correct.
 */
public final class GameWindowFocus {
    /** One poll per couple of milliseconds is plenty for a 20 tick loop. */
    private static final long CACHE_DURATION_NANOS = 2_000_000L;

    private static volatile Method displayIsActive;
    private static volatile boolean lookupCompleted;
    private static volatile long lastQueryNanos;
    private static volatile boolean lastResult = true;

    private GameWindowFocus() {
    }

    /**
     * True while the operating system reports the game window as active, and
     * also true when the runtime cannot be asked - the caller then falls back
     * to the tracked window-message state.
     */
    public static boolean isWindowActive() {
        Method method = resolveDisplayIsActive();
        if (method == null) {
            return true;
        }
        long now = System.nanoTime();
        if (now - lastQueryNanos < CACHE_DURATION_NANOS) {
            return lastResult;
        }
        boolean active = true;
        try {
            active = Boolean.TRUE.equals(method.invoke(null));
        }
        catch (Throwable unavailable) {
            // The display can be gone during shutdown, and reflection can fail
            // on a runtime that only looks like LWJGL 2. Stop asking.
            displayIsActive = null;
        }
        lastResult = active;
        lastQueryNanos = now;
        return active;
    }

    private static Method resolveDisplayIsActive() {
        if (lookupCompleted) {
            return displayIsActive;
        }
        Method resolved = null;
        try {
            Class<?> display = Class.forName("org.lwjgl.opengl.Display");
            resolved = display.getMethod("isActive");
            if (!java.lang.reflect.Modifier.isStatic(resolved.getModifiers())) {
                resolved = null;
            }
        }
        catch (Throwable lwjgl3Runtime) {
            resolved = null;
        }
        displayIsActive = resolved;
        lookupCompleted = true;
        return resolved;
    }
}

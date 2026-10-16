package vegas.sony.runtime;

import java.util.Locale;

/**
 * Identifies the host client the payload was injected into.
 *
 * <p>This runs before {@link vegas.sony.wrapper.impl.ForgeVersion#c()} has resolved a
 * protocol version, so nothing here may depend on the version being known.
 */
public final class HostClientDetection {
    /** Forces a host client, e.g. {@code -Dsonyvegas.host=cheatbreaker}. */
    public static final String HOST_OVERRIDE_PROPERTY = "sv.h";

    /**
     * CheatBreaker ships its own launch pipeline instead of Forge; any one of these
     * resolving means we are inside it.
     */
    private static final String[] CHEATBREAKER_MARKER_CLASSES = {
            "com.cheatbreaker.client.CheatBreaker",
            "com.cheatbreaker.CheatBreaker",
            "com.cheatbreaker.client.CBClient",
            "com.cheatbreaker.launcher.CheatBreakerLauncher",
            "com.cheatbreaker.client.launcher.CheatBreakerTweaker"
    };

    /** Checked against the classpath and launch command when no marker class resolves. */
    private static final String CHEATBREAKER_LAUNCH_MARKER = "cheatbreaker";

    private static Boolean cheatBreakerPresent;

    private HostClientDetection() {
    }

    public static boolean isCheatBreakerPresent() {
        Boolean cached = cheatBreakerPresent;
        if (cached != null) {
            return cached;
        }
        boolean present = HostClientDetection.detectCheatBreaker();
        cheatBreakerPresent = present;
        return present;
    }

    /** Drops the cached result; used when the payload is reloaded into a fresh instance. */
    public static void reset() {
        cheatBreakerPresent = null;
    }

    private static boolean detectCheatBreaker() {
        String override = HostClientDetection.readOverride();
        if (override != null) {
            return override.contains(CHEATBREAKER_LAUNCH_MARKER);
        }
        for (String markerClass : CHEATBREAKER_MARKER_CLASSES) {
            if (HostClientDetection.isClassPresent(markerClass)) {
                return true;
            }
        }
        // Older builds rename their entry classes between releases, so fall back to the
        // launch environment, which still names the jar and the tweaker.
        return HostClientDetection.launchEnvironmentContains(CHEATBREAKER_LAUNCH_MARKER);
    }

    private static String readOverride() {
        String override;
        try {
            override = System.getProperty(HOST_OVERRIDE_PROPERTY);
        }
        catch (Throwable ignored) {
            return null;
        }
        if (override == null || override.trim().isEmpty()) {
            return null;
        }
        return override.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean launchEnvironmentContains(String marker) {
        return HostClientDetection.propertyContains("java.class.path", marker)
                || HostClientDetection.propertyContains("sun.java.command", marker)
                || HostClientDetection.propertyContains("java.library.path", marker);
    }

    private static boolean propertyContains(String key, String marker) {
        try {
            String value = System.getProperty(key);
            return value != null && value.toLowerCase(Locale.ROOT).contains(marker);
        }
        catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isClassPresent(String className) {
        try {
            Class.forName(className, false, HostClientDetection.class.getClassLoader());
            return true;
        }
        catch (Throwable ignored) {
            return false;
        }
    }
}

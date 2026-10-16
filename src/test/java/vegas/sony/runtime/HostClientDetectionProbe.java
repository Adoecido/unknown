package vegas.sony.runtime;

/**
 * Exercises host-client detection without a Minecraft process: the override
 * property, the launch-environment fallback, and the negative case.
 */
public final class HostClientDetectionProbe {
    private HostClientDetectionProbe() {
    }

    public static void main(String[] arguments) {
        String savedCommand = System.getProperty("sun.java.command");
        try {
            clearLaunchMarkers();

            HostClientDetection.reset();
            require(!HostClientDetection.isCheatBreakerPresent(),
                    "CheatBreaker reported on a plain JVM");

            System.setProperty(HostClientDetection.HOST_OVERRIDE_PROPERTY, "cheatbreaker");
            HostClientDetection.reset();
            require(HostClientDetection.isCheatBreakerPresent(),
                    "override property did not select CheatBreaker");

            System.setProperty(HostClientDetection.HOST_OVERRIDE_PROPERTY, "vanilla");
            HostClientDetection.reset();
            require(!HostClientDetection.isCheatBreakerPresent(),
                    "override property did not veto CheatBreaker");

            System.clearProperty(HostClientDetection.HOST_OVERRIDE_PROPERTY);
            System.setProperty("sun.java.command",
                    "com.cheatbreaker.client.launcher.Main --version 1.8.9");
            HostClientDetection.reset();
            require(HostClientDetection.isCheatBreakerPresent(),
                    "launch command did not identify CheatBreaker");

            HostClientDetection.reset();
            require(HostClientDetection.isCheatBreakerPresent(),
                    "cached result changed between calls");

            System.out.println("host client detection probe: OK");
        }
        finally {
            System.clearProperty(HostClientDetection.HOST_OVERRIDE_PROPERTY);
            if (savedCommand == null) {
                System.clearProperty("sun.java.command");
            } else {
                System.setProperty("sun.java.command", savedCommand);
            }
            HostClientDetection.reset();
        }
    }

    private static void clearLaunchMarkers() {
        System.clearProperty(HostClientDetection.HOST_OVERRIDE_PROPERTY);
        System.setProperty("sun.java.command", "probe");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}

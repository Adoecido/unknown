package vegas.sony.remote;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

public final class LocalControlSessionTokenProbe {
    private LocalControlSessionTokenProbe() {
    }

    public static void main(String[] arguments) throws Exception {
        Method randomToken = LocalControlServer.class.getDeclaredMethod(
                "randomToken", Integer.TYPE);
        randomToken.setAccessible(true);
        String first = (String)randomToken.invoke(null, Integer.valueOf(48));
        String second = (String)randomToken.invoke(null, Integer.valueOf(48));
        assertToken(first);
        assertToken(second);
        if (first.equals(second)) {
            throw new AssertionError("consecutive session tokens are equal");
        }

        LocalControlServer server = LocalControlServer.INSTANCE;
        Method installRoot = LocalControlServer.class.getDeclaredMethod(
                "installSessionRoot", String.class);
        installRoot.setAccessible(true);
        installRoot.invoke(server, "/c/" + first);
        Method initializeRoutes = LocalControlServer.class.getDeclaredMethod(
                "initializeRouteAliases");
        initializeRoutes.setAccessible(true);
        initializeRoutes.invoke(server);
        Method routeToken = LocalControlServer.class.getDeclaredMethod(
                "routeToken", String.class);
        routeToken.setAccessible(true);
        String healthToken = (String)routeToken.invoke(server, "/health");
        Method routePath = LocalControlServer.class.getDeclaredMethod(
                "routePath", byte[].class);
        routePath.setAccessible(true);
        assertEquals("/", routePath.invoke(server,
                (Object)bytes("/c/" + first + "/")));
        assertEquals("/health",
                routePath.invoke(server, (Object)bytes("/c/" + first + "/" + healthToken)));
        assertEquals(null,
                routePath.invoke(server, (Object)bytes("/c/" + first + "/health")));
        assertEquals(null,
                routePath.invoke(server, (Object)bytes("/")));
        assertEquals(null,
                routePath.invoke(server, (Object)bytes("/c/" + second + "/" + healthToken)));
        System.out.println("Local control session probe passed: tokenLength="
                + first.length() + ", routeAliases=true, oldRootRejected=true");
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.US_ASCII);
    }

    private static void assertToken(String token) {
        if (token.length() != 64
                || !token.matches("[A-Za-z0-9_-]{64}")) {
            throw new AssertionError("invalid URL-safe token: " + token);
        }
    }

    private static void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("expected=" + expected
                    + " actual=" + actual);
        }
    }
}

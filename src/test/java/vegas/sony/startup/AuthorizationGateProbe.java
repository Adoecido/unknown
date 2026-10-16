package vegas.sony.startup;

import com.google.gson.JsonObject;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import vegas.sony.SonyVegas;
import vegas.sony.account.AccountInfoResponse;
import vegas.sony.api.ApiAccessTokenProvider;
import vegas.sony.api.ApiResponse;
import vegas.sony.api.ApiServices;
import vegas.sony.api.LicenseVerifier;
import vegas.sony.runtime.NativeBridge;

/**
 * Regression coverage for the authorization gate added in Etapa 1:
 * - missing/broken token or provider must fail closed, without activating;
 * - a valid simulated response authorizes;
 * - repeated calls never hit the backend twice.
 *
 * Runs entirely offline: the ApiServices account-info call and the native
 * token provider are replaced by test seams.
 */
public final class AuthorizationGateProbe {

    public static void main(String[] args) throws Exception {
        missingTokenFailsClosed();
        tokenProviderErrorFailsClosed();
        failingApiFailsClosedOnce();
        validApiAuthorizesOnce();
        System.out.println("PASS: authorization gate fails closed, authorizes valid "
                + "responses and latches without duplicate initialization");
    }

    private static void missingTokenFailsClosed() throws Exception {
        SonyVegas client = newClientInstance();
        ApiServices.setAccountInfoOverrideForTesting(() -> {
            throw new AssertionError("backend must not be queried without a token");
        });
        ApiAccessTokenProvider.setAccessTokenForTesting("");
        if (client.initAccountInfo()) {
            throw new AssertionError("empty token must not authorize");
        }
        if (client.isAuthorized()) {
            throw new AssertionError("isAuthorized must stay false after token failure");
        }
        ApiAccessTokenProvider.setAccessTokenForTesting(null);
        ApiServices.setAccountInfoOverrideForTesting(null);
    }

    private static void tokenProviderErrorFailsClosed() throws Exception {
        SonyVegas client = newClientInstance();
        ApiServices.setAccountInfoOverrideForTesting(() -> {
            throw new AssertionError("backend must not be queried when the provider throws");
        });
        // NativeBridge.gat() is a native method: in this probe JVM it raises
        // UnsatisfiedLinkError, which initAccountInfo must treat as failure.
        if (client.initAccountInfo()) {
            throw new AssertionError("provider error must not authorize");
        }
        if (client.isAuthorized()) {
            throw new AssertionError("isAuthorized must stay false after provider error");
        }
        ApiServices.setAccountInfoOverrideForTesting(null);
    }

    private static void failingApiFailsClosedOnce() throws Exception {
        SonyVegas client = newClientInstance();
        AtomicInteger calls = new AtomicInteger();
        ApiServices.setAccountInfoOverrideForTesting(() -> {
            calls.incrementAndGet();
            return CompletableFuture.completedFuture(ApiResponse.failure("simulated_unavailable"));
        });
        ApiAccessTokenProvider.setAccessTokenForTesting("simulated-token");
        if (client.initAccountInfo()) {
            throw new AssertionError("failed API response must not authorize");
        }
        if (client.initAccountInfo()) {
            throw new AssertionError("latched failure must not authorize later");
        }
        if (calls.get() != 1) {
            throw new AssertionError("backend queried " + calls.get() + " times, expected exactly 1");
        }
        if (client.isAuthorized()) {
            throw new AssertionError("isAuthorized must stay false after API failure");
        }
        ApiAccessTokenProvider.setAccessTokenForTesting(null);
        ApiServices.setAccountInfoOverrideForTesting(null);
    }

    private static void validApiAuthorizesOnce() throws Exception {
        SonyVegas client = newClientInstance();
        AtomicInteger calls = new AtomicInteger();
        java.security.KeyPair finalKeyPair = java.security.KeyPairGenerator.getInstance("RSA").generateKeyPair();
        LicenseVerifier.setPublicKeyForTesting(
                ((java.security.interfaces.RSAPublicKey) finalKeyPair.getPublic()).getModulus());
        final JsonObject licenseDoc = signedLicense(finalKeyPair, 7L);
        ApiServices.setAccountInfoOverrideForTesting(() -> {
            calls.incrementAndGet();
            JsonObject data = new JsonObject();
            long userId = 7L;
            data.addProperty("userId", userId);
            data.addProperty("username", "probe");
            data.addProperty("accountCreation", "2024-03-17T00:00:00.000Z");
            data.addProperty("licensed", true);
            data.addProperty("registered", true);
            data.addProperty("profiles", true);
            data.addProperty("banned", false);
            data.add("license", licenseDoc);
            return CompletableFuture.completedFuture(
                    ApiResponse.success(AccountInfoResponse.fromJson(data)));
        });
        ApiAccessTokenProvider.setAccessTokenForTesting("simulated-token");
        if (!client.initAccountInfo()) {
            throw new AssertionError("valid simulated response must authorize");
        }
        if (calls.get() != 1) {
            throw new AssertionError("backend queried " + calls.get() + " times, expected exactly 1");
        }
        // second call returns the latched answer without touching the backend
        ApiServices.setAccountInfoOverrideForTesting(() -> {
            throw new AssertionError("latched success must not re-query the backend");
        });
        if (!client.initAccountInfo()) {
            throw new AssertionError("latched success must keep authorizing");
        }
        if (!client.isAuthorized()) {
            throw new AssertionError("isAuthorized must be true after a valid response");
        }
        // start() itself requires the native bridge; the probe only asserts the
        // completion flag stays unset here, i.e. no partial activation.
        if (NativeBridge.isStartCompleted()) {
            throw new AssertionError("isStartCompleted must be false before native start");
        }
        ApiAccessTokenProvider.setAccessTokenForTesting(null);
        ApiServices.setAccountInfoOverrideForTesting(null);
        LicenseVerifier.clearPublicKeyOverride();
    }

    private static JsonObject signedLicense(java.security.KeyPair pair, long userId) throws Exception {
        JsonObject payload = new JsonObject();
        payload.addProperty("v", 1);
        payload.addProperty("product", "sonyvegas");
        payload.addProperty("userId", userId);
        payload.addProperty("role", "user");
        long now = System.currentTimeMillis() / 1000L;
        payload.addProperty("iat", now - 10);
        payload.addProperty("exp", now + 3600);
        payload.addProperty("requireSignature", true);
        java.security.Signature signer = java.security.Signature.getInstance("SHA256withRSA");
        signer.initSign(pair.getPrivate());
        signer.update(canonical(payload).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        JsonObject doc = new JsonObject();
        doc.add("payload", payload);
        doc.addProperty("signature", Base64.getEncoder().encodeToString(signer.sign()));
        return doc;
    }

    private static String canonical(JsonObject payload) {
        StringBuilder out = new StringBuilder("{");
        java.util.TreeMap<String, com.google.gson.JsonElement> sorted =
                new java.util.TreeMap<String, com.google.gson.JsonElement>();
        for (java.util.Map.Entry<String, com.google.gson.JsonElement> entry : payload.entrySet()) {
            sorted.put(entry.getKey(), entry.getValue());
        }
        boolean first = true;
        for (java.util.Map.Entry<String, com.google.gson.JsonElement> entry : sorted.entrySet()) {
            if (!first) out.append(',');
            first = false;
            com.google.gson.JsonPrimitive primitive = entry.getValue().getAsJsonPrimitive();
            out.append('"').append(entry.getKey()).append('"').append(':')
                    .append(primitive.isNumber() ? primitive.getAsString()
                            : primitive.isBoolean() ? String.valueOf(primitive.getAsBoolean())
                            : '"' + primitive.getAsString() + '"');
        }
        return out.append('}').toString();
    }

    private static SonyVegas newClientInstance() {
        // The real constructor reaches native code (NativeBridge.trs), which
        // is unavailable in a plain probe JVM. Allocate without running it
        // and initialize just the field the gate synchronizes on.
        try {
            java.lang.reflect.Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            SonyVegas instance = (SonyVegas) unsafe.allocateInstance(SonyVegas.class);
            java.lang.reflect.Field lockField = SonyVegas.class.getDeclaredField("accountInfoLock");
            lockField.setAccessible(true);
            lockField.set(instance, new Object());
            return instance;
        } catch (Throwable error) {
            throw new IllegalStateException("could not allocate SonyVegas for probe", error);
        }
    }
}

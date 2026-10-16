package vegas.sony.api;

import com.google.gson.JsonObject;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;

/**
 * Etapa 3 regression coverage for LicenseVerifier:
 * valid license passes; tampered payload, wrong key, expired validity and
 * wrong product all fail closed. Key material is ephemeral (generated in-JVM).
 */
public final class LicenseVerifierProbe {

    public static void main(String[] args) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        java.security.interfaces.RSAPublicKey publicKey =
                (java.security.interfaces.RSAPublicKey) pair.getPublic();
        LicenseVerifier.setPublicKeyForTesting(publicKey.getModulus());

        long userId = 123456789L;
        long now = System.currentTimeMillis() / 1000L;

        // valid
        JsonObject valid = buildPayload(userId, now - 10, now + 3600);
        assertNull(LicenseVerifier.verify(signPayload(pair, valid), userId), "valid license");

        // no license at all (server without key / forged shape)
        assertRejected(LicenseVerifier.verify(null, userId), "missing container");

        // signature over a tampered payload must not verify
        JsonObject tampered = buildPayload(userId, now - 10, now + 3600);
        JsonObject signatureDoc = signPayload(pair, tampered);
        tampered.addProperty("role", "developer"); // mutate after signing
        assertRejected(LicenseVerifier.verify(signatureDoc, userId), "tampered payload");

        // wrong signing key
        KeyPair other = generator.generateKeyPair();
        assertRejected(LicenseVerifier.verify(signPayload(other, buildPayload(userId, now - 10, now + 3600)), userId),
                "wrong key");

        // expired
        assertRejected(LicenseVerifier.verify(signPayload(pair, buildPayload(userId, now - 7200, now - 3600)), userId),
                "expired license");

        // wrong product
        assertRejected(LicenseVerifier.verify(signPayload(pair, buildPayload(userId, now - 10, now + 3600, "other-product")), userId),
                "wrong product");

        // user mismatch
        assertRejected(LicenseVerifier.verify(signPayload(pair, buildPayload(userId, now - 10, now + 3600)), userId + 1),
                "user mismatch");

        LicenseVerifier.clearPublicKeyOverride();
        System.out.println("PASS: license verifier accepts the valid contract and rejects "
                + "missing/tampered/wrong-key/expired/wrong-product licenses");
    }

    private static JsonObject buildPayload(long userId, long iat, long exp) {
        return buildPayload(userId, iat, exp, "sonyvegas");
    }

    private static JsonObject buildPayload(long userId, long iat, long exp, String product) {
        JsonObject payload = new JsonObject();
        payload.addProperty("v", 1);
        payload.addProperty("product", product);
        payload.addProperty("userId", userId);
        payload.addProperty("role", "user");
        payload.addProperty("iat", iat);
        payload.addProperty("exp", exp);
        payload.addProperty("requireSignature", true);
        return payload;
    }

    private static JsonObject signPayload(KeyPair pair, JsonObject payload) throws Exception {
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(pair.getPrivate());
        signer.update(canonical(payload).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        JsonObject doc = new JsonObject();
        doc.add("payload", payload);
        doc.addProperty("signature", Base64.getEncoder().encodeToString(signer.sign()));
        return doc;
    }

    private static String canonical(JsonObject payload) {
        // Match vps/license_crypto.py: sorted keys, compact separators.
        StringBuilder out = new StringBuilder("{");
        java.util.TreeMap<String, com.google.gson.JsonElement> sorted =
                new java.util.TreeMap<String, com.google.gson.JsonElement>();
        for (java.util.Map.Entry<String, com.google.gson.JsonElement> e : payload.entrySet()) {
            sorted.put(e.getKey(), e.getValue());
        }
        boolean first = true;
        for (java.util.Map.Entry<String, com.google.gson.JsonElement> e : sorted.entrySet()) {
            if (!first) out.append(',');
            first = false;
            com.google.gson.JsonPrimitive prim = e.getValue().getAsJsonPrimitive();
            out.append('"').append(e.getKey()).append('"').append(':')
                    .append(prim.isBoolean()
                            ? (prim.getAsBoolean() ? "true" : "false")
                            : prim.isNumber() ? prim.getAsString() : '"' + prim.getAsString() + '"');
        }
        return out.append('}').toString();
    }

    private static void assertNull(String result, String scenario) {
        if (result != null) {
            throw new AssertionError(scenario + " rejected: " + result);
        }
    }

    private static void assertRejected(String result, String scenario) {
        if (result == null || result.isEmpty()) {
            throw new AssertionError(scenario + " unexpectedly accepted");
        }
    }
}

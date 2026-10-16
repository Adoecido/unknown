package vegas.sony.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.RSAPublicKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Verifies the signed license contract embedded in the /authenticated payload.
 *
 * The server signs {"payload":{...},"signature":"base64"} where the payload is
 * canonicalized JSON (sorted keys, no spaces) containing v/product/userId/
 * username/role/iat/exp/requireSignature. The client embeds only the public
 * key; the private key never leaves the operator machine.
 *
 * Rollout: deploy the server with vps/license_private.json FIRST, then release
 * a client whose verification is enforced (this class fails closed when the
 * field is missing or invalid).
 */
public final class LicenseVerifier {
    /** RSA-2048 public modulus from vps/license_public.json (public by design). */
    private static final String PUBLIC_KEY_MODULUS_HEX =
            "9ad9f95ca5702214c5932b4b2b5f0d52f64888a27ef5e3377d50ccfd05ef5be2a49aba38a42f51a41a9c5320f14900e0371316725ec77ad55b3c0b7c7aada305b3162e2977ce1a11218d8fbb9ced8bf9480bb9b38e2c61be19cf20abbe3c7799d77327f1b40febeb8607fcf708b99b6e2efde1b9e3f2dc7fcfb26d8ca1dccfcb3644e23aa442678530f67679b4fba5d57dc0c0ac8df656468e552d390651f231f44407669f31bc056119388a0a6b05ea742efe8352a159600d6efd9498b7d383b8332693f4be9dc6a51fe14132b36084bfbf52d3a2f28815acaff14b2227c53f9130a3585389ae11a958360d1f2f302e142c0e5ace2f8931ff7636b337d2dd55";
    private static final int PUBLIC_KEY_EXPONENT = 65537;
    private static final long CLOCK_SKEW_SECONDS = 120L;
    private static volatile BigInteger testModulusOverride;

    private LicenseVerifier() {
    }

    /** Test seam: unit probes substitute an ephemeral key pair. */
    public static void setPublicKeyForTesting(BigInteger modulus) {
        testModulusOverride = modulus;
    }

    public static void clearPublicKeyOverride() {
        testModulusOverride = null;
    }

    private static PublicKey publicKey() {
        BigInteger modulus = testModulusOverride != null
                ? testModulusOverride
                : new BigInteger(PUBLIC_KEY_MODULUS_HEX, 16);
        try {
            return KeyFactory.getInstance("RSA").generatePublic(
                    new RSAPublicKeySpec(modulus, BigInteger.valueOf(PUBLIC_KEY_EXPONENT)));
        } catch (Exception error) {
            throw new IllegalStateException("failed to build embedded license public key", error);
        }
    }

    /** Canonical JSON bytes, identical to vps/license_crypto.py (sorted keys, compact). */
    private static String canonicalJson(JsonObject payload) {
        StringBuilder builder = new StringBuilder();
        builder.append('{');
        boolean first = true;
        java.util.TreeMap<String, com.google.gson.JsonElement> sorted =
                new java.util.TreeMap<String, com.google.gson.JsonElement>();
        for (java.util.Map.Entry<String, com.google.gson.JsonElement> entry : payload.entrySet()) {
            sorted.put(entry.getKey(), entry.getValue());
        }
        for (java.util.Map.Entry<String, com.google.gson.JsonElement> entry : sorted.entrySet()) {
            if (!first) {
                builder.append(',');
            }
            first = false;
            builder.append('"').append(escapeJson(entry.getKey())).append('"').append(':')
                    .append(canonicalValue(entry.getValue()));
        }
        builder.append('}');
        return builder.toString();
    }

    private static String escapeJson(String text) {
        StringBuilder out = new StringBuilder(text.length() + 8);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"': out.append("\\\""); break;
                case '\\': out.append("\\\\"); break;
                case '\b': out.append("\\b"); break;
                case '\f': out.append("\\f"); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
            }
        }
        return out.toString();
    }

    private static String canonicalValue(com.google.gson.JsonElement element) {
        if (element.isJsonPrimitive()) {
            com.google.gson.JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isNumber()) {
                // JSON numbers from the server are integers here; compare textually
                return primitive.getAsString();
            }
            if (primitive.isBoolean()) {
                return primitive.getAsBoolean() ? "true" : "false";
            }
            return '"' + escapeJson(primitive.getAsString()) + '"';
        }
        throw new IllegalArgumentException("license payload contains non-primitive value");
    }

    /**
     * Verifies the license object attached to /authenticated. Returns null on
     * success or a short reason string (without secrets) on rejection.
     */
    public static String verify(JsonObject licenseContainer, long expectedUserId) {
        if (licenseContainer == null) {
            return "missing_license";
        }
        try {
            if (!licenseContainer.has("payload") || !licenseContainer.has("signature")) {
                return "malformed_license";
            }
            JsonObject payload = licenseContainer.getAsJsonObject("payload");
            String signatureB64 = licenseContainer.get("signature").getAsString();
            if (!payload.has("v") || payload.get("v").getAsInt() != 1) {
                return "unsupported_license_version";
            }
            if (!payload.has("product") || !"sonyvegas".equals(payload.get("product").getAsString())) {
                return "wrong_product";
            }
            if (!payload.has("userId") || payload.get("userId").getAsLong() != expectedUserId) {
                return "user_mismatch";
            }
            long now = System.currentTimeMillis() / 1000L;
            if (!payload.has("iat") || payload.get("iat").getAsLong() > now + CLOCK_SKEW_SECONDS) {
                return "issued_in_future";
            }
            if (!payload.has("exp") || payload.get("exp").getAsLong() <= now - CLOCK_SKEW_SECONDS) {
                return "license_expired";
            }
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(publicKey());
            verifier.update(canonicalJson(payload).getBytes(StandardCharsets.UTF_8));
            if (!verifier.verify(Base64.getDecoder().decode(signatureB64))) {
                return "bad_signature";
            }
            return null;
        } catch (RuntimeException | java.security.GeneralSecurityException error) {
            return "verify_error:" + error.getClass().getSimpleName();
        }
    }
}

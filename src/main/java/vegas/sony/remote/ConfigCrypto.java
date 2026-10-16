package vegas.sony.remote;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Seals config payloads before they leave the machine.
 *
 * <p>Everything the cloud stores - personal slots and published community
 * configs alike - is an AES-256-GCM envelope, so the server only ever holds
 * ciphertext and a copied community config arrives sealed as well. The key is
 * shared by every client because community configs have to be readable by
 * whoever downloads them; this hides the contents from the storage side, not
 * from someone who owns the client.
 */
final class ConfigCrypto {
    private static final Gson GSON = new Gson();
    private static final String ENVELOPE_VERSION = "v1";
    private static final String VERSION_FIELD = "enc";
    private static final String DATA_FIELD = "data";
    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String KEY_MATERIAL =
            "crimson.config.envelope.2026//$";

    private static final SecureRandom RANDOM = new SecureRandom();
    private static volatile SecretKeySpec cachedKey;

    private ConfigCrypto() {
    }

    /** True when the object is one of our envelopes rather than a raw profile. */
    static boolean isSealed(JsonObject candidate) {
        return candidate != null && candidate.has(VERSION_FIELD)
                && candidate.has(DATA_FIELD);
    }

    /**
     * Wraps a profile snapshot into an envelope. A failure here must never let
     * a plain profile reach the network, so it is reported to the caller.
     */
    static JsonObject seal(JsonObject payload) throws Exception {
        if (payload == null) throw new IllegalArgumentException("payload");
        byte[] plain = GSON.toJson(payload).getBytes(StandardCharsets.UTF_8);
        byte[] nonce = new byte[NONCE_BYTES];
        RANDOM.nextBytes(nonce);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, nonce));
        byte[] sealed = cipher.doFinal(plain);
        byte[] combined = new byte[nonce.length + sealed.length];
        System.arraycopy(nonce, 0, combined, 0, nonce.length);
        System.arraycopy(sealed, 0, combined, nonce.length, sealed.length);
        JsonObject envelope = new JsonObject();
        envelope.addProperty(VERSION_FIELD, ENVELOPE_VERSION);
        envelope.addProperty(DATA_FIELD, Base64.getEncoder().encodeToString(combined));
        return envelope;
    }

    /**
     * Unwraps an envelope. Configs saved before the envelope existed are still
     * plain profiles and are returned untouched.
     */
    static JsonObject open(JsonObject envelope) throws Exception {
        if (envelope == null) return null;
        if (!isSealed(envelope)) return envelope;
        if (!ENVELOPE_VERSION.equals(envelope.get(VERSION_FIELD).getAsString())) {
            throw new IllegalStateException("Unsupported config envelope");
        }
        byte[] combined = Base64.getDecoder().decode(
                envelope.get(DATA_FIELD).getAsString());
        if (combined.length <= NONCE_BYTES) {
            throw new IllegalStateException("Truncated config envelope");
        }
        byte[] nonce = new byte[NONCE_BYTES];
        System.arraycopy(combined, 0, nonce, 0, NONCE_BYTES);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, nonce));
        byte[] plain = cipher.doFinal(combined, NONCE_BYTES,
                combined.length - NONCE_BYTES);
        return GSON.fromJson(new String(plain, StandardCharsets.UTF_8),
                JsonObject.class);
    }

    private static SecretKeySpec key() throws Exception {
        SecretKeySpec current = cachedKey;
        if (current != null) return current;
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] material = digest.digest(
                KEY_MATERIAL.replace("//$", "").getBytes(StandardCharsets.UTF_8));
        current = new SecretKeySpec(material, "AES");
        cachedKey = current;
        return current;
    }
}

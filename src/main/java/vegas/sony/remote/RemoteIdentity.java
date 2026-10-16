package vegas.sony.remote;

import vegas.sony.runtime.NativeBridge;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

final class RemoteIdentity {
    private static final byte[] KEY_LABEL =
            "crimson.identity.key.v1".getBytes(StandardCharsets.UTF_8);
    private static final byte[] ASSOCIATED_DATA =
            "crimson.identity.v1".getBytes(StandardCharsets.UTF_8);
    private static volatile String cachedIdentity;

    private RemoteIdentity() {
    }

    static String currentUsername() {
        String identity = currentIdentity();
        int separator = identity.indexOf('\t');
        return separator < 0 ? identity : identity.substring(0, separator);
    }

    static String currentRole() {
        String identity = currentIdentity();
        int separator = identity.indexOf('\t');
        if (separator < 0 || separator + 1 >= identity.length()) return "user";
        String role = identity.substring(separator + 1);
        return "developer".equalsIgnoreCase(role) ? "developer" : "user";
    }

    private static String currentIdentity() {
        String current = cachedIdentity;
        if (current != null && !current.isEmpty()) return current;

        try {
            current = NativeBridge.rid();
        } catch (Throwable ignored) {
            current = "";
        }
        if (isValidIdentity(current)) cachedIdentity = current;
        return isValidIdentity(current) ? current : "";
    }

    static String decrypt(String envelope, String token) throws Exception {
        if (envelope == null || !envelope.startsWith("v1.")
                || token == null || token.isEmpty()) {
            return "";
        }
        byte[] packet = Base64.getDecoder().decode(envelope.substring(3));
        byte[] key = null;
        byte[] nonce = null;
        byte[] encrypted = null;
        byte[] plaintext = null;
        try {
            if (packet.length < 30 || packet[0] != 1) return "";
            nonce = Arrays.copyOfRange(packet, 1, 13);
            byte[] tag = Arrays.copyOfRange(packet, 13, 29);
            byte[] ciphertext = Arrays.copyOfRange(packet, 29, packet.length);
            encrypted = new byte[ciphertext.length + tag.length];
            System.arraycopy(ciphertext, 0, encrypted, 0, ciphertext.length);
            System.arraycopy(tag, 0, encrypted, ciphertext.length, tag.length);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(KEY_LABEL);
            key = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(128, nonce));
            cipher.updateAAD(ASSOCIATED_DATA);
            plaintext = cipher.doFinal(encrypted);
            String identity = new String(plaintext, StandardCharsets.UTF_8);
            return isValidIdentity(identity) ? identity : "";
        } finally {
            Arrays.fill(packet, (byte)0);
            if (key != null) Arrays.fill(key, (byte)0);
            if (nonce != null) Arrays.fill(nonce, (byte)0);
            if (encrypted != null) Arrays.fill(encrypted, (byte)0);
            if (plaintext != null) Arrays.fill(plaintext, (byte)0);
        }
    }

    private static boolean isValidUsername(String username) {
        return username != null && !username.isEmpty() && username.length() <= 32
                && username.indexOf('\r') < 0 && username.indexOf('\n') < 0
                && username.indexOf('\t') < 0 && username.indexOf('|') < 0;
    }

    private static boolean isValidIdentity(String identity) {
        if (identity == null || identity.isEmpty() || identity.length() > 80
                || identity.indexOf('\r') >= 0 || identity.indexOf('\n') >= 0
                || identity.indexOf('|') >= 0) {
            return false;
        }
        int separator = identity.indexOf('\t');
        if (separator < 0) return isValidUsername(identity);
        if (separator != identity.lastIndexOf('\t')) return false;
        String username = identity.substring(0, separator);
        String role = identity.substring(separator + 1);
        return isValidUsername(username)
                && ("developer".equalsIgnoreCase(role)
                    || "user".equalsIgnoreCase(role));
    }
}

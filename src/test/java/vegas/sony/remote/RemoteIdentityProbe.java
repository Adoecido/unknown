package vegas.sony.remote;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

public final class RemoteIdentityProbe {
    private RemoteIdentityProbe() {
    }

    public static void main(String[] arguments) throws Exception {
        String token = "remote-session-token";
        String identity = "Fixture User\tdeveloper";
        String envelope = createNativeFormatEnvelope(identity, token);
        String decrypted = RemoteIdentity.decrypt(envelope, token);
        if (!identity.equals(decrypted)) {
            throw new AssertionError("identity envelope did not decrypt");
        }

        byte[] damaged = Base64.getDecoder().decode(envelope.substring(3));
        damaged[damaged.length - 1] ^= 1;
        try {
            RemoteIdentity.decrypt("v1." + Base64.getEncoder().encodeToString(damaged), token);
            throw new AssertionError("tampered identity envelope was accepted");
        } catch (AssertionError failure) {
            throw failure;
        } catch (Exception expected) {
            // AES-GCM authentication rejects the modified packet.
        }
    }

    private static String createNativeFormatEnvelope(String username,
            String token) throws Exception {
        byte[] nonce = new byte[12];
        for (int index = 0; index < nonce.length; index++) {
            nonce[index] = (byte)(index + 1);
        }
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update("crimson.identity.key.v1".getBytes(StandardCharsets.UTF_8));
        byte[] key = digest.digest(token.getBytes(StandardCharsets.UTF_8));
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"),
                new GCMParameterSpec(128, nonce));
        cipher.updateAAD("crimson.identity.v1".getBytes(StandardCharsets.UTF_8));
        byte[] cipherAndTag = cipher.doFinal(username.getBytes(StandardCharsets.UTF_8));
        int ciphertextSize = cipherAndTag.length - 16;
        byte[] packet = new byte[1 + nonce.length + 16 + ciphertextSize];
        packet[0] = 1;
        System.arraycopy(nonce, 0, packet, 1, nonce.length);
        System.arraycopy(cipherAndTag, ciphertextSize, packet,
                1 + nonce.length, 16);
        System.arraycopy(cipherAndTag, 0, packet,
                1 + nonce.length + 16, ciphertextSize);
        return "v1." + Base64.getEncoder().encodeToString(packet);
    }
}

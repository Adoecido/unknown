package vegas.sony.utils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.jetbrains.annotations.Nullable;

public final class Str {
    private Str() {
    }

    @Nullable
    public static EncString enc(@Nullable String plain) {
        return plain == null ? null : new EncString(plain);
    }

    public static String dec(byte[] cipher, int key) {
        byte[] tmp = cipher.clone();
        xorInPlace(tmp, key);
        String result = new String(tmp, StandardCharsets.UTF_8);
        Arrays.fill(tmp, (byte) 0);
        return result;
    }

    public static String dec(String packed, int key) {
        byte[] cipher = java.util.Base64.getDecoder().decode(packed);
        return dec(cipher, key);
    }

    /**
     * Builds a stable opaque identifier without ever materializing the visible
     * text without its formatting separators. The token remains stable across
     * runs and payload builds, so it is safe for persisted configuration keys.
     */
    public static String stableVisibleToken(String prefix, @Nullable String value) {
        long hash = 0xcbf29ce484222325L;
        if (value != null) {
            for (int index = 0; index < value.length(); ++index) {
                char current = value.charAt(index);
                if (current == '\u00a7' && index + 1 < value.length()) {
                    ++index;
                    continue;
                }
                current = Character.toLowerCase(current);
                hash ^= current & 0xff;
                hash *= 0x100000001b3L;
                hash ^= current >>> 8;
                hash *= 0x100000001b3L;
            }
        }
        return prefix + Long.toUnsignedString(hash, 36);
    }

    public static boolean visibleEqualsIgnoreCase(@Nullable String left, @Nullable String right) {
        if (left == null || right == null) {
            return left == right;
        }
        int leftIndex = 0;
        int rightIndex = 0;
        while (true) {
            leftIndex = nextVisibleIndex(left, leftIndex);
            rightIndex = nextVisibleIndex(right, rightIndex);
            boolean leftDone = leftIndex >= left.length();
            boolean rightDone = rightIndex >= right.length();
            if (leftDone || rightDone) {
                return leftDone && rightDone;
            }
            if (Character.toLowerCase(left.charAt(leftIndex))
                    != Character.toLowerCase(right.charAt(rightIndex))) {
                return false;
            }
            ++leftIndex;
            ++rightIndex;
        }
    }

    private static void xorInPlace(byte[] buf, int key) {
        int k0 = (key >>> 24) & 0xFF;
        int k1 = (key >>> 16) & 0xFF;
        int k2 = (key >>> 8) & 0xFF;
        int k3 = key & 0xFF;
        for (int i = 0; i < buf.length; i++) {
            switch (i & 3) {
                case 0: buf[i] ^= (byte) k0; break;
                case 1: buf[i] ^= (byte) k1; break;
                case 2: buf[i] ^= (byte) k2; break;
                case 3: buf[i] ^= (byte) k3; break;
            }
        }
    }

    private static int nextVisibleIndex(String value, int index) {
        while (index < value.length() && value.charAt(index) == '\u00a7' && index + 1 < value.length()) {
            index += 2;
        }
        return index;
    }
}

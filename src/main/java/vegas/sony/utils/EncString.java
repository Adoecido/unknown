package vegas.sony.utils;

import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import org.jetbrains.annotations.Nullable;

public final class EncString {
    private final byte[] data;
    private final int key;

    public EncString(@Nullable String plain) {
        if (plain == null) {
            this.data = null;
            this.key = 0;
            return;
        }
        this.key = ThreadLocalRandom.current().nextInt() ^ System.identityHashCode(this);
        byte[] raw = plain.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        xorInPlace(raw, this.key);
        this.data = raw;
    }

    EncString(byte[] ciphertext, int key) {
        this.data = ciphertext;
        this.key = key;
    }

    @Nullable
    public String get() {
        if (this.data == null) {
            return null;
        }
        byte[] tmp = this.data.clone();
        xorInPlace(tmp, this.key);
        String result = new String(tmp, java.nio.charset.StandardCharsets.UTF_8);
        Arrays.fill(tmp, (byte) 0);
        return result;
    }

    public void get(char[] out, int off, int len) {
        if (this.data == null) {
            return;
        }
        byte[] tmp = this.data.clone();
        xorInPlace(tmp, this.key);
        String s = new String(tmp, java.nio.charset.StandardCharsets.UTF_8);
        Arrays.fill(tmp, (byte) 0);
        int copyLen = Math.min(len, s.length());
        s.getChars(0, copyLen, out, off);
    }

    public int length() {
        if (this.data == null) {
            return 0;
        }
        return get().length();
    }

    public boolean isEmpty() {
        return this.data == null || this.data.length == 0;
    }

    public boolean visibleEqualsIgnoreCase(@Nullable String other) {
        if (other == null || this.data == null) {
            return other == null && this.data == null;
        }
        return Str.visibleEqualsIgnoreCase(this.get(), other);
    }

    public boolean visibleMatchesSearch(@Nullable String query, boolean exact) {
        if (query == null || this.data == null) {
            return false;
        }
        String normalizedQuery = query.toLowerCase(Locale.ROOT).replace(" ", "").replace("-", "");
        if (normalizedQuery.isEmpty()) {
            return true;
        }
        String protectedText = this.get();
        if (exact) {
            int queryIndex = 0;
            for (int i = 0; i < protectedText.length(); ++i) {
                char c = protectedText.charAt(i);
                if (c == '\u00a7' && i + 1 < protectedText.length()) {
                    ++i;
                    continue;
                }
                if (c == ' ' || c == '-') {
                    continue;
                }
                if (queryIndex >= normalizedQuery.length()
                        || Character.toLowerCase(c) != normalizedQuery.charAt(queryIndex++)) {
                    return false;
                }
            }
            return queryIndex == normalizedQuery.length();
        }
        int[] prefix = new int[normalizedQuery.length()];
        for (int i = 1, matched = 0; i < normalizedQuery.length(); ++i) {
            while (matched > 0 && normalizedQuery.charAt(i) != normalizedQuery.charAt(matched)) {
                matched = prefix[matched - 1];
            }
            if (normalizedQuery.charAt(i) == normalizedQuery.charAt(matched)) {
                ++matched;
            }
            prefix[i] = matched;
        }
        int matched = 0;
        for (int i = 0; i < protectedText.length(); ++i) {
            char c = protectedText.charAt(i);
            if (c == '\u00a7' && i + 1 < protectedText.length()) {
                ++i;
                continue;
            }
            if (c == ' ' || c == '-') {
                continue;
            }
            c = Character.toLowerCase(c);
            while (matched > 0 && c != normalizedQuery.charAt(matched)) {
                matched = prefix[matched - 1];
            }
            if (c == normalizedQuery.charAt(matched) && ++matched == normalizedQuery.length()) {
                return true;
            }
        }
        return false;
    }

    public String visiblePrefixUppercase(int maxVisibleChars) {
        if (this.data == null || maxVisibleChars <= 0) {
            return "";
        }
        String protectedText = this.get();
        StringBuilder prefix = new StringBuilder(maxVisibleChars * 3);
        int visible = 0;
        for (int i = 0; i < protectedText.length() && visible < maxVisibleChars; ++i) {
            char c = protectedText.charAt(i);
            if (c == '\u00a7' && i + 1 < protectedText.length()) {
                ++i;
                continue;
            }
            if (visible++ > 0) {
                prefix.append('\u00a7').append('r');
            }
            prefix.append(Character.toUpperCase(c));
        }
        return prefix.toString();
    }

    public void wipe() {
        if (this.data != null) {
            Arrays.fill(this.data, (byte) 0);
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

}

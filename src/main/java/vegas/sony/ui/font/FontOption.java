package vegas.sony.ui.font;

import vegas.sony.SonyVegas;
import vegas.sony.unmap.ModeSelection;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class FontOption
extends ModeSelection {
    private static final long FNV_OFFSET = 0xcbf29ce484222325L;
    private static final long FNV_PRIME = 0x100000001b3L;
    private static final long LANGUAGE_NATIVE_KEY = 0x9ec5ba4f7c4c69fcL;
    private static final int RESOURCE_HEADER_SIZE = 8;

    private String j;
    private String C;
    private final HashMap<Long, String> g = new HashMap<Long, String>();
    private final HashMap<Long, String> N = new HashMap<Long, String>();

    public SmoothFontRenderer k(float f, boolean bl) {
        return SonyVegas.INSTANCE.getFontManager().b(this.b(), f, bl);
    }

    public void g(BaseFontOption baseFontOption) {
        for (Map.Entry<Long, String> entry : this.g.entrySet()) {
            if (entry.getKey().longValue() == LANGUAGE_NATIVE_KEY) {
                continue;
            }
            String source = baseFontOption.p().get(entry.getKey());
            String translated = entry.getValue();
            if (source == null || translated == null || translated.length() == 0) {
                continue;
            }
            this.N.put(Long.valueOf(hashVisible(source)), translated);
        }
    }

    public Map<Long, String> p() {
        return this.g;
    }

    @Override
    public String toString() {
        return this.C;
    }

    @Override
    public String getName() {
        return this.C;
    }

    public SmoothFontRenderer w(float f) {
        return this.k(f, false);
    }

    @Override
    public String getSerializedName() {
        return this.j;
    }

    public FontOption(String string) {
        super("");
        this.j = string;
        this.p$src$V$169egi9();
    }

    private void p$src$V$169egi9() {
        String resourceName = this.j.toLowerCase() + ".properties.txt";
        byte[] resource = SonyVegas.readResource(resourceName);
        if (resource == null) {
            return;
        }
        try {
            this.parseResource(resource);
            this.C = this.g.get(Long.valueOf(LANGUAGE_NATIVE_KEY));
        }
        catch (Exception exception) {
            SonyVegas.logThrowable(exception);
        }
        finally {
            Arrays.fill(resource, (byte)0);
        }
    }

    private void parseResource(byte[] resource) {
        boolean encrypted = resource.length >= RESOURCE_HEADER_SIZE
                && resource[0] == 'S' && resource[1] == 'V'
                && resource[2] == 'P' && resource[3] == '2';
        int payloadStart = encrypted ? RESOURCE_HEADER_SIZE : 0;
        int key = encrypted
                ? (resource[4] & 0xff) << 24 | (resource[5] & 0xff) << 16
                    | (resource[6] & 0xff) << 8 | resource[7] & 0xff
                : 0;
        int lineStart = payloadStart;
        for (int i = payloadStart; i <= resource.length; ++i) {
            if (i < resource.length && decodedByte(resource, i, payloadStart, key, encrypted) != '\n') {
                continue;
            }
            int lineEnd = i;
            if (lineEnd > lineStart
                    && decodedByte(resource, lineEnd - 1, payloadStart, key, encrypted) == '\r') {
                --lineEnd;
            }
            this.parseLine(resource, lineStart, lineEnd, payloadStart, key, encrypted);
            lineStart = i + 1;
        }
    }

    private void parseLine(byte[] resource, int start, int end, int payloadStart,
            int key, boolean encrypted) {
        if (start >= end) {
            return;
        }
        int separator = -1;
        for (int i = start; i < end; ++i) {
            if (decodedByte(resource, i, payloadStart, key, encrypted) == '=') {
                separator = i;
                break;
            }
        }
        if (separator <= start) {
            return;
        }
        long keyHash = FNV_OFFSET;
        for (int i = start; i < separator; ++i) {
            keyHash ^= decodedByte(resource, i, payloadStart, key, encrypted);
            keyHash *= FNV_PRIME;
        }
        String value = decodeProtectedValue(resource, separator + 1, end,
                payloadStart, key, encrypted);
        this.g.put(Long.valueOf(keyHash), value);
    }

    private static String decodeProtectedValue(byte[] resource, int start, int end,
            int payloadStart, int key, boolean encrypted) {
        StringBuilder protectedValue = new StringBuilder(Math.max(16, (end - start) * 3));
        boolean hasVisibleCharacter = false;
        boolean formattingCodePending = false;
        for (int i = start; i < end;) {
            int first = decodedByte(resource, i, payloadStart, key, encrypted);
            int codePoint;
            int consumed;
            if (first == '\\' && i + 1 < end) {
                int escaped = decodedByte(resource, i + 1, payloadStart, key, encrypted);
                if (escaped == 'n') codePoint = '\n';
                else if (escaped == 'r') codePoint = '\r';
                else if (escaped == 't') codePoint = '\t';
                else if (escaped == 'f') codePoint = '\f';
                else codePoint = escaped;
                consumed = 2;
            } else if ((first & 0x80) == 0) {
                codePoint = first;
                consumed = 1;
            } else if ((first & 0xe0) == 0xc0 && i + 1 < end) {
                codePoint = (first & 0x1f) << 6
                        | decodedByte(resource, i + 1, payloadStart, key, encrypted) & 0x3f;
                consumed = 2;
            } else if ((first & 0xf0) == 0xe0 && i + 2 < end) {
                codePoint = (first & 0x0f) << 12
                        | (decodedByte(resource, i + 1, payloadStart, key, encrypted) & 0x3f) << 6
                        | decodedByte(resource, i + 2, payloadStart, key, encrypted) & 0x3f;
                consumed = 3;
            } else if ((first & 0xf8) == 0xf0 && i + 3 < end) {
                codePoint = (first & 0x07) << 18
                        | (decodedByte(resource, i + 1, payloadStart, key, encrypted) & 0x3f) << 12
                        | (decodedByte(resource, i + 2, payloadStart, key, encrypted) & 0x3f) << 6
                        | decodedByte(resource, i + 3, payloadStart, key, encrypted) & 0x3f;
                consumed = 4;
            } else {
                codePoint = 0xfffd;
                consumed = 1;
            }
            i += consumed;
            if (formattingCodePending) {
                protectedValue.appendCodePoint(codePoint);
                formattingCodePending = false;
                continue;
            }
            if (codePoint == 0x00a7) {
                protectedValue.appendCodePoint(codePoint);
                formattingCodePending = true;
                continue;
            }
            if (hasVisibleCharacter) {
                protectedValue.append('\u00a7').append('r');
            }
            protectedValue.appendCodePoint(codePoint);
            hasVisibleCharacter = true;
        }
        return protectedValue.toString();
    }

    private static int decodedByte(byte[] resource, int index, int payloadStart,
            int key, boolean encrypted) {
        int value = resource[index] & 0xff;
        if (!encrypted) {
            return value;
        }
        int offset = index - payloadStart;
        int keyByte;
        switch (offset & 3) {
            case 0: keyByte = key >>> 24 & 0xff; break;
            case 1: keyByte = key >>> 16 & 0xff; break;
            case 2: keyByte = key >>> 8 & 0xff; break;
            default: keyByte = key & 0xff;
        }
        return value ^ keyByte ^ (offset * 131 + 0x5d & 0xff);
    }

    private static long hashVisible(String value) {
        long hash = FNV_OFFSET;
        for (int i = 0; i < value.length(); ++i) {
            char character = value.charAt(i);
            if (character == '\u00a7' && i + 1 < value.length()) {
                ++i;
                continue;
            }
            hash ^= character;
            hash *= FNV_PRIME;
        }
        return hash;
    }

    public String s(String string) {
        if (string == null) {
            return null;
        }
        String translated = this.N.get(Long.valueOf(hashVisible(string)));
        return translated == null ? string : translated;
    }

    public FontFamily b() {
        return FontFamily.PROXIMA;
    }
}

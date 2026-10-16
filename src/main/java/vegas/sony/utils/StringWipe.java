package vegas.sony.utils;

import java.lang.reflect.Field;
import java.util.Arrays;

public final class StringWipe {

    private static final Field VALUE_FIELD;

    static {
        Field f = null;
        try {
            f = String.class.getDeclaredField("value");
            f.setAccessible(true);
        } catch (Exception ignored) {}
        VALUE_FIELD = f;
    }

    private StringWipe() {}

    public static void wipe(String s) {
        if (s == null || s.isEmpty() || VALUE_FIELD == null) return;
        try {
            char[] value = (char[]) VALUE_FIELD.get(s);
            if (value != null && value.length > 0) {
                Arrays.fill(value, '\0');
            }
        } catch (Exception ignored) {}
    }
}

package vegas.sony.remote;

import java.awt.Color;
import java.lang.reflect.Field;
import vegas.sony.value.ColorValue;

/** Guards the configured colour against animated Fade/Rainbow display frames. */
public final class RemoteColorSerializationProbe {
    public static void main(String[] args) throws Exception {
        Color expected = new Color(24, 83, 219, 173);
        ColorValue value = ColorValue.create(null, "ArrayList", expected);
        set(value, "fadeEnabled", Boolean.TRUE);
        set(value, "fadeColor", new Color(238, 47, 81));

        Color configured = RemoteControlClient.configuredColor(value);
        if (configured.getRed() != expected.getRed()
                || configured.getGreen() != expected.getGreen()
                || configured.getBlue() != expected.getBlue()
                || configured.getAlpha() != expected.getAlpha()) {
            throw new AssertionError("Animated display frame replaced the configured colour");
        }
        System.out.println("Remote colour serialization keeps the configured ArrayList colour.");
    }

    private static void set(ColorValue value, String name, Object next)
            throws Exception {
        Field field = ColorValue.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(value, next);
    }
}

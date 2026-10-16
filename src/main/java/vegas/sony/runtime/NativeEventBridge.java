package vegas.sony.runtime;

import vegas.sony.SonyVegas;

public class NativeEventBridge {
    public static void reg(Class<?> eventClass, int eventId) {
    }

    public static void call(Object event) {
        if (SonyVegas.INSTANCE == null) {
            return;
        }
    }

}


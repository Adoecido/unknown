package vegas.sony.value;

import vegas.sony.value.ValueDisplayNameMode;

public class ValueDisplayNameModeSwitchMap {
    public static final int[] T = new int[ValueDisplayNameMode.values().length];

    ValueDisplayNameModeSwitchMap() {
    }

    static {
        try {
            ValueDisplayNameModeSwitchMap.T[ValueDisplayNameMode.CUSTOM.ordinal()] = 1;
        }
        catch (NoSuchFieldError noSuchFieldError) {
            // empty catch block
        }
        try {
            ValueDisplayNameModeSwitchMap.T[ValueDisplayNameMode.SIMPLE.ordinal()] = 2;
        }
        catch (NoSuchFieldError noSuchFieldError) {
            // empty catch block
        }
        try {
            ValueDisplayNameModeSwitchMap.T[ValueDisplayNameMode.FULL.ordinal()] = 3;
        }
        catch (NoSuchFieldError noSuchFieldError) {
            // empty catch block
        }
    }
}


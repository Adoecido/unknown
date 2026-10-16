package vegas.sony.input;


public class InputFocusState {
    private boolean focused = true;
    private static int legacyState;

    public void markFocused() {
        this.focused = true;
    }

    public static int getLegacySentinelResult() {
        int state = InputFocusState.getLegacyState();
        return 0;
    }

    /**
     * The window-message flag alone is not enough on LWJGL 2, where focus
     * messages never reach the client's message pump, so the operating state
     * of the window has the final say.
     */
    public boolean isFocused() {
        return this.focused && GameWindowFocus.isWindowActive();
    }

    public static void setLegacyState(int state) {
        legacyState = state;
    }


    public void markUnfocused() {
        this.focused = false;
    }

    public static int getLegacyState() {
        return legacyState;
    }

    static {
        if (InputFocusState.getLegacyState() == 0) {
            InputFocusState.setLegacyState(49);
        }
    }
}


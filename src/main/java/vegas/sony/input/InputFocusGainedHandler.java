package vegas.sony.input;

import vegas.sony.input.InputEventDispatcher;
import vegas.sony.input.InputEventHandler;

public class InputFocusGainedHandler
implements InputEventHandler {
    @Override
    public boolean handle(long windowHandle, long focusState) {
        InputEventDispatcher.getInstance().getFocusState().markFocused();
        return false;
    }
}

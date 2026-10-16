package vegas.sony.input;

import vegas.sony.input.InputEventDispatcher;
import vegas.sony.input.InputEventHandler;
import vegas.sony.input.Win32InputConstants;

public class MouseWheelInputHandler
implements InputEventHandler {
    @Override
    public boolean handle(long packedWheelMetadata, long secondArgument) {
        short scrollDelta = Win32InputConstants.extractHighWord(packedWheelMetadata);
        return InputEventDispatcher.getInstance().getMouseState().setScrollDelta(scrollDelta);
    }
}

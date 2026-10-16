package vegas.sony.input;

import vegas.sony.input.InputEventDispatcher;
import vegas.sony.input.InputEventHandler;
import vegas.sony.input.KeyboardCodeUtil;

public class ExtendedMouseButtonPressInputHandler
implements InputEventHandler {
    @Override
    public boolean handle(long buttonMetadata, long secondArgument) {
        return InputEventDispatcher.getInstance().getMouseState().setButtonState(KeyboardCodeUtil.decodeExtendedMouseButton(buttonMetadata), true);
    }
}

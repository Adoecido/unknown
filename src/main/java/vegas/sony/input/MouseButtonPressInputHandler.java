package vegas.sony.input;

import vegas.sony.input.InputEventDispatcher;
import vegas.sony.input.InputEventHandler;

public class MouseButtonPressInputHandler
implements InputEventHandler {
    int button;

    public MouseButtonPressInputHandler(int button) {
        this.button = button;
    }

    @Override
    public boolean handle(long firstArgument, long secondArgument) {
        return InputEventDispatcher.getInstance().getMouseState().setButtonState(this.button, true);
    }
}

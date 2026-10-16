package vegas.sony.input;

import vegas.sony.input.InputEventDispatcher;
import vegas.sony.input.InputEventHandler;
import vegas.sony.input.Win32InputConstants;

public class MouseMoveInputHandler
implements InputEventHandler {
    @Override
    public boolean handle(long firstArgument, long packedCoordinates) {
        short mouseX = Win32InputConstants.extractLowWord(packedCoordinates);
        short mouseY = Win32InputConstants.extractHighWord(packedCoordinates);
        return InputEventDispatcher.getInstance().getMouseState().updateCursorPosition(mouseX, mouseY);
    }
}

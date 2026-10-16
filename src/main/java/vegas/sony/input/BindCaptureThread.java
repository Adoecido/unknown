package vegas.sony.input;

import vegas.sony.SonyVegas;
import vegas.sony.input.BindCaptureTask;
import vegas.sony.input.BindCaptureThreadConstructorMarker;
import vegas.sony.input.InputEventDispatcher;
import vegas.sony.input.KeyboardInputState;
import vegas.sony.input.MouseInput;
import vegas.sony.module.none.ClientSettings;
import java.util.ArrayList;
import java.util.Collections;

class BindCaptureThread
extends Thread {
    final BindCaptureTask captureTask;

    BindCaptureThread(BindCaptureTask bindCaptureTask, BindCaptureThreadConstructorMarker ignoredMarker) {
        this(bindCaptureTask);
    }

    private BindCaptureThread(BindCaptureTask bindCaptureTask) {
        this.captureTask = bindCaptureTask;
    }

    @Override
    public void run() {
        ClientSettings clientSettings = SonyVegas.INSTANCE.getModManager().getMod(ClientSettings.class);
        KeyboardInputState keyboardState = InputEventDispatcher.getInstance().getKeyboardState();
        long lastKeyboardChangeTime = keyboardState.getLastChangeTime();
        long lastMouseChangeTime = MouseInput.getLastChangeTimeBridge();
        ArrayList<Integer> capturedInputs = new ArrayList<Integer>();
        int firstKeyboardKey = -1;
        int firstMouseButton = -1;
        while (true) {
            int inputCode;
            if (Thread.interrupted()) {
                this.captureTask.finishCapture();
                return;
            }
            if (firstKeyboardKey != -1 && !keyboardState.isKeyDown(firstKeyboardKey) || firstMouseButton != -1 && !MouseInput.isButtonDown(firstMouseButton) || capturedInputs.size() >= 3) break;
            if (lastKeyboardChangeTime != keyboardState.getLastChangeTime() && keyboardState.isLastKeyDown()) {
                lastKeyboardChangeTime = keyboardState.getLastChangeTime();
                inputCode = keyboardState.getLastKey();
                if (inputCode == 27) {
                    this.captureTask.getBendable().setBoundInputs(Collections.emptyList());
                    this.captureTask.finishCapture();
                    return;
                }
                if (capturedInputs.contains(inputCode)) continue;
                capturedInputs.add(inputCode);
                if (firstKeyboardKey == -1) {
                    firstKeyboardKey = inputCode;
                }
                if (!clientSettings.multiKeybinding.getEffectiveValue().booleanValue()) break;
                continue;
            }
            if (keyboardState.isKeyDown(160)) continue;
            if (lastMouseChangeTime != MouseInput.getLastChangeTimeBridge() && MouseInput.isLastButtonDown()) {
                lastMouseChangeTime = MouseInput.getLastChangeTimeBridge();
                if (MouseInput.getLastButtonBridge() == 0) {
                    this.captureTask.getBendable().setBoundInputs(Collections.emptyList());
                    break;
                }
                inputCode = -100 + MouseInput.getLastButtonBridge();
                if (firstMouseButton == -1) {
                    firstMouseButton = MouseInput.getLastButtonBridge();
                }
                if (capturedInputs.contains(inputCode)) continue;
                capturedInputs.add(inputCode);
                if (!clientSettings.multiKeybinding.getEffectiveValue().booleanValue()) break;
                continue;
            }
            try {
                Thread.sleep(10L);
            }
            catch (InterruptedException interruptedException) {
                {}
            }
        }
        this.captureTask.getBendable().setBoundInputs(capturedInputs);
        this.captureTask.finishCapture();
    }

    private static InterruptedException identityInterruptedException(InterruptedException interruptedException) {
        return interruptedException;
    }
}

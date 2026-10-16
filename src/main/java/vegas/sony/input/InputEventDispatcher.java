package vegas.sony.input;

import vegas.sony.SonyVegas;
import vegas.sony.config.Profile;
import vegas.sony.input.CharacterInputHandler;
import vegas.sony.input.ExtendedMouseButtonPressInputHandler;
import vegas.sony.input.ExtendedMouseButtonReleaseInputHandler;
import vegas.sony.input.InputEventHandler;
import vegas.sony.input.InputFocusGainedHandler;
import vegas.sony.input.InputFocusLostHandlerPayload;
import vegas.sony.input.InputFocusState;
import vegas.sony.input.KeyboardInputState;
import vegas.sony.input.KeyboardPressInputHandler;
import vegas.sony.input.KeyboardReleaseInputHandler;
import vegas.sony.input.MouseButtonPressInputHandler;
import vegas.sony.input.MouseButtonReleaseInputHandler;
import vegas.sony.input.MouseInputState;
import vegas.sony.input.MouseMoveInputHandler;
import vegas.sony.input.MouseWheelInputHandler;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.wrapper.impl.ForgeVersion;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.RenderTypeBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class InputEventDispatcher {
    public HashMap<Integer, InputEventHandler> handlers = new HashMap();
    private InputFocusState focusState;
    private static InputEventDispatcher instance;
    private MouseInputState mouseState;
    private KeyboardInputState keyboardState = new KeyboardInputState();
    private long windowHandle;
    private static int[] legacyState;


    public static void setLegacyState(int[] state) {
        legacyState = state;
    }

    public KeyboardInputState getKeyboardState() {
        return this.keyboardState;
    }

    public static InputEventDispatcher getInstance() {
        if (instance == null) {
            instance = new InputEventDispatcher();
        }
        return instance;
    }

    public InputEventDispatcher() {
        this.mouseState = new MouseInputState();
        this.focusState = new InputFocusState();
    }

    public InputFocusState getFocusState() {
        return this.focusState;
    }

    public void flushRenderBufferBeforeInput() {
        if (ForgeVersion.MC_1_21_10.v()) {
            return;
        }
        RenderTypeBuffer framerateLimitTracker = Minecraft.getFramerateLimitTracker();
        if (framerateLimitTracker.isNull()) {
            return;
        }
        if (!this.focusState.isFocused()) {
            return;
        }
        framerateLimitTracker.onInputReceived();
    }

    public boolean dispatch(int notificationCode, long firstArgument, long secondArgument) {
        InputEventHandler handler = this.handlers.get(notificationCode);
        if (handler != null) {
            this.flushRenderBufferBeforeInput();
            return handler.handle(firstArgument, secondArgument);
        }
        return false;
    }

    public void registerHandlers() {
        int[] legacyState = InputEventDispatcher.getLegacyState();
        this.handlers.put(258, new CharacterInputHandler());
        this.handlers.put(256, new KeyboardPressInputHandler());
        this.handlers.put(257, new KeyboardReleaseInputHandler());
        this.handlers.put(260, new KeyboardPressInputHandler());
        this.handlers.put(261, new KeyboardReleaseInputHandler());
        this.handlers.put(513, new MouseButtonPressInputHandler(0));
        this.handlers.put(516, new MouseButtonPressInputHandler(1));
        this.handlers.put(519, new MouseButtonPressInputHandler(2));
        this.handlers.put(514, new MouseButtonReleaseInputHandler(0));
        this.handlers.put(517, new MouseButtonReleaseInputHandler(1));
        this.handlers.put(520, new MouseButtonReleaseInputHandler(2));
        this.handlers.put(523, new ExtendedMouseButtonPressInputHandler());
        this.handlers.put(524, new ExtendedMouseButtonReleaseInputHandler());
        this.handlers.put(522, new MouseWheelInputHandler());
        this.handlers.put(512, new MouseMoveInputHandler());
        this.handlers.put(7, new InputFocusGainedHandler());
        this.handlers.put(8, new InputFocusLostHandlerPayload());
        if (legacyState != null) {
            GuiComponent.setLegacyComponentState(new GuiComponent[5]);
        }
    }

    public static int[] getLegacyState() {
        return legacyState;
    }

    public long getWindowHandle() {
        return this.windowHandle;
    }

    public void setWindowHandle(long windowHandle) {
        this.windowHandle = windowHandle;
        this.focusState.markFocused();
    }

    public MouseInputState getMouseState() {
        return this.mouseState;
    }

    /**
     * Reconciles the input state with Windows once per game tick.  CheatBreaker
     * can consume a window message before LWJGL's nUpdate hook observes it, so
     * message callbacks alone are not a reliable source for module binds.
     */
    public void pollBoundInputs() {
        try {
            Set<Integer> keyboardKeys = new HashSet<Integer>();
            Set<Integer> mouseButtons = new HashSet<Integer>();

            for (vegas.sony.module.Mod module : SonyVegas.INSTANCE.getModManager().collectMods()) {
                collectBoundInputs(module.getBind().getBoundInputs(), keyboardKeys, mouseButtons);
            }
            for (Profile profile : SonyVegas.INSTANCE.getProfilesManager().getProfiles()) {
                collectBoundInputs(profile.getBoundInputs(), keyboardKeys, mouseButtons);
            }

            for (int key : keyboardKeys) {
                this.keyboardState.setKeyState(key, isVirtualKeyDown(key));
            }
            for (int button : mouseButtons) {
                this.mouseState.setButtonState(button, isVirtualKeyDown(mouseButtonVirtualKey(button)));
            }
        } catch (Throwable ignored) {
            // Input polling must never interrupt the game tick during startup or shutdown.
        }
    }

    private static void collectBoundInputs(Iterable<Integer> inputs,
                                           Set<Integer> keyboardKeys,
                                           Set<Integer> mouseButtons) {
        for (Integer input : inputs) {
            if (input == null) continue;
            if (input > 0 && input <= 255) {
                keyboardKeys.add(input);
            } else if (input >= -100 && input <= -96) {
                mouseButtons.add(input + 100);
            }
        }
    }

    private static boolean isVirtualKeyDown(int virtualKey) {
        // NativeBridge normalizes GetAsyncKeyState's high bit to 0x100 so the
        // result is stable across the 32/64-bit JNI boundary.
        return (vegas.sony.runtime.NativeBridge.gks(virtualKey) & 0x100) != 0;
    }

    private static int mouseButtonVirtualKey(int button) {
        switch (button) {
            case 0: return 1; // VK_LBUTTON
            case 1: return 2; // VK_RBUTTON
            case 2: return 4; // VK_MBUTTON
            case 3: return 5; // VK_XBUTTON1
            case 4: return 6; // VK_XBUTTON2
            default: return 0;
        }
    }

    static {
        if (InputEventDispatcher.getLegacyState() != null) {
            InputEventDispatcher.setLegacyState(new int[1]);
        }
    }
}


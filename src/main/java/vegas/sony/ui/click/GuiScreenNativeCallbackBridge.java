package vegas.sony.ui.click;

import vegas.sony.SonyVegas;
import vegas.sony.input.InputEventDispatcher;
import vegas.sony.module.none.ClientSettings;

public class GuiScreenNativeCallbackBridge {
    public static void keyTyped(Object screen, char typedChar, int keyCode) {
    }

    public static void h(long timestamp) {
        InputEventDispatcher.getInstance().setWindowHandle(timestamp);
    }

    public static void updateScreen(Object screen) {
    }

    public static void mouseClicked(Object screen, int mouseX, int mouseY, int button) {
        ClientSettings.UI_EXECUTOR.execute(() -> {
            ClientSettings clientSettings = SonyVegas.INSTANCE.getModManager().getMod(ClientSettings.class);
            if (clientSettings != null && !clientSettings.inputEnabled) {
                clientSettings.handleMouseButton(button, mouseX, mouseY);
            }
        });
    }

    public static void drawScreen(Object screen, int mouseX, int mouseY, float partialTicks) {
        ClientSettings clientSettings = SonyVegas.INSTANCE.getModManager().getMod(ClientSettings.class);
        if (!clientSettings.inputEnabled) {
            clientSettings.renderGui();
        }
    }

    public static void initGui(Object screen) {
    }

    public static void handleMouseInput(Object screen) {
    }

    public static void mouseMovedOrUp(Object screen, int mouseX, int mouseY, int button) {
    }


    public static boolean onNotification(int code, long first, long second) {
        return InputEventDispatcher.getInstance().dispatch(code, first, second);
    }
}


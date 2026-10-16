package vegas.sony.ui.click.frame.impl.online;

import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.ui.click.component.PanelComponent;
import vegas.sony.ui.click.frame.impl.main.ClickGuiOverlaySpec;
import vegas.sony.ui.click.frame.impl.main.ClickGuiOverlayTransitionMode;
import vegas.sony.ui.click.frame.impl.online.OnlineAccountSettingsPageComponent;

public final class OnlineConnectionStatusConfigFactory {
    private static void b(PanelComponent panelComponent) {
        OnlineAccountSettingsPageComponent onlineAccountSettingsPageComponent = new OnlineAccountSettingsPageComponent();
        for (GuiComponent guiComponent : onlineAccountSettingsPageComponent.f()) {
            panelComponent.h(guiComponent, new Object[0]);
        }
    }

    private OnlineConnectionStatusConfigFactory() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static ClickGuiOverlaySpec T() {
        return ClickGuiOverlaySpec.builder().title("Welcome Back").sidecarIcon("welcome").initializeContent(OnlineConnectionStatusConfigFactory::b).transitionMode(ClickGuiOverlayTransitionMode.REPLACE).build();
    }
}


package vegas.sony.ui.click.frame.impl.main;

import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.ui.click.component.PanelComponent;
import vegas.sony.ui.click.frame.impl.main.ClickGuiOverlaySpec;
import vegas.sony.ui.click.frame.impl.main.ClickGuiOverlayTransitionMode;
import vegas.sony.ui.click.frame.impl.online.OnlineConnectionConnectingPageComponent;

public final class ClickGuiOverlaySpecFactoryC {
    public static ClickGuiOverlaySpec Z() {
        return ClickGuiOverlaySpec.builder().title("Connecting").sidecarIcon("connecting").initializeContent(ClickGuiOverlaySpecFactoryC::D).transitionMode(ClickGuiOverlayTransitionMode.REPLACE).build();
    }

    private static void D(PanelComponent panelComponent) {
        OnlineConnectionConnectingPageComponent onlineConnectionConnectingPageComponent = new OnlineConnectionConnectingPageComponent();
        for (GuiComponent guiComponent : onlineConnectionConnectingPageComponent.f()) {
            panelComponent.h(guiComponent, new Object[0]);
        }
    }

    private ClickGuiOverlaySpecFactoryC() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}


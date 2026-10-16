package vegas.sony.ui.click.frame.impl.main;

import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.ui.click.component.PanelComponent;
import vegas.sony.ui.click.frame.impl.main.ClickGuiOverlaySpec;
import vegas.sony.ui.click.frame.impl.main.ClickGuiOverlayTransitionMode;
import vegas.sony.ui.click.frame.impl.online.OnlineAccountConnectedPageComponent;

public final class ClickGuiOverlaySpecFactoryA {
    private static void B(PanelComponent panelComponent) {
        OnlineAccountConnectedPageComponent onlineAccountConnectedPageComponent = new OnlineAccountConnectedPageComponent();
        for (GuiComponent guiComponent : onlineAccountConnectedPageComponent.f()) {
            panelComponent.h(guiComponent, new Object[0]);
        }
    }

    private ClickGuiOverlaySpecFactoryA() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static ClickGuiOverlaySpec o() {
        return ClickGuiOverlaySpec.builder().title("Client Login").sidecarIcon("login").initializeContent(ClickGuiOverlaySpecFactoryA::B).transitionMode(ClickGuiOverlayTransitionMode.REPLACE).build();
    }
}


package vegas.sony.protocol.event;

import vegas.sony.friend.UserModel;
import vegas.sony.protocol.ZeusClient;
import vegas.sony.protocol.event.OnlineEvent;
import vegas.sony.ui.click.component.GuiComponent;

public abstract class OnlineEventPayloadBase
extends OnlineEvent {
    private static GuiComponent[] z;
    private final UserModel L;

    public static GuiComponent[] A() {
        return z;
    }

    public UserModel f() {
        return this.L;
    }

    public OnlineEventPayloadBase(ZeusClient zeusClient, UserModel userModel) {
        super(zeusClient);
        this.L = userModel;
    }

    public static void V(GuiComponent[] guiComponentArray) {
        z = guiComponentArray;
    }

    static {
        if (OnlineEventPayloadBase.A() == null) {
            OnlineEventPayloadBase.V(new GuiComponent[5]);
        }
    }
}


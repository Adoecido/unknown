package vegas.sony.protocol.event;

import vegas.sony.friend.UserModel;
import vegas.sony.protocol.PresenceState;
import vegas.sony.protocol.ZeusClient;
import vegas.sony.protocol.event.OnlineEventPayloadBase;
import vegas.sony.ui.click.component.GuiComponent;

public class FriendPresenceStateEvent
extends OnlineEventPayloadBase {
    private final PresenceState q;
    private static GuiComponent[] g;

    public PresenceState O() {
        return this.q;
    }

    public static GuiComponent[] a_up_arr_f() {
        return g;
    }

    public static void u(GuiComponent[] guiComponentArray) {
        g = guiComponentArray;
    }

    public FriendPresenceStateEvent(ZeusClient zeusClient, UserModel userModel, PresenceState presenceState) {
        super(zeusClient, userModel);
        this.q = presenceState;
    }

    static {
        if (FriendPresenceStateEvent.a_up_arr_f() == null) {
            FriendPresenceStateEvent.u(new GuiComponent[2]);
        }
    }
}


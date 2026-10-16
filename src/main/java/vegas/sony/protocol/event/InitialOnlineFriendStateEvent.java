package vegas.sony.protocol.event;

import vegas.sony.friend.FriendModel;
import vegas.sony.friend.FriendRequestModel;
import vegas.sony.protocol.ZeusClient;
import vegas.sony.protocol.event.OnlineEvent;
import vegas.sony.ui.click.component.GuiComponent;
import java.util.ArrayList;
import java.util.List;

public class InitialOnlineFriendStateEvent
extends OnlineEvent {
    private final List<FriendRequestModel> C;
    private final List<FriendRequestModel> K;
    private static GuiComponent[] Z;
    private final List<FriendModel> g = new ArrayList<FriendModel>();

    public List<FriendRequestModel> z() {
        return this.K;
    }

    public static void b(GuiComponent[] upArray) {
        Z = upArray;
    }

    public static GuiComponent[] X() {
        return Z;
    }

    public InitialOnlineFriendStateEvent(ZeusClient oZ, List<FriendModel> list, List<FriendRequestModel> list2, List<FriendRequestModel> list3) {
        super(oZ);
        this.K = new ArrayList<FriendRequestModel>();
        this.C = new ArrayList<FriendRequestModel>();
        this.g.addAll(list);
        this.K.addAll(list2);
        this.C.addAll(list3);
    }

    public List<FriendModel> q() {
        return this.g;
    }

    public List<FriendRequestModel> Z() {
        return this.C;
    }

    static {
        if (InitialOnlineFriendStateEvent.X() != null) {
            InitialOnlineFriendStateEvent.b(new GuiComponent[5]);
        }
    }
}


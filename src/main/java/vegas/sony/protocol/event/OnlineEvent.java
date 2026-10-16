package vegas.sony.protocol.event;

import vegas.sony.protocol.ZeusClient;
import vegas.sony.protocol.event.OnlineEventDispatcher;

public abstract class OnlineEvent {
    private static String[] A;
    private final ZeusClient W;

    public void u() {
        OnlineEventDispatcher.O.G(this);
    }

    public static void n(String[] stringArray) {
        A = stringArray;
    }

    public static String[] L() {
        return A;
    }

    public OnlineEvent(ZeusClient oZ) {
        this.W = oZ;
    }

    public ZeusClient e() {
        return this.W;
    }

    static {
        if (OnlineEvent.L() == null) {
            OnlineEvent.n(new String[2]);
        }
    }
}


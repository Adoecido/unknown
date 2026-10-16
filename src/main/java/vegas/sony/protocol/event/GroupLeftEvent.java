package vegas.sony.protocol.event;

import vegas.sony.protocol.ZeusClient;
import vegas.sony.protocol.event.OnlineEvent;

public class GroupLeftEvent
extends OnlineEvent {
    private static String l;

    public static String java_lang_String_u() {
        return l;
    }

    public GroupLeftEvent(ZeusClient zeusClient) {
        super(zeusClient);
    }

    public static void t(String string) {
        l = string;
    }

    static {
        if (GroupLeftEvent.java_lang_String_u() != null) {
            GroupLeftEvent.t("mqBJS");
        }
    }
}


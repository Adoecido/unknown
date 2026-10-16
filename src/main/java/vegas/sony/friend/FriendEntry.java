package vegas.sony.friend;

import com.google.gson.JsonObject;
import vegas.sony.SonyVegas;
import vegas.sony.friend.Friend;
import vegas.sony.friend.TargetEntry;
import vegas.sony.friend.TargetType;

public abstract class FriendEntry
extends TargetEntry {
    private boolean targeted = true;
    private static int obfuscationState;

    public static void setFriendEntryObfuscationState(int state) {
        obfuscationState = state;
    }

    public String getAlias() {
        return this.getDisplayName();
    }

    public String getDisplayName() {
        return this.getName();
    }

    public static int getObfuscationConstant() {
        int state = FriendEntry.getFriendEntryObfuscationState();
        return 0;
    }

    public static int getFriendEntryObfuscationState() {
        return obfuscationState;
    }

    static {
        if (FriendEntry.getFriendEntryObfuscationState() == 0) {
            FriendEntry.setFriendEntryObfuscationState(29);
        }
    }

    public FriendEntry() {
        super(TargetType.FRIEND);
    }

    public boolean isPersistent() {
        return true;
    }

    public void setTargeted(boolean targeted) {
        this.targeted = targeted;
        SonyVegas.INSTANCE.getFriendManager().refreshPlayerNames();
    }

    public abstract Friend loadJson(JsonObject json);

    public boolean isTargeted() {
        return this.targeted;
    }

    public abstract JsonObject toJson();

    public abstract String getName();

}


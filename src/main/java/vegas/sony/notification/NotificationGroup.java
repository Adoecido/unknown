package vegas.sony.notification;

public enum NotificationGroup {
    NONE,
    FRIENDS;

    private static final NotificationGroup[] cachedValues;

    static {
        String[] groupNames = new String[]{"FRIENDS", "NONE"};


        cachedValues = new NotificationGroup[]{NONE, FRIENDS};
    }

}

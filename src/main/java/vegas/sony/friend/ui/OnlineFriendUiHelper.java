package vegas.sony.friend.ui;

import vegas.sony.SonyVegas;
import vegas.sony.friend.OnlineFriend;
import vegas.sony.friend.ui.ChatMessagePartyMemberRow;
import vegas.sony.friend.ui.OnlineFriendListEntry;
import vegas.sony.friend.ui.OnlineFriendsFrame;
import vegas.sony.friend.ui.PartyDetailsAndChatPanel;
import vegas.sony.friend.ui.PartyMemberRow;
import vegas.sony.friend.ui.PartyMemberTextStatusComponent;
import vegas.sony.module.none.ClientSettings;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.ui.notification.NotificationMessage;
import vegas.sony.ui.notification.NotificationType;

public final class OnlineFriendUiHelper {
    private static final String MESSAGE_NOTIFICATION_PREFIX;
    private static int obfuscationSeed;

    public static int getObfuscationSeed() {
        return obfuscationSeed;
    }

    public static void showNotification(NotificationType type, String message) {
        OnlineFriendUiHelper.showNotification(new NotificationMessage(type, message));
    }

    public static void setObfuscationSeed(int seed) {
        obfuscationSeed = seed;
    }

    public static void refreshFriendLists() {
        ClientSettings.getFrame(OnlineFriendsFrame.class).getOnlineFriendEntriesPanel().requestRefresh();
        OnlineFriendUiHelper.refreshMinecraftFriends();
    }

    public static OnlineFriendsFrame findOnlineFriendsFrame(GuiComponent component) {
        while (!component.getClass().equals(OnlineFriendsFrame.class)) {
            try {
                component = component.getParentFrameComponent();
            }
            catch (Exception exception) {
                // empty catch block
                break;
            }
        }
        return (OnlineFriendsFrame)component;
    }


    static {
        OnlineFriendUiHelper.setObfuscationSeed(0);
        MESSAGE_NOTIFICATION_PREFIX = "Message from \u00a7f";
    }

    public static int getReservedTwentyOne() {
        int seed = OnlineFriendUiHelper.getObfuscationSeed();
        return 21;
    }

    public static void addPartyChatMessage(PartyMemberRow messageRow) {
        OnlineFriendsFrame frame = ClientSettings.getFrame(OnlineFriendsFrame.class);
        PartyDetailsAndChatPanel detailsPanel = frame.getOnlineFriendsListPanel().getCurrentPartyPanel().getDetailsPanel();
        if (detailsPanel == null) {
            return;
        }
        detailsPanel.getChatPanel().getMessageListPanel().addMessageRow(messageRow);
    }

    public static void showNotification(NotificationMessage notification) {
        ClientSettings.getFrame(OnlineFriendsFrame.class).getNotificationOverlay().C(notification);
    }

    public static void refreshMinecraftFriends() {
        ClientSettings.getFrame(OnlineFriendsFrame.class).getMinecraftFriendEntriesPanel().requestRefresh();
    }

    public static void addFriendChatMessage(OnlineFriend conversationFriend, OnlineFriend sender, String message) {
        OnlineFriendListEntry listEntry = SonyVegas.INSTANCE.getOnlineManager().getFriendCache().getListEntry(conversationFriend.getUser().getId());
        if (listEntry == null) {
            return;
        }
        conversationFriend.setHasChatHistory(true);
        conversationFriend.setUnreadMessage(true);
        if (sender != null) {
            SonyVegas.INSTANCE.getNotificationManager().show(MESSAGE_NOTIFICATION_PREFIX + sender.getDisplayName(), message, vegas.sony.notification.NotificationType.FRIENDS_NEW_CHAT, 4000L);
        }
        OnlineFriend messageSender = sender == null ? SonyVegas.INSTANCE.getOnlineManager().getLocalFriend() : sender;
        ChatMessagePartyMemberRow messageRow = new ChatMessagePartyMemberRow(messageSender, new PartyMemberTextStatusComponent(message));
        listEntry.getFriendCard().getDetailsPanel().getChatPanel().getMessageListPanel().addMessageRow(messageRow);
        listEntry.getFriendCard().getDetailsPanel().getChatPanel().getMessageListPanel().l$src$V$1mibm4x();
        listEntry.getFriendCard().updateDisplayMode();
    }
}


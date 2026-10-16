package vegas.sony.friend.ui;

import vegas.sony.friend.OnlineFriend;
import vegas.sony.friend.OnlineStatus;
import vegas.sony.friend.ui.DirectFriendChatSender;
import vegas.sony.friend.ui.OnlineChatPanel;
import vegas.sony.friend.ui.OnlineFriendActionPanel;
import vegas.sony.friend.ui.OnlineFriendCard;
import vegas.sony.ui.click.component.PanelComponent;

public class OnlineFriendDetailsPanel
extends PanelComponent {
    private final OnlineFriend friend;
    private boolean showingChat;
    private final OnlineChatPanel chatPanel;
    private final OnlineFriendCard friendCard;
    private final OnlineFriendActionPanel actionPanel;

    public OnlineFriendDetailsPanel(OnlineFriendCard onlineFriendCard, OnlineFriend onlineFriend) {
        super(99.0, onlineFriendCard.L());
        this.friend = onlineFriend;
        this.actionPanel = new OnlineFriendActionPanel(onlineFriend);
        this.friendCard = onlineFriendCard;
        this.chatPanel = new OnlineChatPanel(new DirectFriendChatSender(onlineFriend));
        this.setShowDisabledOverlay(false);
        this.addChildren(this.actionPanel);
    }

    public OnlineChatPanel getChatPanel() {
        return this.chatPanel;
    }

    public OnlineFriendActionPanel getActionPanel() {
        return this.actionPanel;
    }

    @Override
    public double C() {
        return this.showingChat ? this.chatPanel.L() : this.actionPanel.L();
    }

    @Override
    public void c() {
        super.c();
        if (this.friend.getStatus() == OnlineStatus.ONLINE) {
            this.chatPanel.getChatInput().setInputEnabled(true);
        } else {
            this.chatPanel.getChatInput().setInputEnabled(false);
        }
    }


    public void showChat() {
        this.showingChat = true;
        this.removeChild(this.actionPanel);
        this.addChildren(this.chatPanel);
        this.H(true);
    }

    public void showActions() {
        this.showingChat = false;
        this.removeChild(this.chatPanel);
        this.addChildren(this.actionPanel);
        this.H(true);
    }
}


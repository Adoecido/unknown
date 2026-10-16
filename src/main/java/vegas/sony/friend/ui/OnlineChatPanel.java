package vegas.sony.friend.ui;

import vegas.sony.friend.ui.OnlineChatInputComponent;
import vegas.sony.friend.ui.OnlineChatSender;
import vegas.sony.friend.ui.PartyMemberListPanel;
import vegas.sony.ui.click.component.PanelComponent;

public class OnlineChatPanel
extends PanelComponent {
    private static final String WRAP_LAYOUT = "wrap";
    private final PartyMemberListPanel messageListPanel = new PartyMemberListPanel(99.0, 80.0);
    private final OnlineChatInputComponent chatInput;

    public OnlineChatInputComponent getChatInput() {
        return this.chatInput;
    }

    @Override
    public void c() {
        super.c();
        double contentHeight = this.messageListPanel.L() + this.chatInput.L();
        this.setExplicitHeight(contentHeight);
        this.Y(contentHeight);
        this.t(contentHeight + 1.0);
        this.setShowDisabledOverlay(false);
    }

    public PartyMemberListPanel getMessageListPanel() {
        return this.messageListPanel;
    }

    public OnlineChatPanel(OnlineChatSender onlineChatSender) {
        super(99.0, 100.0);
        this.chatInput = new OnlineChatInputComponent(this, onlineChatSender);
        this.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().M(WRAP_LAYOUT);
        this.addChildren(this.messageListPanel, this.chatInput);
    }
}

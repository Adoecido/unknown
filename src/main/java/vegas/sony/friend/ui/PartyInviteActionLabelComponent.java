package vegas.sony.friend.ui;

import vegas.sony.friend.ui.PartyInviteFriendRowComponent;
import vegas.sony.ui.click.component.gui.AnimatedCenteredTextLabelComponent;
import java.awt.Color;

public class PartyInviteActionLabelComponent
extends AnimatedCenteredTextLabelComponent {
    private final PartyInviteFriendRowComponent row;

    @Override
    public double C() {
        return 8.0;
    }

    @Override
    public double x() {
        return this.getDefaultFontRenderer().N(this.getText()) + 3.0;
    }

    public PartyInviteActionLabelComponent(PartyInviteFriendRowComponent partyInviteFriendRowComponent, String string, Color color) {
        super(string, color);
        this.row = partyInviteFriendRowComponent;
    }
}

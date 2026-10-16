package vegas.sony.friend.ui;

import vegas.sony.friend.ui.PartyFriendRowComponent;
import vegas.sony.ui.click.component.gui.AnimatedCenteredTextLabelComponent;
import java.awt.Color;

public class PartyFriendCompactActionLabelComponent
extends AnimatedCenteredTextLabelComponent {
    private final PartyFriendRowComponent row;

    @Override
    public double C() {
        return 8.0;
    }

    public PartyFriendCompactActionLabelComponent(PartyFriendRowComponent partyFriendRowComponent, String string, Color color) {
        super(string, color);
        this.row = partyFriendRowComponent;
    }

    @Override
    public double x() {
        return this.getDefaultFontRenderer().N(this.getText());
    }
}

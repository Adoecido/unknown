package vegas.sony.friend.ui;

import vegas.sony.friend.ui.PartyFriendRowComponent;
import vegas.sony.ui.click.component.TruncatedTextComponent;
import java.awt.Color;

public class PartyFriendNameLabelComponent
extends TruncatedTextComponent {
    private final PartyFriendRowComponent row;

    @Override
    public double x() {
        return 68.0;
    }

    public PartyFriendNameLabelComponent(PartyFriendRowComponent partyFriendRowComponent, String string, String string2, double d, double d2, Color color, boolean bl) {
        super(string, string2, d, d2, color, bl);
        this.row = partyFriendRowComponent;
    }
}

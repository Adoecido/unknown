package vegas.sony.friend.ui;

import vegas.sony.friend.ui.OnlineFriendCard;
import vegas.sony.ui.click.GuiMouseListener;
import vegas.sony.ui.click.MouseClickButton;
import java.awt.Point;

class OnlineFriendCardToggleDetailsMouseListener
implements GuiMouseListener {
    private final OnlineFriendCard card;

    @Override
    public void g(Point point, MouseClickButton mouseClickButton) {
        OnlineFriendCard.handleCardClick(this.card, mouseClickButton);
    }

    OnlineFriendCardToggleDetailsMouseListener(OnlineFriendCard onlineFriendCard) {
        this.card = onlineFriendCard;
    }
}

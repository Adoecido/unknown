package vegas.sony.friend.ui;

import vegas.sony.friend.ui.OnlineFriendCard;
import vegas.sony.ui.click.component.FlowLayoutComponent;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.ui.click.component.SpacerComponent;

class OnlineFriendCardVisibleChildrenHeightComponent
extends FlowLayoutComponent {
    final OnlineFriendCard onlineFriendCard;


    OnlineFriendCardVisibleChildrenHeightComponent(OnlineFriendCard onlineFriendCard, double width) {
        super(width);
        this.onlineFriendCard = onlineFriendCard;
    }

    @Override
    public double getVisibleChildrenWidth() {
        double totalWidth = 0.0;
        if (!OnlineFriendCard.getCompactActions(this.onlineFriendCard).V$src$Z$1xhop3l()) {
            return totalWidth;
        }
        for (GuiComponent child : OnlineFriendCard.getCompactActions(this.onlineFriendCard).f()) {
            if (child instanceof SpacerComponent || !child.V$src$Z$1xhop3l()) continue;
            totalWidth += child.A();
        }
        return totalWidth + 2.0;
    }
}

package vegas.sony.friend.ui;

import vegas.sony.SonyVegas;
import vegas.sony.friend.OnlineFriend;
import vegas.sony.friend.ui.OnlineFriendListEntry;
import vegas.sony.ui.click.GuiMouseEvent;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.ui.click.component.PanelComponent;
import vegas.sony.ui.click.component.SpacerComponent;
import vegas.sony.ui.click.frame.FrameScrollbarPlacement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class OnlineFriendEntriesPanel
extends PanelComponent {
    private static final String WRAP_LAYOUT = "wrap";
    private double contentHeight;
    private boolean refreshPending = true;

    @Override
    public void v() {
    }

    public OnlineFriendEntriesPanel() {
        super(100.0, 20.0);
        this.setShowDisabledOverlay(false);
        this.F(FrameScrollbarPlacement.OUTSIDE);
        this.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().M(WRAP_LAYOUT);
        this.t(90.0);
    }

    public void refreshList() {
        this.removeMarkedChildren();
        this.h(new SpacerComponent(1.0, 1.0), new Object[0]);
        ArrayList<OnlineFriend> friends = new ArrayList<OnlineFriend>(SonyVegas.INSTANCE.getOnlineFriendManager().getFriends());
        friends.sort(Comparator.comparingInt(OnlineFriendEntriesPanel::getStatusSortOrder).thenComparing(OnlineFriend::getDisplayName));
        for (OnlineFriend friend : friends) {
            OnlineFriendListEntry listEntry = SonyVegas.INSTANCE.getOnlineManager().getFriendCache().getOrCreateListEntry(friend, () -> OnlineFriendEntriesPanel.createListEntry(friend));
            this.h(listEntry, new Object[0]);
        }
    }

    private static OnlineFriendListEntry createListEntry(OnlineFriend friend) {
        return new OnlineFriendListEntry(friend);
    }

    private static int getStatusSortOrder(OnlineFriend friend) {
        return friend.getStatus().ordinal();
    }

    @Override
    public void Y() {
    }

    @Override
    public void c() {
        super.c();
        if (this.refreshPending) {
            this.refreshList();
            this.refreshPending = false;
        }
    }


    @Override
    public void V() {
        double visibleHeight = 0.0;
        List<GuiComponent> children = this.f();
        for (GuiComponent child : children) {
            if (!child.V$src$Z$1xhop3l()) continue;
            visibleHeight += child.L();
        }
        this.contentHeight = visibleHeight;
        this.setExplicitHeight(this.contentHeight);
    }

    @Override
    public void g(GuiMouseEvent guiMouseEvent) {
    }

    @Override
    public double x() {
        return 100.0;
    }

    @Override
    public double C() {
        return 20.0;
    }

    public void requestRefresh() {
        this.refreshPending = true;
    }
}


package vegas.sony.friend.ui;

import vegas.sony.SonyVegas;
import vegas.sony.friend.ExternalFriend;
import vegas.sony.friend.FriendEntry;
import vegas.sony.friend.OnlineStatus;
import vegas.sony.friend.ui.FriendListEntryRow;
import vegas.sony.manager.client.OnlineConnectionManager;
import vegas.sony.ui.click.GuiMouseEvent;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.ui.click.component.PanelComponent;
import vegas.sony.ui.click.frame.FrameScrollbarPlacement;
import java.util.List;

public class FriendEntriesPanel
extends PanelComponent {
    private static final String LAYOUT_MODE;
    private double contentHeight;
    private static boolean obfuscationFlag;
    private boolean refreshPending;

    @Override
    public double x() {
        return 0.0;
    }

    public FriendEntriesPanel() {
        super(99.0, 110.0);
        this.F(FrameScrollbarPlacement.INSIDE);
        this.setShowDisabledOverlay(false);
        this.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().M(LAYOUT_MODE);
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
    public void g(GuiMouseEvent guiMouseEvent) {
    }

    public void removeEntry(FriendListEntryRow friendListEntryRow) {
        this.removeChild(friendListEntryRow);
    }

    private void refreshList() {
        this.removeMarkedChildren();
        for (FriendEntry friendEntry : SonyVegas.INSTANCE.getFriendManager().getFriends()) {
            if (friendEntry instanceof ExternalFriend && (((ExternalFriend)friendEntry).getOnlineFriend().getStatus() == OnlineStatus.OFFLINE || OnlineConnectionManager.INSTANCE.isManualDisconnectRequested())) continue;
            FriendListEntryRow entryRow = new FriendListEntryRow(friendEntry);
            this.h(entryRow, new Object[0]);
        }
    }


    @Override
    public void V() {
        double d = 0.0;
        List<GuiComponent> list = this.f();
        for (GuiComponent guiComponent : list) {
            if (!guiComponent.V$src$Z$1xhop3l()) continue;
            d += guiComponent.L();
        }
        this.contentHeight = d;
        this.t(112.0);
    }

    public static boolean isObfuscationFlagSet() {
        return obfuscationFlag;
    }

    @Override
    public void Y() {
    }

    @Override
    public void v() {
    }

    public void addEntry(FriendListEntryRow friendListEntryRow) {
        this.h(friendListEntryRow, new Object[0]);
    }

    @Override
    public double C() {
        return this.contentHeight;
    }

    public void requestRefresh() {
        this.refreshPending = true;
    }

    public static void setObfuscationFlag(boolean flag) {
        obfuscationFlag = flag;
    }

    public static boolean getObfuscationConstant() {
        boolean flag = FriendEntriesPanel.isObfuscationFlagSet();
        return false;
    }

    static {
        FriendEntriesPanel.setObfuscationFlag(true);
        LAYOUT_MODE = "wrap";
    }
}


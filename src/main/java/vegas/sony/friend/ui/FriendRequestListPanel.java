package vegas.sony.friend.ui;

import vegas.sony.friend.FriendRequest;
import vegas.sony.friend.IncomingFriendRequest;
import vegas.sony.friend.OutgoingFriendRequest;
import vegas.sony.friend.ui.FriendRequestRow;
import vegas.sony.friend.ui.FriendRequestRowsPanel;
import vegas.sony.friend.ui.IncomingFriendRequestRow;
import vegas.sony.friend.ui.OutgoingFriendRequestRow;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.ui.click.component.PanelComponent;
import vegas.sony.ui.click.component.SpacerComponent;
import vegas.sony.ui.font.SmoothFontRenderer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

public class FriendRequestListPanel
extends PanelComponent {
    private HashMap<FriendRequest, FriendRequestRow> rowsByRequest;
    private final PanelComponent headerPanel = new PanelComponent(100.0, 8.0);
    private FriendRequestRowsPanel rowsPanel;
    private final PanelComponent bodyPanel = new PanelComponent(100.0, 90.0);


    public void refreshList() {
        ArrayList<FriendRequestRow> sortedRows = new ArrayList<FriendRequestRow>(this.rowsByRequest.values());
        sortedRows.sort(Comparator.comparing(FriendRequestListPanel::getRequestDisplayName));
        ArrayList<FriendRequestRow> orderedRows = new ArrayList<FriendRequestRow>();
        for (FriendRequestRow friendRequestRow : sortedRows) {
            if (!(friendRequestRow.getRequest() instanceof IncomingFriendRequest)) continue;
            orderedRows.add(friendRequestRow);
        }
        for (FriendRequestRow friendRequestRow : sortedRows) {
            if (!(friendRequestRow.getRequest() instanceof OutgoingFriendRequest)) continue;
            orderedRows.add(friendRequestRow);
        }
        this.rowsPanel.removeMarkedChildren();
        this.rowsPanel.addChildren(orderedRows.toArray(new GuiComponent[0]));
    }

    @Override
    public double C() {
        return this.headerPanel.L() + (this.rowsPanel.d$src$D$ibccpu() + 2.0);
    }

    public void removeRequest(FriendRequest friendRequest) {
        FriendRequestRow friendRequestRow = this.rowsByRequest.remove(friendRequest);
        if (friendRequestRow == null) {
            return;
        }
        this.refreshList();
    }

    public HashMap<FriendRequest, FriendRequestRow> getRowsByRequest() {
        return this.rowsByRequest;
    }

    public void addRequest(FriendRequest friendRequest) {
        FriendRequestRow friendRequestRow = friendRequest instanceof IncomingFriendRequest ? new IncomingFriendRequestRow((IncomingFriendRequest)friendRequest) : new OutgoingFriendRequestRow((OutgoingFriendRequest)friendRequest);
        this.rowsByRequest.put(friendRequest, friendRequestRow);
        this.refreshList();
    }

    private static String getRequestDisplayName(FriendRequestRow row) {
        return row.getRequest().getFriend().getDisplayName();
    }

    public FriendRequestListPanel() {
        super(96.0, 16.0);
        this.rowsByRequest = new HashMap();
        this.rowsPanel = new FriendRequestRowsPanel();
        this.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().M("wrap");
        this.setShowDisabledOverlay(false);
        this.headerPanel.setShowDisabledOverlay(false);
        this.headerPanel.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().Q(true);
        this.bodyPanel.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().M("wrap");
        this.bodyPanel.addChildren(new SpacerComponent(1.0, 2.0), this.rowsPanel);
        this.addChildren(this.headerPanel, this.bodyPanel);
    }

    @Override
    public double x() {
        return 100.0;
    }

    @Override
    public void c() {
        super.c();
        SmoothFontRenderer smoothFontRenderer = this.getAlternateFontRenderer(0.7);
        String requestCountText = this.rowsPanel.getRowCount() + " ";
        String headingText = "PENDING REQUESTS";
        double countWidth = smoothFontRenderer.N(requestCountText) + 1.0;
        double textOffsetY = (this.headerPanel.L() - smoothFontRenderer.d(headingText)) / 2.0;
        smoothFontRenderer.d(requestCountText, this.headerPanel.G$src$D$1b2f02a() + 1.0, this.headerPanel.n() + textOffsetY, FriendRequestListPanel.J.A);
        smoothFontRenderer.d(headingText, this.headerPanel.G$src$D$1b2f02a() + countWidth + 1.0, this.headerPanel.n() + textOffsetY, FriendRequestListPanel.J.h);
    }
}


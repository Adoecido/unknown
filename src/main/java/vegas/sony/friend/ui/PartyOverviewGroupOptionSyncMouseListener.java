package vegas.sony.friend.ui;

import vegas.sony.friend.ui.PartyDetailsPanel;
import vegas.sony.protocol.ZeusConnectionManager;
import vegas.sony.protocol.packet.GroupOption;
import vegas.sony.ui.click.GuiMouseListener;
import vegas.sony.ui.click.MouseClickButton;
import vegas.sony.value.BooleanValue;
import vegas.sony.value.Value;
import java.awt.Point;

public class PartyOverviewGroupOptionSyncMouseListener
implements GuiMouseListener {
    private final GroupOption option;
    private boolean previousValue;
    private final PartyDetailsPanel detailsPanel;
    private final BooleanValue booleanValue;
    private final Value value;

    public PartyOverviewGroupOptionSyncMouseListener(PartyDetailsPanel partyDetailsPanel, BooleanValue booleanValue, GroupOption groupOption, Value value) {
        this.detailsPanel = partyDetailsPanel;
        this.booleanValue = booleanValue;
        this.option = groupOption;
        this.value = value;
        this.previousValue = this.booleanValue.getEffectiveValue();
    }

    @Override
    public void g(Point point, MouseClickButton mouseClickButton) {
        if (this.previousValue != this.booleanValue.getEffectiveValue()) {
            this.previousValue = this.booleanValue.getEffectiveValue();
            ZeusConnectionManager.T().u().Y(this.option, this.value.getValue());
        }
    }

}


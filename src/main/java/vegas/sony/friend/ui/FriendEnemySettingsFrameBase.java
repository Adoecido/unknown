package vegas.sony.friend.ui;

import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.ui.click.frame.CollapsibleFrame;
import vegas.sony.ui.click.frame.OutlinedFrameBase;
import vegas.sony.ui.click.frame.ToggleableFrameHeaderComponent;

public abstract class FriendEnemySettingsFrameBase
extends OutlinedFrameBase
implements CollapsibleFrame {
    protected ToggleableFrameHeaderComponent header;
    private static final String LAYOUT_MODE = "wrap";
    private boolean collapsed = false;

    @Override
    public void w() {
        this.collapsed = !this.collapsed;
        for (GuiComponent guiComponent : this.f()) {
            if (!guiComponent.isRemovable()) continue;
            guiComponent.setVisible(!this.collapsed);
        }
        if (this.collapsed) {
            this.header.L(false);
        }
        this.l$src$V$1mibm4x();
    }


    @Override
    public boolean q() {
        return this.collapsed;
    }

    public FriendEnemySettingsFrameBase(String string, String string2, double d) {
        this.setDisabledOverlayColor(FriendEnemySettingsFrameBase.J.i);
        this.K(300.0);
        this.S(100.0);
        this.setVisible(false);
        this.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().M(false);
        this.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().M(LAYOUT_MODE);
        this.header = new ToggleableFrameHeaderComponent(this, string, string2, d);
        this.Y(this.header);
    }

    public ToggleableFrameHeaderComponent getHeader() {
        return this.header;
    }

    public void addHeaderChildren(GuiComponent ... guiComponentArray) {
        this.header.q(guiComponentArray);
    }

    public FriendEnemySettingsFrameBase(String string, String string2) {
        this(string, string2, 1.0);
    }
}


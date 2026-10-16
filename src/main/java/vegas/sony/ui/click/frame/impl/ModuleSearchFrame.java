package vegas.sony.ui.click.frame.impl;

import vegas.sony.SonyVegas;
import vegas.sony.module.Category;
import vegas.sony.module.Mod;
import vegas.sony.module.none.ClientSettings;
import vegas.sony.ui.click.component.module.ModuleComponent;
import vegas.sony.ui.click.frame.impl.ModuleCategoryFrame;
import vegas.sony.ui.click.frame.impl.ModuleSearchFrameHeader;
import vegas.sony.unmap.ModeSelection;
import vegas.sony.wrapper.impl.Minecraft;

public class ModuleSearchFrame
extends ModuleCategoryFrame {
    private ModuleSearchFrameHeader Rt;

    public void D(String string) {
        this.removeMarkedChildren();
        ClientSettings.activeTooltips = null;
        if (string == null || string.length() < 1) {
            return;
        }
        for (Mod mod : SonyVegas.INSTANCE.getModManager().collectMods()) {
            if (mod.getCategory().equals(Category.NONE)) continue;
            if (!mod.matchesSearch(string, false)) continue;
            ModuleComponent moduleComponent = new ModuleComponent(this, mod);
            this.h(moduleComponent, new Object[0]);
            moduleComponent.buildValueComponents();
        }
    }

    @Override
    public boolean V$src$Z$1xhop3l() {
        return ((ModeSelection)ClientSettings.INSTANCE.searchBarStyle.getValue()).equals(ClientSettings.INSTANCE.floatingSearchBarMode);
    }

    @Override
    public void w() {
    }

    @Override
    public String getName() {
        return "ModuleSearch";
    }

    public ModuleSearchFrame() {
        super(Category.NONE);
        this.setDisabledOverlayColor(ModuleSearchFrame.J.r);
        this.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().M(false);
        this.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().M("wrap");
        this.I2 = false;
        this.Rt = new ModuleSearchFrameHeader(this);
        this.Y(this.Rt);
    }

    public ModuleSearchFrameHeader n$src$Lvegas_sony_ui_click_frame_impl_ModuleSearchFrameHe$xia8v2() {
        return this.Rt;
    }

    public void p() {
        this.M((double)Minecraft.J() / (4.0 * SonyVegas.INSTANCE.getClientSettings().getGuiScaleFactor()) - this.A() / 2.0, 7.0);
    }


    @Override
    public void v() {
    }

    @Override
    public void Y() {
        if (this.n() != 7.0) {
            this.p();
        }
    }

    @Override
    public void V() {
    }
}


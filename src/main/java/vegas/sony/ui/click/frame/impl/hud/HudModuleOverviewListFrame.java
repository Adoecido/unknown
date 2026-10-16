package vegas.sony.ui.click.frame.impl.hud;

import vegas.sony.SonyVegas;
import vegas.sony.module.Mod;
import vegas.sony.module.none.ClientSettings;
import vegas.sony.module.render.hud.HudModule;
import vegas.sony.ui.click.frame.Frame;
import vegas.sony.ui.font.SmoothFontRenderer;
import vegas.sony.utils.NameComparator;
import vegas.sony.utils.render.GuiRenderPrimitives;
import java.util.ArrayList;

public class HudModuleOverviewListFrame
extends Frame {
    @Override
    public void v() {
    }

    @Override
    public void Y() {
    }

    public void refreshFavorites() {
        this.removeMarkedChildren();
        int moduleIndex = 0;
        ArrayList<Mod> mods = new ArrayList<Mod>(SonyVegas.INSTANCE.getModManager().collectMods());
        mods.sort(new NameComparator());
        for (Mod mod : mods) {
            HudModule hudModule;
            if (!(mod instanceof HudModule) || !(hudModule = (HudModule)mod).isFavorite()) continue;
            HudModuleToggleComponent hudModuleToggleComponent = new HudModuleToggleComponent(hudModule);
            if (hudModule.getConfigFrameClass() != null) {
                hudModuleToggleComponent.setConfigFrame((Frame)ClientSettings.getFrame(hudModule.getConfigFrameClass()));
            }
            hudModuleToggleComponent.applyConfigFrameState();
            this.h(hudModuleToggleComponent, moduleIndex > 0 && (moduleIndex + 1) % 5 == 0 ? "wrap" : "");
            ++moduleIndex;
        }
    }

    public HudModuleOverviewListFrame() {
        this.I2 = false;
        this.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().M(false);
        this.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().t(false);
        this.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().I(false);
        this.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().U(false);
        this.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().u(false);
        this.setVisible(false);
        this.L(false, false);
        this.t(50.0);
        this.refreshFavorites();
    }

    @Override
    public String getName() {
        return "LegitMinModuleFrame";
    }

    @Override
    public void t(boolean bl, boolean bl2) {
        super.t(bl, bl2);
    }


    private boolean hasFavoriteModule() {
        for (Mod mod : SonyVegas.INSTANCE.getModManager().collectMods()) {
            HudModule hudModule;
            if (!(mod instanceof HudModule) || !(hudModule = (HudModule)mod).isFavorite()) continue;
            return true;
        }
        return false;
    }

    @Override
    public void c() {
        super.c();
        if (!this.hasFavoriteModule()) {
            SmoothFontRenderer smoothFontRenderer = this.getFontRenderer(0.7);
            GuiRenderPrimitives.F("empty", this.G$src$D$1b2f02a() + this.A() / 2.0 - 2.0, this.n() + this.L() / 2.0 - 10.0, 9.6, 9.6, HudModuleOverviewListFrame.J.A);
            smoothFontRenderer.d("No Favorites", this.G$src$D$1b2f02a() + this.A() / 2.0 - smoothFontRenderer.N("No Favorites") / 2.0, this.n() + this.L() / 2.0 + 3.0, HudModuleOverviewListFrame.J.h);
        }
    }
}


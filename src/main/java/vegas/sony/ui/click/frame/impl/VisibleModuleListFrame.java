package vegas.sony.ui.click.frame.impl;

import vegas.sony.SonyVegas;
import vegas.sony.module.Category;
import vegas.sony.module.Mod;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.ui.click.component.module.ModuleComponent;
import vegas.sony.ui.click.frame.impl.ModuleCategoryFrame;
import vegas.sony.ui.click.frame.impl.ModuleCategoryFrameHeader;
import java.util.concurrent.CopyOnWriteArrayList;

public class VisibleModuleListFrame
extends ModuleCategoryFrame {
    private static VisibleModuleListFrame U2;


    public VisibleModuleListFrame() {
        super(Category.FAVORITES);
        U2 = this;
    }

    @Override
    public void s$src$V$1a2f6mi() {
        this.HF = new ModuleCategoryFrameHeader(this, "newfavorites", "Favorites");
        this.Y(this.HF);
    }

    public static VisibleModuleListFrame U$src$Lvegas_sony_ui_click_frame_impl_VisibleModuleListFr$168oq2s() {
        return U2;
    }

    @Override
    public void v() {
    }

    public static void e() {
        // Profiles can be applied before the Badlion GUI frames are created.
        // Defer the visual refresh until the frame instance exists.
        if (U2 == null) {
            return;
        }
        U2.removeMarkedChildren();
        try {
            CopyOnWriteArrayList<Mod> copyOnWriteArrayList = new CopyOnWriteArrayList<Mod>(SonyVegas.INSTANCE.getModuleProfileMetadataCodec().getSelectedModules());
            for (Mod mod : copyOnWriteArrayList) {
                ModuleComponent moduleComponent = new ModuleComponent(U2, mod);
                U2.h(moduleComponent, new Object[0]);
                moduleComponent.buildValueComponents();
            }
        }
        catch (Exception exception) {
            SonyVegas.logThrowable(exception);
        }
    }

    @Override
    public void Y() {
    }

    public static ModuleComponent A(Mod mod) {
        for (GuiComponent guiComponent : U2.f()) {
            if (!(guiComponent instanceof ModuleComponent) || !((ModuleComponent)guiComponent).getModule().equals(mod)) continue;
            return (ModuleComponent)guiComponent;
        }
        return null;
    }
}


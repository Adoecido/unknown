package vegas.sony.module.render.hud;

import vegas.sony.event.EventHandler;
import vegas.sony.event.impl.EventGuiOpen;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.module.render.hud.HudModule;
import vegas.sony.module.render.hud.HudModuleGroup;
import vegas.sony.render.ShaderGroupRenderStateManager;

public class InventoryBlurHudModule
extends HudModule {
    public InventoryBlurHudModule() {
        super("Inventory Blur //$ui", HudModuleGroup.GAME, "inventory_blur //$");
        this.setSuffix("Blurs the background while in an inventory //$ui");
    }


    @EventHandler
    public void onGuiOpen(EventGuiOpen event) {
        if (event.getGuiScreen().isNull() || event.getGuiScreen().isInstance(MappedClasses.qo) || event.getGuiScreen().isInstance(MappedClasses.Fl) || MappedClasses.zL != null && event.getGuiScreen().isInstance(MappedClasses.zL)) {
            ShaderGroupRenderStateManager.getInstance().disable();
            return;
        }
        ShaderGroupRenderStateManager.getInstance().enable();
    }
}


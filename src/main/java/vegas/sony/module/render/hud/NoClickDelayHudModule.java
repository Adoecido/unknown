package vegas.sony.module.render.hud;

import vegas.sony.config.ClientSettings;
import vegas.sony.event.EventHandler;
import vegas.sony.event.impl.EventPreTick;
import vegas.sony.module.render.hud.HudModule;
import vegas.sony.module.render.hud.HudModuleGroup;
import vegas.sony.wrapper.impl.ForgeVersion;
import vegas.sony.wrapper.impl.Minecraft;

public class NoClickDelayHudModule
extends HudModule {
    public NoClickDelayHudModule() {
        super("NoClickDelay //$ui", HudModuleGroup.GAME, "no_click_delay2 //$");
        this.setSuffix("Removes the click delay that normally occurs after missing an attack //$ui");
    }

    @EventHandler
    public void onTick(EventPreTick event) {
        if (ForgeVersion.MC_1_8_9.d() && ClientSettings.isAttackButtonDown() && Minecraft.currentScreen().isNull()) {
            Minecraft.r(0);
        }
    }

}


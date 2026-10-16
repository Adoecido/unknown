package vegas.sony.module.rise.wtap;

import vegas.sony.event.EventHandler;
import vegas.sony.event.impl.EventPreAttack;
import vegas.sony.event.impl.EventPreMotion;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.module.Mod;
import vegas.sony.module.SubModule;
import vegas.sony.module.blatant.invwalk.InvWalkSettingsState;
import vegas.sony.module.rise.RiseWTap;
import vegas.sony.wrapper.impl.EntityPlayerSP;
import vegas.sony.wrapper.impl.KeyBinding;
import vegas.sony.wrapper.impl.Minecraft;

public class RiseWTapLegitMode extends SubModule<RiseWTap> {
    private boolean unsprint;
    private boolean wTap;

    public RiseWTapLegitMode(Mod parent, String name) {
        super(parent, name);
    }

    @EventHandler
    public void onAttack(EventPreAttack event) {
        if (event.isCanceled() || !event.getTarget().isInstance(MappedClasses.zm)) {
            return;
        }
        this.wTap = this.getParent().rollChance();
        if (!this.wTap || this.unsprint) {
            return;
        }
        EntityPlayerSP player = Minecraft.thePlayer();
        KeyBinding sprintKey = Minecraft.gameSettings().r();
        if (player.B$src$Z$f90iek() || InvWalkSettingsState.isPhysicallyPressed(sprintKey)) {
            InvWalkSettingsState.setPressed(sprintKey, false);
            this.unsprint = true;
        }
    }

    @EventHandler
    public void onPreMotion(EventPreMotion event) {
        if (this.wTap && this.unsprint) {
            InvWalkSettingsState.synchronizeKey(Minecraft.gameSettings().r());
            this.unsprint = false;
            this.wTap = false;
        }
    }

    @Override
    public void onDisable() {
        if (this.unsprint) {
            InvWalkSettingsState.synchronizeKey(Minecraft.gameSettings().r());
        }
        this.unsprint = false;
        this.wTap = false;
    }
}

package vegas.sony.module.rise.invmove;

import vegas.sony.event.EventHandler;
import vegas.sony.event.impl.EventPreEntityUpdate;
import vegas.sony.module.Mod;
import vegas.sony.module.SubModule;
import vegas.sony.module.blatant.invwalk.InvWalkSettingsState;
import vegas.sony.module.rise.RiseInvMove;
import vegas.sony.wrapper.impl.GameSettings;
import vegas.sony.wrapper.impl.Minecraft;

abstract class RiseInvMoveModeBase extends SubModule<RiseInvMove> {
    private boolean bindsHeld;

    RiseInvMoveModeBase(Mod parent, String name) {
        super(parent, name);
    }

    @EventHandler
    public void onPreEntityUpdate(EventPreEntityUpdate event) {
        if (!event.getEntity().equals(Minecraft.thePlayer())) {
            return;
        }
        if (!this.getParent().shouldHandleCurrentScreen()) {
            if (this.bindsHeld) {
                this.getParent().synchronizeMovementKeys();
                this.bindsHeld = false;
            }
            return;
        }
        this.bindsHeld = true;
        GameSettings settings = Minecraft.gameSettings();
        InvWalkSettingsState.synchronizeKey(settings.Y());
        InvWalkSettingsState.synchronizeKey(settings.s());
        InvWalkSettingsState.synchronizeKey(settings.x$src$Lvegas_sony_wrapper_impl_KeyBinding_$1cf7isg());
        InvWalkSettingsState.synchronizeKey(settings.g$src$Lvegas_sony_wrapper_impl_KeyBinding_$qqn5n3());
        InvWalkSettingsState.synchronizeKey(settings.O());
    }

    @Override
    public void onDisable() {
        this.getParent().synchronizeMovementKeys();
        this.bindsHeld = false;
    }
}

package vegas.sony.module.rise.invmove;

import vegas.sony.event.EventHandler;
import vegas.sony.event.impl.EventPreEntityUpdate;
import vegas.sony.event.impl.EventSetSprinting;
import vegas.sony.module.Mod;
import vegas.sony.wrapper.impl.EntityPlayerSP;
import vegas.sony.wrapper.impl.Minecraft;

public final class RiseInvMoveGrim2Mode extends RiseInvMoveModeBase {
    public RiseInvMoveGrim2Mode(Mod parent, String name) {
        super(parent, name);
    }

    @EventHandler
    public void onPreEntityUpdateSprint(EventPreEntityUpdate event) {
        EntityPlayerSP player = Minecraft.thePlayer();
        if (event.getEntity().equals(player) && RiseInvMoveGrimMode.isInventoryScreen()
                && player.B$src$Z$f90iek()) {
            player.R(false);
        }
    }

    @EventHandler
    public void onSetSprinting(EventSetSprinting event) {
        if (!event.isNewStateSprinting() || !event.getEntity().equals(Minecraft.thePlayer())
                || !RiseInvMoveGrimMode.isInventoryScreen()) {
            return;
        }
        event.setCancelled(true);
        EntityPlayerSP player = Minecraft.thePlayer();
        if (player.B$src$Z$f90iek()) {
            player.R(false);
        }
    }
}

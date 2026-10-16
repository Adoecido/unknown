package vegas.sony.module.rise.invmove;

import vegas.sony.event.EventHandler;
import vegas.sony.event.impl.EventPreEntityUpdate;
import vegas.sony.event.impl.EventSetSprinting;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.module.Mod;
import vegas.sony.value.NumberValue;
import vegas.sony.wrapper.impl.EntityPlayerSP;
import vegas.sony.wrapper.impl.Minecraft;

public final class RiseInvMoveGrimMode extends RiseInvMoveModeBase {
    private final NumberValue managerExtraSprintTicks = NumberValue.create(this,
            "Manager Extra Sprint Ticks //$ui", "#", "", 0.0, 9.0, 20.0, 1.0);

    public RiseInvMoveGrimMode(Mod parent, String name) {
        super(parent, name);
        this.managerExtraSprintTicks.setMaximumFractionDigits(0);
        this.addValue(this.managerExtraSprintTicks);
    }

    @EventHandler
    public void onPreEntityUpdateSprint(EventPreEntityUpdate event) {
        EntityPlayerSP player = Minecraft.thePlayer();
        if (event.getEntity().equals(player) && isInventoryScreen()
                && player.B$src$Z$f90iek()) {
            player.R(false);
        }
    }

    @EventHandler
    public void onSetSprinting(EventSetSprinting event) {
        if (!event.isNewStateSprinting() || !event.getEntity().equals(Minecraft.thePlayer())
                || !isInventoryScreen()) {
            return;
        }
        event.setCancelled(true);
        EntityPlayerSP player = Minecraft.thePlayer();
        if (player.B$src$Z$f90iek()) {
            player.R(false);
        }
    }

    public int getManagerExtraSprintTicks() {
        return this.managerExtraSprintTicks.getValue().intValue();
    }

    static boolean isInventoryScreen() {
        return Minecraft.currentScreen().isInstance(MappedClasses.YS)
                || Minecraft.currentScreen().isInstance(MappedClasses.qs);
    }
}

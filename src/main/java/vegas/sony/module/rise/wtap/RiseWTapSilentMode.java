package vegas.sony.module.rise.wtap;

import vegas.sony.event.EventHandler;
import vegas.sony.event.impl.EventPreAttack;
import vegas.sony.event.impl.EventPreMotion;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.module.Mod;
import vegas.sony.module.SubModule;
import vegas.sony.module.rise.RiseWTap;
import vegas.sony.wrapper.impl.CPacketEntityAction;
import vegas.sony.wrapper.impl.EntityPlayerSP;
import vegas.sony.wrapper.impl.Minecraft;

public class RiseWTapSilentMode extends SubModule<RiseWTap> {
    private boolean restoreSprint;

    public RiseWTapSilentMode(Mod parent, String name) {
        super(parent, name);
    }

    @EventHandler
    public void onAttack(EventPreAttack event) {
        if (event.isCanceled() || !event.getTarget().isInstance(MappedClasses.zm)
                || !this.getParent().rollChance()) {
            return;
        }
        EntityPlayerSP player = Minecraft.thePlayer();
        if (player.isNull() || (!player.B$src$Z$f90iek()
                && !Minecraft.gameSettings().r().isKeyDown())) {
            return;
        }
        player.sendQueue().addToSendQueue(CPacketEntityAction.create(player, 4));
        this.restoreSprint = true;
    }

    @EventHandler
    public void onPreMotion(EventPreMotion event) {
        EntityPlayerSP player = Minecraft.thePlayer();
        if (player.isNull()) {
            this.reset();
            return;
        }
        if (this.restoreSprint) {
            player.sendQueue().addToSendQueue(CPacketEntityAction.create(player, 3));
            this.restoreSprint = false;
        }
    }

    private void reset() {
        this.restoreSprint = false;
    }

    @Override
    public void onDisable() {
        this.reset();
    }
}

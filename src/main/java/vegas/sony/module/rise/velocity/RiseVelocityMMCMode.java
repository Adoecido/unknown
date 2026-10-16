package vegas.sony.module.rise.velocity;

import vegas.sony.event.EventHandler;
import vegas.sony.event.impl.EventPacketReceive;
import vegas.sony.event.impl.EventPreMotion;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.module.Mod;
import vegas.sony.module.SubModule;
import vegas.sony.module.rise.RiseVelocity;
import vegas.sony.wrapper.impl.EntityPlayerSP;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.MovementInput;
import vegas.sony.wrapper.impl.Packet;
import vegas.sony.wrapper.impl.SPacketEntityVelocity;

public final class RiseVelocityMMCMode extends SubModule<RiseVelocity> {
    // Present in Rise 6.9.5 as well. Its packet listener writes this flag, but the
    // original MMC mode never reads it.
    private boolean receivedVelocity;

    public RiseVelocityMMCMode(Mod parent, String name) {
        super(parent, name);
    }

    @EventHandler
    public void onPreMotion(EventPreMotion event) {
        EntityPlayerSP player = Minecraft.thePlayer();
        if (player.isNull() || !this.getParent().canRunForSwing(player)) {
            return;
        }
        int velocityTicks = this.getParent().getVelocityTicks();
        if (velocityTicks == 1) {
            // C(double) is lastTickPosX. Using it here made the renderer
            // interpolate from X=0 and looked like a one-frame camera teleport.
            player.setMotionX(0.0);
            player.setMotionZ(0.0);
            if (player.e$src$Z$15bd4i1()) {
                player.setMotionY(player.q() - 9.0);
            }
        }
        double horizontalSpeed = Math.sqrt(player.t() * player.t() + player.T() * player.T());
        if (this.getParent().getOffGroundTicks() == 1 && velocityTicks < 4
                && horizontalSpeed < 0.31) {
            MovementInput input = player.movementInput();
            player.i(input.T(), input.D(), 0.05f);
        }
    }

    @EventHandler
    public void onPacketReceive(EventPacketReceive event) {
        EntityPlayerSP player = Minecraft.thePlayer();
        if (player.isNull()) {
            return;
        }
        Packet.n(event.getPacket(), packet -> {
            if (packet.isInstance(MappedClasses.YX)
                    && new SPacketEntityVelocity(packet.getObject()).getEntityId() == player.S()) {
                this.receivedVelocity = true;
            }
        });
    }

}

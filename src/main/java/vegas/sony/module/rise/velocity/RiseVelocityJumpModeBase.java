package vegas.sony.module.rise.velocity;

import vegas.sony.event.EventHandler;
import vegas.sony.event.impl.EventPacketReceive;
import vegas.sony.event.impl.EventPreLivingTravel;
import vegas.sony.event.impl.EventPreMotion;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.module.Mod;
import vegas.sony.module.SubModule;
import vegas.sony.module.rise.RiseVelocity;
import vegas.sony.value.BooleanValue;
import vegas.sony.value.NumberValue;
import vegas.sony.wrapper.impl.EntityPlayerSP;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.Packet;
import vegas.sony.wrapper.impl.SPacketEntityVelocity;

abstract class RiseVelocityJumpModeBase extends SubModule<RiseVelocity> {
    private final NumberValue chance = NumberValue.create(this, "Chance //$ui", "#", "%",
            0.0, 100.0, 100.0, 1.0);
    private final BooleanValue legitTiming = BooleanValue.create(this, "Legit Timing //$ui", false);
    private volatile boolean jump;

    RiseVelocityJumpModeBase(Mod parent, String name) {
        super(parent, name);
        this.chance.setMaximumFractionDigits(0);
        this.addValue(this.chance, this.legitTiming);
    }

    @EventHandler
    public void onPacketReceive(EventPacketReceive event) {
        EntityPlayerSP player = Minecraft.thePlayer();
        if (event.isCanceled() || player.isNull() || !this.getParent().canRunForSwing(player)
                || !player.b$src$Z$fqlxe4()) {
            return;
        }
        Packet.n(event.getPacket(), packet -> this.inspectVelocity(packet, player));
    }

    private void inspectVelocity(Packet packet, EntityPlayerSP player) {
        if (!packet.isInstance(MappedClasses.YX)) {
            return;
        }
        SPacketEntityVelocity velocity = new SPacketEntityVelocity(packet.getObject());
        if (velocity.getEntityId() == player.S() && velocity.getMotionY() > 0
                && (!this.legitTiming.getEffectiveValue().booleanValue()
                || player.V$src$I$fk0dv5() <= 14
                || this.getParent().getOffGroundTicks() <= 1)) {
            this.jump = true;
        }
    }

    @EventHandler
    public void onPreLivingTravel(EventPreLivingTravel event) {
        EntityPlayerSP player = Minecraft.thePlayer();
        if (!this.jump || player.isNull()) {
            return;
        }
        this.jump = false;
        if (this.getParent().canRunForSwing(player)
                && Math.random() * 100.0 < this.chance.getValue().doubleValue()) {
            player.movementInput().V(true);
        }
    }

    @EventHandler
    public void onPreMotion(EventPreMotion event) {
        this.jump = false;
    }

    @Override
    public void onDisable() {
        this.jump = false;
    }
}

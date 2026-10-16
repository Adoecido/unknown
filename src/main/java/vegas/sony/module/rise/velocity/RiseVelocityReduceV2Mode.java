package vegas.sony.module.rise.velocity;

import vegas.sony.event.EventHandler;
import vegas.sony.event.EventPriority;
import vegas.sony.event.impl.EventPacketReceive;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.module.SubModule;
import vegas.sony.module.rise.RiseVelocity;
import vegas.sony.value.NumberValue;
import vegas.sony.wrapper.impl.EntityPlayerSP;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.Packet;
import vegas.sony.wrapper.impl.PacketVelocityBridge;
import vegas.sony.wrapper.impl.SPacketEntityVelocity;

/**
 * Direct percentage based knockback reduction. This intentionally contains no
 * extra attacks, target dependency, rotation or sprint manipulation.
 */
public final class RiseVelocityReduceV2Mode extends SubModule<RiseVelocity> {
    private final NumberValue chance = NumberValue.create(this, "Chance //$ui", "#", "%",
            1.0, 100.0, 100.0, 1.0);
    private final NumberValue horizontal = NumberValue.create(this, "Horizontal //$ui", "#", "%",
            1.0, 60.0, 100.0, 1.0,
            "Percentage of horizontal entity knockback kept //$ui");
    private final NumberValue vertical = NumberValue.create(this, "Vertical //$ui", "#", "%",
            1.0, 100.0, 100.0, 1.0,
            "Percentage of vertical entity knockback kept //$ui");
    private final NumberValue explosionHorizontal = NumberValue.create(this,
            "Explosion horizontal //$ui", "#", "%", 1.0, 100.0, 100.0, 1.0,
            "Percentage of horizontal explosion knockback kept //$ui");
    private final NumberValue explosionVertical = NumberValue.create(this,
            "Explosion vertical //$ui", "#", "%", 1.0, 100.0, 100.0, 1.0,
            "Percentage of vertical explosion knockback kept //$ui");
    private double chanceCounter;

    public RiseVelocityReduceV2Mode(RiseVelocity parent, String name) {
        super(parent, name);
        this.chance.setMaximumFractionDigits(0);
        this.horizontal.setMaximumFractionDigits(0);
        this.vertical.setMaximumFractionDigits(0);
        this.explosionHorizontal.setMaximumFractionDigits(0);
        this.explosionVertical.setMaximumFractionDigits(0);
        this.addValue(this.chance, this.horizontal, this.vertical,
                this.explosionHorizontal, this.explosionVertical);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPacketReceive(EventPacketReceive event) {
        EntityPlayerSP player = Minecraft.thePlayer();
        if (player.isNull() || event.isCanceled()) {
            return;
        }
        Packet.n(event.getPacket(), packet -> this.reduce(packet, player));
    }

    private void reduce(Packet packet, EntityPlayerSP player) {
        if (packet.isInstance(MappedClasses.YX)) {
            SPacketEntityVelocity velocity = new SPacketEntityVelocity(packet.getObject());
            if (velocity.getEntityId() != player.S() || !this.shouldApply()) {
                return;
            }
            double horizontalScale = percent(this.horizontal);
            double verticalScale = percent(this.vertical);
            velocity.setMotionX(velocity.getMotionX() * horizontalScale);
            velocity.setMotionY(velocity.getMotionY() * verticalScale);
            velocity.setMotionZ(velocity.getMotionZ() * horizontalScale);
            return;
        }

        if (MappedClasses.qe != null && packet.isInstance(MappedClasses.qe)
                && this.shouldApply()) {
            PacketVelocityBridge explosion = new PacketVelocityBridge(packet.getObject());
            float horizontalScale = (float)percent(this.explosionHorizontal);
            float verticalScale = (float)percent(this.explosionVertical);
            explosion.setMotionX(explosion.getMotionX() * horizontalScale);
            explosion.setMotionY(explosion.getMotionY() * verticalScale);
            explosion.setMotionZ(explosion.getMotionZ() * horizontalScale);
        }
    }

    private boolean shouldApply() {
        double configured = this.chance.getValue().doubleValue();
        if (configured >= 100.0) {
            return true;
        }
        this.chanceCounter = this.chanceCounter % 100.0 + Math.max(0.0, configured);
        return this.chanceCounter >= 100.0;
    }

    private static double percent(NumberValue value) {
        return Math.max(1.0, Math.min(100.0, value.getValue().doubleValue())) / 100.0;
    }

    @Override
    public void onEnable() {
        this.chanceCounter = 0.0;
    }

    @Override
    public void onDisable() {
        this.chanceCounter = 0.0;
    }

    @Override
    public String getDetailedSuffix() {
        return this.horizontal.getDisplayValue() + "%";
    }
}

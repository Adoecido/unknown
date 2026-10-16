package vegas.sony.module.rise.velocity;

import java.util.Random;
import vegas.sony.config.ClientSettings;
import vegas.sony.event.EventHandler;
import vegas.sony.event.EventPriority;
import vegas.sony.event.impl.EventPacketReceive;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.module.SubModule;
import vegas.sony.module.rise.RiseVelocity;
import vegas.sony.utils.MathUtil;
import vegas.sony.value.BooleanValue;
import vegas.sony.value.NumberValue;
import vegas.sony.wrapper.impl.EntityPlayerSP;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.Packet;
import vegas.sony.wrapper.impl.SPacketEntityVelocity;

/** Scales incoming player knockback while preserving the packet direction. */
public final class RiseVelocityReduceMode extends SubModule<RiseVelocity> {

    private final NumberValue chance = NumberValue.create(this, "Chance //$ui", "#", "%",
            0.0, 100.0, 100.0, 1.0);
    private final NumberValue percentage = NumberValue.create(this,
            "Velocity percentage //$ui", "#", "%", 1.0, 60.0, 100.0, 1.0,
            "Percentage of knockback kept; 1 is strongest and 100 is vanilla //$ui");
    private final BooleanValue requireSprinting = BooleanValue.create(this,
            "Require sprinting //$ui", true);
    private final BooleanValue disableWhileHoldingBack = BooleanValue.create(this,
            "Disable while holding S //$ui", true);
    private final Random random = new Random();

    public RiseVelocityReduceMode(RiseVelocity parent, String name) {
        super(parent, name);
        this.chance.setMaximumFractionDigits(0);
        this.percentage.setMaximumFractionDigits(0);
        this.addValue(this.chance, this.percentage,
                this.requireSprinting, this.disableWhileHoldingBack);
    }

    @Override
    public String getDetailedSuffix() {
        return this.percentage.getDisplayValue() + "%";
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPacketReceive(EventPacketReceive event) {
        EntityPlayerSP player = Minecraft.thePlayer();
        if (player.isNull() || event.isCanceled()) {
            return;
        }
        if (this.requireSprinting.getEffectiveValue().booleanValue()
                && !player.B$src$Z$f90iek()) {
            return;
        }
        if (this.disableWhileHoldingBack.getEffectiveValue().booleanValue()
                && ClientSettings.isPhysicalKeyDown(Minecraft.gameSettings().Y())) {
            return;
        }
        Packet.n(event.getPacket(), packet -> this.reducePlayerVelocity(player, packet));
    }

    private void reducePlayerVelocity(EntityPlayerSP player, Packet packet) {
        if (!packet.isInstance(MappedClasses.YX)) {
            return;
        }
        SPacketEntityVelocity velocity = new SPacketEntityVelocity(packet.getObject());
        if (velocity.getEntityId() != player.S() || !this.rollChance()) {
            return;
        }

        double configured = this.percentage.getValue().doubleValue();
        double scale = Math.max(1.0, Math.min(100.0, configured)) / 100.0;
        velocity.setMotionX(velocity.getMotionX() * scale);
        velocity.setMotionY(velocity.getMotionY() * scale);
        velocity.setMotionZ(velocity.getMotionZ() * scale);
    }

    private boolean rollChance() {
        double configured = this.chance.getValue().doubleValue();
        return configured >= 100.0 || configured > 0.0
                && MathUtil.randomExclusiveUpper(this.random, 0, 100) < configured;
    }
}

package vegas.sony.module.rise;

import vegas.sony.event.EventHandler;
import vegas.sony.event.EventPriority;
import vegas.sony.event.impl.EventPostAttack;
import vegas.sony.event.impl.EventPreAttack;
import vegas.sony.event.impl.EventSetSprinting;
import vegas.sony.module.Category;
import vegas.sony.module.Mod;
import vegas.sony.value.BooleanValue;
import vegas.sony.value.NumberValue;
import vegas.sony.wrapper.impl.EntityPlayerSP;
import vegas.sony.wrapper.impl.Minecraft;

public class RiseKeepSprint extends Mod {
    private final NumberValue slowDownVelocity = NumberValue.create(this,
            "Hit Slow Down During Velocity //$ui", "#.#", "", 0.0, 0.6, 1.0, 0.05);
    private final NumberValue slowDownNormal = NumberValue.create(this,
            "Hit Slow Down Normal //$ui", "#.#", "", 0.0, 0.6, 1.0, 0.05);
    private final BooleanValue sprintSlowDownVelocity = BooleanValue.create(this,
            "Velocity Hit Sprint //$ui", false);
    private final BooleanValue sprintSlowDownNormal = BooleanValue.create(this,
            "Normal Hit Sprint //$ui", false);
    private final BooleanValue bufferAbuse = BooleanValue.create(this, "Buffer Abuse //$ui", false);
    private final NumberValue bufferDecrease = NumberValue.create(this, "Buffer Decrease //$ui", "#.#", "",
            0.1, 1.0, 10.0, 0.1);
    private final NumberValue maxBuffer = NumberValue.create(this, "Max Buffer //$ui", "#", "",
            1.0, 5.0, 10.0, 1.0);
    private final BooleanValue onlyInAir = BooleanValue.create(this, "Only In Air //$ui", false);
    private boolean resetting;
    private double combo;
    private boolean attackArmed;
    private int targetEntityId = -1;

    public RiseKeepSprint() {
        super("RiseKeepSprint //$ui", 9133823, Category.COMBAT,
                "Rewrites the vanilla post-hit slowdown and sprint state like Rise 6.9.5 //$ui");
        this.maxBuffer.setMaximumFractionDigits(0);
        this.bufferAbuse.addDependentValues(this.bufferDecrease, this.maxBuffer);
        this.addValue(this.slowDownVelocity, this.slowDownNormal,
                this.sprintSlowDownVelocity, this.sprintSlowDownNormal,
                this.bufferAbuse, this.bufferDecrease, this.maxBuffer, this.onlyInAir);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreAttack(EventPreAttack event) {
        this.attackArmed = false;
        EntityPlayerSP player = Minecraft.thePlayer();
        if (event.isCanceled() || player.isNull()) {
            return;
        }
        this.targetEntityId = event.getTarget().S();
        this.attackArmed = true;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSetSprinting(EventSetSprinting event) {
        EntityPlayerSP player = Minecraft.thePlayer();
        if (!this.attackArmed || event.isNewStateSprinting() || player.isNull()
                || !event.getEntity().equals(player)) {
            return;
        }
        this.attackArmed = false;
        if (this.onlyInAir.getEffectiveValue().booleanValue()
                && player.b$src$Z$fqlxe4()) {
            return;
        }
        if (this.bufferAbuse.getEffectiveValue().booleanValue()) {
            if (this.combo < this.maxBuffer.getValue().doubleValue() && !this.resetting) {
                this.combo += 1.0;
            } else if (this.combo > 0.0) {
                this.combo = Math.max(0.0,
                        this.combo - this.bufferDecrease.getValue().doubleValue());
                this.resetting = true;
                return;
            } else {
                this.resetting = false;
            }
        } else {
            this.combo = 0.0;
        }
        boolean duringVelocity = player.c$src$I$15a9iwo() > 0;
        double selectedSlowdown = duringVelocity
                ? this.slowDownVelocity.getValue().doubleValue()
                : this.slowDownNormal.getValue().doubleValue();
        boolean selectedSprint = duringVelocity
                ? this.sprintSlowDownVelocity.getEffectiveValue().booleanValue()
                : this.sprintSlowDownNormal.getEffectiveValue().booleanValue();

        // In 1.8.9 the vanilla attack code multiplies motionX/Z by 0.6 immediately
        // before calling setSprinting(false). This event is therefore the exact
        // equivalent of Rise's HitSlowDownEvent injection point.
        double multiplier = selectedSlowdown / 0.6;
        player.C(player.t() * multiplier);
        player.i(player.T() * multiplier);
        if (selectedSprint) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPostAttack(EventPostAttack event) {
        if (event.getTarget().S() == this.targetEntityId) {
            this.attackArmed = false;
            this.targetEntityId = -1;
        }
    }

    @Override
    public void onDisable() {
        this.resetting = false;
        this.combo = 0.0;
        this.attackArmed = false;
        this.targetEntityId = -1;
    }
}

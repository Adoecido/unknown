package vegas.sony.module.blatant;

import vegas.sony.event.EventHandler;
import vegas.sony.event.impl.EventPacketReceive;
import vegas.sony.event.impl.EventPreMotion;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.module.Category;
import vegas.sony.module.Mod;
import vegas.sony.unmap.ItemLimitData;
import vegas.sony.value.BooleanValue;
import vegas.sony.value.LimitValue;
import vegas.sony.wrapper.impl.EntityPlayerSP;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.MovementInput;
import vegas.sony.wrapper.impl.Packet;
import vegas.sony.wrapper.impl.SPacketEntityVelocity;

/**
 * Cancels the vanilla item-use slowdown. MovementInputFromOptions scales moveForward
 * and moveStrafe by 0.2 while the player is using an item (eating, drawing a bow,
 * blocking with a sword). This module restores those input fields to full deflection
 * and rewrites the horizontal motion vector to normal walk/sprint speed.
 */
public class NoSlowdown extends Mod {
    /** Factor vanilla applies to the movement inputs while an item is in use. */
    private static final float ITEM_USE_INPUT_SCALE = 0.2f;
    /** Blocks per tick a sprinting player covers, relative to the walking baseline. */
    private static final double SPRINT_SPEED_BONUS = (double)1.3f;
    /** Diagonal and pure-strafe normalisation, mirroring vanilla moveFlying. */
    private static final double DIAGONAL_STRAFE_SCALE = 0.5;
    private static final double PURE_STRAFE_SCALE = 0.85;
    private static final long MODULE_ID = -7214429765927550220L;

    private final BooleanValue keepSprint = BooleanValue.create(this,
            "Keep sprint //$ui", true, "Force the sprint flag back on while using an item. //$ui");
    private final BooleanValue limitItems = BooleanValue.create(this,
            "Limit Items //$ui", false, "Limits to whitelisted items only. //$ui");
    private final LimitValue whitelist = LimitValue.create(this,
            "v-5c1d90ab //$", "Whitelisted //$ui", LimitValue.ALLOW_LIST_COLOR,
            new ItemLimitData("swords //$"));

    private final BooleanValue duringHits = BooleanValue.create(this,
            "During hits //$ui", true, "Continue item-use movement correction after receiving knockback. May alter horizontal knockback. Disable for the reference behavior. //$ui");

    /** Set when the server pushed us; suppresses the override until we land again. */
    private boolean pendingVelocity;

    public NoSlowdown() {
        super("NoSlowdown //$ui", (int)MODULE_ID, Category.MISC,
                "Prevents slowing down when\nblocking or using items. //$ui");
        this.limitItems.addDependentValues(this.whitelist);
        this.addValue(this.keepSprint, this.duringHits, this.limitItems, this.whitelist);
    }

    @Override
    public boolean isBlatantMod() {
        return true;
    }

    @EventHandler
    public void onPacketReceive(EventPacketReceive event) {
        Packet packet = event.getPacket();
        // S27PacketExplosion always carries knockback for the local player.
        if (packet.isInstance(MappedClasses.qe)) {
            this.pendingVelocity = true;
            return;
        }
        if (packet.isInstance(MappedClasses.YX)) {
            EntityPlayerSP localPlayer = Minecraft.thePlayer();
            if (localPlayer.isNotNull()
                    && new SPacketEntityVelocity(packet).getEntityId() == localPlayer.S()) {
                this.pendingVelocity = true;
            }
        }
    }

    @EventHandler
    public void onMotionUpdate(EventPreMotion event) {
        EntityPlayerSP localPlayer = Minecraft.thePlayer();
        if (localPlayer.isNull()) {
            return;
        }
        // Reference behavior suspends correction after velocity, including the landing tick.
        // During hits opts into correction while that velocity is pending.
        if (this.pendingVelocity) {
            if (localPlayer.b$src$Z$fqlxe4()) {
                this.pendingVelocity = false;
            }
            if (!this.duringHits.getEffectiveValue()) return;
        }
        if (localPlayer.h$src$Z$ftwoya()) {
            return;
        }
        if (!localPlayer.l$src$Z$1io4duf()) {
            return;
        }
        if (this.limitItems.getEffectiveValue().booleanValue()
                && !this.whitelist.matches(localPlayer.getHeldItemHand())) {
            return;
        }
        MovementInput movementInput = localPlayer.movementInput();
        double forwardInput = movementInput.D();
        double strafeInput = movementInput.T();
        // Only the exact 0.2 deflection is the vanilla item-use scaling; anything else
        // was written by another module or is already unslowed.
        if (Math.abs(strafeInput) != (double)ITEM_USE_INPUT_SCALE
                && Math.abs(forwardInput) != (double)ITEM_USE_INPUT_SCALE) {
            return;
        }
        if (this.keepSprint.getEffectiveValue().booleanValue()) {
            localPlayer.R(true);
        }
        // Restore the raw input fields so vanilla movement, sprint and the server-side
        // input echo all behave as if no item were in use.
        if (Math.abs(strafeInput) == (double)ITEM_USE_INPUT_SCALE) {
            movementInput.M(strafeInput > 0.0 ? 1.0f : -1.0f);
        }
        if (Math.abs(forwardInput) == (double)ITEM_USE_INPUT_SCALE) {
            movementInput.B(forwardInput > 0.0 ? 1.0f : -1.0f);
        }
        // The 0.2 that vanilla left behind doubles as the walking speed in blocks per
        // tick, so the slowed inputs are reused directly as the motion magnitude.
        if (Math.abs(movementInput.T()) != 1.0f && forwardInput > 0.0) {
            forwardInput *= localPlayer.B$src$Z$f90iek() ? SPRINT_SPEED_BONUS : 1.0;
        }
        strafeInput *= forwardInput != 0.0 ? DIAGONAL_STRAFE_SCALE : PURE_STRAFE_SCALE;
        double yawRadians = Math.toRadians(localPlayer.J() + 90.0f);
        localPlayer.setMotionX(forwardInput * Math.cos(yawRadians) + strafeInput * Math.sin(yawRadians));
        localPlayer.setMotionZ(forwardInput * Math.sin(yawRadians) - strafeInput * Math.cos(yawRadians));
    }

    @Override
    public void onDisable() {
        this.pendingVelocity = false;
    }
}

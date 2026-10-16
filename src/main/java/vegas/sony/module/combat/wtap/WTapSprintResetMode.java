package vegas.sony.module.combat.wtap;

import vegas.sony.combat.AttackPacketTimingTracker;
import vegas.sony.config.ClientSettings;
import vegas.sony.event.EventHandler;
import vegas.sony.event.impl.EventClickMouse;
import vegas.sony.event.impl.EventPacketReceive;
import vegas.sony.event.impl.EventPlayerUseItem;
import vegas.sony.event.impl.EventPreTick;
import vegas.sony.event.impl.EventRightClickMouse;
import vegas.sony.event.impl.EventSendClickBlockToController;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.module.Mod;
import vegas.sony.module.SubModule;
import vegas.sony.module.combat.HitSelect;
import vegas.sony.rotation.RotationManager;
import vegas.sony.unmap.ModeOption;
import vegas.sony.value.ModeValue;
import vegas.sony.wrapper.impl.Entity;
import vegas.sony.wrapper.impl.EntityLivingBase;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.Packet;
import vegas.sony.wrapper.impl.RayTraceResult;
import vegas.sony.wrapper.impl.SPacketEntityVelocity;

public class WTapSprintResetMode
extends SubModule<HitSelect> {
    private final ModeValue preference;
    private final ModeOption criticalHitsOption;
    private int velocityTicks = 0;
    private boolean pendingTimestampUpdate;
    private static final long VELOCITY_RESET_TICKS = 988881679777005575L;
    private final ModeOption kbReductionOption = new ModeOption("KB reduction //$ui");
    private long lastResetTime;
    private boolean cancelUse = true;

    public WTapSprintResetMode(Mod mod, String string) {
        super(mod, string);
        this.criticalHitsOption = new ModeOption("Critical hits //$ui");
        this.preference = ModeValue.create((Object)this, "Preference //$ui", this.kbReductionOption, this.kbReductionOption, this.criticalHitsOption);
        this.preference.setDescription("KB reduction: Favors knockback reduction\nCritical hits: Favors critical hit frequency //$ui");
        this.addValue(this.preference);
    }

    @EventHandler
    public void onPlayerUseItem(EventPlayerUseItem eventPlayerUseItem) {
        if (this.cancelUse) {
            eventPlayerUseItem.setCancelled(true);
        }
    }

    private boolean isMovingTowardTarget(Entity entity) {
        double deltaX = entity.z() - Minecraft.thePlayer().z();
        double deltaZ = entity.h() - Minecraft.thePlayer().h();
        return deltaX < 0.0 == Minecraft.thePlayer().t() < 0.0
                && deltaZ < 0.0 == Minecraft.thePlayer().T() < 0.0;
    }

    @EventHandler
    public void onRightClickMouse(EventRightClickMouse eventRightClickMouse) {
        if (this.cancelUse) {
            eventRightClickMouse.setCancelled(true);
        }
    }

    @EventHandler
    public void onPacketReceive(EventPacketReceive eventPacketReceive) {
        SPacketEntityVelocity velocityPacket;
        if (eventPacketReceive.getPacketInstance() == null) {
            return;
        }
        if (Minecraft.thePlayer().isNull()) {
            return;
        }
        Packet packet = eventPacketReceive.getPacket();
        boolean velocityReceived = false;
        if (packet.isInstance(MappedClasses.qe)) {
            velocityReceived = true;
        } else if (packet.isInstance(MappedClasses.YX)
                && (velocityPacket = new SPacketEntityVelocity(packet)).getEntityId() == Minecraft.thePlayer().S()) {
            velocityReceived = true;
        }
        if (velocityReceived) {
            this.velocityTicks = (int)VELOCITY_RESET_TICKS;
        }
    }

    private boolean prefersKnockbackReduction() {
        return this.preference.getValue() == this.kbReductionOption;
    }

    @EventHandler
    public void onClickMouse(EventClickMouse eventClickMouse) {
        RayTraceResult rayTrace;
        if (!((HitSelect)this.getParent()).shouldTrigger()) {
            return;
        }
        boolean airborneAfterVelocity = false;
        if (this.velocityTicks > 0) {
            --this.velocityTicks;
            if (eventClickMouse.getThePlayer().b$src$Z$fqlxe4()) {
                this.velocityTicks = 0;
            }
            if (this.prefersKnockbackReduction()) {
                return;
            }
            if (eventClickMouse.getThePlayer().q() > 0.0) {
                airborneAfterVelocity = true;
            } else if (!eventClickMouse.getThePlayer().b$src$Z$fqlxe4()) {
                return;
            }
        }
        if ((rayTrace = RotationManager.INSTANCE.getExtendedReachRayTrace()).isNotNull()
                && rayTrace.getEntity().isInstance(MappedClasses.zm)) {
            EntityLivingBase target = new EntityLivingBase(rayTrace.getEntity());
            if (!this.isMovingTowardTarget(target)) {
                return;
            }
            if (!airborneAfterVelocity) {
                AttackPacketTimingTracker timingTracker = AttackPacketTimingTracker.INSTANCE;
                int expectedHurtTime = timingTracker.getExpectedHurtTimeTicks();
                int upperHurtTime = expectedHurtTime + 1;
                if (target.c$src$I$15a9iwo() <= expectedHurtTime) {
                    if (System.currentTimeMillis() - this.lastResetTime >= timingTracker.getAverageHitDelay() * 2L) {
                        this.pendingTimestampUpdate = true;
                        return;
                    }
                }
                if (target.c$src$I$15a9iwo() > expectedHurtTime
                        && target.c$src$I$15a9iwo() <= upperHurtTime) {
                    return;
                }
            }
            eventClickMouse.setCancelled(true);
            this.cancelUse = true;
            ClientSettings.applyAttackEffects(target);
        }
    }

    @EventHandler
    public void onTick(EventPreTick eventPreTick) {
        if (this.pendingTimestampUpdate) {
            this.lastResetTime = System.currentTimeMillis();
            this.pendingTimestampUpdate = false;
        }
        this.cancelUse = false;
    }

    @EventHandler
    public void onSendClickBlockToController(EventSendClickBlockToController eventSendClickBlockToController) {
        if (this.cancelUse) {
            eventSendClickBlockToController.setCancelled(true);
        }
    }

}

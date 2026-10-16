package vegas.sony.module.combat.velocity;

import vegas.sony.event.EventHandler;
import vegas.sony.event.EventPriority;
import vegas.sony.event.impl.EventPacketReceive;
import vegas.sony.event.impl.EventPreTick;
import vegas.sony.module.Category;
import vegas.sony.module.Mod;
import vegas.sony.combat.AttackPacketTimingTracker;
import vegas.sony.module.control.SecondaryActionControlClaim;
import vegas.sony.module.control.SharedModuleControlClaims;
import vegas.sony.utils.MathUtil;
import vegas.sony.utils.RotationUtil;
import vegas.sony.utils.network.PacketDispatchGuard;
import vegas.sony.value.BooleanValue;
import vegas.sony.value.NumberValue;
import vegas.sony.value.RandomValue;
import vegas.sony.wrapper.impl.Entity;
import vegas.sony.wrapper.impl.EntityLivingBase;
import vegas.sony.wrapper.impl.EntityPlayerSP;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.NetHandlerPlayClientImpl;
import vegas.sony.wrapper.impl.Packet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class VelocityPacketMode
extends Mod {
    private static final long MODULE_ID = 6243151782707490976L;
    private long delayMillis;
    private long releaseTime;
    private final RandomValue airDelay;
    private final Queue<EventPacketReceive> heldPackets;
    private final NumberValue chance;
    private final RandomValue groundDelay;
    private long lastHitTime;
    private Entity target;
    private final PacketDispatchGuard dispatchGuard = PacketDispatchGuard.b;
    private final BooleanValue waterCheck;
    private int hitCount = 0;
    private SecondaryActionControlClaim controlClaim;

    private boolean rollChance() {
        double configuredChance = (Double)this.chance.getValue();
        if (configuredChance >= 100.0) return true;
        if (configuredChance <= 0.0) return false;
        return new Random().nextDouble() * 100.0 < configuredChance;
    }

    public VelocityPacketMode() {
        super("KnockbackDelay //$ui", (int)MODULE_ID, Category.NETWORK, "Delays incoming knockback packets //$ui");
        this.heldPackets = new LinkedList<EventPacketReceive>();
        this.chance = NumberValue.createWithDescription(this, "Chance //$ui", "#", "%", 0.0, 40.0, 100.0, "Chance of delaying knockback //$ui");
        this.airDelay = RandomValue.create(this, "Air delay //$ui", "#", "", 0.0, 50.0, 100.0, 500.0);
        this.groundDelay = RandomValue.create(this, "Ground delay //$ui", "#", "", 0.0, 200.0, 250.0, 500.0);
        this.waterCheck = BooleanValue.create(this, "Water check //$ui", false, "Won't delay knockback if in water //$ui");
        this.controlClaim = SharedModuleControlClaims.secondaryAction;
        this.addValue(this.chance, this.airDelay, this.groundDelay, this.waterCheck);
        this.chance.setMaximumFractionDigits(0);
        this.controlClaim.setPriority(this, 5);
    }

    @Override
    public String getDetailedSuffix() {
        if (!this.heldPackets.isEmpty()) {
            return "\u00a7cHolding";
        }
        return this.groundDelay.getDisplayValue() + "ms";
    }


    private boolean isInWater() {
        EntityPlayerSP player = Minecraft.thePlayer();
        if (player.isNull()) {
            return true;
        }
        return this.waterCheck.getEffectiveValue() && player.h$src$Z$ftwoya();
    }

    private boolean shouldDelay() {
        return !this.isInWater() && this.rollChance();
    }

    public EntityLivingBase findTargetInRange(double fov, double range) {
        EntityLivingBase target = RotationUtil.u(range, fov / 2.0);
        if (target == null) {
            return null;
        }
        if (RotationUtil.o(Minecraft.thePlayer(), target, range, 90.0, true)) {
            return target;
        }
        return null;
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onPacketReceive(EventPacketReceive event) {
        if (event.getWorld().isNotNull() && event.getThePlayer().isNotNull()) {
            Packet packet = event.getPacket();
            if (this.dispatchGuard.R(packet)) {
                return;
            }
            boolean isVelocity = this.containsPlayerVelocity(packet, event.getThePlayer());
            if (isVelocity) {
                if (!this.shouldDelay()) {
                    isVelocity = false;
                } else {
                    this.lastHitTime = System.currentTimeMillis();
                }
            }
            if (isVelocity && this.heldPackets.isEmpty()) {
                this.delayMillis = this.hitCount < 3 ? (long)this.airDelay.getRandomValue() : (long)this.groundDelay.getRandomValue();
                if (this.delayMillis > 0L) {
                    this.releaseTime = System.currentTimeMillis() + this.delayMillis;
                    this.heldPackets.add(event);
                    event.setCancelled(true);
                    this.controlClaim.markClaimed();
                }
            } else if (!this.heldPackets.isEmpty()) {
                this.heldPackets.add(event);
                event.setCancelled(true);
                this.controlClaim.markClaimed();
            }
            if (this.heldPackets.isEmpty()) {
                this.dispatchGuard.J(packet);
                this.controlClaim.clearClaimed();
            }
        }
    }

    private void flushHeldPackets() {
        if (this.heldPackets.isEmpty()) {
            return;
        }
        if (System.currentTimeMillis() >= this.releaseTime) {
            NetHandlerPlayClientImpl networkHandler = Minecraft.thePlayer().sendQueue();
            for (EventPacketReceive heldEvent : this.heldPackets) {
                this.dispatchGuard.l(heldEvent.getPacket(), networkHandler);
            }
            this.heldPackets.clear();
        }
    }

    private boolean containsPlayerVelocity(Packet packet, EntityPlayerSP player) {
        boolean[] found = new boolean[]{false};
        Packet.n(packet, resolvedPacket -> this.checkVelocityPacket(found, player, resolvedPacket));
        return found[0];
    }

    private void flushOnTick() {
        this.flushHeldPackets();
    }

    @EventHandler
    public void onTick(EventPreTick event) {
        if (Minecraft.thePlayer().isNull()) {
            return;
        }
        PacketDispatchGuard.B(this::flushOnTick);
        if (event.getWorld().isNotNull() && event.getThePlayer().isNotNull()) {
            this.target = this.findTargetInRange(90.0, 5.0);
            this.hitCount = event.getThePlayer().b$src$Z$fqlxe4() ? ++this.hitCount : 0;
        }
    }

    private void checkVelocityPacket(boolean[] found, EntityPlayerSP player, Packet packet) {
        Entity affectedEntity = AttackPacketTimingTracker.getVelocityPacketEntity(packet);
        if (!found[0] && affectedEntity != null && affectedEntity.equals(player)
                && System.currentTimeMillis() - this.lastHitTime > 475L) {
            found[0] = true;
        }
    }
}

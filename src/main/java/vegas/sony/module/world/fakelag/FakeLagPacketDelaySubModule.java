package vegas.sony.module.world.fakelag;

import vegas.sony.event.EventHandler;
import vegas.sony.event.EventPriority;
import vegas.sony.event.impl.DelayedPacketSendEntry;
import vegas.sony.event.impl.EventPacketSend;
import vegas.sony.event.impl.EventPreRenderTick;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.module.Mod;
import vegas.sony.module.SubModule;
import vegas.sony.combat.AttackPacketTimingTracker;
import vegas.sony.module.control.PrimaryActionControlClaim;
import vegas.sony.module.control.SharedModuleControlClaims;
import vegas.sony.module.world.FakeLag;
import vegas.sony.utils.ItemStackScoreUtil;
import vegas.sony.utils.RayTraceUtil;
import vegas.sony.utils.concurrent.ReadWriteLockHelper;
import vegas.sony.utils.datas.PlayerLocationSnapshot;
import vegas.sony.utils.network.PacketDispatchGuard;
import vegas.sony.value.NumberValue;
import vegas.sony.wrapper.impl.C03PacketPlayer;
import vegas.sony.wrapper.impl.CPacketPlayerBlockPlacement;
import vegas.sony.wrapper.impl.Entity;
import vegas.sony.wrapper.impl.EntityLivingBase;
import vegas.sony.wrapper.impl.ItemStack;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.Packet;
import vegas.sony.wrapper.impl.RayTraceResult;
import vegas.sony.wrapper.impl.UseEntityPacketBridge;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class FakeLagPacketDelaySubModule
extends SubModule<FakeLag> {
    public final NumberValue transmissionOffset = NumberValue.create((Object)this, "Transmission offset //$ui", "#", "", 0.0, 5.0, 50.0, 1.0);
    private final Map<DelayedPacketSendEntry, Long> delayedPackets = new LinkedHashMap<DelayedPacketSendEntry, Long>();
    private final PrimaryActionControlClaim controlClaim;
    private final PacketDispatchGuard dispatchGuard = PacketDispatchGuard.b;
    private long lastAttackTime;
    private Entity targetEntity;
    private PlayerLocationSnapshot lastPositionSnapshot;
    private long lastBlockPlaceTime;
    private long blockPlaceCooldownUntil;
    private final ReadWriteLockHelper lock = new ReadWriteLockHelper();

    private void dispatchDelayedPacket(DelayedPacketSendEntry delayedPacketSendEntry) {
        this.dispatchGuard.o(delayedPacketSendEntry.getEvent());
        ((FakeLag)this.getParent()).recordServerPosition(delayedPacketSendEntry.getEvent().getPacket());
    }


    private void flushAll() {
        this.lock.lockWrite();
        try {
            Iterator<Map.Entry<DelayedPacketSendEntry, Long>> iterator = this.delayedPackets.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<DelayedPacketSendEntry, Long> entry = iterator.next();
                this.dispatchDelayedPacket(entry.getKey());
                iterator.remove();
            }
        } finally {
            this.lock.unlockWrite();
        }
    }

    private void delaySend(EventPacketSend eventPacketSend) {
        this.lock.lockWrite();
        try {
            long offset = ((Double)this.transmissionOffset.getValue()).longValue();
            if (offset > 0L) {
                long accumulated = 0L;
                for (Map.Entry<DelayedPacketSendEntry, Long> entry : this.delayedPackets.entrySet()) {
                    DelayedPacketSendEntry delayedPacketSendEntry = entry.getKey();
                    accumulated += offset;
                    if (delayedPacketSendEntry.isOffsetApplied()) continue;
                    entry.setValue(System.currentTimeMillis() + accumulated);
                    delayedPacketSendEntry.setOffsetApplied(true);
                }
                if (eventPacketSend != null) {
                    this.delayedPackets.put(new DelayedPacketSendEntry(eventPacketSend), System.currentTimeMillis() + accumulated + offset);
                    eventPacketSend.setCancelled(true);
                }
            } else {
                this.delayedPackets.keySet().forEach(this::dispatchDelayedPacket);
                this.delayedPackets.clear();
            }
        } finally {
            this.lock.unlockWrite();
        }
    }

    private void processPacket(EventPacketSend eventPacketSend) {
        Packet packet = eventPacketSend.getPacket();
        if (UseEntityPacketBridge.isUseEntityPacket(packet)) {
            UseEntityPacketBridge useEntityPacket = new UseEntityPacketBridge(packet);
            if (useEntityPacket.isAttack()) {
                Entity attackedEntity = Minecraft.theWorld().V(useEntityPacket.getEntityId());
                if (attackedEntity.isInstance(MappedClasses.zm)) {
                    EntityLivingBase livingTarget = new EntityLivingBase(attackedEntity);
                    if (livingTarget.c$src$I$15a9iwo() <= AttackPacketTimingTracker.INSTANCE.getExpectedHurtTimeTicks()) {
                        if (System.currentTimeMillis() - this.lastAttackTime > 250L) {
                            if (this.releasePending()) {
                                return;
                            }
                        } else {
                            this.lastAttackTime = System.currentTimeMillis();
                        }
                    } else if (livingTarget.c$src$I$15a9iwo() <= AttackPacketTimingTracker.INSTANCE.getExpectedHurtTimeTicks() + 1 && this.releasePending()) {
                        return;
                    }
                }
            }
        } else {
            if (packet.isInstance(MappedClasses.F9) || packet.isInstance(MappedClasses.u7) || packet.isInstance(MappedClasses.DN) || packet.isInstance(MappedClasses.q6)) {
                this.delaySend(eventPacketSend);
                return;
            }
            if (packet.isInstance(MappedClasses.YB)) {
                CPacketPlayerBlockPlacement placementPacket = new CPacketPlayerBlockPlacement(packet);
                ItemStack placedStack = placementPacket.getItemStack();
                if (placedStack.isNotNull()) {
                    if (!ItemStackScoreUtil.h(placedStack.getItem())) {
                        this.delaySend(eventPacketSend);
                        this.blockPlaceCooldownUntil = System.currentTimeMillis() + 500L;
                        return;
                    }
                    if (System.currentTimeMillis() - this.lastBlockPlaceTime >= 500L) {
                        this.delaySend(eventPacketSend);
                        this.lastBlockPlaceTime = System.currentTimeMillis();
                        return;
                    }
                }
            } else if (this.blockPlaceCooldownUntil > System.currentTimeMillis() || Minecraft.thePlayer().l$src$Z$1io4duf() && !Minecraft.thePlayer().o$src$Z$1iprrmi() || !Minecraft.thePlayer().b$src$Z$fqlxe4() && Minecraft.thePlayer().M$src$F$ff28gb() > 3.0f) {
                this.delaySend(eventPacketSend);
                return;
            }
        }
        if (this.targetEntity != null && this.targetEntity.isNotNull() && this.targetEntity.isInstance(MappedClasses.zm)) {
            if (packet.isInstance(MappedClasses.qD)) {
                C03PacketPlayer movementPacket = new C03PacketPlayer(packet);
                if (this.lastPositionSnapshot != null && movementPacket.hasPosition()) {
                    PlayerLocationSnapshot targetPosition = new PlayerLocationSnapshot(this.targetEntity.z(), this.targetEntity.N(), this.targetEntity.h());
                    PlayerLocationSnapshot packetPosition = new PlayerLocationSnapshot(movementPacket.getX(), movementPacket.getY(), movementPacket.getZ());
                    if (PlayerLocationSnapshot.rayIntersectionDistance(this.lastPositionSnapshot, targetPosition) < PlayerLocationSnapshot.rayIntersectionDistance(packetPosition, targetPosition) - 0.03) {
                        this.delaySend(eventPacketSend);
                        return;
                    }
                }
            }
        } else {
            this.delaySend(eventPacketSend);
            return;
        }
        long baseDelay = ((Double)((FakeLag)this.getParent()).delay.getValue()).longValue();
        long sendTime = System.currentTimeMillis() + baseDelay;
        long lastPositionPacketTime = System.currentTimeMillis();
        this.lock.lockRead();
        try {
            for (Map.Entry<DelayedPacketSendEntry, Long> entry : this.delayedPackets.entrySet()) {
                C03PacketPlayer movementPacket;
                DelayedPacketSendEntry delayedPacket = entry.getKey();
                Packet queuedPacket = delayedPacket.getEvent().getPacket();
                if (!queuedPacket.isInstance(MappedClasses.qD) || !(movementPacket = new C03PacketPlayer(queuedPacket)).hasPosition()) continue;
                lastPositionPacketTime = entry.getValue();
            }
        } finally {
            this.lock.unlockRead();
        }
        sendTime = Math.min(lastPositionPacketTime + 90L, sendTime);
        this.lock.lockWrite();
        try {
            this.delayedPackets.put(new DelayedPacketSendEntry(eventPacketSend), sendTime);
        } finally {
            this.lock.unlockWrite();
        }
        eventPacketSend.setCancelled(true);
    }

    @EventHandler(priority=EventPriority.NORMAL)
    public void onPacketSend(EventPacketSend eventPacketSend) {
        if (this.controlClaim.isClaimed()) {
            this.flushAll();
            return;
        }
        Packet packet = eventPacketSend.getPacket();
        if (eventPacketSend.isCanceled()) {
            return;
        }
        if (eventPacketSend.wasModified()) {
            return;
        }
        if (Minecraft.thePlayer().isNull() || this.dispatchGuard.R(packet) || packet.isInstance(MappedClasses.VP)) {
            return;
        }
        this.processPacket(eventPacketSend);
    }

    @EventHandler
    public void onPreRenderTick(EventPreRenderTick eventPreRenderTick) {
        this.targetEntity = null;
        RayTraceResult rayTraceResult = RayTraceUtil.F(10.0, 0.0f, true);
        if (rayTraceResult != null && rayTraceResult.isNotNull()) {
            this.targetEntity = rayTraceResult.getEntity();
        }
        this.lock.lockWrite();
        try {
            if (!this.delayedPackets.isEmpty()) {
                this.flushExpired();
            }
        } finally {
            this.lock.unlockWrite();
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void capturePosition(EventPacketSend eventPacketSend) {
        C03PacketPlayer movementPacket;
        Packet packet = eventPacketSend.getPacket();
        if (packet.isInstance(MappedClasses.qD) && (movementPacket = new C03PacketPlayer(packet)).hasPosition()) {
            this.lastPositionSnapshot = new PlayerLocationSnapshot(movementPacket.getX(), movementPacket.getY(), movementPacket.getZ());
        }
    }

    private boolean releasePending() {
        this.lock.lockWrite();
        try {
            Iterator<Map.Entry<DelayedPacketSendEntry, Long>> iterator = this.delayedPackets.entrySet().iterator();
            if (!iterator.hasNext()) {
                return false;
            }
            DelayedPacketSendEntry delayedPacket = iterator.next().getKey();
            this.dispatchDelayedPacket(delayedPacket);
            iterator.remove();
            return true;
        } finally {
            this.lock.unlockWrite();
        }
    }

    public FakeLagPacketDelaySubModule(Mod parent, String name) {
        super(parent, name);
        this.controlClaim = SharedModuleControlClaims.primaryAction;
        this.addValue(this.transmissionOffset);
    }

    @Override
    public void onBeforeDisable() {
        this.flushAll();
    }

    @Override
    public String getDetailedSuffix() {
        return "Dynamic" + ((FakeLag)this.getParent()).delay.getDisplayValue() + "ms";
    }

    private void flushExpired() {
        Iterator<Map.Entry<DelayedPacketSendEntry, Long>> iterator = this.delayedPackets.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<DelayedPacketSendEntry, Long> entry = iterator.next();
            if (System.currentTimeMillis() < entry.getValue()) break;
            this.dispatchDelayedPacket(entry.getKey());
            iterator.remove();
        }
    }
}

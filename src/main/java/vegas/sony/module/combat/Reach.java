package vegas.sony.module.combat;

import vegas.sony.SonyVegas;
import vegas.sony.event.EventHandler;
import vegas.sony.event.impl.EventPostAttack;
import vegas.sony.event.impl.EventPostRenderTick;
import vegas.sony.event.impl.EventPostTick;
import vegas.sony.event.impl.EventPreRenderTick;
import vegas.sony.event.impl.EventPreTick;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.module.Category;
import vegas.sony.module.Mod;
import vegas.sony.module.combat.autoclicker.AutoClickerEntityPositionState;
import vegas.sony.notification.NotificationType;
import vegas.sony.unmap.ModeOption;
import vegas.sony.utils.RayTraceUtil;
import vegas.sony.utils.RotationUtil;
import vegas.sony.value.BooleanValue;
import vegas.sony.value.ModeValue;
import vegas.sony.value.NumberValue;
import vegas.sony.value.RandomValue;
import vegas.sony.wrapper.impl.EffectRenderer;
import vegas.sony.wrapper.impl.Entity;
import vegas.sony.wrapper.impl.EntityFX;
import vegas.sony.wrapper.impl.EntityOtherPlayerMP;
import vegas.sony.wrapper.impl.EntityPlayerSP;
import vegas.sony.wrapper.impl.ForgeVersion;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.Packet;
import vegas.sony.wrapper.impl.RayTraceResult;
import vegas.sony.wrapper.impl.World;
import vegas.sony.wrapper.impl.WorldClient;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Reach
extends Mod {
    private int hitCooldown = 0;
    private final Random random;
    private final BooleanValue misplace;
    private final BooleanValue disableInWater;
    private final NumberValue chance;
    private final Map<Integer, AutoClickerEntityPositionState> entityStates;
    private final RandomValue range = RandomValue.createWithDescription(this, "Range //$ui", "#.##", "", 3.0, 3.0, 3.1, 4.0, 0.01, "The range in which your reach will be increased to. //$ui");
    private final BooleanValue verticalCheck;
    private final ModeValue chanceMode;
    private final BooleanValue disadvantage;
    private boolean reachActive = false;
    private final BooleanValue onlyWhileSprinting;
    private final ModeOption normalMode;
    private Entity lastTarget;
    private final ModeOption advancedMode;

    @EventHandler
    public void onPostAttack(EventPostAttack eventPostAttack) {
        if (Packet.h() && !this.advancedMode.isSelected()) {
            return;
        }
        this.reachActive = false;
    }

    @EventHandler
    public void onPreRenderTick(EventPreRenderTick eventPreRenderTick) {
        if (!this.misplace.getEffectiveValue().booleanValue()) {
            return;
        }
        this.applyMisplace(true);
    }

    private boolean shouldExtendReach() {
        if (Packet.A()) {
            return this.advancedMode.isSelected();
        }
        if (this.advancedMode.isSelected()) {
            return this.reachActive;
        }
        return (Double)this.chance.getValue() > this.random.nextInt(100);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (SonyVegas.INSTANCE.isOnlineConnected()) {
            SonyVegas.INSTANCE.getNotificationManager().show("Reach is unsafe", "Reach is generally unsafe and detected by most servers, it is advised to avoid using it", NotificationType.ALERT, 15000L);
        }
    }

    private void misplaceParticles() {
        float directionOffset = this.disadvantage.getEffectiveValue() ? -90.0f : 90.0f;
        double misplaceDistance = this.range.getMinimumValue() - 3.0;
        EffectRenderer effectRenderer = Minecraft.z();
        if (effectRenderer.isNull()) {
            return;
        }
        try {
            List<EntityFX>[][] particleLayers = effectRenderer.getFxLayers();
            for (List<EntityFX>[] particleLayer : particleLayers) {
                for (List<EntityFX> particleList : particleLayer) {
                    for (EntityFX particle : particleList) {
                        EntityPlayerSP player = Minecraft.thePlayer();
                        if (particle.isNull()
                                || !particle.isInstance(MappedClasses.Fy) && !particle.isInstance(MappedClasses.Vc)
                                || player.isNull()
                                || player.getDistanceToEntity(particle) >= this.range.getMaximumValue() + 2.0
                                || particle.N() >= player.N() + 2.5
                                || RotationUtil.S(player, particle)) {
                            continue;
                        }
                        float angle = this.computeAngle(player.z(), player.h(), particle.z(), particle.h());
                        double offsetX = Math.cos(Math.toRadians(angle + directionOffset)) * misplaceDistance;
                        double offsetZ = Math.sin(Math.toRadians(angle + directionOffset)) * misplaceDistance;
                        particle.H(particle.z() - offsetX);
                        particle.l(particle.h() - offsetZ);
                        particle.C(particle.M() - offsetX);
                        particle.s(particle.m$src$D$fwnne5() - offsetZ);
                    }
                }
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public BooleanValue getMisplaceSetting() {
        return this.misplace;
    }

    @EventHandler
    public void onPreTick(EventPreTick eventPreTick) {
        if (this.advancedMode.isSelected()) {
            this.updateTarget();
        }
    }

    private float computeAngle(double sourceX, double sourceZ, double targetX, double targetZ) {
        double deltaX = targetX - sourceX;
        double deltaZ = targetZ - sourceZ;
        float angle = (float)Math.toDegrees(-Math.atan(deltaX / deltaZ));
        if (deltaZ < 0.0 && deltaX < 0.0) {
            angle = (float)(90.0 + Math.toDegrees(Math.atan(deltaZ / deltaX)));
        } else if (deltaZ < 0.0 && deltaX > 0.0) {
            angle = (float)(-90.0 + Math.toDegrees(Math.atan(deltaZ / deltaX)));
        }
        return angle;
    }

    @EventHandler
    public void onPostRenderTick(EventPostRenderTick eventPostRenderTick) {
        if (!this.misplace.getEffectiveValue().booleanValue()) {
            return;
        }
        this.restorePositions(true);
    }

    private void applyMisplace(boolean includeHistoricalPositions) {
        WorldClient world = Minecraft.theWorld();
        if (world.isNull()) {
            return;
        }
        for (Object underlyingEntity : world.z()) {
            Entity entity = new Entity(underlyingEntity);
            if (!entity.isInstance(MappedClasses.lG) || entity.isInstance(MappedClasses.z5)) {
                continue;
            }
            EntityOtherPlayerMP remotePlayer = new EntityOtherPlayerMP(underlyingEntity);
            int entityId = entity.S();
            AutoClickerEntityPositionState state = this.entityStates.computeIfAbsent(entityId, ignored -> {
                AutoClickerEntityPositionState newState = new AutoClickerEntityPositionState();
                newState.entityId = entityId;
                return newState;
            });
            state.actualPosX = entity.z();
            state.actualPosZ = entity.h();
            state.actualLastTickPosX = entity.M();
            state.actualLastTickPosZ = entity.m$src$D$fwnne5();
            state.actualPrevPosX = remotePlayer.C$src$D$1i1kszo();
            state.actualPrevPosZ = remotePlayer.L$src$D$1i6iybx();
            state.actualServerPosX = remotePlayer.a$src$D$1ii2msi();
            state.actualServerPosZ = remotePlayer.G$src$D$1i3rzd4();
            if (includeHistoricalPositions) {
                remotePlayer.H(state.misplacedPosX);
                remotePlayer.l(state.misplacedPosZ);
                remotePlayer.C(state.misplacedLastTickPosX);
                remotePlayer.s(state.misplacedLastTickPosZ);
                remotePlayer.S(state.misplacedPrevPosX);
                remotePlayer.c(state.misplacedPrevPosZ);
                remotePlayer.B(entity.z(), entity.N(), entity.h());
                continue;
            }
            remotePlayer.H(state.misplacedPosX);
            remotePlayer.l(state.misplacedPosZ);
            remotePlayer.B(entity.z(), entity.N(), entity.h());
            remotePlayer.H(state.actualPosX);
            remotePlayer.l(state.actualPosZ);
        }
    }

    private boolean isReachAllowed() {
        if (Packet.h()) {
            if (!this.isEnabled() || this.misplace.getEffectiveValue()) {
                return false;
            }
            EntityPlayerSP player = Minecraft.thePlayer();
            if (this.disableInWater.getEffectiveValue() && (player.h$src$Z$ftwoya() || player.Q$src$Z$fh9faz())) {
                return false;
            }
            return !this.onlyWhileSprinting.getEffectiveValue() || player.B$src$Z$f90iek();
        }
        return this.isEnabled();
    }

    @EventHandler
    public void onPostTick(EventPostTick eventPostTick) {
        if (!this.misplace.getEffectiveValue().booleanValue()) {
            return;
        }
        this.updateMisplacedPositions();
        this.restorePositions(false);
        if (ForgeVersion.MC_1_8_9.B()) {
            this.misplaceParticles();
        }
    }

    private void updateMisplacedPositions() {
        WorldClient world = Minecraft.theWorld();
        EntityPlayerSP player = Minecraft.thePlayer();
        if (world.isNull()) {
            return;
        }
        float directionOffset = this.disadvantage.getEffectiveValue() ? -90.0f : 90.0f;
        for (Object underlyingEntity : world.z()) {
            Entity entity = new Entity(underlyingEntity);
            if (!entity.isInstance(MappedClasses.lG) || entity.isInstance(MappedClasses.z5)) {
                continue;
            }
            EntityOtherPlayerMP remotePlayer = new EntityOtherPlayerMP(underlyingEntity);
            double misplaceDistance = this.range.getMinimumValue() - 3.0;
            double distanceToPlayer = Math.hypot(player.z() - entity.z(), player.h() - entity.h());
            float angle = this.computeAngle(player.z(), player.h(), entity.z(), entity.h());
            double remainingDistance = distanceToPlayer - misplaceDistance;
            if (remainingDistance < 0.5 && (misplaceDistance += remainingDistance - 0.5) < 0.0) {
                misplaceDistance = 0.0;
            }
            double offsetX = Math.cos(Math.toRadians(angle + directionOffset)) * misplaceDistance;
            double offsetZ = Math.sin(Math.toRadians(angle + directionOffset)) * misplaceDistance;
            int entityId = entity.S();
            AutoClickerEntityPositionState state = this.entityStates.get(entityId);
            boolean hadPreviousState = state != null;
            if (!hadPreviousState) {
                state = new AutoClickerEntityPositionState();
                state.entityId = entityId;
            } else {
                state = this.entityStates.get(entityId);
            }
            this.entityStates.put(entityId, state);
            state.actualPosX = entity.z();
            state.actualPosZ = entity.h();
            state.actualLastTickPosX = entity.M();
            state.actualLastTickPosZ = entity.m$src$D$fwnne5();
            state.actualPrevPosX = remotePlayer.C$src$D$1i1kszo();
            state.actualPrevPosZ = remotePlayer.L$src$D$1i6iybx();
            state.actualServerPosX = remotePlayer.a$src$D$1ii2msi();
            state.actualServerPosZ = remotePlayer.G$src$D$1i3rzd4();
            state.misplacedPosX = state.actualPosX - offsetX;
            state.misplacedPosZ = state.actualPosZ - offsetZ;
            state.misplacedLastTickPosX = state.actualLastTickPosX - offsetX;
            state.misplacedLastTickPosZ = state.actualLastTickPosZ - offsetZ;
            state.misplacedPrevPosX = state.actualPrevPosX - offsetX;
            state.misplacedPrevPosZ = state.actualPrevPosZ - offsetZ;
            if (hadPreviousState) {
                state.misplacedLastTickPosX = state.previousMisplacedLastTickPosX;
                state.misplacedLastTickPosZ = state.previousMisplacedLastTickPosZ;
            }
        }
    }

    private void updateTarget() {
        Entity tracedEntity = null;
        RayTraceResult rayTrace = RayTraceUtil.F(this.range.getMaximumValue(), 0.0f, true);
        if (rayTrace != null && rayTrace.isNotNull() && rayTrace.getEntity().isNotNull()) {
            tracedEntity = rayTrace.getEntity();
        }
        if (Packet.h()) {
            if (this.hitCooldown > 0) {
                --this.hitCooldown;
            }
            if (tracedEntity == null || tracedEntity.isNull()
                    || this.lastTarget != null && !tracedEntity.equals(this.lastTarget)) {
                this.lastTarget = null;
                return;
            }
            if (this.isReachAllowed() && this.hitCooldown == 0) {
                this.reachActive = (Double)this.chance.getValue() > this.random.nextInt(100);
                if (this.reachActive) {
                    this.hitCooldown = 10;
                }
            }
            this.lastTarget = tracedEntity;
            return;
        }
        --this.hitCooldown;
        this.lastTarget = tracedEntity;
    }

    @Override
    public String getDetailedSuffix() {
        return this.range.getDisplayValue();
    }

    @Override
    public void onDisable() {
        this.entityStates.clear();
    }

    public double getReachDistance() {
        if (!this.isReachAllowed() || !this.shouldExtendReach()) {
            return 3.0;
        }
        return this.range.getRandomValue();
    }

    public double getMaximumConfiguredReachDistance() {
        return Math.max(this.range.getMinimumValue(), this.range.getMaximumValue());
    }

    @EventHandler
    public void onPreTickMisplace(EventPreTick eventPreTick) {
        if (!this.misplace.getEffectiveValue().booleanValue()) {
            return;
        }
        this.updateMisplacedPositions();
        this.applyMisplace(false);
    }

    public Reach() {
        super("Reach //$ui", -16711936, Category.COMBAT, "Extends attack reach //$ui");
        this.chance = NumberValue.createWithDescription(this, "Chance //$ui", "#", "%", 0.0, 50.0, 100.0, "The chance of reach taking affect when hitting an opponent //$ui");
        this.advancedMode = new ModeOption("Advanced //$ui");
        this.normalMode = new ModeOption("Normal //$ui");
        this.chanceMode = ModeValue.create((Object)this, "Chance mode //$ui", this.advancedMode, this.advancedMode, this.normalMode);
        this.misplace = BooleanValue.create(this, "Misplace //$ui", false, "Pulls players towards you rather than giving you extra reach distance. Uses the minimum slider value. //$ui");
        this.disadvantage = BooleanValue.create(this, "Disadvantage //$ui", false, "Moves misplaced players in opposite direction. Useful for framing other players //$ui");
        this.verticalCheck = BooleanValue.create(this, "Vertical check //$ui", false, "Prevents hitting players which are y0.2 above or below you\nfor more legitimate use //$ui");
        this.onlyWhileSprinting = BooleanValue.create(this, "Only while sprinting //$ui", false, "Only give extra reach while sprinting //$ui");
        this.disableInWater = BooleanValue.create(this, "Disable in water //$ui", false, "Won't give any extra reach while standing in water //$ui");
        this.entityStates = new HashMap<Integer, AutoClickerEntityPositionState>();
        this.random = new Random();
        this.misplace.addDependentValues(this.disadvantage);
        this.range.setAbsoluteClampLimit(100.0);
        this.addValue(this.range, this.chance, this.chanceMode, this.misplace, this.disadvantage, this.verticalCheck, this.onlyWhileSprinting, this.disableInWater);
    }

    private void restorePositions(boolean restoreHistoricalPositions) {
        WorldClient world = Minecraft.theWorld();
        if (world.isNull()) {
            return;
        }
        for (Map.Entry<Integer, AutoClickerEntityPositionState> entry : this.entityStates.entrySet()) {
            AutoClickerEntityPositionState state = entry.getValue();
            Object underlyingEntity = ((World)world).V(state.entityId);
            Entity entity = new Entity(underlyingEntity);
            if (!entity.isNotNull() || !entity.isInstance(MappedClasses.lG)) {
                continue;
            }
            EntityOtherPlayerMP remotePlayer = new EntityOtherPlayerMP(underlyingEntity);
            if (restoreHistoricalPositions) {
                state.previousMisplacedLastTickPosX = state.misplacedPosX;
                state.previousMisplacedLastTickPosZ = state.misplacedPosZ;
                remotePlayer.H(state.actualPosX);
                remotePlayer.l(state.actualPosZ);
                remotePlayer.C(state.actualLastTickPosX);
                remotePlayer.s(state.actualLastTickPosZ);
                remotePlayer.S(state.actualPrevPosX);
                remotePlayer.c(state.actualPrevPosZ);
            }
            remotePlayer.B(remotePlayer.z(), remotePlayer.N(), remotePlayer.h());
        }
    }
}

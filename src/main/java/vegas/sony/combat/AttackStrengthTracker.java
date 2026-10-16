package vegas.sony.combat;

import vegas.sony.SonyVegas;
import vegas.sony.event.EventHandler;
import vegas.sony.event.EventListener;
import vegas.sony.event.impl.EventEntityJoinWorld;
import vegas.sony.event.impl.EventLivingUpdate;
import vegas.sony.event.impl.EventPostTick;
import vegas.sony.event.impl.EventPreAttack;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.utils.ItemStackScoreUtil;
import vegas.sony.utils.RotationUtil;
import vegas.sony.wrapper.impl.AttributeModifier;
import vegas.sony.wrapper.impl.EnchantmentHelper;
import vegas.sony.wrapper.impl.Entity;
import vegas.sony.wrapper.impl.EntityLivingBase;
import vegas.sony.wrapper.impl.EntityPlayer;
import vegas.sony.wrapper.impl.EntityPotion;
import vegas.sony.wrapper.impl.EnumCreatureAttribute;
import vegas.sony.wrapper.impl.ForgeVersion;
import vegas.sony.wrapper.impl.ItemAttributeModifiers;
import vegas.sony.wrapper.impl.ItemSplashPotion;
import vegas.sony.wrapper.impl.ItemStack;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.PotionEffect;
import vegas.sony.wrapper.impl.World;
import vegas.sony.wrapper.impl.WorldClient;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AttackStrengthTracker
implements EventListener {
    private final HashMap<Integer, TrackedPlayerAttackState> playerStates = new HashMap();
    private Object trackedWorldHandle;
    public static final AttackStrengthTracker INSTANCE = new AttackStrengthTracker();
    private static int[] controlFlowMarker;
    private final List<EntityPotion> healingPotions = new ArrayList<EntityPotion>();

    static float calculateAttackDamage(ItemStack itemStack, EntityPlayer player,
            boolean targetWasBlocking, boolean criticalHit) {
        float baseDamage = 1.0f;
        if (itemStack.isNotNull()) {
            ItemAttributeModifiers modifiers = itemStack.o();
            if (modifiers.size() > 0) {
                int attackDamageModifierIndex = ForgeVersion.MC_1_12_2.L() ? 1 : 0;
                AttributeModifier attackDamageModifier = new AttributeModifier(
                        modifiers.values().toArray()[attackDamageModifierIndex]);
                baseDamage += (float)attackDamageModifier.getAmount();
            }
        }
        float enchantmentDamage = EnchantmentHelper.C(itemStack, EnumCreatureAttribute.undefined());
        if (baseDamage > 0.0f || enchantmentDamage > 0.0f) {
            if (criticalHit && baseDamage > 0.0f) {
                baseDamage *= 1.5f;
            }
            return applyDamageReductions(player, targetWasBlocking, baseDamage + enchantmentDamage);
        }
        return 0.0f;
    }

    @EventHandler
    public void onUpdate(EventLivingUpdate eventLivingUpdate) {
        if (eventLivingUpdate.getEntity().isInstance(MappedClasses.Yl)
                && Minecraft.thePlayer().getDistanceToEntity(eventLivingUpdate.getEntity()) < 6.0f) {
            TrackedPlayerAttackState state = this.playerStates.get(eventLivingUpdate.getEntity().S());
            if (state != null) {
                state.markLivingUpdate();
            }
        }
    }

    @EventHandler
    public void onEntityJoinWorld(EventEntityJoinWorld event) {
        if (event.getEntity().isInstance(MappedClasses.Yl)) {
            TrackedPlayerAttackState state = this.playerStates.getOrDefault(
                    event.getEntity().S(), new TrackedPlayerAttackState(new EntityPlayer(event.getEntity())));
            if (state != null) {
                state.updatePlayer(new EntityPlayer(event.getEntity().getObject()));
                if (state.getInactivityTimer().hasTimeElapsed(10000L)) {
                    state.resetPrediction();
                }
            }
            return;
        }
        if (!event.getEntity().isInstance(MappedClasses.Zf)) {
            return;
        }
        if (Minecraft.thePlayer().isNull()) {
            return;
        }
        EntityPotion entityPotion = new EntityPotion(event.getEntity());
        if (entityPotion.getPotion().isNull()) {
            return;
        }
        boolean isHealingPotion = ItemStackScoreUtil.i(entityPotion.getPotion());
        if (!isHealingPotion) {
            return;
        }
        this.healingPotions.add(new EntityPotion(event.getEntity().getObject()));
    }

    @EventHandler
    public void onPreAttack(EventPreAttack event) {
        if (event.getTarget().isInstance(MappedClasses.Yl)) {
            TrackedPlayerAttackState state = this.playerStates.get(event.getTarget().S());
            if (state != null) {
                EntityPlayer target = new EntityPlayer(event.getTarget().getObject());
                state.recordAttack(target.o$src$Z$1iprrmi(),
                        Minecraft.thePlayer().B$src$Lvegas_sony_wrapper_impl_ItemStack_$impdvt());
            }
        }
    }

    static float applyDamageReductions(EntityPlayer player, boolean blocking, float damage) {
        if (blocking && damage > 0.0f) {
            damage = (1.0f + damage) * 0.5f;
        }
        damage = RotationUtil.m(player, damage);
        return RotationUtil.j((EntityLivingBase)player, damage);
    }

    static {
        AttackStrengthTracker.setControlFlowMarker(new int[3]);
    }

    private static double squaredDistance(Entity entity, double x, double y, double z) {
        double deltaX = entity.z() - x;
        double deltaY = entity.N() - y;
        double deltaZ = entity.h() - z;
        return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
    }

    private static float calculateInstantHealthAmount(int amplifier, double intensity) {
        return (int)(intensity * (double)(4 << amplifier) + 0.5);
    }

    public boolean isHealthPredictionEnabled() {
        return SonyVegas.INSTANCE.getClientSettings().healthPrediction.getEffectiveValue();
    }

    public boolean shouldEstimateFoodHealing() {
        return SonyVegas.INSTANCE.getClientSettings().estimateFoodHealing.getEffectiveValue();
    }

    private static float calculateInstantHealthFromPotion(EntityPlayer player, ItemStack potionStack,
            double x, double y, double z, boolean directHit) {
        double distanceSquared = squaredDistance(player, x, y, z);
        int amplifier = 0;
        if (potionStack.getItem().isInstance(MappedClasses.Di)) {
            ItemSplashPotion splashPotion = new ItemSplashPotion(potionStack.getItem().getObject());
            if (splashPotion.getRawPotionEffects(potionStack) != null
                    && ItemSplashPotion.isSplashPotion(potionStack)) {
                for (PotionEffect potionEffect : splashPotion.getPotionEffects(potionStack)) {
                if (potionEffect.C() != 6) continue;
                    amplifier = potionEffect.L();
                }
            }
        }
        if (distanceSquared < 16.0) {
            double intensity = 1.0 - Math.sqrt(distanceSquared) / 4.0;
            if (directHit) {
                intensity = 1.0;
            }
            return calculateInstantHealthAmount(amplifier, intensity);
        }
        return 0.0f;
    }

    @EventHandler
    public void onPostTick(EventPostTick event) {
        WorldClient world = Minecraft.theWorld();
        if (world.isNull()) {
            return;
        }
        if (this.trackedWorldHandle == null) {
            this.trackedWorldHandle = world.getObject();
        }
        if (!world.getObject().equals(this.trackedWorldHandle)) {
            this.healingPotions.clear();
            this.playerStates.clear();
            this.trackedWorldHandle = world.getObject();
        }
        for (Object entityHandle : world.z()) {
            if (!MappedClasses.Yl.isAssignableFrom(entityHandle.getClass())
                    || MappedClasses.z5.isAssignableFrom(entityHandle.getClass())) continue;
            EntityPlayer player = new EntityPlayer(entityHandle);
            if (this.playerStates.containsKey(player.S())) {
                this.playerStates.get(player.S()).update();
                continue;
            }
            this.playerStates.put(player.S(), new TrackedPlayerAttackState(player));
        }
        ArrayList<EntityPotion> expiredPotions = new ArrayList<EntityPotion>();
        for (EntityPotion potion : this.healingPotions) {
            if (world.z().contains(potion.getObject())) continue;
            double potionX = potion.z();
            double potionY = potion.N();
            double potionZ = potion.h();
            for (Map.Entry<Integer, TrackedPlayerAttackState> entry : this.playerStates.entrySet()) {
                Entity entity = ((World)world).V(entry.getKey());
                if (!entity.isNotNull() || !entity.isInstance(MappedClasses.Yl)) continue;
                boolean directHit = potion.N() > entity.N() + 0.5
                        && entity.getDistanceToEntity(potion) < 2.2f && potion.l() >= 5;
                float healing = calculateInstantHealthFromPotion(new EntityPlayer(entity.getObject()),
                        potion.getPotion(), potionX, potionY, potionZ, directHit);
                entry.getValue().addEstimatedHealth(healing);
            }
            expiredPotions.add(potion);
        }
        if (!expiredPotions.isEmpty()) {
            this.healingPotions.removeAll(expiredPotions);
        }
    }

    public float getEstimatedHealth(EntityPlayer player) {
        if (!this.isHealthPredictionEnabled()) {
            return player.w$src$F$15l9epb();
        }
        TrackedPlayerAttackState state = this.playerStates.get(player.S());
        return state != null ? state.getEstimatedHealth() : player.w$src$F$15l9epb();
    }

    public static void setControlFlowMarker(int[] marker) {
        controlFlowMarker = marker;
    }

    public static int[] getControlFlowMarker() {
        return controlFlowMarker;
    }


    public boolean shouldEstimateFallDamage() {
        return SonyVegas.INSTANCE.getClientSettings().estimateFallDamage.getEffectiveValue();
    }
}


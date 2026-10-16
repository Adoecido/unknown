package vegas.sony.module.combat;

import vegas.sony.SonyVegas;
import vegas.sony.config.ClientSettings;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.module.Category;
import vegas.sony.module.Mod;
import vegas.sony.module.combat.aimassist.AimAssistRotationSubModule;
import vegas.sony.module.combat.aimassist.AimAssistTargetingSubModule;
import vegas.sony.module.control.SharedModuleControlClaims;
import vegas.sony.unmap.ItemLimitData;
import vegas.sony.unmap.ModeOption;
import vegas.sony.unmap.ModeSelection;
import vegas.sony.utils.EntityAngleComparator;
import vegas.sony.utils.EntityArmorValueComparator;
import vegas.sony.utils.EntityDistanceComparator;
import vegas.sony.utils.EntityEquipmentValueComparator;
import vegas.sony.utils.EntityHealthComparator;
import vegas.sony.utils.RayTraceUtil;
import vegas.sony.utils.RotationUtil;
import vegas.sony.value.BooleanValue;
import vegas.sony.value.EntityTargetFilterValue;
import vegas.sony.value.LimitValue;
import vegas.sony.value.ModeValue;
import vegas.sony.value.NumberValue;
import vegas.sony.wrapper.impl.Entity;
import vegas.sony.wrapper.impl.EntityLivingBase;
import vegas.sony.wrapper.impl.EntityPlayerSP;
import vegas.sony.wrapper.impl.ItemStack;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.PlayerControllerMP;
import vegas.sony.wrapper.impl.RayTraceResult;
import vegas.sony.wrapper.impl.RayTraceResult_type;
import vegas.sony.wrapper.impl.WorldClient;
import java.util.ArrayList;
import java.util.Arrays;
import org.jetbrains.annotations.Nullable;

public class AimAssist
extends Mod {
    private int blockBreakCooldown = 0;
    private final BooleanValue strafeIncrease;
    public ModeValue targetArea;
    protected final ModeValue mode;
    public final ModeOption closestAreaMode;
    public final ModeOption centerMode;
    private final AimAssistTargetingSubModule adaptiveTargeting;
    public final ModeOption yawMode;
    public final ModeOption threatMode;
    private final NumberValue horizontalSpeed;
    private final AimAssistRotationSubModule simpleRotation = new AimAssistRotationSubModule(this, "Simple");
    private final LimitValue allowedItems;
    private final NumberValue maxAngle;
    private final BooleanValue limitToItems;
    public ModeValue targetMode;
    private final LimitValue blockBreakItems;
    private final BooleanValue aimVertically;
    public final ModeOption healthMode;
    private final BooleanValue breakBlocksWhitelist;
    private final BooleanValue checkBlockBreak;
    private final ModeOption distanceMode;
    private final EntityTargetFilterValue targetFilter;
    private final NumberValue verticalSpeed;
    private final BooleanValue requireMouseDown;
    public final ModeOption armorMode;
    private final NumberValue distance;

    @Nullable
    public EntityLivingBase findBestTarget() {
        WorldClient worldClient = Minecraft.theWorld();
        if (worldClient.isNull()) {
            return null;
        }
        ArrayList<EntityLivingBase> targets = new ArrayList<EntityLivingBase>();
        ArrayList<?> loadedEntities = new ArrayList<Object>(worldClient.z());
        for (Object entityObject : loadedEntities) {
            Entity entity = new Entity(entityObject);
            EntityLivingBase livingEntity;
            if (ClientSettings.IS_LEGACY_1_7 && entity.isInstance(MappedClasses.FT)
                    || !entity.isInstance(MappedClasses.zm)
                    || !this.isValidTarget(livingEntity = new EntityLivingBase(entityObject))) {
                continue;
            }
            targets.add(livingEntity);
        }
        if (this.targetMode.getValue() == this.yawMode) {
            targets.sort(new EntityAngleComparator());
        } else if (this.targetMode.getValue() == this.distanceMode) {
            targets.sort(new EntityDistanceComparator());
        } else if (this.targetMode.getValue() == this.threatMode) {
            targets.sort(new EntityArmorValueComparator());
        } else if (this.targetMode.getValue() == this.armorMode) {
            targets.sort(new EntityEquipmentValueComparator());
        } else if (this.targetMode.getValue() == this.healthMode) {
            targets.sort(new EntityHealthComparator());
        }
        if (!targets.isEmpty()) {
            return targets.get(0);
        }
        return null;
    }

    private boolean passesItemFilter(EntityLivingBase target) {
        if (this.limitToItems.getEffectiveValue().booleanValue()) {
            ItemStack itemStack = Minecraft.thePlayer().getHeldItemHand();
            if (!this.allowedItems.isValid(itemStack, false)) {
                return false;
            }
            return this.targetFilter.isValidTarget(target);
        }
        return this.targetFilter.isValidTarget(target);
    }

    public BooleanValue getStrafeIncrease() {
        return this.strafeIncrease;
    }

    public BooleanValue getRequireMouseDown() {
        return this.requireMouseDown;
    }

    public NumberValue getVerticalSpeed() {
        return this.verticalSpeed;
    }

    public boolean isValidTarget(EntityLivingBase target) {
        if (target.isNull()) {
            return false;
        }
        if (target.equals(Minecraft.thePlayer())) {
            return false;
        }
        if (target.w$src$F$15l9epb() <= 0.0f || target.M$src$Z$ff28xj()) {
            return false;
        }
        if (Minecraft.thePlayer().getDistanceToEntity(target) >= (float)((Double)this.distance.getValue()).intValue()) {
            return false;
        }
        if (RotationUtil.a(Minecraft.thePlayer(), target) > ((Double)this.maxAngle.getValue()).intValue() / 2) {
            return false;
        }
        if (SonyVegas.INSTANCE.getFriendManager().isFriend(target)) {
            return false;
        }
        if (target.equals(Minecraft.thePlayer().S$src$Lvegas_sony_wrapper_impl_Entity_$dgzs12())) {
            return false;
        }
        return this.passesItemFilter(target);
    }


    public NumberValue getHorizontalSpeed() {
        return this.horizontalSpeed;
    }

    private boolean hasRequiredItem() {
        if (!this.limitToItems.getEffectiveValue().booleanValue()) {
            return true;
        }
        ItemStack itemStack = Minecraft.thePlayer().getHeldItemHand();
        return this.allowedItems.isValid(itemStack, false);
    }

    public AimAssist() {
        super("Aim //$ui", -327674, Category.COMBAT, "Smoothly aims to closest valid target //$ui");
        this.adaptiveTargeting = new AimAssistTargetingSubModule(this, "Adaptive");
        this.mode = ModeValue.create((Object)this, "Mode //$ui", "Simple - Lightweight smooth aiming\nAdaptive - Advanced tracking with adaptive behavior //$ui", (ModeSelection)this.simpleRotation.getSelectionValue(), this.simpleRotation.getSelectionValue(), this.adaptiveTargeting.getSelectionValue());
        this.targetFilter = EntityTargetFilterValue.createForModule(this);
        this.requireMouseDown = BooleanValue.create(this, "Require mouse down //$ui", true, "Only aim while mouse is down //$ui");
        this.aimVertically = BooleanValue.create(this, "Aim vertically //$ui", false, "Aims up and down as well //$ui");
        this.strafeIncrease = BooleanValue.create(this, "Strafe increase //$ui", false, "Increase speed while strafing away from target //$ui");
        this.checkBlockBreak = BooleanValue.create(this, "Check block break //$ui", false, "Prevents from aiming while breaking blocks //$ui");
        this.breakBlocksWhitelist = BooleanValue.create(this, "Break blocks whitelist //$ui", false);
        this.blockBreakItems = LimitValue.create(this, "aimassist-blockbreak-items //$", "Items //$ui", LimitValue.ALLOW_LIST_COLOR, Arrays.asList(new ItemLimitData("pickaxes //$"), new ItemLimitData("shovels //$")));
        this.limitToItems = BooleanValue.create(this, "Limit to items //$ui", false, "Aim functions only while holding selected items //$ui");
        this.allowedItems = LimitValue.create(this, "v-4ab91e70 //$", "Allowed Items //$ui", LimitValue.ALLOW_LIST_COLOR, new ItemLimitData("swords //$"));
        this.verticalSpeed = NumberValue.create(this, "Vertical speed //$ui", "#.#", "", 1.0, 5.0, 10.0);
        this.horizontalSpeed = NumberValue.create(this, "Horizontal speed //$ui", "#.#", "", 1.0, 5.0, 10.0);
        this.maxAngle = NumberValue.create(this, "Max angle //$ui", "#", "", 1.0, 180.0, 360.0, 1.0, "Maximum allowed angle to still aim at target //$ui");
        this.distance = NumberValue.create(this, "Distance //$ui", "#.#", "", 1.0, 5.0, 8.0, 0.1, "Maximum distance allowed to still aim at target //$ui");
        this.distanceMode = new ModeOption("Distance //$ui");
        this.yawMode = new ModeOption("Yaw //$ui");
        this.armorMode = new ModeOption("Armor //$ui");
        this.threatMode = new ModeOption("Threat //$ui");
        this.healthMode = new ModeOption("Health //$ui");
        this.targetMode = ModeValue.create((Object)this, "Target mode //$ui", "How Aim should prioritize targets\nArmor/Threat will default to Distance for non player targets //$ui", (ModeSelection)this.yawMode, this.yawMode, this.distanceMode, this.armorMode, this.threatMode, this.healthMode);
        this.centerMode = new ModeOption("Center //$ui");
        this.closestAreaMode = new ModeOption("Closest //$ui");
        this.targetArea = ModeValue.create((Object)this, "Target area //$ui", "Where Aim will aim towards\nCenter: Center of entity\nClosest: Closest position on entity hitbox //$ui", (ModeSelection)this.centerMode, this.centerMode, this.closestAreaMode);
        this.aimVertically.addDependentValues(this.verticalSpeed);
        this.limitToItems.addDependentValues(this.allowedItems);
        this.limitToItems.setCompactListValue(this.allowedItems);
        this.breakBlocksWhitelist.setCompactListValue(this.blockBreakItems);
        this.breakBlocksWhitelist.addDependentValues(this.blockBreakItems);
        this.checkBlockBreak.addDependentValues(this.breakBlocksWhitelist);
        this.addValue(this.mode, this.targetFilter, this.requireMouseDown, this.strafeIncrease, this.checkBlockBreak, this.breakBlocksWhitelist, this.blockBreakItems, this.aimVertically, this.verticalSpeed, this.horizontalSpeed, this.maxAngle, this.distance, this.limitToItems, this.allowedItems, this.targetArea, this.targetMode);
        this.horizontalSpeed.setMaximumFractionDigits(0);
    }

    public BooleanValue getAimVertically() {
        return this.aimVertically;
    }

    @Override
    public String getDetailedSuffix() {
        return this.horizontalSpeed.getDisplayValue();
    }

    @Nullable
    public EntityLivingBase getCurrentTarget() {
        if (this.simpleRotation.isSelectedSubModule()) {
            return this.simpleRotation.getTarget();
        }
        if (this.adaptiveTargeting.isSelectedSubModule()) {
            return this.adaptiveTargeting.getTarget();
        }
        return null;
    }

    public NumberValue getMaxAngle() {
        return this.maxAngle;
    }

    public NumberValue getDistance() {
        return this.distance;
    }

    public boolean canAim() {
        EntityPlayerSP player = Minecraft.thePlayer();
        PlayerControllerMP playerController = Minecraft.playerController();
        if (player.isNull() || playerController.isNull()) {
            return false;
        }
        if (SharedModuleControlClaims.movementInput.isLocked()) {
            return false;
        }
        boolean checkCurrentBlock = this.checkBlockBreak.getEffectiveValue();
        if (checkCurrentBlock && this.breakBlocksWhitelist.getEffectiveValue().booleanValue()) {
            checkCurrentBlock = this.blockBreakItems.matches(player.getHeldItemHand());
        }
        if (checkCurrentBlock) {
            RayTraceResult mouseOver = RayTraceUtil.o();
            boolean aimingAtBlock = mouseOver.isNotNull() && mouseOver.getTypeOfHit().equals(RayTraceResult_type.block());
            if (aimingAtBlock) {
                this.blockBreakCooldown = 250;
                return false;
            }
            if (this.blockBreakCooldown > 0) {
                --this.blockBreakCooldown;
            }
            if (this.blockBreakCooldown > 0) {
                return false;
            }
        }
        return this.hasRequiredItem();
    }

}


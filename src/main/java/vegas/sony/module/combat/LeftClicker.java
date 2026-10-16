package vegas.sony.module.combat;

import vegas.sony.SonyVegas;
import vegas.sony.click.ClickButton;
import vegas.sony.click.ClickEngine;
import vegas.sony.event.EventHandler;
import vegas.sony.event.impl.EventPreTick;
import vegas.sony.input.InputEventDispatcher;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.module.control.SharedModuleControlClaims;
import vegas.sony.module.none.ClientSettings;
import vegas.sony.module.render.Animations;
import vegas.sony.rotation.RotationManager;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.unmap.ItemLimitData;
import vegas.sony.utils.TimerUtil;
import vegas.sony.value.BooleanValue;
import vegas.sony.value.LimitValue;
import vegas.sony.value.RandomValue;
import vegas.sony.wrapper.impl.EntityPlayerSP;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.Packet;
import vegas.sony.wrapper.impl.RayTraceResult;
import vegas.sony.wrapper.impl.RayTraceResult_type;
import java.util.Arrays;

public class LeftClicker
extends ClickerMod {
    private boolean wasClicking = false;
    private final BooleanValue jitter;
    private final LimitValue blockBreakItems;
    private final RandomValue cps;
    private final BooleanValue breakBlocks;
    private final BooleanValue triggerMode;
    private final LimitValue itemWhitelist;
    private final BooleanValue breakBlocksWhitelist;
    private final BooleanValue limitItems;
    private final BooleanValue holdToClick = BooleanValue.create(this, "Hold to click //$ui", true);
    private final TimerUtil breakBlockTimer;
    private volatile boolean blocked = false;
    private final RandomValue breakBlocksDelay;

    @Override
    public boolean shouldSimulateBlockHit(ClickEngine clickEngine, EntityPlayerSP player) {
        Animations animations = SonyVegas.INSTANCE.getModManager().getMod(Animations.class);
        return animations != null && animations.shouldBlock();
    }

    @Override
    public boolean isTriggerModeEnabled() {
        return this.triggerMode.getEffectiveValue();
    }

    private boolean computeBlocked() {
        if (!ClientSettings.INSTANCE.isInputEnabled()) {
            return true;
        }
        if (!InputEventDispatcher.getInstance().getFocusState().isFocused()) {
            return true;
        }
        if (SharedModuleControlClaims.mouseButtons.isLocked()) {
            return true;
        }
        EntityPlayerSP player = Minecraft.thePlayer();
        if (player.isNull()) {
            return true;
        }
        if (!this.shouldAllowClick(player)) {
            if (vegas.sony.config.ClientSettings.isAttackButtonDown()) {
                if (!Minecraft.gameSettings().F().isKeyDown()) {
                    // empty if block
                }
                this.suppressNextRelease = true;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean isHitSelectActive() {
        HitSelect hitSelect = SonyVegas.INSTANCE.getModManager().getMod(HitSelect.class);
        if (hitSelect == null) {
            return false;
        }
        return hitSelect.isEnabled() && hitSelect.isRightClickCancelActive();
    }

    public boolean shouldAllowClick(EntityPlayerSP player) {
        if (!vegas.sony.config.ClientSettings.isAttackButtonDown()) {
            this.breakBlockTimer.reset();
        }
        if (this.breakBlocks.getEffectiveValue().booleanValue() && this.breakBlockTimer.hasTimeElapsed((long)this.breakBlocksDelay.getRandomValue())) {
            if (Minecraft.currentScreen().isInstance(MappedClasses.Ft)) {
                return true;
            }
            if (this.breakBlocksWhitelist.getEffectiveValue().booleanValue() && !this.blockBreakItems.matches(player.getHeldItemHand())) {
                return true;
            }
            RayTraceResult rayTraceResult = RotationManager.INSTANCE.getNormalReachRayTrace();
            if (rayTraceResult.isNotNull() && rayTraceResult.getTypeOfHit().equals(RayTraceResult_type.block())) {
                return false;
            }
            this.breakBlockTimer.reset();
        }
        return true;
    }


    @Override
    public boolean isClickCycleBlocked() {
        return this.blocked;
    }

    @EventHandler
    public void updateBlockedState(EventPreTick eventPreTick) {
        this.blocked = this.computeBlocked();
        ClickEngine clickEngine = this.getClickEngine();
        if (this.blocked && InputEventDispatcher.getInstance().getFocusState().isFocused() && ClientSettings.INSTANCE.inputEnabled && Minecraft.currentScreen().isNull() && clickEngine.isActivationHeld() && !Minecraft.gameSettings().F().isKeyDown() && !this.wasClicking) {
            this.wasClicking = true;
            clickEngine.pressClickButton();
        } else {
            this.wasClicking = false;
        }
    }

    @Override
    public String getDetailedSuffix() {
        // Just the range: the HUD shows "Clicker (1-2)", no unit suffix.
        return this.cps.getDisplayValue();
    }

    public LeftClicker() {
        super("Clicker //$");
        this.jitter = BooleanValue.create(this, "Jitter //$ui", false);
        this.cps = RandomValue.create(this, "CPS //$ui", "#.#", "", 1.0, 6.0, 13.0, 20.0);
        this.limitItems = BooleanValue.create(this, "Limit items //$ui", false);
        this.itemWhitelist = LimitValue.create(this, "v-13f6b8e4 //$", "Item whitelist //$ui", LimitValue.ALLOW_LIST_COLOR, new ItemLimitData("swords //$"));
        this.triggerMode = BooleanValue.create(this, "Trigger mode //$ui", false, "Only clicks while hovering an entity //$ui");
        this.breakBlocks = BooleanValue.create(this, "Break blocks //$ui", false);
        this.breakBlocksDelay = RandomValue.create(this, "Break blocks delay //$ui", "#", "", 0.0, 0.0, 10.0, 2000.0);
        this.breakBlocksWhitelist = BooleanValue.create(this, "Break blocks whitelist //$ui", false);
        this.blockBreakItems = LimitValue.create(this, "autoclicker-blockbreak-items //$", "Items //$ui", LimitValue.ALLOW_LIST_COLOR, Arrays.asList(new ItemLimitData("pickaxes //$"), new ItemLimitData("shovels //$")));
        this.breakBlockTimer = new TimerUtil();
        this.limitItems.addDependentValues(this.itemWhitelist);
        this.limitItems.setCompactListValue(this.itemWhitelist);
        this.breakBlocks.addDependentValues(this.breakBlocksDelay, this.breakBlocksWhitelist);
        this.breakBlocksWhitelist.setCompactListValue(this.blockBreakItems);
        this.breakBlocksWhitelist.addDependentValues(this.blockBreakItems);
        this.addValue(this.holdToClick, this.triggerMode, this.breakBlocks, this.breakBlocksDelay, this.breakBlocksWhitelist, this.blockBreakItems, this.cps, this.jitter, this.limitItems, this.itemWhitelist);
        ClickEngine clickEngine = new ClickEngine(ClickButton.LEFT, this.cps, this.limitItems,
                this.itemWhitelist, this.holdToClick, null, this.jitter, this);
        this.setClickEngine(clickEngine);
        this.cps.setMaximumFractionDigits(0);
    }

    @Override
    public boolean matchesName(String candidate) {
        return super.matchesName(candidate) || "AutoClicker".equalsIgnoreCase(candidate);
    }
}

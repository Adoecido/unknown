package vegas.sony.module.combat;

import vegas.sony.click.ClickButton;
import vegas.sony.click.ClickEngine;
import vegas.sony.click.ClickerWorker;
import vegas.sony.event.EventHandler;
import vegas.sony.event.impl.EventPreRenderTick;
import vegas.sony.event.impl.EventPreTick;
import vegas.sony.input.InputEventDispatcher;
import vegas.sony.module.Category;
import vegas.sony.module.Mod;
import vegas.sony.rotation.RotationManager;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.utils.SleepUtil;
import vegas.sony.utils.TimerUtil;
import vegas.sony.wrapper.impl.EntityPlayerSP;
import vegas.sony.wrapper.impl.ForgeVersion;
import vegas.sony.wrapper.impl.GuiScreen;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.RayTraceResult;
import vegas.sony.wrapper.impl.RayTraceResult_type;

public class ClickerMod
extends Mod {
    private long activationStartTime;
    private boolean activationWasHeld;
    private static final long MODULE_ID = 7524649824893733649L;
    protected volatile boolean suppressNextRelease;
    private ClickEngine clickEngine;
    private final ClickerWorker clickerWorker = new ClickerWorker(this);

    public boolean isHitSelectActive() {
        return false;
    }

    public boolean shouldBlockClick(EntityPlayerSP player) {
        return false;
    }

    public boolean shouldSimulateBlockHit(ClickEngine clickEngine, EntityPlayerSP player) {
        return false;
    }

    public boolean isTriggerModeEnabled() {
        return false;
    }

    @Override
    public void onEnable() {
        this.clickerWorker.startOrResume();
    }

    @Override
    public void onDisable() {
        this.clickerWorker.pause();
    }


    public ClickerMod(String moduleName, int moduleId, Category category) {
        super(moduleName, moduleId, category, "");
    }

    public ClickerMod(String moduleName) {
        super(moduleName, (int)MODULE_ID, Category.COMBAT, "");
    }

    public void setClickEngine(ClickEngine clickEngine) {
        this.clickEngine = clickEngine;
    }

    public ClickEngine getClickEngine() {
        return this.clickEngine;
    }

    @Override
    public String getDetailedSuffix() {
        if (this.clickEngine == null) {
            return "";
        }
        int minimum = this.clickEngine.getCpsRange().getMinimumInt();
        int maximum = this.clickEngine.getCpsRange().getMaximumInt();
        if (minimum == maximum) {
            return minimum + " CPS";
        }
        return minimum + "-" + maximum + " CPS";
    }

    public boolean isClickCycleBlocked() {
        return false;
    }

    public boolean shouldDeferAttack(EntityPlayerSP player) {
        RayTraceResult rayTraceResult = RotationManager.INSTANCE.getExtendedReachRayTrace();
        GuiScreen guiScreen = Minecraft.currentScreen();
        boolean isEntityHit = rayTraceResult.isNotNull()
                && rayTraceResult.getTypeOfHit().equals(RayTraceResult_type.entity());
        if (isEntityHit) {
            if (this.isTriggerModeEnabled() && ForgeVersion.MC_1_12_2.d()
                    && Minecraft.thePlayer().getCooledAttackStrength(0.5f) < 1.0f) {
                return true;
            }
            if (this.isHitSelectActive() && rayTraceResult.getEntity().isNotNull()
                    && player.b$src$Z$fqlxe4() && rayTraceResult.getEntity().V$src$I$fk0dv5() > 12) {
                return true;
            }
            return false;
        }

        if (this.isTriggerModeEnabled()) {
            if (guiScreen.isNull()) {
                return true;
            }
            if (ForgeVersion.MC_1_12_2.d()
                    && Minecraft.thePlayer().getCooledAttackStrength(0.5f) < 1.0f) {
                return true;
            }
        }
        return false;
    }

    public double getStartDelayMillis() {
        return 0.0;
    }

    @EventHandler
    public final void onTick(EventPreTick eventPreTick) {
        this.clickEngine.updateJitter(eventPreTick);
    }

    @EventHandler
    public final void onPreRenderTick(EventPreRenderTick eventPreRenderTick) {
        this.clickEngine.applyJitterRotation(eventPreRenderTick);
    }

    public void runClickCycle() {
        EntityPlayerSP player = Minecraft.thePlayer();
        GuiScreen guiScreen = Minecraft.currentScreen();
        if (player.isNull() || !this.isEnabled() || guiScreen.isNotNull()) {
            return;
        }
        if (!InputEventDispatcher.getInstance().getFocusState().isFocused()) {
            return;
        }
        if (!this.clickEngine.isActivationHeld()) {
            this.activationWasHeld = false;
            return;
        }
        if (!this.activationWasHeld) {
            this.activationWasHeld = true;
            this.activationStartTime = System.currentTimeMillis();
        }
        long activationDelayMillis = 50L + (long)this.getStartDelayMillis();
        if (System.currentTimeMillis() - this.activationStartTime < activationDelayMillis) {
            return;
        }
        if (!this.clickEngine.canClick(player)) {
            return;
        }
        if (this.shouldBlockClick(player)) {
            return;
        }

        boolean isHighCps = this.clickEngine.getCpsRange().getMinimumInt() > 20;
        long clickDelayMillis = Math.max(0L, this.clickEngine.calculateNextClickDelay() - 5L);
        if (!isHighCps && clickDelayMillis <= 50L) clickDelayMillis = 45L;
        double releaseFraction = 0.5;
        long holdDurationMillis = Math.max(0L, (long)(clickDelayMillis * (1.0 - releaseFraction)));
        long releaseDurationMillis = Math.max(0L, (long)(clickDelayMillis * releaseFraction));
        if (!this.clickEngine.isActivationHeld()) {
            this.activationWasHeld = false;
            return;
        }
        if (!this.isEnabled() || Minecraft.currentScreen().isNotNull() || !Minecraft.a()) return;
        if (this.clickEngine.getButton() == ClickButton.LEFT && this.shouldDeferAttack(player)) return;
        boolean simulateBlockHit = this.shouldSimulateBlockHit(this.clickEngine, player);
        boolean completed = false;
        this.suppressNextRelease = false;
        TimerUtil phaseTimer = new TimerUtil();
        try {
            this.clickEngine.pressClickButton();
            if (simulateBlockHit) this.clickEngine.pressUseItem();
            SleepUtil.sleep(holdDurationMillis);
            completed = true;
        } finally {
            phaseTimer.reset();
            try {
                if (simulateBlockHit) this.clickEngine.releaseUseItem();
            } finally {
                if (!completed || !this.suppressNextRelease) this.clickEngine.releaseClickButton();
                this.suppressNextRelease = false;
            }
        }
        if (isHighCps) releaseDurationMillis = Math.max(0L, releaseDurationMillis - phaseTimer.getLastMS());
        SleepUtil.sleep(releaseDurationMillis);
    }
}

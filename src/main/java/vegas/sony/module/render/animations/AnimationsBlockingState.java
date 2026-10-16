package vegas.sony.module.render.animations;

import vegas.sony.SonyVegas;
import vegas.sony.combat.AttackPacketTimingTracker;
import vegas.sony.config.ClientSettings;
import vegas.sony.event.EventHandler;
import vegas.sony.event.impl.EventMouseButton;
import vegas.sony.event.impl.EventPreTick;
import vegas.sony.module.Mod;
import vegas.sony.module.combat.LeftClicker;
import vegas.sony.module.combat.SilentAura;
import vegas.sony.module.render.Animations;
import vegas.sony.value.RandomValue;
import vegas.sony.wrapper.impl.Minecraft;

public class AnimationsBlockingState
extends AnimationsMode {
    private boolean blocking = false;
    private long releaseTime;
    private final RandomValue chance = RandomValue.createWithDescription(this, "Chance //$ui", "#", "%", 0.0, 70.0, 90.0, 100.0, 1.0, "Chance that a click will blockhit\n(Blocks per second = Your CPS * Chance) //$ui");

    public AnimationsBlockingState(Mod parent, String name) {
        super(parent, name);
        this.addValue(this.chance);
    }

    @Override
    public boolean shouldBlock() {
        if (!((Animations)this.getParent()).isHoldingSword()) {
            return false;
        }
        if (((Animations)this.getParent()).requiresMouseDown() && !ClientSettings.isUseItemButtonDown()) {
            return false;
        }
        return this.chance.getRandomValue() >= Math.random() * 100.0;
    }

    public void setBlocking(boolean blocking) {
        if (this.blocking != blocking) {
            this.blocking = blocking;
            this.releaseTime = 0L;
            Minecraft.gameSettings().b$src$Lvegas_sony_wrapper_impl_KeyBinding_$1yi3362().setPressed(blocking);
        }
    }

    @Override
    public String getDetailedSuffix() {
        return this.chance.getDisplayValue();
    }

    @Override
    public boolean isBlocking() {
        return this.blocking;
    }

    @EventHandler
    public void onTick(EventPreTick event) {
        if (this.isAutoClickerActive()) {
            return;
        }
        if (Minecraft.thePlayer().isNull()) {
            return;
        }
        boolean hurtTimeExpired = event.getThePlayer().c$src$I$15a9iwo() > AttackPacketTimingTracker.INSTANCE.getExpectedHurtTimeTicks() + 1;
        boolean releaseTimeReached = this.releaseTime > 0L && System.currentTimeMillis() >= this.releaseTime;
        if (hurtTimeExpired || releaseTimeReached) {
            this.setBlocking(false);
            return;
        }
    }


    @EventHandler
    public void onMouseButton(EventMouseButton event) {
        if (!event.getButtonState()) {
            return;
        }
        if (this.isAutoClickerActive()) {
            return;
        }
        int buttonBinding = -100 + event.getButton();
        if (event.getButtonState() && buttonBinding == Minecraft.gameSettings().F().getKeyCode()) {
            if (!this.shouldBlock()) {
                return;
            }
            if (!this.blocking && !event.getThePlayer().o$src$Z$1iprrmi()) {
                this.setBlocking(true);
                this.releaseTime = System.currentTimeMillis() + 50L;
            }
        }
    }

    private boolean isAutoClickerActive() {
        LeftClicker leftClicker = SonyVegas.INSTANCE.getModManager().getMod(LeftClicker.class);
        if (leftClicker.isEnabled()) {
            return true;
        }
        SilentAura silentAura = SonyVegas.INSTANCE.getModManager().getMod(SilentAura.class);
        return silentAura.isEnabled();
    }
}
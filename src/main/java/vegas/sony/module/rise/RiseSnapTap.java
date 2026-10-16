package vegas.sony.module.rise;

import vegas.sony.event.EventHandler;
import vegas.sony.event.EventPriority;
import vegas.sony.event.impl.EventPreLivingTravel;
import vegas.sony.module.Category;
import vegas.sony.module.Mod;
import vegas.sony.module.blatant.invwalk.InvWalkSettingsState;
import vegas.sony.wrapper.impl.GameSettings;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.MovementInput;

public class RiseSnapTap extends Mod {
    private boolean lastForward;
    private boolean lastBack;
    private boolean lastLeft;
    private boolean lastRight;
    private long forwardTime;
    private long backTime;
    private long leftTime;
    private long rightTime;

    public RiseSnapTap() {
        super("SnapTap //$ui", 9133823, Category.MISC,
                "Rise 6.9.5 SOCD cleaning: the latest opposing direction wins //$ui");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLivingTravel(EventPreLivingTravel event) {
        if (Minecraft.thePlayer().isNull()) {
            return;
        }
        if (Minecraft.currentScreen().isNotNull()) {
            // Leave screen movement to InvMove (including its bypass modes).
            // SnapTap must not erase the input another module just supplied.
            this.resetInputState();
            return;
        }
        GameSettings settings = Minecraft.gameSettings();
        boolean forward = InvWalkSettingsState.isPhysicallyPressed(settings.Y());
        boolean back = InvWalkSettingsState.isPhysicallyPressed(settings.s());
        boolean left = InvWalkSettingsState.isPhysicallyPressed(
                settings.x$src$Lvegas_sony_wrapper_impl_KeyBinding_$1cf7isg());
        boolean right = InvWalkSettingsState.isPhysicallyPressed(
                settings.g$src$Lvegas_sony_wrapper_impl_KeyBinding_$qqn5n3());
        long now = System.currentTimeMillis();
        if (forward && !this.lastForward) this.forwardTime = now;
        if (back && !this.lastBack) this.backTime = now;
        if (left && !this.lastLeft) this.leftTime = now;
        if (right && !this.lastRight) this.rightTime = now;

        MovementInput input = Minecraft.thePlayer().movementInput();
        float movementScale = this.getVanillaMovementScale(input);
        if (forward && back) {
            input.B(this.forwardTime == this.backTime ? 0.0f
                    : (this.forwardTime > this.backTime ? movementScale : -movementScale));
        }
        if (left && right) {
            input.M(this.leftTime == this.rightTime ? 0.0f
                    : (this.leftTime > this.rightTime ? movementScale : -movementScale));
        }

        this.lastForward = forward;
        this.lastBack = back;
        this.lastLeft = left;
        this.lastRight = right;
    }

    private float getVanillaMovementScale(MovementInput input) {
        float scale = Math.max(Math.abs(input.D()), Math.abs(input.T()));
        if (scale > 1.0E-4f) {
            return scale;
        }
        scale = input.D$src$Z$v5d6e8() ? 0.3f : 1.0f;
        if (Minecraft.thePlayer().l$src$Z$1io4duf()) {
            scale *= 0.2f;
        }
        return scale;
    }

    @Override
    public void onDisable() {
        this.resetInputState();
    }

    private void resetInputState() {
        this.lastForward = false;
        this.lastBack = false;
        this.lastLeft = false;
        this.lastRight = false;
        this.forwardTime = 0L;
        this.backTime = 0L;
        this.leftTime = 0L;
        this.rightTime = 0L;
    }
}

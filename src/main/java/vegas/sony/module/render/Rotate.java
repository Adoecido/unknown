package vegas.sony.module.render;

import vegas.sony.module.Category;
import vegas.sony.module.Mod;
import vegas.sony.value.BooleanValue;

/**
 * Displays the silent rotation produced by combat modules (SilentAura, HitFlick,
 * anything else that drives the {@code RotationManager}) instead of hiding it.
 *
 * <p>This is only a switchboard: {@code RotationManager} owns both render paths and
 * asks this module whether they are wanted. The same two toggles also exist as
 * client settings, and either source turning one on is enough.
 */
public final class Rotate extends Mod {
    private final BooleanValue playerModel;
    private final BooleanValue aimIndicator;

    public Rotate() {
        super("Rotate //$ui", 9133823, Category.RENDER,
                "Shows module rotations on your player model and on screen //$ui");
        this.playerModel = BooleanValue.create(this, "Player model //$ui", true,
                "In 3rd person aims your 3D model where you are aiming silently //$ui");
        this.aimIndicator = BooleanValue.create(this, "Aim indicator //$ui", true,
                "Draws a line on screen showing where you are aiming silently //$ui");
        this.addValue(this.playerModel, this.aimIndicator);
    }

    /** True while the local player model should be drawn with the silent rotation. */
    public boolean showsPlayerModel() {
        return this.isEnabled() && this.playerModel.getEffectiveValue().booleanValue();
    }

    /** True while the on screen silent aim line should be drawn. */
    public boolean showsAimIndicator() {
        return this.isEnabled() && this.aimIndicator.getEffectiveValue().booleanValue();
    }
}

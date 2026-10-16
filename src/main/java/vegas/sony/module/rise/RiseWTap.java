package vegas.sony.module.rise;

import vegas.sony.module.Category;
import vegas.sony.module.Mod;
import vegas.sony.module.rise.wtap.RiseWTapLegitMode;
import vegas.sony.module.rise.wtap.RiseWTapSilentMode;
import vegas.sony.unmap.ModeSelection;
import vegas.sony.value.ModeValue;
import vegas.sony.value.NumberValue;
import vegas.sony.value.SubModuleValue;

public class RiseWTap extends Mod {
    private final SubModuleValue<RiseWTapLegitMode> legitMode =
            new RiseWTapLegitMode(this, "Legit").getSelectionValue();
    private final SubModuleValue<RiseWTapSilentMode> silentMode =
            new RiseWTapSilentMode(this, "Silent").getSelectionValue();
    private final ModeValue mode = ModeValue.create(this, "Mode //$ui", this.legitMode,
            this.legitMode, this.silentMode);
    private final NumberValue chance = NumberValue.create(this, "WTap Chance //$ui", "#", "%",
            0.0, 100.0, 100.0, 1.0);

    public RiseWTap() {
        super("WTap //$ui", 9133823, Category.COMBAT,
                "Rise 6.9.5 sprint reset with Legit and Silent modes //$ui");
        this.chance.setMaximumFractionDigits(0);
        this.addValue(this.mode, this.chance);
    }

    public boolean rollChance() {
        return Math.random() * 100.0 < this.chance.getValue().doubleValue();
    }

    public NumberValue getChance() {
        return this.chance;
    }

    @Override
    public String getSimpleSuffix() {
        return ((ModeSelection)this.mode.getValue()).getName();
    }
}

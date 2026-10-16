package vegas.sony.module.combat;

import vegas.sony.module.Category;
import vegas.sony.module.Mod;
import vegas.sony.module.combat.wtap.WTapRightClickUseCancelMode;
import vegas.sony.module.combat.wtap.WTapSprintResetMode;
import vegas.sony.unmap.ModeSelection;
import vegas.sony.value.ModeValue;
import vegas.sony.value.NumberValue;
import vegas.sony.value.SubModuleValue;

public class HitSelect
extends Mod {
    private final SubModuleValue<WTapRightClickUseCancelMode> rightClickUseCancel = new WTapRightClickUseCancelMode(this, "Right-click use cancel").getSelectionValue();
    private final NumberValue chance;
    private final SubModuleValue<WTapSprintResetMode> sprintReset = new WTapSprintResetMode(this, "Normal").getSelectionValue();
    private final ModeValue mode = ModeValue.create((Object)this, "Mode //$ui", this.rightClickUseCancel, this.rightClickUseCancel, this.sprintReset);

    @Override
    public String getDetailedSuffix() {
        return this.getSimpleSuffix() + " " + this.chance.getDisplayValue() + "%";
    }


    public HitSelect() {
        super("HitSelect //$ui", 82881230, Category.COMBAT, "");
        this.chance = NumberValue.create(this, "Chance //$ui", "#", "%", 0.0, 90.0, 100.0);
        this.addValue(this.mode, this.chance);
        this.mode.setDescription("Mode //$ui");
        this.chance.setMaximumFractionDigits(0);
    }

    public boolean shouldTrigger() {
        return (Double)this.chance.java_lang_Object_K() >= Math.random() * 100.0;
    }

    public boolean isRightClickCancelActive() {
        if (!this.rightClickUseCancel.isSelected()) {
            return false;
        }
        return this.rightClickUseCancel.getInstance().isCancelActive();
    }

    @Override
    public String getSimpleSuffix() {
        return ((ModeSelection)this.mode.java_lang_Object_K()).getName();
    }
}

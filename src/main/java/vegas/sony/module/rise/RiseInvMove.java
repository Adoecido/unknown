package vegas.sony.module.rise;

import vegas.sony.module.Category;
import vegas.sony.module.Mod;
import vegas.sony.module.blatant.invwalk.InvWalkSettingsState;
import vegas.sony.module.none.ClientSettings;
import vegas.sony.module.rise.invmove.RiseInvMoveGrim2Mode;
import vegas.sony.module.rise.invmove.RiseInvMoveGrimMode;
import vegas.sony.module.rise.invmove.RiseInvMoveNormalMode;
import vegas.sony.unmap.ModeSelection;
import vegas.sony.value.ModeValue;
import vegas.sony.value.SubModuleValue;
import vegas.sony.wrapper.impl.GameSettings;
import vegas.sony.wrapper.impl.Minecraft;

public class RiseInvMove extends Mod {
    private final SubModuleValue<RiseInvMoveNormalMode> normalMode =
            new RiseInvMoveNormalMode(this, "Normal").getSelectionValue();
    private final SubModuleValue<RiseInvMoveGrimMode> grimMode =
            new RiseInvMoveGrimMode(this, "Grim").getSelectionValue();
    private final SubModuleValue<RiseInvMoveGrim2Mode> grim2Mode =
            new RiseInvMoveGrim2Mode(this, "Grim 2").getSelectionValue();
    private final ModeValue mode = ModeValue.create(this, "Bypass Mode //$ui", this.normalMode,
            this.normalMode, this.grimMode, this.grim2Mode);

    public RiseInvMove() {
        super("InvMove //$ui", 9133823, Category.MISC,
                "Rise 6.9.5 inventory movement with Normal, Grim and Grim 2 modes //$ui");
        this.addValue(this.mode);
    }

    public boolean shouldHandleCurrentScreen() {
        if (Minecraft.currentScreen().isNull() || ClientSettings.activeComponent != null) {
            return false;
        }
        return !Minecraft.currentScreen().isInstance(vegas.sony.mapping.MappedClasses.qo);
    }

    public void synchronizeMovementKeys() {
        GameSettings settings = Minecraft.gameSettings();
        InvWalkSettingsState.synchronizeKey(settings.Y());
        InvWalkSettingsState.synchronizeKey(settings.s());
        InvWalkSettingsState.synchronizeKey(settings.x$src$Lvegas_sony_wrapper_impl_KeyBinding_$1cf7isg());
        InvWalkSettingsState.synchronizeKey(settings.g$src$Lvegas_sony_wrapper_impl_KeyBinding_$qqn5n3());
        InvWalkSettingsState.synchronizeKey(settings.O());
    }

    @Override
    public void onDisable() {
        this.synchronizeMovementKeys();
    }

    @Override
    public String getSimpleSuffix() {
        return ((ModeSelection)this.mode.getValue()).getName();
    }
}

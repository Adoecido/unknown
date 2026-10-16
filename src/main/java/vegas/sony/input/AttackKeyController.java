package vegas.sony.input;

import vegas.sony.event.impl.SyntheticAttackRequestEvent;
import vegas.sony.input.KeyBindingHelper;
import vegas.sony.module.Mod;
import vegas.sony.wrapper.impl.KeyBinding;
import vegas.sony.wrapper.impl.Minecraft;

public class AttackKeyController {
    public static void releaseAttackKey() {
        KeyBinding attackKey = Minecraft.a_w3_0_S().F();
        KeyBindingHelper.updateKeyBinding(attackKey, false, false);
    }


    public static boolean requestSyntheticAttack(Mod module) {
        SyntheticAttackRequestEvent event = new SyntheticAttackRequestEvent(module);
        if (event.fire()) {
            return false;
        }
        AttackKeyController.pressAttackKey();
        return true;
    }

    /**
     * Generates the attack through the game window's mouse-input path. Client
     * CPS overlays observe this path in the same way as a physical click.
     */
    public static boolean requestSyntheticMouseAttack(Mod module) {
        SyntheticAttackRequestEvent event = new SyntheticAttackRequestEvent(module, true);
        if (event.fire()) {
            return false;
        }
        KeyBindingInputState.pressAttackKey(true);
        return true;
    }

    public static void pressAttackKey() {
        KeyBindingHelper.updateKeyBinding(Minecraft.a_w3_0_S().F(), true, true);
    }
}


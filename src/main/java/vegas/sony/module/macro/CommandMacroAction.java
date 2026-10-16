package vegas.sony.module.macro;

import vegas.sony.module.macro.CommandMacro;
import vegas.sony.module.macro.MacroAction;
import vegas.sony.wrapper.impl.Minecraft;

class CommandMacroAction
implements MacroAction {
    private final CommandMacro macro;

    CommandMacroAction(CommandMacro commandMacro) {
        this.macro = commandMacro;
    }

    @Override
    public boolean isFinished() {
        return true;
    }

    @Override
    public void tick() {
        Minecraft.a_xH_J().sendChatMessage(this.macro.getName());
    }
}


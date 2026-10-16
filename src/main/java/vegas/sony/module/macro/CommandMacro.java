package vegas.sony.module.macro;

import vegas.sony.module.Macro;
import vegas.sony.module.macro.CommandMacroAction;
import vegas.sony.module.macro.MacroAction;

public class CommandMacro
extends Macro {
    @Override
    public MacroAction createAction() {
        return new CommandMacroAction(this);
    }

    public CommandMacro(String name) {
        super(name);
    }
}


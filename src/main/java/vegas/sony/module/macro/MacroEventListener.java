package vegas.sony.module.macro;

import vegas.sony.SonyVegas;
import vegas.sony.event.EventHandler;
import vegas.sony.event.EventListener;
import vegas.sony.event.impl.EventKeyPress;
import vegas.sony.event.impl.EventMouseButton;
import vegas.sony.event.impl.EventPreTick;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.module.Macro;
import vegas.sony.module.macro.MacroAction;
import vegas.sony.wrapper.impl.Minecraft;
import java.util.List;

public class MacroEventListener
implements EventListener {
    private MacroAction activeAction;

    private boolean startMacro(Macro macro) {
        MacroAction nextAction = macro.createAction();
        if (nextAction == null) {
            return false;
        }
        if (this.activeAction != null) {
            this.activeAction.cancel();
            nextAction.inheritState(this.activeAction);
        }
        this.activeAction = nextAction;
        return true;
    }

    @EventHandler
    public void onTick(EventPreTick event) {
        if (this.activeAction == null) {
            return;
        }
        this.activeAction.tick();
        if (this.activeAction.isFinished()) {
            this.activeAction = null;
        }
    }


    @EventHandler
    public void onMouseButton(EventMouseButton event) {
        if (event.getButtonState()) {
            int binding = -100 + event.getButton();
            List<Macro> macros = SonyVegas.INSTANCE.getMacrosManager().getMacros(binding);
            for (Macro macro : macros) {
                if (macro.activateIfMatched(binding) && this.startMacro(macro)) {
                    break;
                }
            }
        }
    }

    @EventHandler
    public void onKeyPress(EventKeyPress event) {
        if (event.isDown()) {
            return;
        }
        if (event.getThePlayer().isNull()) {
            return;
        }
        if (Minecraft.a_pt_1_w().isInstance(MappedClasses.qo)) {
            return;
        }
        int binding = event.getKey();
        List<Macro> macros = SonyVegas.INSTANCE.getMacrosManager().getMacros(binding);
        for (Macro macro : macros) {
            if (macro.activateIfMatched(binding) && this.startMacro(macro)) {
                break;
            }
        }
    }
}


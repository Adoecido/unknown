package vegas.sony.ui.click.frame.impl;

import vegas.sony.SonyVegas;
import vegas.sony.module.Macro;
import vegas.sony.module.none.ClientSettings;
import vegas.sony.ui.click.component.GuiKeyTypedListener;
import vegas.sony.ui.click.frame.impl.FrameMacros;
import vegas.sony.ui.click.frame.impl.FrameMacrosAddMacroInputComponent;
import vegas.sony.ui.click.frame.impl.FrameMacrosEditor;

class FrameMacrosAddMacroKeyTypedListener
implements GuiKeyTypedListener {
    final FrameMacros C;
    final FrameMacrosAddMacroInputComponent d;


    FrameMacrosAddMacroKeyTypedListener(FrameMacrosAddMacroInputComponent frameMacrosAddMacroInputComponent, FrameMacros frameMacros) {
        this.d = frameMacrosAddMacroInputComponent;
        this.C = frameMacros;
    }

    @Override
    public void onKeyTyped(char c, int n) {
        if (FrameMacrosAddMacroInputComponent.p$src$Z$xrofzd(this.d)) {
            this.d.submit();
            if (FrameMacrosAddMacroInputComponent.j(this.d) != null) {
                Macro macro = Macro.create(FrameMacrosAddMacroInputComponent.j(this.d));
                if (SonyVegas.INSTANCE.getMacrosManager().getMacro(FrameMacrosAddMacroInputComponent.j(this.d)) != null) {
                    this.d.setText("");
                    ClientSettings.activeComponent = null;
                    return;
                }
                ClientSettings.activeComponent = null;
                FrameMacrosAddMacroInputComponent.w(this.d, new FrameMacrosEditor(this.C, macro));
                FrameMacrosAddMacroInputComponent.p(this.d).N$src$V$13y6z98();
                this.d.addChildren(FrameMacrosAddMacroInputComponent.p(this.d));
            }
            this.d.setText("");
        }
    }
}


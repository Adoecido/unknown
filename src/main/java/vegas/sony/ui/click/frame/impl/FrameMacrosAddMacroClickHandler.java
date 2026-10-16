package vegas.sony.ui.click.frame.impl;

import vegas.sony.SonyVegas;
import vegas.sony.module.Macro;
import vegas.sony.module.none.ClientSettings;
import vegas.sony.ui.click.component.GuiClickListener;
import vegas.sony.ui.click.frame.impl.FrameMacros;
import vegas.sony.ui.click.frame.impl.FrameMacrosAddMacroInputComponent;
import vegas.sony.ui.click.frame.impl.FrameMacrosEditor;

class FrameMacrosAddMacroClickHandler
implements GuiClickListener {
    final FrameMacrosAddMacroInputComponent P;
    final FrameMacros D;


    @Override
    public void onPrimaryClick() {
        if (FrameMacrosAddMacroInputComponent.p$src$Z$xrofzd(this.P)) {
            this.P.submit();
            if (FrameMacrosAddMacroInputComponent.j(this.P) != null) {
                Macro macro = Macro.create(FrameMacrosAddMacroInputComponent.j(this.P));
                if (SonyVegas.INSTANCE.getMacrosManager().getMacro(FrameMacrosAddMacroInputComponent.j(this.P)) != null) {
                    this.P.setText("");
                    ClientSettings.activeComponent = null;
                    return;
                }
                ClientSettings.activeComponent = null;
                FrameMacrosAddMacroInputComponent.w(this.P, new FrameMacrosEditor(this.D, macro));
                FrameMacrosAddMacroInputComponent.p(this.P).N$src$V$13y6z98();
                this.P.addChildren(FrameMacrosAddMacroInputComponent.p(this.P));
            }
            this.P.setText("");
        }
    }

    FrameMacrosAddMacroClickHandler(FrameMacrosAddMacroInputComponent frameMacrosAddMacroInputComponent, FrameMacros frameMacros) {
        this.P = frameMacrosAddMacroInputComponent;
        this.D = frameMacros;
    }
}


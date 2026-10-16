package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingField;
import vegas.sony.mapping.MappingMethod;
import vegas.sony.mapping.mappings.MTextComponentTranslationBridge;
import vegas.sony.ui.click.component.GuiComponent;

public class MGlStateManagerFogStateObject
extends Mapping {
    private final MappingMethod resetMethod;
    private final MappingField currentField;

    public Object getCurrent(Object fogStateHandle) {
        return this.currentField.getObject(fogStateHandle);
    }

    public MGlStateManagerFogStateObject() {
        this(MTextComponentTranslationBridge.isControlFlowStateEnabled());
    }

    private MGlStateManagerFogStateObject(boolean controlFlowState) {
        super(MappedClasses.i);
        this.currentField = this.J("current", true, MappedClasses.zM);
        this.resetMethod = this.Y("reset", true, Void.TYPE);
        if (controlFlowState) {
            return;
        }
        GuiComponent.setLegacyComponentState(new GuiComponent[4]);
    }


    public void reset(Object fogStateHandle) {
        this.resetMethod.invokeVoidNoArgs(fogStateHandle);
    }
}


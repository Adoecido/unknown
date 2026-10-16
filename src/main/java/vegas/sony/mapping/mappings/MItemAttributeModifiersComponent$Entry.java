package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingField;
import vegas.sony.mapping.mappings.MItemAttributeModifiersComponent;
import vegas.sony.ui.click.component.GuiComponent;

public class MItemAttributeModifiersComponent$Entry
extends Mapping {
    private final MappingField attributeField;
    private final MappingField modifierField;

    public Object getModifier(Object entry) {
        return this.modifierField.getObject(entry);
    }

    public Object getAttribute(Object entry) {
        return this.attributeField.getObject(entry);
    }

    public MItemAttributeModifiersComponent$Entry() {
        this(MItemAttributeModifiersComponent.getItemAttributeModifiersControlFlowState());
    }

    private MItemAttributeModifiersComponent$Entry(GuiComponent[] controlFlowState) {
        super(MappedClasses.zP);
        this.attributeField = this.J("attribute", true, MappedClasses.Vo);
        this.modifierField = this.J("modifier", true, MappedClasses.z_);
        if (GuiComponent.getLegacyComponentState() == null) {
            MItemAttributeModifiersComponent.setItemAttributeModifiersControlFlowState(new GuiComponent[5]);
        }
    }

}


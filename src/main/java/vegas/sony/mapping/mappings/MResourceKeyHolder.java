package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingField;
import vegas.sony.mapping.mappings.MEquippable;
import vegas.sony.ui.click.component.GuiComponent;

public class MResourceKeyHolder
extends Mapping {
    private static final String GOLD_FIELD_NAME = "GOLD";
    private final MappingField goldField;


    public MResourceKeyHolder() {
        this(MEquippable.p());
    }

    private MResourceKeyHolder(String controlFlowMarker) {
        super(MappedClasses.qL);
        this.goldField = this.registerStaticField(GOLD_FIELD_NAME, true, MappedClasses.qB);
        if (GuiComponent.getLegacyComponentState() == null) {
            MEquippable.z("Xwix5b");
        }
    }

    public Object getGold() {
        return this.goldField.getObject(null);
    }
}


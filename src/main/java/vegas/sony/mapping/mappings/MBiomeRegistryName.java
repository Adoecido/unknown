package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingField;
import vegas.sony.mapping.mappings.MBiomeRegistrySwitch;
import vegas.sony.ui.click.component.GuiComponent;

public class MBiomeRegistryName
extends Mapping {
    private static final String NAME_FIELD_NAME = "name";
    private final MappingField nameField;


    public MBiomeRegistryName() {
        this(MBiomeRegistrySwitch.getBiomeRegistryControlFlowState());
    }

    private MBiomeRegistryName(int[] biomeRegistryControlFlowState) {
        super(MappedClasses.h);
        this.nameField = this.J(NAME_FIELD_NAME, true, String.class);
        if (biomeRegistryControlFlowState != null) {
            return;
        }
        GuiComponent.setLegacyComponentState(new GuiComponent[5]);
    }

    private String readName(Object registryName) {
        return (String)this.nameField.getObject(registryName);
    }

    public static String getName(MBiomeRegistryName mapping, Object registryName) {
        return mapping.readName(registryName);
    }
}


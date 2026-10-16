package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingField;
import vegas.sony.mapping.mappings.MGuiContainer;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.wrapper.impl.ForgeVersion;

public class MGuiChest
extends Mapping {
    private final MappingField lowerChestInventoryField;

    public Object getLowerChestInventory(Object screenHandle) {
        return this.lowerChestInventoryField.getObject(screenHandle);
    }

    public MGuiChest() {
        this(MGuiContainer.l());
    }

    private MGuiChest(String[] legacyMappingState) {
        super(MappedClasses.qs);
        if (legacyMappingState != null) {
            if (ForgeVersion.MC_1_16_5.d()) {
                this.lowerChestInventoryField = this.registerInstanceFieldForOwner(
                        MappedClasses.zZ, "lowerChestInventory", true, MappedClasses.l0);
            } else {
                this.lowerChestInventoryField = this.J("lowerChestInventory", true, MappedClasses.l0);
            }
            if (GuiComponent.getLegacyComponentState() == null) {
                MGuiContainer.r(new String[4]);
            }
            return;
        }
        this.lowerChestInventoryField = this.J("lowerChestInventory", true, MappedClasses.l0);
        if (GuiComponent.getLegacyComponentState() == null) {
            MGuiContainer.r(new String[4]);
        }
    }

}

package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingField;
import vegas.sony.mapping.mappings.MBlock;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.wrapper.impl.ForgeVersion;

public class MBlockHorizontal
extends Mapping {
    private final MappingField facingField;

    public MBlockHorizontal() {
        this(MBlock.m());
    }

    private MBlockHorizontal(GuiComponent[] controlFlowState) {
        super(MappedClasses.FQ);
        Class<?> facingType = controlFlowState == null && ForgeVersion.MC_1_21_4.d() ? MappedClasses.Vh : MappedClasses.lo;
        this.facingField = this.registerStaticField("FACING", true, facingType);
        if (GuiComponent.getLegacyComponentState() == null) {
            MBlock.Y(new GuiComponent[1]);
        }
    }

    private Object readFacing() {
        return this.facingField.getObject(null);
    }

    public static Object getFacing(MBlockHorizontal mapping) {
        return mapping.readFacing();
    }

}

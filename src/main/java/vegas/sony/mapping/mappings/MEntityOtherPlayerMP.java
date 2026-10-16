package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingMethod;
import vegas.sony.mapping.mappings.MEntityPlayerSP;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.wrapper.impl.ForgeVersion;

public class MEntityOtherPlayerMP
extends Mapping {
    private static final String CONSTRUCTOR = "<init>";
    private final MappingMethod constructor;

    public Object create(Object worldHandle, Object gameProfileHandle) {
        return this.constructor.newInstance(worldHandle, gameProfileHandle);
    }

    public MEntityOtherPlayerMP() {
        this(MEntityPlayerSP.r());
    }

    private MEntityOtherPlayerMP(GuiComponent[] legacyComponentState) {
        super(MappedClasses.lG);
        if (legacyComponentState != null) {
            if (ForgeVersion.MC_1_16_5.d()) {
                this.constructor = this.Y(CONSTRUCTOR, false, Void.TYPE, MappedClasses.Z, MappedClasses.VD);
            } else {
                this.constructor = this.Y(CONSTRUCTOR, false, Void.TYPE, MappedClasses.YU, MappedClasses.VD);
            }
            return;
        }
        this.constructor = this.Y(CONSTRUCTOR, false, Void.TYPE, MappedClasses.YU, MappedClasses.VD);
    }
}

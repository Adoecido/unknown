package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingMethod;
import vegas.sony.utils.datas.BlockData;
import vegas.sony.wrapper.impl.ForgeVersion;

public class MNonNullList
extends Mapping {
    private final MappingMethod createMethod;

    public Object create() {
        if (ForgeVersion.MC_1_17.d()) {
            return this.createMethod.invokeObject(null);
        }
        return this.createMethod.newInstance();
    }

    public MNonNullList() {
        this(BlockData.W());
    }

    private MNonNullList(String[] nonNullListMappingState) {
        super(MappedClasses.Vd);
        if (nonNullListMappingState != null) {
            if (ForgeVersion.MC_1_17.d()) {
                this.createMethod = this.registerStaticMethod("create", true, MappedClasses.Vd);
            } else {
                this.createMethod = this.Y("<init>", false, Void.TYPE);
            }
            return;
        }
        this.createMethod = this.Y("<init>", false, Void.TYPE);
    }
}


package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.asm.helper.Local;
import vegas.sony.asm.transform.ClassTransformer;
import vegas.sony.event.impl.EventPostLocalPlayerTick;
import vegas.sony.event.impl.EventPreLocalPlayerTick;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.MappingMethod;
import vegas.sony.wrapper.impl.ForgeVersion;

public class LocalPlayerTickClassTransformer
extends ClassTransformer {
    private void w() {
        if (ForgeVersion.MC_1_21_10.v()) {
            return;
        }
        MappingMethod mappingMethod = SonyVegas.INSTANCE.getMappings().CC.z;
        this.injectEventAtEntry(mappingMethod, EventPreLocalPlayerTick.class, new Local("this").setDescriptorClass(Object.class));
        this.injectEventAtExit(mappingMethod, EventPostLocalPlayerTick.class, new Local("this").setDescriptorClass(Object.class));
    }

    public LocalPlayerTickClassTransformer() {
        super(MappedClasses.z5);
    }


    @Override
    public void transform() {
        this.w();
    }
}


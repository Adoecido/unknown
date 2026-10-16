package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.asm.ITramsformNode;
import vegas.sony.asm.helper.DescUtils;
import vegas.sony.asm.helper.TypedIndexedLocal;
import vegas.sony.asm.transform.ClassTransformer;
import vegas.sony.event.impl.EventPostRenderTick;
import vegas.sony.event.impl.EventPreRenderTick;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.MappingMethod;
import vegas.sony.wrapper.impl.ForgeVersion;

public class RenderTickEventMappingTask
extends ClassTransformer {

    @Override
    public void transform() {
        MappingMethod mappingMethod = SonyVegas.INSTANCE.getMappings().RY.J;
        if (ForgeVersion.MC_1_21_4.d()) {
            this.injectEventAtEntry(mappingMethod, EventPreRenderTick.class, new TypedIndexedLocal(1, DescUtils.getDescriptor(MappedClasses.uy)).setDescriptorClass(Object.class));
            this.injectEventAtExit(mappingMethod, EventPostRenderTick.class, new ITramsformNode[0]);
        }
    }

    public RenderTickEventMappingTask() {
        super(MappedClasses.FW);
    }
}


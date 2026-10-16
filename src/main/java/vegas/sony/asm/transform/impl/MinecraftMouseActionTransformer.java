package vegas.sony.asm.transform.impl;

import vegas.sony.SonyVegas;
import vegas.sony.asm.ITramsformNode;
import vegas.sony.asm.helper.DescUtils;
import vegas.sony.asm.helper.TypedIndexedLocal;
import vegas.sony.asm.transform.ClassTransformer;
import vegas.sony.event.impl.EventClickMouse;
import vegas.sony.event.impl.EventGuiOpen;
import vegas.sony.event.impl.EventRightClickMouse;
import vegas.sony.event.impl.EventSendClickBlockToController;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.wrapper.impl.ForgeVersion;

public class MinecraftMouseActionTransformer
extends ClassTransformer {
    public MinecraftMouseActionTransformer() {
        super(MappedClasses.uP);
    }

    @Override
    public void transform() {
        if (ForgeVersion.MC_1_20_6.v()) {
            this.injectEventAtEntry(SonyVegas.INSTANCE.getMappings().U.q, EventClickMouse.class, new ITramsformNode[0]);
        }
        this.injectEventAtEntry(SonyVegas.INSTANCE.getMappings().U.OS, EventGuiOpen.class, new TypedIndexedLocal(1, DescUtils.getDescriptor(MappedClasses.VW)).setDescriptorClass(Object.class));
        this.injectEventAtEntry(SonyVegas.INSTANCE.getMappings().U.g, EventRightClickMouse.class, new ITramsformNode[0]);
        this.injectEventAtEntry(SonyVegas.INSTANCE.getMappings().U.Of, EventSendClickBlockToController.class, new ITramsformNode[0]);
    }

}


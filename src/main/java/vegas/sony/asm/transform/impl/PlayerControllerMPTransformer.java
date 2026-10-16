package vegas.sony.asm.transform.impl;

import vegas.sony.SonyVegas;
import vegas.sony.asm.helper.IndexedLocal;
import vegas.sony.asm.transform.ClassTransformer;
import vegas.sony.event.impl.EventPostAttack;
import vegas.sony.event.impl.EventPreAttack;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.MappingMethod;
import vegas.sony.mapping.PlayerControllerMPEventMappingTask;

public class PlayerControllerMPTransformer
extends ClassTransformer {
    private static int opaqueState;

    static {
        if (PlayerControllerMPTransformer.opaquePredicate() != 0) {
            PlayerControllerMPTransformer.setOpaqueState(19);
        }
    }

    public static int getOpaqueState() {
        return opaqueState;
    }

    public PlayerControllerMPTransformer() {
        super(MappedClasses.ld);
    }


    public static void setOpaqueState(int state) {
        opaqueState = state;
    }

    @Override
    public void transform() {
        if (!PlayerControllerMPEventMappingTask.U) {
            return;
        }
        MappingMethod mappingMethod = SonyVegas.INSTANCE.getMappings().hj.i;
        this.injectEventAtEntry(mappingMethod, EventPreAttack.class, new IndexedLocal(2).setDescriptorClass(Object.class));
        this.injectEventAtExit(mappingMethod, EventPostAttack.class, new IndexedLocal(2).setDescriptorClass(Object.class));
    }

    public static int opaquePredicate() {
        int state = PlayerControllerMPTransformer.getOpaqueState();
        if (state == 0) {
            return 52;
        }
        return 0;
    }
}


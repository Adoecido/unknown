package vegas.sony.wrapper.impl;

import vegas.sony.SonyVegas;
import vegas.sony.mapping.mappings.MMappedFieldSingletonWrapper;
import vegas.sony.wrapper.Wrapper;

public class MappedFieldSingletonWrapper
extends Wrapper {
    private static MappedFieldSingletonWrapper humanoidArmorInstance;

    public MappedFieldSingletonWrapper(Object wrappedObject) {
        super(wrappedObject);
    }


    public static MappedFieldSingletonWrapper humanoidArmor() {
        if (humanoidArmorInstance == null) {
            humanoidArmorInstance = new MappedFieldSingletonWrapper(MMappedFieldSingletonWrapper.getHumanoidArmorField(SonyVegas.INSTANCE.getMappings().CL).getObject(null));
        }
        return humanoidArmorInstance;
    }
}


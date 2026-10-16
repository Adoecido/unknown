package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.event.impl.EventKeyBindingState;
import vegas.sony.mapping.JavassistMappingTask;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.wrapper.impl.ForgeVersion;

public class KeyBindingStateEventMappingTask
extends JavassistMappingTask {
    private static final String c;

    public KeyBindingStateEventMappingTask() {
        super(MappedClasses.DR);
    }

    @Override
    public void transform() {
        if (ForgeVersion.MC_1_16_5.d()) {
            this.O(SonyVegas.INSTANCE.getMappings().hJ.s, EventKeyBindingState.class, c, "");
            return;
        }
    }


    static {
        try {
            c = "$0, $1";
        }
        catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}


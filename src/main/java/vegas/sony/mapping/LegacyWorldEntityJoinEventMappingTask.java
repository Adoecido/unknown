package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.event.impl.EventEntityJoinWorld;
import vegas.sony.mapping.JavassistMappingTask;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.MappingMethod;
import vegas.sony.wrapper.impl.ForgeVersion;

public class LegacyWorldEntityJoinEventMappingTask
extends JavassistMappingTask {
    private static final String c;

    @Override
    public void transform() {
        if (ForgeVersion.MC_1_16_5.d()) {
            return;
        }
        if (ForgeVersion.MC_1_12_2.d()) {
            return;
        }
        MappingMethod mappingMethod = SonyVegas.INSTANCE.getMappings().Cy.q;
        this.c(mappingMethod, EventEntityJoinWorld.class, c);
    }


    public LegacyWorldEntityJoinEventMappingTask() {
        super(MappedClasses.YU);
    }

    static {
        try {
            c = "$1";
        }
        catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}


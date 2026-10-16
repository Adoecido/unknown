package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.event.impl.EventEntityJoinWorld;
import vegas.sony.mapping.JavassistMappingTask;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.MappingMethod;
import vegas.sony.wrapper.impl.ForgeVersion;

public class WorldEntityJoinEventMappingTask
extends JavassistMappingTask {
    @Override
    public void transform() {
        MappingMethod mappingMethod = SonyVegas.INSTANCE.getMappings().Cy.q;
        String string = ForgeVersion.MC_1_20_6.d() ? "$1" : "$2";
        this.c(mappingMethod, EventEntityJoinWorld.class, string);
    }

    public WorldEntityJoinEventMappingTask() {
        super(MappedClasses.Z);
    }

}


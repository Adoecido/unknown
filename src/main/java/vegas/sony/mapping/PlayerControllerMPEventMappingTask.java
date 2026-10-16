package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.event.impl.EventBedBreakerUpdate;
import vegas.sony.event.impl.EventPlayerUseItem;
import vegas.sony.event.impl.EventPostAttack;
import vegas.sony.event.impl.EventPreAttack;
import vegas.sony.event.impl.EventWindowClick;
import vegas.sony.wrapper.impl.ForgeVersion;

public class PlayerControllerMPEventMappingTask
extends JavassistMappingTask {
    public static final boolean U = ForgeVersion.MC_1_16_5.d() && (SonyVegas.INSTANCE.isForgeAbsent() || SonyVegas.INSTANCE.isVanillaMinecraftPresent());

    public PlayerControllerMPEventMappingTask() {
        super(MappedClasses.ld);
    }

    @Override
    public void transform() {
        Object object;
        MappingMethod mappingMethod = SonyVegas.INSTANCE.getMappings().hj.c;
        if (ForgeVersion.MC_1_12_2.v()) {
            this.O(mappingMethod, EventPlayerUseItem.class, "$3", "false");
        } else if (ForgeVersion.MC_1_12_2.d()) {
            object = ForgeVersion.MC_1_20_6.d() ? "$1, $2" : "$1, $3";
            this.O(mappingMethod, PlayerUseItemCallback.class, (String)object, MappedClasses.zr.getName() + "." + SonyVegas.INSTANCE.getMappings().enumActionResult.passField.getResolvedName());
        }
        this.c(SonyVegas.INSTANCE.getMappings().hj.L, EventBedBreakerUpdate.class, "");
        if (!U) {
            object = SonyVegas.INSTANCE.getMappings().hj.i;
            this.c((MappingMethod)object, EventPreAttack.class, "$2");
            this.k((MappingMethod)object, EventPostAttack.class, "$2");
        }
        this.O(SonyVegas.INSTANCE.getMappings().hj.I, EventWindowClick.class, "$0", "null");
    }

}


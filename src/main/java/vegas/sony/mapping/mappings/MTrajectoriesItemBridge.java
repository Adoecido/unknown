package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingMethod;

public class MTrajectoriesItemBridge
extends Mapping {
    private final MappingMethod isChargedMethod;

    public MTrajectoriesItemBridge() {
        super(MappedClasses.YA);
        this.isChargedMethod = this.registerStaticMethod("isCharged", true, Boolean.TYPE, new Class[]{MappedClasses.VK});
    }

    public boolean isCharged(Object itemStack) {
        return this.isChargedMethod.invokeBoolean(null, itemStack);
    }
}


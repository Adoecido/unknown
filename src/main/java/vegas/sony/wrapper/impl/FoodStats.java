package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MFoodStats;
import vegas.sony.wrapper.Wrapper;

public class FoodStats
extends Wrapper {
    public int getFoodLevel() {
        return MFoodStats.getFoodLevel(FoodStats.sonyVegasInstance.getMappingsMapperCompat().DJ, this.I);
    }

    public FoodStats(Object wrappedObject) {
        super(wrappedObject);
    }
}

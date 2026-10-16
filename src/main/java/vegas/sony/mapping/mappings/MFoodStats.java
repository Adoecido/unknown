package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingMethod;

public class MFoodStats
extends Mapping {
    private static final String GET_FOOD_LEVEL_METHOD_NAME = "getFoodLevel";
    private final MappingMethod getFoodLevelMethod;

    private int invokeGetFoodLevel(Object foodStats) {
        return this.getFoodLevelMethod.invokeInt(foodStats, new Object[0]);
    }

    public MFoodStats() {
        super(MappedClasses.Zd);
        this.getFoodLevelMethod = this.Y(GET_FOOD_LEVEL_METHOD_NAME, true, Integer.TYPE, new Class[]{});
    }

    public static int getFoodLevel(MFoodStats mapping, Object foodStats) {
        return mapping.invokeGetFoodLevel(foodStats);
    }
}


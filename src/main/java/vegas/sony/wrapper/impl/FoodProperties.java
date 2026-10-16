package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class FoodProperties
extends Wrapper {
    public FoodProperties(Object wrappedObject) {
        super(wrappedObject);
    }

    public boolean canAlwaysEat() {
        return FoodProperties.sonyVegasInstance.getMappingsMapperCompat().foodProperties.canAlwaysEat(this.getObject());
    }

    public int getNutrition() {
        return FoodProperties.sonyVegasInstance.getMappingsMapperCompat().foodProperties.getNutrition(this.getObject());
    }

    public float getSaturation() {
        return FoodProperties.sonyVegasInstance.getMappingsMapperCompat().foodProperties.getSaturation(this.getObject());
    }
}

package vegas.sony.wrapper.impl;

public class ItemFood
extends Item {
    public float getSaturation() {
        return ItemFood.sonyVegasInstance.getMappings().itemFood.getSaturation(this.I);
    }

    public ItemFood(Object wrappedObject) {
        super(wrappedObject);
    }

    public int getNutrition() {
        return ItemFood.sonyVegasInstance.getMappings().itemFood.getNutrition(this.I);
    }
}

package vegas.sony.module.utility.inventory.cleaner;

import com.google.gson.JsonObject;
import vegas.sony.module.utility.inventory.cleaner.InventoryFilterCondition;
import vegas.sony.module.utility.inventory.cleaner.InventoryFilterConditionType;
import vegas.sony.wrapper.impl.ItemStack;

public class EmptyInventoryFilterCondition
implements InventoryFilterCondition<EmptyInventoryFilterCondition> {
    @Override
    public EmptyInventoryFilterCondition copy() {
        return new EmptyInventoryFilterCondition();
    }
    @Override
    public JsonObject toJson() {
        return null;
    }

    @Override
    public InventoryFilterConditionType getType() {
        return null;
    }

    @Override
    public boolean matches(ItemStack itemStack) {
        return false;
    }

}

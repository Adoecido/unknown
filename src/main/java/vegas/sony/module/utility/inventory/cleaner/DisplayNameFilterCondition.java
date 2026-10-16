package vegas.sony.module.utility.inventory.cleaner;

import com.google.gson.JsonObject;
import vegas.sony.module.utility.inventory.cleaner.AbstractTextFilterCondition;
import vegas.sony.module.utility.inventory.cleaner.InventoryFilterConditionType;
import vegas.sony.module.utility.inventory.cleaner.TextMatchMode;
import vegas.sony.wrapper.impl.ItemStack;
import java.util.List;

public class DisplayNameFilterCondition
extends AbstractTextFilterCondition<DisplayNameFilterCondition> {
    @Override
    public DisplayNameFilterCondition copy() {
        return new DisplayNameFilterCondition(this.getTexts(), this.getMatchMode());
    }

    @Override
    public InventoryFilterConditionType getType() {
        return InventoryFilterConditionType.DISPLAY_NAME;
    }

    public DisplayNameFilterCondition(List<String> texts, TextMatchMode matchMode) {
        super(texts, matchMode);
    }

    public DisplayNameFilterCondition(JsonObject jsonObject) {
        super(jsonObject);
    }


    public boolean matches(ItemStack itemStack) {
        if (itemStack.isNull()) {
            return false;
        }
        String displayName = itemStack.x();
        return this.getMatchMode().matchesAny(displayName, this.getTexts());
    }

    public DisplayNameFilterCondition() {
    }
}

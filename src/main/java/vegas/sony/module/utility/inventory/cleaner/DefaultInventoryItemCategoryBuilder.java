package vegas.sony.module.utility.inventory.cleaner;

import vegas.sony.module.utility.inventory.cleaner.InventoryItemCategoryBuilder;
import vegas.sony.module.utility.inventory.cleaner.MatcherBackedInventoryItemCategoryBuilder;
import vegas.sony.module.utility.inventory.cleaner.StackSizeInventoryItemCategoryBuilder;

public class DefaultInventoryItemCategoryBuilder
extends InventoryItemCategoryBuilder {
    public StackSizeInventoryItemCategoryBuilder stackSize() {
        return new StackSizeInventoryItemCategoryBuilder();
    }

    public MatcherBackedInventoryItemCategoryBuilder matcherBacked() {
        return new MatcherBackedInventoryItemCategoryBuilder();
    }
}

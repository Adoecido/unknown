package vegas.sony.module.utility.inventory.cleaner;

import vegas.sony.module.utility.inventory.cleaner.InventoryItemCategoryBuilder;
import vegas.sony.module.utility.inventory.cleaner.MatcherBackedInventoryItemCategory;

public class MatcherBackedInventoryItemCategoryBuilder
extends InventoryItemCategoryBuilder<MatcherBackedInventoryItemCategoryBuilder> {
    public MatcherBackedInventoryItemCategory build() {
        return new MatcherBackedInventoryItemCategory(this);
    }
}


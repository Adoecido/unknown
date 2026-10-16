package vegas.sony.module.utility.inventory.cleaner;

import vegas.sony.module.utility.inventory.cleaner.ClassInventoryItemMatcher;
import vegas.sony.module.utility.inventory.cleaner.ClassInventoryItemMatcherBuilder;
import vegas.sony.module.utility.inventory.cleaner.CompositeInventoryItemMatcher;
import vegas.sony.module.utility.inventory.cleaner.InventoryItemMatcherBuilderBase;
import vegas.sony.module.utility.inventory.cleaner.InventoryItemMatcherBuilderFoundation;
import vegas.sony.module.utility.inventory.cleaner.StringInventoryItemMatcher;
import vegas.sony.module.utility.inventory.cleaner.StringInventoryItemMatcherBuilder;

public class InventoryItemMatcherBuilder
extends InventoryItemMatcherBuilderBase<InventoryItemMatcherBuilder> {
    public InventoryItemMatcherBuilderFoundation composite() {
        return CompositeInventoryItemMatcher.builderFrom(this);
    }

    public StringInventoryItemMatcherBuilder stringMatcher() {
        return StringInventoryItemMatcher.builderFrom(this);
    }

    public ClassInventoryItemMatcherBuilder classMatcher() {
        return ClassInventoryItemMatcher.builderFrom(this);
    }
}

package vegas.sony.module.utility.inventory.cleaner;

import vegas.sony.module.utility.inventory.cleaner.AbstractInventoryItemMatcher;
import vegas.sony.module.utility.inventory.cleaner.InventoryItemMatcher;
import vegas.sony.module.utility.inventory.cleaner.InventoryItemMatcherBuilder;
import vegas.sony.module.utility.inventory.cleaner.InventoryItemMatcherBuilderBase;
import vegas.sony.module.utility.inventory.cleaner.InventoryItemMatcherGroup;
import vegas.sony.wrapper.impl.Item;
import vegas.sony.wrapper.impl.ItemStack;

public class EmptySlotInventoryItemMatcher
extends AbstractInventoryItemMatcher {
    public static final EmptySlotInventoryItemMatcher EMPTY_SLOT = new EmptySlotInventoryItemMatcher();


    EmptySlotInventoryItemMatcher() {
        super((InventoryItemMatcherBuilderBase<?>)((InventoryItemMatcherBuilder)((InventoryItemMatcherBuilder)InventoryItemMatcher.builder().withName("Hand")).withDescription("No item")).withGroup(InventoryItemMatcherGroup.HIDDEN));
    }

    @Override
    public boolean matches(ItemStack itemStack, Item item) {
        return itemStack == null || itemStack.isNull() || item == null || item.isNull();
    }
}


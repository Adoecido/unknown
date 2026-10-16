package vegas.sony.module.utility.inventory.cleaner;

import vegas.sony.module.utility.inventory.cleaner.InventoryItemMatcher;
import vegas.sony.module.utility.inventory.cleaner.InventoryItemMatcherBuilderFoundation;
import vegas.sony.module.utility.inventory.cleaner.InventoryItemMatcherGroup;
import vegas.sony.module.utility.inventory.cleaner.InventoryItemMatcherRegistry;
import vegas.sony.utils.ItemStackScoreUtil;
import vegas.sony.wrapper.impl.Item;
import vegas.sony.wrapper.impl.ItemStack;

public class ArmorInventoryItemMatchers {
    public static final InventoryItemMatcher ANY_ARMOR;

    static {
        String[] labels = new String[]{"any-armor", "Any armor", "armor_item", "Any type of armor"};
        ANY_ARMOR = ((InventoryItemMatcherBuilderFoundation)((InventoryItemMatcherBuilderFoundation)((InventoryItemMatcherBuilderFoundation)((InventoryItemMatcherBuilderFoundation)((InventoryItemMatcherBuilderFoundation)InventoryItemMatcher.builder().composite().withId(labels[0])).withName(labels[1])).withIconName(labels[2])).withDescription(labels[3])).withGroup(InventoryItemMatcherGroup.ARMOR)).withPredicate(ArmorInventoryItemMatchers::isArmorItem).build();
    }

    static void initialize() {
        InventoryItemMatcherRegistry.register(ANY_ARMOR);
    }

    private static boolean isArmorItem(ItemStack itemStack, Item item) {
        return ItemStackScoreUtil.R(item);
    }
}


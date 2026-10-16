package vegas.sony.module.utility.inventory.cleaner;

import vegas.sony.SonyVegas;
import vegas.sony.mapping.ItemMappingEntry;
import vegas.sony.module.utility.inventory.cleaner.AbstractInventoryItemMatcher;
import vegas.sony.module.utility.inventory.cleaner.InventoryItemMatcherBuilderBase;
import vegas.sony.module.utility.inventory.cleaner.StringInventoryItemMatcherBuilder;
import vegas.sony.module.utility.inventory.cleaner.StringMatchOperator;
import vegas.sony.wrapper.impl.Item;
import vegas.sony.wrapper.impl.ItemStack;
import java.util.Map;

public class StringInventoryItemMatcher
extends AbstractInventoryItemMatcher {
    private final Map<String, StringMatchOperator> matchOperators;

    @Override
    public boolean matches(ItemStack itemStack, Item item) {
        if (this.matchOperators.isEmpty()) {
            return false;
        }
        ItemMappingEntry mappingEntry = SonyVegas.INSTANCE.getItemStackResolver().resolve(itemStack);
        if (mappingEntry == null) {
            return false;
        }
        for (Map.Entry<String, StringMatchOperator> entry : this.matchOperators.entrySet()) {
            String pattern = entry.getKey();
            StringMatchOperator operator = entry.getValue();
            if (!operator.getPredicate().test(mappingEntry.getModernId(), pattern)) continue;
            return true;
        }
        return false;
    }

    public static StringInventoryItemMatcherBuilder builderFrom(InventoryItemMatcherBuilderBase<?> baseBuilder) {
        return new StringInventoryItemMatcherBuilder(baseBuilder, null);
    }


    public StringInventoryItemMatcher(StringInventoryItemMatcherBuilder builder) {
        super(builder);
        this.matchOperators = builder.getOperatorsByPattern();
    }
}


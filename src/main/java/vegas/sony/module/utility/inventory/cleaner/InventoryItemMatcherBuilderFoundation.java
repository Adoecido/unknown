package vegas.sony.module.utility.inventory.cleaner;

import vegas.sony.inventory.cleaner.InventoryMatcherMarker;
import vegas.sony.wrapper.impl.Item;
import vegas.sony.wrapper.impl.ItemStack;
import java.util.function.BiPredicate;

public class InventoryItemMatcherBuilderFoundation
extends InventoryItemMatcherBuilderBase<InventoryItemMatcherBuilderFoundation> {
    private BiPredicate<ItemStack, Item> predicate;

    InventoryItemMatcherBuilderFoundation(InventoryItemMatcherBuilderBase base, InventoryMatcherMarker marker) {
        this(base);
    }

    public CompositeInventoryItemMatcher build() {
        return new CompositeInventoryItemMatcher(this);
    }

    public BiPredicate<ItemStack, Item> getPredicate() {
        return this.predicate;
    }

    public InventoryItemMatcherBuilderFoundation withPredicate(BiPredicate<ItemStack, Item> predicate) {
        this.predicate = predicate;
        return this;
    }

    private InventoryItemMatcherBuilderFoundation(InventoryItemMatcherBuilderBase<?> base) {
        super(base);
    }
}


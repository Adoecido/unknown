package vegas.sony.module.utility.inventory.cleaner;

import vegas.sony.module.utility.inventory.cleaner.InventoryItemCategory;
import vegas.sony.module.utility.inventory.cleaner.InventoryItemMatchContext;
import vegas.sony.module.utility.inventory.cleaner.InventoryItemMatcherBuilder;
import vegas.sony.module.utility.inventory.cleaner.InventoryItemMatcherGroup;
import vegas.sony.unmap.INamed;
import vegas.sony.wrapper.impl.Item;
import vegas.sony.wrapper.impl.ItemStack;
import java.util.Comparator;
import vegas.sony.module.utility.inventory.cleaner.DescribedOption;
import org.jetbrains.annotations.Nullable;

public interface InventoryItemMatcher
extends INamed,
DescribedOption {
    default public boolean matches(ItemStack itemStack) {
        return this.matches(itemStack, itemStack.getItem());
    }

    default public InventoryItemCategory getCategory() {
        return null;
    }

    @Nullable
    public String getIconName();

    @Nullable
    public Comparator<InventoryItemMatchContext> getComparator();

    public static InventoryItemMatcherBuilder builder() {
        return new InventoryItemMatcherBuilder();
    }

    public String getId();

    public InventoryItemMatcherGroup getGroup();

    public boolean matches(ItemStack itemStack, Item item);

    default public void setCategory(InventoryItemCategory category) {
    }
}

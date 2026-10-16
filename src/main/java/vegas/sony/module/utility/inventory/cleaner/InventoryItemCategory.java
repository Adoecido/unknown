package vegas.sony.module.utility.inventory.cleaner;

import vegas.sony.module.utility.inventory.cleaner.DefaultInventoryItemCategoryBuilder;
import vegas.sony.module.utility.inventory.cleaner.InventoryItemMatcher;
import vegas.sony.module.utility.inventory.cleaner.ItemFilterSelection;
import vegas.sony.unmap.INamed;
import vegas.sony.wrapper.impl.ItemStack;
import java.util.Comparator;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

public interface InventoryItemCategory
extends INamed {
    public String getId();

    public @UnmodifiableView List<InventoryItemMatcher> getMatchers();

    @Override
    public String getName();

    public static DefaultInventoryItemCategoryBuilder builder() {
        return new DefaultInventoryItemCategoryBuilder();
    }

    public boolean isCompatible(ItemFilterSelection selection);

    public String getDisplayName();

    @Nullable
    public Comparator<ItemStack> getComparator();
}

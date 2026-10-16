package vegas.sony.module.utility.inventory.cleaner;

import vegas.sony.module.utility.inventory.cleaner.InventoryFilterConditionGroup;
import vegas.sony.unmap.INamed;
import vegas.sony.wrapper.impl.ItemStack;
import java.util.List;
import org.jetbrains.annotations.UnmodifiableView;

public interface InventoryFilterPresetData
extends INamed {
    public @UnmodifiableView List<InventoryFilterConditionGroup> getConditionGroups();

    public boolean matches(ItemStack itemStack);

    @Override
    public String getName();
}

package vegas.sony.module.utility.inventory.cleaner;

import vegas.sony.module.utility.inventory.cleaner.InventoryFilterPreset;
import vegas.sony.module.utility.inventory.cleaner.InventoryItemCategory;
import vegas.sony.module.utility.inventory.cleaner.ItemFilterSelection;
import vegas.sony.wrapper.impl.ItemStack;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface InventoryFilterRule {
    public void setPreset(@Nullable InventoryFilterPreset preset);

    public void clearPresetReference();

    @Nullable
    public UUID getSharedPresetId();

    default public boolean matches(ItemStack itemStack) {
        InventoryFilterPreset preset = this.resolvePreset();
        return preset == null || preset.matches(itemStack);
    }

    public ItemFilterSelection getItemSelection();

    public void setPriorityOverride(@Nullable InventoryItemCategory priority);

    @NotNull
    public InventoryItemCategory getPriority();


    public void reset();

    public InventoryItemCategory getDefaultPriority();

    @Nullable
    public InventoryFilterPreset resolvePreset();
}


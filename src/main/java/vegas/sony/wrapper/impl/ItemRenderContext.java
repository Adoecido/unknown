package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class ItemRenderContext
extends Wrapper {
    public static ItemRenderContext create(boolean wasItemInteraction, ItemStack heldItem) {
        return new ItemRenderContext(ItemRenderContext.sonyVegasInstance.getMappingsMapperCompat().itemRenderContext.create(wasItemInteraction, heldItem.getObject()));
    }

    public static ItemRenderContext defaultContext() {
        return new ItemRenderContext(ItemRenderContext.sonyVegasInstance.getMappingsMapperCompat().itemRenderContext.getDefault());
    }

    public ItemRenderContext(Object handle) {
        super(handle);
    }

    public static ItemRenderContext none() {
        return new ItemRenderContext(ItemRenderContext.sonyVegasInstance.getMappingsMapperCompat().itemRenderContext.getNone());
    }
}

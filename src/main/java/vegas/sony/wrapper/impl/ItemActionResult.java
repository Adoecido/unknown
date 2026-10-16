package vegas.sony.wrapper.impl;

public class ItemActionResult
extends EnumActionResult {
    public static ItemActionResult create(GlStateManagerTexGenCoord swingSource, ItemRenderContext itemContext) {
        return new ItemActionResult(ItemActionResult.sonyVegasInstance.getMappingsMapperCompat().itemActionResult.create(swingSource.getObject(), itemContext.getObject()));
    }

    public boolean consumesAction() {
        return ItemActionResult.sonyVegasInstance.getMappingsMapperCompat().itemActionResult.consumesAction(this.getObject());
    }

    public ItemActionResult withoutItem() {
        return new ItemActionResult(ItemActionResult.sonyVegasInstance.getMappingsMapperCompat().itemActionResult.withoutItem(this.getObject()));
    }

    public ItemStack getHeldItemTransformedTo() {
        return new ItemStack(ItemActionResult.sonyVegasInstance.getMappingsMapperCompat().itemActionResult.getHeldItemTransformedTo(this.getObject()));
    }

    public ItemActionResult heldItemTransformedTo(ItemStack itemStack) {
        return new ItemActionResult(ItemActionResult.sonyVegasInstance.getMappingsMapperCompat().itemActionResult.heldItemTransformedTo(this.getObject(), itemStack.getObject()));
    }

    public boolean wasItemInteraction() {
        return ItemActionResult.sonyVegasInstance.getMappingsMapperCompat().itemActionResult.wasItemInteraction(this.getObject());
    }

    public ItemActionResult(Object handle) {
        super(handle);
    }
}

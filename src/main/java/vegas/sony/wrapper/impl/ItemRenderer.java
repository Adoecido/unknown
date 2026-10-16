package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MItemRenderer;
import vegas.sony.wrapper.Wrapper;

public class ItemRenderer
extends Wrapper {
    public void g(EntityLivingBase entityLivingBase, ItemStack itemStack, ItemRendererBridge itemRendererBridge) {
        ItemRenderer.sonyVegasInstance.getMappingsMapperCompat().qE.Z(this.I, entityLivingBase.getObject(), itemStack.getObject(), itemRendererBridge.getObject());
    }

    public ItemRenderer(Object object) {
        super(object);
    }

    public void X(AbstractClientPlayer abstractClientPlayer) {
        MItemRenderer.K(ItemRenderer.sonyVegasInstance.getMappingsMapperCompat().qE, this.I, abstractClientPlayer.getObject());
    }

    public float e() {
        return ItemRenderer.sonyVegasInstance.getMappingsMapperCompat().qE.S(this.I);
    }

    public ItemStack k() {
        return new ItemStack(ItemRenderer.sonyVegasInstance.getMappingsMapperCompat().qE.v(this.I));
    }

    public float R() {
        return ItemRenderer.sonyVegasInstance.getMappingsMapperCompat().qE.Y(this.I);
    }
}


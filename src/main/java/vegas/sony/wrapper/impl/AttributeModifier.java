package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MAttributeModifier;
import vegas.sony.wrapper.Wrapper;

import java.util.UUID;

public class AttributeModifier
extends Wrapper {
    public ResourceLocation getResourceLocation() {
        return new ResourceLocation(MAttributeModifier.getResourceLocation(AttributeModifier.sonyVegasInstance.getMappings().hv, this.I));
    }

    public AttributeModifier(Object wrappedObject) {
        super(wrappedObject);
    }

    public double getAmount() {
        return AttributeModifier.sonyVegasInstance.getMappings().hv.getAmount(this.I);
    }

    public UUID getId() {
        return MAttributeModifier.getUuid(AttributeModifier.sonyVegasInstance.getMappings().hv, this.I);
    }
}

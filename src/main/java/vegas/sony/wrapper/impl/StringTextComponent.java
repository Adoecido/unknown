package vegas.sony.wrapper.impl;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.wrapper.Wrapper;

public class StringTextComponent
extends Wrapper {

    public StringTextComponent(Object componentHandle) {
        super(componentHandle);
    }

    public TextComponentTranslation getItemStackRenderState() {
        Object renderStateHandle = StringTextComponent.sonyVegasInstance.getMappingsMapperCompat().stringTextComponentBridge
                .getItemStackRenderState(this.I);
        return new TextComponentTranslation(renderStateHandle);
    }

    public String getName() {
        if (MappedClasses.DE != null && ForgeVersion.MC_26_1.d()) {
            return "";
        }
        return StringTextComponent.sonyVegasInstance.getMappingsMapperCompat().stringTextComponentBridge.getName(this.I);
    }
}


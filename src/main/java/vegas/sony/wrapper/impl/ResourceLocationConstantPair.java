package vegas.sony.wrapper.impl;

import vegas.sony.SonyVegas;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.wrapper.impl.ResourceLocation;

public class ResourceLocationConstantPair {
    private static GuiComponent[] controlFlowState;

    public static void setControlFlowState(GuiComponent[] state) {
        controlFlowState = state;
    }

    public static ResourceLocation getItems() {
        return new ResourceLocation(SonyVegas.INSTANCE.getMappingsMapperCompat().resourceLocationConstants.getItems());
    }

    public static GuiComponent[] getControlFlowState() {
        return controlFlowState;
    }

    public static ResourceLocation getGui() {
        return new ResourceLocation(SonyVegas.INSTANCE.getMappingsMapperCompat().resourceLocationConstants.getGui());
    }

    static {
        if (ResourceLocationConstantPair.getControlFlowState() != null) {
            ResourceLocationConstantPair.setControlFlowState(new GuiComponent[3]);
        }
    }
}

package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MScreen;
import vegas.sony.wrapper.Wrapper;

public class Screen
extends Wrapper {
    private static int[] controlFlowState;

    public static void setControlFlowState(int[] state) {
        controlFlowState = state;
    }

    public static int[] getControlFlowState() {
        return controlFlowState;
    }

    public Screen(Object screenHandle) {
        super(screenHandle);
    }

    static {
        if (Screen.getControlFlowState() != null) {
            Screen.setControlFlowState(new int[1]);
        }
    }

    public static ResourceLocation getMobEffectSprite(Holder holder) {
        Object spriteLocationHandle = Screen.sonyVegasInstance.getMappings().screen.getMobEffectSprite(holder.getObject());
        return new ResourceLocation(spriteLocationHandle);
    }
}

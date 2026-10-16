package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingMethod;
import vegas.sony.ui.click.component.GuiComponent;

public class MLegacyEntityRenderPreHook
extends Mapping {
    private static final String CONSTRUCTOR_NAME;
    private static String[] controlFlowState;
    public final MappingMethod constructorMethod;

    public static String[] getControlFlowState() {
        return controlFlowState;
    }


    public static void setControlFlowState(String[] state) {
        controlFlowState = state;
    }

    public MLegacyEntityRenderPreHook() {
        super(MappedClasses.x);
        Class[] parameterTypes = new Class[]{MappedClasses.zm, MappedClasses.Fq, Double.TYPE, Double.TYPE, Double.TYPE};
        Class<Void> returnType = Void.TYPE;
        boolean methodPublic = false;
        String methodName = CONSTRUCTOR_NAME;
        MLegacyEntityRenderPreHook mapping = this;
        this.constructorMethod = mapping.Y(methodName, methodPublic, returnType, parameterTypes);
        if (MLegacyEntityRenderPreHook.getControlFlowState() != null) {
            GuiComponent.setLegacyComponentState(new GuiComponent[4]);
            return;
        }
    }

    static {
        MLegacyEntityRenderPreHook.setControlFlowState(null);
        CONSTRUCTOR_NAME = "<init>";
    }
}


package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingMethod;

public class MTextComponentTranslationBridge
extends Mapping {
    private MappingMethod getModelIdentityMethod;
    private static final String GET_MODEL_IDENTITY_METHOD_NAME;
    private static boolean controlFlowStateEnabled;


    static {
        MTextComponentTranslationBridge.setControlFlowStateEnabled(true);
        GET_MODEL_IDENTITY_METHOD_NAME = "getModelIdentity";
    }

    public MTextComponentTranslationBridge() {
        super(MappedClasses.zE);
        Class[] parameterTypes = new Class[]{};
        Class<Object> returnType = Object.class;
        boolean methodPublic = true;
        String methodName = GET_MODEL_IDENTITY_METHOD_NAME;
        MTextComponentTranslationBridge mapping = this;
        this.getModelIdentityMethod = mapping.Y(methodName, methodPublic, returnType, parameterTypes);
    }

    public Object getModelIdentity(Object renderStateHandle) {
        return this.getModelIdentityMethod.invokeObject(renderStateHandle, new Object[0]);
    }

    public static boolean isControlFlowStateDisabled() {
        boolean enabled = MTextComponentTranslationBridge.isControlFlowStateEnabled();
        return !enabled;
    }

    public static boolean isControlFlowStateEnabled() {
        return controlFlowStateEnabled;
    }

    public static void setControlFlowStateEnabled(boolean enabled) {
        controlFlowStateEnabled = enabled;
    }
}


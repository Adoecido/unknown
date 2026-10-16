package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingMethod;
import vegas.sony.mapping.mappings.MItemStack;
import vegas.sony.wrapper.impl.ForgeVersion;

public class MMethodSlotZYBridge
extends Mapping {
    public final MappingMethod useMethod;

    public MMethodSlotZYBridge() {
        this(MItemStack.f());
    }

    private MMethodSlotZYBridge(int controlFlowState) {
        super(MappedClasses.zY);
        int currentControlFlowState = controlFlowState;
        if (ForgeVersion.MC_1_21_4.d()) {
            Class[] parameterTypes = new Class[]{MappedClasses.YU, MappedClasses.Yl, MappedClasses.Yf};
            Class returnType = MappedClasses.Yn;
            boolean methodPublic = true;
            String methodName = "use";
            Class itemClass = MappedClasses.lb;
            MMethodSlotZYBridge mapping = this;
            this.useMethod = mapping.registerInstanceMethodForOwner(itemClass, methodName, methodPublic, returnType, parameterTypes);
        } else if (ForgeVersion.MC_1_16_5.d()) {
            Class[] parameterTypes = new Class[]{MappedClasses.YU, MappedClasses.Yl, MappedClasses.Yf};
            Class returnType = MappedClasses.zU;
            boolean methodPublic = true;
            String methodName = "use";
            MMethodSlotZYBridge mapping = this;
            this.useMethod = mapping.Y(methodName, methodPublic, returnType, parameterTypes);
        } else if (ForgeVersion.MC_1_12_2.d()) {
            Class[] parameterTypes = new Class[]{MappedClasses.YU, MappedClasses.Yl, MappedClasses.Yf};
            Class returnType = MappedClasses.zU;
            boolean methodPublic = true;
            String methodName = "onItemRightClick";
            MMethodSlotZYBridge mapping = this;
            this.useMethod = mapping.Y(methodName, methodPublic, returnType, parameterTypes);
        } else {
            Class[] parameterTypes = new Class[]{MappedClasses.VK, MappedClasses.YU, MappedClasses.Yl};
            Class returnType = MappedClasses.VK;
            boolean methodPublic = true;
            String methodName = "onItemRightClick";
            MMethodSlotZYBridge mapping = this;
            this.useMethod = mapping.Y(methodName, methodPublic, returnType, parameterTypes);
        }
    }

}


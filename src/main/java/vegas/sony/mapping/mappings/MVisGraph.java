package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingMethod;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.wrapper.Wrapper;
import vegas.sony.wrapper.impl.ForgeVersion;

public class MVisGraph
extends Mapping {
    private static String controlFlowMarker;
    public MappingMethod computeVisibilityMethod;
    public MappingMethod setOpaqueCubeMethod;

    public static String getVisGraphControlFlowMarker() {
        return controlFlowMarker;
    }

    public static void setVisGraphControlFlowMarker(String marker) {
        controlFlowMarker = marker;
    }


    public MVisGraph() {
        this(MVisGraph.getVisGraphControlFlowMarker());
    }

    private MVisGraph(String controlFlowMarker) {
        super(MappedClasses.VIS_GRAPH);
        if (controlFlowMarker != null) {
            Class[] parameterTypes = new Class[]{};
            Class returnType = MappedClasses.SET_VISIBILITY;
            boolean remap = true;
            String methodName = "computeVisibility";
            MVisGraph mappings = this;
            this.computeVisibilityMethod = mappings.Y(methodName, remap, returnType, parameterTypes);
            if (ForgeVersion.MC_1_12_2.d()) {
                Class[] opaqueCubeParameterTypes = new Class[]{MappedClasses.lf};
                Class<Void> opaqueCubeReturnType = Void.TYPE;
                boolean remapOpaqueCubeMethod = true;
                String opaqueCubeMethodName = "setOpaqueCube";
                this.setOpaqueCubeMethod = this.Y(opaqueCubeMethodName, remapOpaqueCubeMethod, opaqueCubeReturnType, opaqueCubeParameterTypes);
            } else if (ForgeVersion.MC_1_7_10.Y()) {
                Class[] opaqueCubeParameterTypes = new Class[]{MappedClasses.lf};
                Class<Void> opaqueCubeReturnType = Void.TYPE;
                boolean remapOpaqueCubeMethod = Wrapper.isNativeAvailable;
                String opaqueCubeMethodName = "func_178606_a";
                this.setOpaqueCubeMethod = this.Y(opaqueCubeMethodName, remapOpaqueCubeMethod, opaqueCubeReturnType, opaqueCubeParameterTypes);
            }
            if (GuiComponent.getLegacyComponentState() == null) {
                MVisGraph.setVisGraphControlFlowMarker("K5b3Uc");
            }
            return;
        }
        Class[] parameterTypes = new Class[]{};
        Class returnType = MappedClasses.SET_VISIBILITY;
        boolean remap = true;
        String methodName = "computeVisibility";
        MVisGraph mappings = this;
        this.computeVisibilityMethod = mappings.Y(methodName, remap, returnType, parameterTypes);
        if (ForgeVersion.MC_1_12_2.d()) {
            Class[] opaqueCubeParameterTypes = new Class[]{MappedClasses.lf};
            Class<Void> opaqueCubeReturnType = Void.TYPE;
            boolean remapOpaqueCubeMethod = Wrapper.isNativeAvailable;
            String opaqueCubeMethodName = "func_178606_a";
            this.setOpaqueCubeMethod = this.Y(opaqueCubeMethodName, remapOpaqueCubeMethod, opaqueCubeReturnType, opaqueCubeParameterTypes);
        }
        if (GuiComponent.getLegacyComponentState() == null) {
            MVisGraph.setVisGraphControlFlowMarker("K5b3Uc");
        }
    }

    static {
        MVisGraph.setVisGraphControlFlowMarker("qWXvyc");
    }
}


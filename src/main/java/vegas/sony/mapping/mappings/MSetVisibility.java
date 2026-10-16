package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingMethod;
import vegas.sony.mapping.mappings.MVisGraph;
import vegas.sony.ui.click.component.GuiComponent;

public class MSetVisibility
extends Mapping {
    public final MappingMethod setAllVisibleMethod;
    public final MappingMethod constructor;


    public void setAllVisible(Object visibilitySet, boolean visible) {
        this.setAllVisibleMethod.invokeVoid(visibilitySet, visible);
    }

    public MSetVisibility() {
        this(MVisGraph.getVisGraphControlFlowMarker());
    }

    private MSetVisibility(String controlFlowMarker) {
        super(MappedClasses.SET_VISIBILITY);
        Class[] constructorParameterTypes = new Class[]{};
        Class<Void> constructorReturnType = Void.TYPE;
        boolean remapConstructor = false;
        String constructorName = "<init>";
        MSetVisibility mappings = this;
        this.constructor = mappings.Y(constructorName, remapConstructor, constructorReturnType, constructorParameterTypes);
        if (controlFlowMarker != null) {
            Class[] parameterTypes = new Class[]{Boolean.TYPE};
            Class<Void> returnType = Void.TYPE;
            boolean remap = true;
            String methodName = "setAllVisible";
            this.setAllVisibleMethod = this.Y(methodName, remap, returnType, parameterTypes);
            return;
        }
        Class[] parameterTypes = new Class[]{Boolean.TYPE};
        Class<Void> returnType = Void.TYPE;
        boolean remap = true;
        String methodName = "setAllVisible";
        this.setAllVisibleMethod = this.Y(methodName, remap, returnType, parameterTypes);
        GuiComponent.setLegacyComponentState(new GuiComponent[2]);
    }
}


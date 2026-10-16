package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingMethod;
import vegas.sony.mapping.mappings.MOrdering;
import vegas.sony.ui.click.component.GuiComponent;
import java.util.Collection;

public class MItemAttributeModifiers
extends Mapping {
    public MappingMethod sizeMethod;
    public MappingMethod putMethod;
    public MappingMethod valuesMethod;

    public boolean put(Object multimapHandle, Object key, Object value) {
        return this.putMethod.invokeBoolean(multimapHandle, key, value);
    }

    public int size(Object multimapHandle) {
        return this.sizeMethod.invokeInt(multimapHandle, new Object[0]);
    }

    public MItemAttributeModifiers() {
        this(MOrdering.getControlFlowMarker());
    }

    private MItemAttributeModifiers(String controlFlowMarker) {
        super(MappedClasses.Yb);
        if (controlFlowMarker != null) {
            Class[] sizeParameterTypes = new Class[]{};
            Class<Integer> sizeReturnType = Integer.TYPE;
            boolean sizePublic = false;
            String sizeMethodName = "size";
            MItemAttributeModifiers mapping = this;
            this.sizeMethod = mapping.Y(sizeMethodName, sizePublic, sizeReturnType, sizeParameterTypes);
            Class[] valuesParameterTypes = new Class[]{};
            Class<Collection> valuesReturnType = Collection.class;
            boolean valuesPublic = false;
            String valuesMethodName = "values";
            MItemAttributeModifiers valuesMapping = this;
            this.valuesMethod = valuesMapping.Y(valuesMethodName, valuesPublic, valuesReturnType, valuesParameterTypes);
            Class[] putParameterTypes = new Class[]{Object.class, Object.class};
            Class<Boolean> putReturnType = Boolean.TYPE;
            boolean putPublic = false;
            String putMethodName = "put";
            MItemAttributeModifiers putMapping = this;
            this.putMethod = putMapping.Y(putMethodName, putPublic, putReturnType, putParameterTypes);
            return;
        }
        Class[] sizeParameterTypes = new Class[]{};
        Class<Integer> sizeReturnType = Integer.TYPE;
        boolean sizePublic = false;
        String sizeMethodName = "size";
        MItemAttributeModifiers mapping = this;
        this.sizeMethod = mapping.Y(sizeMethodName, sizePublic, sizeReturnType, sizeParameterTypes);
        Class[] valuesParameterTypes = new Class[]{};
        Class<Collection> valuesReturnType = Collection.class;
        boolean valuesPublic = false;
        String valuesMethodName = "values";
        MItemAttributeModifiers valuesMapping = this;
        this.valuesMethod = valuesMapping.Y(valuesMethodName, valuesPublic, valuesReturnType, valuesParameterTypes);
        Class[] putParameterTypes = new Class[]{Object.class, Object.class};
        Class<Boolean> putReturnType = Boolean.TYPE;
        boolean putPublic = false;
        String putMethodName = "put";
        MItemAttributeModifiers putMapping = this;
        this.putMethod = putMapping.Y(putMethodName, putPublic, putReturnType, putParameterTypes);
        GuiComponent.setLegacyComponentState(new GuiComponent[4]);
    }


    public Collection values(Object multimapHandle) {
        return (Collection)this.valuesMethod.invokeObject(multimapHandle, new Object[0]);
    }
}


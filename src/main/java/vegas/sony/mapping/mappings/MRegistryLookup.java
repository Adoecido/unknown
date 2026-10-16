package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingMethod;
import vegas.sony.mapping.mappings.MRegistryAccess;
import vegas.sony.ui.click.component.GuiComponent;
import java.util.Optional;

public class MRegistryLookup
extends Mapping {
    private final MappingMethod lookupOrThrowMethod;
    private final MappingMethod getMethod;
    private final MappingMethod lookupMethod;

    public MRegistryLookup() {
        super(MappedClasses.A);
        Class[] lookupOrThrowParameterTypes = new Class[]{MappedClasses.qB};
        Class lookupOrThrowReturnType = MappedClasses.u2;
        boolean lookupOrThrowPublic = true;
        String lookupOrThrowMethodName = "lookupOrThrow";
        MRegistryLookup mapping = this;
        this.lookupOrThrowMethod = mapping.Y(lookupOrThrowMethodName, lookupOrThrowPublic, lookupOrThrowReturnType, lookupOrThrowParameterTypes);
        Class[] lookupParameterTypes = new Class[]{MappedClasses.qB};
        Class<Optional> lookupReturnType = Optional.class;
        boolean lookupPublic = true;
        String lookupMethodName = "lookup";
        MRegistryLookup lookupMapping = this;
        this.lookupMethod = lookupMapping.Y(lookupMethodName, lookupPublic, lookupReturnType, lookupParameterTypes);
        Class[] getParameterTypes = new Class[]{MappedClasses.qB};
        Class<Optional> getReturnType = Optional.class;
        boolean getPublic = true;
        String getMethodName = "get";
        MRegistryLookup getMapping = this;
        this.getMethod = getMapping.Y(getMethodName, getPublic, getReturnType, getParameterTypes);
        if (MRegistryAccess.getControlFlowMarker() != null) {
            return;
        }
        GuiComponent.setLegacyComponentState(new GuiComponent[3]);
    }

    public Optional<Object> lookup(Object providerHandle, Object resourceKeyHandle) {
        return (Optional)this.lookupMethod.invokeObject(providerHandle, resourceKeyHandle);
    }


    public Optional<Object> get(Object providerHandle, Object resourceKeyHandle) {
        return (Optional)this.getMethod.invokeObject(providerHandle, resourceKeyHandle);
    }

    public Object lookupOrThrow(Object providerHandle, Object resourceKeyHandle) {
        return this.lookupOrThrowMethod.invokeObject(providerHandle, resourceKeyHandle);
    }
}


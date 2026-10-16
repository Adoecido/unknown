package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingMethod;
import java.util.stream.Stream;

public class MResourceKeyRegistryLookupStreamBridge
extends Mapping {
    private static final String LIST_ELEMENTS_METHOD_NAME = "listElements";
    private final MappingMethod listElementsMethod;

    public MResourceKeyRegistryLookupStreamBridge() {
        super(MappedClasses.Da);
        this.listElementsMethod = this.Y(LIST_ELEMENTS_METHOD_NAME, true, Stream.class, new Class[]{});
    }

    public Stream<Object> listElements(Object registryLookup) {
        return (Stream)this.listElementsMethod.invokeObject(registryLookup, new Object[0]);
    }
}


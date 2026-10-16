package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;
import java.util.Collection;

public class ResourceManager
extends Wrapper {
    public Collection<String> getSelectedIds() {
        return (Collection)ResourceManager.sonyVegasInstance.getMappingsMapperCompat().packRepository.getSelectedIds(this.I);
    }

    public ResourceManager(Object handle) {
        super(handle);
    }
}

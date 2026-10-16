package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingField;
import vegas.sony.mapping.MappingMethod;

public class MBlockStateContainerBridge
extends Mapping {
    private final MappingMethod getOrUpdateMethod;
    private final MappingField textureViewField;

    public Object getTextureView(Object atlas) {
        return this.textureViewField.getObject(atlas);
    }

    public Object getOrUpdate(Object atlas, Object renderState) {
        return this.getOrUpdateMethod.invokeObject(atlas, renderState);
    }

    public MBlockStateContainerBridge() {
        super(MappedClasses.uF);
        this.getOrUpdateMethod = this.Y("getOrUpdate", true, MappedClasses.uC, new Class[]{MappedClasses.zE});
        this.textureViewField = this.J("textureView", true, MappedClasses.GPU_TEXTURE_VIEW);
    }
}


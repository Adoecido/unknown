package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingMethod;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.wrapper.impl.ResourceLocationConstantPair;

public class MModelManager
extends Mapping {
    private static final String GET_ATLAS_OR_THROW = "getAtlasOrThrow";
    private final MappingMethod getAtlasMethod;

    public MModelManager() {
        this(ResourceLocationConstantPair.getControlFlowState());
    }

    private MModelManager(GuiComponent[] guiComponentArray) {
        super(MappedClasses.q4);
        this.getAtlasMethod = this.Y(GET_ATLAS_OR_THROW, true, MappedClasses.L, new Class[]{MappedClasses.zC});
        if (GuiComponent.getLegacyComponentState() == null) {
            ResourceLocationConstantPair.setControlFlowState(new GuiComponent[1]);
        }
    }

    public Object getAtlas(Object modelManagerHandle, Object atlasLocation) {
        return this.getAtlasMethod.invokeObject(modelManagerHandle, atlasLocation);
    }
}

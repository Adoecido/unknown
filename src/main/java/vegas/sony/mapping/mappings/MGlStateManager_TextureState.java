package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingField;
import vegas.sony.mapping.MappingFieldBuilder;
import vegas.sony.wrapper.impl.ForgeVersion;

public class MGlStateManager_TextureState
extends Mapping {
    public final MappingField textureNameField;

    public MGlStateManager_TextureState() {
        super(MappedClasses.Zn);
        this.textureNameField = ((MappingFieldBuilder)this.fieldBuilder("textureName", Integer.TYPE)
                .setNameForVersion(ForgeVersion.MC_1_20_6.n(), "binding"))
                .buildField();
    }

    public int getTextureName(Object textureStateHandle) {
        return this.textureNameField.getInt(textureStateHandle);
    }

    public void setTextureName(Object textureStateHandle, int textureName) {
        this.textureNameField.setInt(textureStateHandle, textureName);
    }
}


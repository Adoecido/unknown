package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingField;

public class MItemBlockBridge
extends Mapping {
    private static final String BLOCK_FIELD_NAME = "block";
    private final MappingField blockField;

    public Object getBlock(Object itemBlock) {
        return this.blockField.getObject(itemBlock);
    }

    public MItemBlockBridge() {
        super(MappedClasses.Vw);
        this.blockField = this.J(BLOCK_FIELD_NAME, true, MappedClasses.Zk);
    }
}


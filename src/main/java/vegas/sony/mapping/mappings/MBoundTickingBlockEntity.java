package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingField;

public class MBoundTickingBlockEntity
extends Mapping {
    private static final String BLOCK_ENTITY_FIELD_NAME = "blockEntity";
    private final MappingField blockEntityField;

    public MBoundTickingBlockEntity() {
        super(MappedClasses.BOUND_TICKING_BLOCK_ENTITY);
        this.blockEntityField = this.J(BLOCK_ENTITY_FIELD_NAME, true, MappedClasses.ZI);
    }

    public Object getBlockEntity(Object boundTickingBlockEntity) {
        return this.blockEntityField.getObject(boundTickingBlockEntity);
    }
}

package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingField;

public class MItemBlock
extends Mapping {
    private static final String STORAGE_FIELD_NAME = "storage";
    private final MappingField storageField;

    public Object getStorage(Object itemBlock) {
        return this.storageField.getObject(itemBlock);
    }

    public MItemBlock() {
        super(MappedClasses.I);
        this.storageField = this.J(STORAGE_FIELD_NAME, true, MappedClasses.zd);
    }
}


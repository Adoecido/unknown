package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingField;

public class MMatrixStackEntry
extends Mapping {
    private static final String MATRIX_FIELD_NAME = "matrix";
    private final MappingField matrixField;

    private Object readMatrix(Object matrixStackEntry) {
        return this.matrixField.getObject(matrixStackEntry);
    }

    public static Object getMatrix(MMatrixStackEntry mapping, Object matrixStackEntry) {
        return mapping.readMatrix(matrixStackEntry);
    }

    public MMatrixStackEntry() {
        super(MappedClasses.G);
        this.matrixField = this.J(MATRIX_FIELD_NAME, true, MappedClasses.qr);
    }
}


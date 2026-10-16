package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MMatrixStackEntry;
import vegas.sony.wrapper.Wrapper;

public class MatrixStackEntry
extends Wrapper {
    public Matrix4f getMatrix() {
        return new Matrix4f(MMatrixStackEntry.getMatrix(MatrixStackEntry.sonyVegasInstance.getMappingsMapperCompat().CK, this.I));
    }

    public MatrixStackEntry(Object wrappedObject) {
        super(wrappedObject);
    }
}

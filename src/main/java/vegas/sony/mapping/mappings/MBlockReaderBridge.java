package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingMethod;

public class MBlockReaderBridge
extends Mapping {
    private static final String GET_BLOCK_STATE_METHOD_NAME = "getBlockState";
    private final MappingMethod getBlockStateMethod;

    public Object getBlockState(Object blockReader, Object blockPos) {
        return this.getBlockStateMethod.invokeObject(blockReader, blockPos);
    }

    public MBlockReaderBridge() {
        super(MappedClasses.zJ);
        this.getBlockStateMethod = this.Y(GET_BLOCK_STATE_METHOD_NAME, true, MappedClasses.Zl, new Class[]{MappedClasses.lf});
    }
}


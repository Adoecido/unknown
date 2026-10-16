package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingField;

public class MSPacketChunkData
extends Mapping {
    private final MappingField chunkZField;
    private final MappingField extractedDataField;
    private final MappingField chunkXField;

    public int getChunkX(Object packet) {
        return this.chunkXField.getInt(packet);
    }

    public int getChunkZ(Object packet) {
        return this.chunkZField.getInt(packet);
    }

    public Object getExtractedData(Object packet) {
        return this.extractedDataField.getObject(packet);
    }

    public MSPacketChunkData() {
        super(MappedClasses.ZJ);
        this.extractedDataField = this.J("extractedData", true, MappedClasses.uU);
        this.chunkXField = this.J("chunkX", true, Integer.TYPE);
        this.chunkZField = this.J("chunkZ", true, Integer.TYPE);
    }
}


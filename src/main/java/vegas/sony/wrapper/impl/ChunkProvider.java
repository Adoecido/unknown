package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class ChunkProvider
extends Wrapper {
    public boolean isChunkLoaded(int chunkX, int chunkZ) {
        return ChunkProvider.sonyVegasInstance.getMappingsMapperCompat().chunkProvider
                .chunkExists(this.I, chunkX, chunkZ);
    }

    public ChunkProvider(Object chunkProviderHandle) {
        super(chunkProviderHandle);
    }
}

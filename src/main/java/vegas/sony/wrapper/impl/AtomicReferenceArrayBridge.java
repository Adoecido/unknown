package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class AtomicReferenceArrayBridge
extends Wrapper {
    public AtomicReferenceArray getChunks() {
        return (AtomicReferenceArray)AtomicReferenceArrayBridge.sonyVegasInstance.getMappingsMapperCompat().H.getChunks(this.I);
    }

    public AtomicReferenceArrayBridge(Object wrappedObject) {
        super(wrappedObject);
    }
}

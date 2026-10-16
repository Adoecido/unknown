package vegas.sony.wrapper.impl;

public class RegistryNamespaced
extends RegistrySimple {
    public Object getByValue(int id) {
        return RegistryNamespaced.sonyVegasInstance.getMappingsMapperCompat().qc.getByValue(this.I, id);
    }

    public RegistryNamespaced(Object wrappedObject) {
        super(wrappedObject);
    }
}

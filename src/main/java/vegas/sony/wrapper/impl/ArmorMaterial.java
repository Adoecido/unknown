package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MArmorMaterial;
import vegas.sony.wrapper.Wrapper;

public class ArmorMaterial
extends Wrapper {
    public static ArmorMaterial gold() {
        return new ArmorMaterial(MArmorMaterial.getGold(ArmorMaterial.sonyVegasInstance.getMappingsMapperCompat().e));
    }

    public static ArmorMaterial iron() {
        return new ArmorMaterial(MArmorMaterial.getIron(ArmorMaterial.sonyVegasInstance.getMappingsMapperCompat().e));
    }

    public static ArmorMaterial chain() {
        return new ArmorMaterial(MArmorMaterial.getChain(ArmorMaterial.sonyVegasInstance.getMappingsMapperCompat().e));
    }

    public static ArmorMaterial leather() {
        return new ArmorMaterial(MArmorMaterial.getLeather(ArmorMaterial.sonyVegasInstance.getMappingsMapperCompat().e));
    }

    public ArmorMaterial(Object wrappedObject) {
        super(wrappedObject);
    }

    public static ArmorMaterial diamond() {
        return new ArmorMaterial(MArmorMaterial.getDiamond(ArmorMaterial.sonyVegasInstance.getMappingsMapperCompat().e));
    }
}

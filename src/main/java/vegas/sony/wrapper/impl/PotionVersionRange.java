package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MPotionVersionRange;
import vegas.sony.wrapper.Wrapper;
import java.util.Set;

public class PotionVersionRange
extends Wrapper {
    public Set entrySet() {
        return (Set)MPotionVersionRange.getEntrySet(PotionVersionRange.sonyVegasInstance.getMappingsMapperCompat().itemEnchantments, this.I);
    }

    public static PotionVersionRange empty() {
        return new PotionVersionRange(MPotionVersionRange.getEmpty(PotionVersionRange.sonyVegasInstance.getMappingsMapperCompat().itemEnchantments));
    }

    public PotionVersionRange(Object handle) {
        super(handle);
    }
}

package vegas.sony.unmap;

import vegas.sony.unmap.PropertyKey;

public final class PropertyContainerBooleanKeyC
extends PropertyKey<Boolean> {
    @Override
    public Boolean getDefaultValue() {
        return this.getFalseDefault();
    }

    Boolean getFalseDefault() {
        return false;
    }
}

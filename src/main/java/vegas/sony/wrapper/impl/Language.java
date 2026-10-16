package vegas.sony.wrapper.impl;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.wrapper.Wrapper;

public class Language
extends Wrapper {
    public Language(Object languageHandle) {
        super(languageHandle);
    }


    public boolean isUnicode() {
        if (ForgeVersion.MC_1_16_5.d() && !MappedClasses.Vi.isInstance(this.getObject())) {
            return false;
        }
        return Language.sonyVegasInstance.getMappingsMapperCompat().language.isUnicode(this.I);
    }
}


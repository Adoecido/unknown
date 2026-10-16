package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class I18n
extends Wrapper {
    public I18n(Object i18nHandle) {
        super(i18nHandle);
    }

    public static String format(String translationKey, Object ... arguments) {
        return I18n.sonyVegasInstance.getMappingsMapperCompat().i18n.format(translationKey, arguments);
    }

    public static Language getLanguage() {
        return new Language(I18n.sonyVegasInstance.getMappingsMapperCompat().i18n.getLanguage());
    }
}

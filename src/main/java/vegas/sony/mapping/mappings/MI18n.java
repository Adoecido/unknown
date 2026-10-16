package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingField;
import vegas.sony.mapping.MappingMethod;
import vegas.sony.utils.TimerUtil;
import vegas.sony.wrapper.impl.ForgeVersion;

public class MI18n
extends Mapping {
    private final MappingField languageField;
    private final MappingMethod formatMethod;


    public MI18n() {
        this(TimerUtil.p());
    }

    private MI18n(String timerState) {
        super(MappedClasses.Vm);
        if (ForgeVersion.MC_1_16_5.d()) {
            this.languageField = this.registerStaticField("field_239501_a_", true, MappedClasses.Fn);
        } else {
            this.languageField = this.registerStaticField("i18nLocale", true, MappedClasses.Fn);
        }
        this.formatMethod = this.registerStaticMethod("format", true, String.class, String.class, Object[].class);
    }

    public String format(String translationKey, Object ... arguments) {
        return (String)this.formatMethod.invokeObject(null, translationKey, arguments);
    }

    public Object getLanguage() {
        return this.languageField.getObject(null);
    }
}

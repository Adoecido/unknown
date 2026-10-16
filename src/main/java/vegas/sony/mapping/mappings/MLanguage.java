package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingField;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.wrapper.Wrapper;
import vegas.sony.wrapper.impl.ForgeVersion;

public class MLanguage
extends Mapping {
    private final MappingField unicodeField;

    public boolean isUnicode(Object languageHandle) {
        return this.unicodeField.getBoolean(languageHandle);
    }

    public MLanguage() {
        this(MMappedClassSlotFp.l());
    }

    private MLanguage(GuiComponent[] legacyComponentState) {
        super(MappedClasses.Fn);
        if (ForgeVersion.MC_1_16_5.d()) {
            this.unicodeField = this.registerInstanceFieldForOwner(
                    MappedClasses.Vi, "field_239496_d_", Wrapper.isNativeAvailable, Boolean.TYPE);
        } else if (ForgeVersion.MC_1_7_10.L()) {
            this.unicodeField = this.J("field_135029_d", Wrapper.isNativeAvailable, Boolean.TYPE);
        } else {
            this.unicodeField = this.J("unicode", true, Boolean.TYPE);
        }
    }

}


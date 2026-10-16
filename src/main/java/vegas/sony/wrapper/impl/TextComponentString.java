package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MTextComponentString;

public class TextComponentString
extends ITextComponent {
    public TextComponentString(Object handle) {
        super(handle);
    }

    public String getText() {
        return MTextComponentString.getText(TextComponentString.sonyVegasInstance.getMappingsMapperCompat().textComponentString, this.getObject());
    }

    public static TextComponentString create(String text) {
        return new TextComponentString(MTextComponentString.create(TextComponentString.sonyVegasInstance.getMappingsMapperCompat().textComponentString, text));
    }

    public void setText(String text) {
        MTextComponentString.setText(TextComponentString.sonyVegasInstance.getMappingsMapperCompat().textComponentString, this.getObject(), text);
    }
}

package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class TextFormatting
extends Wrapper {
    public static TextFormatting fromName(String name) {
        Object formattingHandle = TextFormatting.sonyVegasInstance.getMappingsMapperCompat().textFormatting.getByName(name);
        TextFormatting textFormatting = formattingHandle != null ? new TextFormatting(formattingHandle) : null;
        return textFormatting;
    }

    public TextFormatting(Object object) {
        super(object);
    }

    public Integer getColor() {
        return TextFormatting.sonyVegasInstance.getMappingsMapperCompat().textFormatting.getColor(this.getObject());
    }


    public String getName() {
        return TextFormatting.sonyVegasInstance.getMappingsMapperCompat().textFormatting.getName(this.getObject());
    }
}


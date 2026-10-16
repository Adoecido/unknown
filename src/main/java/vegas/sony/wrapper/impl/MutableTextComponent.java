package vegas.sony.wrapper.impl;

import vegas.sony.unmap.TextComponentBase;

public class MutableTextComponent
extends TextComponent {
    public MutableTextComponent(Object object) {
        super(object);
    }

    public TextComponent f(ITextComponent t3_02) {
        return new TextComponent(MutableTextComponent.sonyVegasInstance.getMappingsMapperCompat().RJ.p(this.getObject(), t3_02.getObject()));
    }

    public TextComponent C(TextComponentBase t7_02) {
        return new TextComponent(MutableTextComponent.sonyVegasInstance.getMappingsMapperCompat().RJ.v(this.getObject(), t7_02.getObject()));
    }
}


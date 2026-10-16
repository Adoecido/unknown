package vegas.sony.wrapper.impl;

import vegas.sony.SonyVegas;
import vegas.sony.unmap.TextComponentBase;
import vegas.sony.wrapper.Wrapper;

import java.util.List;
import java.util.stream.Collectors;

public class TextComponentBaseBridge
extends ITextComponent {
    public TextComponentBaseBridge(Object handle) {
        super(handle);
    }

    public TextComponentBaseBridge setStyle(TextComponentBase style) {
        if (ForgeVersion.MC_1_20_6.v()) {
            SonyVegas.notifyNativeStackTrace();
            throw new UnsupportedOperationException("Unsupported");
        }
        return new TextComponentBaseBridge(TextComponentBaseBridge.sonyVegasInstance.getMappings().mutableComponent.setStyle(this.getObject(), style.getObject()));
    }

    private static UnsupportedOperationException b(UnsupportedOperationException unsupportedOperationException) {
        return unsupportedOperationException;
    }

    public static TextComponentBaseBridge create(StringTextComponentBase contents, List<ITextComponent> siblings, TextComponentBase style) {
        if (ForgeVersion.MC_1_20_6.v()) {
            SonyVegas.notifyNativeStackTrace();
            throw new UnsupportedOperationException("Unsupported");
        }
        return new TextComponentBaseBridge(TextComponentBaseBridge.sonyVegasInstance.getMappings().mutableComponent.create(contents.getObject(), siblings.stream().map(Wrapper::getObject).collect(Collectors.toList()), style.getObject()));
    }
}

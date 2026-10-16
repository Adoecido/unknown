package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class ClientChatReceivedEvent
extends Wrapper {
    public ClientChatReceivedEvent(Object object) {
        super(object);
    }

    public ITextComponent P() {
        return new ITextComponent(ClientChatReceivedEvent.sonyVegasInstance.getMappingsMapperCompat().qP.V(this.I));
    }

    public void t(ITextComponent iTextComponent) {
        ClientChatReceivedEvent.sonyVegasInstance.getMappingsMapperCompat().qP.i(this.I, iTextComponent.getObject());
    }
}


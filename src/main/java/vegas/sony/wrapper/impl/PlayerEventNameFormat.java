package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class PlayerEventNameFormat
extends Wrapper {
    public String getUrl() {
        return PlayerEventNameFormat.sonyVegasInstance.getMappingsMapperCompat().resourcePackSendPacket.getUrl(this.I);
    }

    public void setUrl(String url) {
        PlayerEventNameFormat.sonyVegasInstance.getMappingsMapperCompat().resourcePackSendPacket.setUrl(this.I, url);
    }

    public PlayerEventNameFormat(Object handle) {
        super(handle);
    }

    public String getHash() {
        return PlayerEventNameFormat.sonyVegasInstance.getMappingsMapperCompat().resourcePackSendPacket.getHash(this.I);
    }
}

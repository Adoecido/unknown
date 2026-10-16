package vegas.sony.module.render.freecam;

import vegas.sony.mapping.mappings.MFreecamPlayerBridge;
import vegas.sony.wrapper.Wrapper;

public class FreecamPlayerBridge
extends Wrapper {
    public void setCloakZ(double z) {
        MFreecamPlayerBridge.A(FreecamPlayerBridge.sonyVegasInstance.getMappings().w, this.I, z);
    }

    public double getPreviousCloakZ() {
        return MFreecamPlayerBridge.Z(FreecamPlayerBridge.sonyVegasInstance.getMappings().w, this.I);
    }

    public void setPreviousCloakZ(double z) {
        MFreecamPlayerBridge.d(FreecamPlayerBridge.sonyVegasInstance.getMappings().w, this.I, z);
    }

    public double getPreviousCloakY() {
        return MFreecamPlayerBridge.m(FreecamPlayerBridge.sonyVegasInstance.getMappings().w, this.I);
    }

    public double getCloakY() {
        return MFreecamPlayerBridge.Y(FreecamPlayerBridge.sonyVegasInstance.getMappings().w, this.I);
    }

    public double getPreviousCloakX() {
        return MFreecamPlayerBridge.C(FreecamPlayerBridge.sonyVegasInstance.getMappings().w, this.I);
    }

    public void setCloakX(double x) {
        MFreecamPlayerBridge.c(FreecamPlayerBridge.sonyVegasInstance.getMappings().w, this.I, x);
    }

    public double getCloakX() {
        return MFreecamPlayerBridge.T(FreecamPlayerBridge.sonyVegasInstance.getMappings().w, this.I);
    }

    public void setPreviousCloakY(double y) {
        MFreecamPlayerBridge.E(FreecamPlayerBridge.sonyVegasInstance.getMappings().w, this.I, y);
    }

    public void setCloakY(double y) {
        MFreecamPlayerBridge.n(FreecamPlayerBridge.sonyVegasInstance.getMappings().w, this.I, y);
    }

    public void resetBobbingState() {
        MFreecamPlayerBridge.m$src$V$n8prsk(FreecamPlayerBridge.sonyVegasInstance.getMappings().w, this.I);
    }

    public void setPreviousCloakX(double x) {
        MFreecamPlayerBridge.y(FreecamPlayerBridge.sonyVegasInstance.getMappings().w, this.I, x);
    }

    public FreecamPlayerBridge(Object object) {
        super(object);
    }

    public double getCloakZ() {
        return MFreecamPlayerBridge.l(FreecamPlayerBridge.sonyVegasInstance.getMappings().w, this.I);
    }
}

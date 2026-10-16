package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MModelPlayer;
import vegas.sony.wrapper.Wrapper;

public class ModelPlayer
extends Wrapper {
    public boolean isFlying() {
        return MModelPlayer.v(ModelPlayer.sonyVegasInstance.getMappings().Ri, this.I);
    }

    public boolean N() {
        return MModelPlayer.u(ModelPlayer.sonyVegasInstance.getMappings().Ri, this.I);
    }

    public float m$src$F$1kykyr0() {
        return MModelPlayer.p(ModelPlayer.sonyVegasInstance.getMappings().Ri, this.I);
    }

    public boolean isCreativeMode() {
        return MModelPlayer.k(ModelPlayer.sonyVegasInstance.getMappings().Ri, this.I);
    }

    public float l() {
        return MModelPlayer.Y(ModelPlayer.sonyVegasInstance.getMappings().Ri, this.I);
    }

    public boolean H() {
        return MModelPlayer.D(ModelPlayer.sonyVegasInstance.getMappings().Ri, this.I);
    }

    public void c(boolean bl) {
        MModelPlayer.t(ModelPlayer.sonyVegasInstance.getMappings().Ri, this.I, bl);
    }

    public ModelPlayer(Object object) {
        super(object);
    }

    public boolean c() {
        return MModelPlayer.a(ModelPlayer.sonyVegasInstance.getMappings().Ri, this.I);
    }

    public static ModelPlayer Q() {
        return new ModelPlayer(MModelPlayer.C(ModelPlayer.sonyVegasInstance.getMappings().Ri));
    }

    public void G(float f) {
        MModelPlayer.H(ModelPlayer.sonyVegasInstance.getMappings().Ri, this.I, f);
    }

    public void n(boolean bl) {
        MModelPlayer.y(ModelPlayer.sonyVegasInstance.getMappings().Ri, this.I, bl);
    }

    public void R(float f) {
        MModelPlayer.E(ModelPlayer.sonyVegasInstance.getMappings().Ri, this.I, f);
    }
}


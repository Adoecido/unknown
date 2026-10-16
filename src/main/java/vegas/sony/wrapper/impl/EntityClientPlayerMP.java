package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MEntityClientPlayerMP;

public class EntityClientPlayerMP
extends EntityPlayerSP {
    public void N(boolean bl) {
        MEntityClientPlayerMP.D(EntityClientPlayerMP.sonyVegasInstance.getMappings().q_, this.I, bl);
    }

    public void setTicksSinceMovePacket(int n) {
        MEntityClientPlayerMP.Y(EntityClientPlayerMP.sonyVegasInstance.getMappings().q_, this.I, n);
    }

    public void x(double d) {
        MEntityClientPlayerMP.v(EntityClientPlayerMP.sonyVegasInstance.getMappings().q_, this.I, d);
    }

    public double y$src$D$lo113c() {
        return MEntityClientPlayerMP.H(EntityClientPlayerMP.sonyVegasInstance.getMappings().q_, this.I);
    }

    public double I$src$D$kxmwm0() {
        return MEntityClientPlayerMP.x(EntityClientPlayerMP.sonyVegasInstance.getMappings().q_, this.I);
    }

    public void w(float f) {
        MEntityClientPlayerMP.O(EntityClientPlayerMP.sonyVegasInstance.getMappings().q_, this.I, f);
    }

    public void Q(double d) {
        MEntityClientPlayerMP.q(EntityClientPlayerMP.sonyVegasInstance.getMappings().q_, this.I, d);
    }

    public void S(float f) {
        MEntityClientPlayerMP.D(EntityClientPlayerMP.sonyVegasInstance.getMappings().q_, this.I, f);
    }

    @Override
    public void sendChatMessage(String string) {
        MEntityClientPlayerMP.e(EntityClientPlayerMP.sonyVegasInstance.getMappings().q_, this.I, string);
    }

    public double O$src$D$l0xo66() {
        return MEntityClientPlayerMP.g(EntityClientPlayerMP.sonyVegasInstance.getMappings().q_, this.I);
    }

    public float E$src$F$kvfqaa() {
        return MEntityClientPlayerMP.K(EntityClientPlayerMP.sonyVegasInstance.getMappings().q_, this.I);
    }

    public void J(double d) {
        MEntityClientPlayerMP.z(EntityClientPlayerMP.sonyVegasInstance.getMappings().q_, this.I, d);
    }

    public float o() {
        return MEntityClientPlayerMP.l(EntityClientPlayerMP.sonyVegasInstance.getMappings().q_, this.I);
    }

    public boolean a() {
        return MEntityClientPlayerMP.T(EntityClientPlayerMP.sonyVegasInstance.getMappings().q_, this.I);
    }

    public void A(boolean bl) {
        MEntityClientPlayerMP.F(EntityClientPlayerMP.sonyVegasInstance.getMappings().q_, this.I, bl);
    }

    @Override
    public NetHandlerPlayClientImpl sendQueue() {
        return new NetHandlerPlayClientImpl(MEntityClientPlayerMP.W(EntityClientPlayerMP.sonyVegasInstance.getMappings().q_, this.I));
    }

    public EntityClientPlayerMP(Object object) {
        super(object);
    }

    public void a(double d) {
        MEntityClientPlayerMP.n(EntityClientPlayerMP.sonyVegasInstance.getMappings().q_, this.I, d);
    }

    public void v(boolean bl) {
        MEntityClientPlayerMP.S(EntityClientPlayerMP.sonyVegasInstance.getMappings().q_, this.I, bl);
    }

    public double w() {
        return MEntityClientPlayerMP.p(EntityClientPlayerMP.sonyVegasInstance.getMappings().q_, this.I);
    }

    public int getTicksSinceMovePacket() {
        return MEntityClientPlayerMP.K$src$I$jaup0x(EntityClientPlayerMP.sonyVegasInstance.getMappings().q_, this.I);
    }

    public boolean R$src$Z$l2l2h7() {
        return MEntityClientPlayerMP.Q(EntityClientPlayerMP.sonyVegasInstance.getMappings().q_, this.I);
    }
}


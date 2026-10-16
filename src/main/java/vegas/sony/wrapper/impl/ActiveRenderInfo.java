package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MActiveRenderInfo;
import vegas.sony.wrapper.Wrapper;

public class ActiveRenderInfo
extends Wrapper {
    public Entity W() {
        return new Entity(MActiveRenderInfo.Z(ActiveRenderInfo.sonyVegasInstance.getMappings().hi, this.I));
    }

    public void g(Vec3 vec3) {
        MActiveRenderInfo.P(ActiveRenderInfo.sonyVegasInstance.getMappings().hi, this.I, vec3.getObject());
    }

    public Quaternion G() {
        return new Quaternion(MActiveRenderInfo.d(ActiveRenderInfo.sonyVegasInstance.getMappings().hi, this.I));
    }

    public float Z() {
        return MActiveRenderInfo.q(ActiveRenderInfo.sonyVegasInstance.getMappings().hi, this.I);
    }

    public float x() {
        return MActiveRenderInfo.A(ActiveRenderInfo.sonyVegasInstance.getMappings().hi, this.I);
    }

    public Vec3 o() {
        return new Vec3(MActiveRenderInfo.p(ActiveRenderInfo.sonyVegasInstance.getMappings().hi, this.I));
    }

    public ActiveRenderInfo(Object object) {
        super(object);
    }

    public BlockReader x$src$Lvegas_sony_wrapper_impl_BlockReader_$120g8sh() {
        return new BlockReader(MActiveRenderInfo.i(ActiveRenderInfo.sonyVegasInstance.getMappings().hi, this.I));
    }
}


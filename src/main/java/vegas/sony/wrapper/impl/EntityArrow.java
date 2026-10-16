package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MEntityArrow;

public class EntityArrow
extends Entity {

    public double o() {
        if (ForgeVersion.MC_1_21_0.d()) {
            return this.P$src$D$xovcst();
        }
        return MEntityArrow.g(EntityArrow.sonyVegasInstance.getMappings().qo, this.I);
    }

    public double L() {
        if (ForgeVersion.MC_1_21_0.d()) {
            return this.P$src$D$xovcst();
        }
        return MEntityArrow.d(EntityArrow.sonyVegasInstance.getMappings().qo, this.I);
    }

    public double P$src$D$xovcst() {
        return MEntityArrow.t(EntityArrow.sonyVegasInstance.getMappings().qo, this.I);
    }

    public EntityArrow(Object object) {
        super(object);
    }

    public double X$src$D$xt9pjp() {
        if (ForgeVersion.MC_1_21_0.d()) {
            return this.P$src$D$xovcst();
        }
        return MEntityArrow.D(EntityArrow.sonyVegasInstance.getMappings().qo, this.I);
    }
}


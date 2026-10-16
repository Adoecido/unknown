package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class Material
extends Wrapper {
    private static Material air;
    private static Material water;
    private static Material vine;

    public boolean isReplaceable() {
        return Material.sonyVegasInstance.getMappingsMapperCompat().Cn.isReplaceable(this.I);
    }


    public static Material vine() {
        if (vine == null) {
            vine = new Material(Material.sonyVegasInstance.getMappingsMapperCompat().Cn.getVine());
        }
        return vine;
    }

    public boolean isToolNotRequired() {
        if (ForgeVersion.MC_1_16_5.d()) {
            return false;
        }
        return Material.sonyVegasInstance.getMappingsMapperCompat().Cn.isToolNotRequired(this.I);
    }

    public static Material air() {
        if (air == null) {
            air = new Material(Material.sonyVegasInstance.getMappingsMapperCompat().Cn.getAir());
        }
        return air;
    }

    public static Material water() {
        if (water == null) {
            water = new Material(Material.sonyVegasInstance.getMappingsMapperCompat().Cn.getWater());
        }
        return water;
    }

    public static Material fire() {
        return new Material(Material.sonyVegasInstance.getMappingsMapperCompat().Cn.getFire());
    }

    public boolean isLiquid() {
        return Material.sonyVegasInstance.getMappingsMapperCompat().Cn.isLiquid(this.I);
    }

    public Material(Object object) {
        super(object);
    }

    public boolean blocksMovement() {
        return Material.sonyVegasInstance.getMappingsMapperCompat().Cn.blocksMovement(this.I);
    }

    public boolean isSolid() {
        return Material.sonyVegasInstance.getMappingsMapperCompat().Cn.isSolid(this.I);
    }
}


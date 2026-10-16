package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class Blocks
extends Wrapper {
    private static Block ladder;
    private static Block air;
    private static Block powderSnow;
    private static Block stone;
    private static Block web;

    public Blocks(Object wrappedObject) {
        super(wrappedObject);
    }

    public static Block air() {
        if (air == null) {
            air = new Block(Blocks.sonyVegasInstance.getMappingsMapperCompat().blocks.getAir());
        }
        return air;
    }

    public static Block stone() {
        if (stone == null) {
            stone = new Block(Blocks.sonyVegasInstance.getMappingsMapperCompat().blocks.getStone());
        }
        return stone;
    }

    public static Block ladder() {
        if (ladder == null) {
            ladder = new Block(Blocks.sonyVegasInstance.getMappingsMapperCompat().blocks.getLadder());
        }
        return ladder;
    }

    public static Block web() {
        if (web == null) {
            Object o = Blocks.sonyVegasInstance.getMappingsMapperCompat().blocks.getWeb();
            web = o == null ? null : new Block(o);
        }
        return web;
    }

    public static Block powderSnow() {
        if (powderSnow == null) {
            powderSnow = new Block(Blocks.sonyVegasInstance.getMappingsMapperCompat().blocks.getPowderSnow());
        }
        return powderSnow;
    }

}


package vegas.sony.wrapper.impl;

import com.google.common.collect.ImmutableMap;
import vegas.sony.mapping.mappings.MIBlockState;
import vegas.sony.wrapper.Wrapper;

public class BlockState
extends Wrapper {
    public boolean u() {
        return BlockState.sonyVegasInstance.getMappings().DE.d(this.I);
    }

    public boolean Y() {
        return BlockState.sonyVegasInstance.getMappings().DE.W(this.I);
    }

    public Object I(BlockProperty blockProperty) {
        if (ForgeVersion.MC_1_20_6.d()) {
            return BlockState.sonyVegasInstance.getMappings().DE.o(this.I, blockProperty.getObject());
        }
        ImmutableMap immutableMap = BlockState.sonyVegasInstance.getMappings().DE.w(this.I);
        for (Object e : immutableMap.keySet()) {
            if (!e.getClass().equals(blockProperty.getObject().getClass())) continue;
            Object object = immutableMap.get(e);
            return object;
        }
        if (immutableMap.containsKey(blockProperty.getObject())) {
            Object object = immutableMap.get(blockProperty.getObject());
            return object;
        }
        return null;
    }

    public boolean x() {
        return BlockState.sonyVegasInstance.getMappings().DE.e(this.I);
    }

    public Block getBlock() {
        return new Block(BlockState.sonyVegasInstance.getMappings().DE.v(this.I));
    }

    public BlockState(Object object) {
        super(object);
    }

    public boolean g() {
        return BlockState.sonyVegasInstance.getMappings().DE.I(this.I);
    }

    public BlockStateWorldBridge j() {
        return new BlockStateWorldBridge(MIBlockState.j(BlockState.sonyVegasInstance.getMappings().DE, this.I));
    }

}


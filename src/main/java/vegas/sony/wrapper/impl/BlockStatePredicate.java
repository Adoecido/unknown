package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.impl.Block;
import vegas.sony.wrapper.impl.BlockState;

public class BlockStatePredicate
extends BlockState {
    public BlockStatePredicate(Object object) {
        super(object);
    }

    public boolean w(Block px_12) {
        return this.getBlock().equals(px_12);
    }
}


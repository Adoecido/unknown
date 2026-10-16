package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MPlayerInput;
import vegas.sony.wrapper.Wrapper;

public class PlayerInput
extends Wrapper {
    public PlayerInput(Object handle) {
        super(handle);
    }

    public void setForwardImpulse(float forwardImpulse) {
        MPlayerInput.setY(PlayerInput.sonyVegasInstance.getMappingsMapperCompat().playerInputVector, this.I, forwardImpulse);
    }

    public float getForwardImpulse() {
        return MPlayerInput.getY(PlayerInput.sonyVegasInstance.getMappingsMapperCompat().playerInputVector, this.I);
    }

    public float getLeftImpulse() {
        return MPlayerInput.getX(PlayerInput.sonyVegasInstance.getMappingsMapperCompat().playerInputVector, this.I);
    }

    public void setLeftImpulse(float leftImpulse) {
        MPlayerInput.setX(PlayerInput.sonyVegasInstance.getMappingsMapperCompat().playerInputVector, this.I, leftImpulse);
    }
}

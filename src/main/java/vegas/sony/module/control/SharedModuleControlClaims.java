package vegas.sony.module.control;

import vegas.sony.input.MouseButtonInputLock;
import vegas.sony.input.MovementInputLock;
import vegas.sony.rotation.RotationControlClaim;

public class SharedModuleControlClaims {
    public static final PrimaryActionControlClaim primaryAction = new PrimaryActionControlClaim();
    public static final MouseButtonInputLock mouseButtons = new MouseButtonInputLock();
    public static final SecondaryActionControlClaim secondaryAction = new SecondaryActionControlClaim();
    public static final RightClickUseControlClaim rightClickUse = new RightClickUseControlClaim();
    public static final MovementInputLock movementInput = new MovementInputLock();
    public static final RenderPassControlClaim renderPass = new RenderPassControlClaim();
    public static final RotationControlClaim rotation = new RotationControlClaim();
    public static final MouseOverUpdateControlClaim mouseOverUpdate = new MouseOverUpdateControlClaim();
}

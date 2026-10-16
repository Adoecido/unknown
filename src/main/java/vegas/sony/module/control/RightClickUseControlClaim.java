package vegas.sony.module.control;

public class RightClickUseControlClaim
extends ModuleControlClaim {
    public void blockUse() {
        this.setClaimed(true);
    }
}

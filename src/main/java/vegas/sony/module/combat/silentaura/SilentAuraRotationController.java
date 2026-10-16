package vegas.sony.module.combat.silentaura;

import vegas.sony.module.combat.SilentAura;
import vegas.sony.rotation.AdaptiveRotationController;

public class SilentAuraRotationController
extends AdaptiveRotationController {
    private final SilentAura silentAura;

    @Override
    public float getSpeed() {
        double meanLog = 0.65;
        double stdDevLog = 0.25;
        double uniform1 = Math.random();
        double uniform2 = Math.random();
        if (uniform1 < 1.0E-4) {
            uniform1 = 1.0E-4;
        }
        double gaussian = Math.sqrt(-2.0 * Math.log(uniform1)) * Math.cos(Math.PI * 2 * uniform2);
        double multiplier = Math.exp(meanLog + stdDevLog * gaussian);
        multiplier = Math.max(1.4, Math.min(3.0, multiplier));
        double reach = (double)((Double)this.silentAura.getAimSpeed().getValue()).floatValue() * multiplier;
        return (float)reach;
    }


    public SilentAuraRotationController(SilentAura owner) {
        this.silentAura = owner;
    }
}

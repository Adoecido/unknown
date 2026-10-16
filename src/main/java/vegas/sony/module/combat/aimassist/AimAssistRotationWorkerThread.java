package vegas.sony.module.combat.aimassist;

import vegas.sony.SonyVegas;
import vegas.sony.runtime.CheatBreakerDebugLog;

public class AimAssistRotationWorkerThread
extends Thread {
    private final AimAssistRotationSubModule rotationModule;
    private boolean failureLogged;

    public AimAssistRotationWorkerThread(AimAssistRotationSubModule rotationModule) {
        this.rotationModule = rotationModule;
    }

    @Override
    public void run() {
        while (!SonyVegas.INSTANCE.isEnabled()) {
            try {
                Thread.sleep(1L);
                if (!this.rotationModule.isEnabled() || !this.rotationModule.isSelectedSubModule()) continue;
                AimAssistRotationSubModule.runWorkerTick(this.rotationModule);
            }
            catch (Exception failure) {
                if (!this.failureLogged) {
                    this.failureLogged = true;
                    CheatBreakerDebugLog.throwable("AIM_SIMPLE_WORKER_FAILED", failure);
                }
            }
        }
    }
}


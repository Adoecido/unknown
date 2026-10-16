package vegas.sony.module.combat.aimassist;

import vegas.sony.SonyVegas;
import vegas.sony.runtime.CheatBreakerDebugLog;

public class AimAssistWorkerThread
extends Thread {
    private final AimAssistTargetingSubModule targetingSubModule;
    private boolean failureLogged;

    @Override
    public void run() {
        while (!SonyVegas.INSTANCE.isEnabled()) {
            try {
                Thread.sleep(1L);
                if (!this.targetingSubModule.isEnabled() || !this.targetingSubModule.isSelectedSubModule()) continue;
                AimAssistTargetingSubModule.runWorkerTick(this.targetingSubModule);
            }
            catch (Exception failure) {
                if (!this.failureLogged) {
                    this.failureLogged = true;
                    CheatBreakerDebugLog.throwable("AIM_ADAPTIVE_WORKER_FAILED", failure);
                }
            }
        }
    }

    public AimAssistWorkerThread(AimAssistTargetingSubModule targetingSubModule) {
        this.targetingSubModule = targetingSubModule;
    }
}


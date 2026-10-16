package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.mapping.MappingTask;
import vegas.sony.notification.NotificationType;
import java.util.ArrayList;
import java.util.List;

public class MappingTaskSet {
    protected List<MappingTask> D = new ArrayList<MappingTask>();

    public void d() {
        int n = 0;
        int successCount = 0;
        int failCount = 0;
        for (MappingTask mappingTask : this.D) {
            ++n;
            if (mappingTask.isApplied()) continue;
            int n2 = 0;
            String phase = "prepare";
            String target = MappingTaskSet.targetName(mappingTask);
            try {
                mappingTask.prepare();
                phase = "transform";
                mappingTask.transform();
                phase = "serialize";
                mappingTask.serialize();
                phase = "commit";
                n2 = mappingTask.commit();
            }
            catch (Throwable throwable) {
                n2 = -1;
                String string = "Mapping task " + n + " failed during " + phase
                    + ": " + mappingTask.getClass().getName()
                    + " -> " + target;
                SonyVegas.debugLog(string);
                SonyVegas.logThrowable(throwable);
                vegas.sony.runtime.CheatBreakerDebugLog.info("TASK_FAIL " + string);
                vegas.sony.runtime.CheatBreakerDebugLog.throwable("task " + n + " " + phase, throwable);
            }
            if (n2 == 0) {
                ++successCount;
                continue;
            }
            ++failCount;
            String string = n2 + " " + n;
            if (n2 != -1) {
                String string2 = "Mapping task " + n + " commit returned " + n2
                    + ": " + mappingTask.getClass().getName()
                    + " -> " + target;
                SonyVegas.debugLog(string2);
                vegas.sony.runtime.CheatBreakerDebugLog.info("TASK_COMMIT " + string2);
            }
            if (SonyVegas.INSTANCE.getNotificationManager() == null
                    || vegas.sony.runtime.NativeBridge.isBadlion189Runtime()) continue;
            // One failed hook must degrade to "some features may not
            // function", never abort the whole boot: the notification UI
            // itself may not be ready (fonts/render) this early.
            try {
                SonyVegas.INSTANCE.getNotificationManager().show("Error with injection", "Please report to support:\nError code " + string + "\n\nSome features may not function", NotificationType.ALERT, 30000L, true);
            } catch (Throwable notificationFailure) {
                vegas.sony.runtime.CheatBreakerDebugLog.throwable("notify task " + n, notificationFailure);
            }
        }
        if (failCount > 0) {
            SonyVegas.debugLog("Hook summary: " + successCount + "/" + n + " ok, "
                    + failCount + " failed");
        }
        vegas.sony.runtime.CheatBreakerDebugLog.info("TASK_SUMMARY total=" + n
                + " ok=" + successCount + " fail=" + failCount);
    }

    /**
     * Badlion 1.8.9 bring-up: on the live client, hooks committed through
     * scb/RedefineClasses only started executing after an extra
     * RetransformClasses cycle (observed via the gcb census probe). Re-run
     * retransform once over every applied target so hook activation does not
     * depend on JVM timing. Only invoked for Badlion; other runtimes keep
     * their exact previous behavior.
     */
    public void retransformApplied() {
        int kicked = 0;
        for (MappingTask mappingTask : this.D) {
            if (!mappingTask.isApplied()) {
                continue;
            }
            try {
                Class target = mappingTask.getTargetClass();
                if (target == null) {
                    continue;
                }
                // Discard the bytes: the retransform itself settles activation.
                vegas.sony.runtime.NativeBridge.gcb(target);
                ++kicked;
            } catch (Throwable ignored) {
                // One stuck class must not block the rest.
            }
        }
        vegas.sony.runtime.CheatBreakerDebugLog.info("HOOK_RETRANSFORM classes=" + kicked + " //$");
    }

    private static String targetName(MappingTask mappingTask) {
        Class targetClass = mappingTask.getTargetClass();
        return targetClass == null ? "<null>" : targetClass.getName();
    }

    private static Throwable a(Throwable throwable) {
        return throwable;
    }

    public void C() {
        for (MappingTask mappingTask : this.D) {
            mappingTask.rollback();
        }
    }
}

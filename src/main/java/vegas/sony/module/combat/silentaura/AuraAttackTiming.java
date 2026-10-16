package vegas.sony.module.combat.silentaura;

/** One monotonic deadline per attack, independent of how often readiness is polled. */
public final class AuraAttackTiming {
    private boolean scheduled;
    private long nextAttackAtNanos;

    public synchronized boolean isReady(long nowNanos) {
        return !this.scheduled || nowNanos >= this.nextAttackAtNanos;
    }

    public synchronized void recordAttack(long nowNanos, double cps) {
        long interval = Math.round(1.0E9 / clampCps(cps));
        // Preserve the fractional tick remainder. Resetting from each actual
        // packet rounds every interval above 50 ms up to 100 ms (10 CPS).
        // A long interruption instead starts a fresh cycle, without catch-up.
        if (this.scheduled && nowNanos >= this.nextAttackAtNanos
                && nowNanos - this.nextAttackAtNanos <= interval * 2L) {
            this.nextAttackAtNanos += interval;
        } else {
            this.nextAttackAtNanos = nowNanos + interval;
        }
        this.scheduled = true;
    }

    public synchronized void reset() {
        this.scheduled = false;
        this.nextAttackAtNanos = 0L;
    }

    public static long clickIntervalMillis(double cps) {
        return Math.round(1000.0 / clampCps(cps));
    }

    private static double clampCps(double cps) {
        return Double.isNaN(cps) || Double.isInfinite(cps)
                ? 1.0 : Math.max(1.0, Math.min(20.0, cps));
    }
}

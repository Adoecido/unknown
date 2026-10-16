package vegas.sony.module.combat;

import vegas.sony.module.combat.silentaura.AuraAttackTiming;

/** Regression checks for Aura CPS scheduling and Legit extra-swing boundaries. */
public final class AuraAttackTimingProbe {
    public static void main(String[] args) {
        for (int cps : new int[]{10, 12, 15, 18, 19, 20}) {
            checkRate(cps, false);
            checkRate(cps, true);
        }

        AuraAttackTiming timing = new AuraAttackTiming();
        require(timing.isReady(0L), "First attack was delayed");
        timing.recordAttack(0L, 20.0);
        for (int i = 0; i < 10000; i++) {
            require(!timing.isReady(49_000_000L), "Polling changed the attack deadline");
        }
        require(timing.isReady(50_000_000L), "20 CPS was not ready at 50 ms");
        timing.recordAttack(5_000_000_000L, 20.0);
        require(!timing.isReady(5_000_000_000L), "Idle time accumulated a catch-up burst");
        timing.reset();
        require(timing.isReady(5_000_000_000L), "Reset retained an old deadline");
        require(AuraAttackTiming.clickIntervalMillis(20.0) == 50L, "Legit click interval is not 20 CPS");
        require(AuraAttackTiming.clickIntervalMillis(15.0) == 67L, "Legit click interval is not 15 CPS");

        require(SilentAura.isWithinConfiguredRange(2.88, 3.0), "Attack range rejected 2.88 blocks");
        require(SilentAura.isWithinConfiguredRange(3.0, 3.0), "Attack range is capped below 3.0 blocks");
        require(!SilentAura.isWithinConfiguredRange(3.0001, 3.0), "Attack range exceeded 3.0 blocks");
        double swingRange = SilentAura.getSwingRange(1.0);
        require(swingRange == 4.0 && SilentAura.isWithinConfiguredRange(4.0, swingRange),
                "Legit Extra Swing did not extend swing range");
        require(!SilentAura.isWithinConfiguredRange(4.0001, swingRange), "Extra Swing exceeded its setting");
        require(!SilentAura.isWithinConfiguredRange(Double.NaN, swingRange), "Invalid distance passed");
        System.out.println("PASS: Aura honors 10-20 CPS on game ticks, polling is stable, swings extend range but attacks stop at 3.0");
    }

    private static void checkRate(int cps, boolean jitter) {
        AuraAttackTiming timing = new AuraAttackTiming();
        int sent = 0;
        for (int tick = 0; tick < 200; tick++) {
            long now = tick * 50_000_000L;
            if (jitter && tick > 0) now += tick % 2 == 0 ? 1_000_000L : -1_000_000L;
            if (timing.isReady(now)) {
                sent++;
                timing.recordAttack(now, cps);
            }
        }
        require(Math.abs(sent - cps * 10) <= 1,
                "Configured " + cps + " CPS produced " + sent + " attacks in 10 s, jitter=" + jitter);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

package vegas.sony.module.combat;

import java.util.ArrayList;
import java.util.Arrays;

/** Regression checks for the post-hit Switch delay target lock. */
public final class AuraSwitchDelayProbe {
    private AuraSwitchDelayProbe() {
    }

    public static void main(String[] args) {
        ArrayList<String> reorderedCandidates = new ArrayList<String>(Arrays.asList("other", "original"));
        ArrayList<String> outOfRangeCandidates = new ArrayList<String>(Arrays.asList("other"));
        ArrayList<String> noCandidates = new ArrayList<String>();
        long hitAt = 1000L;
        long switchAt = hitAt + 200L;

        require(SilentAura.isSwitchDelayHolding(true, 7, 7, switchAt, hitAt + 199L),
                "Switch released the target before the configured delay");
        require(SilentAura.chooseDuringSwitchDelay(reorderedCandidates, "original", true).equals("original"),
                "Candidate sorting changed the target during Switch delay");
        require(SilentAura.chooseDuringSwitchDelay(outOfRangeCandidates, "original", true).equals("original"),
                "Leaving acquisition range changed the target during Switch delay");
        require(SilentAura.chooseDuringSwitchDelay(noCandidates, "original", true).equals("original"),
                "An empty candidate list dropped the locked target");

        require(!SilentAura.isSwitchDelayHolding(true, 7, 7, switchAt, switchAt),
                "Switch did not release the target at the deadline");
        require(SilentAura.chooseDuringSwitchDelay(reorderedCandidates, "original", false).equals("other"),
                "Switch did not select the next candidate after the deadline");
        require(SilentAura.chooseDuringSwitchDelay(noCandidates, "original", false) == null,
                "Switch kept a target without candidates after the deadline");
        require(!SilentAura.isSwitchDelayHolding(false, 7, 7, switchAt, hitAt + 1L),
                "Switch off still held a target");
        require(!SilentAura.isSwitchDelayHolding(true, 7, 8, switchAt, hitAt + 1L),
                "Switch held the wrong target ID");
        require(!SilentAura.isSwitchDelayHolding(true, Integer.MIN_VALUE, 7, switchAt, hitAt + 1L),
                "Switch held a target without an armed hit");

        System.out.println("PASS: Aura Switch delay locks the hit target until the deadline, even out of range");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

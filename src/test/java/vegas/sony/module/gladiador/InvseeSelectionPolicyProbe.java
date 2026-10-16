package vegas.sony.module.gladiador;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import vegas.sony.value.InvseeTargetCandidatesValue;
import vegas.sony.value.InvseeTargetCandidatesValue.RankedPlayer;

public final class InvseeSelectionPolicyProbe {
    public static void main(String[] args) {
        InvseeSelectionPolicy policy = new InvseeSelectionPolicy();
        List<RankedPlayer> first = Arrays.asList(new RankedPlayer("One", 1), new RankedPlayer("Two", 2),
                new RankedPlayer("Three", 3), new RankedPlayer("Four", 4));
        List<RankedPlayer> next = Arrays.asList(new RankedPlayer("Four", 0), new RankedPlayer("One", 1),
                new RankedPlayer("Two", 2), new RankedPlayer("Three", 3), new RankedPlayer("Five", 5));
        require("Four", policy.select(first, true, 4, false, "", Collections.<String>emptyList()));
        require("Three", policy.select(next, true, 4, false, "", Collections.<String>emptyList()));
        policy.reset();
        require("Four", policy.select(first, true, 4, true, "", Collections.<String>emptyList()));
        require("Four", policy.select(next, true, 4, true, "", Collections.<String>emptyList()));
        require("Four", policy.select(Collections.<RankedPlayer>emptyList(), true, 4, true, "", Collections.<String>emptyList()));
        require("Five", policy.select(next, true, 4, true, "", Arrays.asList("four")));
        require("One", policy.select(next, true, 2, true, "", Collections.<String>emptyList()));
        require("Fixed", policy.select(next, false, 2, true, "Fixed", Collections.<String>emptyList()));
        require("", policy.select(next, false, 2, true, "Fixed", Arrays.asList("fixed")));
        policy.reset();
        require("", policy.select(first, true, 10, true, "", Collections.<String>emptyList()));
        InvseeTargetCandidatesValue value = InvseeTargetCandidatesValue.create(null);
        value.setPersistenceSuppressed(true);
        value.parse("Fixed");
        value.updatePlayers(first, true);
        value.setWaitingState("Report expired");
        require("Fixed", value.getValue());
        System.out.println("Invsee policy: fixed/rank, until-death, missing, death and config changes passed");
    }
    private static void require(String expected, String actual) {
        if (!expected.equals(actual)) throw new AssertionError(expected + " != " + actual);
    }
}

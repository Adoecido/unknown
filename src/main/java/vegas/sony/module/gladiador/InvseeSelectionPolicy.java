package vegas.sony.module.gladiador;

import java.util.Collection;
import java.util.List;
import vegas.sony.value.InvseeTargetCandidatesValue.RankedPlayer;

/** Pure selection policy. Missing/offline/out-of-range is NOT a confirmed death. */
public final class InvseeSelectionPolicy {
    private String locked = "";
    private int previousPosition = -1;
    private boolean previousAutomatic, previousUntilDeath;

    public void reset() { locked = ""; previousPosition = -1; }

    public String select(List<RankedPlayer> players, boolean automatic, int position,
                         boolean untilDeath, String manual, Collection<String> dead) {
        if (previousPosition != position || previousAutomatic != automatic || previousUntilDeath != untilDeath) {
            locked = "";
        }
        previousPosition = position;
        previousAutomatic = automatic;
        previousUntilDeath = untilDeath;
        if (!automatic) return contains(dead, manual) ? "" : manual;
        if (untilDeath && !locked.isEmpty() && !contains(dead, locked)) return locked;
        locked = "";
        int index = 0;
        for (RankedPlayer player : players) {
            if (contains(dead, player.getName())) continue;
            if (++index == position) { locked = player.getName(); break; }
        }
        return locked;
    }

    private static boolean contains(Collection<String> names, String name) {
        if (name == null) return false;
        for (String other : names) if (name.equalsIgnoreCase(other)) return true;
        return false;
    }
}

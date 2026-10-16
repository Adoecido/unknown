package vegas.sony.value;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The ordered Invsee roster shown by the native Target menu. The selected
 * nickname remains the value used by Target, while the complete ranking is
 * transient UI data supplied by the authenticated ticket bridge.
 */
public final class InvseeTargetCandidatesValue
        extends Value<String, InvseeTargetCandidatesValue> {
    private static final String ID = "Invsee Ranking //$ui";
    private static final int MAX_PLAYERS = 100;
    private volatile List<RankedPlayer> players = Collections.emptyList();
    private volatile List<String> candidates = Collections.emptyList();
    private volatile int minimumSets = -1;
    private volatile String state = "Waiting for Invsee report //$ui";

    public static InvseeTargetCandidatesValue create(Object owner) {
        return new InvseeTargetCandidatesValue(owner);
    }

    private InvseeTargetCandidatesValue(Object owner) {
        super(owner, ID, "");
        this.setResettable(false);
    }

    @Override
    public String getName() {
        int count = this.players.size();
        if (count == 0) return this.state;
        return "Invsee ranking — " + count + (count == 1 ? " player //$ui" : " players //$ui");
    }

    public List<RankedPlayer> getPlayers() {
        return this.players;
    }

    public List<String> getCandidates() {
        return this.candidates;
    }

    public int getMinimumSets() {
        return this.minimumSets;
    }

    public synchronized void updatePlayers(Collection<RankedPlayer> nextPlayers) {
        this.updatePlayers(nextPlayers, false);
    }

    public synchronized void updatePlayers(Collection<RankedPlayer> nextPlayers, boolean keepSelection) {
        Set<String> seen = new LinkedHashSet<String>();
        ArrayList<RankedPlayer> cleaned = new ArrayList<RankedPlayer>();
        if (nextPlayers != null) {
            for (RankedPlayer raw : nextPlayers) {
                if (raw == null || raw.name == null) continue;
                String name = raw.name.trim();
                if (!name.matches("[A-Za-z0-9_]{1,16}")
                        || raw.sets < 0
                        || !seen.add(name.toLowerCase(Locale.ROOT))) continue;
                cleaned.add(new RankedPlayer(name, raw.sets, raw.helmet,
                        raw.chestplate, raw.leggings, raw.boots));
            }
        }
        // Collections.sort is stable, so equal set counts keep the roster
        // order published by the bot while still receiving individual ranks.
        Collections.sort(cleaned, new Comparator<RankedPlayer>() {
            @Override
            public int compare(RankedPlayer left, RankedPlayer right) {
                return Integer.compare(left.sets, right.sets);
            }
        });
        if (cleaned.size() > MAX_PLAYERS) {
            cleaned = new ArrayList<RankedPlayer>(cleaned.subList(0, MAX_PLAYERS));
        }

        ArrayList<String> names = new ArrayList<String>(cleaned.size());
        for (RankedPlayer player : cleaned) names.add(player.name);
        this.players = Collections.unmodifiableList(cleaned);
        this.candidates = Collections.unmodifiableList(names);
        this.minimumSets = cleaned.isEmpty() ? -1 : cleaned.get(0).sets;
        this.state = cleaned.isEmpty() ? "Waiting for Invsee report //$ui"
                : "Invsee ranking //$ui";

        // A fresh report must never silently choose a player in Manual mode.
        // Automatic mode explicitly asks for the lowest player through
        // selectLowestPlayer(), while a manual report starts unselected.
        String selected = this.getValue();
        if (!keepSelection && !containsIgnoreCase(names, selected)) this.setTransientSelection("");
    }

    public synchronized void setActivePlayer(String name) {
        this.setTransientSelection(name == null ? "" : name);
    }

    public synchronized void setWaitingState(String state) {
        this.updatePlayers(Collections.<RankedPlayer>emptyList(), true);
        this.state = state;
    }

    /** Backwards-compatible helper used by older reports and probes. */
    public synchronized void updateCandidates(Collection<String> nextCandidates,
                                               int nextMinimumSets) {
        ArrayList<RankedPlayer> ranked = new ArrayList<RankedPlayer>();
        if (nextCandidates != null) {
            for (String name : nextCandidates) {
                ranked.add(new RankedPlayer(name, Math.max(0, nextMinimumSets)));
            }
        }
        this.updatePlayers(ranked);
    }

    public synchronized boolean selectPlayer(String name) {
        if (name == null) return false;
        String candidate = name.trim();
        if (!containsIgnoreCase(this.candidates, candidate)) return false;
        if (candidate.equalsIgnoreCase(this.getValue())) return true;
        this.setValue(canonicalName(this.candidates, candidate));
        return true;
    }

    /** Selects one of the lowest-set players for Invsee Automatic mode. */
    public synchronized boolean selectLowestPlayer() {
        String current = this.getValue();
        if (!current.isEmpty() && !this.players.isEmpty()) {
            int lowestSets = this.players.get(0).sets;
            for (RankedPlayer player : this.players) {
                if (player.name.equalsIgnoreCase(current))
                    return player.sets != lowestSets && this.selectLowestPlayerInternal();
            }
        }
        return this.selectLowestPlayerInternal();
    }

    private boolean selectLowestPlayerInternal() {
        String selected = this.randomLowestPlayer(this.players);
        if (selected.isEmpty()) {
            this.setTransientSelection("");
            return false;
        }
        if (selected.equalsIgnoreCase(this.getValue())) return false;
        this.setTransientSelection(selected);
        return true;
    }

    /** Clears the manual selection without persisting it as a config choice. */
    public synchronized void clearSelection() {
        this.setTransientSelection("");
    }

    /** Accepts the legacy one-hot selector used by older native menus. */
    public synchronized boolean selectFromFlags(String flags) {
        List<String> available = this.candidates;
        if (flags == null || flags.length() != available.size()
                || !flags.matches("[01]*")) return false;
        String selected = this.getValue();
        for (int index = 0; index < available.size(); ++index) {
            if (flags.charAt(index) != '1') continue;
            String candidate = available.get(index);
            if (!candidate.equalsIgnoreCase(selected)) {
                this.setValue(candidate);
                return true;
            }
        }
        return false;
    }

    /** Keeps the panel in sync when the chosen player is not loaded locally. */
    public synchronized boolean selectAvailableCandidate(String name) {
        if (!containsIgnoreCase(this.candidates, name)) return false;
        this.setTransientSelection(canonicalName(this.candidates, name));
        return true;
    }

    public synchronized void clearCandidates() {
        this.updatePlayers(Collections.<RankedPlayer>emptyList());
    }

    @Override
    public void parse(String serializedValue) {
        // Profiles load before the first report. Preserve a fixed player's
        // name, but never acquire an entity until a valid report arrives.
        if (serializedValue != null && (serializedValue.isEmpty()
                || serializedValue.matches("[A-Za-z0-9_]{1,16}"))) this.setValue(serializedValue);
    }

    @Override
    public InvseeTargetCandidatesValue copyValueDefinition() {
        InvseeTargetCandidatesValue copy = new InvseeTargetCandidatesValue(null);
        copy.updatePlayers(this.players);
        copy.setValue(this.getValue());
        return copy;
    }

    private String randomLowestPlayer(List<RankedPlayer> ranked) {
        if (ranked.isEmpty()) return "";
        int lowest = ranked.get(0).sets;
        int tied = 1;
        while (tied < ranked.size() && ranked.get(tied).sets == lowest) ++tied;
        return ranked.get(ThreadLocalRandom.current().nextInt(tied)).name;
    }

    private void setTransientSelection(String selection) {
        if (selection.equals(this.getValue())) return;
        boolean wasSuppressed = this.isPersistenceSuppressed();
        this.setPersistenceSuppressed(true);
        try {
            this.setValue(selection);
        } finally {
            this.setPersistenceSuppressed(wasSuppressed);
        }
    }

    private static String canonicalName(Collection<String> names, String candidate) {
        if (candidate == null) return "";
        for (String name : names) {
            if (name.equalsIgnoreCase(candidate)) return name;
        }
        return "";
    }

    private static boolean containsIgnoreCase(Collection<String> names, String candidate) {
        return !canonicalName(names, candidate).isEmpty();
    }

    public static final class RankedPlayer {
        private final String name;
        private final int sets;
        private final int helmet;
        private final int chestplate;
        private final int leggings;
        private final int boots;

        public RankedPlayer(String name, int sets) {
            this(name, sets, 0, 0, 0, 0);
        }

        public RankedPlayer(String name, int sets, int helmet, int chestplate,
                            int leggings, int boots) {
            this.name = name;
            this.sets = sets;
            this.helmet = Math.max(0, helmet);
            this.chestplate = Math.max(0, chestplate);
            this.leggings = Math.max(0, leggings);
            this.boots = Math.max(0, boots);
        }

        public String getName() {
            return this.name;
        }

        public int getSets() {
            return this.sets;
        }

        public int getHelmet() { return this.helmet; }
        public int getChestplate() { return this.chestplate; }
        public int getLeggings() { return this.leggings; }
        public int getBoots() { return this.boots; }
    }
}

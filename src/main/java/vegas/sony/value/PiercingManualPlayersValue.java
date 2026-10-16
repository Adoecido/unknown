package vegas.sony.value;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Live clan-member choices for Piercing's manual mode. The persisted value is
 * the set of players that should be hidden; the candidate list is transient
 * and follows the players currently detected in the world.
 */
public final class PiercingManualPlayersValue
        extends Value<String, PiercingManualPlayersValue> {
    private static final String ID = "ManualShow //$ui";
    private volatile List<String> candidates = Collections.emptyList();

    public static PiercingManualPlayersValue create(Object owner) {
        return new PiercingManualPlayersValue(owner);
    }

    private PiercingManualPlayersValue(Object owner) {
        super(owner, ID, "");
        this.setDescription("Select the clan members that should disappear. //$ui");
    }

    @Override
    public String getName() {
        return "Clan Members //$ui";
    }

    public List<String> getCandidates() {
        return this.candidates;
    }

    public synchronized void updateCandidates(Collection<String> nextCandidates) {
        Map<String, String> unique = new LinkedHashMap<String, String>();
        if (nextCandidates != null) {
            for (String raw : nextCandidates) {
                String name = cleanName(raw);
                if (name.isEmpty()) continue;
                unique.put(name.toLowerCase(Locale.ROOT), name);
            }
        }
        this.candidates = Collections.unmodifiableList(
                new ArrayList<String>(unique.values()));
    }

    public boolean isSelected(String playerName) {
        return selectedNames().contains(cleanName(playerName).toLowerCase(Locale.ROOT));
    }

    public synchronized boolean selectFromFlags(String flags) {
        List<String> available = this.candidates;
        if (flags == null || flags.length() != available.size()
                || !flags.matches("[01]*")) return false;
        ArrayList<String> selected = new ArrayList<String>();
        for (int index = 0; index < available.size(); ++index) {
            if (flags.charAt(index) == '1') selected.add(available.get(index));
        }
        String serialized = join(selected);
        if (!serialized.equals(this.getValue())) this.setValue(serialized);
        return true;
    }

    @Override
    public void parse(String serializedValue) {
        LinkedHashMap<String, String> unique = new LinkedHashMap<String, String>();
        if (serializedValue != null) {
            for (String raw : serializedValue.split("(?:\\r?\\n|,)")) {
                String name = cleanName(raw);
                if (!name.isEmpty()) unique.put(name.toLowerCase(Locale.ROOT), name);
            }
        }
        this.setValue(join(unique.values()));
    }

    @Override
    public PiercingManualPlayersValue copyValueDefinition() {
        PiercingManualPlayersValue copy = new PiercingManualPlayersValue(null);
        copy.updateCandidates(this.candidates);
        copy.setValue(this.getValue());
        return copy;
    }

    private Set<String> selectedNames() {
        LinkedHashSet<String> selected = new LinkedHashSet<String>();
        String value = this.getValue();
        if (value == null || value.isEmpty()) return selected;
        for (String raw : value.split("\\r?\\n")) {
            String clean = cleanName(raw);
            if (!clean.isEmpty()) selected.add(clean.toLowerCase(Locale.ROOT));
        }
        return selected;
    }

    private static String join(Collection<String> names) {
        StringBuilder joined = new StringBuilder();
        for (String name : names) {
            if (joined.length() > 0) joined.append('\n');
            joined.append(name);
        }
        return joined.toString();
    }

    private static String cleanName(String raw) {
        if (raw == null) return "";
        String clean = raw.replaceAll("§.", "").trim();
        return clean.matches("[A-Za-z0-9_]{1,16}") ? clean : "";
    }
}

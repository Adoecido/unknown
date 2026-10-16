package vegas.sony.value;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Persistent, ordered player names used by the Friends module. */
public final class FriendsValue extends Value<List<String>, FriendsValue> {
    private static final String ID = "Friends //$ui";
    private static final int MAX_FRIENDS = 128;

    public static FriendsValue create(Object owner) {
        return new FriendsValue(owner);
    }

    private FriendsValue(Object owner) {
        super(owner, ID, Collections.<String>emptyList());
    }

    public synchronized List<String> getFriends() {
        return this.getValue();
    }

    public synchronized boolean addFriend(String rawName) {
        String name = normalize(rawName);
        if (name.isEmpty() || containsIgnoreCase(this.getValue(), name)
                || this.getValue().size() >= MAX_FRIENDS) return false;
        ArrayList<String> next = new ArrayList<String>(this.getValue());
        next.add(name);
        this.setValue(Collections.unmodifiableList(next));
        return true;
    }

    public synchronized boolean containsFriend(String rawName) {
        String name = normalize(rawName);
        return !name.isEmpty() && containsIgnoreCase(this.getValue(), name);
    }

    public synchronized boolean removeFriend(String rawName) {
        String name = normalize(rawName);
        ArrayList<String> next = new ArrayList<String>(this.getValue());
        boolean removed = false;
        for (int index = next.size() - 1; index >= 0; --index) {
            if (!next.get(index).equalsIgnoreCase(name)) continue;
            next.remove(index);
            removed = true;
        }
        if (removed) this.setValue(Collections.unmodifiableList(next));
        return removed;
    }

    /** Replaces the complete list from the newline-delimited native GUI value. */
    public synchronized boolean replaceSerialized(String serialized) {
        String raw = serialized == null ? "" : serialized;
        String[] entries = raw.split("(?:\\r?\\n|,)", -1);
        LinkedHashSet<String> names = new LinkedHashSet<String>();
        Set<String> lower = new LinkedHashSet<String>();
        for (String entry : entries) {
            if (entry == null || entry.trim().isEmpty()) continue;
            String name = normalize(entry);
            if (name.isEmpty() || !lower.add(name.toLowerCase(Locale.ROOT))) return false;
            if (names.size() >= MAX_FRIENDS) return false;
            names.add(name);
        }
        List<String> next = Collections.unmodifiableList(new ArrayList<String>(names));
        if (next.equals(this.getValue())) return true;
        this.setValue(next);
        return true;
    }

    @Override
    public void parse(String serializedValue) {
        this.replaceSerialized(serializedValue);
    }

    @Override
    public JsonObject toJson(boolean includeValue) {
        JsonObject json = super.toJson(includeValue);
        if (includeValue) json.addProperty("value", serialize(this.getValue()));
        return json;
    }

    @Override
    public FriendsValue copyValueDefinition() {
        FriendsValue copy = new FriendsValue(null);
        copy.setValue(Collections.unmodifiableList(new ArrayList<String>(this.getValue())));
        return copy;
    }

    @Override
    public String getDisplayValue() {
        int count = this.getValue().size();
        return count == 0 ? "No friends //$ui"
                : count + (count == 1 ? " friend //$ui" : " friends //$ui");
    }

    public static String serialize(Collection<String> names) {
        StringBuilder result = new StringBuilder();
        if (names != null) {
            for (String name : names) {
                if (name == null || name.isEmpty()) continue;
                if (result.length() > 0) result.append('\n');
                result.append(name);
            }
        }
        return result.toString();
    }

    private static String normalize(String rawName) {
        if (rawName == null) return "";
        String name = rawName.replaceAll("§.", "").trim();
        return name.matches("[A-Za-z0-9_]{1,16}") ? name : "";
    }

    private static boolean containsIgnoreCase(Collection<String> names, String candidate) {
        for (String name : names) {
            if (name.equalsIgnoreCase(candidate)) return true;
        }
        return false;
    }
}

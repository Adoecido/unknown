package vegas.sony.value;

import com.google.common.collect.ImmutableList;
import vegas.sony.value.ListValueMutableBackingList;
import vegas.sony.value.ListValueSuggestionProvider;
import vegas.sony.value.Value;
import vegas.sony.utils.EncString;
import vegas.sony.utils.Str;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public abstract class ListValue<C, T extends ListValue<C, T>>
extends Value<List<C>, T> {
    @Nullable
    private ListValueSuggestionProvider suggestionProvider;
    private final EncString displayName;
    private boolean playerNameEntries;


    public ListValue(Object owner, String id, String displayName) {
        this(owner, id, displayName, new ArrayList<C>());
    }

    /**
     * Marks the entries as player names so the editor draws each one with that player's head.
     */
    public T setPlayerNameEntries(boolean playerNameEntries) {
        this.playerNameEntries = playerNameEntries;
        return (T)this;
    }

    public boolean hasPlayerNameEntries() {
        return this.playerNameEntries;
    }

    public T setSuggestionProvider(ListValueSuggestionProvider suggestionProvider) {
        this.suggestionProvider = suggestionProvider;
        return (T)this;
    }

    public abstract C createEntry(String value, int legacyIndex);

    public List<C> snapshotEntries() {
        return new ArrayList<C>(this.getValue());
    }

    @Override
    public String getDisplayValue() {
        List<C> list = this.getValue();
        if (list.isEmpty()) {
            return "None";
        }
        if (list.size() == 1) {
            return list.get(0).toString();
        }
        return list.get(0).toString() + " +" + (list.size() - 1);
    }

    @Override
    public List<C> copyValue() {
        return this.snapshotEntries();
    }

    @Nullable
    public ListValueSuggestionProvider getSuggestionProvider() {
        return this.suggestionProvider;
    }

    public ListValue(Object owner, String id, String displayName, List<C> entries) {
        super(owner, id, ImmutableList.copyOf(entries));
        this.setDirectValue(new ListValueMutableBackingList<C>(this, entries));
        this.displayName = Str.enc(displayName);
    }

    @Override
    public String getName() {
        return this.displayName.get();
    }
}

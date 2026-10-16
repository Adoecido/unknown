package vegas.sony.unmap;

import vegas.sony.unmap.ItemMatchRuleConstructorMarker;
import java.util.function.Predicate;
import org.jetbrains.annotations.Nullable;

class ItemMatchRule<T> {
    @Nullable
    private Class[] acceptedClasses;
    private Predicate<T> predicate;
    private final String[] aliases;

    private ItemMatchRule(String ... aliases) {
        this.aliases = aliases;
    }

    public Predicate<T> getPredicate() {
        return this.predicate;
    }

    public String[] getAliases() {
        return this.aliases;
    }

    ItemMatchRule(String[] aliases, ItemMatchRuleConstructorMarker constructorMarker) {
        this(aliases);
    }

    @Nullable
    public Class[] getAcceptedClasses() {
        return this.acceptedClasses;
    }

    public void setPredicate(Predicate<T> predicate) {
        this.predicate = predicate;
    }

    public void setAcceptedClasses(Class[] acceptedClasses) {
        this.acceptedClasses = acceptedClasses;
    }
}

package vegas.sony.value;

import vegas.sony.SonyVegas;
import vegas.sony.value.AbstractListValueSuggestionProvider;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.UnmodifiableView;

public class FriendNameSuggestionProvider
extends AbstractListValueSuggestionProvider {
    @Override
    public @UnmodifiableView List<String> getValues() {
        return new ArrayList<String>(SonyVegas.INSTANCE.getPublicProfileManager().getTags());
    }
}

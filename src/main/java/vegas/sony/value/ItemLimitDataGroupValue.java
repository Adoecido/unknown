package vegas.sony.value;

import vegas.sony.value.ItemLimitDataGroup;
import vegas.sony.value.Value;
import java.util.ArrayList;
import java.util.List;

public class ItemLimitDataGroupValue
extends Value<List<ItemLimitDataGroup>, ItemLimitDataGroupValue> {
    public List<ItemLimitDataGroup> getGroupsCompat() {
        return null;
    }

    public ItemLimitDataGroupValue(Object owner, String name) {
        super(owner, name, new ArrayList());
    }

    public void setGroupsCompat(List<ItemLimitDataGroup> groups) {
    }

    @Override
    public void parse(String serializedValue) {
    }

    public ItemLimitDataGroupValue copyDefinition() {
        return new ItemLimitDataGroupValue(null, this.getId());
    }

    @Override
    public ItemLimitDataGroupValue copyValueDefinition() {
        return this.copyDefinition();
    }
}

package vegas.sony.value;

import vegas.sony.value.Value;

public interface ValueChangeListener<V extends Value<?, V>> {
    public void onValueChanged(V value);
}

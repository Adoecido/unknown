package vegas.sony.value;

import com.google.gson.JsonObject;
import vegas.sony.SonyVegas;
import vegas.sony.config.ConfigJsonUtils;
import vegas.sony.config.PublicProfileSettings;
import vegas.sony.module.Mod;
import vegas.sony.module.SubModule;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.ui.click.component.value.ValueComponentFactory;
import vegas.sony.unmap.INamed;
import vegas.sony.unmap.PropertyContainer;
import vegas.sony.utils.Base64Util;
import vegas.sony.utils.EncString;
import vegas.sony.utils.Str;
import vegas.sony.value.ColorValue;
import vegas.sony.value.ConditionalValue;
import vegas.sony.value.DirectValueAccessor;
import vegas.sony.value.EntityTargetFilterValue;
import vegas.sony.value.ListValue;
import vegas.sony.value.ValueAccessor;
import vegas.sony.value.ValueChangeListener;
import vegas.sony.value.ValueChangeValidator;
import vegas.sony.value.ValueCondition;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

public abstract class Value<K, T extends Value<K, T>>
extends PropertyContainer
implements INamed {
    @Nullable
    private EncString description;
    private ValueAccessor<K, T> accessor;
    private final Object owner;
    private final List<ValueChangeValidator<T, K>> validators;
    private boolean suppressPersistence;
    protected K defaultValue;
    private boolean encodeBase64 = false;
    private final List<String> aliases;
    private GuiComponent boundComponent;
    private boolean hidden = false;
    private boolean serializable = true;
    private boolean resettable = true;
    private Value parent;
    private final DirectValueAccessor<K, T> directAccessor;
    private final HashMap<ConditionalValue, ValueCondition> conditions = new HashMap();
    private final EncString name;
    private static String legacyState;
    private final List<ValueChangeListener<T>> changeListeners = new ArrayList<ValueChangeListener<T>>();
    @Nullable
    private Color overrideColor = null;
    private K currentValue;

    protected JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", this.getId());
        return jsonObject;
    }

    public Object getOwner() {
        return this.owner;
    }

    public boolean matchesJsonId(JsonObject jsonObject) {
        String string = ConfigJsonUtils.getString(jsonObject, "id");
        if (string == null) return false;
        for (String string2 : this.aliases) {
            if (!Str.visibleEqualsIgnoreCase(string2, string)) continue;
            return true;
        }
        if (this.getId().equalsIgnoreCase(string)) return true;
        return Str.visibleEqualsIgnoreCase(this.segmentedLegacyId(), string);
    }

    public K getValue() {
        return this.accessor.getValue();
    }


    public void notifyChangeListeners() {
        for (ValueChangeListener<T> valueChangeListener : this.changeListeners) {
            valueChangeListener.onValueChanged((T)this);
        }
    }

    @Override
    public String getName() {
        return this.name.get();
    }

    public K getDefaultValue() {
        return this.defaultValue;
    }

    public void addValidator(ValueChangeValidator<T, K> validator) {
        this.validators.add(validator);
    }

    public void useDirectAccessor() {
        this.accessor = this.directAccessor;
    }

    public boolean isResettable() {
        return this.resettable;
    }

    public void setPersistenceSuppressed(boolean suppressed) {
        this.suppressPersistence = suppressed;
    }

    public abstract void parse(String var1);

    public boolean isDefault() {
        K k = this.getValue();
        K k2 = this.getDefaultValue();
        if (k2 == null && k == null) {
            return true;
        }
        if (k2 != null && k == null) {
            return false;
        }
        if (k2 == null) {
            return false;
        }
        if (this instanceof ListValue) {
            List list = (List)k2;
            List list2 = (List)k;
            if (list.size() != list2.size()) {
                return false;
            }
            for (int i = 0; i < list.size(); ++i) {
                Object defaultEntryText = ValueComponentFactory.getComparableListEntryText(list.get(i));
                Object currentEntryText = ValueComponentFactory.getComparableListEntryText(list2.get(i));
                if (defaultEntryText == null && currentEntryText == null || defaultEntryText != null && defaultEntryText.equals(currentEntryText)) continue;
                return false;
            }
            return true;
        }
        if (k2 instanceof double[]) {
            double[] dArray = (double[])k;
            double[] dArray2 = (double[])k2;
            if (dArray.length != dArray2.length) {
                return false;
            }
            boolean bl = true;
            for (int i = 0; i < dArray.length; ++i) {
                double d = dArray[i];
                double d2 = dArray2[i];
                if (d == d2) continue;
                bl = false;
                break;
            }
            return bl;
        }
        return k2.equals(k);
    }

    public JsonObject toJson(boolean bl) {
        JsonObject jsonObject = this.toJson();
        jsonObject.addProperty("id", this.getId());
        if (this.getValue() != null) {
            String string = "value";
            String string2 = this.getValue().toString();
            if (this.encodeBase64) {
                string2 = "b64:" + Base64Util.encodeUtf8Base64(string2);
            }
            jsonObject.addProperty(string, string2);
        }
        return jsonObject;
    }

    public void addCondition(ValueCondition valueCondition) {
        for (ConditionalValue conditionalValue : valueCondition.getPredicatesByValue().keySet()) {
            this.conditions.put(conditionalValue, valueCondition);
            this.propagateConditionToDependents(conditionalValue, valueCondition);
        }
    }

    public void addChangeListener(ValueChangeListener<T> valueChangeListener) {
        this.changeListeners.add(valueChangeListener);
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public Value(Object owner, String name, K defaultValue) {
        this.validators = new ArrayList<ValueChangeValidator<T, K>>();
        this.aliases = new ArrayList<String>();
        this.directAccessor = new DirectValueAccessor<K, T>(this);
        this.accessor = this.directAccessor;
        this.owner = owner;
        this.name = Str.enc(name);
        // Do not dispatch to subclass setters while the subclass is still
        // uninitialized. NumberValue overrides A(), so virtual dispatch here
        // left its default value null and also ran change side effects.
        this.defaultValue = defaultValue;
        this.currentValue = defaultValue;
        if (owner != null) {
            if (owner instanceof ColorValue || owner instanceof EntityTargetFilterValue) {
                return;
            }
            if (owner instanceof PublicProfileSettings) {
                SonyVegas.INSTANCE.getSettingsManager().registerValue(this);
            } else if (!(owner instanceof Mod)) {
                SonyVegas.INSTANCE.getValueManager().registerValue(this);
            }
        }
    }

    public void setAccessor(ValueAccessor<K, T> valueAccessor) {
        this.accessor = valueAccessor;
    }

    @Nullable
    public Color getDisplayColor() {
        return null;
    }

    public void addConditionalValue(ConditionalValue conditionalValue) {
        if (this.conditions.containsKey(conditionalValue)) {
            return;
        }
        this.conditions.put(conditionalValue, null);
        this.propagateConditionToDependents(conditionalValue, null);
    }

    /**
     * Conditions on a mode/boolean parent must also gate its nested values.
     * A mode can depend on another mode (for example a style selector that
     * owns a color selector); without propagating the condition, the nested
     * controls stay active even while their whole parent branch is hidden.
     */
    private void propagateConditionToDependents(ConditionalValue conditionalValue,
                                                 ValueCondition valueCondition) {
        if (!(this instanceof ConditionalValue)) {
            return;
        }
        for (Object dependent : ((ConditionalValue)this).getDependentValues()) {
            if (dependent instanceof Value) {
                ((Value)dependent).inheritCondition(conditionalValue, valueCondition);
            }
        }
    }

    private void inheritCondition(ConditionalValue conditionalValue,
                                  ValueCondition valueCondition) {
        if (this.conditions.containsKey(conditionalValue)) {
            // addDependentValues() first installs an unconditional parent
            // edge and ValueCondition.applyTo() replaces that edge with its
            // predicate immediately afterwards.  Nested children used to
            // keep the first null edge forever, which made them ask the outer
            // boolean whether they were direct children and hid the entire
            // branch.  Promote the inherited edge to the real predicate and
            // pass the correction through every descendant.
            if (this.conditions.get(conditionalValue) == null
                    && valueCondition != null) {
                this.conditions.put(conditionalValue, valueCondition);
                this.propagateConditionToDependents(conditionalValue,
                        valueCondition);
            }
            return;
        }
        this.conditions.put(conditionalValue, valueCondition);
        this.propagateConditionToDependents(conditionalValue, valueCondition);
    }

    /** Copies the complete ancestor-condition set to a newly attached child. */
    void copyConditionsTo(Value<?, ?> dependentValue) {
        for (Map.Entry<ConditionalValue, ValueCondition> entry : this.conditions.entrySet()) {
            dependentValue.inheritCondition(entry.getKey(), entry.getValue());
        }
    }

    public void setSerializable(boolean serializable) {
        this.serializable = serializable;
    }

    public static void setLegacyState(String state) {
        legacyState = state;
    }

    public boolean areConditionsMet() {
        HashMap<ValueCondition, Boolean> hashMap = new HashMap<ValueCondition, Boolean>();
        for (Map.Entry<ConditionalValue, ValueCondition> entry : this.conditions.entrySet()) {
            ValueCondition valueCondition = entry.getValue();
            if (valueCondition != null) {
                if (!hashMap.containsKey(valueCondition)) {
                    hashMap.put(valueCondition, valueCondition.isSatisfied());
                }
                if (((Boolean)hashMap.get(valueCondition)).booleanValue()) continue;
                return false;
            }
            if (entry.getKey().isDependentValueActive(this)) continue;
            return false;
        }
        return true;
    }

    public T addAlias(String alias) {
        this.aliases.add(alias);
        return (T)this;
    }

    @Nullable
    public Color getOverrideColor() {
        return this.overrideColor;
    }

    public boolean isPersistenceSuppressed() {
        return this.suppressPersistence;
    }

    @Nullable
    public String getDescription() {
        return this.description == null ? null : this.description.get();
    }

    public abstract T copyValueDefinition();

    public void setBoundComponent(GuiComponent component) {
        this.boundComponent = component;
    }

    public void setDirectValue(K k) {
        this.currentValue = k;
        this.notifyChanged();
    }

    public boolean loadJson(JsonObject jsonObject) {
        if (this.matchesJsonId(jsonObject)) {
            String string = "";
            String string2 = ConfigJsonUtils.getString(jsonObject, "value");
            if (string2 != null) {
                string = string2;
            }
            if (this.encodeBase64 && string.startsWith("b64:")) {
                string = string.split(":")[1];
                string = Base64Util.decodeUtf8Base64(string);
            }
            this.parse(string);
            this.notifyChangeListeners();
            return true;
        }
        return false;
    }

    public Value getParent() {
        return this.parent;
    }

    public K copyValue() {
        return this.currentValue;
    }

    public void notifyChanged() {
        if (!this.suppressPersistence) {
            SonyVegas.INSTANCE.saveAndStop();
        }
        for (ValueChangeListener<T> valueChangeListener : this.changeListeners) {
            valueChangeListener.onValueChanged((T)this);
        }
    }

    public K getDirectValue() {
        return this.currentValue;
    }

    public void setParent(Value value) {
        this.parent = value;
    }

    public <T> T setResettable(boolean resettable) {
        this.resettable = resettable;
        return (T)this;
    }

    public void setValue(K k) {
        this.accessor.setValue(k);
    }

    public void setDefaultValue(K k) {
        this.defaultValue = k;
    }

    public boolean isHidden() {
        return this.hidden;
    }

    public GuiComponent getBoundComponent() {
        return this.boundComponent;
    }

    public boolean isSerializable() {
        return this.serializable;
    }

    public T setDescription(@Nullable String description) {
        this.description = Str.enc(description);
        return (T)this;
    }

    public boolean isChangeValid(K k) {
        K k2 = this.getValue();
        for (ValueChangeValidator<T, K> valueChangeValidator : this.validators) {
            if (valueChangeValidator.isValidChange((T)this, k2, k)) continue;
            return false;
        }
        return true;
    }

    public void setOverrideColor(@Nullable Color color) {
        this.overrideColor = color;
    }

    public K getValueCompat() {
        return this.accessor.getValue();
    }

    public static String getLegacyState() {
        return legacyState;
    }

    public String getId() {
        return Str.stableVisibleToken("v", this.segmentedLegacyId());
    }

    /** Keeps the old readable config path segmented while matching old files. */
    private String segmentedLegacyId() {
        StringBuilder path = new StringBuilder();
        this.appendSegmentedLegacyId(path);
        return path.toString();
    }

    private void appendSegmentedLegacyId(StringBuilder path) {
        if (this.parent != null) {
            this.parent.appendSegmentedLegacyId(path);
            path.append('-');
        }
        if (this.owner instanceof SubModule) {
            path.append(((SubModule)this.owner).getName()).append('-');
        }
        path.append(this.getName());
    }

    public void reset() {
        if (this.resettable && this.getDefaultValue() != null) {
            this.setValue(this.getDefaultValue());
        }
    }

    public String getDisplayValue() {
        K k = this.getValue();
        if (k == null) {
            return "";
        }
        String string = k.toString();
        if (string.indexOf(64) != -1 && string.indexOf(32) == -1) {
            return "";
        }
        return string;
    }

    public T setBase64Encoded(boolean encodeBase64) {
        this.encodeBase64 = encodeBase64;
        return (T)this;
    }

    static {
        Value.setLegacyState("RrLfY");
    }

    public Object java_lang_Object_K() {
        return this.getValue();
    }
}

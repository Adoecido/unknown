package vegas.sony.value;

import vegas.sony.value.Value;
import vegas.sony.value.ValueCondition;
import java.util.ArrayList;
import java.util.List;

public abstract class ConditionalValue<K, T extends ConditionalValue<K, T>>
extends Value<K, T> {
    private final List<Value> dependentValues = new ArrayList<Value>();
    private static String[] legacyState;

    public ValueCondition whenNotEqualTo(K value) {
        return new ValueCondition().require(this, candidate -> ConditionalValue.isNotEqual(value, candidate));
    }

    public static void setLegacyState(String[] state) {
        legacyState = state;
    }

    public abstract boolean hasActiveDependentBranch();

    public List<Value> getDependentValues() {
        return this.dependentValues;
    }

    public static String[] getConditionalLegacyState() {
        return legacyState;
    }

    public ConditionalValue(Object object, String string, K k) {
        super(object, string, k);
    }


    static {
        if (ConditionalValue.getConditionalLegacyState() != null) {
            ConditionalValue.setLegacyState(new String[3]);
        }
    }

    public T addDependentValues(Value<?, ?> ... values) {
        for (Value<?, ?> value : values) {
            if (value == null) {
                continue;
            }
            value.setParent(this);
            value.addConditionalValue(this);
            // A child may be attached after this conditional value already
            // inherited an outer mode condition. Copy those ancestors now so
            // late-registered descendants cannot leak into another branch.
            this.copyConditionsTo(value);
            // A mode can expose the same value in more than one branch. Keep
            // one dependency node while retaining all branch associations in
            // ModeValue's active-mode map.
            if (!this.dependentValues.contains(value)) {
                this.dependentValues.add(value);
            }
        }
        return (T)this;
    }

    public ValueCondition whenEqualTo(K value) {
        return new ValueCondition().requireEqual(this, value);
    }

    private static boolean isNotEqual(Object expected, Object candidate) {
        return !candidate.equals(expected);
    }

    public abstract boolean isDependentValueActive(Value value);

    public Value getTerminalDependentValue() {
        for (Value value : this.getDependentValues()) {
            if (!value.equals(this.getDependentValues().get(this.getDependentValues().size() - 1))) continue;
            if (value instanceof ConditionalValue) {
                ConditionalValue conditionalValue = (ConditionalValue)value;
                if (!conditionalValue.getDependentValues().isEmpty() && conditionalValue.hasActiveDependentBranch()) {
                    Value terminalValue = (Value)conditionalValue.getDependentValues().get(conditionalValue.getDependentValues().size() - 1);
                    if (!conditionalValue.getDependentValues().isEmpty()) {
                        return conditionalValue.getTerminalDependentValue();
                    }
                    return terminalValue;
                }
                return value;
            }
            return value;
        }
        return null;
    }
}

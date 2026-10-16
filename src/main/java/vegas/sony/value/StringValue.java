package vegas.sony.value;

import vegas.sony.value.Value;

public class StringValue
extends Value<String, StringValue> {
    private String definitionName;
    private ListValueSuggestionProvider suggestionProvider;

    public String toString() {
        return (String)this.getValue();
    }

    public StringValue(Object owner, String name, String defaultValue) {
        super(owner, name, defaultValue);
    }

    @Override
    public void parse(String serializedValue) {
        this.setValue(serializedValue);
    }

    public static StringValue create(Object owner, String name, String defaultValue) {
        StringValue stringValue = new StringValue(owner, name, defaultValue);
        stringValue.setDefinitionName(name);
        return stringValue;
    }

    public StringValue copyDefinition() {
        return StringValue.create(null, this.getDefinitionName(), (String)this.getValue());
    }

    @Override
    public StringValue copyValueDefinition() {
        return this.copyDefinition();
    }

    public String getDefinitionName() {
        return this.definitionName;
    }

    public StringValue setSuggestionProvider(ListValueSuggestionProvider suggestionProvider) {
        this.suggestionProvider = suggestionProvider;
        return this;
    }

    public ListValueSuggestionProvider getSuggestionProvider() {
        return this.suggestionProvider;
    }

    private void setDefinitionName(String definitionName) {
        this.definitionName = definitionName;
    }
}

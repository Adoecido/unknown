package vegas.sony.ui.click.component.value;

import vegas.sony.module.blatant.antibot.AntiBotBooleanValue;
import vegas.sony.module.blatant.antibot.AntiBotModeValue;
import vegas.sony.module.utility.inventory.HotbarSlotRuleEditorComponent;
import vegas.sony.module.utility.inventory.HotbarSlotRuleValue;
import vegas.sony.module.utility.inventory.cleaner.InventoryCleanerProfileValue;
import vegas.sony.module.utility.inventory.cleaner.ui.InventoryCleanerProfileValueComponent;
import vegas.sony.ui.click.component.DropdownSelectComponent;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.ui.click.component.input.BindValueRowComponent;
import vegas.sony.unmap.ItemLimitData;
import vegas.sony.value.BindValue;
import vegas.sony.value.BooleanValue;
import vegas.sony.value.ColorValue;
import vegas.sony.value.EntityTargetFilterValue;
import vegas.sony.value.ListValue;
import vegas.sony.value.ModeValue;
import vegas.sony.value.NumberValue;
import vegas.sony.value.OptionalLimitEntry;
import vegas.sony.value.RandomValue;
import vegas.sony.value.SnapshotValueAccessor;
import vegas.sony.value.StringMapValue;
import vegas.sony.value.StringValue;
import vegas.sony.value.Value;
import vegas.sony.value.ValueSnapshot;

public class ValueComponentFactory {
    public static GuiComponent createValueComponent(Value<?, ?> value, boolean snapshotEditor, ValueComponentMode componentMode) {
        GuiComponent component = value.getBoundComponent();
        if (value instanceof BooleanValue) {
            component = new BooleanToggleComponent((BooleanValue)value);
        } else if (value instanceof AntiBotModeValue) {
            component = new ColorValueDropdownComponent((AntiBotModeValue)value);
        } else if (value instanceof AntiBotBooleanValue) {
            component = new AntiBotBooleanValueOptionRow((AntiBotBooleanValue)value);
        } else if (value instanceof ModeValue) {
            component = new DropdownSelectComponent((ModeValue)value);
            if (componentMode == ValueComponentMode.STANDALONE) {
                ((DropdownSelectComponent)component).setHighlightedStyle(true);
            }
        } else if (value instanceof NumberValue) {
            component = new NumberSliderComponent((NumberValue)value);
        } else if (value instanceof RandomValue) {
            component = new RandomRangeSliderComponent((RandomValue)value);
        } else if (value instanceof ColorValue) {
            component = new ColorValueEditorComponent((ColorValue)value);
        } else if (value instanceof ListValue) {
            BooleanValue parentToggle;
            ListValueComponent listValueComponent = new ListValueComponent((ListValue)value);
            if (componentMode == ValueComponentMode.STANDALONE) {
                listValueComponent.setMode(ValueComponentMode.STANDALONE);
            }
            component = listValueComponent;
            ListValue listValue = (ListValue)value;
            if (listValue.getParent() instanceof BooleanValue && (parentToggle = (BooleanValue)listValue.getParent()).getTerminalDependentValue() != null && parentToggle.getTerminalDependentValue().equals(listValue)) {
                component = null;
            }
        } else if (value instanceof HotbarSlotRuleValue) {
            if (!snapshotEditor) {
                HotbarSlotRuleValue hotbarSlotRuleValue = (HotbarSlotRuleValue)value;
                if (hotbarSlotRuleValue.getEditor() != null) {
                    component = hotbarSlotRuleValue.getEditor();
                    component.setVisible(true);
                } else {
                    component = new HotbarSlotRuleEditorComponent(hotbarSlotRuleValue);
                }
            }
        } else if (value instanceof EntityTargetFilterValue) {
            component = snapshotEditor ? new EntityTargetFilterPopupComponent((EntityTargetFilterValue)value) : new EntityTargetFilterComponent((EntityTargetFilterValue)value);
        } else if (value instanceof StringMapValue) {
            component = new StringMapValueComponent((StringMapValue)value);
        } else if (value instanceof StringValue) {
            component = new StringValueTextInputComponent((StringValue)value);
        } else if (value instanceof BindValue) {
            component = new BindValueRowComponent((BindValue)value);
        } else if (value instanceof InventoryCleanerProfileValue) {
            component = new InventoryCleanerProfileValueComponent((InventoryCleanerProfileValue)value);
        }
        return component;
    }


    public static GuiComponent createMainValueComponent(Value<?, ?> value) {
        return ValueComponentFactory.createValueComponent(value, false, ValueComponentMode.MAIN);
    }

    public static Value createSnapshotProxyValue(ValueSnapshot valueSnapshot) {
        Value sourceValue = valueSnapshot.getSourceValue();
        Value proxyValue = sourceValue.copyValueDefinition();
        proxyValue.setDefaultValue(sourceValue.getDefaultValue());
        proxyValue.setValue(valueSnapshot.getValue());
        proxyValue.setAccessor(new SnapshotValueAccessor(valueSnapshot, proxyValue));
        return proxyValue;
    }

    public static Object getComparableListEntryText(Object entry) {
        if (entry instanceof OptionalLimitEntry || entry instanceof ItemLimitData) {
            return entry.toString();
        }
        return null;
    }

    public static GuiComponent createMainValueComponent(Value<?, ?> value, boolean snapshotEditor) {
        return ValueComponentFactory.createValueComponent(value, snapshotEditor, ValueComponentMode.MAIN);
    }
}

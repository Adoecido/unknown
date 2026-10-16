package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingField;
import vegas.sony.ui.click.component.GuiComponent;

public class MResourceKeyEnchantmentBridge
extends Mapping {
    private MappingField enchantmentField;
    private static boolean enchantmentRegistryControlFlowState;
    private static final String ENCHANTMENT_FIELD_NAME;

    public static Object getEnchantment(MResourceKeyEnchantmentBridge mapping) {
        return mapping.readEnchantment();
    }

    public static void setEnchantmentRegistryControlFlowState(boolean state) {
        enchantmentRegistryControlFlowState = state;
    }

    public static boolean getEnchantmentRegistryControlFlowState() {
        return enchantmentRegistryControlFlowState;
    }

    static {
        MResourceKeyEnchantmentBridge.setEnchantmentRegistryControlFlowState(true);
        ENCHANTMENT_FIELD_NAME = "ENCHANTMENT";
    }


    public static boolean shouldUpdateLegacyState() {
        boolean controlFlowState = MResourceKeyEnchantmentBridge.getEnchantmentRegistryControlFlowState();
        return false;
    }

    public MResourceKeyEnchantmentBridge() {
        super(MappedClasses.a);
        Class enchantmentFieldType = MappedClasses.qB;
        boolean enchantmentFieldPublic = true;
        String enchantmentFieldName = ENCHANTMENT_FIELD_NAME;
        MResourceKeyEnchantmentBridge mapping = this;
        this.enchantmentField = mapping.registerStaticField(enchantmentFieldName, enchantmentFieldPublic, enchantmentFieldType);
        if (MResourceKeyEnchantmentBridge.shouldUpdateLegacyState()) {
            GuiComponent.setLegacyComponentState(new GuiComponent[1]);
            return;
        }
    }

    private Object readEnchantment() {
        return this.enchantmentField.getObject(null);
    }
}


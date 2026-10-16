package vegas.sony.module.utility.inventory.cleaner;

import com.google.gson.JsonObject;
import vegas.sony.config.ConfigJsonUtils;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.module.utility.inventory.cleaner.ComparisonOperator;
import vegas.sony.module.utility.inventory.cleaner.InventoryFilterConditionType;
import vegas.sony.module.utility.inventory.cleaner.NumericFilterCondition;
import vegas.sony.module.utility.inventory.cleaner.PotionEffectFilterMode;
import vegas.sony.wrapper.impl.Item;
import vegas.sony.wrapper.impl.ItemSplashPotion;
import vegas.sony.wrapper.impl.ItemStack;
import vegas.sony.wrapper.impl.PotionEffect;
import vegas.sony.wrapper.impl.PotionEntry;
import vegas.sony.wrapper.impl.PotionRegistry;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public class PotionEffectFilterCondition
implements NumericFilterCondition<PotionEffectFilterCondition> {
    @Override
    public PotionEffectFilterCondition parseValue(String value) throws NumberFormatException {
        return this.parseLevel(value);
    }

    @Override
    public PotionEffectFilterCondition copy() {
        return new PotionEffectFilterCondition(this.mode, this.potionId, this.level, this.operator);
    }
    private Short potionId;
    private ComparisonOperator operator;
    private PotionEffectFilterMode mode = PotionEffectFilterMode.HAS;
    private int level = 1;

    @Override
    public InventoryFilterConditionType getType() {
        return InventoryFilterConditionType.POTION_EFFECT;
    }

    @Override
    public ComparisonOperator getOperator() {
        return this.operator;
    }

    public PotionEffectFilterCondition withPotionId(Short potionId) {
        this.potionId = potionId;
        return this;
    }

    @Override
    public JsonObject toJson() {
        JsonObject jsonObject = NumericFilterCondition.super.toJson();
        jsonObject.addProperty("mode", this.mode.getName());
        jsonObject.addProperty("potionId", (Number)this.potionId);
        jsonObject.addProperty("level", (Number)this.level);
        return jsonObject;
    }

    @Nullable
    public PotionEntry getPotionEntry() {
        if (this.potionId == null) {
            return null;
        }
        return PotionRegistry.A(this.potionId);
    }

    public PotionEffectFilterMode getMode() {
        return this.mode;
    }

    @Override
    public boolean matches(ItemStack itemStack) {
        if (this.potionId == null) {
            return false;
        }
        if (itemStack.isNull()) {
            return false;
        }
        Item item = itemStack.getItem();
        if (item.isNull() || !item.isInstance(MappedClasses.Di) && !item.isInstance(MappedClasses.o)) {
            return false;
        }
        ItemSplashPotion potionItem = new ItemSplashPotion(item);
        List<PotionEffect> effects = potionItem.getPotionEffects(itemStack);
        for (PotionEffect potionEffect : effects) {
            PotionEntry potionEntry = PotionRegistry.R(potionEffect);
            if (potionEntry == null || potionEntry.getLegacyId() != this.potionId.shortValue()) continue;
            switch (this.mode) {
                case HAS: {
                    return true;
                }
                case LEVEL: {
                    return this.operator.compare(potionEffect.L(), this.level);
                }
                case DURATION: {
                    return this.operator.compare(potionEffect.k(), this.level);
                }
            }
        }
        return false;
    }

    public PotionEffectFilterCondition withOperator(ComparisonOperator operator) {
        this.operator = operator;
        return this;
    }

    public PotionEffectFilterCondition() {
        this.operator = ComparisonOperator.EQUALS;
    }

    public int getLevel() {
        return this.level;
    }

    public Short getPotionId() {
        return this.potionId;
    }

    public PotionEffectFilterCondition withMode(PotionEffectFilterMode mode) {
        this.mode = mode;
        return this;
    }

    @Override
    public String getValueText() {
        return String.valueOf(this.level);
    }

    public PotionEffectFilterCondition(JsonObject jsonObject) {
        this.operator = ComparisonOperator.EQUALS;
        this.mode = PotionEffectFilterMode.fromName(ConfigJsonUtils.getString(jsonObject, "mode"));
        this.potionId = ConfigJsonUtils.getShort(jsonObject, "potionId");
        Integer configuredLevel = ConfigJsonUtils.getInteger(jsonObject, "level");
        this.level = configuredLevel != null ? configuredLevel : 1;
        this.operator = ComparisonOperator.fromName(jsonObject.get("operator").getAsString());
    }

    public PotionEffectFilterCondition withLevel(int level) {
        this.level = level;
        return this;
    }

    public PotionEffectFilterCondition(PotionEffectFilterMode mode, short potionId, int level, ComparisonOperator operator) {
        this.operator = ComparisonOperator.EQUALS;
        this.mode = mode;
        this.potionId = potionId;
        this.level = level;
        this.operator = operator;
    }

    public PotionEffectFilterCondition parseLevel(String value) throws NumberFormatException {
        this.level = Integer.parseInt(value);
        return this;
    }

}

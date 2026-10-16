package vegas.sony.wrapper.impl;

public class EnchantmentModifierDamage
extends EnchantmentModifier {
    public DamageSource getSource() {
        return new DamageSource(EnchantmentModifierDamage.sonyVegasInstance.getMappingsMapperCompat().enchantmentDamageModifier.getSource(this.I));
    }

    public void setSource(DamageSource damageSource) {
        EnchantmentModifierDamage.sonyVegasInstance.getMappingsMapperCompat().enchantmentDamageModifier.setSource(this.I, damageSource.getObject());
    }

    public int getDamageModifierValue() {
        return EnchantmentModifierDamage.sonyVegasInstance.getMappingsMapperCompat().enchantmentDamageModifier.getDamageModifier(this.I);
    }

    public void setDamageModifier(int damageModifier) {
        EnchantmentModifierDamage.sonyVegasInstance.getMappingsMapperCompat().enchantmentDamageModifier.setDamageModifier(this.I, damageModifier);
    }

    public EnchantmentModifierDamage(Object handle) {
        super(handle);
    }
}

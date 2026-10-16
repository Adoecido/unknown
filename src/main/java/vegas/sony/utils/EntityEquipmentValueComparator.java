package vegas.sony.utils;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.utils.ItemStackScoreUtil;
import vegas.sony.wrapper.impl.Entity;
import vegas.sony.wrapper.impl.EntityPlayer;
import vegas.sony.wrapper.impl.EntityPlayerSP;
import vegas.sony.wrapper.impl.ItemStack;
import vegas.sony.wrapper.impl.Minecraft;
import java.util.Comparator;

public class EntityEquipmentValueComparator
implements Comparator<Entity> {
    @Override
    public int compare(Entity firstEntity, Entity secondEntity) {
        return this.compareByEquipmentValue(firstEntity, secondEntity);
    }


    public int compareByEquipmentValue(Entity firstEntity, Entity secondEntity) {
        if (firstEntity.isInstance(MappedClasses.Yl) && secondEntity.isInstance(MappedClasses.Yl)) {
            return Double.compare(this.calculateEquipmentValue(firstEntity), this.calculateEquipmentValue(secondEntity));
        }
        EntityPlayerSP player = Minecraft.thePlayer();
        return Float.compare(player.getDistanceToEntity(firstEntity), player.getDistanceToEntity(secondEntity));
    }

    private double calculateEquipmentValue(Entity entity) {
        double equipmentValue = 0.0;
        for (Object itemStackHandle : new EntityPlayer(entity.getObject()).V$src$Lvegas_sony_wrapper_impl_InventoryPlayer_$erqak6().i()) {
            ItemStack itemStack = new ItemStack(itemStackHandle);
            equipmentValue += ItemStackScoreUtil.L(itemStack);
        }
        return equipmentValue;
    }
}

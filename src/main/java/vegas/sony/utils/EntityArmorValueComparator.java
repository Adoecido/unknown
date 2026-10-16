package vegas.sony.utils;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.utils.ItemStackScoreUtil;
import vegas.sony.wrapper.impl.Entity;
import vegas.sony.wrapper.impl.EntityPlayer;
import vegas.sony.wrapper.impl.EntityPlayerSP;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.PotionEffect;
import vegas.sony.wrapper.impl.PotionRegistry;
import java.util.Comparator;

public class EntityArmorValueComparator
implements Comparator<Entity> {
    @Override
    public int compare(Entity firstEntity, Entity secondEntity) {
        return this.compareByArmorValue(firstEntity, secondEntity);
    }

    public int compareByArmorValue(Entity firstEntity, Entity secondEntity) {
        if (firstEntity.isInstance(MappedClasses.Yl) && secondEntity.isInstance(MappedClasses.Yl)) {
            return Float.compare(this.calculateArmorValue(firstEntity), this.calculateArmorValue(secondEntity));
        }
        EntityPlayerSP player = Minecraft.thePlayer();
        return Float.compare(player.getDistanceToEntity(firstEntity), player.getDistanceToEntity(secondEntity));
    }


    private float calculateArmorValue(Entity entity) {
        float armorValue = 0.0f;
        EntityPlayer player = new EntityPlayer(entity.getObject());
        if (player.B$src$Lvegas_sony_wrapper_impl_ItemStack_$impdvt().isNotNull()) {
            PotionEffect potionEffect;
            armorValue += ItemStackScoreUtil.I$src$F$dh3k81(player.B$src$Lvegas_sony_wrapper_impl_ItemStack_$impdvt());
            if (player.i(PotionRegistry.t) && (potionEffect = player.b(PotionRegistry.t)).k() > 0) {
                armorValue = (float)((double)armorValue * (1.375 * (double)potionEffect.L()));
            }
        }
        return armorValue;
    }
}

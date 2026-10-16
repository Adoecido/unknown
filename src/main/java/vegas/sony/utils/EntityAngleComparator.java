package vegas.sony.utils;

import vegas.sony.utils.RotationUtil;
import vegas.sony.wrapper.impl.Entity;
import vegas.sony.wrapper.impl.EntityPlayerSP;
import vegas.sony.wrapper.impl.Minecraft;
import java.util.Comparator;

public class EntityAngleComparator
implements Comparator<Entity> {
    @Override
    public int compare(Entity firstEntity, Entity secondEntity) {
        return this.compareByAngle(firstEntity, secondEntity);
    }

    public int compareByAngle(Entity firstEntity, Entity secondEntity) {
        EntityPlayerSP player = Minecraft.thePlayer();
        return Integer.compare(RotationUtil.a(player, firstEntity), RotationUtil.a(player, secondEntity));
    }
}

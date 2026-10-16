package vegas.sony.utils;

import vegas.sony.wrapper.impl.Entity;
import vegas.sony.wrapper.impl.EntityPlayerSP;
import vegas.sony.wrapper.impl.Minecraft;
import java.util.Comparator;

public class EntityDistanceComparator
implements Comparator<Entity> {
    @Override
    public int compare(Entity firstEntity, Entity secondEntity) {
        return this.compareByDistance(firstEntity, secondEntity);
    }

    public int compareByDistance(Entity firstEntity, Entity secondEntity) {
        EntityPlayerSP player = Minecraft.thePlayer();
        return Float.compare(player.getDistanceToEntity(firstEntity), player.getDistanceToEntity(secondEntity));
    }
}

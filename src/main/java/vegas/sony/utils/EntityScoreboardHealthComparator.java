package vegas.sony.utils;

import vegas.sony.wrapper.impl.EntityLivingBase;
import java.util.Comparator;
import java.util.Map;

public class EntityScoreboardHealthComparator
implements Comparator<EntityLivingBase> {
    private final Map<String, Integer> scoreboardHealth;

    public EntityScoreboardHealthComparator(Map<String, Integer> scoreboardHealth) {
        this.scoreboardHealth = scoreboardHealth;
    }

    private float getHealth(EntityLivingBase entityLivingBase) {
        Integer scoreboardValue = this.scoreboardHealth.get(entityLivingBase.getName());
        if (scoreboardValue != null) {
            return scoreboardValue.floatValue();
        }
        return entityLivingBase.w$src$F$15l9epb();
    }

    @Override
    public int compare(EntityLivingBase entityLivingBase, EntityLivingBase entityLivingBase2) {
        return Float.compare(this.getHealth(entityLivingBase), this.getHealth(entityLivingBase2));
    }
}
package vegas.sony.module.render.proj;

import vegas.sony.wrapper.impl.EntityEnderPearl;
import java.awt.Color;

public interface IProjectile {
    default float getCollisionRadius() {
        return 0.125f;
    }

    boolean matches(EntityEnderPearl entity);

    default float getCollisionHeight() {
        return 0.25f;
    }

    Color getColor();
}

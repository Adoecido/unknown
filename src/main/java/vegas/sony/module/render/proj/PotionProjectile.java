package vegas.sony.module.render.proj;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.module.render.proj.Projectile;
import java.awt.Color;
import java.util.Collections;
import java.util.HashSet;

public class PotionProjectile
extends Projectile {
    public PotionProjectile() {
        super(new HashSet<Class>(Collections.singleton(MappedClasses.Zf)), new Color(255, 66, 249));
    }
}


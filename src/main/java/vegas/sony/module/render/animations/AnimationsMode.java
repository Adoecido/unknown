package vegas.sony.module.render.animations;

import vegas.sony.module.Mod;
import vegas.sony.module.SubModule;
import vegas.sony.module.render.Animations;

public abstract class AnimationsMode
extends SubModule<Animations> {
    public boolean shouldBlock() {
        return false;
    }

    public abstract boolean isBlocking();

    public AnimationsMode(Mod parent, String name) {
        super(parent, name);
    }
}
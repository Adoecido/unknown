package vegas.sony.module;

import vegas.sony.module.Category;
import vegas.sony.module.Mod;

public class UtilityMod
extends Mod {
    public UtilityMod(String name) {
        super(name, 0, 0, Category.HIDDEN, "");
    }

    public UtilityMod(String name, String description) {
        super(name, 0, 0, Category.HIDDEN, description);
    }

    @Override
    public boolean isRequiresBind() {
        return true;
    }

    public UtilityMod(String name, Category category, String description) {
        super(name, 0, 0, category, description);
    }
}


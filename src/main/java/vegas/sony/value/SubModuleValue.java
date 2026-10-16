package vegas.sony.value;

import vegas.sony.module.Mod;
import vegas.sony.module.SubModule;
import vegas.sony.unmap.ModeOption;

public class SubModuleValue<T extends SubModule>
extends ModeOption {
    private final T instance;

    public SubModuleValue(String name, T instance) {
        super(name);
        this.instance = instance;
    }

    public SubModuleValue(T instance) {
        this(((Mod)instance).getName(), instance);
    }

    public T getInstance() {
        return this.instance;
    }
}

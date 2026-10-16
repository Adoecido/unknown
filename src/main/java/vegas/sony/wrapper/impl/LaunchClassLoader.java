package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MLaunchClassLoader;
import vegas.sony.wrapper.Wrapper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LaunchClassLoader
extends Wrapper {
    public static LaunchClassLoader getLaunchClassLoader() {
        return new LaunchClassLoader(LaunchClassLoader.sonyVegasInstance.getMappingsMapperCompat().C4.getInstance());
    }

    public LaunchClassLoader(Object object) {
        super(object);
    }

    public Set getClassLoaderExceptions() {
        if (ForgeVersion.MC_1_16_5.d()) {
            return new HashSet();
        }
        return MLaunchClassLoader.l(LaunchClassLoader.sonyVegasInstance.getMappingsMapperCompat().C4, this.I);
    }

    public Map cachedClasses() {
        if (ForgeVersion.MC_1_16_5.d()) {
            return new HashMap();
        }
        return LaunchClassLoader.sonyVegasInstance.getMappingsMapperCompat().C4.cachedClasses(this.I);
    }

    public boolean supportsLegacyClassCache() {
        return LaunchClassLoader.sonyVegasInstance.getMappingsMapperCompat().C4.supportsLegacyClassCache();
    }

}


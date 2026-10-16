package vegas.sony.wrapper.impl;

import vegas.sony.SonyVegas;
import vegas.sony.mapping.mappings.MBuiltInRegistries;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.wrapper.impl.Registry;

public class BuiltInRegistries {
    private static GuiComponent[] N;

    public static Registry W() {
        return new Registry(MBuiltInRegistries.M(SonyVegas.INSTANCE.getMappingsMapperCompat().q7));
    }

    public static Registry j() {
        return new Registry(MBuiltInRegistries.w(SonyVegas.INSTANCE.getMappingsMapperCompat().q7));
    }

    public static Registry Y() {
        return new Registry(MBuiltInRegistries.v(SonyVegas.INSTANCE.getMappingsMapperCompat().q7));
    }

    public static Registry I() {
        return new Registry(MBuiltInRegistries.F(SonyVegas.INSTANCE.getMappingsMapperCompat().q7));
    }

    public static GuiComponent[] f() {
        return N;
    }

    public static void O(GuiComponent[] guiComponentArray) {
        N = guiComponentArray;
    }

    public static Registry a() {
        return new Registry(MBuiltInRegistries.g(SonyVegas.INSTANCE.getMappingsMapperCompat().q7));
    }

    static {
        if (BuiltInRegistries.f() != null) {
            BuiltInRegistries.O(new GuiComponent[1]);
        }
    }
}


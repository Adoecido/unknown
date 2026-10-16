package vegas.sony.utils.render;

import vegas.sony.wrapper.impl.EntityRenderer;
import vegas.sony.wrapper.impl.ForgeVersion;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.FogType;

public class Post117RenderPhaseCompat {

    public static void applyRenderPhaseCompat() {
        if (ForgeVersion.MC_1_21_6.v()) {
            return;
        }
        EntityRenderer entityRenderer = Minecraft.m$src$Lvegas_sony_wrapper_impl_EntityRenderer_$13begmf();
        entityRenderer.V().P(entityRenderer.getFogRenderer().getBuffer(FogType.noneOrSky()));
    }
}


package vegas.sony.utils.render;

import vegas.sony.wrapper.impl.EntityLivingBase;
import vegas.sony.wrapper.impl.ResourceLocation;
import java.awt.Color;

public interface EntityModelRenderBackend {
    public void captureEntity(EntityLivingBase entity);

    public void render(float x, float y, int width, int height, Color color, float cornerRadius);

    public void captureTexture(ResourceLocation texture);

    public void dispose();
}

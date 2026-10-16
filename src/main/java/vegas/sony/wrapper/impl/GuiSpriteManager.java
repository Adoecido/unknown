package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class GuiSpriteManager
extends Wrapper {
    public TextureAtlasSprite getSprite(ResourceLocation resourceLocation) {
        return new TextureAtlasSprite(GuiSpriteManager.sonyVegasInstance.getMappingsMapperCompat().h4.getSprite(this.I, resourceLocation.getObject()));
    }

    public GuiSpriteManager(Object handle) {
        super(handle);
    }
}

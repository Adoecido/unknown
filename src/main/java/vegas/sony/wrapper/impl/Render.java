package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MRender;
import vegas.sony.wrapper.Wrapper;

public class Render<T extends Entity>
extends Wrapper {
    public ResourceLocation getEntityTexture(Entity entity) {
        return new ResourceLocation(MRender.s(Render.sonyVegasInstance.getMappingsMapperCompat().qe, this.I, entity.getObject()));
    }

    public void doRender(Entity entity, double d, double d2, double d3, float f, float f2) {
        MRender.E(Render.sonyVegasInstance.getMappingsMapperCompat().qe, this.I, entity.getObject(), d, d2, d3, f, f2);
    }

    public RenderStateBridge a(Entity entity, float f) {
        return new RenderStateBridge(MRender.s(Render.sonyVegasInstance.getMappingsMapperCompat().qe, this.I, entity.getObject(), f));
    }

    public Render(Object object) {
        super(object);
    }
}


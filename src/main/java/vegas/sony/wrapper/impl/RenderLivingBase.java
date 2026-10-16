package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MRenderLivingBase;

import java.nio.FloatBuffer;
import java.util.List;

public class RenderLivingBase<T extends EntityLivingBase>
extends Render<T> {
    public ModelBipedSkeletonBridge getMainModel() {
        return new ModelBipedSkeletonBridge(MRenderLivingBase.x(RenderLivingBase.sonyVegasInstance.getMappingsMapperCompat().CP, this.I));
    }

    public List<Object> getLayerRenderers() {
        return (List)MRenderLivingBase.i(RenderLivingBase.sonyVegasInstance.getMappingsMapperCompat().CP, this.I);
    }

    public RenderLivingBase(Object object) {
        super(object);
    }

    public void setLayerRenderers(List list) {
        MRenderLivingBase.O(RenderLivingBase.sonyVegasInstance.getMappingsMapperCompat().CP, this.I, list);
    }

    public FloatBuffer P() {
        return MRenderLivingBase.y(RenderLivingBase.sonyVegasInstance.getMappingsMapperCompat().CP, this.I);
    }
}


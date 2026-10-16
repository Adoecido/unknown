package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MModelBiped;

public class ModelBiped
extends ModelBipedSkeletonBridge {
    public ModelBiped(Object object) {
        super(object);
    }

    public ModelRenderer getBipedLeftArm() {
        return new ModelRenderer(MModelBiped.j(ModelBiped.sonyVegasInstance.getMappingsMapperCompat().Ct, this.I));
    }

    public ModelRenderer getBipedLeftLeg() {
        return new ModelRenderer(MModelBiped.k(ModelBiped.sonyVegasInstance.getMappingsMapperCompat().Ct, this.I));
    }

    public ModelRenderer getBipedRightLeg() {
        return new ModelRenderer(MModelBiped.J(ModelBiped.sonyVegasInstance.getMappingsMapperCompat().Ct, this.I));
    }

    public ModelRenderer getBipedRightArm() {
        return new ModelRenderer(MModelBiped.R(ModelBiped.sonyVegasInstance.getMappingsMapperCompat().Ct, this.I));
    }

    public ModelRenderer Z() {
        return new ModelRenderer(MModelBiped.a(ModelBiped.sonyVegasInstance.getMappingsMapperCompat().Ct, this.I));
    }

    public ModelRenderer F() {
        return new ModelRenderer(MModelBiped.x(ModelBiped.sonyVegasInstance.getMappingsMapperCompat().Ct, this.I));
    }
}


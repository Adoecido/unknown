package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingMethod;

/** Mapping for the legacy LayerCape renderer used by Badlion 1.8.9. */
public final class MCapeLayer extends Mapping {
    public final MappingMethod renderMethod;

    public MCapeLayer() {
        super(MappedClasses.CAPE_LAYER);
        if (MappedClasses.CAPE_LAYER == null || MappedClasses.zt == null) {
            this.renderMethod = null;
        } else {
            this.renderMethod = this.Y("doRenderLayer", true, Void.TYPE,
                    MappedClasses.zt, Float.TYPE, Float.TYPE, Float.TYPE,
                    Float.TYPE, Float.TYPE, Float.TYPE, Float.TYPE);
        }
    }
}

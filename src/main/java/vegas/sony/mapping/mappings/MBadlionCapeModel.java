package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingMethod;

/** Maps the legacy ModelPlayer cape method used by Badlion 1.8.9. */
public final class MBadlionCapeModel extends Mapping {
    public final MappingMethod renderCape;

    public MBadlionCapeModel() {
        super(MappedClasses.ud);
        this.renderCape = MappedClasses.ud == null ? null
                : this.Y("renderCape", true, Void.TYPE, Float.TYPE);
    }
}

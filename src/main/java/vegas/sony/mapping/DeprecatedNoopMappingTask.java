package vegas.sony.mapping;

import vegas.sony.mapping.JavassistMappingTask;
import vegas.sony.mapping.MappedClasses;

@Deprecated
public class DeprecatedNoopMappingTask
extends JavassistMappingTask {
    @Override
    public void transform() {
    }

    public DeprecatedNoopMappingTask() {
        super(MappedClasses.DC);
    }
}

package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.mapping.InjectionParameterSpec;
import vegas.sony.mapping.JavassistMappingTask;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.MappingMethod;
import vegas.sony.mapping.ThreadBoundEventPostTickCallback;
import vegas.sony.mapping.ThreadBoundEventPreTickCallback;

public class ThreadBoundTickEventMappingTask
extends JavassistMappingTask {
    public ThreadBoundTickEventMappingTask() {
        super(MappedClasses.zY);
    }

    @Override
    public void transform() {
        ThreadBoundTickEventMappingTask.p(ThreadBoundEventPreTickCallback.class);
        ThreadBoundTickEventMappingTask.p(ThreadBoundEventPostTickCallback.class);
        MappingMethod mappingMethod = SonyVegas.INSTANCE.getMappings().itemBucketUse.useMethod;
        MappingMethod mappingMethod2 = SonyVegas.INSTANCE.getMappings().CF.p;
        this.k(mappingMethod, mappingMethod2, ThreadBoundEventPreTickCallback.class.getName() + "#call", true, false, new InjectionParameterSpec[0]);
        this.k(mappingMethod, mappingMethod2, ThreadBoundEventPostTickCallback.class.getName() + "#call", false, false, new InjectionParameterSpec[0]);
    }
}

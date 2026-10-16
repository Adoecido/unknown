package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.mapping.EventInjectionSpec;
import vegas.sony.mapping.ItemStackTooltipCallback;
import vegas.sony.mapping.JavassistMappingTask;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.MappingMethod;
import vegas.sony.wrapper.impl.ForgeVersion;

public class ItemStackTooltipMappingTask
extends JavassistMappingTask {

    public ItemStackTooltipMappingTask() {
        super(MappedClasses.VK);
    }

    @Override
    public void transform() {
        MappingMethod mappingMethod = SonyVegas.INSTANCE.getMappings().q8.J;
        if (ForgeVersion.MC_1_17.d()) {
            JavassistMappingTask.p(ItemStackTooltipCallback.class);
            try {
                this.F(mappingMethod).insertAfter("{ $_ = ($r) " + ItemStackTooltipCallback.class.getName()
                        + "#call((java.lang.Object)$0, (java.util.List)$_); }");
                this.F(mappingMethod).getMethodInfo().rebuildStackMap(e);
            }
            catch (Exception exception) {
                throw new IllegalStateException("Could not rebuild ItemStack tooltip stack map", exception);
            }
        }
    }
}


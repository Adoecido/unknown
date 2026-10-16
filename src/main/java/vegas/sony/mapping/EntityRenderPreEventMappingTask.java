package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.mapping.EventPreRenderEntityCallback;
import vegas.sony.mapping.EventRenderPlayerPostCallback;
import vegas.sony.mapping.EventRenderPlayerPreCallback;
import vegas.sony.mapping.InjectionParameterSpec;
import vegas.sony.mapping.JavassistMappingTask;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.MappingMethod;
import vegas.sony.wrapper.impl.ForgeVersion;
import javassist.CtBehavior;

public class EntityRenderPreEventMappingTask
extends JavassistMappingTask {
    public EntityRenderPreEventMappingTask() {
        super(MappedClasses.Dc);
    }

    @Override
    public void transform() {
        if (ForgeVersion.MC_1_21_10.d()) {
            this.L();
            return;
        }
        MappingMethod mappingMethod = ForgeVersion.MC_1_21_6.d() ? SonyVegas.INSTANCE.getMappings().CA.r : SonyVegas.INSTANCE.getMappings().qe.p;
        MappingMethod mappingMethod2 = SonyVegas.INSTANCE.getMappings().CA.x;
        InjectionParameterSpec[] injectionParameterSpecArray = new InjectionParameterSpec[]{new InjectionParameterSpec(1, Object.class), new InjectionParameterSpec(6, Object.class), new InjectionParameterSpec(9, Object.class)};
        if (ForgeVersion.MC_1_21_0.H().y()) {
            injectionParameterSpecArray = new InjectionParameterSpec[]{new InjectionParameterSpec(1, Object.class), new InjectionParameterSpec(7, Object.class), new InjectionParameterSpec(0, Object.class)};
        }
        this.H(mappingMethod2, injectionParameterSpecArray);
        this.k(mappingMethod2, mappingMethod, EventRenderPlayerPreCallback.class.getName() + "#call", true, true, injectionParameterSpecArray);
        this.k(mappingMethod2, mappingMethod, EventRenderPlayerPostCallback.class.getName() + "#call", false, false, injectionParameterSpecArray);
        InjectionParameterSpec[] injectionParameterSpecArray2 = new InjectionParameterSpec[]{new InjectionParameterSpec(1, Object.class)};
        if (ForgeVersion.MC_1_21_6.d()) {
            injectionParameterSpecArray2 = new InjectionParameterSpec[]{new InjectionParameterSpec(1, Object.class), new InjectionParameterSpec(6, Object.class), new InjectionParameterSpec(5, Float.TYPE), new InjectionParameterSpec(9, Object.class), new InjectionParameterSpec(7, Object.class), new InjectionParameterSpec(8, Integer.TYPE)};
        }
        this.H(mappingMethod2, injectionParameterSpecArray2);
        if (ForgeVersion.MC_1_21_6.d()) {
            this.k(mappingMethod2, mappingMethod, EventPreRenderEntityCallback.class.getName() + "#call", true, false, injectionParameterSpecArray2);
        } else {
            this.T(mappingMethod2, mappingMethod, EventPreRenderEntityCallback.class.getName() + "#call", true, injectionParameterSpecArray2);
        }
    }

    private void L() {
        MappingMethod mappingMethod = SonyVegas.INSTANCE.getMappings().CA.j;
        CtBehavior ctBehavior = this.F(mappingMethod);
        try {
            ctBehavior.insertBefore("{" + EventPreRenderEntityCallback.class.getName() + "#call($1);}");
        }
        catch (Exception exception) {
            SonyVegas.logThrowable(exception);
        }
    }

}


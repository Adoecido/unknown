package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.event.impl.EventMotion;
import vegas.sony.event.impl.EventPostMotion;
import vegas.sony.event.impl.EventPreMotion;
import vegas.sony.runtime.NativeBridge;
import vegas.sony.wrapper.impl.ForgeVersion;
import javassist.CannotCompileException;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.NotFoundException;

public class EntityClientPlayerMPMotionMappingTask
extends JavassistMappingTask {
    private String y;
    private String q;
    private String u;
    private CtClass p;
    private String M;
    private String b;
    private MappingMethod J = null;

    public static String z(EntityClientPlayerMPMotionMappingTask entityClientPlayerMPMotionMappingTask) {
        return entityClientPlayerMPMotionMappingTask.u;
    }

    public static String Q(EntityClientPlayerMPMotionMappingTask entityClientPlayerMPMotionMappingTask) {
        return entityClientPlayerMPMotionMappingTask.b;
    }

    private static CannotCompileException a(CannotCompileException cannotCompileException) {
        return cannotCompileException;
    }

    private Object lambda$create$0() throws CannotCompileException, NotFoundException {
        this.s();
        return null;
    }

    public EntityClientPlayerMPMotionMappingTask(Class clazz) {
        super(clazz);
    }

    @Override
    public void transform() {
        this.J(this::lambda$create$0);
    }

    private void s() throws CannotCompileException, NotFoundException {
        String string;
        this.J = SonyVegas.INSTANCE.getMappings().q_.M;
        this.q = SonyVegas.INSTANCE.getMappings().RQ.n.getResolvedName();
        this.M = SonyVegas.INSTANCE.getMappings().RQ.n.getDescriptor();
        String string2 = ForgeVersion.MC_1_16_5.d() ? SonyVegas.INSTANCE.getMappings().Rr.jz.getResolvedName() : null;
        this.y = SonyVegas.INSTANCE.getMappings().Rr.jv.getResolvedName();
        this.u = SonyVegas.INSTANCE.getMappings().Rr.jT.getResolvedName();
        this.b = SonyVegas.INSTANCE.getMappings().Rr.U.getResolvedName();
        this.p = this.i(MappedClasses.uk);
        String string3 = EventMotion.class.getName();
        CtBehavior ctBehavior = this.F(this.J);
        this.c(this.J, EventPreMotion.class, "$0");
        this.k(this.J, EventPostMotion.class, "$0");
        boolean bl = SonyVegas.INSTANCE.isForgeAbsent();
        if (bl && NativeBridge.gc(string = "aaa") == null || ForgeVersion.MC_1_7_10.L() && SonyVegas.INSTANCE.isVanillaMinecraftPresent()) {
            bl = false;
        }
        boolean bl2 = bl;
        CtClass ctClass = this.i(MappedClasses.zc);
        ctBehavior.instrument(new EntityClientPlayerMPMotionExprEditor(this, string2, string3, bl2, ctClass));
    }

    public static String j(EntityClientPlayerMPMotionMappingTask entityClientPlayerMPMotionMappingTask) {
        return entityClientPlayerMPMotionMappingTask.y;
    }
}

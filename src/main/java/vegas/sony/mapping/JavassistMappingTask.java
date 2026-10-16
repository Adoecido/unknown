package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.asm.transform.ClassTransformer;
import vegas.sony.event.EventBus;
import vegas.sony.event.IEvent;
import vegas.sony.mapping.expr.MethodCallInjectionExprEditor;
import vegas.sony.mapping.expr.MethodCallLineCaptureExprEditor;
import vegas.sony.mapping.expr.MethodCallReplacementExprEditor;
import vegas.sony.runtime.ClassBytecodeCache;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.wrapper.impl.LaunchClassLoader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javassist.CannotCompileException;
import javassist.ClassPath;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.LoaderClassPath;
import javassist.NotFoundException;
import javassist.compiler.CompileError;
import javassist.expr.MethodCall;

public abstract class JavassistMappingTask
implements MappingTask {
    private CtClass Y;
    private static int T;
    protected Class E;
    static Set<Class> a;
    private boolean k;
    private static final MappingClassBytecodeResolver V;
    static int m;
    private boolean i;
    protected byte[] t;
    static ClassPool e;
    protected byte[] B;

    public void H(MappingMethod mappingMethod, InjectionParameterSpec ... injectionParameterSpecArray) {
        try {
            CtBehavior ctBehavior = this.F(mappingMethod);
            for (InjectionParameterSpec injectionParameterSpec : injectionParameterSpecArray) {
                String string = "v_param" + injectionParameterSpec.parameterIndex;
                ctBehavior.addLocalVariable(string, this.i(injectionParameterSpec.parameterType));
                ctBehavior.insertBefore("{ " + string + " = $" + injectionParameterSpec.parameterIndex + "; }");
            }
        }
        catch (Exception exception) {
            SonyVegas.logThrowable(exception);
        }
    }

    public String c(MappingMethod mappingMethod, Class clazz, String string) {
        return this.H(this.F(mappingMethod), clazz, string, "", "");
    }

    public String z(Class<?> clazz, String string, String string2, String string3, String string4) {
        boolean bl;
        boolean bl2;
        String string5 = "";
        string5 = string5 + clazz.getName() + " " + string + " = new " + clazz.getName() + "(" + string2 + ");\n";
        string5 = string5 + "if(" + string + "." + EventBus.getFireMethod(clazz).getName() + "()) { return " + string3 + "; } " + string4;
        int n = ClassTransformer.opaquePredicate();
        if (n != 0) {
            boolean bl3;
            boolean bl4;
            boolean bl5 = IEvent.class.isAssignableFrom(clazz);
            boolean bl6 = bl4 = bl5 && (bl3 = InsertedCallbackEventMarker.class.isAssignableFrom(clazz));
            if (bl4) {
                @SuppressWarnings("unchecked")
                Class<? extends IEvent> eventClass = (Class<? extends IEvent>)clazz;
                Method method = EventBus.findEventListenersAccessor(eventClass);
                string5 = "if(" + clazz.getName() + "#" + method.getName() + "()." + EventBus.getHasListenersMethod().getName() + "()) { " + string5 + "}";
            }
            String string6 = string5 = "{ " + string5 + " }";
            return string6;
        }
        boolean bl7 = bl2 = (bl = IEvent.class.isAssignableFrom(clazz));
        String string7 = string5;
        GuiComponent.setLegacyComponentState(new GuiComponent[4]);
        return string7;
    }

    public String I(MappingMethod mappingMethod, Class clazz, String string, String string2, String string3) {
        return this.m(this.F(mappingMethod), clazz, string, string2, string3);
    }

    @Override
    public abstract void transform();

    public static int O$src$I$5vfrz4() {
        return T;
    }

    public String m(CtBehavior ctBehavior, Class clazz, String string, String string2, String string3) {
        return this.P(ctBehavior, clazz, string, false, string2, string3);
    }

    @Override
    public void rollback() {
        byte[] byArray = this.t;
        Class clazz = this.E;
        int n = ClassBytecodeCache.setClassBytecode(clazz, byArray);
        int n2 = ClassTransformer.opaquePredicate();
        if (GuiComponent.getLegacyComponentState() == null) {
            ClassTransformer.setTransformerState(++n2);
        }
    }

    public static void q(ClassPath classPath, Class clazz) {
        a.add(clazz);
        e.insertClassPath(classPath);
    }

    /**
     * Lunar-only: pre-seed the shared pool with known-good live bytes for the
     * game classes every hook needs, before any transform runs. Lunar serves
     * notch originals for remapped resource paths, which poisons find()-time
     * lookups ("pk found in net/minecraft/entity/Entity.class") that bypass
     * get()/N() entirely. Inserting verified live bytes first shadows every
     * poison source for these classes. Skips anything inconsistent.
     */
    public static void preseedLunarGameClasses() {
        String[] names = {
            "net.minecraft.entity.Entity",
            "net.minecraft.entity.EntityLiving",
            "net.minecraft.entity.EntityLivingBase",
            "net.minecraft.entity.player.EntityPlayer",
            "net.minecraft.client.entity.EntityPlayerSP",
            "net.minecraft.client.entity.EntityClientPlayerMP",
            "net.minecraft.client.entity.EntityOtherPlayerMP",
            "net.minecraft.client.renderer.EntityRenderer",
            "net.minecraft.client.Minecraft",
            "net.minecraft.network.NetworkManager",
            "net.minecraft.client.multiplayer.PlayerControllerMP",
            "net.minecraft.client.gui.GuiPlayerTabOverlay",
            "net.minecraft.client.gui.GuiIngame",
            "net.minecraft.client.gui.GuiScreen",
            "net.minecraft.profiler.Profiler",
            "net.minecraft.world.storage.WorldInfo",
            "net.minecraft.client.renderer.ItemRenderer",
            "net.minecraft.item.ItemBucket",
            "net.minecraft.client.renderer.entity.RendererLivingEntity",
            "net.minecraft.client.renderer.entity.RenderManager",
            "net.minecraft.entity.item.EntityItem",
            "net.minecraft.item.ItemStack",
            "net.minecraft.client.gui.FontRenderer",
            "net.minecraft.client.gui.ScaledResolution",
            "net.minecraft.network.play.server.S38PacketPlayerListItem",
            "net.minecraft.scoreboard.ScoreObjective",
        };
        int seeded = 0;
        int skipped = 0;
        for (String name : names) {
            try {
                Class<?> live = vegas.sony.runtime.NativeBridge.gvc(name.replace('.', '/'));
                if (live == null || !name.equals(live.getName())) {
                    continue;
                }
                byte[] bytes = vegas.sony.runtime.ClassBytecodeCache.getClassBytecode(live, true);
                if (bytes == null) {
                    continue;
                }
                String internal = new javassist.bytecode.ClassFile(
                        new java.io.DataInputStream(new java.io.ByteArrayInputStream(bytes)))
                        .getName().replace('/', '.');
                if (!name.equals(internal)) {
                    vegas.sony.runtime.CheatBreakerDebugLog.info("HOOK_PRESEED skip " + name + " served=" + internal + " //$");
                    ++skipped;
                    continue;
                }
                e.insertClassPath(new javassist.ByteArrayClassPath(name, bytes));
                ++seeded;
            } catch (Throwable failure) {
                vegas.sony.runtime.CheatBreakerDebugLog.throwable("preseed " + name, failure);
            }
        }
        vegas.sony.runtime.CheatBreakerDebugLog.info("HOOK_PRESEED done seeded=" + seeded + " skipped=" + skipped + " //$");
    }

    private static Exception b(Exception exception) {
        return exception;
    }

    public void Y(String string) {
        int n = ClassTransformer.getTransformerState();
        try {
            File file = new File(string);
            boolean bl = file.getParentFile().exists();
            if (n == 0 && !bl) {
                bl = file.getParentFile().mkdirs();
            }
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(this.B);
            fileOutputStream.close();
        }
        catch (Exception exception) {
            SonyVegas.logThrowable(exception);
        }
    }

    private void dumpFailedCommit(int errorCode) {
        // Release build: do not persist transformed class diagnostics.
    }

    public String k(MappingMethod mappingMethod, Class clazz, String string) {
        return this.m(this.F(mappingMethod), clazz, string, "", "");
    }

    private <T> T J(MappingTaskCallable<T> mappingTaskCallable, Throwable throwable) {
        String string = throwable.getMessage();
        if (string.endsWith(": failed to resolve types")) {
            return this.J(mappingTaskCallable, this.h(throwable));
        }
        String[] stringArray = string.split(" ");
        String string2 = stringArray[stringArray.length - 1];
        return this.v(mappingTaskCallable, string2);
    }

    public CtBehavior F(MappingMethod mappingMethod) {
        try {
            if (mappingMethod.getOriginalName().equals("<init>")) {
                return this.Y.getConstructor(mappingMethod.getDescriptor());
            }
            return this.Y.getMethod(mappingMethod.getResolvedName(), mappingMethod.getDescriptor());
        }
        catch (NotFoundException notFoundException) {
            // Some launchers expose a package-private/bridge method that
            // CtClass#getMethod does not return even though it is present in
            // the class bytecode. Scan declared behaviors as a safe fallback.
            try {
                String resolvedName = mappingMethod.getResolvedName();
                String descriptor = mappingMethod.getDescriptor();
                for (CtBehavior behavior : this.Y.getDeclaredBehaviors()) {
                    if (resolvedName.equals(behavior.getName())
                            && descriptor.equals(behavior.getMethodInfo().getDescriptor())) {
                        return behavior;
                    }
                }
            }
            catch (Throwable fallbackFailure) {
                SonyVegas.logThrowable(fallbackFailure);
            }
            SonyVegas.logThrowable(notFoundException);
            return null;
        }
    }

    public static void p(Class clazz) {
        V.O(clazz);
        if (!SonyVegas.INSTANCE.isForgeAbsent()) {
            ClassBytecodeCache.getClassBytecode(clazz, true);
            String string = clazz.getName();
            LaunchClassLoader launchClassLoader = LaunchClassLoader.getLaunchClassLoader();
            if (launchClassLoader.supportsLegacyClassCache()) {
                Set set = launchClassLoader.getClassLoaderExceptions();
                set.remove(string);
                launchClassLoader.cachedClasses().put(string, clazz);
            }
            if (clazz.getSuperclass() != null && clazz.getSuperclass() != Object.class) {
                JavassistMappingTask.p(clazz.getSuperclass());
            }
        }
    }

    public static void X(int n) {
        T = n;
    }

    public String q(MappingMethod mappingMethod, Class clazz, String string, String string2, String string3) {
        return this.H(this.F(mappingMethod), clazz, string, string2, string3);
    }

    @Override
    public Class getTargetClass() {
        return this.E;
    }

    public CtClass x() {
        try {
            this.Y = e.makeClass(new ByteArrayInputStream(this.t), false);
            return this.Y;
        }
        catch (IOException iOException) {
            SonyVegas.logThrowable(iOException);
            return null;
        }
    }

    public void registerEventInjection(EventInjectionSpec eventInjectionSpec) {
        this.J(() -> this.insertEventInjectionCode(eventInjectionSpec));
    }

    public JavassistMappingTask(Class clazz) {
        this.E = clazz;
    }

    public String j(MappingMethod mappingMethod, Class clazz, String string, String string2) {
        return this.m(this.F(mappingMethod), clazz, string, string2, "");
    }

    private Throwable h(Throwable throwable) {
        Throwable throwable2 = throwable.getCause();
        if (throwable2 == null) {
            return throwable;
        }
        return this.h(throwable2);
    }

    public void E(MappingMethod mappingMethod) {
        try {
            CtClass ctClass = ClassPool.getDefault().get("java.lang.Exception");
            this.F(mappingMethod).addCatch("{ return; }", ctClass);
        }
        catch (Exception exception) {
            SonyVegas.logThrowable(exception);
        }
    }

    public void k(MappingMethod mappingMethod, MappingMethod mappingMethod2, String string, int n) {
        try {
            CtBehavior ctBehavior = this.F(mappingMethod);
            AtomicInteger atomicInteger = new AtomicInteger();
            ctBehavior.instrument(new MethodCallLineCaptureExprEditor(this, mappingMethod2, atomicInteger));
            ctBehavior.insertAt(atomicInteger.get() + n, string);
        }
        catch (Exception exception) {
            SonyVegas.logThrowable(exception);
        }
    }

    protected static boolean c(MethodCall methodCall, MappingMethod mappingMethod) {
        return methodCall.getMethodName().equals(mappingMethod.getResolvedName()) && methodCall.getSignature().equals(mappingMethod.getDescriptor()) && methodCall.getClassName().equals(mappingMethod.getOwnerClass().getName());
    }

    public static int U() {
        int n = JavassistMappingTask.O$src$I$5vfrz4();
        return 0;
    }

    private String lambda$makeAndInsertEvent$0(Class clazz, String string, CtBehavior ctBehavior, String string2, String string3, boolean bl) throws CannotCompileException, NotFoundException {
        JavassistMappingTask.p(clazz);
        String string4 = "";
        String string5 = string;
        string4 = "event" + ++m;
        string5 = string5.replace("$event", string4);
        ctBehavior.getDeclaringClass().defrost();
        ctBehavior.addLocalVariable(string4, this.i(clazz));
        String string6 = string5;
        String string7 = string3;
        String string8 = string2;
        String string9 = string4;
        Class clazz2 = clazz;
        JavassistMappingTask javassistMappingTask = this;
        String string10 = javassistMappingTask.z(clazz2, string9, string8, string7, string6);
        if (bl) {
            ctBehavior.insertBefore(string10);
        } else {
            ctBehavior.insertAfter(string10);
        }
        this.Y.defrost();
        return string4;
    }

    public String O(MappingMethod mappingMethod, Class clazz, String string, String string2) {
        return this.H(this.F(mappingMethod), clazz, string, string2, "");
    }

    @Override
    public void prepare() {
        if (this.E == null) {
            throw new IllegalStateException("Mapping task target class is null: " + this.getClass().getName());
        }
        this.t = ClassBytecodeCache.getClassBytecode(this.E, true);
        if (this.t == null || this.t.length == 0) {
            throw new IllegalStateException("Could not capture bytecode for " + this.E.getName());
        }
        V.O(this.E.getClassLoader());
        if (!SonyVegas.INSTANCE.isForgeAbsent()) {
            LaunchClassLoader launchClassLoader = LaunchClassLoader.getLaunchClassLoader();
            if (launchClassLoader.supportsLegacyClassCache()) {
                launchClassLoader.cachedClasses().put(this.E.getName(), this.E);
            }
        }
        if (this.x() == null) {
            throw new IllegalStateException("Javassist could not parse bytecode for " + this.E.getName());
        }
        this.Y.defrost();
        if (vegas.sony.runtime.NativeBridge.isLunar189Runtime()) {
            vegas.sony.runtime.CheatBreakerDebugLog.info("HOOK_LUNAR filter active for " + this.E.getName() + " //$");
            JavassistMappingTask.q(new LunarFilteredLoaderClassPath(this.E.getClassLoader()), this.E);
        } else {
            vegas.sony.runtime.CheatBreakerDebugLog.info("HOOK_LUNAR filter off for " + this.E.getName() + " //$");
            JavassistMappingTask.q(new LoaderClassPath(this.E.getClassLoader()), this.E);
        }
    }

    public void k(MappingMethod mappingMethod, MappingMethod mappingMethod2, String string, boolean bl, boolean bl2, InjectionParameterSpec ... injectionParameterSpecArray) {
        try {
            String string2;
            CtBehavior ctBehavior = this.F(mappingMethod);
            StringBuilder stringBuilder = new StringBuilder();
            if (bl2) {
                stringBuilder.append("if(");
            }
            stringBuilder.append(string + "(");
            for (int i = 0; i < injectionParameterSpecArray.length; ++i) {
                string2 = "v_param" + injectionParameterSpecArray[i].parameterIndex;
                if (i < injectionParameterSpecArray.length - 1) {
                    stringBuilder.append(string2 + ", ");
                    continue;
                }
                stringBuilder.append(string2);
            }
            if (bl2) {
                stringBuilder.append(")) { return; } ");
            } else {
                stringBuilder.append(");");
            }
            String string3 = stringBuilder.toString();
            string2 = bl ? "{" + string3 + " $_ = $proceed($$);}" : "{$_ = $proceed($$);" + string3 + "}";
            ctBehavior.instrument(new MethodCallInjectionExprEditor(this, mappingMethod2, string2));
        }
        catch (Exception exception) {
            SonyVegas.logThrowable(exception);
        }
    }

    static {
        JavassistMappingTask.X(122);
        V = new MappingClassBytecodeResolver();
        e = new BytecodeClassPool(V);
        a = new HashSet<Class>();
        m = 0;
    }

    public <T> T J(MappingTaskCallable<T> mappingTaskCallable) {
        return this.x(mappingTaskCallable, false);
    }

    @Override
    public int commit() {
        JavassistMappingTask javassistMappingTask;
        int n = ClassTransformer.opaquePredicate();
        if (n != 0) {
            int n2;
            JavassistMappingTask javassistMappingTask2 = this;
            if (javassistMappingTask2.i) {
                return -2;
            }
            JavassistMappingTask javassistMappingTask3 = this;
            if (javassistMappingTask3.B == null) {
                return 0;
            }
            Class clazz = this.E;
            byte[] byArray = this.B;
            int n3 = ClassBytecodeCache.setClassBytecode(clazz, byArray);
            if (n3 != 0) {
                this.dumpFailedCommit(n3);
            }
            if ((n2 = n3) == 0) {
                this.k = true;
            }
            return n3;
        }
        JavassistMappingTask javassistMappingTask4 = javassistMappingTask = this;
        Class clazz = javassistMappingTask4.E;
        byte[] byArray = this.B;
        int n4 = ClassBytecodeCache.setClassBytecode(clazz, byArray);
        if (n4 != 0) {
            this.dumpFailedCommit(n4);
        }
        int n5 = n4;
        return n5;
    }

    @Override
    public void serialize() {
        try {
            this.B = this.Y.toBytecode();
        }
        catch (Exception exception) {
            SonyVegas.logThrowable(exception);
        }
    }

    private <T> T x(MappingTaskCallable<T> mappingTaskCallable, boolean bl) {
        try {
            return mappingTaskCallable.c();
        }
        catch (CannotCompileException cannotCompileException) {
            if (bl) {
                this.i = true;
                throw new RuntimeException(cannotCompileException);
            }
            Throwable throwable = this.h(cannotCompileException);
            if (throwable instanceof CompileError || throwable instanceof NotFoundException) {
                return this.J(mappingTaskCallable, cannotCompileException);
            }
            this.i = true;
        }
        catch (NotFoundException notFoundException) {
            if (bl) {
                return null;
            }
            return this.J(mappingTaskCallable, notFoundException);
        }
        return null;
    }

    public void T(MappingMethod mappingMethod, MappingMethod mappingMethod2, String string, boolean bl, InjectionParameterSpec[] injectionParameterSpecArray) {
        try {
            CtBehavior ctBehavior = this.F(mappingMethod);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(string + "(");
            for (InjectionParameterSpec injectionParameterSpec : injectionParameterSpecArray) {
                String string2 = "v_param" + injectionParameterSpec.parameterIndex;
                stringBuilder.append(string2 + ", ");
            }
            stringBuilder.delete(stringBuilder.length() - 2, stringBuilder.length());
            stringBuilder.append(", $$");
            stringBuilder.append(");");
            String string3 = stringBuilder.toString();
            String string4 = bl ? "{" + string3 + " $_ = $proceed($$);}" : "{$_ = $proceed($$);" + string3 + "}";
            ctBehavior.instrument(new MethodCallReplacementExprEditor(this, mappingMethod2, string4));
        }
        catch (Exception exception) {
            SonyVegas.logThrowable(exception);
        }
    }

    public CtClass i(Class clazz) throws NotFoundException {
        return e.get(clazz.getName());
    }

    public void l(MappingMethod mappingMethod) { }

    public String P(CtBehavior ctBehavior, Class clazz, String string, boolean bl, String string2, String string3) {
        if (ctBehavior == null) {
            return null;
        }
        return this.J(() -> this.lambda$makeAndInsertEvent$0(clazz, string3, ctBehavior, string, string2, bl));
    }

    private Object insertEventInjectionCode(EventInjectionSpec eventInjectionSpec) throws CannotCompileException, NotFoundException {
        JavassistMappingTask.p(eventInjectionSpec.getEventClass());
        CtBehavior ctBehavior = this.F(eventInjectionSpec.getMappingMethod());
        if (ctBehavior == null) {
            // HOOK_ prefix: CheatBreakerDebugLog.info drops other prefixes,
            // which hid every silent mapping skip on Badlion 1.8.9 bring-up.
            vegas.sony.runtime.CheatBreakerDebugLog.info("HOOK_SKIP method-not-found owner="
                    + eventInjectionSpec.getMappingMethod().getOwnerClass()
                    + " name=" + eventInjectionSpec.getMappingMethod().getResolvedName()
                    + " sig=" + eventInjectionSpec.getMappingMethod().getDescriptor()
                    + " failed=" + eventInjectionSpec.getMappingMethod().hasResolutionFailed()
                    + " event=" + eventInjectionSpec.getEventClass().getSimpleName());
            SonyVegas.debugLog("Skipping event injection (method not found in bytecode): "
                    + eventInjectionSpec.getMappingMethod().getOriginalName());
            return null;
        }
        ctBehavior.getDeclaringClass().defrost();
        if (eventInjectionSpec.isInsertBefore()) {
            ctBehavior.insertBefore(eventInjectionSpec.buildInjectionCode());
        } else {
            ctBehavior.insertAfter(eventInjectionSpec.buildInjectionCode());
        }
        this.Y.defrost();
        return null;
    }

    public String H(CtBehavior ctBehavior, Class clazz, String string, String string2, String string3) {
        return this.P(ctBehavior, clazz, string, true, string2, string3);
    }

    public void z() {
    }

    private <T> T v(MappingTaskCallable<T> mappingTaskCallable, String string) {
        try {
            Class<?> clazz = Class.forName(string);
            if (a.contains(clazz)) {
                JavassistMappingTask.p(clazz);
                return this.x(mappingTaskCallable, true);
            }
            JavassistMappingTask.p(clazz);
            return this.J(mappingTaskCallable);
        }
        catch (ClassNotFoundException classNotFoundException) {
            return null;
        }
    }

    @Override
    public boolean isApplied() {
        return this.k;
    }
}

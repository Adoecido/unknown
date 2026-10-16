package vegas.sony.runtime;

import vegas.sony.SonyVegas;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.runtime.RuntimeNameMappingRegistry;
import vegas.sony.reflect.Badlion189Mappings;
import vegas.sony.reflect.CheatBreaker189Mappings;
import vegas.sony.reflect.Fabric12111Mappings;
import vegas.sony.reflect.Fabric262Mappings;
import vegas.sony.reflect.Type;
import vegas.sony.reflect.Vanilla1122Mappings;
import vegas.sony.reflect.Vanilla1206Mappings;
import vegas.sony.reflect.Vanilla12111Mappings;
import vegas.sony.reflect.Vanilla1710Mappings;
import vegas.sony.reflect.Vanilla189Mappings;
import vegas.sony.reflect.Vanilla262Mappings;
import vegas.sony.ui.click.GuiScreenNativeCallbackBridge;
import vegas.sony.utils.Base64Util;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Map;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

public class NativeBridge {
    private static final String DEFAULT_CONFIG_JSON = "{"
            + "\"friends\":[],"
            + "\"profiles\":{},"
            + "\"otherdata\":[{\"frames\":["
            + "{\"title\":\"Combat\",\"x\":32,\"y\":32,\"visible\":true,\"pinned\":false},"
            + "{\"title\":\"Render\",\"x\":144,\"y\":32,\"visible\":true,\"pinned\":false},"
            + "{\"title\":\"Utility\",\"x\":256,\"y\":32,\"visible\":true,\"pinned\":false},"
            + "{\"title\":\"World\",\"x\":368,\"y\":32,\"visible\":true,\"pinned\":false},"
            + "{\"title\":\"Inventory\",\"x\":480,\"y\":32,\"visible\":true,\"pinned\":false},"
            + "{\"title\":\"Favorites\",\"x\":592,\"y\":32,\"visible\":true,\"pinned\":false},"
            + "{\"title\":\"Settings\",\"x\":32,\"y\":32,\"visible\":false,\"pinned\":false},"
            + "{\"title\":\"ModuleSearch\",\"x\":32,\"y\":32,\"visible\":false,\"pinned\":false}"
            + "]}]} //$";
    private static boolean forgeAbsent = true;
    private static volatile int vanillaMappingVersion;
    private static volatile boolean badlion189Runtime;
    private static volatile boolean cheatBreaker189Runtime;
    private static volatile boolean lunar189Runtime;
    private static volatile boolean fabric12111Runtime;
    private static volatile boolean fabric262Runtime;
    private static volatile boolean moduleMenuVisible;
    private static boolean startInProgress;
    static boolean alphaTestWasEnabled;
    private static Method glGetFloatMethod;
    private static Method glGetIntegerVectorMethod;
    private static Method glVertexPointerMethod;
    private static Method glColorPointerMethod;
    private static Method glTexCoordPointerMethod;

    private static void resolveGlGetFloatMethod() {
        if (glGetFloatMethod != null) {
            return;
        }
        try {
            glGetFloatMethod = GL11.class.getMethod("glGetFloatv //$", Integer.TYPE, FloatBuffer.class);
        }
        catch (NoSuchMethodException modernNameMissing) {
            try {
                glGetFloatMethod = GL11.class.getMethod("glGetFloat //$", Integer.TYPE, FloatBuffer.class);
            }
            catch (NoSuchMethodException legacyNameMissing) {
                throw new IllegalStateException("Unable to resolve OpenGL float state method //$", legacyNameMissing);
            }
        }
    }

    private static void readGlFloats(int state, FloatBuffer destination) {
        NativeBridge.resolveGlGetFloatMethod();
        try {
            glGetFloatMethod.invoke(null, state, destination);
        }
        catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to read OpenGL state  //$" + state, exception);
        }
    }

    private static void readGlIntegers(int state, IntBuffer destination) {
        if (glGetIntegerVectorMethod == null) {
            try {
                glGetIntegerVectorMethod = GL11.class.getMethod(
                        "glGetIntegerv //$", Integer.TYPE, IntBuffer.class);
            }
            catch (NoSuchMethodException modernNameMissing) {
                try {
                    glGetIntegerVectorMethod = GL11.class.getMethod(
                            "glGetInteger //$", Integer.TYPE, IntBuffer.class);
                }
                catch (NoSuchMethodException legacyNameMissing) {
                    throw new IllegalStateException(
                            "Unable to resolve OpenGL integer state method //$", legacyNameMissing);
                }
            }
        }
        try {
            glGetIntegerVectorMethod.invoke(null, state, destination);
        }
        catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to read OpenGL integer state  //$" + state, exception);
        }
    }

    private static Method resolveFloatPointerMethod(String name) {
        try {
            return GL11.class.getMethod(
                    name, Integer.TYPE, Integer.TYPE, Integer.TYPE, FloatBuffer.class);
        }
        catch (NoSuchMethodException modernSignatureMissing) {
            try {
                return GL11.class.getMethod(
                        name, Integer.TYPE, Integer.TYPE, FloatBuffer.class);
            }
            catch (NoSuchMethodException legacySignatureMissing) {
                throw new IllegalStateException("Unable to resolve OpenGL pointer method  //$" + name,
                        legacySignatureMissing);
            }
        }
    }

    private static void invokeFloatPointer(Method method, int size, int stride,
                                           FloatBuffer buffer, String name) {
        try {
            if (method.getParameterCount() == 4) {
                method.invoke(null, size, 5126, stride, buffer);
            } else {
                method.invoke(null, size, stride, buffer);
            }
        }
        catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to invoke OpenGL pointer method  //$" + name, exception);
        }
    }

    public static void vertexPointer(int size, int stride, FloatBuffer buffer) {
        if (glVertexPointerMethod == null) {
            glVertexPointerMethod = NativeBridge.resolveFloatPointerMethod("glVertexPointer //$");
        }
        NativeBridge.invokeFloatPointer(glVertexPointerMethod, size, stride, buffer, "glVertexPointer //$");
    }

    public static void colorPointer(int size, int stride, FloatBuffer buffer) {
        if (glColorPointerMethod == null) {
            glColorPointerMethod = NativeBridge.resolveFloatPointerMethod("glColorPointer //$");
        }
        NativeBridge.invokeFloatPointer(glColorPointerMethod, size, stride, buffer, "glColorPointer //$");
    }

    public static void texCoordPointer(int size, int stride, FloatBuffer buffer) {
        if (glTexCoordPointerMethod == null) {
            glTexCoordPointerMethod = NativeBridge.resolveFloatPointerMethod("glTexCoordPointer //$");
        }
        NativeBridge.invokeFloatPointer(glTexCoordPointerMethod, size, stride, buffer, "glTexCoordPointer //$");
    }

    public static int ss_3(String value) {
        return 0;
    }

    public static int sts() {
        return 1;
    }

    public static void smdp(int mode, int value) {
        NativeBridge.smd(mode, value);
    }

    //GetRenderHandler
    //Java Layer Unused
    public static Object grh() {
        return null;
    }

    //MakeFont
    //Java Layer Unused
    public static int mf(int fontId, int style, String text) {
        return 0;
    }

    //Controller Exit
    public static void exit(boolean forced) {
        {}
    }

    //GetTexture
    //Java Layer Unused
    public static byte[] gt(String key) {
        return new byte[0];
    }

    //Disconnect
    //Disconnect dll with loader after finished loading
    public static void dc() {
    }

    //GetClassFields
    //Java Layer Unused
    public static String[] gcf(Class<?> targetClass) {
        if (targetClass == null) {
            return new String[0];
        }
        java.lang.reflect.Field[] fields = targetClass.getDeclaredFields();
        String[] names = new String[fields.length];
        for (int index = 0; index < fields.length; ++index) {
            names[index] = fields[index].getName();
        }
        return names;
    }

    public static int gts() {
        return 1;
    }

    public static boolean isForgeAbsent() {
        return forgeAbsent;
    }

    //Translate
    public static double[] trn(double worldX, double worldY, double worldZ) {
        FloatBuffer modelViewMatrix = BufferUtils.createFloatBuffer(16);
        FloatBuffer projectionMatrix = BufferUtils.createFloatBuffer(16);
        IntBuffer viewport = BufferUtils.createIntBuffer(16);
        NativeBridge.readGlFloats(2982, modelViewMatrix);
        NativeBridge.readGlFloats(2983, projectionMatrix);
        NativeBridge.readGlIntegers(2978, viewport);
        FloatBuffer screenPosition = BufferUtils.createFloatBuffer(3);
        GLU.gluProject((float)worldX, (float)worldY, (float)worldZ,
                modelViewMatrix, projectionMatrix, viewport, screenPosition);
        return new double[]{screenPosition.get(0), screenPosition.get(1), screenPosition.get(2)};
    }

    public static boolean gtcf(Object target, int index, int flags) {
        return false;
    }

    //GetKeyName
    public static native String gkn(long keyCode);

    //MessageBox
    public static void mb(int messageCode) {
        //can't understand why manthe would print error code instead of any meaningful text
    }

    //GetKeyState
    public static native short gks(int keyCode);

    //RenderState
    public static void rs(int phase, double width, double height) {
        GL11.glClear((int)256);
        GL11.glMatrixMode((int)5889);
        GL11.glLoadIdentity();
        GL11.glOrtho(0.0, width, height, 0.0, 1000.0, 3000.0);
        GL11.glMatrixMode((int)5888);
        GL11.glLoadIdentity();
        GL11.glTranslatef((float)0.0f, (float)0.0f, (float)-2000.0f);
        if (phase > 0) {
            if (alphaTestWasEnabled) {
                GL11.glEnable((int)3008);
            }
        } else {
            alphaTestWasEnabled = GL11.glIsEnabled((int)3008);
            if (alphaTestWasEnabled) {
                GL11.glDisable((int)3008);
            }
        }
    }

    //GetClass
    public static Class<?> gc(String internalName) {
        try {
            return Class.forName(internalName.replace("/", "."));
        }
        catch (Throwable throwable) {
            return null;
        }
    }

    //GetAccessToken
    public static native String gat();

    // Zeus relay port for this session (written by the injector through the
    // bootstrap mapping; no address text ever enters the Java heap).
    public static native int gzp();

    // One HTTPS account request through the loader's Windows transport.
    // The returned account must still pass LicenseVerifier before startup.
    public static native String agi() throws java.io.IOException;

    // Account API request with an already-validated relative path. The
    // transport joins it to the pinned control host natively; Java never
    // sees a URL. statusOut[0] receives the HTTP status (0 on transport
    // failure, which also yields a null return).
    public static native String acb(String method, String path, String body,
            int[] statusOut) throws java.io.IOException;

    // The control session remains native; Java only checks availability.
    public static native boolean rca();

    // Generic control API request (delegates to native WinHTTP implementation
    // to avoid materializing full URLs in Java heap).
    // Returns JSON response string or empty string on failure.
    public static native String cca(String action, String argumentsJson);

    // Validated display identity, without exposing the control session to Java.
    public static native String rid();

    // Shows or hides the loader window for the current injected session.
    public static native boolean slw(boolean visible);

    /** Compatibility callback retained for older native menu builds. */
    public static boolean ov(final boolean visible) {
        moduleMenuVisible = visible;
        if (!visible) vegas.sony.ui.hud.HudLayout.setEditing(false);
        return false;
    }

    public static boolean isModuleMenuVisible() {
        return moduleMenuVisible;
    }

    //GetClassObjects
    //Java Layer Unused
    //since I hate jvmti, so I'm not going to implement this
    public static Object[] gco(Class<?> targetClass) {
        return new Object[0];
    }

    //GetClassBytes
    public static native byte[] gcb(Class<?> targetClass);

    //GetLoadedClass. Uses JVMTI and therefore does not depend on a launcher's
    // context ClassLoader being able to rediscover an already loaded class.
    public static native Class<?> glc(String internalName);

    public static native void trs(int state);

    //CopyString
    //Java Layer Unused
    public static String cs(int stringId) {
        return "";
    }

    public static native byte[] gfb(String name);

    // Called by the memory bootstrap before start() moves to the render thread.
    public static void srl(ClassLoader loader) {
        PayloadResources.installResourceLoader(loader);
    }

    public static void scm(String sourceName, String mappedName) {
    }

    //GetClassJava
    public static Class<?> gcj(String descriptor) {
        try {
            return descriptor.startsWith("[")
                    ? Class.forName(descriptor.substring(2, descriptor.length() - 1).replace("/", "."))
                    : Class.forName(Type.getType(descriptor.substring(1, descriptor.length() - 1)).getClassName());
        }
        catch (Exception exception) {
            return null;
        }
    }

    //GetProfile
    public static String gp(String key) {
        if ("all".equals(key)) {
            return Base64Util.encodeUtf8Base64(DEFAULT_CONFIG_JSON);
        }
        return "";
    }

    public static void test() {
    }

    //GetStringHightV2
    //Java Layer Unused
    public static double gshv2(int fontId, String text) {
        return 0.0;
    }

    //GetClassSignature
    public static String gcs(Class<?> targetClass) {
        if (targetClass == null) {
            return "";
        }
        return Type.getDescriptor(targetClass);
    }

    //MapVirtualKey
    public static native int mvk(int virtualKey, int scanCode);

    //GetStringHeight
    //Java Layer Unused
    public static double gsh(int fontId, String text) {
        return 0.0;
    }

    public static synchronized void start() throws Throwable {
        if (startCompleted) {
            CheatBreakerDebugLog.info("NativeBridge.start skipped: already completed //$");
            return;
        }
        if (startInProgress) {
            return;
        }
        startInProgress = true;
        try {
            startOnce();
        }
        finally {
            startInProgress = false;
        }
    }

    private static void startOnce() throws Throwable {
        CheatBreakerDebugLog.initIfNeeded();
        CheatBreakerDebugLog.info("NativeBridge.start detection begin //$");
        forgeAbsent = !isClassPresent("net.minecraftforge.common.ForgeVersion //$")
                && !isClassPresent("net.minecraftforge.fml.loading.FMLLoader //$");
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader bridgeLoader = NativeBridge.class.getClassLoader();
        int detectedMappingVersion = detectVanillaMappingVersion(
                contextLoader, bridgeLoader);
        /* Native bootstrap sees Badlion's loaded classes before the Java
         * resource probe can reliably inspect LaunchClassLoader. Keep both
         * detectors in sync so initialization uses the Badlion frame hook. */
        if (NativeBridge.blr()) {
            badlion189Runtime = true;
            if (vanillaMappingVersion == 0) {
                vanillaMappingVersion = 15;
            }
            detectedMappingVersion = 15;
        }
        CheatBreakerDebugLog.info("NativeBridge.start detection result version="
                + detectedMappingVersion + " cb=" + cheatBreaker189Runtime
                + " badlion=" + badlion189Runtime + " forgeAbsent=" + forgeAbsent + " //$");
        {}
        if (cheatBreaker189Runtime) {
            CheatBreakerDebugLog.initIfNeeded();
            CheatBreakerDebugLog.info("NativeBridge.start begin (CheatBreaker detected) //$");
            {}
        } else if (badlion189Runtime) {
            {}
        } else if (vanillaMappingVersion == 15) {
            {}
        }
        SonyVegas sonyvegas = new SonyVegas();
        NativeBridge.invokeRequiredVoidInit(sonyvegas, "loadMappings //$");
        if (cheatBreaker189Runtime) {
            NativeBridge.registerCheatBreakerClassNames();
            {}
            CheatBreakerDebugLog.info("profile active; dumping critical classes //$");
            NativeBridge.dumpCheatBreakerCriticalClasses();
        } else if (badlion189Runtime) {
            {}
        }
        {}
        // Authorization is a hard precondition for enabling the mod. A missing
        // token, invalid response, timeout or unreachable API must stop before
        // any manager/callback registration, leaving the game itself untouched.
        if (!sonyvegas.initAccountInfo()) {
            NativeBridge.sce("START halted: " + sonyvegas.getAccountInfoFailureReason()
                    + "; Java " + System.getProperty("java.version", "unknown"));
            CheatBreakerDebugLog.info("START_HALTED reason=authorization_unavailable //$");
            return;
        }
        NativeBridge.invokeRequiredVoidInit(sonyvegas, "initializeManagers //$");
        startCompleted = true;
    }

    private static volatile boolean startCompleted;

    /** True only when a start() call ran initializeManagers to completion. */
    public static boolean isStartCompleted() {
        return startCompleted;
    }

    private static void invokeRequiredVoidInit(SonyVegas sonyvegas, String name)
            throws Throwable {
        for (Method method : SonyVegas.class.getDeclaredMethods()) {
            if (!method.getName().equals(name) || !method.getReturnType().equals(Void.TYPE)) continue;
            NativeBridge.sce("LOAD " + name);
            method.setAccessible(true);
            try {
                method.invoke(sonyvegas, new Object[0]);
                NativeBridge.sce("OK " + name);
            }
            catch (java.lang.reflect.InvocationTargetException wrapper) {
                Throwable cause = wrapper.getCause() != null
                        ? wrapper.getCause() : wrapper;
                NativeBridge.logThrowable(name, cause);
                CheatBreakerDebugLog.throwable("INIT " + name, cause);
                NativeBridge.sce("FAILED " + name + ": "
                        + cause.getClass().getName() + ": "
                        + String.valueOf(cause.getMessage()));
                throw cause;
            }
            catch (Throwable other) {
                NativeBridge.logThrowable(name, other);
                CheatBreakerDebugLog.throwable("INIT " + name, other);
                NativeBridge.sce("FAILED " + name + ": "
                        + other.getClass().getName() + ": "
                        + String.valueOf(other.getMessage()));
                throw other;
            }
            return;
        }
        NativeBridge.sce("MISSING void " + name + "()");
        throw new NoSuchMethodException(name + "()");
    }

    private static void logThrowable(String context, Throwable error) {
        // Runtime diagnostics are intentionally disabled in production.
    }

    //GetStringWidthV2
    //Java Layer Unused
    public static double gswv2(int fontId, String text) {
        return 0.0;
    }

    //DrawString
    //Java Layer Unused
    public static int ds(int fontId, String text, double x, double y, int color) {
        return 0;
    }

    //GetStringWidth
    //Java Layer Unused
    public static double gsw(int fontId, String text) {
        return 0.0;
    }

    //SetUsername
    //Not available under current recovery project
    public static void su(String username) {
    }

    //ClipboardCopy
    public static native void cpy(String text);

    public static long smpm(boolean pressed, long windowHandle, int button,
                            long cursorPosition, long extraInfo) {
        return 0L;
    }

    //Reload
    //Java Layer Unused
    public static void rl() {
    }

    //GetClassMethods
    //Java Layer Unused
    public static String[] gcm(Class<?> targetClass) {
        if (targetClass == null) {
            return new String[0];
        }
        Method[] methods = targetClass.getDeclaredMethods();
        String[] names = new String[methods.length];
        for (int index = 0; index < methods.length; ++index) {
            names[index] = methods[index].getName();
        }
        return names;
    }

    //GetKey
    //Java Layer Unused
    public static int gk() {
        return 0;
    }

    //SendMouseDown
    public static native void smd(int mode, int value);

    public static void rsc() {
    }

    //UpdateDiscord
    public static void updc(String serverDescription, String clientDescription) {
    }

    public static void fs() {
    }

    //DrawStringV2
    //Java Layer Unused
    public static native int dsv2(int fontId, String text, double x, double y,
                                  int color, float scale);

    //GetMinorVersion
    public static int gmv() {
        Throwable lastFailure = null;

        try {
            Object minorVersion = readStaticField(
                    "net.minecraftforge.common.ForgeVersion //$", "minorVersion //$");
            if (minorVersion instanceof Number) {
                return ((Number)minorVersion).intValue();
            }
            lastFailure = new IllegalStateException("ForgeVersion.minorVersion is not numeric //$");
        }
        catch (Throwable throwable) {
            lastFailure = throwable;
        }

        try {
            Object forgeVersion = readStaticField(
                    "net.minecraftforge.fml.loading.FMLLoader //$", "forgeVersion //$");
            int parsedVersion = parseForgeVersion(forgeVersion);
            if (parsedVersion >= 0) {
                return parsedVersion;
            }
            lastFailure = new IllegalStateException("FMLLoader.forgeVersion is not supported:  //$" + forgeVersion);
        }
        catch (Throwable throwable) {
            lastFailure = throwable;
        }

        // Newer Forge versions expose a version string instead of minorVersion.
        try {
            Object forgeVersion = readStaticField(
                    "net.minecraftforge.common.ForgeVersion //$", "forgeVersion //$");
            int parsedVersion = parseForgeVersion(forgeVersion);
            if (parsedVersion >= 0) {
                return parsedVersion;
            }
            lastFailure = new IllegalStateException("ForgeVersion.forgeVersion is not supported:  //$" + forgeVersion);
        }
        catch (Throwable throwable) {
            lastFailure = throwable;
        }

        try {
            Class<?> fmlLoader = Class.forName("net.minecraftforge.fml.loading.FMLLoader //$");
            Object versionInfo = fmlLoader.getMethod("versionInfo //$").invoke(null);
            Object forgeVersion = versionInfo.getClass().getMethod("forgeVersion //$").invoke(versionInfo);
            int parsedVersion = parseForgeVersion(forgeVersion);
            if (parsedVersion >= 0) {
                return parsedVersion;
            }
            lastFailure = new IllegalStateException(
                    "FMLLoader.versionInfo().forgeVersion() is not supported:  //$" + forgeVersion);
        }
        catch (Throwable throwable) {
            lastFailure = throwable;
        }

        // Vanilla-family fallback: 1.7.10 / 1.8.9 / 1.12.2 / 1.20.6 / 1.21.11 / 26.x
        // are resolved through runtime probe classes. Badlion and Lunar expose the same
        // vanilla obfuscated namespace; CheatBreaker (1.8.9) is handled by its own
        // Allatori mapping profile.
        try {
            int detectedVersion = detectVanillaMappingVersion(
                    Thread.currentThread().getContextClassLoader(),
                    NativeBridge.class.getClassLoader());
            if (detectedVersion != 0) {
                return detectedVersion;
            }
            lastFailure = new IllegalStateException("No supported Minecraft version detected //$");
        }
        catch (Throwable throwable) {
            lastFailure = throwable;
        }

        int versionFromRuntimeString = parseMinecraftVersionString(detectMinecraftVersionString());
        if (versionFromRuntimeString >= 0) {
            vanillaMappingVersion = versionFromRuntimeString;
            // The probe table can abstain (0/2+ matches) inside Genesis
            // loaders while the version string is unambiguous. Promote the
            // Lunar flag here too so the dedicated Lunar path engages.
            if (versionFromRuntimeString == 15 && !cheatBreaker189Runtime && !badlion189Runtime
                    && isLunar189Runtime(Thread.currentThread().getContextClassLoader(),
                        NativeBridge.class.getClassLoader())) {
                lunar189Runtime = true;
            }
            return versionFromRuntimeString;
        }
        if (isLunar189Runtime(Thread.currentThread().getContextClassLoader(),
                NativeBridge.class.getClassLoader())) {
            vanillaMappingVersion = 15;
            lunar189Runtime = true;
            return 15;
        }

        // Manual override for unsupported launchers, e.g. -Dsonyvegas.mappingVersion=15 for MC 1.8.9.
        try {
            String override = System.getProperty("IlIIlIlIIl");
            if (override == null) {
                override = System.getenv("IlIIlIlIIl");
            }
            if (override != null) {
                int parsedVersion = Integer.parseInt(override.trim());
                if (isKnownVersionId(parsedVersion)) {
                    return parsedVersion;
                }
            }
            lastFailure = new IllegalStateException(
                    "Invalid mapping override:  //$" + override);
        }
        catch (Throwable throwable) {
            lastFailure = throwable;
        }

        IllegalStateException failure = new IllegalStateException(
                "Unable to determine Forge minor version //$");
        if (lastFailure != null) {
            failure.initCause(lastFailure);
        }
        throw failure;
    }

    private static String detectMinecraftVersionString() {
        try {
            Object value = Class.forName("net.optifine.Config //$")
                    .getMethod("getVersion //$", new Class[0])
                    .invoke(null, new Object[0]);
            if (value != null) {
                return String.valueOf(value);
            }
        }
        catch (Throwable ignored) {
        }
        try {
            Object value = readStaticField("net.minecraft.client.Minecraft //$", "version //$");
            if (value != null) {
                return String.valueOf(value);
            }
        }
        catch (Throwable ignored) {
        }
        return null;
    }

    private static int parseMinecraftVersionString(String text) {
        if (text == null) {
            return -1;
        }
        String value = text.toLowerCase(java.util.Locale.ROOT);
        if (value.contains("1.7.10 //$")) return 13;
        if (value.contains("1.8.9 //$")) return 15;
        if (value.contains("1.12.2 //$")) return 23;
        if (value.contains("1.14.4 //$")) return 28;
        if (value.contains("1.16.5 //$")) return 35;
        if (value.contains("1.17 //$")) return 37;
        if (value.contains("1.20.6 //$")) return 50;
        if (value.contains("1.21.10 //$")) return 60;
        if (value.contains("1.21.11 //$")) return 61;
        if (value.contains("1.21.6 //$")) return 56;
        if (value.contains("1.21.5 //$")) return 55;
        if (value.contains("1.21.4 //$")) return 54;
        if (value.contains("1.21 //$")) return 51;
        if (value.contains("26.2 //$")) return 110;
        if (value.contains("26.1 //$")) return 100;
        return -1;
    }

    private static boolean isClassPresent(String className) {
        try {
            Class.forName(className, false, NativeBridge.class.getClassLoader());
            return true;
        }
        catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    private static boolean isLunar189Runtime(ClassLoader... loaders) {
        String[] markers = {
                "com/moonsworth/lunar/genesis/Genesis.class //$",
                "com/moonsworth/lunar/Genesis.class //$",
                "com/moonsworth/lunar/client/LunarClient.class //$",
                "com/moonsworth/lunar/agent/LunarAgent.class //$"
        };
        if (loaders == null) return false;
        for (ClassLoader loader : loaders) {
            if (loader == null) continue;
            for (String marker : markers) {
                try {
                    if (loader.getResource(marker) != null) return true;
                } catch (RuntimeException ignored) {}
            }
        }
        return false;
    }

    private static Object readStaticField(String className, String fieldName) throws Exception {
        Class<?> owner = Class.forName(className);
        Field field;
        try {
            field = owner.getField(fieldName);
        }
        catch (NoSuchFieldException ignored) {
            field = owner.getDeclaredField(fieldName);
        }
        field.setAccessible(true);
        return field.get(null);
    }

    private static int parseForgeVersion(Object value) {
        if (value == null) {
            return -1;
        }
        String text = String.valueOf(value).trim();
        int start = 0;
        while (start < text.length() && !Character.isDigit(text.charAt(start))) {
            ++start;
        }
        int end = start;
        while (end < text.length() && Character.isDigit(text.charAt(end))) {
            ++end;
        }
        if (start == end) {
            return -1;
        }
        try {
            int parsed = Integer.parseInt(text.substring(start, end));
            return isKnownVersionId(parsed) ? parsed : -1;
        }
        catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static boolean isKnownVersionId(int version) {
        switch (version) {
            case 13: case 15: case 23: case 28: case 35: case 36: case 37:
            case 50: case 51: case 54: case 55: case 56: case 60: case 61:
            case 100: case 110:
                return true;
            default:
                return false;
        }
    }

    public static native int ss_2(String value);

    /** Current HUD drag coordinates, copied directly from the embedded editor. */
    public static native int hps(float[] positions);

    public static String sp(String key, String value) {
        return null;
    }

    public static void reload() {
    }

    public static void printLog(String message) {
        {}
    }

    //SetClassBytes
    public static native int scb(Class<?> targetClass, byte[] bytecode);

    //MakeFontV2
    //Java Layer Unused
    public static native int mfv2(int fontId, int style, String text);

    // Menu bootstrap: binary session descriptor (port, masked session root,
    // CSRF token, route aliases and GUI bind). No URL text crosses this
    // boundary in either direction; the payload is wiped by the caller.
    public static native void ss(byte[] payload);

    public static boolean[] gls() {
        return new boolean[0];
    }

    //GetVanillaClas
    public static Class<?> gvc(String internalName) {
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader bridgeLoader = NativeBridge.class.getClassLoader();
        int mappingVersion = detectVanillaMappingVersion(
                contextLoader, bridgeLoader);
        if (mappingVersion == 13) {
            return Vanilla1710Mappings.resolveClass(
                    internalName, contextLoader, bridgeLoader);
        }
        if (mappingVersion == 15) {
            if (cheatBreaker189Runtime) {
                return CheatBreaker189Mappings.resolveClass(
                        internalName, contextLoader, bridgeLoader);
            }
            return Vanilla189Mappings.resolveClass(
                    internalName, contextLoader, bridgeLoader);
        }
        if (mappingVersion == 23) {
            return Vanilla1122Mappings.resolveClass(
                    internalName, contextLoader, bridgeLoader);
        }
        if (mappingVersion == 50) {
            return Vanilla1206Mappings.resolveClass(
                    internalName, contextLoader, bridgeLoader);
        }
        if (mappingVersion == 61) {
            if (fabric12111Runtime) {
                return Fabric12111Mappings.resolveClass(
                        internalName, contextLoader, bridgeLoader);
            }
            return Vanilla12111Mappings.resolveClass(
                    internalName, contextLoader, bridgeLoader);
        }
        if (mappingVersion == 110) {
            if (fabric262Runtime) {
                return Fabric262Mappings.resolveClass(
                        internalName, contextLoader, bridgeLoader);
            }
            return Vanilla262Mappings.resolveClass(
                    internalName, contextLoader, bridgeLoader);
        }
        return gc(internalName);
    }

    public static boolean isBadlion189Runtime() {
        return badlion189Runtime;
    }

    /**
     * Lunar Client 1.8.9 (vanilla/Ichor, no Forge). Separate from the
     * CheatBreaker/Badlion/vanilla paths: Lunar's loaders serve notch
     * originals for remapped resource paths, so mapping tasks use a
     * filtered class path there. Never enabled alongside the CB profile.
     *
     * <p>Self-healing: Ichor finishes wiring game loaders after bootstrap,
     * so a fresh marker probe latches the flag the first time Lunar becomes
     * visible, even if early detection ran with incomplete loaders.
     */
    public static boolean isLunar189Runtime() {
        if (!lunar189Runtime) {
            try {
                if (isLunar189Runtime(Thread.currentThread().getContextClassLoader(),
                        NativeBridge.class.getClassLoader())) {
                    lunar189Runtime = true;
                }
            } catch (Throwable ignored) {
            }
        }
        return lunar189Runtime;
    }

    public static boolean isCheatBreaker189Runtime() {
        return cheatBreaker189Runtime;
    }

    public static boolean isFabric12111Runtime() {
        return fabric12111Runtime;
    }

    public static boolean isFabric262Runtime() {
        return fabric262Runtime;
    }

    private static int detectVanillaMappingVersion(
            ClassLoader... preferredLoaders) {
        int detectedVersion = vanillaMappingVersion;
        if (detectedVersion != 0) {
            return detectedVersion;
        }
        // CheatBreaker Allatori Minecraft must win over Badlion/vanilla notch probes.
        // Probe it first: legacy named-runtime fallbacks may try to load an SRG
        // resource that is irrelevant (and unavailable) inside the CB class loader.
        boolean cheatBreaker189 = probeRuntime(1, "cheatbreaker189 //$", preferredLoaders);
        if (cheatBreaker189) {
            cheatBreaker189Runtime = true;
            badlion189Runtime = false;
            lunar189Runtime = false;
            fabric12111Runtime = false;
            fabric262Runtime = false;
            vanillaMappingVersion = 15;
            CheatBreakerDebugLog.info(
                    "VERSION_PROBES cb=true; forcing CheatBreaker 1.8.9 //$");
            return 15;
        }
        boolean vanilla1710 = probeRuntime(2, "vanilla1710 //$", preferredLoaders);
        boolean badlion189 = probeRuntime(3, "badlion189 //$", preferredLoaders);
        boolean vanilla189 = cheatBreaker189
                || badlion189
                || probeRuntime(4, "vanilla189 //$", preferredLoaders);
        boolean vanilla1122 = probeRuntime(5, "vanilla1122 //$", preferredLoaders);
        boolean vanilla1206 = probeRuntime(6, "vanilla1206 //$", preferredLoaders);
        boolean vanilla12111 = probeRuntime(7, "vanilla12111 //$", preferredLoaders);
        boolean fabric12111 = probeRuntime(8, "fabric12111 //$", preferredLoaders);
        boolean vanilla262 = probeRuntime(9, "vanilla262 //$", preferredLoaders);
        boolean fabric262 = probeRuntime(10, "fabric262 //$", preferredLoaders);
        int matchingVersions = (vanilla1710 ? 1 : 0)
                + (vanilla189 ? 1 : 0) + (vanilla1122 ? 1 : 0)
                + (vanilla1206 ? 1 : 0)
                + (vanilla12111 || fabric12111 ? 1 : 0)
                + (vanilla262 || fabric262 ? 1 : 0);
        CheatBreakerDebugLog.info("VERSION_PROBES cb=false vanilla1710=" + vanilla1710
                + " vanilla189=" + vanilla189 + " badlion189=" + badlion189
                + " vanilla1122=" + vanilla1122 + " vanilla1206=" + vanilla1206
                + " vanilla12111=" + vanilla12111 + " fabric12111=" + fabric12111
                + " vanilla262=" + vanilla262 + " fabric262=" + fabric262
                + " matches=" + matchingVersions + " //$");
        if (matchingVersions == 1) {
            cheatBreaker189Runtime = cheatBreaker189;
            badlion189Runtime = badlion189;
            fabric12111Runtime = fabric12111;
            fabric262Runtime = fabric262;
            lunar189Runtime = !cheatBreaker189 && !badlion189
                    && isLunar189Runtime(preferredLoaders);
            if (vanilla1710) {
                detectedVersion = 13;
            } else if (vanilla189) {
                detectedVersion = 15;
            } else if (vanilla1122) {
                detectedVersion = 23;
            } else if (vanilla1206) {
                detectedVersion = 50;
            } else if (vanilla12111 || fabric12111) {
                detectedVersion = 61;
            } else {
                detectedVersion = 110;
            }
        }
        if (detectedVersion != 0) {
            vanillaMappingVersion = detectedVersion;
        }
        return detectedVersion;
    }

    private static boolean probeRuntime(int probe, String name,
            ClassLoader... preferredLoaders) {
        try {
            switch (probe) {
                case 1:
                    return CheatBreaker189Mappings.isRuntimePresent(preferredLoaders);
                case 2:
                    return Vanilla1710Mappings.isRuntimePresent(preferredLoaders);
                case 3:
                    return Badlion189Mappings.isRuntimePresent(preferredLoaders);
                case 4:
                    return Vanilla189Mappings.isRuntimePresent(preferredLoaders);
                case 5:
                    return Vanilla1122Mappings.isRuntimePresent(preferredLoaders);
                case 6:
                    return Vanilla1206Mappings.isRuntimePresent(preferredLoaders);
                case 7:
                    return Vanilla12111Mappings.isRuntimePresent(preferredLoaders);
                case 8:
                    return Fabric12111Mappings.isRuntimePresent(preferredLoaders);
                case 9:
                    return Vanilla262Mappings.isRuntimePresent(preferredLoaders);
                case 10:
                    return Fabric262Mappings.isRuntimePresent(preferredLoaders);
                default:
                    return false;
            }
        }
        catch (Throwable failure) {
            CheatBreakerDebugLog.throwable("VERSION_PROBE_FAILED " + name, failure);
            return false;
        }
    }

    private static void registerCheatBreakerClassNames() {
        for (Map.Entry<String, String> entry
                : CheatBreaker189Mappings.getClassNameMap().entrySet()) {
            String source = entry.getKey();
            if (source.indexOf('/') < 0 && source.indexOf('.') < 0) {
                continue;
            }
            RuntimeNameMappingRegistry.registerClassName(source, entry.getValue());
        }
    }

    private static void dumpCheatBreakerCriticalClasses() {
        String[] critical = {
                "net/minecraft/client/Minecraft //$",
                "net/minecraft/entity/Entity //$",
                "net/minecraft/entity/EntityLivingBase //$",
                "net/minecraft/entity/player/EntityPlayer //$",
                "net/minecraft/client/entity/AbstractClientPlayer //$",
                "net/minecraft/client/entity/EntityPlayerSP //$",
                "net/minecraft/client/entity/EntityClientPlayerMP //$",
                "net/minecraft/client/entity/EntityOtherPlayerMP //$",
                "net/minecraft/world/World //$",
                "net/minecraft/client/multiplayer/WorldClient //$",
                "net/minecraft/world/WorldClient$Shell //$",
                "net/minecraft/util/EnumFacing //$",
                "net/minecraft/util/BlockPos //$",
                "net/minecraft/util/Vec3 //$",
                "net/minecraft/client/settings/GameSettings //$",
                "net/minecraft/network/play/client/C03PacketPlayer //$",
                "net/minecraft/network/play/client/C03PacketPlayer$C05PacketPlayerLook //$",
                "net/minecraft/network/play/client/C02PacketUseEntity //$"
        };
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader bridgeLoader = NativeBridge.class.getClassLoader();
        for (String name : critical) {
            Class<?> resolved = CheatBreaker189Mappings.resolveClass(
                    name, contextLoader, bridgeLoader);
            CheatBreakerDebugLog.missingClass(name, resolved);
        }
        CheatBreakerDebugLog.info("MappedClasses snapshot //$"
                + " Minecraft= //$" + MappedClasses.uP
                + " EntityPlayerSP= //$" + MappedClasses.z5
                + " EntityClientPlayerMP= //$" + MappedClasses.DI
                + " World= //$" + MappedClasses.YU
                + " WorldClient= //$" + MappedClasses.Z
                + " EnumFacing= //$" + MappedClasses.q0
                + " OtherPlayerMP= //$" + MappedClasses.lG);
        if (CheatBreakerDebugLog.getLogFile() != null) {
            {}
        }
    }

    //SendClientError
    public static native void sce(String message);

    /** Returns the Badlion profile detected by the native bootstrap. */
    public static native boolean blr();

    public static native Object inv(Method method, Object target, Object ... arguments);

    // Both of these are entered from native code inside Display.update, so an
    // exception here does not land in a handler - it unwinds straight through
    // LWJGL into the game loop and takes the client down. An unresolved mapping
    // is worth a dead feature, not a crash.
    public static boolean om(int eventId, long firstArgument, long secondArgument) {
        try {
            SonyVegas.runPendingClientFrameInit();
            return GuiScreenNativeCallbackBridge.onNotification(eventId, firstArgument, secondArgument);
        }
        catch (Throwable failure) {
            logThrowable("input callback  //$" + eventId, failure);
            return false;
        }
    }

    public static void wh(long windowHandle) {
        try {
            GuiScreenNativeCallbackBridge.h(windowHandle);
        }
        catch (Throwable failure) {
            logThrowable("window callback //$", failure);
        }
    }
}

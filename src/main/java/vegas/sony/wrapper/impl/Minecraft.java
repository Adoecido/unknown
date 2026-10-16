package vegas.sony.wrapper.impl;

import vegas.sony.SonyVegas;
import vegas.sony.account.MinecraftSessionWrapper;
import vegas.sony.mapping.mappings.MMinecraft;
import vegas.sony.wrapper.Wrapper;

import java.util.Map;

public class Minecraft
extends Wrapper {
    private static Object L;
    private static boolean s;

    public static void J(boolean bl) {
        if (ForgeVersion.MC_1_16_5.d()) {
            Minecraft.p().D(bl);
            return;
        }
        MMinecraft.t(Minecraft.sonyVegasInstance.getMappings().U, Minecraft.i(), bl);
    }

    public static NetworkManager x$src$Lvegas_sony_wrapper_impl_NetworkManager_$1sglv7v() {
        return new NetworkManager(MMinecraft.w(SonyVegas.INSTANCE.getMappings().U, Minecraft.i()));
    }

    public static boolean V() {
        return MMinecraft.I(Minecraft.sonyVegasInstance.getMappings().U, Minecraft.i());
    }

    public static int w() {
        return MMinecraft.y(Minecraft.sonyVegasInstance.getMappings().U, Minecraft.i());
    }

    public static void b() {
        if (ForgeVersion.MC_1_16_5.d()) {
            Minecraft.p().Q();
            return;
        }
        MMinecraft.R(Minecraft.sonyVegasInstance.getMappings().U, Minecraft.i());
    }

    static {
        if (!Minecraft.g()) {
            Minecraft.e(true);
        }
    }

    public static void r(int n) {
        MMinecraft.x(Minecraft.sonyVegasInstance.getMappings().U, Minecraft.i(), n);
    }

    public static void stopBlockBreaking() {
        Minecraft.sonyVegasInstance.getMappings().U.Of.invokeVoid(Minecraft.i(), false);
    }

    public static Map P() {
        return Minecraft.sonyVegasInstance.getMappings().U.h$src$Ljava_util_Map_$8i5rvc(Minecraft.i());
    }

    public static RenderManager D() {
        return ForgeVersion.MC_1_7_10.L() ? RenderManager.getInstance() : new RenderManager(Minecraft.sonyVegasInstance.getMappings().U.w(Minecraft.i()));
    }

    public static GuiSpriteManager T() {
        return new GuiSpriteManager(MMinecraft.p(Minecraft.sonyVegasInstance.getMappings().U, Minecraft.i()));
    }

    public static boolean l$src$Z$b9uwii() {
        boolean bl = Minecraft.g();
        return false;
    }

    public static int h() {
        if (ForgeVersion.MC_1_16_5.d()) {
            return Minecraft.p().R();
        }
        return Minecraft.sonyVegasInstance.getMappings().U.R(Minecraft.i());
    }

    public static void O(boolean bl) {
        MMinecraft.u(Minecraft.sonyVegasInstance.getMappings().U, Minecraft.i(), bl);
    }

    public static TextureManagerHandle getMainRenderTarget() {
        if (!ForgeVersion.MC_1_21_10.d()) {
            return null;
        }
        return new TextureManagerHandle(Minecraft.sonyVegasInstance.getMappings().U.V(Minecraft.i()));
    }

    public static boolean a() {
        return Minecraft.sonyVegasInstance.getMappings().U.f(Minecraft.i());
    }

    public static EffectRenderer z() {
        return new EffectRenderer(MMinecraft.v(Minecraft.sonyVegasInstance.getMappings().U, Minecraft.i()));
    }

    public static EntityLivingBase F() {
        return new EntityLivingBase(Minecraft.sonyVegasInstance.getMappings().U.h(Minecraft.i()));
    }

    public static ModelManager x() {
        return new ModelManager(MMinecraft.n(Minecraft.sonyVegasInstance.getMappings().U, Minecraft.i()));
    }

    public static boolean F$src$Z$aoypys() {
        if (ForgeVersion.MC_1_16_5.d()) {
            return Minecraft.p().x();
        }
        return MMinecraft.f(Minecraft.sonyVegasInstance.getMappings().U, Minecraft.i());
    }

    public static VoxelShape H$src$Lvegas_sony_wrapper_impl_VoxelShape_$1dlcquv() {
        return new VoxelShape(MMinecraft.x(Minecraft.sonyVegasInstance.getMappings().U, Minecraft.i()));
    }

    public static FontRenderer getFontRenderer() {
        return new FontRenderer(Minecraft.sonyVegasInstance.getMappings().U.J(Minecraft.i()));
    }

    public static ScaledResolution G() {
        return new ScaledResolution();
    }

    public static PlayerControllerMP playerController() {
        return new PlayerControllerMP(Minecraft.sonyVegasInstance.getMappings().U.r(Minecraft.i()));
    }

    public static int Q() {
        return MMinecraft.S(Minecraft.sonyVegasInstance.getMappings().U, Minecraft.i());
    }

    public static Framebuffer getFrameBuffer() {
        return new Framebuffer(Minecraft.sonyVegasInstance.getMappings().U.u(Minecraft.i()));
    }

    public static TextureManagerBridge M() {
        return new TextureManagerBridge(Minecraft.sonyVegasInstance.getMappings().U.P(Minecraft.i()));
    }

    public static Entity Y() {
        return new Entity(Minecraft.sonyVegasInstance.getMappings().U.F(Minecraft.i()));
    }

    public static void e(boolean bl) {
        s = bl;
    }

    public static FontManager q() {
        if (!ForgeVersion.MC_1_21_10.d()) {
            return null;
        }
        Object object = Minecraft.sonyVegasInstance.getMappings().U.j(Minecraft.i());
        return object != null ? new FontManager(object) : null;
    }

    public static int J() {
        if (ForgeVersion.MC_1_16_5.d()) {
            return Minecraft.p().I();
        }
        return Minecraft.sonyVegasInstance.getMappings().U.Y(Minecraft.i());
    }

    public static boolean m$src$Z$baep3v() {
        return Minecraft.sonyVegasInstance.getMappings().U.X$src$Z$1ecebix(Minecraft.i());
    }

    public static Object u() {
        return Minecraft.sonyVegasInstance.getMappings().U.R$src$Ljava_lang_Object_$11ec019(Minecraft.i());
    }

    public static RenderItem v() {
        if (ForgeVersion.MC_1_7_10.L()) {
            return RenderItem.d();
        }
        return new RenderItem(MMinecraft.k(Minecraft.sonyVegasInstance.getMappings().U, Minecraft.i()));
    }

    public static ResourceManager getResourcePackRepository() {
        return new ResourceManager(Minecraft.sonyVegasInstance.getMappings().U.X(Minecraft.i()));
    }

    public static Timer getTimer() {
        return new Timer(MMinecraft.W(Minecraft.sonyVegasInstance.getMappings().U, Minecraft.i()));
    }

    public static KeyboardHandler r() {
        return new KeyboardHandler(MMinecraft.o(Minecraft.sonyVegasInstance.getMappings().U, Minecraft.i()));
    }

    public static GameSettings gameSettings() {
        return new GameSettings(Minecraft.sonyVegasInstance.getMappings().U.b(Minecraft.i()));
    }

    public static MouseHandler s() {
        return new MouseHandler(MMinecraft.X(Minecraft.sonyVegasInstance.getMappings().U, Minecraft.i()));
    }

    public static NetHandlerPlayClientImpl N() {
        return new NetHandlerPlayClientImpl(Minecraft.sonyVegasInstance.getMappings().U.a(Minecraft.i()));
    }

    public static EntityPlayerSP thePlayer() {
        return new EntityPlayerSP(SonyVegas.INSTANCE.getMappings().U.T(Minecraft.i()));
    }

    public static int l() {
        return Minecraft.sonyVegasInstance.getMappings().U.L();
    }

    public static boolean g() {
        return s;
    }

    public static void B(GuiScreen guiScreen) {
        Minecraft.sonyVegasInstance.getMappings().U.r(Minecraft.i(), guiScreen.getObject());
    }

    public static void E(int n) {
        MMinecraft.v(Minecraft.sonyVegasInstance.getMappings().U, Minecraft.i(), n);
    }

    public static void W(Entity entity) {
        Minecraft.sonyVegasInstance.getMappings().U.a(Minecraft.i(), entity.getObject());
    }

    public static TitledScreen k() {
        return new TitledScreen(Minecraft.sonyVegasInstance.getMappings().U.A(Minecraft.i()));
    }

    public static RenderTypeBuffer getFramerateLimitTracker() {
        return new RenderTypeBuffer(Minecraft.sonyVegasInstance.getMappings().U.S(Minecraft.i()));
    }

    public Minecraft() {
        super(SonyVegas.INSTANCE.getMappings().U.J());
    }

    public static void w(MinecraftSessionWrapper minecraftSessionWrapper) {
        MMinecraft.p(Minecraft.sonyVegasInstance.getMappings().U, Minecraft.i(), minecraftSessionWrapper.getObject());
    }

    public static MinecraftSessionWrapper Q$src$Lvegas_sony_account_MinecraftSessionWrapper_$1ftnn3u() {
        return new MinecraftSessionWrapper(MMinecraft.G(Minecraft.sonyVegasInstance.getMappings().U, Minecraft.i()));
    }

    public static Object i() {
        if (L == null) {
            L = Minecraft.sonyVegasInstance.getMappings().U.J();
        }
        return L;
    }

    public static void S() {
        MMinecraft.Y(Minecraft.sonyVegasInstance.getMappings().U, Minecraft.i());
    }

    public static TextureManager getTextureManager() {
        return new TextureManager(Minecraft.sonyVegasInstance.getMappings().U.c(Minecraft.i()));
    }

    public static void R() {
        MMinecraft.b(Minecraft.sonyVegasInstance.getMappings().U, Minecraft.i());
    }

    public static void r(EntityLivingBase entityLivingBase) {
        Minecraft.sonyVegasInstance.getMappings().U.G(Minecraft.i(), entityLivingBase == null ? null : entityLivingBase.getObject());
    }


    public static void currentScreen(Object object) {
        Minecraft.sonyVegasInstance.getMappings().U.Y(Minecraft.i(), object);
    }

    public static RayTraceResult p$src$Lvegas_sony_wrapper_impl_RayTraceResult_$5rw6n0() {
        return new RayTraceResult(Minecraft.sonyVegasInstance.getMappings().U.q(Minecraft.i()));
    }

    public static MouseHelper p() {
        return new MouseHelper(MMinecraft.y$src$Ljava_lang_Object_$1igycos(Minecraft.sonyVegasInstance.getMappings().U, Minecraft.i()));
    }

    public static EntityRenderer m$src$Lvegas_sony_wrapper_impl_EntityRenderer_$13begmf() {
        return new EntityRenderer(Minecraft.sonyVegasInstance.getMappings().U.m(Minecraft.i()));
    }

    public static void v(Runnable runnable) {
        MMinecraft.m(Minecraft.sonyVegasInstance.getMappings().U).invokeVoid(Minecraft.i(), runnable);
    }

    public static ServerData H() {
        return new ServerData(Minecraft.sonyVegasInstance.getMappings().U.l(Minecraft.i()));
    }

    public static WorldClient theWorld() {
        return new WorldClient(Minecraft.sonyVegasInstance.getMappings().U.E(Minecraft.i()));
    }

    public static RenderGlobal O() {
        return new RenderGlobal(MMinecraft.N(Minecraft.sonyVegasInstance.getMappings().U, Minecraft.i()));
    }

    public static void F$src$V$aoypvc() {
        Minecraft.sonyVegasInstance.getMappings().U.V$src$V$9672l7(Minecraft.i());
        if (ForgeVersion.MC_1_16_5.d()) {
            Minecraft.s().u();
        }
    }

    public static void X(int n) {
        if (ForgeVersion.MC_1_16_5.d()) {
            Minecraft.p().a(n);
            return;
        }
        Minecraft.sonyVegasInstance.getMappings().U.B(Minecraft.i(), n);
    }

    public static void O(RayTraceResult rayTraceResult) {
        Minecraft.sonyVegasInstance.getMappings().U.P(Minecraft.i(), rayTraceResult.getObject());
    }

    public static void U(int n) {
        if (ForgeVersion.MC_1_16_5.d()) {
            Minecraft.p().H(n);
            return;
        }
        Minecraft.sonyVegasInstance.getMappings().U.R(Minecraft.i(), n);
    }

    public static GuiScreen currentScreen() {
        return new GuiScreen(Minecraft.sonyVegasInstance.getMappings().U.U(Minecraft.i()));
    }

    public static Timer a_jo_2_T() {
        return Minecraft.getTimer();
    }

    public static EntityPlayerSP a_xH_J() {
        return Minecraft.thePlayer();
    }

    public static GuiScreen a_pt_1_w() {
        return Minecraft.currentScreen();
    }

    public static GameSettings a_w3_0_S() {
        return Minecraft.gameSettings();
    }
}

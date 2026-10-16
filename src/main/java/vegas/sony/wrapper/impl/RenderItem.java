package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MRenderItem;
import vegas.sony.utils.render.BufferedGuiRenderPrimitives;
import vegas.sony.wrapper.Wrapper;


public class RenderItem
extends Wrapper {
    private static Object h;

    public static RenderItem d() {
        if (h == null && MRenderItem.V(RenderItem.sonyVegasInstance.getMappings().DI) == null && ForgeVersion.MC_1_7_10.L() && isNativeAvailable) {
            h = MRenderItem.F(RenderItem.sonyVegasInstance.getMappings().DI).newInstance(new Object[0]);
        }
        return new RenderItem(h != null ? h : MRenderItem.a(RenderItem.sonyVegasInstance.getMappings().DI));
    }

    public void z(float f) {
        MRenderItem.l(RenderItem.sonyVegasInstance.getMappings().DI, this.I, f);
    }

    public RenderItem(Object object) {
        super(object);
    }

    public void a(ItemStack itemStack, int n, int n2, MatrixStack matrixStack) {
        boolean bl;
        EntityPlayerSP entityPlayerSP = Minecraft.thePlayer();
        Channel bakedModel = this.getBakedModel(itemStack, null, entityPlayerSP.getObject(), 0);
        this.e(this.T() + 50.0f);
        TextureManager textureManager = Minecraft.getTextureManager();
        textureManager.getTexture(TextureAtlas.getBlocksAtlasLocation()).setFilter(false, false);
        RenderSystem.k(0, textureManager.getTexture(TextureAtlas.getBlocksAtlasLocation()).getId());
        RenderSystem.x();
        RenderSystem.s(770, 771);
        RenderSystem.U(1.0f, 1.0f, 1.0f, 1.0f);
        MatrixStack matrixStack2 = RenderSystem.p();
        matrixStack2.H();
        matrixStack2.V(n, n2, 100.0f + this.T());
        matrixStack2.S(1.0f, -1.0f, 1.0f);
        matrixStack2.S(16.0f, 16.0f, 16.0f);
        RenderSystem.v();
        RenderItemFontBridge renderItemFontBridge = Minecraft.H$src$Lvegas_sony_wrapper_impl_VoxelShape_$1dlcquv().getBufferSource();
        boolean bl2 = bl = !bakedModel.usesBlockLight();
        if (bl) {
            RenderHelper.l();
            this.I(itemStack, RenderItemContext.gui(), false, matrixStack, renderItemFontBridge, 15728880, 0, bakedModel);
            renderItemFontBridge.X();
            RenderSystem.n();
            RenderHelper.e();
            matrixStack2 = RenderSystem.p();
            matrixStack2.U();
            RenderSystem.v();
            this.e(this.T() - 50.0f);
            return;
        }
        this.I(itemStack, RenderItemContext.gui(), false, matrixStack, renderItemFontBridge, 15728880, 0, bakedModel);
        renderItemFontBridge.X();
        RenderSystem.n();
        matrixStack2 = RenderSystem.p();
        matrixStack2.U();
        RenderSystem.v();
        this.e(this.T() - 50.0f);
    }

    public float b() {
        return MRenderItem.I(RenderItem.sonyVegasInstance.getMappings().DI, this.I);
    }

    public void I(ItemStack itemStack, RenderItemContext renderItemContext, boolean bl, MatrixStack matrixStack, RenderItemFontBridge renderItemFontBridge, int n, int n2, Channel bakedModel) {
        MRenderItem.x(RenderItem.sonyVegasInstance.getMappings().DI, this.I, itemStack.getObject(), renderItemContext.getObject(), bl, matrixStack.getObject(), renderItemFontBridge.getObject(), n, n2, bakedModel.getObject());
    }

    public void e(float f) {
        if (ForgeVersion.MC_1_20_6.d()) {
            return;
        }
        MRenderItem.G(RenderItem.sonyVegasInstance.getMappings().DI, this.I, f);
    }

    public Channel getBakedModel(ItemStack itemStack, World world, Object entityHandle, int seed) {
        Object worldHandle = world == null ? null : world.getObject();
        return new Channel(MRenderItem.A(RenderItem.sonyVegasInstance.getMappings().DI, this.I, itemStack.getObject(), worldHandle, entityHandle, seed));
    }

    public void c(FontRenderer fontRenderer, TextureManager textureManager, ItemStack itemStack, int n, int n2) {
        if (ForgeVersion.MC_1_7_10.L()) {
            MRenderItem.z(RenderItem.sonyVegasInstance.getMappings().DI, this.I, fontRenderer.getObject(), textureManager.getObject(), itemStack.getObject(), n, n2);
        } else if (ForgeVersion.MC_1_20_6.d()) {
            MatrixStack matrixStack = MatrixStack.A();
            matrixStack.H();
            matrixStack.i(BufferedGuiRenderPrimitives.matrixStack.peek().toMinecraftMatrix());
            this.o(RenderItemTextBridge.t(matrixStack), itemStack, n, n2);
        } else {
            RenderItem.sonyVegasInstance.getMappings().DI.M(this.I, itemStack.getObject(), n, n2);
        }
    }

    public float T() {
        if (ForgeVersion.MC_1_20_6.d()) {
            return 100.0f;
        }
        return MRenderItem.x(RenderItem.sonyVegasInstance.getMappings().DI, this.I);
    }

    public void o(RenderItemTextBridge renderItemTextBridge, ItemStack itemStack, int n, int n2) {
        RenderItem.sonyVegasInstance.getMappings().DI.M(renderItemTextBridge.getObject(), itemStack.getObject(), n, n2);
    }

    public void P(FontRenderer fontRenderer, TextureManager textureManager, ItemStack itemStack, int n, int n2) {
        if (ForgeVersion.MC_1_7_10.L()) {
            MRenderItem.P(RenderItem.sonyVegasInstance.getMappings().DI, this.I, fontRenderer.getObject(), textureManager.getObject(), itemStack.getObject(), n, n2);
        } else {
            MRenderItem.Y(RenderItem.sonyVegasInstance.getMappings().DI, this.I, fontRenderer.getObject(), itemStack.getObject(), n, n2);
        }
    }

}

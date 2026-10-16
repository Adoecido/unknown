package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MFontRenderer;
import vegas.sony.utils.render.BufferedGuiRenderPrimitives;
import vegas.sony.utils.render.GlScissorRect;
import vegas.sony.utils.render.OpenGlBackendHolder;
import vegas.sony.utils.render.RenderBatchBuilder;
import vegas.sony.utils.render.RenderBatchManager;
import vegas.sony.utils.render.RenderMatrix4f;
import vegas.sony.utils.render.VertexCoordinateMode;
import vegas.sony.wrapper.Wrapper;

import java.awt.Color;
import java.util.HashMap;
import java.util.function.Supplier;

public class FontRenderer
extends Wrapper {
    private HashMap<String, Integer> H = new HashMap();

    public int B(String string, double d, double d2, int n, boolean bl, RenderMatrix4f renderMatrix4f, SharedMonsterAttributes sharedMonsterAttributes, GlScissorRect glScissorRect) {
        if (renderMatrix4f == null) {
            renderMatrix4f = new RenderMatrix4f().setIdentity();
            renderMatrix4f.multiply(BufferedGuiRenderPrimitives.viewMatrix);
            renderMatrix4f.multiply(BufferedGuiRenderPrimitives.matrixStack.peek());
        }
        if (ForgeVersion.MC_1_21_6.d()) {
            RenderItemTextBridge renderItemTextBridge = RenderItemTextBridge.l(Matrix4fHandle.b(16));
            Matrix4fHandle matrix4fHandle = renderItemTextBridge.F();
            matrix4fHandle.o();
            float[] fArray = renderMatrix4f.elements;
            matrix4fHandle.K(fArray[0]);
            matrix4fHandle.m(fArray[1]);
            matrix4fHandle.z(fArray[4]);
            matrix4fHandle.b(fArray[5]);
            matrix4fHandle.p(fArray[12]);
            matrix4fHandle.T(fArray[13]);
            if (glScissorRect != null) {
                renderItemTextBridge.a(glScissorRect.x, glScissorRect.y, glScissorRect.x + glScissorRect.width, glScissorRect.y + glScissorRect.height);
            }
            renderItemTextBridge.P(Minecraft.getFontRenderer(), string, (int)d, (int)d2, n, bl);
            if (glScissorRect != null) {
                renderItemTextBridge.k();
            }
            matrix4fHandle.N();
            return 0;
        }
        RenderItemFontBridge renderItemFontBridge = Minecraft.H$src$Lvegas_sony_wrapper_impl_VoxelShape_$1dlcquv().getBufferSource();
        int n2 = this.p(string, (float)d, (float)d2, n, bl, renderMatrix4f.toMinecraftMatrix(), renderItemFontBridge, sharedMonsterAttributes, 0, 15728880);
        renderItemFontBridge.q();
        return n2;
    }

    public int getFontHeight() {
        return FontRenderer.sonyVegasInstance.getMappings().R7.m(this.I);
    }

    public int H(MatrixStack matrixStack, String string, int n, int n2, int n3, boolean bl) {
        RenderItemTextBridge renderItemTextBridge = RenderItemTextBridge.t(matrixStack);
        return FontRenderer.sonyVegasInstance.getMappings().R7.C(renderItemTextBridge.getObject(), this.I, string, n, n2, n3, bl);
    }

    public int i(String string, double d, double d2, Color color, MatrixStack matrixStack) {
        return this.V(string, d, d2, color.getRGB(), matrixStack);
    }

    public int V(String string, double d, double d2, int n, MatrixStack matrixStack) {
        if (ForgeVersion.MC_1_20_6.d()) {
            return this.s(string, d, d2, n, true, matrixStack, null);
        }
        if (ForgeVersion.MC_1_16_5.d()) {
            int n2 = FontRenderer.sonyVegasInstance.getMappings().R7.M(this.I, matrixStack.getObject(), string, (float)d, (float)d2, n);
            return n2;
        }
        if (ForgeVersion.MC_1_7_10.Y()) {
            return FontRenderer.sonyVegasInstance.getMappings().R7.drawStringWithShadow(this.I, string, (float)d, (float)d2, n);
        }
        return FontRenderer.sonyVegasInstance.getMappings().R7.d(this.I, string, (int)d, (int)d2, n);
    }

    public int Z(ITextComponent iTextComponent, float f, float f2, int n, boolean bl, Matrix4f matrix4f, RenderItemFontBridge renderItemFontBridge, boolean bl2, int n2, int n3) {
        return FontRenderer.sonyVegasInstance.getMappings().R7.C(this.I, iTextComponent.getObject(), f, f2, n, bl, matrix4f.getObject(), renderItemFontBridge.getObject(), bl2, n2, n3);
    }

    public int drawCenteredString(String string, double d, double d2, int n) {
        return this.P(string, d, d2, n, ForgeVersion.MC_1_16_5.d() ? MatrixStack.A() : null);
    }

    public int drawString(String string, double d, double d2, Color color) {
        return this.Q(string, d, d2, color, ForgeVersion.MC_1_16_5.d() ? MatrixStack.A() : null);
    }

    public int getHalfFontHeight() {
        return this.getFontHeight() / 2;
    }

    public int p(String string, float f, float f2, int n, boolean bl, Matrix4f matrix4f, RenderItemFontBridge renderItemFontBridge, SharedMonsterAttributes sharedMonsterAttributes, int n2, int n3) {
        return MFontRenderer.i(FontRenderer.sonyVegasInstance.getMappings().R7, this.I, string, f, f2, n, bl, matrix4f.getObject(), renderItemFontBridge.getObject(), sharedMonsterAttributes.getObject(), n2, n3);
    }

    public int A(String string, float f, float f2, int n, boolean bl, MatrixStack matrixStack, GlScissorRect glScissorRect) {
        if (ForgeVersion.MC_1_16_5.d()) {
            return this.s(string, f, f2, n, bl, matrixStack, glScissorRect);
        }
        if (ForgeVersion.MC_1_7_10.Y()) {
            this.r();
        }
        return FontRenderer.sonyVegasInstance.getMappings().R7.Y(this.I, string, f, f2, n, bl);
    }

    public int s(String string, double d, double d2, int n, MatrixStack matrixStack) {
        return this.s(string, d, d2, n, false, matrixStack, null);
    }

    public int E(String string, float f, float f2, int n, boolean bl, MatrixStack matrixStack) {
        return this.A(string, f, f2, n, bl, matrixStack, null);
    }

    public int FONT_HEIGHT(String string) {
        return this.getFontHeight();
    }

    private Void lambda$drawString3D$0(String string, double d, double d2, int n, boolean bl, RenderMatrix4f renderMatrix4f, SharedMonsterAttributes sharedMonsterAttributes) {
        RenderItemFontBridge renderItemFontBridge = Minecraft.H$src$Lvegas_sony_wrapper_impl_VoxelShape_$1dlcquv().getBufferSource();
        GlStateManager.disableDepth();
        this.p(string, (float)d, (float)d2, n, bl, renderMatrix4f.toMinecraftMatrix(), renderItemFontBridge, sharedMonsterAttributes, 0, 15728880);
        renderItemFontBridge.q();
        GlStateManager.enableDepth();
        return null;
    }

    public int s(String string, double d, double d2, int n, boolean bl, MatrixStack matrixStack, GlScissorRect glScissorRect) {
        if (ForgeVersion.MC_1_20_6.d()) {
            return this.B(string, d, d2, n, bl, matrixStack.F().getMatrix().m$src$Lvegas_sony_utils_render_RenderMatrix4f_$1hodrum(), SharedMonsterAttributes.V(), glScissorRect);
        }
        if (ForgeVersion.MC_1_16_5.d()) {
            if (bl) {
                return FontRenderer.sonyVegasInstance.getMappings().R7.M(this.I, matrixStack.getObject(), string, (float)d, (float)d2, n);
            }
            return FontRenderer.sonyVegasInstance.getMappings().R7.S(this.I, matrixStack.getObject(), string, (float)d, (float)d2, n);
        }
        return FontRenderer.sonyVegasInstance.getMappings().R7.u(this.I, string, (int)d, (int)d2, n);
    }

    public int getStringWidth(String string) {
        if (this.H.containsKey(string)) {
            return this.H.get(string);
        }
        int n = FontRenderer.sonyVegasInstance.getMappings().R7.e(this.I, string);
        this.H.put(string, n);
        return n;
    }

    public int X(String string, double d, double d2, int n, double d3, MatrixStack matrixStack) {
        double d4 = 1.0 / d3;
        OpenGlBackendHolder.backend.scale(d3, d3, d3);
        int n2 = this.s(string, d * d4, d2 * d4, n, matrixStack);
        OpenGlBackendHolder.backend.scale(d4, d4, d4);
        return n2;
    }

    public int drawStringWithShadow(String string, double d, double d2, int n) {
        return this.V(string, d, d2, n, ForgeVersion.MC_1_16_5.d() ? MatrixStack.A() : null);
    }

    public int drawStringWithShadow(String string, double d, double d2, Color color) {
        return this.i(string, d, d2, color, ForgeVersion.MC_1_16_5.d() ? MatrixStack.A() : null);
    }

    public double getHalfFontHeight(String string) {
        return this.getHalfFontHeight();
    }

    public int drawString(String string, double d, double d2, int n) {
        return this.s(string, d, d2, n, ForgeVersion.MC_1_16_5.d() ? MatrixStack.A() : null);
    }

    public int drawCenteredStringWithShadow(String string, double d, double d2, Color color) {
        return this.drawCenteredStringWithShadow(string, d, d2, color.getRGB());
    }

    public int h(String string, double d, double d2, int n, boolean bl, RenderMatrix4f renderMatrix4f, SharedMonsterAttributes sharedMonsterAttributes) {
        if (renderMatrix4f == null) {
            renderMatrix4f = new RenderMatrix4f().setIdentity();
            renderMatrix4f.multiply(BufferedGuiRenderPrimitives.viewMatrix);
            renderMatrix4f.multiply(BufferedGuiRenderPrimitives.matrixStack.peek());
        }
        RenderMatrix4f renderMatrix4f2 = renderMatrix4f;
        Supplier<Void> supplier = () -> this.lambda$drawString3D$0(string, d, d2, n, bl, renderMatrix4f2, sharedMonsterAttributes);
        RenderBatchBuilder renderBatchBuilder = new RenderBatchBuilder(4, VertexCoordinateMode.MINECRAFT, true).setStandaloneRenderCallback(supplier);
        RenderBatchManager.getInstance().queueWorldBatch(renderBatchBuilder);
        return 0;
    }

    public void J(MatrixStack matrixStack, ITextComponent iTextComponent, float f, float f2, int n) {
        MFontRenderer.q(FontRenderer.sonyVegasInstance.getMappings().R7, this.I, matrixStack.getObject(), iTextComponent.getObject(), f, f2, n);
    }

    public int n(String string, double d, double d2, Color color, MatrixStack matrixStack) {
        return this.P(string, d, d2, color.getRGB(), matrixStack);
    }

    public int P(String string, double d, double d2, int n, MatrixStack matrixStack) {
        return this.s(string, (int)d - this.getStringWidth(string) / 2, (int)d2, n, matrixStack);
    }

    public int L(String string, double d, double d2, int n, MatrixStack matrixStack) {
        return this.V(string, d - (double)(this.getStringWidth(string) / 2), d2, n, matrixStack);
    }

    public void r() {
        FontRenderer.sonyVegasInstance.getMappings().R7.J(this.I);
    }

    public int drawCenteredStringWithShadow(String string, double d, double d2, int n) {
        return this.L(string, d, d2, n, ForgeVersion.MC_1_16_5.d() ? MatrixStack.A() : null);
    }

    public int Q(String string, double d, double d2, Color color, MatrixStack matrixStack) {
        return this.s(string, d, d2, color.getRGB(), matrixStack);
    }

    public FontRenderer(Object object) {
        super(object);
    }

    public int drawStringScaled(String string, double d, double d2, int n, double d3) {
        return this.X(string, d, d2, n, d3, ForgeVersion.MC_1_16_5.d() ? MatrixStack.A() : null);
    }

    public void U(MatrixStack matrixStack, ITextComponent iTextComponent, float f, float f2, int n) {
        MFontRenderer.A(FontRenderer.sonyVegasInstance.getMappings().R7, this.I, matrixStack.getObject(), iTextComponent.getObject(), f, f2, n);
    }

}

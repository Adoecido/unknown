package vegas.sony.module.world;

import vegas.sony.module.Category;
import vegas.sony.module.Mod;
import vegas.sony.module.world.fakelag.FakeLagDelayedPacketMode;
import vegas.sony.module.world.fakelag.FakeLagPacketDelaySubModule;
import vegas.sony.module.world.fakelag.LegacyFakeLagCombatPacketQueueMode;
import vegas.sony.event.EventHandler;
import vegas.sony.event.impl.EventRender3D;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.utils.datas.PlayerLocationSnapshot;
import vegas.sony.utils.render.GuiRenderPrimitives;
import vegas.sony.utils.render.OpenGlBackendHolder;
import vegas.sony.utils.render.RenderUtil;
import vegas.sony.value.BooleanValue;
import vegas.sony.value.ModeValue;
import vegas.sony.value.NumberValue;
import vegas.sony.value.SubModuleValue;
import vegas.sony.wrapper.impl.ForgeVersion;
import vegas.sony.wrapper.impl.C03PacketPlayer;
import vegas.sony.wrapper.impl.Packet;
import vegas.sony.wrapper.impl.RenderManager;
import java.awt.Color;
import org.lwjgl.opengl.GL11;

public class FakeLag
extends Mod {
    private final SubModuleValue repelMode;
    private final ModeValue modeValue;
    private static final long MOD_ID = -7445823933619103425L;
    public final NumberValue delay;
    private final BooleanValue showBox;
    private final SubModuleValue dynamicMode;
    private final SubModuleValue latencyMode = new FakeLagDelayedPacketMode(this, "Latency").getSelectionValue();
    private volatile PlayerLocationSnapshot serverPosition;

    public FakeLag() {
        super("FakeLag //$ui", (int)MOD_ID, Category.NETWORK, "Simulates lag //$ui");
        this.dynamicMode = new FakeLagPacketDelaySubModule(this, "Dynamic").getSelectionValue();
        this.repelMode = new LegacyFakeLagCombatPacketQueueMode(this, "Repel").getSelectionValue();
        this.delay = NumberValue.create((Object)this, "Delay //$ui", "#", "ms", 1.0, 100.0, 1000.0, 10.0);
        this.showBox = BooleanValue.create(this, "Show box //$ui", false, "Draws your last position sent to the server //$ui");
        this.modeValue = ForgeVersion.MC_1_7_10.Y() ? ModeValue.create((Object)this, "Mode //$ui", this.latencyMode, this.latencyMode, this.dynamicMode, this.repelMode) : ModeValue.create((Object)this, "Mode //$ui", this.latencyMode, this.latencyMode, this.repelMode);
        this.addValue(this.modeValue, this.delay, this.showBox);
        this.delay.setMaximumFractionDigits(0);
    }

    @Override
    public String getSimpleSuffix() {
        return this.modeValue.getDisplayValue();
    }

    @Override
    public String getDetailedSuffix() {
        SubModuleValue subModuleValue = (SubModuleValue)this.modeValue.getValue();
        return ((Mod)subModuleValue.getInstance()).getDetailedSuffix();
    }

    @EventHandler
    public void onRender3D(EventRender3D event) {
        if (!this.isEnabled() || !this.showBox.getEffectiveValue()) return;
        PlayerLocationSnapshot position = this.serverPosition;
        if (position == null) return;

        event.getEntityRenderer().B(1.0);
        RenderUtil.d();
        if (!GuiRenderPrimitives.d()) {
            OpenGlBackendHolder.backend.enableCapability(3042);
            GL11.glBlendFunc(770, 771);
            GL11.glLineWidth(1.5f);
            OpenGlBackendHolder.backend.disableCapability(3553);
            OpenGlBackendHolder.backend.enableCapability(2848);
            OpenGlBackendHolder.backend.disableCapability(2929);
            GL11.glDepthMask(false);
        }
        RenderUtil.u(position.getX() - 0.3, position.getY(), position.getZ() - 0.3,
                0.6, 1.8, 0.6, 1.0,
                new Color(65, 145, 255, 230), new Color(65, 145, 255, 55),
                RenderManager.getInterpolatedRenderPosX(),
                RenderManager.getInterpolatedRenderPosY(),
                RenderManager.getInterpolatedRenderPosZ());
        if (!GuiRenderPrimitives.d()) {
            GL11.glDepthMask(true);
            OpenGlBackendHolder.backend.enableCapability(2929);
            OpenGlBackendHolder.backend.enableCapability(3553);
            OpenGlBackendHolder.backend.disableCapability(2848);
            OpenGlBackendHolder.backend.disableCapability(3042);
        }
        RenderUtil.Y();
        event.getEntityRenderer().O(1.0);
    }

    public void recordServerPosition(Packet packet) {
        if (!this.isEnabled() || packet == null || packet.isNull() || !packet.isInstance(MappedClasses.qD)) return;
        C03PacketPlayer movement = new C03PacketPlayer(packet);
        if (movement.hasPosition()) {
            this.serverPosition = new PlayerLocationSnapshot(movement.getX(), movement.getY(), movement.getZ());
        }
    }

    @Override
    public void onDisable() {
        this.serverPosition = null;
    }
}

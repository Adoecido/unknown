package vegas.sony.module.blatant;

import vegas.sony.SonyVegas;
import vegas.sony.config.ClientSettings;
import vegas.sony.event.EventHandler;
import vegas.sony.event.EventPriority;
import vegas.sony.event.PacketSendDispatchGuardCallback;
import vegas.sony.event.impl.EventPacketSend;
import vegas.sony.event.impl.EventPreTick;
import vegas.sony.event.impl.EventRender3D;
import vegas.sony.event.impl.EventTickBase;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.module.Category;
import vegas.sony.module.Mod;
import vegas.sony.unmap.ModeOption;
import vegas.sony.unmap.ModeSelection;
import vegas.sony.utils.TimerUtil;
import vegas.sony.utils.Vec3d;
import vegas.sony.utils.network.PacketDispatchGuard;
import vegas.sony.utils.render.GuiRenderPrimitives;
import vegas.sony.utils.render.OpenGlBackendHolder;
import vegas.sony.utils.render.RenderUtil;
import vegas.sony.value.BooleanValue;
import vegas.sony.value.ConditionalValue;
import vegas.sony.value.ModeValue;
import vegas.sony.value.NumberValue;
import vegas.sony.wrapper.impl.EntityOtherPlayerMP;
import vegas.sony.wrapper.impl.EntityPlayerSP;
import vegas.sony.wrapper.impl.ForgeVersion;
import vegas.sony.wrapper.impl.GuiScreen;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.Packet;
import vegas.sony.wrapper.impl.RenderManager;
import java.awt.Color;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import org.lwjgl.opengl.GL11;

public class Blink
extends Mod {
    private boolean pendingReEnable;
    private EntityOtherPlayerMP fakePlayer;
    private boolean stopped;
    private boolean threadRunning = false;
    private final Queue<PacketSendDispatchGuardCallback> pendingCallbacks;
    private static final long MODULE_ID = -5919086810783803073L;
    private int fakePlayerEntityId;
    private final NumberValue sendThreshold;
    private final BooleanValue breadcrumbs;
    private final BooleanValue autoSend;
    private final List<Vec3d> breadcrumbTrail;
    private final TimerUtil fakePlayerTimer;
    private final PacketDispatchGuard dispatchGuard;
    private final ModeOption biDirectionalMode;
    private final ModeValue directionMode;
    private final Runnable fakePlayerTask;
    private final BooleanValue spawnFake;
    private final ModeOption outgoingOnlyMode = new ModeOption("Outgoing only //$ui");
    private int chokeCount = 0;

    @Override
    public void setEnabled(boolean enabled, boolean bypassVisibilityCheck) {
        if (!enabled) {
            this.stopped = true;
            this.pendingReEnable = true;
        } else {
            super.setEnabled(true, bypassVisibilityCheck);
        }
    }

    private int getChokeCount() {
        return this.chokeCount;
    }

    @Override
    public void onDisable() {
        this.breadcrumbTrail.clear();
        if (Minecraft.thePlayer().isNull() || Minecraft.theWorld().isNull()) {
            this.fakePlayer = null;
        }
        if (this.fakePlayer != null && Minecraft.theWorld().V(this.fakePlayer.S()).isNotNull()) {
            Minecraft.theWorld().M(this.fakePlayer);
        }
        this.fakePlayer = null;
    }

    private void fakePlayerLoop() {
        while (this.threadRunning) {
            try {
                Thread.sleep(50L);
            }
            catch (InterruptedException interruptedException) {
                {}
            }
        }
    }

    private void flushPackets() {
        this.threadRunning = false;
        this.fakePlayerTimer.reset();
        this.stopped = false;
        this.chokeCount = 0;
        for (PacketSendDispatchGuardCallback packetSendDispatchGuardCallback : this.pendingCallbacks) {
            try {
                packetSendDispatchGuardCallback.dispatch(this.dispatchGuard);
            }
            catch (Exception exception) {
                SonyVegas.logThrowable(exception);
            }
        }
        this.pendingCallbacks.clear();
        if (this.pendingReEnable) {
            super.setEnabled(false, true);
            this.pendingReEnable = false;
        }
    }

    private boolean isMoving() {
        EntityPlayerSP player = Minecraft.thePlayer();
        return player.t() != 0.0 || player.T() != 0.0;
    }

    @EventHandler
    public void onRender3D(EventRender3D eventRender3D) {
        if (!this.breadcrumbs.getEffectiveValue().booleanValue() || this.breadcrumbTrail.isEmpty()) {
            return;
        }
        eventRender3D.getEntityRenderer().B(1.0);
        RenderUtil.d();
        if (!GuiRenderPrimitives.d()) {
            OpenGlBackendHolder.backend.enableCapability(3042);
            GL11.glBlendFunc((int)770, (int)771);
            GL11.glLineWidth((float)1.5f);
            OpenGlBackendHolder.backend.disableCapability(3553);
            OpenGlBackendHolder.backend.enableCapability(2848);
            OpenGlBackendHolder.backend.disableCapability(2929);
            GL11.glDepthMask((boolean)false);
        }
        double renderX = RenderManager.getInterpolatedRenderPosX();
        double renderY = RenderManager.getInterpolatedRenderPosY();
        double renderZ = RenderManager.getInterpolatedRenderPosZ();
        boolean firstBreadcrumb = true;
        for (Vec3d breadcrumb : this.breadcrumbTrail) {
            Color color = Color.RED;
            if (firstBreadcrumb) {
                color = Color.YELLOW;
                firstBreadcrumb = false;
            }
            RenderUtil.u(breadcrumb.getX() - 0.1, breadcrumb.getY(), breadcrumb.getZ() - 0.1, 0.2, 0.0, 0.2, 1.0, Color.BLACK, color, renderX, renderY, renderZ);
        }
        if (!GuiRenderPrimitives.d()) {
            GL11.glDepthMask((boolean)true);
            OpenGlBackendHolder.backend.enableCapability(2929);
            OpenGlBackendHolder.backend.enableCapability(3553);
            OpenGlBackendHolder.backend.disableCapability(2848);
            OpenGlBackendHolder.backend.disableCapability(3042);
        }
        RenderUtil.Y();
        eventRender3D.getEntityRenderer().O(1.0);
    }

    @EventHandler
    public void onTick(EventPreTick eventPreTick) {
        EntityPlayerSP player = eventPreTick.getThePlayer();
        ConditionalValue.getConditionalLegacyState();
        if (player.isNull() || Minecraft.theWorld().isNull()) {
            this.threadRunning = false;
            this.pendingCallbacks.clear();
            this.breadcrumbTrail.clear();
            this.chokeCount = 0;
            this.stopped = false;
            this.pendingReEnable = false;
            super.setEnabled(false, true);
            return;
        }
        if (((ModeSelection)this.directionMode.getValue()).equals(this.biDirectionalMode) && this.fakePlayerTimer.hasTimeElapsed(50L) && !this.threadRunning && player.isNotNull()) {
            this.fakePlayerTimer.reset();
            this.threadRunning = true;
            player.sendQueue().a().getChannel().eventLoop().execute(this.fakePlayerTask);
        }
        int currentChokeCount = this.getChokeCount();
        if (!this.stopped && this.autoSend.getEffectiveValue().booleanValue() && (Double)this.sendThreshold.getValue() > 0.0 && (double)currentChokeCount >= (Double)this.sendThreshold.getValue()) {
            if (this.fakePlayer != null && this.fakePlayer.isNotNull()) {
                this.fakePlayer.t(player.z(), ClientSettings.IS_LEGACY_1_7 ? player.N() : player.N() - 1.5, player.h(), player.J(), player.V());
                this.fakePlayer.z(player.s());
            }
            this.breadcrumbTrail.clear();
            this.stopped = true;
            this.pendingReEnable = false;
        }
        GuiScreen currentScreen = Minecraft.currentScreen();
        if (currentScreen.isInstance(MappedClasses.u5) || currentScreen.isInstance(MappedClasses.D6) || currentScreen.isInstance(MappedClasses.F_) || Minecraft.thePlayer().isNull()) {
            this.stopped = true;
        }
        if (this.stopped) {
            this.flushPackets();
        }
    }

    public Blink() {
        super("Blink //$ui", (int)MODULE_ID, Category.NETWORK, "Chokes packets until disabled. //$ui");
        this.biDirectionalMode = new ModeOption("Bi-directional //$ui");
        this.directionMode = ModeValue.create((Object)this, "Direction //$ui", "Outgoing only - only chokes packets that you're sending\nBi-directional - additionally chokes incoming packets from the server //$ui", (ModeSelection)this.outgoingOnlyMode, this.outgoingOnlyMode, this.biDirectionalMode);
        this.breadcrumbs = BooleanValue.create(this, "Breadcrumbs //$ui", false, "Drops breadcrumbs in case you forgot where you came from. //$ui");
        this.spawnFake = BooleanValue.create(this, "Spawn fake //$ui", false, "Spawns a fake player where you started/are on the servers side. //$ui");
        this.autoSend = BooleanValue.create(this, "Auto send //$ui", false, "Automatically sends packets after a limit is reached //$ui");
        this.sendThreshold = NumberValue.create(this, "Send threshold //$ui", "#", "", 0.0, 50.0, 100.0, 1.0, "Allows you to choke your packets for a custom set limit.\nAfter the limit is reached, all packets will be sent.\nNOTE: 0 = No Limit //$ui");
        this.dispatchGuard = PacketDispatchGuard.b;
        this.pendingCallbacks = new ConcurrentLinkedQueue<PacketSendDispatchGuardCallback>();
        this.breadcrumbTrail = new CopyOnWriteArrayList<Vec3d>();
        this.fakePlayerTimer = new TimerUtil();
        this.fakePlayerTask = this::fakePlayerLoop;
        this.autoSend.addDependentValues(this.sendThreshold);
        this.addValue(this.directionMode, this.breadcrumbs, this.spawnFake, this.autoSend, this.sendThreshold);
    }

    @EventHandler(priority=EventPriority.LOWEREST)
    public void onPacketSend(EventPacketSend eventPacketSend) {
        if (!Thread.currentThread().equals(EventTickBase.PRE_TICK_EXECUTOR.getOwnerThread())) {
            return;
        }
        if (eventPacketSend.isCanceled()) {
            return;
        }
        Packet packet = eventPacketSend.getPacket();
        if (this.dispatchGuard.R(packet)) {
            return;
        }
        ++this.chokeCount;
        int currentChokeCount = this.getChokeCount();
        if (packet.isInstance(MappedClasses.qD) && this.isMoving() && currentChokeCount % 5 == 0) {
            EntityPlayerSP player = Minecraft.thePlayer();
            int offsetSign = currentChokeCount % 2 == 0 ? -1 : 1;
            double offsetX = 0.2 * (double)offsetSign * Math.cos(Math.toRadians(player.J()));
            double offsetZ = 0.2 * (double)offsetSign * Math.sin(Math.toRadians(player.J()));
            this.breadcrumbTrail.add(new Vec3d(player.z() + offsetX, player.N(), player.h() + offsetZ));
        }
        this.pendingCallbacks.add(new PacketSendDispatchGuardCallback(eventPacketSend));
        eventPacketSend.setCancelled(true);
    }

    @Override
    public void onEnable() {
        EntityPlayerSP player = Minecraft.thePlayer();
        if (player.isNull()) {
            super.setEnabled(false, true);
            return;
        }
        if (this.spawnFake.getEffectiveValue().booleanValue()) {
            this.fakePlayer = EntityOtherPlayerMP.create(Minecraft.theWorld(), Minecraft.thePlayer().c$src$Lvegas_sony_wrapper_impl_GameProfile_$ir8937());
            this.fakePlayerEntityId = ClientSettings.reserveEntityId();
            this.fakePlayer.M(player, true);
            if (ForgeVersion.MC_1_17.d()) {
                this.fakePlayer.Q(this.fakePlayerEntityId);
                UUID offlineUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + player.getName()).getBytes(StandardCharsets.UTF_8));
                this.fakePlayer.y(offlineUuid);
            }
            this.updateFakePlayerPosition();
            this.fakePlayer.z(player.s());
            Minecraft.theWorld().D(this.fakePlayerEntityId, this.fakePlayer);
        }
    }

    private void updateFakePlayerPosition() {
        EntityPlayerSP player = Minecraft.thePlayer();
        if (player.isNull() || this.fakePlayer.isNull() || !this.spawnFake.getEffectiveValue().booleanValue()) {
            return;
        }
        this.fakePlayer.t(player.z(), ClientSettings.IS_LEGACY_1_7 ? player.N() : player.N() - 1.5, player.h(), player.J(), player.V());
        this.fakePlayer.z(player.s());
    }

    @Override
    public String getSimpleSuffix() {
        int chokeCount = this.getChokeCount();
        if (chokeCount == 0) {
            return "";
        }
        return String.valueOf(chokeCount);
    }
}

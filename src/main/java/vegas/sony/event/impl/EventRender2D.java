package vegas.sony.event.impl;

import vegas.sony.SonyVegas;
import vegas.sony.event.Event;
import vegas.sony.event.EventListeners;
import vegas.sony.module.none.ClientSettings;
import vegas.sony.runtime.NativeBridge;
import vegas.sony.ui.click.GuiScreenNativeCallbackBridge;
import vegas.sony.utils.render.GuiRenderPrimitives;
import vegas.sony.wrapper.impl.ForgeVersion;
import vegas.sony.wrapper.impl.GlStateManager;
import vegas.sony.wrapper.impl.Minecraft;

public class EventRender2D
extends Event {
    private static final EventListeners EVENT_LISTENERS = new EventListeners();
    private static long lastBadlionGuiTickNanos;
    private static long lastCheatBreakerGuiTickNanos;
    private static boolean badlionCreateLogged;
    private static boolean badlionFramesReadyLogged;
    private static boolean createFiredLogged;
    private static boolean createSkippedLogged;
    private static boolean renderFailureLogged;
    private static boolean badlionLoaderProbeLogged;
    private static long badlionLastRegDumpNanos;
    private static int displayHeight;
    private static int displayWidth;

    public static EventListeners getEventListeners() {
        return EVENT_LISTENERS;
    }

    /**
     * Badlion 1.8.9 bring-up probe: tick/motion/packet hooks install "ok"
     * but never fire. Runs once on the game thread and compares the hooked
     * Class objects against what the live game thread resolves, to detect a
     * dual-copy (LaunchWrapper/Tweaker) class-identity mismatch. Badlion-only;
     * Lunar/CB never execute this branch.
     */
    private static void probeBadlionClassLoaderIdentity() {
        // Periodic registration census: the one-shot loader-identity report
        // runs once, but listener counts grow after frame init, so re-dump
        // the registration map every 60s. Badlion-only.
        if (badlionLoaderProbeLogged) {
            long now = System.nanoTime();
            if (now - badlionLastRegDumpNanos >= 60000000000L) {
                badlionLastRegDumpNanos = now;
                dumpBadlionRegistrations();
            }
            return;
        }
        badlionLoaderProbeLogged = true;
        badlionLastRegDumpNanos = System.nanoTime();
        try {
            if (!NativeBridge.isBadlion189Runtime()) {
                vegas.sony.runtime.CheatBreakerDebugLog.info(
                        "HOOK_LOADER_PROBE badlionFlag=false thread="
                                + Thread.currentThread().getName());
                return;
            }
            ClassLoader gameLoader = Thread.currentThread().getContextClassLoader();
            // CheatBreakerDebugLog.info only keeps HOOK_/TASK_/EVENT_/etc
            // prefixes; keep this probe visible with HOOK_.
            StringBuilder report = new StringBuilder("HOOK_LOADER_PROBE gameLoader=");
            report.append(loaderName(gameLoader));
            report.append(" thread=").append(Thread.currentThread().getName());
            report.append(" hooked_uP=").append(classIdentity(vegas.sony.mapping.MappedClasses.uP));
            report.append(" hooked_z5=").append(classIdentity(vegas.sony.mapping.MappedClasses.z5));
            try {
                Class<?> liveAve = Class.forName("ave", false, gameLoader);
                report.append(" live_ave=").append(classIdentity(liveAve));
                report.append(" sameMc=").append(liveAve == vegas.sony.mapping.MappedClasses.uP);
            } catch (Throwable missingAve) {
                report.append(" live_ave=MISSING:").append(missingAve.getClass().getSimpleName());
            }
            try {
                Class<?> liveBew = Class.forName("bew", false, gameLoader);
                report.append(" live_bew=").append(classIdentity(liveBew));
                report.append(" sameSp=").append(liveBew == vegas.sony.mapping.MappedClasses.z5);
            } catch (Throwable missingBew) {
                report.append(" live_bew=MISSING:").append(missingBew.getClass().getSimpleName());
            }
            appendBadlionRegistrations(report);
            report.append(" opaque=").append(vegas.sony.asm.transform.ClassTransformer.opaquePredicate());
            vegas.sony.runtime.CheatBreakerDebugLog.info(report.toString());
        } catch (Throwable ignored) {
            // Probe must never break the render loop.
        }
    }

    private static void dumpBadlionRegistrations() {
        try {
            if (!NativeBridge.isBadlion189Runtime()) {
                return;
            }
            StringBuilder report = new StringBuilder("HOOK_REG_CENSUS");
            appendBadlionRegistrations(report);
            appendBadlionInstanceClasses(report);
            vegas.sony.runtime.CheatBreakerDebugLog.info(report.toString());
        } catch (Throwable ignored) {
            // Census must never break the render loop.
        }
    }

    /**
     * Reads the LIVE bytecode back through NativeBridge.gcb (JVMTI
     * retransform + capture) and checks whether the injected callback
     * references are present. Distinguishes "redefine lost" from
     * "code present but never executed". Badlion-only.
     */
    private static void appendBadlionLiveBytes(StringBuilder report) {
        checkLiveHook(report, "ave",
                vegas.sony.mapping.MappedClasses.uP,
                vegas.sony.mapping.EventPreTickCallback.class.getName().replace('.', '/'));
        checkLiveHook(report, "bew",
                vegas.sony.mapping.MappedClasses.z5,
                vegas.sony.event.impl.EventPreMotion.class.getName().replace('.', '/'));
        checkLiveHook(report, "wn",
                vegas.sony.mapping.MappedClasses.Yl,
                vegas.sony.event.impl.EventPrePlayerTick.class.getName().replace('.', '/'));
        checkLiveHook(report, "ek",
                vegas.sony.mapping.MappedClasses.FO,
                vegas.sony.event.impl.EventPacketSend.class.getName().replace('.', '/'));
    }

    private static void checkLiveHook(StringBuilder report, String tag, Class<?> target, String marker) {
        try {
            if (target == null) {
                report.append(" ").append(tag).append("Bytes=notarget");
                return;
            }
            byte[] live = NativeBridge.gcb(target);
            if (live == null) {
                report.append(" ").append(tag).append("Bytes=gcbnull");
                return;
            }
            byte[] needle = marker.getBytes("UTF-8");
            boolean found = indexOf(live, needle) >= 0;
            report.append(" ").append(tag).append("Bytes=").append(live.length)
                    .append(found ? ":hooked" : ":CLEAN");
        } catch (Throwable failure) {
            report.append(" ").append(tag).append("Bytes=ERR:").append(failure.getClass().getSimpleName());
        }
    }

    private static int indexOf(byte[] haystack, byte[] needle) {
        outer:
        for (int i = 0; i + needle.length <= haystack.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    /**
     * Badlion may run subclass instances of the vanilla game classes (an
     * override without super would silently bypass hooks placed in the base
     * method). Report the live instance classes for the tick/motion/packet
     * owners. Badlion-only.
     */
    private static void appendBadlionInstanceClasses(StringBuilder report) {
        try {
            Object mcHandle = vegas.sony.wrapper.impl.Minecraft.i();
            report.append(" mcInst=").append(instanceIdentity(mcHandle));
        } catch (Throwable failure) {
            report.append(" mcInst=ERR:").append(failure.getClass().getSimpleName());
        }
        try {
            vegas.sony.wrapper.impl.EntityPlayerSP player = vegas.sony.wrapper.impl.Minecraft.thePlayer();
            Object handle = player == null ? null : player.getObject();
            report.append(" playerInst=").append(instanceIdentity(handle));
        } catch (Throwable failure) {
            report.append(" playerInst=ERR:").append(failure.getClass().getSimpleName());
        }
        try {
            vegas.sony.wrapper.impl.WorldClient world = vegas.sony.wrapper.impl.Minecraft.theWorld();
            Object handle = world == null ? null : world.getObject();
            report.append(" worldInst=").append(instanceIdentity(handle));
        } catch (Throwable failure) {
            report.append(" worldInst=ERR:").append(failure.getClass().getSimpleName());
        }
    }

    private static String instanceIdentity(Object handle) {
        if (handle == null) {
            return "null";
        }
        Class<?> clazz = handle.getClass();
        Class<?> superclass = clazz.getSuperclass();
        return clazz.getName() + "@" + Integer.toHexString(System.identityHashCode(handle))
                + " super=" + (superclass == null ? "null" : superclass.getName());
    }

    private static void appendBadlionRegistrations(StringBuilder report) {
        try {
            java.util.Map<Class<? extends vegas.sony.event.IEvent>, java.util.ArrayList<vegas.sony.event.EventListenerRegistration>> regs =
                    vegas.sony.event.EventBus.getInstance().getRegistrationsByEventType();
            report.append(" regTypes=").append(regs.size());
            // Compare by Class identity: payload class SIMPLE names are
            // renamed by RenameClasses, so simple-name matching never hits.
            report.append(" PreTick=").append(regCount(regs, vegas.sony.event.impl.EventPreTick.class));
            report.append(" PreMotion=").append(regCount(regs, vegas.sony.event.impl.EventPreMotion.class));
            report.append(" PreMove=").append(regCount(regs, vegas.sony.event.impl.EventPreMove.class));
            report.append(" PacketSend=").append(regCount(regs, vegas.sony.event.impl.EventPacketSend.class));
            report.append(" Render2D=").append(regCount(regs, EventRender2D.class));
            report.append(" PrePlayerTick=").append(regCount(regs, vegas.sony.event.impl.EventPrePlayerTick.class));
            report.append(" PreRenderTick=").append(regCount(regs, vegas.sony.event.impl.EventPreRenderTick.class));
            report.append(" KeyPress=").append(regCount(regs, vegas.sony.event.impl.EventKeyPress.class));
            report.append(" hasL=").append(regCount(regs, EventRender2D.class) >= 0);
        } catch (Throwable regFailure) {
            report.append(" regDUMP_FAILED:").append(regFailure.getClass().getSimpleName());
        }
    }

    private static int regCount(
            java.util.Map<Class<? extends vegas.sony.event.IEvent>, java.util.ArrayList<vegas.sony.event.EventListenerRegistration>> regs,
            Class<? extends vegas.sony.event.IEvent> type) {
        java.util.ArrayList<vegas.sony.event.EventListenerRegistration> list = regs.get(type);
        return list == null ? -1 : list.size();
    }

    private static String loaderName(ClassLoader loader) {
        if (loader == null) {
            return "bootstrap-null";
        }
        return loader.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(loader));
    }

    private static String classIdentity(Class<?> clazz) {
        if (clazz == null) {
            return "null";
        }
        ClassLoader loader = clazz.getClassLoader();
        return clazz.getName() + "@" + Integer.toHexString(System.identityHashCode(clazz))
                + " loader=" + loaderName(loader);
    }

    public static void create() {
        try {
            renderFrame();
        }
        catch (Throwable failure) {
            // This method is injected directly into Display.update. Never let
            // a payload exception unwind into Badlion's render loop.
            if (!renderFailureLogged) {
                renderFailureLogged = true;
                NativeBridge.sce("RENDER_CALLBACK_FAILED "
                        + failure.getClass().getName() + ": "
                        + String.valueOf(failure.getMessage()));
            }
            SonyVegas.logThrowable(failure);
        }
    }

    private static void renderFrame() {
        boolean badlionRender = NativeBridge.isBadlion189Runtime();
        if (badlionRender && !badlionCreateLogged) {
            badlionCreateLogged = true;
            NativeBridge.sce("RENDER callback entered");
        }
        if (badlionRender) {
            probeBadlionClassLoaderIdentity();
            SonyVegas.retryPendingBadlionMappings();
            /*
             * The final frame callback is also a reliable game-thread drain
             * point while Badlion is between worlds or suppresses a normal
             * client tick. This keeps native-menu actions responsive without
             * replacing the regular tick hook used by gameplay modules.
             */
            EventTickBase.PRE_TICK_EXECUTOR.runPending();
            EventTickBase.POST_TICK_EXECUTOR.runPending();
        }
        if (NativeBridge.isBadlion189Runtime()
                || NativeBridge.isCheatBreaker189Runtime()
                || NativeBridge.isLunar189Runtime()) {
            SonyVegas.runPendingClientFrameInit();
            if (!ClientSettings.framesInitialized) {
                if (!createSkippedLogged) {
                    createSkippedLogged = true;
                    {}
                }
                return;
            }
            if (badlionRender && !badlionFramesReadyLogged) {
                badlionFramesReadyLogged = true;
                NativeBridge.sce("RENDER frames ready");
            }
            if (NativeBridge.isCheatBreaker189Runtime() && !createFiredLogged) {
                createFiredLogged = true;
                vegas.sony.runtime.CheatBreakerDebugLog.info("EventRender2D.create fired (CB)");
            }
        }
        if (ForgeVersion.MC_1_17.d() && Minecraft.thePlayer().isNull()) {
            return;
        }
        displayWidth = Minecraft.J();
        displayHeight = Minecraft.h();
        // Lunar-only GL fortress: Ichor/OptiFine/WebOSR share this context
        // and assume vanilla overlay state afterwards (Lunar's own menu
        // compositing breaks otherwise). Save everything the HUD pass may
        // touch and restore it when done, so a mid-frame exception or a
        // leaky helper can never corrupt Lunar's rendering. Other runtimes
        // keep their exact previous behavior.
        boolean lunarFortress = NativeBridge.isLunar189Runtime();
        int[] savedFbo = new int[1];
        int[] savedActiveTexture = new int[1];
        if (lunarFortress) {
            try {
                org.lwjgl.opengl.GL11.glPushAttrib(org.lwjgl.opengl.GL11.GL_ALL_ATTRIB_BITS);
                org.lwjgl.opengl.GL11.glMatrixMode(org.lwjgl.opengl.GL11.GL_PROJECTION);
                org.lwjgl.opengl.GL11.glPushMatrix();
                org.lwjgl.opengl.GL11.glMatrixMode(org.lwjgl.opengl.GL11.GL_MODELVIEW);
                org.lwjgl.opengl.GL11.glPushMatrix();
                savedFbo[0] = org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_BINDING);
                savedActiveTexture[0] = org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL13.GL_ACTIVE_TEXTURE);
            } catch (Throwable fortressSetupFailure) {
                lunarFortress = false;
            }
        }
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GuiRenderPrimitives.o(displayWidth, displayHeight);
        try {
            EventRender2D eventRender2D = new EventRender2D();
            vegas.sony.ui.hud.HudLayout.refreshPreviewPositions();
            eventRender2D.fire();
            vegas.sony.ui.hud.HudLayout.renderPreviews(eventRender2D);
            // HUD frames (including TargetInfo) and notifications are the final
            // client pass so combat-module drawings cannot cover them.
            SonyVegas.INSTANCE.getModManager().getMod(ClientSettings.class).renderHudOverlay();
        }
        finally {
            GuiRenderPrimitives.L(displayWidth, displayHeight);
            if (lunarFortress) {
                try {
                    org.lwjgl.opengl.GL30.glBindFramebuffer(org.lwjgl.opengl.GL30.GL_FRAMEBUFFER, savedFbo[0]);
                    org.lwjgl.opengl.GL13.glActiveTexture(savedActiveTexture[0]);
                    org.lwjgl.opengl.GL11.glMatrixMode(org.lwjgl.opengl.GL11.GL_MODELVIEW);
                    org.lwjgl.opengl.GL11.glPopMatrix();
                    org.lwjgl.opengl.GL11.glMatrixMode(org.lwjgl.opengl.GL11.GL_PROJECTION);
                    org.lwjgl.opengl.GL11.glPopMatrix();
                    org.lwjgl.opengl.GL11.glPopAttrib();
                } catch (Throwable fortressRestoreFailure) {
                }
            }
        }
        if (NativeBridge.isBadlion189Runtime()
                || NativeBridge.isCheatBreaker189Runtime()) {
            long now = System.nanoTime();
            long lastTick = NativeBridge.isCheatBreaker189Runtime()
                    ? lastCheatBreakerGuiTickNanos : lastBadlionGuiTickNanos;
            if (!SonyVegas.INSTANCE.getModManager()
                    .getMod(ClientSettings.class).isInputEnabled()
                    && now - lastTick >= 50000000L) {
                if (NativeBridge.isCheatBreaker189Runtime()) {
                    lastCheatBreakerGuiTickNanos = now;
                } else {
                    lastBadlionGuiTickNanos = now;
                }
                SonyVegas.INSTANCE.getModManager()
                        .getMod(ClientSettings.class).onTick();
            }
            GuiScreenNativeCallbackBridge.drawScreen(
                    null, 0, 0, 0.0f);
        }
    }


    @Override
    public EventListeners getListeners() {
        return EVENT_LISTENERS;
    }

    public int getDisplayWidth() {
        return displayWidth;
    }

    public int getDisplayHeight() {
        return displayHeight;
    }
}

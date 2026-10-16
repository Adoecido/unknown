package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.asm.transform.impl.MinecraftMouseActionTransformer;
import vegas.sony.asm.transform.impl.PlayerControllerMPTransformer;
import vegas.sony.event.impl.EventChatMessageRender;
import vegas.sony.event.impl.EventFogDensity;
import vegas.sony.event.impl.EventPreRenderLivingSpecials;
import vegas.sony.event.impl.EventRender2D;
import vegas.sony.runtime.NativeBridge;
import vegas.sony.wrapper.impl.ForgeVersion;

public class PrimaryMappingTaskSet
extends MappingTaskSet {
    private static boolean E;
    private boolean S;
    private boolean i;

    public static void B(boolean bl) {
        E = bl;
    }

    public PrimaryMappingTaskSet() {
        JavassistMappingTask.p(EventPreTickCallback.class);
        JavassistMappingTask.p(EventPostTickCallback.class);
        JavassistMappingTask.p(EventRenderPlayerPreCallback.class);
        JavassistMappingTask.p(EventRenderPlayerPostCallback.class);
        JavassistMappingTask.p(EventPreRenderEntityCallback.class);
        JavassistMappingTask.p(EventRender2D.class);
        JavassistMappingTask.p(EventFogDensity.class);
        JavassistMappingTask.p(RenderBatchFlushCallbackMarker.class);
        JavassistMappingTask.p(InsertedCallbackLockMarker.class);
        if (ForgeVersion.MC_1_20_6.d()) {
            JavassistMappingTask.p(EventChatMessageRender.class);
        }
        if (ForgeVersion.c() == ForgeVersion.MC_1_21_11.i()) {
            JavassistMappingTask.p(EventPreRenderLivingSpecials.class);
        }
    }

    private void addBadlionRenderTask() {
        if (NativeBridge.isBadlion189Runtime()) {
            this.D.add(new Badlion189Render2DMappingTask());
        }
    }

    static {
        if (PrimaryMappingTaskSet.K()) {
            PrimaryMappingTaskSet.B(true);
        }
    }

    public void X() {
        CheatBreakerInventoryCompatibility.register(this.D);
        this.D.add(new MinecraftMouseActionTransformer());
        this.D.add(new MinecraftTickEventMappingTask());
        this.D.add(new EntityRendererEventMappingTask());
        if (ForgeVersion.MC_1_21_4.d()) {
            this.D.add(new RenderTickEventMappingTask());
        }
        this.D.add(new PlayerTickEventMappingTask());
        this.D.add(new EntityLivingBaseEventMappingTask());
        this.D.add(new EntityBoundingBoxFilterMappingTask());
        this.D.add(new LegacyWorldEntityJoinEventMappingTask());
        this.D.add(new EntityPlayerSPEventMappingTask());
        this.D.add(new LocalPlayerTickClassTransformer());
        if (!ForgeVersion.MC_1_8_9.S().y()) {
            this.D.add(new RenderPlayerEventMappingTask());
        }
        if (ForgeVersion.c() <= ForgeVersion.MC_1_16_5_ACTUAL.i()) {
            this.D.add(new CapeTextureOverrideMappingTask());
            this.D.add(new CapeVisibilityOverrideMappingTask());
            if (NativeBridge.isBadlion189Runtime()) {
                this.D.add(new BadlionCapeRenderMappingTask());
                this.D.add(new BadlionCapeLayerMappingTask());
                this.D.add(new BadlionCapeModelMappingTask());
            }
        }
        this.D.add(new EntityRenderStateMappingTask());
        this.D.add(new PlayerControllerMPEventMappingTask());
        this.D.add(new PlayerControllerMPTransformer());
        this.D.add(new ScoreboardScoresEventMappingTask());
        if (ForgeVersion.MC_1_21_4.v()) {
            this.D.add(new RenderManagerEntityMappingTask());
        }
        if (ForgeVersion.MC_1_7_10.L() && !SonyVegas.INSTANCE.isForgeAbsent()) {
            this.D.add(new LegacyEntityRenderPreEventMappingTask());
        }
        if (ForgeVersion.MC_1_12_2.B()) {
            this.D.add(new RenderLivingBaseEventMappingTask());
        }
        if (ForgeVersion.MC_1_16_5.d()) {
            this.D.add(new RenderHandEventMappingTask());
            this.D.add(new WorldEntityJoinEventMappingTask());
            this.D.add(new Render3DEventMappingTask());
            if (ForgeVersion.MC_1_21_4.v()) {
                this.D.add(new FogDensityEventMappingTask());
            }
        } else {
            this.D.add(new LegacyRenderStringHookMappingTask());
        }
        this.D.add(new NetworkPacketEventMappingTask());
        if (ForgeVersion.MC_1_7_10.L()) {
            this.D.add(new EntityClientPlayerMPMotionMappingTask(MappedClasses.DI));
        }
        if (ForgeVersion.MC_1_8_9.L()) {
            this.D.add(new ItemRendererFirstPersonEventMappingTask());
            this.D.add(new WorldTimeEventMappingTask());
        }
        if (ForgeVersion.MC_1_16_5.d()) {
            this.D.add(new PlayerTabOverlayDisplayNameMappingTask());
        } else if (ForgeVersion.MC_1_8_9.d()) {
            this.D.add(new PlayerTabOverlayDisplayNameLegacyMappingTask());
        }
        // This task only changes the post-1.17 tooltip implementation.  On
        // legacy clients it serializes an untouched ItemStack class and asks
        // JVMTI to redefine it anyway, which is what produced "-1 19" on
        // CheatBreaker 1.7.10.
        if (ForgeVersion.MC_1_17.d()) {
            this.D.add(new ItemStackTooltipMappingTask());
        }
        if (ForgeVersion.MC_1_21_0.d()) {
            this.D.add(new EntityRenderPreEventMappingTask());
        }
        if (ForgeVersion.MC_1_20_6.d()) {
            this.D.add(new Render2DStaticCallbackMappingTask());
        } else {
            this.D.add(new ScoreboardObjectiveRenderMappingTask());
        }
        if (ForgeVersion.MC_1_21_4.d()) {
            this.D.add(new RenderWorldPassEventMappingTask());
        }
        if (ForgeVersion.MC_1_21_6.d()) {
            this.D.add(new GlStateManagerBlendFuncMappingTask());
        }
        this.D.add(new ThreadBoundTickEventMappingTask());
        if (ForgeVersion.MC_1_20_6.d()) {
            this.D.add(new ChatMessageRenderMappingTask());
        }
        if (ForgeVersion.MC_1_21_11.L() || ForgeVersion.MC_1_8_9.L()) {
            this.D.add(new LivingSpecialsRenderMappingTask());
        }
        if (ForgeVersion.MC_1_21_11.d()) {
            this.D.add(new KeyBindingStateEventMappingTask());
        }
        if (NativeBridge.isBadlion189Runtime()) {
            this.addBadlionRenderTask();
        }
        if (NativeBridge.isLunar189Runtime()) {
            this.D.add(new Lunar189Render2DMappingTask());
        }
    }

    public static boolean K() {
        boolean bl = PrimaryMappingTaskSet.Y$src$Z$1vg5zod();
        return !bl;
    }

    public static boolean Y$src$Z$1vg5zod() {
        return E;
    }

    public void f() {
        if (this.i) {
            return;
        }
        this.D.add(new EntityMoveEventMappingTask());
        this.d();
        this.i = true;
    }

    public boolean Q() {
        return this.S;
    }

    public void Y() {
        if (ForgeVersion.MC_1_7_10.L()) {
            this.D.add(new ChunkRenderRebuildEventMappingTask());
            this.D.add(new RenderBlocksEventMappingTask());
        } else if (ForgeVersion.MC_1_16_5.v()) {
            this.D.add(new VisGraphXRayVisibilityMappingTask());
            this.D.add(new BlockRenderColorOpacityMappingTask());
            this.D.add(new BlockModelRenderEventMappingTask());
            this.D.add(new BlockRenderLayerEventMappingTask());
            this.D.add(new BlockLayerOverrideModernEventMappingTask());
            this.D.add(new BlockLayerOverrideEventMappingTask());
        }
        this.d();
        this.S = true;
    }


    public void L() {
        RenderWorldPassExecutorDrainMappingTask renderWorldPassExecutorDrainMappingTask = new RenderWorldPassExecutorDrainMappingTask();
        this.D.add(renderWorldPassExecutorDrainMappingTask);
    }
}


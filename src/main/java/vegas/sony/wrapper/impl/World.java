package vegas.sony.wrapper.impl;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.mappings.MWorld;
import vegas.sony.wrapper.Wrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class World
extends Wrapper {
    private static int[] w;

    public ChunkWorldBridge C() {
        if (ForgeVersion.MC_1_16_5.d()) {
            return null;
        }
        return new ChunkWorldBridge(World.sonyVegasInstance.getMappings().Cy.g(this.I));
    }

    public float V() {
        return World.sonyVegasInstance.getMappings().Cy.a(this.I);
    }

    private List P() {
        Iterable iterable = World.sonyVegasInstance.getMappings().Cy.m(this.I);
        ArrayList arrayList = new ArrayList();
        for (Object t : iterable) {
            arrayList.add(t);
        }
        return arrayList;
    }

    public Object q() {
        return World.sonyVegasInstance.getMappings().Cy.y(this.I);
    }

    public RayTraceResult K(Vec3 vec3, Vec3 vec32, boolean bl, boolean bl2, boolean bl3, Entity entity) {
        if (ForgeVersion.MC_1_16_5.d()) {
            RayTraceContext$FluidMode blockMode = bl2 ? RayTraceContext$FluidMode.collider() : (bl3 ? RayTraceContext$FluidMode.visual() : RayTraceContext$FluidMode.outline());
            RayTraceContext$BlockMode fluidMode = bl ? RayTraceContext$BlockMode.any() : RayTraceContext$BlockMode.none();
            RayTraceContext rayTraceContext = RayTraceContext.b(vec3, vec32, blockMode, fluidMode, entity);
            return new RayTraceResult(World.sonyVegasInstance.getMappings().Cy.i(this.I, rayTraceContext.getObject()));
        }
        return new RayTraceResult(World.sonyVegasInstance.getMappings().Cy.l(this.I, vec3.getObject(), vec32.getObject(), bl, bl2, bl3));
    }

    private static UnsupportedOperationException a(UnsupportedOperationException unsupportedOperationException) {
        return unsupportedOperationException;
    }

    public List X() {
        if (ForgeVersion.MC_1_16_5.d()) {
            return this.u();
        }
        return World.sonyVegasInstance.getMappings().Cy.Q(this.I);
    }

    public void T(float f) {
        World.sonyVegasInstance.getMappings().Cy.M(this.I, f);
    }

    public int R() {
        if (ForgeVersion.MC_1_21_4.d()) {
            return MWorld.i(World.sonyVegasInstance.getMappings().Cy, this.I);
        }
        return 0;
    }

    private static boolean lambda$getEntitiesWithinAABBExcludingEntity$0(Object object) {
        return true;
    }

    public void o(float f) {
        World.sonyVegasInstance.getMappings().Cy.L(this.I, f);
    }

    private static boolean lambda$getEntitiesWithinAABBExcludingEntity$2(Object object) {
        return true;
    }

    public GlStateManager$BlendState E() {
        Object object = World.sonyVegasInstance.getMappings().Cy.j(this.I);
        if (MappedClasses.I.equals(object.getClass())) {
            return new GlStateManager$BlendState(object);
        }
        return null;
    }

    public List i(Entity entity, AxisAlignedBB axisAlignedBB) {
        ArrayList<AxisAlignedBB> arrayList = new ArrayList<AxisAlignedBB>();
        if (ForgeVersion.MC_1_20_6.d()) {
            Iterable iterable = (Iterable)World.sonyVegasInstance.getMappings().Cy.y(this.I, entity.getObject(), axisAlignedBB.getObject());
            for (Object t : iterable) {
                EntityFishHook entityFishHook = new EntityFishHook(t);
                arrayList.add(new AxisAlignedBB(entityFishHook.n().getObject()));
            }
            return arrayList;
        }
        if (ForgeVersion.MC_1_16_5.d()) {
            Stream stream = (Stream)World.sonyVegasInstance.getMappings().Cy.y(this.I, entity.getObject(), axisAlignedBB.getObject());
            for (Object object : stream.toArray()) {
                EntityFishHook entityFishHook = new EntityFishHook(object);
                arrayList.add(new AxisAlignedBB(entityFishHook.n().getObject()));
            }
            return arrayList;
        }
        return (List)World.sonyVegasInstance.getMappings().Cy.y(this.I, entity.getObject(), axisAlignedBB.getObject());
    }

    public boolean M(Entity entity, AxisAlignedBB axisAlignedBB) {
        if (ForgeVersion.MC_1_21_4.d()) {
            return MWorld.x(World.sonyVegasInstance.getMappings().Cy, this.I, entity.getObject(), axisAlignedBB.getObject());
        }
        return false;
    }

    public boolean z(Entity entity, AxisAlignedBB axisAlignedBB, BiPredicate biPredicate) {
        return World.sonyVegasInstance.getMappings().Cy.u(this.I, entity.getObject(), axisAlignedBB.getObject(), biPredicate);
    }

    public BlockStateWorldBridge o(BlockPos blockPos) {
        return new BlockStateWorldBridge(World.sonyVegasInstance.getMappings().Cy.v(this.I, blockPos.getObject()));
    }

    public int A(int n, int n2, int n3) {
        return MWorld.X(World.sonyVegasInstance.getMappings().Cy, this.I, n, n2, n3);
    }

    public Biome Y(BlockPos blockPos) {
        return new Biome(World.sonyVegasInstance.getMappings().Cy.a(this.I, blockPos.getObject()));
    }

    public boolean j$src$Z$11aji0a(BlockPos blockPos) {
        if (ForgeVersion.MC_1_17.d()) {
            return World.sonyVegasInstance.getMappings().Cy.d(this.I, blockPos.getX() >> 4, blockPos.getZ() >> 4);
        }
        return World.sonyVegasInstance.getMappings().Cy.r(this.I, blockPos.getObject());
    }

    public Chunk j(BlockPos blockPos) {
        return new Chunk(World.sonyVegasInstance.getMappings().Cy.Z(this.I, blockPos.getObject()));
    }

    public ChunkProvider getChunkProvider() {
        return new ChunkProvider(World.sonyVegasInstance.getMappings().Cy.E(this.I));
    }

    public List z() {
        if (ForgeVersion.MC_1_16_5.d()) {
            return this.P();
        }
        return World.sonyVegasInstance.getMappings().Cy.y$src$Ljava_util_List_$1xik31o(this.I);
    }

    public float N() {
        return World.sonyVegasInstance.getMappings().Cy.X(this.I);
    }

    public Block getBlockByPos(int n, int n2, int n3) {
        if (ForgeVersion.MC_1_7_10.L()) {
            return new Block(World.sonyVegasInstance.getMappings().Cy.U(this.I, n, n2, n3));
        }
        BlockPos blockPos = BlockPos.create(n, n2, n3);
        BlockState blockState = new BlockState(World.sonyVegasInstance.getMappings().Cy.u(this.I, blockPos.getObject()));
        return blockState.getBlock();
    }

    public Entity V(int n) {
        return new Entity(MWorld.b(World.sonyVegasInstance.getMappings().Cy, this.I, n));
    }

    public static int[] a() {
        return w;
    }

    public boolean h(AxisAlignedBB axisAlignedBB, Material material) {
        return World.sonyVegasInstance.getMappings().Cy.H(this.getObject(), axisAlignedBB.getObject(), material.getObject());
    }

    public void f(float f) {
        World.sonyVegasInstance.getMappings().Cy.R(this.I, f);
    }

    static {
        World.F(null);
    }

    public static void F(int[] nArray) {
        w = nArray;
    }

    public void i(long l) {
        if (ForgeVersion.MC_1_16_5.v()) {
            return;
        }
        World.sonyVegasInstance.getMappings().Cy.V(this.I, l);
    }

    public WorldProvider getWorldProvider() {
        return new WorldProvider(MWorld.getWorldProvider(World.sonyVegasInstance.getMappings().Cy, this.I));
    }

    public float n() {
        return World.sonyVegasInstance.getMappings().Cy.Z(this.I);
    }

    public void M(Entity entity) {
        if (ForgeVersion.MC_1_17.d()) {
            entity.k();
            MWorld.a(World.sonyVegasInstance.getMappings().Cy, this.I, entity.S(), EntityRemovalReason.P().getObject());
            return;
        }
        if (ForgeVersion.MC_1_16_5.d()) {
            entity.k();
            MWorld.c(World.sonyVegasInstance.getMappings().Cy, this.I, entity.getObject());
            return;
        }
        MWorld.c(World.sonyVegasInstance.getMappings().Cy, this.I, entity.getObject());
    }

    private List u() {
        Iterable iterable = World.sonyVegasInstance.getMappings().Cy.m(this.I);
        ArrayList arrayList = new ArrayList();
        for (Object t : iterable) {
            if (!MappedClasses.Yl.isInstance(t)) continue;
            arrayList.add(t);
        }
        return arrayList;
    }

    public RegistryAccess e() {
        if (ForgeVersion.MC_1_21_4.d()) {
            return new RegistryAccess(World.sonyVegasInstance.getMappings().Cy.u(this.I));
        }
        throw new UnsupportedOperationException("unavailable in this version");
    }

    public float y() {
        return World.sonyVegasInstance.getMappings().Cy.w(this.I);
    }

    public LevelEntityGetter getEntityGetter() {
        return new LevelEntityGetter(World.sonyVegasInstance.getMappings().Cy.getEntityGetter(this.I));
    }

    public BlockState getBlockState(BlockPos blockPos) {
        return new BlockState(World.sonyVegasInstance.getMappings().Cy.u(this.I, blockPos.getObject()));
    }

    public List R$src$Ljava_util_List_$1ycbpra() {
        if (ForgeVersion.MC_1_17.d()) {
            ArrayList arrayList = new ArrayList();
            ArrayList<Chunk> arrayList2 = new ArrayList<Chunk>();
            AtomicReferenceArray atomicReferenceArray = this.E().X().getChunks();
            int n = 0;
            while (n < atomicReferenceArray.length()) {
                Object chunkHandle;
                if ((chunkHandle = atomicReferenceArray.get(n++)) == null) continue;
                arrayList2.add(new Chunk(chunkHandle));
            }
            for (Chunk chunk : arrayList2) {
                arrayList.addAll(chunk.l().values());
            }
            return arrayList;
        }
        return World.sonyVegasInstance.getMappings().Cy.j$src$Ljava_util_List_$jm6ihn(this.I);
    }

    public List F(Entity entity, AxisAlignedBB axisAlignedBB) {
        if (ForgeVersion.MC_1_20_6.d()) {
            Predicate<Object> predicate = World::lambda$getEntitiesWithinAABBExcludingEntity$0;
            return World.sonyVegasInstance.getMappings().Cy.T(this.I, entity.getObject(), axisAlignedBB.getObject(), predicate);
        }
        if (ForgeVersion.MC_1_16_5.d()) {
            com.google.common.base.Predicate predicate = World::lambda$getEntitiesWithinAABBExcludingEntity$1;
            return World.sonyVegasInstance.getMappings().Cy.T(this.I, entity.getObject(), axisAlignedBB.getObject(), ForgeVersion.MC_1_17.d() ? predicate : null);
        }
        if (ForgeVersion.MC_1_12_2.d()) {
            com.google.common.base.Predicate predicate = World::lambda$getEntitiesWithinAABBExcludingEntity$2;
            return World.sonyVegasInstance.getMappings().Cy.T(this.I, entity.getObject(), axisAlignedBB.getObject(), ForgeVersion.MC_1_17.d() ? predicate : null);
        }
        return World.sonyVegasInstance.getMappings().Cy.K(this.I, entity.getObject(), axisAlignedBB.getObject());
    }

    public List i(Entity entity, AxisAlignedBB axisAlignedBB, Object object) {
        return World.sonyVegasInstance.getMappings().Cy.X(this.I, entity.getObject(), axisAlignedBB.getObject(), object);
    }

    public World(Object object) {
        super(object);
    }

    private static boolean lambda$getEntitiesWithinAABBExcludingEntity$1(Object object) {
        return true;
    }

    public boolean I() {
        return MWorld.w(World.sonyVegasInstance.getMappings().Cy, this.I);
    }

    public Chunk z(int n, int n2) {
        return new Chunk(MWorld.V(World.sonyVegasInstance.getMappings().Cy, this.I, n, n2));
    }

    public TileEntity G(BlockPos blockPos) {
        return new TileEntity(World.sonyVegasInstance.getMappings().Cy.j(this.I, blockPos.getObject()));
    }

    public void g(float f) {
        World.sonyVegasInstance.getMappings().Cy.D(this.I, f);
    }

    public Chunk P(int n, int n2) {
        if (ForgeVersion.MC_1_7_10.Y()) {
            return this.j(BlockPos.create(n, 0, n2));
        }
        return new Chunk(World.sonyVegasInstance.getMappings().Cy.H(this.I, n, n2));
    }

    public Block getBlock(double d, double d2, double d3) {
        if (ForgeVersion.MC_1_7_10.L()) {
            return new Block(World.sonyVegasInstance.getMappings().Cy.U(this.I, (int)d, (int)d2, (int)d3));
        }
        BlockPos blockPos = BlockPos.D(d, d2, d3);
        BlockState blockState = new BlockState(World.sonyVegasInstance.getMappings().Cy.u(this.I, blockPos.getObject()));
        return blockState.getBlock();
    }

    public void Z(int n, int n2, int n3, int n4, int n5, int n6) {
        MWorld.f(World.sonyVegasInstance.getMappings().Cy, this.I, n, n2, n3, n4, n5, n6);
    }

    public boolean M(int n, int n2, int n3) {
        return MWorld.v(World.sonyVegasInstance.getMappings().Cy, this.I, n, n2, n3);
    }

    public boolean o(int n, int n2, int n3, int n4, int n5, int n6) {
        if (ForgeVersion.MC_1_7_10.L()) {
            return MWorld.q(World.sonyVegasInstance.getMappings().Cy, this.I, n, n2, n3, n4, n5, n6);
        }
        return MWorld.K(World.sonyVegasInstance.getMappings().Cy, this.I, BlockPos.create(n, n2, n3), BlockPos.create(n4, n5, n6));
    }

    public Scoreboard getScoreboard() {
        if (World.sonyVegasInstance.getMappings().Cy.scoreboardMethod == null
                || World.sonyVegasInstance.getMappings().Cy.scoreboardMethod.hasResolutionFailed()) {
            return null;
        }
        return new Scoreboard(MWorld.getScoreboard(World.sonyVegasInstance.getMappings().Cy, this.I));
    }

    public BlockRayTraceResult r(RayTraceContextFactory rayTraceContextFactory) {
        if (ForgeVersion.MC_1_20_6.d()) {
            return new BlockRayTraceResult(World.sonyVegasInstance.getMappings().Cy.t(this.I, rayTraceContextFactory.getObject()));
        }
        throw new UnsupportedOperationException("Clip method unavailable in this version");
    }
}

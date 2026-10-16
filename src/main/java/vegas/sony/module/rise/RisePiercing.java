package vegas.sony.module.rise;

import java.util.List;
import vegas.sony.SonyVegas;
import vegas.sony.event.EventHandler;
import vegas.sony.event.EventPriority;
import vegas.sony.event.impl.EventClickMouse;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.module.Category;
import vegas.sony.module.Mod;
import vegas.sony.module.combat.Reach;
import vegas.sony.rotation.RotationManager;
import vegas.sony.wrapper.impl.AxisAlignedBB;
import vegas.sony.wrapper.impl.Entity;
import vegas.sony.wrapper.impl.EntityLivingBase;
import vegas.sony.wrapper.impl.EntityPlayerSP;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.RayTraceResult;
import vegas.sony.wrapper.impl.Vec3;
import vegas.sony.wrapper.impl.WorldClient;

public final class RisePiercing extends Mod {
    public RisePiercing() {
        super("RisePiercing //$ui", 9133823, Category.COMBAT,
                "Rise 6.9.5 entity raycast that can pass through blocks //$ui");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(EventClickMouse event) {
        EntityPlayerSP player = Minecraft.thePlayer();
        WorldClient world = Minecraft.theWorld();
        if (player.isNull() || world.isNull()) {
            return;
        }
        RayTraceResult current = Minecraft.p$src$Lvegas_sony_wrapper_impl_RayTraceResult_$5rw6n0();
        if (current.isNotNull() && current.isEntityHit()) {
            return;
        }
        RayTraceResult piercingHit = this.rayTraceEntities(player, this.getReachRange(),
                this.getHitBoxExpand());
        if (piercingHit != null && piercingHit.isNotNull() && piercingHit.isEntityHit()) {
            Minecraft.O(piercingHit);
            Entity hit = piercingHit.getEntity();
            if (hit.isNotNull() && hit.isInstance(MappedClasses.zm)) {
                Minecraft.r(new EntityLivingBase(hit.getObject()));
            }
        }
    }

    private RayTraceResult rayTraceEntities(EntityPlayerSP player, double range, float expand) {
        float yaw = RotationManager.INSTANCE.getManagedYaw();
        float pitch = RotationManager.INSTANCE.getManagedPitch();
        Vec3 origin = Vec3.create(player.z(), player.N() + (double)player.X(), player.h());
        double yawRadians = Math.toRadians(yaw);
        double pitchRadians = Math.toRadians(pitch);
        double cosPitch = Math.cos(pitchRadians);
        Vec3 direction = Vec3.create(-Math.sin(yawRadians) * cosPitch,
                -Math.sin(pitchRadians), Math.cos(yawRadians) * cosPitch);
        Vec3 end = origin.addVector(direction.getX() * range, direction.getY() * range,
                direction.getZ() * range);
        AxisAlignedBB search = player.R$src$Lvegas_sony_wrapper_impl_AxisAlignedBB_$r19dfl()
                .addCoord(direction.getX() * range, direction.getY() * range,
                        direction.getZ() * range)
                .expand(1.0 + expand, 1.0 + expand, 1.0 + expand);
        List candidates = Minecraft.theWorld().F(player, search);
        EntityLivingBase best = null;
        Vec3 bestHit = null;
        double bestDistance = range;
        for (Object handle : candidates) {
            Entity entity = new Entity(handle);
            if (!entity.isInstance(MappedClasses.zm) || entity.equals(player)
                    || !entity.n$src$Z$fx7gig()) {
                continue;
            }
            EntityLivingBase living = new EntityLivingBase(handle);
            if (living.M$src$Z$ff28xj() || living.w$src$F$15l9epb() <= 0.0f) {
                continue;
            }
            float border = living.b() + expand;
            AxisAlignedBB box = living.R$src$Lvegas_sony_wrapper_impl_AxisAlignedBB_$r19dfl()
                    .expand(border, border, border);
            RayTraceResult intercept = box.calculateIntercept(origin, end);
            Vec3 hitVec = null;
            double distance = Double.POSITIVE_INFINITY;
            if (box.isVecInside(origin)) {
                hitVec = origin;
                distance = 0.0;
            } else if (intercept != null && intercept.isNotNull()) {
                hitVec = intercept.getHitVec();
                distance = origin.distanceTo(hitVec);
            }
            if (hitVec != null && distance <= bestDistance) {
                best = living;
                bestHit = hitVec;
                bestDistance = distance;
            }
        }
        return best == null ? new RayTraceResult(null) : RayTraceResult.create(best, bestHit);
    }

    public double getReachRange() {
        Reach reach = SonyVegas.INSTANCE.getModManager().getMod(Reach.class);
        return reach != null && reach.isEnabled()
                ? Math.max(3.0, reach.getMaximumConfiguredReachDistance()) : 3.0;
    }

    public float getHitBoxExpand() {
        return 0.0f;
    }
}

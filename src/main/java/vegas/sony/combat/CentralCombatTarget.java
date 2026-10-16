package vegas.sony.combat;

import vegas.sony.SonyVegas;
import vegas.sony.module.combat.SilentAura;
import vegas.sony.module.gladiador.Target;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.utils.RotationUtil;
import vegas.sony.wrapper.impl.Entity;
import vegas.sony.wrapper.impl.EntityLivingBase;
import vegas.sony.wrapper.impl.EntityPlayerSP;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.RayTraceResult;

/** Resolves the single combat target shared by range-oriented modules. */
public final class CentralCombatTarget {
    private static final long STICKY_TARGET_MILLIS = 5000L;
    private static final double AUTO_RANGE = 12.0;
    private static Object stickyTargetHandle;
    private static long stickyTargetUntil;

    private CentralCombatTarget() {
    }

    public static EntityLivingBase get() {
        Target targetModule = SonyVegas.INSTANCE.getModManager().getMod(Target.class);
        if (targetModule != null && targetModule.isEnabled()) {
            EntityLivingBase target = valid(targetModule.getCurrentTarget());
            if (target != null) return remember(target);
        }
        SilentAura aura = SonyVegas.INSTANCE.getModManager().getMod(SilentAura.class);
        if (aura != null && aura.isEnabled()) {
            EntityLivingBase target = valid(aura.getTarget());
            if (target != null) return remember(target);
        }
        EntityLivingBase mouseTarget = mouseTarget();
        if (mouseTarget != null) return remember(mouseTarget);
        EntityLivingBase sticky = valid(stickyTargetHandle == null ? null : new EntityLivingBase(stickyTargetHandle));
        if (sticky != null && System.currentTimeMillis() <= stickyTargetUntil
                && !Minecraft.thePlayer().isNull()
                && Minecraft.thePlayer().getDistanceToEntity(sticky) <= AUTO_RANGE) return sticky;
        EntityLivingBase centered = centeredTarget();
        return centered == null ? null : remember(centered);
    }

    public static void record(Entity entity) {
        if (entity == null || entity.isNull() || !entity.isInstance(MappedClasses.zm)) return;
        remember(valid(new EntityLivingBase(entity.getObject())));
    }

    private static EntityLivingBase mouseTarget() {
        if (Minecraft.thePlayer().isNull() || Minecraft.theWorld().isNull()) return null;
        RayTraceResult hit = Minecraft.p$src$Lvegas_sony_wrapper_impl_RayTraceResult_$5rw6n0();
        if (hit == null || hit.isNull()) return null;
        Entity entity = hit.getEntity();
        if (entity.isNull() || !entity.isInstance(MappedClasses.zm)
                || entity.getObject() == Minecraft.thePlayer().getObject()) return null;
        return valid(new EntityLivingBase(entity.getObject()));
    }

    private static EntityLivingBase centeredTarget() {
        if (Minecraft.thePlayer().isNull() || Minecraft.theWorld().isNull()) return null;
        EntityPlayerSP player = Minecraft.thePlayer();
        EntityLivingBase best = null;
        int bestAngle = Integer.MAX_VALUE;
        for (Object handle : Minecraft.theWorld().z()) {
            try {
                Entity entity = new Entity(handle);
                if (!entity.isInstance(MappedClasses.Yl) || handle == player.getObject()) continue;
                EntityLivingBase living = valid(new EntityLivingBase(handle));
                if (living == null || player.getDistanceToEntity(living) > AUTO_RANGE) continue;
                int angle = RotationUtil.a(player, living);
                if (angle > 60 || angle >= bestAngle) continue;
                bestAngle = angle;
                best = living;
            } catch (Throwable ignored) {
            }
        }
        return best;
    }

    private static EntityLivingBase remember(EntityLivingBase target) {
        if (target == null) return null;
        stickyTargetHandle = target.getObject();
        stickyTargetUntil = System.currentTimeMillis() + STICKY_TARGET_MILLIS;
        return target;
    }

    public static EntityLivingBase valid(EntityLivingBase target) {
        return target == null || target.isNull() || target.isDead() ? null : target;
    }
}

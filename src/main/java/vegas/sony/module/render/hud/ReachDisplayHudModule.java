package vegas.sony.module.render.hud;

import vegas.sony.event.EventHandler;
import vegas.sony.event.impl.EventPostAttack;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.module.render.hud.HudModule;
import vegas.sony.module.render.hud.HudModuleGroup;
import vegas.sony.rotation.RotationManager;
import vegas.sony.ui.click.frame.impl.hud.ReachDisplayHudFrame;
import vegas.sony.wrapper.impl.AxisAlignedBB;
import vegas.sony.wrapper.impl.Entity;
import vegas.sony.wrapper.impl.EntityLivingBase;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.RayTraceResult;
import vegas.sony.wrapper.impl.Vec3;

public class ReachDisplayHudModule
extends HudModule {
    private float savedPitch;
    private long lastAttackTime;
    private float savedYaw;
    private float lastReach;

    public float getLastReach() {
        if (System.currentTimeMillis() - this.lastAttackTime >= 5000L) {
            this.lastReach = 0.0f;
            this.lastAttackTime = 0L;
        }
        return this.lastReach;
    }

    private void restorePlayerRotation() {
        RotationManager rotationManager = RotationManager.INSTANCE;
        if (rotationManager.hasAdaptiveController()) {
            EntityLivingBase entityLivingBase = Minecraft.F();
            entityLivingBase.H(this.savedYaw);
            entityLivingBase.C(this.savedPitch);
        }
    }

    @EventHandler
    public void onPostAttack(EventPostAttack event) {
        if (event.getTarget().isInstance(MappedClasses.zm) && !event.getTarget().isInstance(MappedClasses.FT) && Minecraft.p$src$Lvegas_sony_wrapper_impl_RayTraceResult_$5rw6n0().isNotNull()) {
            Entity entity = Minecraft.p$src$Lvegas_sony_wrapper_impl_RayTraceResult_$5rw6n0().getEntity();
            if (entity.isNull()) {
                return;
            }
            EntityLivingBase player = Minecraft.F();
            double reachDistance = Minecraft.playerController().N();
            this.applyManagedRotation();
            Vec3 eyePosition = player.O(1.0f);
            Vec3 lookDirection = player.J(1.0f);
            this.restorePlayerRotation();
            Vec3 rayEnd = eyePosition.addVector(
                    lookDirection.getX() * reachDistance,
                    lookDirection.getY() * reachDistance,
                    lookDirection.getZ() * reachDistance);
            float collisionBorder = entity.b();
            AxisAlignedBB targetBounds = entity.R$src$Lvegas_sony_wrapper_impl_AxisAlignedBB_$r19dfl()
                    .expand(collisionBorder, collisionBorder, collisionBorder);
            RayTraceResult intercept = targetBounds.calculateIntercept(eyePosition, rayEnd);
            if (intercept.isNull()) {
                return;
            }
            double attackDistance = eyePosition.distanceTo(intercept.getHitVec());
            this.lastReach = (float)attackDistance;
            this.lastAttackTime = System.currentTimeMillis();
        }
    }

    public ReachDisplayHudModule() {
        super("Reach Display //$ui", HudModuleGroup.HUD, "reach_display //$", ReachDisplayHudFrame.class);
        this.setSuffix("Shows how far away your last attack was //$ui");
    }


    private void applyManagedRotation() {
        RotationManager rotationManager = RotationManager.INSTANCE;
        if (rotationManager.hasAdaptiveController()) {
            EntityLivingBase entityLivingBase = Minecraft.F();
            this.savedYaw = entityLivingBase.J();
            this.savedPitch = entityLivingBase.V();
            entityLivingBase.H(rotationManager.getManagedYaw());
            entityLivingBase.C(rotationManager.getManagedPitch());
        }
    }
}

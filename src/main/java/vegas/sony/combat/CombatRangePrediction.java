package vegas.sony.combat;

import vegas.sony.wrapper.impl.AxisAlignedBB;
import vegas.sony.wrapper.impl.EntityLivingBase;
import vegas.sony.wrapper.impl.EntityPlayerSP;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.MovementInput;

/** Small deterministic movement predictor used by range modules. */
public final class CombatRangePrediction {
    private CombatRangePrediction() {
    }

    public static Result predict(EntityPlayerSP player, int ticks) {
        MovementInput input = player.movementInput();
        return predict(player, input.D(), input.T(), ticks);
    }

    public static Result predict(EntityPlayerSP player, float forward, float strafe, int ticks) {
        double x = player.z();
        double z = player.h();
        double motionX = finite(player.t()) ? player.t() : 0.0;
        double motionZ = finite(player.T()) ? player.T() : 0.0;
        int count = Math.max(1, Math.min(20, ticks));
        double yaw = Math.toRadians(player.J());
        double length = Math.sqrt(forward * forward + strafe * strafe);
        if (length > 1.0) {
            forward /= length;
            strafe /= length;
        }
        for (int tick = 0; tick < count; ++tick) {
            double acceleration = player.b$src$Z$fqlxe4() ? 0.10 : 0.02;
            motionX += (strafe * Math.cos(yaw) - forward * Math.sin(yaw)) * acceleration;
            motionZ += (forward * Math.cos(yaw) + strafe * Math.sin(yaw)) * acceleration;
            x += motionX;
            z += motionZ;
            motionX *= 0.91;
            motionZ *= 0.91;
        }
        double dx = x - player.z();
        double dz = z - player.h();
        boolean collision = !Minecraft.theWorld().i(player,
                player.R$src$Lvegas_sony_wrapper_impl_AxisAlignedBB_$r19dfl().A(dx, 0.0, dz)).isEmpty();
        return new Result(x, z, !collision && finite(x) && finite(z));
    }

    public static double distanceToTarget(double x, double y, double z, EntityLivingBase target) {
        AxisAlignedBB box = target.R$src$Lvegas_sony_wrapper_impl_AxisAlignedBB_$r19dfl();
        double closestX = clamp(x, box.getMinX(), box.getMaxX());
        double closestY = clamp(y, box.getMinY(), box.getMaxY());
        double closestZ = clamp(z, box.getMinZ(), box.getMaxZ());
        double dx = x - closestX;
        double dy = y - closestY;
        double dz = z - closestZ;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public static double currentDistance(EntityPlayerSP player, EntityLivingBase target) {
        return distanceToTarget(player.z(), player.N() + player.X() * 0.5, player.h(), target);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static boolean finite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }

    public static final class Result {
        public final double x;
        public final double z;
        public final boolean valid;

        private Result(double x, double z, boolean valid) {
            this.x = x;
            this.z = z;
            this.valid = valid;
        }
    }
}

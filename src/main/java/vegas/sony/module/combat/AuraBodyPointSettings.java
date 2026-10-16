package vegas.sony.module.combat;

import vegas.sony.module.Mod;
import vegas.sony.unmap.ModeOption;
import vegas.sony.value.ModeValue;
import vegas.sony.wrapper.impl.AxisAlignedBB;
import vegas.sony.wrapper.impl.EntityPlayerSP;

/** Body limits and crosshair height selection shared by both Auras. */
public final class AuraBodyPointSettings {
    private final ModeOption head = new ModeOption("Head //$ui");
    private final ModeOption neck = new ModeOption("Neck //$ui");
    private final ModeOption body = new ModeOption("Body //$ui");
    private final ModeOption feet = new ModeOption("Feet //$ui");
    private final ModeOption lowHead = new ModeOption("Head //$ui");
    private final ModeOption lowNeck = new ModeOption("Neck //$ui");
    private final ModeOption lowBody = new ModeOption("Body //$ui");
    private final ModeOption lowFeet = new ModeOption("Feet //$ui");
    private final ModeValue highest;
    private final ModeValue lowest;

    public AuraBodyPointSettings(Mod owner) {
        this.highest = ModeValue.create(owner, "Highest body point //$ui", this.head,
                this.head, this.neck, this.body, this.feet);
        this.lowest = ModeValue.create(owner, "Lowest body point //$ui", this.lowFeet,
                this.lowHead, this.lowNeck, this.lowBody, this.lowFeet);
    }

    public ModeValue getHighest() {
        return this.highest;
    }

    public ModeValue getLowest() {
        return this.lowest;
    }

    public double lowestRatio() {
        return Math.min(this.ratio(this.lowest.getValue()), this.ratio(this.highest.getValue()));
    }

    public double highestRatio() {
        return Math.max(this.ratio(this.lowest.getValue()), this.ratio(this.highest.getValue()));
    }

    private double ratio(Object point) {
        if (point == this.head || point == this.lowHead) return 0.9;
        if (point == this.neck || point == this.lowNeck) return 0.8;
        if (point == this.body || point == this.lowBody) return 0.5;
        return 0.1;
    }

    public double[] aimPoint(EntityPlayerSP player, AxisAlignedBB box) {
        double height = box.getMaxY() - box.getMinY();
        return pointFromView(player.z(), player.N() + player.X(), player.h(), player.V(),
                (box.getMinX() + box.getMaxX()) * 0.5,
                (box.getMinZ() + box.getMaxZ()) * 0.5,
                box.getMinY() + height * this.lowestRatio(),
                box.getMinY() + height * this.highestRatio());
    }

    public static double[] pointFromView(double eyeX, double eyeY, double eyeZ, double pitch,
                                         double centerX, double centerZ, double minY, double maxY) {
        double horizontalDistance = Math.hypot(centerX - eyeX, centerZ - eyeZ);
        double safePitch = Math.max(-89.9, Math.min(89.9, pitch));
        double lookedY = eyeY - Math.tan(Math.toRadians(safePitch)) * horizontalDistance;
        return new double[]{centerX, Math.max(minY, Math.min(maxY, lookedY)), centerZ};
    }
}

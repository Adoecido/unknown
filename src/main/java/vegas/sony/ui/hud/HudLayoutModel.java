package vegas.sony.ui.hud;

import com.google.gson.JsonObject;

/** Resolution-independent edge anchors shared by the HUD renderers and editor. */
public final class HudLayoutModel {
    private HudLayoutModel() {}

    public static final class Placement {
        public final double x, y;
        public final boolean right, bottom;

        public Placement(double x, double y, boolean right, boolean bottom) {
            this.x = clamp(x, 0.0, 1.0);
            this.y = clamp(y, 0.0, 1.0);
            this.right = right;
            this.bottom = bottom;
        }

        public JsonObject toJson() {
            JsonObject value = new JsonObject();
            value.addProperty("x", this.x);
            value.addProperty("y", this.y);
            value.addProperty("right", this.right);
            value.addProperty("bottom", this.bottom);
            return value;
        }
    }

    public static final class Bounds {
        public final double x, y, width, height;
        public final boolean right, bottom;

        public Bounds(double x, double y, double width, double height,
                boolean right, boolean bottom) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.right = right;
            this.bottom = bottom;
        }
    }

    public static Placement drag(double x, double y, double width, double height) {
        width = clamp(width, 0.0, 1.0);
        height = clamp(height, 0.0, 1.0);
        x = clamp(x, 0.0, 1.0 - width);
        y = clamp(y, 0.0, 1.0 - height);
        boolean right = x + width * 0.5 > 0.5;
        boolean bottom = y + height * 0.5 > 0.5;
        return new Placement(x + (right ? width : 0.0),
                y + (bottom ? height : 0.0), right, bottom);
    }

    public static Bounds resolve(Placement placement, double screenWidth,
            double screenHeight, double width, double height, double defaultX, double defaultY) {
        screenWidth = Math.max(1.0, screenWidth);
        screenHeight = Math.max(1.0, screenHeight);
        width = clamp(width, 1.0, screenWidth);
        height = clamp(height, 1.0, screenHeight);
        double x = placement == null ? defaultX
                : placement.x * screenWidth - (placement.right ? width : 0.0);
        double y = placement == null ? defaultY
                : placement.y * screenHeight - (placement.bottom ? height : 0.0);
        x = clamp(x, 0.0, screenWidth - width);
        y = clamp(y, 0.0, screenHeight - height);
        boolean right = placement == null ? x + width * 0.5 > screenWidth * 0.5 : placement.right;
        boolean bottom = placement == null ? y + height * 0.5 > screenHeight * 0.5 : placement.bottom;
        return new Bounds(x, y, width, height, right, bottom);
    }

    public static Placement read(JsonObject object) {
        try {
            double x = object.get("x").getAsDouble(), y = object.get("y").getAsDouble();
            if (!Double.isFinite(x) || !Double.isFinite(y)
                    || x < 0.0 || x > 1.0 || y < 0.0 || y > 1.0) return null;
            return new Placement(x, y, object.get("right").getAsBoolean(),
                    object.get("bottom").getAsBoolean());
        } catch (RuntimeException invalid) { return null; }
    }

    public static double rowX(Bounds bounds, double width, double progress) {
        progress = clamp(progress, 0.0, 1.0);
        return bounds.right ? bounds.x + bounds.width - width * progress
                : bounds.x - width * (1.0 - progress);
    }

    public static double clamp(double value, double minimum, double maximum) {
        return Double.isFinite(value) ? Math.max(minimum, Math.min(maximum, value)) : minimum;
    }
}

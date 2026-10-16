package vegas.sony.ui.hud;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import vegas.sony.SonyVegas;
import vegas.sony.event.impl.EventRender2D;
import vegas.sony.module.gladiador.Target;
import vegas.sony.module.render.ArrayListModule;
import vegas.sony.runtime.NativeBridge;
import vegas.sony.wrapper.impl.Minecraft;

/** Preview-only editor state. Never enables modules or changes combat targets. */
public final class HudLayout {
    public static final String ARRAY_LIST = "h0", TARGET_INFO = "h1", NOTIFICATIONS = "h2";
    private static final Gson GSON = new Gson();
    private static final Map<String, Shape> shapes = new ConcurrentHashMap<String, Shape>();
    private static volatile boolean editing;
    private static final float[] nativePositions = new float[6];
    private static int nativePositionFlags;
    private static volatile boolean nativeEditorAvailable;
    private static volatile boolean nativeEditing;

    private HudLayout() {}

    public static boolean isEditing() {
        return (nativeEditorAvailable ? nativeEditing : editing)
                && NativeBridge.isModuleMenuVisible();
    }
    public static void setEditing(boolean enabled) { editing = enabled; }

    /** Called once on the render thread before any HUD element draws. */
    public static void refreshPreviewPositions() {
        if (!NativeBridge.isModuleMenuVisible() && nativePositionFlags == 0) return;
        try {
            nativePositionFlags = NativeBridge.hps(nativePositions);
            nativeEditing = (nativePositionFlags & 8) != 0;
            nativeEditorAvailable = true;
        } catch (UnsatisfiedLinkError unavailable) {
            nativePositionFlags = 0;
            nativeEditorAvailable = false;
        }
    }

    private static JsonObject placements() {
        try {
            JsonObject value = GSON.fromJson(SonyVegas.INSTANCE.getPublicProfileSettings()
                    .hudLayout.getValue(), JsonObject.class);
            return value == null ? new JsonObject() : value;
        } catch (RuntimeException unavailable) { return new JsonObject(); }
    }

    public static HudLayoutModel.Bounds resolve(String id, double screenWidth, double screenHeight,
            double width, double height, double defaultX, double defaultY) {
        JsonObject positions = placements();
        HudLayoutModel.Placement placement = positions.has(id) && positions.get(id).isJsonObject()
                ? HudLayoutModel.read(positions.getAsJsonObject(id)) : null;
        int index = ARRAY_LIST.equals(id) ? 0 : TARGET_INFO.equals(id) ? 1
                : NOTIFICATIONS.equals(id) ? 2 : -1;
        if (index >= 0 && (nativePositionFlags & (1 << index)) != 0) {
            placement = HudLayoutModel.drag(nativePositions[index * 2],
                    nativePositions[index * 2 + 1], width / Math.max(1.0, screenWidth),
                    height / Math.max(1.0, screenHeight));
        }
        HudLayoutModel.Bounds bounds = HudLayoutModel.resolve(placement, screenWidth,
                screenHeight, width, height, defaultX, defaultY);
        shapes.put(id, new Shape(bounds.width / screenWidth, bounds.height / screenHeight,
                defaultX / screenWidth, defaultY / screenHeight));
        return bounds;
    }

    public static boolean move(String id, double x, double y) {
        return move(id, x, y, false);
    }

    public static boolean move(String id, double x, double y, boolean commit) {
        // The final request can arrive after the menu-close callback disables
        // editing. It still needs to apply and persist the last dragged position.
        if ((!editing && !commit) || !validId(id) || !Double.isFinite(x) || !Double.isFinite(y)
                || x < 0.0 || x > 1.0 || y < 0.0 || y > 1.0) return false;
        Shape shape = shapes.get(id);
        if (shape == null) return false;
        JsonObject positions = placements();
        positions.add(id, HudLayoutModel.drag(x, y, shape.width, shape.height).toJson());
        SonyVegas.INSTANCE.getPublicProfileSettings().hudLayout.setValue(positions.toString());
        return true;
    }

    private static boolean validId(String id) {
        return ARRAY_LIST.equals(id) || TARGET_INFO.equals(id) || NOTIFICATIONS.equals(id);
    }

    public static JsonObject captureState() {
        double width = Math.max(1.0, Minecraft.J() / 2.0);
        double height = Math.max(1.0, Minecraft.h() / 2.0);
        if (!shapes.containsKey(ARRAY_LIST))
            shapes.put(ARRAY_LIST, new Shape(Math.min(145.0 / width, 1.0),
                    Math.min(70.0 / height, 1.0), Math.max(0.0, 1.0 - 145.0 / width), 4.0 / height));
        if (!shapes.containsKey(TARGET_INFO))
            shapes.put(TARGET_INFO, new Shape(Math.min(118.0 / width, 1.0),
                    Math.min(22.0 / height, 1.0), 7.0 / width, 0.3));
        if (!shapes.containsKey(NOTIFICATIONS))
            shapes.put(NOTIFICATIONS, new Shape(Math.min(170.0 / width, 1.0),
                    Math.min(38.0 / height, 1.0), Math.max(0.0, 1.0 - 175.0 / width),
                    Math.max(0.0, 1.0 - 52.0 / height)));
        JsonObject positions = placements();
        JsonArray elements = new JsonArray();
        for (Map.Entry<String, Shape> entry : shapes.entrySet()) {
            Shape shape = entry.getValue();
            HudLayoutModel.Placement placement = positions.has(entry.getKey())
                    && positions.get(entry.getKey()).isJsonObject()
                    ? HudLayoutModel.read(positions.getAsJsonObject(entry.getKey())) : null;
            // Resolve in a common pixel space, then normalize only once.
            HudLayoutModel.Bounds bounds = HudLayoutModel.resolve(placement, 10000.0, 10000.0,
                    shape.width * 10000.0, shape.height * 10000.0,
                    shape.x * 10000.0, shape.y * 10000.0);
            JsonObject element = new JsonObject();
            element.addProperty("id", entry.getKey());
            element.addProperty("label", ARRAY_LIST.equals(entry.getKey()) ? "ArrayList //$ui"
                    : TARGET_INFO.equals(entry.getKey()) ? "Target Info //$ui" : "Notifications //$ui");
            element.addProperty("x", bounds.x / 10000.0);
            element.addProperty("y", bounds.y / 10000.0);
            element.addProperty("width", bounds.width / 10000.0);
            element.addProperty("height", bounds.height / 10000.0);
            element.addProperty("right", bounds.right);
            element.addProperty("bottom", bounds.bottom);
            elements.add(element);
        }
        JsonObject state = new JsonObject();
        state.addProperty("active", isEditing());
        state.add("elements", elements);
        return state;
    }

    public static void renderPreviews(EventRender2D event) {
        if (!isEditing()) return;
        ArrayListModule array = SonyVegas.INSTANCE.getModManager().getMod(ArrayListModule.class);
        Target target = SonyVegas.INSTANCE.getModManager().getMod(Target.class);
        if (array != null) array.renderLayoutPreview(event);
        if (target != null) target.renderLayoutPreview(event);
        SonyVegas.INSTANCE.getNotificationManager().renderLayoutPreview();
    }

    private static final class Shape {
        final double width, height, x, y;
        Shape(double width, double height, double x, double y) {
            this.width = width; this.height = height; this.x = x; this.y = y;
        }
    }
}

package vegas.sony.mapping;

import vegas.sony.wrapper.impl.AxisAlignedBB;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Supplies filtered entity bounding boxes without mutating Minecraft's entity
 * state. Maps are built on the client tick thread and then published as
 * immutable snapshots for the hot getEntityBoundingBox path.
 */
public final class EntityBoundingBoxFilterHook extends InsertedCallbackMarker {
    private static volatile Map<Object, Object> hideClansBoxes = Collections.emptyMap();
    private static volatile Map<Object, Object> targetBoxes = Collections.emptyMap();
    private static volatile Map<Object, Boolean> targetPreservedBoxes = Collections.emptyMap();

    private EntityBoundingBoxFilterHook() {
    }

    public static Object filter(Object entityHandle, Object originalBox) {
        if (targetPreservedBoxes.containsKey(entityHandle)) return originalBox;
        Object filtered = targetBoxes.get(entityHandle);
        if (filtered == null) {
            filtered = hideClansBoxes.get(entityHandle);
        }
        return filtered == null ? originalBox : filtered;
    }

    public static void updateHideClans(Collection<?> entityHandles) {
        hideClansBoxes = buildBoxes(entityHandles);
    }

    public static void clearHideClans() {
        hideClansBoxes = Collections.emptyMap();
    }

    public static void updateTarget(Collection<?> entityHandles) {
        updateTarget(entityHandles, Collections.emptyList());
    }

    public static void updateTarget(Collection<?> hiddenHandles, Collection<?> preservedHandles) {
        // Publish the ally exemption first so a previous Piercing snapshot cannot
        // briefly remove its hitbox while Target's current tick is updating.
        targetPreservedBoxes = buildPreserved(preservedHandles);
        targetBoxes = buildBoxes(hiddenHandles);
    }

    public static void clearTarget() {
        targetBoxes = Collections.emptyMap();
        targetPreservedBoxes = Collections.emptyMap();
    }

    private static Map<Object, Boolean> buildPreserved(Collection<?> handles) {
        if (handles == null || handles.isEmpty()) return Collections.emptyMap();
        IdentityHashMap<Object, Boolean> preserved = new IdentityHashMap<Object, Boolean>(handles.size());
        for (Object handle : handles) {
            if (handle != null) preserved.put(handle, Boolean.TRUE);
        }
        return preserved.isEmpty() ? Collections.emptyMap() : preserved;
    }

    private static Map<Object, Object> buildBoxes(Collection<?> entityHandles) {
        if (entityHandles == null || entityHandles.isEmpty()) {
            return Collections.emptyMap();
        }
        IdentityHashMap<Object, Object> boxes = new IdentityHashMap<Object, Object>(entityHandles.size());
        // A zero-volume box at the entity position is still ray-traceable when the
        // crosshair lands on that exact point (normally the player's feet).  NaN
        // coordinates make every AABB range/intercept comparison fail while this
        // hook is active, without mutating the entity's real bounding box.
        Object nonTargetableBox;
        try {
            nonTargetableBox = AxisAlignedBB.create(
                    Double.NaN, Double.NaN, Double.NaN,
                    Double.NaN, Double.NaN, Double.NaN).getObject();
        } catch (Throwable ignored) {
            return Collections.emptyMap();
        }
        for (Object handle : entityHandles) {
            if (handle == null) continue;
            try {
                boxes.put(handle, nonTargetableBox);
            } catch (Throwable ignored) {
            }
        }
        return boxes.isEmpty() ? Collections.emptyMap() : boxes;
    }
}

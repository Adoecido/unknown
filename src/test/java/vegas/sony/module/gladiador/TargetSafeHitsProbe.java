package vegas.sony.module.gladiador;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import vegas.sony.mapping.EntityBoundingBoxFilterHook;

/** Regression checks for Target's independent ally and pass-through rules. */
public final class TargetSafeHitsProbe {
    private TargetSafeHitsProbe() {
    }

    public static void main(String[] args) throws Exception {
        Target target = new Target();
        require(target.safeHits.areConditionsMet(), "Safe Hits is hidden in Manual mode");
        target.automatic.setPersistenceSuppressed(true);
        target.automatic.setValue(true);
        require(target.safeHits.areConditionsMet(), "Safe Hits is hidden in Automatic mode");

        require(!Target.shouldHideOtherPlayerModel(true, false, false, true),
                "Safe Hits hid a clan ally");
        require(!Target.shouldPassThroughOtherPlayer(true, false, false, true),
                "Safe Hits removed a clan ally's hitbox");
        require(!Target.shouldCancelOtherPlayerAttack(true, true),
                "Safe Hits cancelled a manual attack on a clan ally");
        require(Target.shouldCancelOtherPlayerAttack(true, false),
                "Safe Hits permitted a manual attack on an unrelated non-target player");
        require(Target.shouldHideOtherPlayerModel(false, false, false, true),
                "Safe Hits left an unrelated player visible without Show All Players");
        require(Target.shouldPassThroughOtherPlayer(false, false, false, true),
                "Safe Hits left an unrelated player's hitbox active");

        require(!Target.shouldHideOtherPlayerModel(true, true, false, true),
                "Show All Players hid a clan ally");
        require(!Target.shouldPassThroughOtherPlayer(true, true, false, true),
                "Show All Players removed a clan ally's hitbox");
        require(!Target.shouldCancelOtherPlayerAttack(true, true),
                "Show All Players cancelled a manual attack on a clan ally");
        require(!Target.shouldHideOtherPlayerModel(false, true, false, true),
                "Show All Players hid an unrelated player");
        require(Target.shouldPassThroughOtherPlayer(false, true, false, true),
                "Show All Players made an unrelated player hittable under Safe Hits");

        require(!Target.shouldPassThroughOtherPlayer(false, true, false, false),
                "Safe Hits off changed Show All Players' old hitbox behavior");
        require(!Target.shouldPassThroughOtherPlayer(true, false, true, false),
                "Safe Hits off changed Show Clan's old hitbox behavior");

        Field piercingBoxes = EntityBoundingBoxFilterHook.class.getDeclaredField("hideClansBoxes");
        piercingBoxes.setAccessible(true);
        Object previousBoxes = piercingBoxes.get(null);
        Object ally = new Object();
        Object originalBox = new Object();
        Object hiddenBox = new Object();
        Map<Object, Object> hidden = new IdentityHashMap<Object, Object>();
        hidden.put(ally, hiddenBox);
        try {
            piercingBoxes.set(null, hidden);
            EntityBoundingBoxFilterHook.updateTarget(
                    Collections.emptyList(), Collections.singletonList(ally));
            require(EntityBoundingBoxFilterHook.filter(ally, originalBox) == originalBox,
                    "Piercing overrode Safe Hits' ally exemption");
            EntityBoundingBoxFilterHook.clearTarget();
            require(EntityBoundingBoxFilterHook.filter(ally, originalBox) == hiddenBox,
                    "Disabling Target did not restore Piercing's hitbox behavior");
        } finally {
            EntityBoundingBoxFilterHook.clearTarget();
            piercingBoxes.set(null, previousBoxes);
        }
        System.out.println("PASS: Safe Hits ally visibility and hitboxes in Manual, Automatic and Show All Players");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

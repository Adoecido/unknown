package vegas.sony.module.combat;

import vegas.sony.event.EventHandler;
import vegas.sony.event.impl.EventPreTick;
import vegas.sony.module.Category;
import vegas.sony.module.Mod;
import vegas.sony.wrapper.impl.ForgeVersion;
import vegas.sony.wrapper.impl.Minecraft;

/**
 * Clears Minecraft's mapped leftClickCounter (missTime in CheatBreaker 1.8.9)
 * so a missed click does not add the vanilla hit delay.
 */
public class NoHitDelay extends Mod {
    public NoHitDelay() {
        super("HitFix //$ui", 18742032, Category.MISC,
                "Removes the delay after a missed hit //$ui");
    }

    @Override
    public boolean matchesName(String candidate) {
        return super.matchesName(candidate) || "NoHitDelay".equalsIgnoreCase(candidate);
    }

    @EventHandler
    public void onTick(EventPreTick event) {
        if (!ForgeVersion.MC_1_8_9.d() || Minecraft.currentScreen().isNotNull()) {
            return;
        }
        Minecraft.r(0);
    }
}

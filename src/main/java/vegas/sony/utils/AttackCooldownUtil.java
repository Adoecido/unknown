package vegas.sony.utils;

import vegas.sony.event.EventListener;
import vegas.sony.wrapper.impl.ForgeVersion;
import vegas.sony.wrapper.impl.Minecraft;

public class AttackCooldownUtil
implements EventListener {
    private static float lastCooldownStrength;

    public static boolean isAttackReady(float partialTicks) {
        if (ForgeVersion.MC_1_12_2.v()) {
            return true;
        }
        float cooldownStrength = Minecraft.a_xH_J().getCooledAttackStrength(partialTicks);
        boolean fullyCooledDown = cooldownStrength == 1.0f;
        return fullyCooledDown;
    }

}


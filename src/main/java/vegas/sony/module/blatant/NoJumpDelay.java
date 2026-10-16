package vegas.sony.module.blatant;

import vegas.sony.event.EventHandler;
import vegas.sony.event.impl.EventPrePlayerTick;
import vegas.sony.module.Category;
import vegas.sony.module.Mod;
import vegas.sony.wrapper.impl.EntityPlayerSP;
import vegas.sony.wrapper.impl.ForgeVersion;
import vegas.sony.wrapper.impl.Minecraft;

/**
 * Removes the vanilla jump cooldown using the mapped EntityLivingBase field
 * jumpTicks (noJumpDelay in the CheatBreaker 1.8.9 mappings).
 */
public class NoJumpDelay extends Mod {
    public NoJumpDelay() {
        super("NoJumpDelay //$ui", 18742031, Category.MISC,
                "Removes the delay between consecutive jumps //$ui");
    }

    @EventHandler
    public void onPlayerTick(EventPrePlayerTick event) {
        if (!ForgeVersion.MC_1_8_9.d()) {
            return;
        }
        EntityPlayerSP player = Minecraft.thePlayer();
        if (player.isNull()) {
            return;
        }
        player.L(0);
    }
}

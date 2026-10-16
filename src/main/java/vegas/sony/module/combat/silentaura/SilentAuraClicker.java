package vegas.sony.module.combat.silentaura;

import vegas.sony.SonyVegas;
import vegas.sony.click.ClickButton;
import vegas.sony.click.ClickEngine;
import vegas.sony.module.Category;
import vegas.sony.module.combat.ClickerMod;
import vegas.sony.module.combat.SilentAura;
import vegas.sony.module.render.Animations;
import vegas.sony.value.BooleanValue;
import vegas.sony.wrapper.impl.EntityPlayerSP;

public class SilentAuraClicker
extends ClickerMod {
    private static final String CLICKER_NAME = "clicker //$ui";
    private final SilentAura silentAura;

    @Override
    public boolean isClickCycleBlocked() {
        if (!this.silentAura.isEnabled()) {
            return true;
        }
        return !this.silentAura.canClickAttack();
    }

    @Override
    public boolean shouldSimulateBlockHit(ClickEngine clickEngine, EntityPlayerSP player) {
        Animations animations = SonyVegas.INSTANCE.getModManager().getMod(Animations.class);
        return animations != null && animations.shouldBlock();
    }

    @Override
    public boolean isVisible() {
        return false;
    }

    public SilentAuraClicker(SilentAura silentAura) {
        super(CLICKER_NAME, 0, Category.NONE);
        this.silentAura = silentAura;
        ClickEngine clickEngine = new ClickEngine(ClickButton.LEFT, silentAura.getAttackRate(), silentAura.getLimitToItems(), silentAura.getAllowedItems(), silentAura.getRequireMouseDown(), null, new BooleanValue((Object)null, "", false), this) {
            @Override
            public long calculateNextClickDelay() {
                return AuraAttackTiming.clickIntervalMillis(silentAura.getAttackRate().getRandomValue());
            }
        };
        this.setClickEngine(clickEngine);
        this.setEnabled(true);
    }
}

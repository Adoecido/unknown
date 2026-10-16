package vegas.sony.module.combat;

import vegas.sony.SonyVegas;
import vegas.sony.config.ClientSettings;
import vegas.sony.event.EventHandler;
import vegas.sony.event.impl.EventPreTick;
import vegas.sony.input.InputEventDispatcher;
import vegas.sony.input.KeyBindingInputState;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.module.Category;
import vegas.sony.module.Mod;
import vegas.sony.notification.NotificationType;
import vegas.sony.value.NumberValue;
import vegas.sony.wrapper.impl.EntityPlayerSP;
import vegas.sony.wrapper.impl.ForgeVersion;
import vegas.sony.wrapper.impl.InventoryPlayer;
import vegas.sony.wrapper.impl.ItemStack;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.Potion;
import vegas.sony.wrapper.impl.PotionEffect;
import vegas.sony.wrapper.impl.PotionRegistry;

import java.util.Collection;
import java.util.Locale;

/**
 * Automatically consumes a golden apple when the regeneration effect reaches
 * a configurable remaining duration. Item and potion identification go through
 * the CheatBreaker-compatible wrappers and mappings.
 */
public class AutoGapple extends Mod {
    private static final int TICKS_PER_SECOND = 20;
    private static final int USE_DURATION_TICKS = 35;
    private static final int WARNING_LEAD_TICKS = 3 * TICKS_PER_SECOND;

    private final NumberValue regenerationTime = NumberValue.create(this, "Regeneration time //$ui", "#", " s //$ui",
            1.0, 5.0, 25.0, 1.0,
            "Consumes the apple when regeneration has this many seconds or less remaining //$ui");
    private boolean usingApple;
    private boolean waitingForRegenerationEnd;
    private boolean regenerationCountdownArmed;
    private boolean warningShown;
    private int previousRegenerationTicks = -1;
    private int displayedRegenerationTicks = -1;
    private long displayedRegenerationAt;
    private int originalHotbarSlot = -1;
    private int appleHotbarSlot = -1;
    private int useTicks;

    public AutoGapple() {
        super("Gapple //$ui", 18742033, Category.GLADIADOR,
                "Automatically eats golden apples based on regeneration duration //$ui");
        this.addValue(this.regenerationTime);
    }

    @Override
    public boolean matchesName(String candidate) {
        return super.matchesName(candidate) || "AutoGapple".equalsIgnoreCase(candidate);
    }

    @Override
    public String getSimpleSuffix() {
        return this.regenerationTime.getDisplayValue() + "s";
    }

    /**
     * Remaining regeneration time, which is what the module is waiting for.
     * The HUD asks for this on every frame, so the potion lookup is only
     * repeated a few times per second.
     */
    @Override
    public String getDetailedSuffix() {
        long now = System.currentTimeMillis();
        if (now - this.displayedRegenerationAt >= 50L) {
            EntityPlayerSP player = Minecraft.thePlayer();
            this.displayedRegenerationTicks = player.isNull()
                    ? -1 : this.getRegenerationTicks(player);
            this.displayedRegenerationAt = now;
        }
        if (this.displayedRegenerationTicks < 0) {
            return this.getSimpleSuffix();
        }
        return formatTicksUntilEat(this.displayedRegenerationTicks,
                this.getTriggerTicks());
    }

    static String formatTicksUntilEat(int regenerationTicks, int triggerTicks) {
        int remaining = Math.max(0, regenerationTicks - triggerTicks);
        if (remaining % TICKS_PER_SECOND == 0) {
            return remaining / TICKS_PER_SECOND + "s";
        }
        // Round upward to a tenth so the HUD never reaches 0.0 before the
        // configured consume point has actually been crossed.
        double seconds = Math.ceil(remaining / 2.0) / 10.0;
        return String.format(Locale.ROOT, "%.1fs", seconds);
    }

    @EventHandler
    public void onTick(EventPreTick event) {
        if (!ForgeVersion.MC_1_8_9.d()) {
            return;
        }

        EntityPlayerSP player = Minecraft.thePlayer();
        if (player.isNull() || Minecraft.currentScreen().isNotNull()
                || !InputEventDispatcher.getInstance().getFocusState().isFocused()) {
            // Chat, inventory, or the game window sitting in the background:
            // never keep holding the use key there.
            this.stopUsingApple(player);
            return;
        }

        InventoryPlayer inventory = player.V$src$Lvegas_sony_wrapper_impl_InventoryPlayer_$erqak6();
        if (this.usingApple) {
            if (inventory.v() != this.appleHotbarSlot
                    || ++this.useTicks >= USE_DURATION_TICKS) {
                this.stopUsingApple(player);
            }
            return;
        }

        int regenerationTicks = this.getRegenerationTicks(player);
        int triggerTicks = this.getTriggerTicks();
        if (this.waitingForRegenerationEnd) {
            if (regenerationTicks > 0) {
                // The apple landed. Never start another use on this same tick:
                // arm the countdown only when the new effect is above the
                // configured threshold, then wait for it to cross that value.
                this.waitingForRegenerationEnd = false;
                this.regenerationCountdownArmed = regenerationTicks > triggerTicks;
                this.previousRegenerationTicks = regenerationTicks;
                this.warningShown = false;
                return;
            } else {
                // Wait for the server's effect update. The active-effect
                // collection lookup below avoids the stale resolved-entry path
                // that previously kept this state at zero forever.
                return;
            }
        }

        if (this.previousRegenerationTicks < 0 && regenerationTicks > 0) {
            // Enabling the module during an existing effect should still honor
            // the selected remaining-time threshold.
            this.regenerationCountdownArmed = true;
        }
        if (this.previousRegenerationTicks >= 0
                && regenerationTicks > this.previousRegenerationTicks + 5) {
            this.warningShown = false;
        }
        if (regenerationTicks > triggerTicks) {
            this.regenerationCountdownArmed = true;
        }
        if (regenerationTicks > triggerTicks
                && regenerationTicks <= triggerTicks + WARNING_LEAD_TICKS
                && !this.warningShown) {
            this.warningShown = true;
            if (SonyVegas.INSTANCE.getNotificationManager() != null) {
                SonyVegas.INSTANCE.getNotificationManager().show(
                        "Gapple //$ui", "Eating in 3 seconds //$ui",
                        NotificationType.INFO, 3000L);
            }
        }

        boolean shouldEat = regenerationTicks == 0
                || this.regenerationCountdownArmed && regenerationTicks <= triggerTicks;
        this.previousRegenerationTicks = regenerationTicks;
        if (ClientSettings.isUseItemButtonDown() || !shouldEat) {
            return;
        }

        int slot = this.findGoldenAppleSlot(inventory);
        if (slot == -1) {
            return;
        }

        this.originalHotbarSlot = inventory.v();
        this.appleHotbarSlot = slot;
        this.useTicks = 0;
        if (this.originalHotbarSlot != slot) {
            inventory.g(slot);
        }
        this.waitingForRegenerationEnd = true;
        this.regenerationCountdownArmed = false;
        KeyBindingInputState.sendUseKeyDown();
        this.usingApple = true;
    }

    @Override
    public void onDisable() {
        this.stopUsingApple(Minecraft.thePlayer());
        this.waitingForRegenerationEnd = false;
        this.regenerationCountdownArmed = false;
        this.warningShown = false;
        this.previousRegenerationTicks = -1;
    }

    private int findGoldenAppleSlot(InventoryPlayer inventory) {
        for (int slot = 0; slot < 9; ++slot) {
            ItemStack stack = inventory.c(slot);
            if (stack.isNull() || stack.getItem().isNull()) {
                continue;
            }
            if (stack.getItem().isInstance(MappedClasses.q3)) {
                return slot;
            }
        }
        return -1;
    }

    private int getRegenerationTicks(EntityPlayerSP player) {
        // This is the same active-effect collection used by the potion HUD and
        // remains reliable on the 1.8.9 clients where the resolved PotionEntry
        // lookup can be stale.
        try {
            Collection activeEffects = player.B$src$Ljava_util_Collection_$1uxz2f9();
            if (activeEffects != null) {
                for (Object effectHandle : activeEffects) {
                    PotionEffect effect = new PotionEffect(effectHandle);
                    if (effect.isNotNull() && effect.C() == 10) {
                        return Math.max(0, effect.k());
                    }
                }
            }
        } catch (Throwable ignored) {
            // Fall through to the direct potion accessors.
        }
        try {
            if (PotionRegistry.i.getResolvedObject() != null && player.i(PotionRegistry.i)) {
                PotionEffect regeneration = player.b(PotionRegistry.i);
                if (regeneration != null && regeneration.isNotNull()) {
                    return Math.max(0, regeneration.k());
                }
            }
        } catch (Throwable ignored) {
            // Fall through to the stable legacy id used by 1.8.9/1.12.2.
        }
        if (!ForgeVersion.MC_1_16_5.d()) {
            try {
                Potion regenerationPotion = Potion.getPotionById(10);
                if (regenerationPotion != null && regenerationPotion.isNotNull()
                        && player.isPotionActive(regenerationPotion)) {
                    PotionEffect regeneration = player.getActivePotionEffect(regenerationPotion);
                    return regeneration == null || regeneration.isNull()
                            ? 0 : Math.max(0, regeneration.k());
                }
            } catch (Throwable ignored) {
                // Missing mappings must behave as no active regeneration, never break the tick loop.
            }
        }
        return 0;
    }

    private int getTriggerTicks() {
        // The configured value is the exact point at which the use action
        // begins.  A 5 second setting therefore presses use at 5 seconds left.
        return (int)Math.round(this.regenerationTime.getValue()
                * TICKS_PER_SECOND);
    }

    private void stopUsingApple(EntityPlayerSP player) {
        if (!this.usingApple) {
            this.resetState();
            return;
        }
        boolean completedUse = this.useTicks >= USE_DURATION_TICKS;
        KeyBindingInputState.sendUseKeyUp();
        if (player != null && player.isNotNull()) {
            InventoryPlayer inventory = player.V$src$Lvegas_sony_wrapper_impl_InventoryPlayer_$erqak6();
            if (inventory.v() == this.appleHotbarSlot && this.originalHotbarSlot >= 0) {
                inventory.g(this.originalHotbarSlot);
            }
        }
        this.resetState();
        if (!completedUse) {
            this.waitingForRegenerationEnd = false;
            this.regenerationCountdownArmed = false;
        }
    }

    private void resetState() {
        this.usingApple = false;
        this.originalHotbarSlot = -1;
        this.appleHotbarSlot = -1;
        this.useTicks = 0;
    }
}

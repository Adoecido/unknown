package vegas.sony.module.gladiador;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import vegas.sony.SonyVegas;
import vegas.sony.combat.AttackPacketTimingTracker;
import vegas.sony.combat.PlayerVisibility;
import vegas.sony.event.EventHandler;
import vegas.sony.event.impl.EventPacketReceive;
import vegas.sony.event.impl.EventPostAttack;
import vegas.sony.event.impl.EventPreAttack;
import vegas.sony.event.impl.EventPreRenderEntity;
import vegas.sony.event.impl.EventPreRenderLiving;
import vegas.sony.event.impl.EventPreTick;
import vegas.sony.event.impl.EventRender2D;
import vegas.sony.event.impl.EventRender3D;
import vegas.sony.event.impl.EventRenderPlayerPre;
import vegas.sony.mapping.EntityBoundingBoxFilterHook;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.module.Category;
import vegas.sony.module.Mod;
import vegas.sony.ui.font.SmoothFontRenderer;
import vegas.sony.ui.hud.HudLayout;
import vegas.sony.ui.hud.HudLayoutModel;
import vegas.sony.remote.LocalControlServer;
import vegas.sony.remote.InvseeSyncClient;
import vegas.sony.remote.PartyTargetSync;
import vegas.sony.remote.RemoteControlClient;
import vegas.sony.runtime.NativeBridge;
import vegas.sony.utils.ClipboardUtil;
import vegas.sony.unmap.ModeOption;
import vegas.sony.utils.render.RenderUtil;
import vegas.sony.utils.render.GuiRenderPrimitives;
import vegas.sony.value.BooleanValue;
import vegas.sony.value.InvseeTargetCandidatesValue;
import vegas.sony.value.ModeValue;
import vegas.sony.value.NumberValue;
import vegas.sony.value.PlayerNameSuggestionProvider;
import vegas.sony.value.RandomValue;
import vegas.sony.value.StringValue;
import vegas.sony.wrapper.impl.Entity;
import vegas.sony.wrapper.impl.EntityLivingBase;
import vegas.sony.wrapper.impl.EntityPlayer;
import vegas.sony.wrapper.impl.EntityPlayerSP;
import vegas.sony.wrapper.impl.FontRenderer;
import vegas.sony.wrapper.impl.ItemStack;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.Score;
import vegas.sony.wrapper.impl.ScoreObjective;
import vegas.sony.wrapper.impl.Scoreboard;
import vegas.sony.wrapper.impl.Team;
import vegas.sony.wrapper.impl.World;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ThreadLocalRandom;

public class Target extends Mod {
    private static final float BROWSE_MAX_DISTANCE = 16.0f;
    public final BooleanValue automatic;
    public final ModeValue mode;
    private final ModeOption lowArmorMode;
    private final ModeOption breakArmorMode;
    private final ModeOption healthMode;
    private final ModeOption browseTimeMode;
    private final ModeOption browseHitMode;
    public final RandomValue browseHits;
    public final RandomValue browseTimeMillis;
    public final BooleanValue clearBrowseCache;
    public final BooleanValue showBrowsedPlayers;
    public final BooleanValue showClan;
    public final BooleanValue showAllPlayers;
    public final BooleanValue safeHits;
    public final BooleanValue considerDurability;
    public final NumberValue breakArmorPriority;
    public final BooleanValue info;
    public final BooleanValue showBox;
    public final StringValue playerName;
    public final BooleanValue invseeSynchronization;
    public final ModeValue invseeMode;
    public final InvseeTargetCandidatesValue invseeCandidates;
    public final NumberValue invseePosition;
    public final ModeValue invseeRetarget;
    public final BooleanValue copyInvseeCode;

    private static final long MODULE_COLOR = -1033271949203766542L;
    private static final java.util.regex.Pattern COLOR_CODE = java.util.regex.Pattern.compile("§.");
    private static final java.util.regex.Pattern CLAN_TAG =
            java.util.regex.Pattern.compile("\\s*\\[[^\\[\\]]{1,16}\\]\\s*");

    private static final int PANEL_X = 7;
    private static final int PANEL_MARGIN_TOP = 8;
    private static final Color PANEL_BACKGROUND = new Color(17, 17, 17, 240);
    private static final Color BORDER = new Color(30, 30, 30, 240);
    private static final Color STATUS_HIT = new Color(244, 244, 244);
    private static final Color STATUS_PENDING = new Color(78, 78, 78);
    private static final int TEXT_PRIMARY = 0xFFF4F4F4;
    private static final int TEXT_MUTED = 0xFF7C7C7C;
    private static final int MODE_LOW_ARMOR = 0;
    private static final int MODE_BREAK_ARMOR = 1;
    private static final int MODE_HEALTH = 2;
    private static final int MODE_BROWSE_ALL_TIME = 3;
    private static final int MODE_BROWSE_ALL_HITS = 4;
    private static final int INVSEE_MODE_AUTOMATIC = 1;
    private static final long PENDING_HIT_TIMEOUT_MILLIS = 1500L;
    private static final long INVSEE_POLL_INTERVAL_MILLIS = 1500L;

    private String lockedTarget = "";
    private long lastSwitchTime;
    private int hitsOnCurrentTarget;
    private int activeBrowseHitLimit;
    private long activeBrowseTimeLimitMillis;
    private String breakArmorTargetName = "";
    private int breakArmorArmorMask;
    private EntityLivingBase currentTarget;
    private String browseClanTag = "";
    private final Map<String, Integer> browseOrder = new HashMap<String, Integer>();
    private final Set<String> browseClanMembers = new LinkedHashSet<String>();
    private final Map<String, String> browseClanDisplayNames = new HashMap<String, String>();
    private final Set<String> browsedPlayerNames = new LinkedHashSet<String>();
    private final Set<String> completedBrowseCycleNames = new LinkedHashSet<String>();
    private boolean awaitingBrowseSelection;
    private String browseCursorName = "";
    private long browsePausedAt;
    private int pendingHitEntityId = -1;
    private long pendingHitDeadline;
    private int lastMode = -1;
    private final ScheduledExecutorService invseeRequestExecutor = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactory() {
                @Override
                public Thread newThread(Runnable runnable) {
                    Thread thread = new Thread(runnable, "Target-Invsee-Sync");
                    thread.setDaemon(true);
                    return thread;
                }
            });
    private final AtomicBoolean invseeRequestInFlight = new AtomicBoolean(false);
    private volatile InvseeSnapshot invseeSnapshot = InvseeSnapshot.unavailable();
    private long nextInvseeRequestAt;
    private volatile boolean invseeSyncWasEnabled;
    private volatile long invseeGeneration;
    private volatile String invseeCode = "";
    private volatile long invseeCodeExpiresAt;
    private ScheduledFuture<?> invseePollTask;
    private final InvseeSelectionPolicy invseeSelection = new InvseeSelectionPolicy();
    private final Set<String> invseeConfirmedDeaths = new LinkedHashSet<String>();
    private long invseeDeathReportAt;
    private final PartyTargetSync.Selection partySelection = new PartyTargetSync.Selection();

    public Target() {
        super("Target //$ui", (int) MODULE_COLOR, Category.GLADIADOR, "Focuses and filters combat targets using priority logic. //$ui");
        this.automatic = BooleanValue.create(this, "Automatic //$ui", false);
        this.lowArmorMode = new ModeOption("Low Armor //$ui");
        this.breakArmorMode = new ModeOption("Break Armor //$ui");
        this.healthMode = new ModeOption("Health //$ui");
        this.browseTimeMode = new ModeOption("Browse All Players Time //$ui");
        this.browseHitMode = new ModeOption("Browse All Players Hit //$ui");
        this.mode = ModeValue.create(this, "Mode //$ui", this.lowArmorMode, this.lowArmorMode, this.breakArmorMode, this.healthMode, this.browseTimeMode, this.browseHitMode);
        this.browseHits = RandomValue.createWithDescription(this, "Hits per player //$ui", "#", "",
                1, 2, 5, 10, 1,
                "Random hit limit selected once for each player //$ui");
        this.browseTimeMillis = RandomValue.createWithDescription(this, "Time per player //$ui", "#", "ms",
                1, 1000, 1500, 3000, 1,
                "Random time limit selected once for each player //$ui");
        this.clearBrowseCache = BooleanValue.create(this, "Clear cache //$ui", false,
                "Clears the stored clan order and waits for a new hit //$ui");
        this.clearBrowseCache.setResettable(false);
        this.showBrowsedPlayers = BooleanValue.create(this, "Show Browsed Players //$ui", true);
        this.showClan = BooleanValue.create(this, "Show Clan //$ui", false,
                "Keeps clan members visible without allowing Aura to select them unless Target chose one //$ui");
        this.showAllPlayers = BooleanValue.create(this, "Show All Players //$ui", false,
                "Keeps every player visible while Aura continues to attack only Target's selection //$ui");
        this.safeHits = BooleanValue.create(this, "Safe Hits //$ui", false,
                "Keeps clan allies visible and hittable; other non-target players remain pass-through even with Show All Players //$ui");
        this.considerDurability = BooleanValue.create(this, "Consider Durability //$ui", true);
        this.breakArmorPriority = NumberValue.create(this, "Break Armor Priority //$ui", "#.#", "", 0.0, 5.0, 10.0, 0.1);
        this.info = BooleanValue.create(this, "Info //$ui", false);
        this.showBox = BooleanValue.create(this, "Show Box //$ui", true);
        this.playerName = StringValue.create(this, "Player Name //$ui", "").setSuggestionProvider(new PlayerNameSuggestionProvider());
        this.invseeSynchronization = BooleanValue.create(this,
                "Invsee Synchronization //$ui", false,
                "Copies a pairing code for /syncunknown in your Discord ticket //$ui");
        ModeOption invseeManualMode = new ModeOption("Manual //$ui");
        ModeOption invseeAutomaticMode = new ModeOption("Automatic //$ui");
        this.invseeMode = ModeValue.create(this, "Invsee Mode //$ui",
                "Manual: fixed player. Automatic: follow a ranking position //$ui",
                invseeManualMode, invseeManualMode, invseeAutomaticMode);
        this.invseeCandidates = InvseeTargetCandidatesValue.create(this);
        this.invseePosition = NumberValue.create(this, "Invsee Position //$ui", "#", "", 1, 1, 100, 1);
        ModeOption followPosition = new ModeOption("Follow Position //$ui");
        ModeOption untilDeath = new ModeOption("Until Death //$ui");
        this.invseeRetarget = ModeValue.create(this, "Invsee Retarget //$ui", followPosition, followPosition, untilDeath);
        this.copyInvseeCode = BooleanValue.create(this, "Copy Sync Code //$ui", false,
                "Copies the pending code or generates a new one to pair again //$ui");
        this.copyInvseeCode.setResettable(false);
        this.invseeMode.addModeDependentValues(invseeAutomaticMode, this.invseePosition, this.invseeRetarget);
        this.invseeSynchronization.addChangeListener(value -> {
            if (value.isPersistenceSuppressed()) return;
            if (value.getEffectiveValue()) this.beginInvseeLink(); else this.endInvseeLink();
        });
        this.copyInvseeCode.addChangeListener(value -> {
            if (!value.isPersistenceSuppressed() && Boolean.TRUE.equals(value.getValue())) {
                value.setValue(false);
                if (!this.invseeSynchronization.getEffectiveValue()) return;
                if (!this.invseeCode.isEmpty() && this.invseeCodeExpiresAt > System.currentTimeMillis()) {
                    copyInvseeCodeToClipboard(this.invseeCode);
                } else { this.endInvseeLink(); this.beginInvseeLink(); }
            }
        });

        this.automatic.addDependentValues(this.mode, this.browseHits, this.browseTimeMillis,
                this.showBrowsedPlayers, this.showClan, this.considerDurability, this.breakArmorPriority);
        this.mode.addModeDependentValues(this.browseTimeMode, this.browseTimeMillis, this.showBrowsedPlayers, this.showClan);
        this.mode.addModeDependentValues(this.browseHitMode, this.browseHits,
                this.showBrowsedPlayers, this.showClan);
        this.mode.addModeDependentValues(this.browseTimeMode, this.clearBrowseCache);
        this.mode.addModeDependentValues(this.browseHitMode, this.clearBrowseCache);
        this.mode.addModeDependentValues(this.lowArmorMode, this.considerDurability);
        this.mode.addModeDependentValues(this.breakArmorMode, this.considerDurability, this.breakArmorPriority);
        this.showBrowsedPlayers.addDependentValues(this.showClan);
        this.automatic.whenEqualTo(false).applyTo(this.playerName);
        this.invseeSynchronization.whenEqualTo(false).applyTo(this.automatic,
                this.info, this.showBox, this.playerName);
        this.invseeSynchronization.whenEqualTo(true).applyTo(this.invseeMode,
                this.invseeCandidates, this.copyInvseeCode);

        this.addValue(this.invseeSynchronization, this.invseeMode,
                this.invseePosition, this.invseeRetarget, this.copyInvseeCode, this.invseeCandidates,
                this.automatic, this.mode, this.browseHits, this.browseTimeMillis,
                this.clearBrowseCache,
                this.showBrowsedPlayers, this.showClan, this.showAllPlayers, this.safeHits,
                this.considerDurability, this.breakArmorPriority, this.info, this.showBox,
                this.playerName);
    }

    // A synthetic request only *asks* for an attack: AttackKeyController fires the event and then
    // presses the attack key, so the real swing still arrives as EventPreAttack a tick later. Counting
    // here as well scored every aura swing twice and burned through "Hits per player" at double rate.

    @EventHandler
    public void onPreAttack(EventPreAttack event) {
        if (!this.isEnabled() || event == null || event.getTarget() == null || event.getTarget().isNull()) return;
        if (this.isFollowingPartyTarget()) {
            if (!this.partySelection.isFresh(System.currentTimeMillis())
                    || this.currentTarget == null || this.currentTarget.isNull()
                    || event.getTarget().getObject() != this.currentTarget.getObject()) {
                if (this.shouldCancelOtherPlayerAttack(event.getTarget())) event.setCancelled(true);
            }
            return;
        }
        if (this.invseeSynchronization.getEffectiveValue()) {
            if (!this.invseeSnapshot.isUsable(System.currentTimeMillis()) || this.currentTarget == null
                    || this.currentTarget.isNull() || event.getTarget().getObject() != this.currentTarget.getObject()) {
                if (this.shouldCancelOtherPlayerAttack(event.getTarget())) event.setCancelled(true);
            }
            return;
        }
        int modeIndex = this.mode.getSelectedIndex();
        if (this.isBrowseMode(modeIndex) && this.awaitingBrowseSelection) {
            EntityLivingBase hitPlayer = new EntityLivingBase(event.getTarget().getObject());
            if (this.isValidTarget(hitPlayer, true)) {
                this.beginBrowseSelection(hitPlayer);
                if (!this.isBrowseHitMode()) {
                    this.armPendingHit(hitPlayer);
                }
            }
            return;
        }
        if (this.currentTarget == null || this.currentTarget.isNull()) return;
        if (event.getTarget().getObject() != this.currentTarget.getObject()) {
            if (this.shouldCancelOtherPlayerAttack(event.getTarget())) {
                event.setCancelled(true);
            }
            return;
        }
        if (!this.isBrowseHitMode()) {
            this.armPendingHit(this.currentTarget);
        }
    }

    @EventHandler
    public void onPostAttack(EventPostAttack event) {
        // Browse-by-hits measures damage the selected player receives.  It is
        // intentionally independent from the local player's CPS/attack event.
        if (this.isBrowseHitMode()) return;
        if (!this.isEnabled() || event == null || event.getTarget() == null
                || event.getTarget().isNull()) return;
        int entityId = event.getTarget().S();
        if (this.pendingHitEntityId < 0 || entityId != this.pendingHitEntityId) return;
        this.clearPendingHit();
        if (this.currentTarget != null && !this.currentTarget.isNull()
                && this.currentTarget.S() == entityId) {
            this.recordCurrentTargetDamage();
        }
    }

    @EventHandler
    public void onPacketReceive(EventPacketReceive event) {
        if (!this.isEnabled() || event == null) return;
        Entity damagedEntity = AttackPacketTimingTracker.getDamagePacketEntity(event.getPacket());
        if (this.isBrowseHitMode()) {
            if (damagedEntity != null && !damagedEntity.isNull()
                    && this.currentTarget != null && !this.currentTarget.isNull()
                    && damagedEntity.S() == this.currentTarget.S()) {
                this.recordCurrentTargetDamage();
            }
            return;
        }
        if (this.pendingHitEntityId < 0) return;
        long now = System.currentTimeMillis();
        if (now > this.pendingHitDeadline) {
            this.clearPendingHit();
            return;
        }
        if (damagedEntity == null || damagedEntity.isNull()
                || damagedEntity.S() != this.pendingHitEntityId) return;
        if (this.currentTarget == null || this.currentTarget.isNull()
                || this.currentTarget.S() != this.pendingHitEntityId) {
            this.clearPendingHit();
            return;
        }
        this.clearPendingHit();
        this.recordCurrentTargetDamage();
    }

    @EventHandler(priority=vegas.sony.event.EventPriority.HIGHEST)
    public void onTick(EventPreTick event) {
        if (!this.isEnabled() || Minecraft.theWorld().isNull() || Minecraft.thePlayer().isNull()) {
            this.restoreManagedBoundingBoxes();
            this.clearTargetState();
            return;
        }
        if (this.pendingHitEntityId >= 0 && System.currentTimeMillis() > this.pendingHitDeadline) {
            this.clearPendingHit();
        }

        EntityPlayerSP localPlayer = Minecraft.thePlayer();
        if (Boolean.TRUE.equals(this.clearBrowseCache.getValue())) {
            this.resetBrowseCacheFromControl();
        }
        Scoreboard scoreboard = new World(Minecraft.theWorld().getObject()).getScoreboard();
        // One world scan per tick, shared by candidate selection and the bounding box sync.
        ArrayList<EntityLivingBase> worldPlayers = this.collectWorldPlayers(localPlayer);
        if (this.isFollowingPartyTarget()) {
            this.currentTarget = this.partySelection.isFresh(System.currentTimeMillis())
                    ? this.findInvseeCandidate(worldPlayers, this.partySelection.getName(), true) : null;
            this.clearPendingHit();
            this.syncHiddenBoundingBoxes(worldPlayers);
            return;
        }
        if (this.invseeSynchronization.getEffectiveValue()) {
            this.tickInvseeSynchronization(worldPlayers);
            this.syncHiddenBoundingBoxes(worldPlayers);
            return;
        }
        if (this.invseeSyncWasEnabled) {
            this.endInvseeLink();
            this.clearTargetState();
        }
        if (!this.automatic.getEffectiveValue()) {
            this.currentTarget = this.findManualTarget(localPlayer);
            this.awaitingBrowseSelection = false;
            this.lastMode = -1;
            this.syncHiddenBoundingBoxes(worldPlayers);
            return;
        }

        int modeIndex = this.mode.getSelectedIndex();
        boolean modeChanged = modeIndex != this.lastMode;
        if (modeChanged) {
            this.currentTarget = null;
            this.breakArmorTargetName = "";
            this.breakArmorArmorMask = 0;
            this.hitsOnCurrentTarget = 0;
            this.activeBrowseHitLimit = 0;
            this.activeBrowseTimeLimitMillis = 0L;
            this.clearPendingHit();
            this.clearBrowseState();
            this.awaitingBrowseSelection = this.isBrowseMode(modeIndex);
        }
        if (this.isBrowseMode(modeIndex) && this.awaitingBrowseSelection) {
            this.currentTarget = null;
            this.syncHiddenBoundingBoxes(worldPlayers);
            this.lastMode = modeIndex;
            return;
        }
        boolean browseMode = this.isBrowseMode(modeIndex);
        ArrayList<EntityLivingBase> candidates = this.collectCandidates(
                worldPlayers, browseMode, localPlayer);
        if (this.isBrowseMode(modeIndex) && this.currentTarget != null
                && !this.currentTarget.isNull()) {
            this.readBrowseClan(this.currentTarget, worldPlayers);
        }
        if (this.isBrowseMode(modeIndex) && !this.browseClanMembers.isEmpty()) {
            this.retainBrowseClanCandidates(candidates);
        }
        if (candidates.isEmpty()) {
            if (this.isBrowseMode(modeIndex)) {
                if (this.currentTarget != null && !this.currentTarget.isNull()) {
                    this.browseCursorName = this.normalizeName(this.currentTarget.getName());
                }
                this.currentTarget = null;
                this.clearPendingHit();
                this.lastMode = modeIndex;
            } else {
                this.clearTargetState();
            }
            this.syncHiddenBoundingBoxes(worldPlayers);
            return;
        }

        Map<String, Float> scoreboardHealth = modeIndex == MODE_HEALTH
                ? this.getScoreboardHealthMap(scoreboard) : new HashMap<String, Float>();
        this.sortCandidates(candidates, localPlayer, modeIndex, scoreboardHealth);
        if (this.isBrowseMode(modeIndex)) this.sortBrowseCandidates(candidates);

        String previousName = this.currentTarget == null
                ? (this.isBrowseMode(modeIndex) ? this.browseCursorName : "")
                : this.currentTarget.getName();
        EntityLivingBase selected = null;
        if (selected == null) {
            if (modeIndex == MODE_BREAK_ARMOR && !modeChanged) {
                EntityLivingBase held = this.findCandidate(candidates, this.breakArmorTargetName);
                if (held != null) {
                    int armorMask = this.getArmorPieceMask(held);
                    if (armorMask == this.breakArmorArmorMask) {
                        selected = held;
                    } else {
                        selected = this.findFirstDifferentCandidate(candidates, this.breakArmorTargetName);
                    }
                }
            } else if (this.isBrowseMode(modeIndex)) {
                String cursor = this.currentTarget != null
                        ? this.currentTarget.getName() : this.browseCursorName;
                selected = this.findCandidate(candidates, cursor);
                if (selected == null && !cursor.isEmpty()) {
                    selected = this.findNextBrowseCandidate(candidates, cursor);
                }
            }
        }
        if (selected == null) selected = candidates.get(0);

        this.currentTarget = selected;
        if (this.isBrowseMode(modeIndex)) {
            this.browseCursorName = this.normalizeName(selected.getName());
        }
        if (modeChanged || !previousName.equalsIgnoreCase(selected.getName())) {
            this.hitsOnCurrentTarget = 0;
            this.lastSwitchTime = System.currentTimeMillis();
            this.clearPendingHit();
            if (this.isBrowseMode(modeIndex)) this.rollBrowseLimit(modeIndex);
        }
        if (this.isBrowseMode(modeIndex) && this.shouldSwitch(modeIndex)) this.switchToNextTarget(candidates);

        if (modeIndex == MODE_BREAK_ARMOR && this.currentTarget != null) {
            this.breakArmorTargetName = this.currentTarget.getName();
            this.breakArmorArmorMask = this.getArmorPieceMask(this.currentTarget);
        } else {
            this.breakArmorTargetName = "";
            this.breakArmorArmorMask = 0;
        }

        this.lastMode = modeIndex;
        this.syncHiddenBoundingBoxes(worldPlayers);
    }

    /**
     * Invsee is deliberately a separate target mode.  Normal Target controls
     * are hidden while it is enabled and this method is the only selector
     * allowed to change the active target.
     */
    private void tickInvseeSynchronization(ArrayList<EntityLivingBase> worldPlayers) {
        long now = System.currentTimeMillis();
        if (!this.invseeSyncWasEnabled) this.beginInvseeLink();
        this.requestInvseeSnapshot(now);

        InvseeSnapshot snapshot = this.invseeSnapshot;
        if (!snapshot.isUsable(now)) {
            this.invseeCandidates.setWaitingState(snapshot.expiresAt > 0L
                    ? "Report expired — attacks suspended //$ui" : snapshot.status);
            this.currentTarget = null;
            this.clearPendingHit();
            this.lastMode = -1;
            return;
        }

        if (snapshot.players.isEmpty()) this.invseeCandidates.setWaitingState(snapshot.status);
        else this.invseeCandidates.updatePlayers(snapshot.players, true);
        // A missing/offline player is not dead. A fresh report can introduce a new life.
        if (snapshot.updatedAt != this.invseeDeathReportAt) {
            this.invseeDeathReportAt = snapshot.updatedAt;
            for (InvseeTargetCandidatesValue.RankedPlayer row : snapshot.players) {
                this.invseeConfirmedDeaths.remove(this.normalizeName(row.getName()));
            }
        }
        for (EntityLivingBase player : worldPlayers) {
            if (player.isDead() || player.w$src$F$15l9epb() == 0.0f) {
                this.invseeConfirmedDeaths.add(this.normalizeName(player.getName()));
            }
        }
        String chosen = this.invseeSelection.select(snapshot.players,
                this.invseeMode.getSelectedIndex() == INVSEE_MODE_AUTOMATIC,
                this.invseePosition.getValue().intValue(), this.invseeRetarget.getSelectedIndex() == 1,
                this.invseeCandidates.getValue(), this.invseeConfirmedDeaths);
        this.invseeCandidates.setActivePlayer(chosen);
        boolean reportedAlive = false;
        for (InvseeTargetCandidatesValue.RankedPlayer row : snapshot.players) {
            if (row.getName().equalsIgnoreCase(chosen)) { reportedAlive = true; break; }
        }
        EntityLivingBase selected = reportedAlive ? this.findInvseeCandidate(worldPlayers, chosen) : null;
        if (selected == null) {
            this.currentTarget = null;
            this.clearPendingHit();
            this.lastMode = -1;
            return;
        }

        String previousName = this.currentTarget == null || this.currentTarget.isNull()
                ? "" : this.currentTarget.getName();
        this.currentTarget = selected;
        if (!previousName.equalsIgnoreCase(selected.getName())) {
            this.hitsOnCurrentTarget = 0;
            this.lastSwitchTime = now;
            this.clearPendingHit();
        }
        this.awaitingBrowseSelection = false;
        this.lastMode = -1;
    }

    private EntityLivingBase findInvseeCandidate(ArrayList<EntityLivingBase> worldPlayers,
                                                   String preferredName) {
        // Keep the row chosen in the ranking selected even while that player
        // is outside the local render distance. Target will acquire the same
        // player as soon as its entity becomes available instead of silently
        // moving the detail panel to another clan member.
        return this.findInvseeCandidate(worldPlayers, preferredName, true);
    }

    private EntityLivingBase findInvseeCandidate(ArrayList<EntityLivingBase> worldPlayers,
                                                   String expectedName,
                                                   boolean allowClanPlayer) {
        String expected = this.normalizeName(expectedName);
        if (expected.isEmpty()) return null;
        for (EntityLivingBase player : worldPlayers) {
            if (expected.equals(this.normalizeName(player.getName()))
                    && this.isValidTarget(player, allowClanPlayer)) return player;
        }
        return null;
    }

    private synchronized void beginInvseeLink() {
        if (this.invseeSyncWasEnabled) return;
        this.invseeSyncWasEnabled = true;
        ++this.invseeGeneration;
        this.invseeCode = "";
        this.invseeCodeExpiresAt = 0L;
        this.invseeSelection.reset();
        this.invseeConfirmedDeaths.clear();
        this.invseeDeathReportAt = 0L;
        this.clearTargetState();
        this.invseeCandidates.updatePlayers(Collections.<InvseeTargetCandidatesValue.RankedPlayer>emptyList(), true);
        this.invseeSnapshot = InvseeSnapshot.waiting("Generating sync code //$ui");
        this.nextInvseeRequestAt = 0L;
        this.requestInvseeSnapshot(System.currentTimeMillis());
        this.invseePollTask = this.invseeRequestExecutor.scheduleAtFixedRate(() -> {
            if (this.invseeSyncWasEnabled) this.requestInvseeSnapshot(System.currentTimeMillis());
        }, INVSEE_POLL_INTERVAL_MILLIS, INVSEE_POLL_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
    }

    private synchronized void endInvseeLink() {
        if (!this.invseeSyncWasEnabled) return;
        this.invseeSyncWasEnabled = false;
        if (this.invseePollTask != null) this.invseePollTask.cancel(false);
        this.invseePollTask = null;
        ++this.invseeGeneration;
        this.invseeCode = "";
        this.invseeCodeExpiresAt = 0L;
        this.invseeCandidates.clearCandidates();
        this.invseeSnapshot = InvseeSnapshot.unavailable();
        this.invseeSelection.reset();
        this.clearTargetState();
        // Queue close before any subsequent create; generation rejects late replies.
        this.invseeRequestExecutor.execute(() -> {
            try { InvseeSyncClient.close(); } catch (Exception ignored) {}
        });
        LocalControlServer.notifyExternalStateChanged();
    }

    private void requestInvseeSnapshot(long now) {
        if (now < this.nextInvseeRequestAt
                || !this.invseeRequestInFlight.compareAndSet(false, true)) return;
        this.nextInvseeRequestAt = now + INVSEE_POLL_INTERVAL_MILLIS;
        final long generation = this.invseeGeneration;
        this.invseeRequestExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (generation != invseeGeneration || !invseeSyncWasEnabled) return;
                    InvseeSnapshot previous = invseeSnapshot;
                    InvseeSnapshot next;
                    if (invseeCode.isEmpty() && invseeCodeExpiresAt == 0L) {
                        JsonObject created = InvseeSyncClient.create();
                        String code = jsonString(created, "code");
                        long expires = jsonLong(created, "expiresAt", 0L);
                        if (!code.matches("[A-Za-z0-9]{30}") || expires <= System.currentTimeMillis()) {
                            throw new IllegalStateException("Invalid pairing reply");
                        }
                        synchronized (Target.this) {
                            if (generation != invseeGeneration || !invseeSyncWasEnabled) return;
                            invseeCode = code;
                            invseeCodeExpiresAt = expires;
                            copyInvseeCodeToClipboard(code);
                        }
                        next = InvseeSnapshot.waiting("Code copied — use /syncunknown in your ticket //$ui");
                    } else { next = fetchInvseeSnapshot(); }
                    synchronized (Target.this) {
                        if (generation != invseeGeneration || !invseeSyncWasEnabled) return;
                        invseeSnapshot = next;
                        if (next.players.isEmpty()) invseeCandidates.setWaitingState(next.status);
                        else invseeCandidates.updatePlayers(next.players, true);
                        if (next.paired) invseeCode = "";
                    }
                    // The native menu checks the local state version before
                    // doing a full snapshot. A bot report can arrive without
                    // any module/value mutation, so explicitly wake that
                    // path when the Invsee payload changes (including a
                    // ready -> stale transition).
                    if (!next.hasSamePresentationAs(previous)) {
                        LocalControlServer.notifyExternalStateChanged();
                    }
                } catch (Exception ignored) {
                    synchronized (Target.this) {
                        if (generation == invseeGeneration && invseeSyncWasEnabled) {
                            invseeSnapshot = InvseeSnapshot.waiting("Sync API unavailable — retrying //$ui");
                            invseeCandidates.setWaitingState(invseeSnapshot.status);
                        }
                    }
                    LocalControlServer.notifyExternalStateChanged();
                } finally {
                    invseeRequestInFlight.set(false);
                    if (generation != invseeGeneration && invseeSyncWasEnabled && invseeCodeExpiresAt == 0L) {
                        requestInvseeSnapshot(System.currentTimeMillis());
                    }
                }
            }
        });
    }

    private static void copyInvseeCodeToClipboard(String code) {
        try { NativeBridge.cpy(code); }
        catch (UnsatisfiedLinkError unavailable) { ClipboardUtil.setText(code); }
    }

    private static InvseeSnapshot fetchInvseeSnapshot() throws Exception {
                JsonObject root = InvseeSyncClient.poll();
                String status = jsonString(root, "status");
                if (!"ready".equals(status)) {
                    String message = "waiting".equals(status) ? "Paired — waiting for Invsee report //$ui"
                            : "stale".equals(status) ? "Report expired — attacks suspended //$ui"
                            : "code_expired".equals(status) ? "Code expired — click Copy Sync Code //$ui"
                            : "pending".equals(status) ? "Code copied — use /syncunknown in your ticket //$ui"
                            : "Not paired — click Copy Sync Code //$ui";
                    return new InvseeSnapshot(Collections.<InvseeTargetCandidatesValue.RankedPlayer>emptyList(),
                            0L, 0L, message, "waiting".equals(status) || "stale".equals(status));
                }
                long updatedAt = jsonLong(root, "updatedAt", 0L);
                long expiresAt = jsonLong(root, "expiresAt", 0L);
                if (expiresAt <= System.currentTimeMillis()) return InvseeSnapshot.unavailable();
                int minimumSets = (int)Math.max(0L, Math.min(Integer.MAX_VALUE,
                        jsonLong(root, "minimumSets", 0L)));
                ArrayList<InvseeTargetCandidatesValue.RankedPlayer> players =
                        parseInvseePlayers(root.get("players"), false, minimumSets);
                // Older bot builds only published lowestPlayers. Keep those
                // reports usable while the richer ranking schema rolls out.
                if (players.isEmpty()) {
                    players = parseInvseePlayers(root.get("lowestPlayers"), true,
                            minimumSets);
                }
                Collections.sort(players,
                        new Comparator<InvseeTargetCandidatesValue.RankedPlayer>() {
                            @Override
                            public int compare(InvseeTargetCandidatesValue.RankedPlayer left,
                                               InvseeTargetCandidatesValue.RankedPlayer right) {
                                return Integer.compare(left.getSets(), right.getSets());
                            }
                        });
                if (players.size() > 100) {
                    players = new ArrayList<InvseeTargetCandidatesValue.RankedPlayer>(
                            players.subList(0, 100));
                }
                return new InvseeSnapshot(players, updatedAt, expiresAt);
    }

    private static String jsonString(JsonObject object, String property) {
        JsonElement value = object.get(property);
        return value != null && value.isJsonPrimitive()
                ? value.getAsString() : "";
    }

    private static long jsonLong(JsonObject object, String property, long fallback) {
        JsonElement value = object.get(property);
        if (value == null || !value.isJsonPrimitive()) return fallback;
        try {
            return value.getAsLong();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static boolean jsonBoolean(JsonObject object, String property,
                                       boolean fallback) {
        JsonElement value = object.get(property);
        if (value == null || !value.isJsonPrimitive()) return fallback;
        try {
            return value.getAsBoolean();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static ArrayList<InvseeTargetCandidatesValue.RankedPlayer> parseInvseePlayers(
            JsonElement entries, boolean legacyLowestPlayers, int fallbackSets) {
        ArrayList<InvseeTargetCandidatesValue.RankedPlayer> players =
                new ArrayList<InvseeTargetCandidatesValue.RankedPlayer>();
        if (entries == null || !entries.isJsonArray()) return players;
        for (JsonElement entry : entries.getAsJsonArray()) {
            if (entry == null || !entry.isJsonObject()) continue;
            JsonObject player = entry.getAsJsonObject();
            String name = jsonString(player, "name");
            long rawSets = jsonLong(player, "sets",
                    legacyLowestPlayers ? fallbackSets : -1L);
            if (!name.matches("[A-Za-z0-9_]{1,16}") || rawSets < 0L
                    || rawSets > Integer.MAX_VALUE) continue;
            if (!legacyLowestPlayers
                    && (!jsonBoolean(player, "online", true)
                            || !jsonBoolean(player, "analysed", true)
                            || jsonBoolean(player, "dead", false))) continue;
            players.add(new InvseeTargetCandidatesValue.RankedPlayer(
                    name, (int)rawSets,
                    (int)Math.max(0L, jsonLong(player, "helmet", 0L)),
                    (int)Math.max(0L, jsonLong(player, "chestplate", 0L)),
                    (int)Math.max(0L, jsonLong(player, "leggings", 0L)),
                    (int)Math.max(0L, jsonLong(player, "boots", 0L))));
        }
        return players;
    }

    private EntityLivingBase findManualTarget(EntityPlayerSP localPlayer) {
        String desired = this.normalizeName(this.playerName.getValue());
        if (desired.isEmpty()) return null;
        for (Object handle : Minecraft.theWorld().z()) {
            Entity entity = new Entity(handle);
            if (!entity.isInstance(MappedClasses.Yl)) continue;
            EntityLivingBase player = new EntityLivingBase(handle);
            // An explicitly typed player is allowed to be a clan member. The
            // manual selector is the user's override; applying the automatic
            // clan filter here left currentTarget null and therefore hid no
            // other hitboxes.
            if (player.getObject() != localPlayer.getObject()
                    && desired.equals(this.normalizeName(player.getName()))
                    && this.isValidTarget(player, true)) return player;
        }
        return null;
    }

    /** Single pass over the world entity list, yielding every remote player. */
    private ArrayList<EntityLivingBase> collectWorldPlayers(EntityPlayerSP localPlayer) {
        ArrayList<EntityLivingBase> players = new ArrayList<EntityLivingBase>();
        for (Object handle : Minecraft.theWorld().z()) {
            try {
                Entity entity = new Entity(handle);
                if (!entity.isInstance(MappedClasses.Yl)) continue;
                if (handle == localPlayer.getObject()) continue;
                EntityLivingBase player = new EntityLivingBase(handle);
                if (player.isNull()) continue;
                players.add(player);
            } catch (Throwable ignored) {}
        }
        return players;
    }

    private ArrayList<EntityLivingBase> collectCandidates(ArrayList<EntityLivingBase> worldPlayers,
                                                           boolean allowClanPlayers,
                                                           EntityPlayerSP localPlayer) {
        ArrayList<EntityLivingBase> candidates = new ArrayList<EntityLivingBase>();
        for (EntityLivingBase player : worldPlayers) {
            if (allowClanPlayers && player.getDistanceToEntity(localPlayer)
                    > BROWSE_MAX_DISTANCE) continue;
            if (this.isValidTarget(player, allowClanPlayers)) candidates.add(player);
        }
        return candidates;
    }

    private boolean isValidTarget(EntityLivingBase player) {
        return this.isValidTarget(player, false);
    }

    private boolean isValidTarget(EntityLivingBase player, boolean allowClanPlayer) {
        if (player == null || player.isNull() || !player.isInstance(MappedClasses.Yl)) return false;
        if (!allowClanPlayer && HideClans.isClanEntity(player)) return false;
        try {
            if (player.isDead() || player.isInvisible()) return false;
        } catch (Throwable ignored) {}
        if (!PlayerVisibility.isInteractable(player)) return false;
        try {
            return player.w$src$F$15l9epb() > 0.0f;
        } catch (Throwable ignored) {
            return true;
        }
    }

    private void sortCandidates(ArrayList<EntityLivingBase> candidates, EntityPlayerSP localPlayer, int modeIndex, Map<String, Float> health) {
        final boolean durability = this.considerDurability.getEffectiveValue();
        final float breakPriority = this.breakArmorPriority.getValue().floatValue();
        candidates.sort((a, b) -> {
            float scoreA = this.getPriorityScore(a, localPlayer, modeIndex, health, breakPriority, durability);
            float scoreB = this.getPriorityScore(b, localPlayer, modeIndex, health, breakPriority, durability);
            int comparison = Float.compare(scoreA, scoreB);
            return comparison != 0 ? comparison : Float.compare(a.getDistanceToEntity(localPlayer), b.getDistanceToEntity(localPlayer));
        });
    }

    private float getPriorityScore(EntityLivingBase player, EntityPlayerSP localPlayer, int modeIndex,
                                   Map<String, Float> health, float breakPriority, boolean durability) {
        switch (modeIndex) {
            case MODE_LOW_ARMOR: return this.getLowArmorScore(player, durability);
            case MODE_BREAK_ARMOR: return this.getBreakArmorScore(player, breakPriority, durability);
            case MODE_HEALTH: return this.getHealthScore(player, health);
            default: return player.getDistanceToEntity(localPlayer);
        }
    }

    private float getLowArmorScore(EntityLivingBase player, boolean durability) {
        if (!durability) return player.getTotalArmorValue();
        ArmorStats stats = this.getArmorStats(player);
        return stats.pieces == 0 ? 0.0f : stats.remainingPercent / stats.pieces;
    }

    private float getBreakArmorScore(EntityLivingBase player, float priority, boolean durability) {
        ArmorStats stats = this.getArmorStats(player);
        float condition = durability ? stats.remainingDurability : player.getTotalArmorValue() * 100.0f;
        return -(stats.pieces * (10000.0f + priority * 1000.0f) + condition);
    }

    private ArmorStats getArmorStats(EntityLivingBase player) {
        ArmorStats stats = new ArmorStats();
        try {
            Object[] armor = new EntityPlayer(player.getObject()).V$src$Lvegas_sony_wrapper_impl_InventoryPlayer_$erqak6().i();
            for (Object handle : armor) {
                ItemStack stack = new ItemStack(handle);
                if (stack.isNull()) continue;
                stats.pieces++;
                int maximum = Math.max(0, stack.getItem().a());
                int damage = Math.max(0, stack.L());
                int remaining = maximum > 0 ? Math.max(0, maximum - damage) : 0;
                stats.remainingDurability += remaining;
                stats.remainingPercent += maximum > 0 ? remaining * 100.0f / maximum : 100.0f;
                stats.signature = 31 * stats.signature + stack.getItem().P();
                stats.signature = 31 * stats.signature + damage;
            }
        } catch (Throwable ignored) {
            stats.pieces = Math.max(0, player.getTotalArmorValue() / 5);
            stats.remainingDurability = player.getTotalArmorValue() * 100;
            stats.remainingPercent = player.getTotalArmorValue() * 5.0f;
            stats.signature = player.getTotalArmorValue();
        }
        return stats;
    }

    private int getArmorSignature(EntityLivingBase player) {
        return this.getArmorStats(player).signature;
    }

    private int getArmorPieceMask(EntityLivingBase player) {
        int mask = 0;
        try {
            Object[] armor = new EntityPlayer(player.getObject()).V$src$Lvegas_sony_wrapper_impl_InventoryPlayer_$erqak6().i();
            for (int slot = 0; slot < armor.length && slot < Integer.SIZE; slot++) {
                if (!new ItemStack(armor[slot]).isNull()) mask |= 1 << slot;
            }
            return mask;
        } catch (Throwable ignored) {
            return this.getArmorStats(player).pieces;
        }
    }

    private float getHealthScore(EntityLivingBase player, Map<String, Float> scoreboardHealth) {
        float realHealth = player.getRealHealth();
        if (realHealth >= 0.0f) return realHealth;
        Float scoreboardValue = scoreboardHealth.get(this.normalizeName(player.getName()));
        return scoreboardValue != null ? scoreboardValue : player.w$src$F$15l9epb();
    }

    private float getHealthPercent(EntityLivingBase player, Map<String, Float> scoreboardHealth) {
        return Math.max(0.0f, this.getHealthScore(player, scoreboardHealth)) * 5.0f;
    }

    private Map<String, Float> getScoreboardHealthMap(Scoreboard scoreboard) {
        Map<String, Float> health = new HashMap<String, Float>();
        if (scoreboard == null || scoreboard.isNull()) return health;
        ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(2);
        if (objective == null) objective = scoreboard.getObjectiveInDisplaySlot(1);
        if (objective == null || objective.isNull()) return health;
        Collection<Score> scores = scoreboard.getPlayerScores(objective);
        if (scores == null) return health;
        for (Score score : scores) {
            if (score == null || score.isNull()) continue;
            String owner = score.getOwner();
            if (owner != null && !owner.isEmpty() && score.getScore() >= 0) health.put(this.normalizeName(owner), (float) score.getScore());
        }
        return health;
    }

    private void sortBrowseCandidates(ArrayList<EntityLivingBase> candidates) {
        candidates.sort((a, b) -> Integer.compare(
                this.browseOrder.getOrDefault(this.normalizeName(a.getName()), Integer.MAX_VALUE),
                this.browseOrder.getOrDefault(this.normalizeName(b.getName()), Integer.MAX_VALUE)));
    }

    private void armPendingHit(EntityLivingBase target) {
        if (target == null || target.isNull()) return;
        this.pendingHitEntityId = target.S();
        this.pendingHitDeadline = System.currentTimeMillis() + PENDING_HIT_TIMEOUT_MILLIS;
    }

    private void clearPendingHit() {
        this.pendingHitEntityId = -1;
        this.pendingHitDeadline = 0L;
    }

    private void recordCurrentTargetDamage() {
        if (this.currentTarget == null || this.currentTarget.isNull()) return;
        String targetName = this.normalizeName(this.currentTarget.getName());
        this.hitsOnCurrentTarget++;
        if (this.isBrowseMode(this.mode.getSelectedIndex())) {
            this.browsedPlayerNames.add(targetName);
        }
    }

    private void beginBrowseSelection(EntityLivingBase hitPlayer) {
        this.clearBrowseState();
        this.currentTarget = hitPlayer;
        this.awaitingBrowseSelection = false;
        this.hitsOnCurrentTarget = 0;
        this.clearPendingHit();
        this.lastSwitchTime = System.currentTimeMillis();
        this.readBrowseClan(hitPlayer,
                this.collectWorldPlayers(Minecraft.thePlayer()));
        this.rollBrowseLimit(this.mode.getSelectedIndex());
    }

    private void readBrowseClan(EntityLivingBase hitPlayer,
                                ArrayList<EntityLivingBase> worldPlayers) {
        String hitName = this.normalizeName(hitPlayer.getName());
        if (hitName.isEmpty()) return;
        Team team = null;
        try { team = hitPlayer.getTeam(); } catch (Throwable ignored) {}
        String clanKey = HideClans.getClanIdentity(hitPlayer);
        if (clanKey.isEmpty()) clanKey = "player:" + hitName;
        boolean clanChanged = !clanKey.equals(this.browseClanTag);
        if (clanChanged) {
            this.browseClanTag = clanKey;
            this.browseClanMembers.clear();
            this.browseClanDisplayNames.clear();
            this.browseOrder.clear();
            this.browsedPlayerNames.clear();
            this.completedBrowseCycleNames.clear();
        }
        if (team != null && !team.isNull()) {
            try {
                for (String member : team.getMembershipList()) {
                    this.addBrowseMember(member);
                }
            } catch (Throwable ignored) {}
        }
        this.addBrowseMember(hitPlayer.getName());
        if (worldPlayers != null && !clanKey.startsWith("player:")) {
            for (EntityLivingBase player : worldPlayers) {
                try {
                    if (clanKey.equals(HideClans.getClanIdentity(player))) {
                        this.addBrowseMember(player.getName());
                    }
                } catch (Throwable ignored) {}
            }
        }
    }

    private void addBrowseMember(String rawName) {
        String normalized = this.normalizeName(rawName);
        if (normalized.isEmpty()) return;
        if (this.browseClanMembers.add(normalized)) {
            this.browseOrder.put(normalized, this.browseOrder.size());
        }
        this.browseClanDisplayNames.put(normalized, this.cleanPlayerName(rawName));
    }

    private void retainBrowseClanCandidates(ArrayList<EntityLivingBase> candidates) {
        candidates.removeIf(candidate -> !this.browseClanMembers.contains(this.normalizeName(candidate.getName())));
    }

    private boolean shouldSwitch(int modeIndex) {
        long elapsed = System.currentTimeMillis() - this.lastSwitchTime;
        if (modeIndex == MODE_BROWSE_ALL_HITS) {
            if (this.activeBrowseHitLimit <= 0) this.rollBrowseLimit(modeIndex);
            return this.hitsOnCurrentTarget >= this.activeBrowseHitLimit;
        }
        if (modeIndex == MODE_BROWSE_ALL_TIME) {
            if (this.activeBrowseTimeLimitMillis <= 0L) this.rollBrowseLimit(modeIndex);
            return elapsed >= this.activeBrowseTimeLimitMillis;
        }
        return false;
    }

    private void rollBrowseLimit(int modeIndex) {
        if (modeIndex == MODE_BROWSE_ALL_HITS) {
            int minimum = Math.min(this.browseHits.getMinimumInt(), this.browseHits.getMaximumInt());
            int maximum = Math.max(this.browseHits.getMinimumInt(), this.browseHits.getMaximumInt());
            this.activeBrowseHitLimit = minimum == maximum
                    ? minimum : ThreadLocalRandom.current().nextInt(minimum, maximum + 1);
            this.activeBrowseTimeLimitMillis = 0L;
            return;
        }
        if (modeIndex == MODE_BROWSE_ALL_TIME) {
            long minimum = Math.round(Math.min(this.browseTimeMillis.getMinimumValue(),
                    this.browseTimeMillis.getMaximumValue()));
            long maximum = Math.round(Math.max(this.browseTimeMillis.getMinimumValue(),
                    this.browseTimeMillis.getMaximumValue()));
            this.activeBrowseTimeLimitMillis = minimum == maximum
                    ? minimum : ThreadLocalRandom.current().nextLong(minimum, maximum + 1L);
            this.activeBrowseHitLimit = 0;
        }
    }

    private void switchToNextTarget(ArrayList<EntityLivingBase> candidates) {
        String completedName = this.currentTarget == null ? "" : this.normalizeName(this.currentTarget.getName());
        if (!completedName.isEmpty()) this.completedBrowseCycleNames.add(completedName);
        if (this.hasCompletedBrowseCycle(candidates)) {
            this.browsedPlayerNames.clear();
            this.completedBrowseCycleNames.clear();
        }
        int index = this.indexOfCandidate(candidates, this.currentTarget == null ? "" : this.currentTarget.getName());
        this.currentTarget = candidates.get(index < 0 ? 0 : (index + 1) % candidates.size());
        this.browseCursorName = this.normalizeName(this.currentTarget.getName());
        this.hitsOnCurrentTarget = 0;
        this.clearPendingHit();
        this.lastSwitchTime = System.currentTimeMillis();
        this.rollBrowseLimit(this.mode.getSelectedIndex());
    }

    private boolean hasCompletedBrowseCycle(ArrayList<EntityLivingBase> candidates) {
        if (candidates.isEmpty()) return false;
        for (EntityLivingBase candidate : candidates) {
            if (!this.completedBrowseCycleNames.contains(this.normalizeName(candidate.getName()))) return false;
        }
        return true;
    }

    private boolean isBrowseMode(int modeIndex) {
        return modeIndex == MODE_BROWSE_ALL_TIME || modeIndex == MODE_BROWSE_ALL_HITS;
    }

    private boolean isBrowseHitMode() {
        return this.mode.getSelectedIndex() == MODE_BROWSE_ALL_HITS;
    }

    private EntityLivingBase findCandidate(ArrayList<EntityLivingBase> candidates, String name) {
        int index = this.indexOfCandidate(candidates, name);
        return index < 0 ? null : candidates.get(index);
    }

    private EntityLivingBase findFirstDifferentCandidate(ArrayList<EntityLivingBase> candidates, String name) {
        String normalized = this.normalizeName(name);
        for (EntityLivingBase candidate : candidates) {
            if (!normalized.equals(this.normalizeName(candidate.getName()))) return candidate;
        }
        return null;
    }

    private EntityLivingBase findNextBrowseCandidate(ArrayList<EntityLivingBase> candidates,
                                                       String previousName) {
        int previousOrder = this.browseOrder.getOrDefault(
                this.normalizeName(previousName), -1);
        EntityLivingBase first = candidates.get(0);
        for (EntityLivingBase candidate : candidates) {
            int order = this.browseOrder.getOrDefault(
                    this.normalizeName(candidate.getName()), Integer.MAX_VALUE);
            if (order > previousOrder) return candidate;
        }
        return first;
    }

    private int indexOfCandidate(ArrayList<EntityLivingBase> candidates, String name) {
        String normalized = this.normalizeName(name);
        if (normalized.isEmpty()) return -1;
        for (int i = 0; i < candidates.size(); i++) {
            if (normalized.equals(this.normalizeName(candidates.get(i).getName()))) return i;
        }
        return -1;
    }

    @EventHandler
    public void onRenderPlayer(EventRenderPlayerPre event) {
        if (event != null && event.getEntityPlayer() != null
                && this.shouldHideRenderedEntity(event.getEntityPlayer())) {
            event.setCancelled(true);
        }
    }

    /**
     * RenderPlayerPre only suppresses the player model on legacy clients.  Cancel the
     * complete RenderManager dispatch as well so hidden non-targets cannot leave a
     * shadow or burning/fire layer behind.
     */
    @EventHandler
    public void onPreRenderEntity(EventPreRenderEntity event) {
        if (event != null && this.shouldHideRenderedEntity(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPreRenderLiving(EventPreRenderLiving event) {
        if (event != null && this.shouldHideRenderedEntity(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    private boolean shouldHideRenderedEntity(Entity entity) {
        if (!this.isEnabled() || this.currentTarget == null || this.currentTarget.isNull()
                || entity == null || entity.isNull()) return false;
        try {
            Object handle = entity.getObject();
            if (Minecraft.thePlayer().isNotNull() && handle == Minecraft.thePlayer().getObject()) return false;
            if (handle == this.currentTarget.getObject()) return false;
            EntityLivingBase renderedPlayer = new EntityLivingBase(handle);
            if (!renderedPlayer.isInstance(MappedClasses.Yl)) return false;
            boolean showAll = Boolean.TRUE.equals(this.showAllPlayers.getValue());
            if (showAll) return false;
            boolean safe = this.safeHits.getEffectiveValue();
            boolean showClan = Boolean.TRUE.equals(this.showClan.getValue());
            boolean ally = safe ? HideClans.isClanMember(renderedPlayer)
                    : showClan && HideClans.isClanEntity(renderedPlayer);
            return shouldHideOtherPlayerModel(ally, showAll, showClan, safe);
        } catch (Throwable ignored) {
            return false;
        }
    }

    @EventHandler
    public void onRender2D(EventRender2D event) {
        if (HudLayout.isEditing()) return;
        this.renderInfo(event, false);
    }

    public void renderLayoutPreview(EventRender2D event) {
        this.renderInfo(event, true);
    }

    private void renderInfo(EventRender2D event, boolean preview) {
        if (!preview && (!this.isEnabled() || this.invseeSynchronization.getEffectiveValue()
                || !this.info.getEffectiveValue())) return;
        FontRenderer font = event.getFontRenderer();
        SmoothFontRenderer titleFont = this.infoFont(true);
        SmoothFontRenderer detailFont = this.infoFont(false);
        if ((font == null || font.isNull()) && (titleFont == null || detailFont == null)) return;
        boolean browseMode = this.isBrowseMode(this.mode.getSelectedIndex());
        boolean hasTarget = this.currentTarget != null && !this.currentTarget.isNull();
        if (!preview && !hasTarget
                && !(browseMode && this.awaitingBrowseSelection)) return;

        if (browseMode && this.awaitingBrowseSelection) {
            HudLayoutModel.Bounds bounds = this.infoBounds(event, 128, 30);
            double x = bounds.x, y = bounds.y;
            this.drawInfoPanel(bounds);
            this.drawInfoText(titleFont, font, "Browse All Players", x + 8, y + 6, TEXT_PRIMARY);
            this.drawInfoText(detailFont, font, "Hit an enemy to begin", x + 8, y + 18, TEXT_MUTED);
            return;
        }

        String displayName = hasTarget ? this.cleanPlayerName(this.currentTarget.getName()) : "Target Info //$ui";
        String progress = hasTarget ? this.getTargetProgressText() : "0 hits";
        int progressWidth = (int)Math.ceil(this.infoTextWidth(detailFont, font, progress));
        int memberCount = browseMode ? this.browseClanMembers.size() : 0;
        int columns = memberCount > 6 ? 2 : 1;
        int maximumNameWidth = 0;
        if (browseMode) {
            for (String name : this.browseClanMembers) {
                maximumNameWidth = Math.max(maximumNameWidth,
                        (int)Math.round(this.infoTextWidth(detailFont, font,
                                this.browseClanDisplayNames.getOrDefault(name, name))));
            }
        }
        int columnWidth = Math.min(72, Math.max(51, maximumNameWidth + 9));
        int headerWidth = (int)Math.ceil(this.infoTextWidth(titleFont, font, displayName))
                + progressWidth + 22;
        int width = Math.min(160, Math.max(118,
                Math.max(headerWidth, 16 + columns * columnWidth)));
        int rows = memberCount == 0 ? 0 : (memberCount + columns - 1) / columns;
        int headerHeight = 22;
        int height = headerHeight + (rows == 0 ? 0 : rows * 10 + 6);
        HudLayoutModel.Bounds bounds = this.infoBounds(event, width, height);
        double x = bounds.x, y = bounds.y;

        this.drawInfoPanel(bounds);
        displayName = this.fitText(titleFont, font, displayName, width - progressWidth - 22);
        this.drawInfoText(titleFont, font, displayName, x + 8,
                y + (headerHeight - this.infoTextHeight(titleFont)) * 0.5, TEXT_PRIMARY);
        this.drawInfoText(detailFont, font, progress, x + width - progressWidth - 8,
                y + (headerHeight - this.infoTextHeight(detailFont)) * 0.5, TEXT_MUTED);

        if (rows > 0) {
            GuiRenderPrimitives.j(x + 8, y + headerHeight, width - 16, 0.5, BORDER);
            int index = 0;
            for (String name : this.browseClanMembers) {
                int row = index / columns;
                int column = index % columns;
                double nameX = x + 8 + column * columnWidth;
                double nameY = y + headerHeight + 4 + row * 10;
                boolean hit = this.browsedPlayerNames.contains(name);
                Color statusColor = hit ? STATUS_HIT : STATUS_PENDING;
                GuiRenderPrimitives.e(nameX, nameY + 2, 3, 3, statusColor, false, 1.5f, 1.0f);
                String memberName = this.fitText(detailFont, font,
                        this.browseClanDisplayNames.getOrDefault(name, name), columnWidth - 9);
                this.drawInfoText(detailFont, font, memberName,
                        nameX + 7, nameY, hit ? TEXT_PRIMARY : TEXT_MUTED);
                index++;
            }
        }
    }

    private void drawInfoPanel(HudLayoutModel.Bounds bounds) {
        GuiRenderPrimitives.e(bounds.x, bounds.y, bounds.width, bounds.height,
                PANEL_BACKGROUND, false, 4.0f, 1.0f);
        GuiRenderPrimitives.P(bounds.x, bounds.y, bounds.width, bounds.height,
                BORDER, 4.0f, 0.5f, 1.0f);
    }

    private double infoTextHeight(SmoothFontRenderer smooth) {
        return smooth == null ? 9.0 : smooth.d("Ag");
    }

    private HudLayoutModel.Bounds infoBounds(EventRender2D event, int width, int height) {
        return HudLayout.resolve(HudLayout.TARGET_INFO, event.getDisplayWidth() / 2.0,
                event.getDisplayHeight() / 2.0, width, height,
                PANEL_X, panelTop(event.getDisplayHeight(), height));
    }

    /** Sits just under the top of the screen instead of being vertically centred. */
    private static int panelTop(int displayHeight, int panelHeight) {
        int preferred = Math.max(PANEL_MARGIN_TOP, (displayHeight - panelHeight) / 5);
        return Math.min(preferred, Math.max(PANEL_MARGIN_TOP, displayHeight - panelHeight - PANEL_MARGIN_TOP));
    }

    private String fitText(FontRenderer font, String text, int maximumWidth) {
        if (font.getStringWidth(text) <= maximumWidth) return text;
        String suffix = "...";
        int limit = Math.max(0, maximumWidth - font.getStringWidth(suffix));
        String shortened = text;
        while (!shortened.isEmpty() && font.getStringWidth(shortened) > limit) {
            shortened = shortened.substring(0, shortened.length() - 1);
        }
        return shortened + suffix;
    }

    /** Inter and neutral surfaces match the embedded ClickGUI. */
    private SmoothFontRenderer infoFont(boolean semibold) {
        try {
            return SonyVegas.INSTANCE.getFontManager().getHudFont("inter",
                    semibold ? 14 : 12, semibold);
        } catch (Throwable ignored) { }
        return null;
    }

    private void drawInfoText(SmoothFontRenderer smooth, FontRenderer vanilla,
                              String text, double x, double y, int color) {
        if (smooth != null) {
            smooth.v(text, x, y, new Color(color, true));
            return;
        }
        vanilla.drawStringWithShadow(text, (float)x, (float)y, color);
    }

    private double infoTextWidth(SmoothFontRenderer smooth, FontRenderer vanilla, String text) {
        return smooth != null ? smooth.N(text) : vanilla.getStringWidth(text);
    }

    private String fitText(SmoothFontRenderer smooth, FontRenderer vanilla,
                           String text, int maximumWidth) {
        if (smooth == null) return this.fitText(vanilla, text, maximumWidth);
        if (smooth.N(text) <= maximumWidth) return text;
        String suffix = "...";
        double limit = Math.max(0.0, maximumWidth - smooth.N(suffix));
        String shortened = text;
        while (!shortened.isEmpty() && smooth.N(shortened) > limit) {
            shortened = shortened.substring(0, shortened.length() - 1);
        }
        return shortened + suffix;
    }

    private String getTargetProgressText() {
        int modeIndex = this.mode.getSelectedIndex();
        if (modeIndex == MODE_BROWSE_ALL_TIME) {
            long elapsed = Math.max(0L, System.currentTimeMillis() - this.lastSwitchTime);
            long remaining = Math.max(0L, this.activeBrowseTimeLimitMillis - elapsed);
            double seconds = Math.ceil(remaining / 100.0) / 10.0;
            return String.format(Locale.ROOT, "%.1fs", seconds);
        }
        if (modeIndex == MODE_BROWSE_ALL_HITS) {
            int remaining = Math.max(0, this.activeBrowseHitLimit - this.hitsOnCurrentTarget);
            return remaining == 1 ? "1 hit restante" : remaining + " hits restantes";
        }
        return this.hitsOnCurrentTarget == 1 ? "1 hit" : this.hitsOnCurrentTarget + " hits";
    }

    private void syncHiddenBoundingBoxes(ArrayList<EntityLivingBase> worldPlayers) {
        boolean safe = this.safeHits.getEffectiveValue();
        boolean hasTarget = this.currentTarget != null && !this.currentTarget.isNull();
        if (!hasTarget && !safe) {
            EntityBoundingBoxFilterHook.clearTarget();
            return;
        }
        // getEffectiveValue() reports false whenever the setting is hidden by the GUI, which silently
        // stripped the clan exemption outside the browse modes. The stored value is what decides
        // whether an ally stays hittable, so read it directly.
        boolean showAll = Boolean.TRUE.equals(this.showAllPlayers.getValue());
        boolean spareClan = Boolean.TRUE.equals(this.showClan.getValue());
        Object targetHandle = hasTarget ? this.currentTarget.getObject() : null;
        ArrayList<Object> hiddenHandles = new ArrayList<Object>();
        ArrayList<Object> preservedAllies = new ArrayList<Object>();
        for (EntityLivingBase player : worldPlayers) {
            try {
                Object handle = player.getObject();
                boolean ally = safe ? HideClans.isClanMember(player)
                        : !showAll && spareClan && HideClans.isClanEntity(player);
                if (safe && ally) preservedAllies.add(handle);
                if (hasTarget && handle != targetHandle
                        && shouldPassThroughOtherPlayer(ally, showAll, spareClan, safe)) {
                    hiddenHandles.add(handle);
                }
            } catch (Throwable ignored) {}
        }
        EntityBoundingBoxFilterHook.updateTarget(hiddenHandles, preservedAllies);
    }

    static boolean shouldHideOtherPlayerModel(boolean ally, boolean showAll,
                                               boolean showClan, boolean safeHits) {
        return !showAll && !(ally && (showClan || safeHits));
    }

    static boolean shouldPassThroughOtherPlayer(boolean ally, boolean showAll,
                                                boolean showClan, boolean safeHits) {
        if (safeHits) return !ally;
        return !showAll && !(showClan && ally);
    }

    static boolean shouldCancelOtherPlayerAttack(boolean safeHits, boolean ally) {
        return !safeHits || !ally;
    }

    private boolean shouldCancelOtherPlayerAttack(Entity attackedPlayer) {
        boolean safe = this.safeHits.getEffectiveValue();
        return shouldCancelOtherPlayerAttack(safe,
                safe && HideClans.isClanMember(attackedPlayer));
    }

    private void restoreManagedBoundingBoxes() {
        EntityBoundingBoxFilterHook.clearTarget();
    }

    @EventHandler
    public void onRender3D(EventRender3D event) {
        if (!this.isEnabled() || this.invseeSynchronization.getEffectiveValue()
                || !this.showBox.getEffectiveValue() || Minecraft.theWorld().isNull()) return;
        if (this.currentTarget != null && !this.currentTarget.isNull()) {
            RenderUtil.k(this.currentTarget, 1.0, new Color(235, 65, 65, 135),
                    new Color(235, 65, 65, 28), event.getTicks());
        }
    }

    public boolean isActivelyManaging() {
        return this.isEnabled() && (this.invseeSynchronization.getEffectiveValue()
                || this.automatic.getEffectiveValue())
                && this.currentTarget != null && !this.currentTarget.isNull();
    }

    /**
     * Target is the only module allowed to opt a clan member into Aura.  This
     * keeps the global clan safety filter intact while letting Browse/Manual
     * attack the exact player Target selected.
     */
    public boolean isCurrentAuraTarget(EntityLivingBase candidate) {
        return this.isEnabled() && candidate != null && !candidate.isNull()
                && this.currentTarget != null && !this.currentTarget.isNull()
                && candidate.getObject() == this.currentTarget.getObject();
    }

    public String getSimpleSuffix() {
        if (this.invseeSynchronization.getEffectiveValue()) return "Invsee";
        return this.automatic.getEffectiveValue() ? this.mode.getDisplayValue() : "Manual";
    }

    /**
     * The current target's name, in automatic and in manual mode alike - the
     * mode name only shows while nobody is being targeted.  "Info" adds the
     * health readout on top of the name.
     */
    @Override
    public String getDetailedSuffix() {
        if (this.currentTarget == null || this.currentTarget.isNull()) return this.getSimpleSuffix();
        String name = cleanPlayerName(this.currentTarget.getName());
        if (name == null || name.isEmpty()) return this.getSimpleSuffix();
        if (!this.info.getEffectiveValue()) return name;
        float health;
        try {
            health = this.currentTarget.getRealHealth();
            if (health < 0.0f) health = this.currentTarget.w$src$F$15l9epb();
        } catch (Throwable ignored) {
            health = 0.0f;
        }
        return name + " | " + String.format(Locale.ROOT, "%.1f HP", health);
    }

    @Override
    public void onEnable() {
        if (this.invseeSynchronization.getEffectiveValue() && !this.isFollowingPartyTarget()) this.beginInvseeLink();
        if (this.browsePausedAt > 0L && this.lastSwitchTime > 0L) {
            this.lastSwitchTime += Math.max(0L,
                    System.currentTimeMillis() - this.browsePausedAt);
        }
        this.browsePausedAt = 0L;
    }

    @Override
    public void onDisable() {
        this.restoreManagedBoundingBoxes();
        if (this.invseeSynchronization.getEffectiveValue()) {
            this.clearTargetState();
        } else if (this.automatic.getEffectiveValue()
                && this.isBrowseMode(this.mode.getSelectedIndex())) {
            if (this.currentTarget != null && !this.currentTarget.isNull()) {
                this.browseCursorName = this.normalizeName(this.currentTarget.getName());
            }
            this.currentTarget = null;
            this.clearPendingHit();
            this.browsePausedAt = System.currentTimeMillis();
        } else {
            this.clearTargetState();
        }
        this.lockedTarget = "";
    }

    private void resetBrowseCacheFromControl() {
        // Reset the momentary control and persist that neutral state so a
        // loaded profile cannot clear the cache repeatedly on every tick.
        this.clearBrowseCache.setValue(false);
        this.currentTarget = null;
        this.clearBrowseState();
        this.hitsOnCurrentTarget = 0;
        this.activeBrowseHitLimit = 0;
        this.activeBrowseTimeLimitMillis = 0L;
        this.awaitingBrowseSelection = this.isBrowseMode(this.mode.getSelectedIndex());
        this.clearPendingHit();
        this.lastSwitchTime = 0L;
        this.browsePausedAt = 0L;
        this.lastMode = this.mode.getSelectedIndex();
    }

    private void clearTargetState() {
        this.currentTarget = null;
        this.breakArmorTargetName = "";
        this.breakArmorArmorMask = 0;
        this.clearBrowseState();
        this.hitsOnCurrentTarget = 0;
        this.activeBrowseHitLimit = 0;
        this.activeBrowseTimeLimitMillis = 0L;
        this.awaitingBrowseSelection = false;
        this.clearPendingHit();
        this.lastMode = -1;
    }

    private void clearBrowseState() {
        this.browseClanTag = "";
        this.browseCursorName = "";
        this.browseOrder.clear();
        this.browseClanMembers.clear();
        this.browseClanDisplayNames.clear();
        this.browsedPlayerNames.clear();
        this.completedBrowseCycleNames.clear();
    }

    public EntityLivingBase getCurrentTarget() {
        if (this.isFollowingPartyTarget() && (!this.partySelection.isFresh(System.currentTimeMillis())
                || this.partySelection.getName().isEmpty()
                || this.currentTarget != null && !this.currentTarget.isNull()
                    && !this.partySelection.getName().equalsIgnoreCase(this.currentTarget.getName()))) return null;
        return this.currentTarget;
    }

    public String getPartySyncPlayerName() {
        if (!this.isEnabled()) return "";
        if (!this.automatic.getEffectiveValue() && !this.invseeSynchronization.getEffectiveValue()) {
            String manual = this.playerName.getValue().trim();
            if (manual.matches("[A-Za-z0-9_]{1,16}")) return manual;
            return "";
        }
        EntityLivingBase selected = this.getCurrentTarget();
        if (selected != null && !selected.isNull()) return selected.getName();
        return "";
    }

    public boolean acceptPartyTargetSelection(String code, String playerName, long now) {
        boolean changed = !this.partySelection.getName().equalsIgnoreCase(playerName);
        if (this.currentTarget != null && !this.currentTarget.isNull()
                && !playerName.equalsIgnoreCase(this.currentTarget.getName())) changed = true;
        if (!this.partySelection.accept(code, playerName, now)) return false;
        // Never keep the previous local Y entity until the next selection tick.
        if (changed) {
            this.currentTarget = null;
            this.clearPendingHit();
        }
        return true;
    }

    private boolean isFollowingPartyTarget() {
        return this.partySelection.follows(RemoteControlClient.INSTANCE.getRemotePartyCode(),
                RemoteControlClient.INSTANCE.isRemotePartyLeader());
    }
    public boolean isAwaitingBrowseSelection() {
        if (this.isEnabled() && this.isFollowingPartyTarget()) {
            return !this.partySelection.isFresh(System.currentTimeMillis())
                    || this.partySelection.getName().isEmpty() || this.getCurrentTarget() == null
                    || this.currentTarget.isNull();
        }
        if (this.isInvseeSelectionRequired()) {
            return !this.invseeSnapshot.isUsable(System.currentTimeMillis()) || this.currentTarget == null
                    || this.currentTarget.isNull();
        }
        return this.isEnabled() && this.automatic.getEffectiveValue()
                && this.isBrowseMode(this.mode.getSelectedIndex()) && this.awaitingBrowseSelection;
    }
    public String getLockedTarget() { return this.lockedTarget; }
    public boolean isInvseeSelectionRequired() {
        return this.isEnabled() && this.invseeSynchronization.getEffectiveValue();
    }
    public void setLockedTarget(String name) { this.lockedTarget = name == null ? "" : name; }

    private String normalizeName(String raw) {
        return this.cleanPlayerName(raw).toLowerCase(Locale.ROOT);
    }

    static String cleanPlayerName(String raw) {
        if (raw == null) return "";
        String plain = COLOR_CODE.matcher(raw).replaceAll("");
        return CLAN_TAG.matcher(plain).replaceAll(" ").trim();
    }

    private static final class ArmorStats {
        int pieces;
        int remainingDurability;
        float remainingPercent;
        int signature = 1;
    }

    private static final class InvseeSnapshot {
        final List<InvseeTargetCandidatesValue.RankedPlayer> players;
        final long updatedAt;
        final long expiresAt;
        final String status;
        final boolean paired;

        InvseeSnapshot(List<InvseeTargetCandidatesValue.RankedPlayer> players,
                       long updatedAt,
                       long expiresAt) {
            this(players, updatedAt, expiresAt, "No living players in report //$ui", true);
        }

        InvseeSnapshot(List<InvseeTargetCandidatesValue.RankedPlayer> players,
                       long updatedAt, long expiresAt, String status, boolean paired) {
            this.players = Collections.unmodifiableList(
                    new ArrayList<InvseeTargetCandidatesValue.RankedPlayer>(players));
            this.updatedAt = updatedAt;
            this.expiresAt = expiresAt;
            this.status = status;
            this.paired = paired;
        }

        static InvseeSnapshot unavailable() {
            return waiting("Waiting for Invsee report //$ui");
        }
        static InvseeSnapshot waiting(String status) {
            return new InvseeSnapshot(Collections.<InvseeTargetCandidatesValue.RankedPlayer>emptyList(), 0L, 0L, status, false);
        }

        boolean hasSamePresentationAs(InvseeSnapshot other) {
            if (other == null || this.updatedAt != other.updatedAt
                    || this.expiresAt != other.expiresAt
                    || !this.status.equals(other.status)
                    || this.players.size() != other.players.size()) return false;
            for (int index = 0; index < this.players.size(); ++index) {
                InvseeTargetCandidatesValue.RankedPlayer left = this.players.get(index);
                InvseeTargetCandidatesValue.RankedPlayer right = other.players.get(index);
                if (!left.getName().equalsIgnoreCase(right.getName())
                        || left.getSets() != right.getSets()
                        || left.getHelmet() != right.getHelmet()
                        || left.getChestplate() != right.getChestplate()
                        || left.getLeggings() != right.getLeggings()
                        || left.getBoots() != right.getBoots()) return false;
            }
            return true;
        }

        boolean isUsable(long now) {
            return this.expiresAt > now;
        }
    }
}

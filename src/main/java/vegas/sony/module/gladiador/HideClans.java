package vegas.sony.module.gladiador;

import vegas.sony.SonyVegas;
import vegas.sony.event.EventHandler;
import vegas.sony.event.impl.EventPreAttack;
import vegas.sony.event.impl.EventPreRenderEntity;
import vegas.sony.event.impl.EventPreRenderLiving;
import vegas.sony.event.impl.EventPreTick;
import vegas.sony.event.impl.EventRender3D;
import vegas.sony.event.impl.EventRenderPlayerPre;
import vegas.sony.mapping.EntityBoundingBoxFilterHook;
import vegas.sony.module.Category;
import vegas.sony.module.Mod;
import vegas.sony.utils.render.RenderUtil;
import vegas.sony.utils.MutableColor;
import vegas.sony.value.BooleanValue;
import vegas.sony.value.ColorValue;
import vegas.sony.value.ModeValue;
import vegas.sony.value.NumberValue;
import vegas.sony.value.PiercingManualPlayersValue;
import vegas.sony.unmap.ModeOption;
import vegas.sony.wrapper.impl.Entity;
import vegas.sony.wrapper.impl.EntityLivingBase;
import vegas.sony.wrapper.impl.ForgeVersion;
import vegas.sony.wrapper.impl.GameProfile;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.PlayerInfo;
import vegas.sony.wrapper.impl.ScorePlayerTeam;
import vegas.sony.wrapper.impl.Scoreboard;
import vegas.sony.wrapper.impl.Team;
import vegas.sony.wrapper.impl.World;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.awt.Color;

public class HideClans extends Mod {
    private final ModeValue piercingMode;
    private final ModeOption blatantMode;
    private final ModeOption renderOnlyMode;
    private final BooleanValue showAllies;
    private final ModeValue showAlliesMode;
    private final ModeOption nearestMode;
    private final ModeOption semiAutoMode;
    private final ModeOption manualMode;
    private final NumberValue nearestHiddenAlliesCount;
    private final NumberValue semiAutoHiddenAlliesCount;
    private final PiercingManualPlayersValue manualHiddenAllies;
    private final BooleanValue showBox;
    private final BooleanValue showEnemiesBox;
    private final ColorValue alliesBoxColor;
    private final ColorValue enemiesBoxColor;

    private final LinkedHashSet<String> hiddenAllies = new LinkedHashSet<String>();
    private final LinkedHashSet<String> semiAutoVisibleAllies = new LinkedHashSet<String>();
    private final ArrayList<EntityLivingBase> boxAllies = new ArrayList<EntityLivingBase>();
    private boolean lastShowAllies = false;
    private String lastShowAlliesMode = "";
    private boolean wasEnabled = false;
    private int refreshTicks = 0;
    private int desiredNearestCount = 1;
    private int desiredSemiAutoCount = 1;
    private int semiAutoSnapshotCount = -1;
    private boolean semiAutoSnapshotReady;
    private String currentClanDisplayTag = "";

    private static final long MODULE_COLOR = -1033271949203766542L;
    private static final int REFRESH_INTERVAL_TICKS = 3;

    // These run per entity per tick from both HideClans and Target; String#replaceAll would
    // recompile each of them on every single call.
    private static final java.util.regex.Pattern COLOR_CODE = java.util.regex.Pattern.compile("§.");
    private static final java.util.regex.Pattern NON_ALPHANUMERIC = java.util.regex.Pattern.compile("[^a-z0-9]");
    private static final java.util.regex.Pattern TRAILING_CLAN_TAG =
            java.util.regex.Pattern.compile("\\[([^\\[\\]]{1,16})\\]\\s*$");
    private static final java.util.regex.Pattern BRACKETED_CLAN_TAG =
            java.util.regex.Pattern.compile("\\[([^\\[\\]]{1,16})\\]");

    private static final long CLAN_LOOKUP_CACHE_TTL_MS = 50L;
    private static final java.util.concurrent.ConcurrentHashMap<Object, Boolean> clanLookupCache =
            new java.util.concurrent.ConcurrentHashMap<Object, Boolean>();
    private static volatile long clanLookupCacheStamp;

    public HideClans() {
        super("Piercing //$ui", (int) MODULE_COLOR, Category.GLADIADOR, "Hides allied players using team and clan rules. //$ui");
        this.blatantMode = new ModeOption("Blatant //$ui");
        this.renderOnlyMode = new ModeOption("Render only //$ui");
        this.piercingMode = ModeValue.create((Object) this, "Mode //$ui",
                "Blatant removes allies from render, targeting and hit detection. Render only hides the model while leaving its entity and hitbox intact. //$ui",
                this.blatantMode, this.blatantMode, this.renderOnlyMode);
        this.showAllies = BooleanValue.create(this, "ShowAllies //$ui", false);
        this.nearestMode = new ModeOption("Nearest //$ui");
        this.semiAutoMode = new ModeOption("SemiAuto //$ui");
        this.manualMode = new ModeOption("Manual //$ui");
        this.showAlliesMode = ModeValue.create((Object) this, "ShowAlliesMode //$ui", this.nearestMode, this.nearestMode, this.semiAutoMode, this.manualMode);
        this.nearestHiddenAlliesCount = NumberValue.create(this, "Nearest Players //$ui", "#", "", 1, 1, 16, 1);
        this.semiAutoHiddenAlliesCount = NumberValue.create(this, "SemiAuto Players //$ui", "#", "", 1, 1, 16, 1);
        this.manualHiddenAllies = PiercingManualPlayersValue.create(this);
        this.showBox = BooleanValue.create(this, "ShowBox //$ui", false);
        this.showEnemiesBox = BooleanValue.create(this, "ShowEnemiesBox //$ui", false);
        this.alliesBoxColor = ColorValue.create(this, "AlliesBoxColor //$ui", new Color(255, 255, 255, 135));
        this.enemiesBoxColor = ColorValue.create(this, "EnemiesBoxColor //$ui", new Color(255, 0, 0, 135));
        this.showAllies.setDescription("Enable a limit instead of hiding every ally. //$ui");
        this.showAlliesMode.setDescription("How the allies to hide are selected. //$ui");
        this.nearestHiddenAlliesCount.setDescription("Number of nearest allies that should remain visible. //$ui");
        this.semiAutoHiddenAlliesCount.setDescription("Number of allies locked as visible until they leave. //$ui");
        this.showAllies.addDependentValues(this.showAlliesMode, this.nearestHiddenAlliesCount,
                this.semiAutoHiddenAlliesCount, this.manualHiddenAllies);
        this.showAlliesMode.addModeDependentValues(this.nearestMode, this.nearestHiddenAlliesCount);
        this.showAlliesMode.addModeDependentValues(this.semiAutoMode, this.semiAutoHiddenAlliesCount);
        this.showAlliesMode.addModeDependentValues(this.manualMode, this.manualHiddenAllies);
        // Color pickers only show when their box option is enabled
        this.showBox.addDependentValues(this.alliesBoxColor);
        this.showEnemiesBox.addDependentValues(this.enemiesBoxColor);
        this.addValue(this.piercingMode, this.showAllies, this.showAlliesMode, this.nearestHiddenAlliesCount, this.semiAutoHiddenAlliesCount,
                this.manualHiddenAllies, this.showBox, this.showEnemiesBox, this.alliesBoxColor, this.enemiesBoxColor);
    }

    public String getSimpleSuffix() {
        return "";
    }

    @Override
    public boolean matchesName(String candidate) {
        return super.matchesName(candidate) || "HideClans".equalsIgnoreCase(candidate);
    }

    @Override
    public String getDetailedSuffix() {
        return "";
    }

    @EventHandler
    public void onTick(EventPreTick event) {
        if (Minecraft.theWorld().isNull() || Minecraft.thePlayer().isNull()) {
            this.currentClanDisplayTag = "";
            this.restoreAll();
            return;
        }
        this.refreshCurrentClanDisplayTag();
        if (!this.isEnabled()) {
            if (this.wasEnabled) {
                this.restoreAll();
                this.wasEnabled = false;
            }
            return;
        }
        boolean refreshNow = !this.wasEnabled || ++this.refreshTicks >= REFRESH_INTERVAL_TICKS;
        this.wasEnabled = true;
        if (!refreshNow) return;
        this.refreshTicks = 0;
        this.updateHiddenAllies();
        this.publishHiddenBoundingBoxes();
    }

    @EventHandler
    public void onRenderPlayer(EventRenderPlayerPre event) {
        if (!this.isEnabled() || event == null || event.getEntityPlayer() == null) return;
        if (this.hiddenAllies.contains(normalizeNameStatic(event.getEntityPlayer().getName()))
                && !isProtectedByTargetSafeHits(event.getEntityPlayer())) {
            event.setCancelled(true);
        }
    }

    /**
     * Cancels the complete entity render before RenderManager dispatches to RenderPlayer.
     * RenderPlayerPre alone is too late on legacy versions: RenderManager can still draw the
     * entity shadow and the burning/fire pass after the player model has been cancelled.
     */
    @EventHandler
    public void onPreRenderEntity(EventPreRenderEntity event) {
        if (event != null && shouldHideEntity(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    /**
     * Equivalent early-render guard for versions whose renderer exposes the living-entity event.
     */
    @EventHandler
    public void onPreRenderLiving(EventPreRenderLiving event) {
        if (event != null && shouldHideEntity(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPreAttack(EventPreAttack event) {
        if (!this.isEnabled() || event == null || event.getTarget() == null || event.getTarget().isNull()) return;
        if (this.isRenderOnly()) return;
        if (this.hiddenAllies.contains(normalizeName(event.getTarget().getName()))
                && !isProtectedByTargetSafeHits(event.getTarget())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onRender3D(EventRender3D event) {
        if (!this.isEnabled() || Minecraft.theWorld().isNull()) return;
        boolean showAlliesBox = this.showBox.getEffectiveValue();
        boolean showEnemiesBox = this.showEnemiesBox.getEffectiveValue();
        if (!showAlliesBox && !showEnemiesBox) return;

        MutableColor alliesOutlineMutable = this.alliesBoxColor.getMutableColor();
        Color alliesOutlineColor = new Color(alliesOutlineMutable.getRed(), alliesOutlineMutable.getGreen(), alliesOutlineMutable.getBlue(), alliesOutlineMutable.getAlpha());
        Color alliesFillColor = new Color(alliesOutlineColor.getRed(), alliesOutlineColor.getGreen(), alliesOutlineColor.getBlue(), 28);
        
        MutableColor enemiesOutlineMutable = this.enemiesBoxColor.getMutableColor();
        Color enemiesOutlineColor = new Color(enemiesOutlineMutable.getRed(), enemiesOutlineMutable.getGreen(), enemiesOutlineMutable.getBlue(), enemiesOutlineMutable.getAlpha());
        Color enemiesFillColor = new Color(enemiesOutlineColor.getRed(), enemiesOutlineColor.getGreen(), enemiesOutlineColor.getBlue(), 28);

        // Render ally boxes (hidden allies)
        if (showAlliesBox) {
            for (EntityLivingBase player : this.boxAllies) {
                try {
                    if (player.isNull() || !this.hiddenAllies.contains(normalizeName(player.getName()))
                            || isProtectedByTargetSafeHits(player)) continue;
                    RenderUtil.k(player, 0.0, alliesOutlineColor, alliesFillColor, event.getTicks());
                } catch (Throwable ignored) {}
            }
        }

        // Render enemy boxes (all non-ally players)
        if (showEnemiesBox) {
            Collection<?> worldEntities;
            try { worldEntities = Minecraft.theWorld().z(); } catch (Throwable t) { return; }
            for (Object obj : worldEntities) {
                try {
                    Entity e = new Entity(obj);
                    if (!e.isInstance(vegas.sony.mapping.MappedClasses.Yl)) continue;
                    EntityLivingBase player = new EntityLivingBase(obj);
                    if (player.isNull() || player.getObject() == Minecraft.thePlayer().getObject()) continue;
                    // Enemy = NOT an ally (not in clan/team)
                    if (this.isAlly(player)) continue;
                    if (isProtectedByTargetSafeHits(player)) continue;
                    RenderUtil.k(player, 0.0, enemiesOutlineColor, enemiesFillColor, event.getTicks());
                } catch (Throwable ignored) {}
            }
        }
    }

    @Override
    public void onEnable() {
        this.resetSemiAutoSnapshot();
        this.refreshTicks = REFRESH_INTERVAL_TICKS;
    }

    @Override
    public void onDisable() {
        this.hiddenAllies.clear();
        this.resetSemiAutoSnapshot();
        this.restoreAll();
        this.wasEnabled = false;
        this.lastShowAllies = false;
        this.lastShowAlliesMode = "";
    }

    /**
     * Resolving an ally walks teams, display names and clan tags through the reflective mapping
     * layer. Target asks this for every player twice a tick, so the answer is memoized for roughly
     * one tick - long enough to collapse the duplicate work, short enough to track team changes.
     */
    public static boolean isClanEntity(Entity entity) {
        if (entity == null || entity.isNull()) return false;
        HideClans inst = SonyVegas.INSTANCE.getModManager().getMod(HideClans.class);
        // Render-only mode must behave exactly as a render cancellation. Do not
        // let target filters treat the hidden model as a non-interactable entity.
        if (inst != null && inst.isEnabled() && inst.isRenderOnly()) return false;
        return isClanMember(entity);
    }

    /** Team/clan identity regardless of Piercing's render-only mode. */
    public static boolean isClanMember(Entity entity) {
        if (entity == null || entity.isNull()) return false;
        Object handle = entity.getObject();
        if (handle == null) return false;
        long now = System.currentTimeMillis();
        if (now - clanLookupCacheStamp > CLAN_LOOKUP_CACHE_TTL_MS) {
            clanLookupCache.clear();
            clanLookupCacheStamp = now;
        }
        Boolean cached = clanLookupCache.get(handle);
        if (cached != null) return cached.booleanValue();
        boolean isClan = computeClanEntity(handle);
        clanLookupCache.put(handle, Boolean.valueOf(isClan));
        return isClan;
    }

    private static boolean computeClanEntity(Object handle) {
        try {
            EntityLivingBase base = new EntityLivingBase(handle);
            return base.isInstance(vegas.sony.mapping.MappedClasses.Yl) && isAllyStatic(base);
        } catch (Throwable t) { return false; }
    }

    public static boolean shouldHideEntity(Entity entity) {
        if (entity == null || entity.isNull()) return false;
        try {
            HideClans inst = SonyVegas.INSTANCE.getModManager().getMod(HideClans.class);
            if (inst == null || !inst.isEnabled()) return false;
            if (!(entity.getObject() instanceof Object)) return false;
            EntityLivingBase base = new EntityLivingBase(entity.getObject());
            if (!base.isInstance(vegas.sony.mapping.MappedClasses.Yl)) return false;
            String norm = normalizeNameStatic(base.getName());
            return inst.hiddenAllies.contains(norm)
                    && !isProtectedByTargetSafeHits(entity);
        } catch (Throwable t) { return false; }
    }

    private static boolean isProtectedByTargetSafeHits(Entity entity) {
        if (entity == null || entity.isNull() || SonyVegas.INSTANCE == null
                || SonyVegas.INSTANCE.getModManager() == null) return false;
        try {
            Target target = SonyVegas.INSTANCE.getModManager().getMod(Target.class);
            return target != null && target.isEnabled() && target.safeHits.getEffectiveValue()
                    && isClanMember(entity);
        } catch (Throwable ignored) { return false; }
    }

    private static boolean isAllyStatic(EntityLivingBase entity) {
        try {
            HideClans inst = SonyVegas.INSTANCE.getModManager().getMod(HideClans.class);
            if (inst == null) {
                return isAllyNoInstance(entity);
            }
            return inst.isAlly(entity);
        } catch (Throwable t) { return false; }
    }

    private static boolean isAllyNoInstance(EntityLivingBase entity) {
        try {
            if (entity == null || entity.isNull()) return false;
            if (Minecraft.theWorld().isNull() || Minecraft.thePlayer().isNull()) return false;
            EntityLivingBase local = new EntityLivingBase(Minecraft.thePlayer().getObject());
            if (entity.getObject() == local.getObject()) return false;
            String localTrailingClan = trailingClanTagStatic(local);
            String entityTrailingClan = trailingClanTagStatic(entity);
            if (!localTrailingClan.isEmpty() || !entityTrailingClan.isEmpty() || isCheatBreaker()) {
                return !localTrailingClan.isEmpty() && localTrailingClan.equals(entityTrailingClan);
            }
            Team localTeam = local.getTeam();
            Team entityTeam = entity.getTeam();
            if (localTeam != null && entityTeam != null) {
                String lr = safeRegistered(localTeam);
                String er = safeRegistered(entityTeam);
                if (!lr.isEmpty() && lr.equals(er)) return true;
                try { if (localTeam.isSameTeam(entityTeam)) return true; } catch (Throwable ignored) {}
            }
            String localClan = clanTagStatic(local);
            String entityClan = clanTagStatic(entity);
            return !localClan.isEmpty() && localClan.equals(entityClan);
        } catch (Throwable t) { return false; }
    }

    private static String safeRegistered(Team team) {
        try { String s = team.getRegisteredName(); return s == null ? "" : s; } catch (Throwable t) { return ""; }
    }

    public boolean isAlly(EntityLivingBase entity) {
        if (entity == null || entity.isNull()) return false;
        EntityLivingBase localPlayer;
        try { localPlayer = new EntityLivingBase(Minecraft.thePlayer().getObject()); } catch (Throwable t) { return false; }
        if (entity.getObject() == localPlayer.getObject()) return false;
        String localTrailingClan = trailingClanTagStatic(localPlayer);
        String entityTrailingClan = trailingClanTagStatic(entity);
        if (!localTrailingClan.isEmpty() || !entityTrailingClan.isEmpty() || isCheatBreaker()) {
            return !localTrailingClan.isEmpty() && localTrailingClan.equals(entityTrailingClan);
        }
        Team localTeam = null;
        Team entityTeam = null;
        try { localTeam = localPlayer.getTeam(); } catch (Throwable ignored) {}
        try { entityTeam = entity.getTeam(); } catch (Throwable ignored) {}
        if (localTeam != null && entityTeam != null) {
            String localRegistered = "";
            String entityRegistered = "";
            try { localRegistered = localTeam.getRegisteredName(); } catch (Throwable ignored) {}
            try { entityRegistered = entityTeam.getRegisteredName(); } catch (Throwable ignored) {}
            if (localRegistered == null) localRegistered = "";
            if (entityRegistered == null) entityRegistered = "";
            if (!localRegistered.isEmpty() && localRegistered.equals(entityRegistered)) return true;
            try { if (localTeam.isSameTeam(entityTeam)) return true; } catch (Throwable ignored) {}
        }
        String localClanTag = "";
        String entityClanTag = "";
        try { localClanTag = clanTag(localPlayer); } catch (Throwable ignored) {}
        try { entityClanTag = clanTag(entity); } catch (Throwable ignored) {}
        if (!localClanTag.isEmpty() && localClanTag.equals(entityClanTag)) return true;
        return false;
    }

    private static boolean isCheatBreaker() {
        return ForgeVersion.MC_1_7_10.L() && !vegas.sony.wrapper.Wrapper.sonyVegasInstance.isVanillaMinecraftPresent();
    }

    private static String trailingClanTagStatic(EntityLivingBase entity) {
        if (entity == null || entity.isNull()) return "";
        ArrayList<String> candidates = new ArrayList<String>();
        try {
            Team team = entity.getTeam();
            if (team != null && !team.isNull()) {
                ScorePlayerTeam scoreTeam = new ScorePlayerTeam(team.getObject());
                candidates.add(scoreTeam.getSuffix());
                candidates.add(team.getColorSuffix());
            }
        } catch (Throwable ignored) {}
        try { candidates.add(entity.getFormattedDisplayName()); } catch (Throwable ignored) {}
        for (String candidate : candidates) {
            String plain = stripColorStatic(candidate).trim();
            java.util.regex.Matcher matcher = TRAILING_CLAN_TAG.matcher(plain);
            if (matcher.find()) {
                String tag = normalizeTagStatic(matcher.group(1));
                if (tag.length() >= 2) return tag;
            }
        }
        return "";
    }

    private static String clanTagStatic(EntityLivingBase entity) {
        try {
            Team team = entity.getTeam();
            if (team != null) {
                // try ScorePlayerTeam suffix/prefix
                try {
                    ScorePlayerTeam spt = new ScorePlayerTeam(team.getObject());
                    String suffix = normalizeTagStatic(spt.getSuffix());
                    if (suffix.length() >= 2) return suffix;
                    String prefix = normalizeTagStatic(spt.getPrefix());
                    if (prefix.length() >= 2) return prefix;
                } catch (Throwable ignored) {}
                try {
                    String suffix = normalizeTagStatic(team.getColorSuffix());
                    if (suffix.length() >= 2) return suffix;
                    String prefix = normalizeTagStatic(team.getColorPrefix());
                    if (prefix.length() >= 2) return prefix;
                } catch (Throwable ignored) {}
            }
            String plainName = stripColorStatic(entity.getName());
            String plainDisplay = stripColorStatic(entity.getFormattedDisplayName());
            String cleaned = normalizeTagStatic(plainDisplay.replaceFirst(java.util.regex.Pattern.quote(plainName), ""));
            return cleaned.length() >= 2 ? cleaned : "";
        } catch (Throwable t) { return ""; }
    }

    /**
     * Stable identity used when a server represents each player with an
     * individual scoreboard team. CheatBreaker clans are normally exposed as a
     * trailing [TAG], so prefer that over the registered team name.
     */
    public static String getClanIdentity(EntityLivingBase entity) {
        if (entity == null || entity.isNull()) return "";
        String trailing = trailingClanTagStatic(entity);
        if (!trailing.isEmpty()) return "tag:" + trailing;
        String tag = clanTagStatic(entity);
        if (!tag.isEmpty()) return "tag:" + tag;
        try {
            Team team = entity.getTeam();
            String registered = team == null || team.isNull()
                    ? "" : normalizeTagStatic(team.getRegisteredName());
            return registered.isEmpty() ? "" : "team:" + registered;
        } catch (Throwable ignored) {
            return "";
        }
    }

    private String clanTag(EntityLivingBase entity) {
        Team team = entity.getTeam();
        if (team != null) {
            try {
                ScorePlayerTeam spt = new ScorePlayerTeam(team.getObject());
                String suffix = normalizeTag(spt.getSuffix());
                if (suffix.length() >= 2) return suffix;
                String prefix = normalizeTag(spt.getPrefix());
                if (prefix.length() >= 2) return prefix;
            } catch (Throwable ignored) {}
            try {
                String suffix = normalizeTag(team.getColorSuffix());
                if (suffix.length() >= 2) return suffix;
                String prefix = normalizeTag(team.getColorPrefix());
                if (prefix.length() >= 2) return prefix;
            } catch (Throwable ignored) {}
        }
        String plainName = stripColor(entity.getName());
        String plainDisplay = "";
        try { plainDisplay = stripColor(entity.getFormattedDisplayName()); } catch (Throwable ignored) { plainDisplay = plainName; }
        String cleaned = normalizeTag(plainDisplay.replaceFirst(java.util.regex.Pattern.quote(plainName == null ? "" : plainName), ""));
        return cleaned.length() >= 2 ? cleaned : "";
    }

    /**
     * The matching helpers intentionally normalize a tag, but the array list needs the
     * original scoreboard presentation. Keep the original case and formatting codes here.
     */
    private String getCurrentClanDisplayTag() {
        return this.currentClanDisplayTag;
    }

    private void refreshCurrentClanDisplayTag() {
        try {
            EntityLivingBase localPlayer = new EntityLivingBase(Minecraft.thePlayer().getObject());
            String detected = findDisplayClanTag(localPlayer);
            if (detected.isEmpty()) {
                World world = Minecraft.theWorld();
                Scoreboard scoreboard = world == null || world.isNull() ? null : world.getScoreboard();
                detected = extractDirectClanTag(localPlayer.getFormattedClanTag(scoreboard));
                if (detected.isEmpty() && scoreboard != null) {
                    detected = displayRegisteredTeamTag(scoreboard.getPlayersTeam(localPlayer.getName()));
                }
            }
            if (!detected.isEmpty()) this.currentClanDisplayTag = detected;
        } catch (Throwable ignored) {
            // The old tag remains visible while the scoreboard reloads.
        }
    }

    private static String findDisplayClanTag(EntityLivingBase entity) {
        if (entity == null || entity.isNull()) return "";
        try {
            Team team = entity.getTeam();
            if (team != null && !team.isNull()) {
                try {
                    ScorePlayerTeam scoreTeam = new ScorePlayerTeam(team.getObject());
                    String tag = extractDirectClanTag(scoreTeam.getSuffix());
                    if (!tag.isEmpty()) return tag;
                    tag = extractDirectClanTag(scoreTeam.getPrefix());
                    if (!tag.isEmpty()) return tag;
                } catch (Throwable ignored) { }
                try {
                    String tag = extractDirectClanTag(team.getColorSuffix());
                    if (!tag.isEmpty()) return tag;
                    tag = extractDirectClanTag(team.getColorPrefix());
                    if (!tag.isEmpty()) return tag;
                } catch (Throwable ignored) { }
            }
        } catch (Throwable ignored) { }
        try {
            String tag = extractAdjacentDisplayClanTag(entity.getFormattedDisplayName(), entity.getName());
            if (!tag.isEmpty()) return tag;
        } catch (Throwable ignored) { }
        try {
            return displayRegisteredTeamTag(entity.getTeam());
        } catch (Throwable ignored) { }
        return "";
    }

    private static String displayRegisteredTeamTag(Team team) {
        if (team == null || team.isNull()) return "";
        try {
            String registered = team.getRegisteredName();
            String normalized = normalizeTagStatic(registered);
            if (normalized.length() < 2 || normalized.length() > 16) return "";
            String prefix = team.getColorPrefix();
            String suffix = team.getColorSuffix();
            return (prefix == null ? "" : prefix)
                    + registered + (suffix == null ? "" : suffix);
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static String extractFormattedClanTag(String formattedText) {
        if (formattedText == null || formattedText.isEmpty()) return "";
        StringBuilder visible = new StringBuilder();
        ArrayList<Integer> originalOffsets = new ArrayList<Integer>();
        for (int index = 0; index < formattedText.length(); ++index) {
            char character = formattedText.charAt(index);
            if (character == '\u00a7' && index + 1 < formattedText.length()) {
                ++index;
                continue;
            }
            visible.append(character);
            originalOffsets.add(Integer.valueOf(index));
        }
        java.util.regex.Matcher matcher = TRAILING_CLAN_TAG.matcher(visible);
        if (!matcher.find()) return "";
        int start = originalOffsets.get(matcher.start()).intValue();
        int end = originalOffsets.get(matcher.end() - 1).intValue() + 1;
        // Color/formatting codes directly before the opening bracket apply to the tag too.
        while (start >= 2 && formattedText.charAt(start - 2) == '\u00a7') {
            start -= 2;
        }
        // Preserve a trailing reset code so the tag cannot tint following HUD text.
        while (end + 1 < formattedText.length() && formattedText.charAt(end) == '\u00a7') {
            end += 2;
        }
        return formattedText.substring(start, Math.min(end, formattedText.length()));
    }

    /** A team prefix/suffix is itself the tag, including servers that do not use brackets. */
    private static String extractDirectClanTag(String formattedText) {
        String bracketed = extractFormattedClanTag(formattedText);
        if (!bracketed.isEmpty()) return bracketed;
        if (formattedText == null) return "";
        String visible = stripColorStatic(formattedText).trim();
        if (!visible.matches("[A-Za-z0-9_-]{2,16}")) return "";
        return formattedText.trim();
    }

    /**
     * A display name also contains the player name, so it must not be treated
     * as a direct tag. Read a bracketed tag anywhere in it, then only accept a
     * compact token directly before or after the player's name.
     */
    private static String extractAdjacentDisplayClanTag(String formattedText, String playerName) {
        String bracketed = extractFormattedClanTag(formattedText);
        if (!bracketed.isEmpty() || formattedText == null || playerName == null || playerName.isEmpty()) {
            return bracketed;
        }
        String visible = stripColorStatic(formattedText);
        int playerIndex = visible.toLowerCase(Locale.ROOT).lastIndexOf(playerName.toLowerCase(Locale.ROOT));
        if (playerIndex < 0) return "";
        String suffix = visible.substring(playerIndex + playerName.length()).trim();
        if (suffix.matches("[A-Za-z0-9_-]{2,16}")) {
            return preserveFormattedToken(formattedText, suffix, false);
        }
        String prefix = visible.substring(0, playerIndex).trim();
        if (prefix.matches("[A-Za-z0-9_-]{2,16}")) {
            return preserveFormattedToken(formattedText, prefix, true);
        }
        return "";
    }

    private static String preserveFormattedToken(String formattedText, String token, boolean fromStart) {
        int visibleSeen = 0;
        int start = -1;
        for (int index = 0; index < formattedText.length(); ++index) {
            if (formattedText.charAt(index) == '\u00a7' && index + 1 < formattedText.length()) {
                ++index;
                continue;
            }
            if (visibleSeen == (fromStart ? 0 : stripColorStatic(formattedText).lastIndexOf(token))) {
                start = index;
                while (start >= 2 && formattedText.charAt(start - 2) == '\u00a7') start -= 2;
                break;
            }
            ++visibleSeen;
        }
        if (start < 0) return token;
        int end = start;
        int remaining = token.length();
        while (end < formattedText.length() && remaining > 0) {
            if (formattedText.charAt(end) == '\u00a7' && end + 1 < formattedText.length()) {
                end += 2;
            } else {
                ++end;
                --remaining;
            }
        }
        return formattedText.substring(start, Math.min(end, formattedText.length()));
    }

    private static String normalizeTagStatic(String tag) {
        return NON_ALPHANUMERIC.matcher(stripColorStatic(tag == null ? "" : tag).toLowerCase(Locale.ROOT)).replaceAll("");
    }

    private String normalizeTag(String tag) {
        if (tag == null) return "";
        return NON_ALPHANUMERIC.matcher(stripColor(tag).toLowerCase(Locale.ROOT)).replaceAll("");
    }

    private static String normalizeNameStatic(String name) {
        return stripColorStatic(name == null ? "" : name).trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeName(String name) {
        if (name == null) return "";
        return stripColor(name).trim().toLowerCase(Locale.ROOT);
    }

    private static String stripColorStatic(String text) {
        if (text == null) return "";
        return COLOR_CODE.matcher(text).replaceAll("");
    }

    private String stripColor(String text) {
        if (text == null) return "";
        return COLOR_CODE.matcher(text).replaceAll("");
    }

    private boolean isAllyColor(String formattedName) {
        if (formattedName == null) return false;
        boolean hasGreen = formattedName.contains("\u00a7a") || formattedName.contains("\u00A7a") || formattedName.contains("\u00a72") || formattedName.contains("\u00A72");
        boolean hasRed = formattedName.contains("\u00a7c") || formattedName.contains("\u00A7c") || formattedName.contains("\u00A74") || formattedName.contains("\u00A74");
        // case-insensitive already, but ensure
        String lower = formattedName.toLowerCase(Locale.ROOT);
        hasGreen = lower.contains("\u00a7a") || lower.contains("\u00a72");
        hasRed = lower.contains("\u00a7c") || lower.contains("\u00a74");
        return hasGreen && !hasRed;
    }

    private String clanKey(EntityLivingBase entity) {
        Team team = entity.getTeam();
        String registered = "";
        try { if (team != null) registered = normalizeTag(team.getRegisteredName()); } catch (Throwable ignored) {}
        if (!registered.isEmpty()) return "team:" + registered;
        String tag = "";
        try { tag = clanTag(entity); } catch (Throwable ignored) {}
        return tag.isEmpty() ? "" : "tag:" + tag;
    }

    private void updateHiddenAllies() {
        EntityLivingBase localPlayer;
        try { localPlayer = new EntityLivingBase(Minecraft.thePlayer().getObject()); } catch (Throwable t) { clearState(); return; }
        Collection<?> worldEntities;
        try { worldEntities = Minecraft.theWorld().z(); } catch (Throwable t) { clearState(); return; }

        boolean modeChanged = !this.lastShowAlliesMode.equals(this.showAlliesMode.getDisplayValue());
        boolean showAlliesEnabledNow = this.showAllies.getEffectiveValue().booleanValue();
        if ((this.lastShowAllies && !showAlliesEnabledNow) || modeChanged) {
            this.semiAutoVisibleAllies.clear();
            this.semiAutoSnapshotReady = false;
        }
        if (!this.lastShowAllies && showAlliesEnabledNow) {
            this.semiAutoSnapshotReady = false;
        }
        this.lastShowAllies = showAlliesEnabledNow;
        this.lastShowAlliesMode = this.showAlliesMode.getDisplayValue();

        ArrayList<AllyInfo> allies = new ArrayList<AllyInfo>();
        for (Object obj : worldEntities) {
            try {
                Entity e = new Entity(obj);
                if (!e.isInstance(vegas.sony.mapping.MappedClasses.Yl)) continue;
                EntityLivingBase p = new EntityLivingBase(obj);
                if (p.isNull() || p.getObject() == localPlayer.getObject()) continue;
                if (!this.isAlly(p)) continue;
                float dist = 0f;
                try { dist = p.getDistanceToEntity(Minecraft.thePlayer()); } catch (Throwable ignored) {}
                allies.add(new AllyInfo(p, normalizeName(p.getName()), dist));
            } catch (Throwable ignored) {}
        }
        this.syncHiddenAlliesSliderBounds(allies.size());
        ArrayList<String> manualCandidates = new ArrayList<String>();
        allies.sort((a, b) -> Float.compare(a.distance, b.distance));
        for (AllyInfo ally : allies) manualCandidates.add(ally.displayName);
        // World entities only cover the loaded render area.  Manual selection
        // must expose the complete clan, so merge the authenticated Tab-list
        // snapshot where distant online members remain available.
        manualCandidates.addAll(this.collectOnlineClanMembers(localPlayer));
        this.manualHiddenAllies.updateCandidates(manualCandidates);

        Set<String> visibleAllies = new LinkedHashSet<String>();
        boolean showAlliesEnabled = showAlliesEnabledNow;
        if (!showAlliesEnabled) {
            // An empty visible set means every detected clan member is hidden.
        } else if (this.nearestMode.isSelected()) {
            allies.sort((a, b) -> Float.compare(a.distance, b.distance));
            int visibleCount = Math.min(this.nearestHiddenAlliesCount.getValue().intValue(), allies.size());
            for (int i = 0; i < visibleCount; i++) visibleAllies.add(allies.get(i).normalizedName);
        } else if (this.semiAutoMode.isSelected()) {
            int requestedCount = Math.max(1, this.desiredSemiAutoCount);
            int visibleCount = Math.min(requestedCount, allies.size());
            if (!this.semiAutoSnapshotReady || requestedCount != this.semiAutoSnapshotCount) {
                this.semiAutoVisibleAllies.clear();
                for (AllyInfo ally : allies) {
                    if (this.semiAutoVisibleAllies.size() >= visibleCount) break;
                    this.semiAutoVisibleAllies.add(ally.normalizedName);
                }
                this.semiAutoSnapshotCount = requestedCount;
                this.semiAutoSnapshotReady = true;
            }
            visibleAllies.addAll(this.semiAutoVisibleAllies);
        } else {
            // Manual selections are the exact allies that should disappear.
        }

        this.hiddenAllies.clear();
        this.boxAllies.clear();
        allies.sort((a, b) -> Float.compare(a.distance, b.distance));
        for (AllyInfo ally : allies) this.boxAllies.add(ally.player);
        for (AllyInfo ally : allies) {
            if (this.manualMode.isSelected() && showAlliesEnabled) {
                if (this.manualHiddenAllies.isSelected(ally.normalizedName)) {
                    this.hiddenAllies.add(ally.normalizedName);
                }
            } else if (!visibleAllies.contains(ally.normalizedName)) {
                this.hiddenAllies.add(ally.normalizedName);
            }
        }
    }

    private void clearState() {
        this.hiddenAllies.clear();
        this.resetSemiAutoSnapshot();
        this.manualHiddenAllies.updateCandidates(java.util.Collections.<String>emptyList());
        this.boxAllies.clear();
    }

    private void resetSemiAutoSnapshot() {
        this.semiAutoVisibleAllies.clear();
        this.semiAutoSnapshotReady = false;
        this.semiAutoSnapshotCount = -1;
    }

    private Collection<String> collectOnlineClanMembers(EntityLivingBase localPlayer) {
        String localIdentity = getClanIdentity(localPlayer);
        if (localIdentity.isEmpty()) return java.util.Collections.emptyList();
        return collectTabClanMembers(localIdentity, localPlayer.getName());
    }

    /**
     * Returns every matching member from Minecraft's Tab list.  Unlike the
     * world entity list this includes players outside the render distance, so
     * both Piercing Manual and .friends addall can operate on the whole clan.
     */
    public static Collection<String> collectTabClanMembers(String clanIdentity,
                                                            String excludedPlayerName) {
        ArrayList<String> members = new ArrayList<String>();
        if (clanIdentity == null || clanIdentity.isEmpty() || Minecraft.N() == null
                || Minecraft.N().isNull()
                || Minecraft.N().getPlayerInfoMap() == null) return members;
        String excluded = normalizeNameStatic(excludedPlayerName);
        for (Object handle : Minecraft.N().getPlayerInfoMap()) {
            if (handle == null) continue;
            try {
                PlayerInfo info = new PlayerInfo(handle);
                GameProfile profile = info.v();
                if (profile == null || profile.isNull()) continue;
                String name = profile.getName();
                if (name == null || normalizeNameStatic(name).equals(excluded)) continue;
                if (clanIdentity.equals(playerInfoClanIdentity(info, name))) {
                    members.add(name);
                }
            } catch (Throwable ignored) {
                // Tab entries are replaced asynchronously by the game.
            }
        }
        return members;
    }

    private static String playerInfoClanIdentity(PlayerInfo info, String playerName) {
        try {
            if (info.R() != null && info.R().isNotNull()) {
                String formatted = info.R().getFormattedText();
                // Servers use both "Nick [TAG]" and "[TAG] Nick".  The old
                // trailing-only matcher saw only loaded entity teammates when
                // the Tab list used the prefix form.
                String tag = normalizeTagStatic(extractAnyBracketedClanTag(formatted));
                if (!tag.isEmpty()) return "tag:" + tag;
                String adjacent = normalizeTagStatic(
                        extractAdjacentDisplayClanTag(formatted, playerName));
                if (!adjacent.isEmpty()) return "tag:" + adjacent;
            }
        } catch (Throwable ignored) { }
        try {
            ScorePlayerTeam team = info.X();
            if (team != null && team.isNotNull()) {
                String suffix = normalizeTagStatic(team.getSuffix());
                if (suffix.length() >= 2) return "tag:" + suffix;
                String prefix = normalizeTagStatic(team.getPrefix());
                if (prefix.length() >= 2) return "tag:" + prefix;
                String registered = normalizeTagStatic(team.getRegisteredName());
                if (!registered.isEmpty()) return "team:" + registered;
            }
        } catch (Throwable ignored) { }
        return "";
    }

    private static String extractAnyBracketedClanTag(String formattedText) {
        if (formattedText == null || formattedText.isEmpty()) return "";
        java.util.regex.Matcher matcher = BRACKETED_CLAN_TAG.matcher(
                stripColorStatic(formattedText));
        return matcher.find() ? matcher.group(1) : "";
    }

    private void syncHiddenAlliesSliderBounds(int allyCount) {
        int maximum = Math.max(1, allyCount);
        this.desiredNearestCount = rememberDesiredCount(this.nearestHiddenAlliesCount, this.desiredNearestCount);
        this.desiredSemiAutoCount = rememberDesiredCount(this.semiAutoHiddenAlliesCount, this.desiredSemiAutoCount);
        applySliderBound(this.nearestHiddenAlliesCount, maximum, this.desiredNearestCount);
        applySliderBound(this.semiAutoHiddenAlliesCount, maximum, this.desiredSemiAutoCount);
    }

    /**
     * A value sitting below the current cap is a deliberate pick, so it becomes the new intent.
     * A value pinned at the cap may just be the clamp from a previous shrink, so the larger
     * remembered pick wins and is restored once more allies show up again.
     */
    private static int rememberDesiredCount(NumberValue slider, int previousDesired) {
        int current = slider.getValue().intValue();
        if ((double) current < slider.getMaximum()) {
            return current;
        }
        return Math.max(previousDesired, current);
    }

    private static void applySliderBound(NumberValue slider, int maximum, int desired) {
        slider.setMaximum(maximum);
        int restored = Math.min(Math.max(1, desired), maximum);
        if (slider.getValue().intValue() != restored) {
            slider.setValue(Double.valueOf(restored));
        }
    }

    private void publishHiddenBoundingBoxes() {
        if (this.isRenderOnly()) {
            EntityBoundingBoxFilterHook.clearHideClans();
            return;
        }
        ArrayList<Object> hiddenHandles = new ArrayList<Object>();
        for (EntityLivingBase player : this.boxAllies) {
            try {
                if (!player.isNull() && this.hiddenAllies.contains(normalizeName(player.getName()))
                        && !isProtectedByTargetSafeHits(player)) {
                    hiddenHandles.add(player.getObject());
                }
            } catch (Throwable ignored) {}
        }
        EntityBoundingBoxFilterHook.updateHideClans(hiddenHandles);
    }

    private boolean isRenderOnly() {
        return this.renderOnlyMode.isSelected();
    }

    private void restoreAll() {
        EntityBoundingBoxFilterHook.clearHideClans();
        this.hiddenAllies.clear();
        this.boxAllies.clear();
        this.refreshTicks = 0;
        this.semiAutoVisibleAllies.clear();
        this.semiAutoSnapshotReady = false;
        this.semiAutoSnapshotCount = -1;
        this.manualHiddenAllies.updateCandidates(java.util.Collections.<String>emptyList());
    }

    private static class AllyInfo {
        final EntityLivingBase player;
        final String displayName;
        final String normalizedName;
        final float distance;
        AllyInfo(EntityLivingBase player, String normalizedName, float distance) {
            this.player = player;
            this.displayName = player.getName();
            this.normalizedName = normalizedName;
            this.distance = distance;
        }
    }
}

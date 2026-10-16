package vegas.sony.manager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import vegas.sony.SonyVegas;
import vegas.sony.config.Profile;
import vegas.sony.event.EventHandler;
import vegas.sony.event.EventListener;
import vegas.sony.event.impl.EventModStateChange;
import vegas.sony.module.Category;
import vegas.sony.module.blatant.Backtrack;
import vegas.sony.module.blatant.DiscardedBlink;
import vegas.sony.module.blatant.KeepSprint;
import vegas.sony.module.blatant.NoJumpDelay;
import vegas.sony.module.blatant.NoSlowdown;
import vegas.sony.module.combat.AimAssist;
import vegas.sony.module.combat.AutoGapple;
import vegas.sony.module.combat.Basquete;
import vegas.sony.module.combat.Criticals;
import vegas.sony.module.combat.HitFlick;
import vegas.sony.module.combat.HitSelect;
import vegas.sony.module.combat.LeftClicker;
import vegas.sony.module.combat.LagRange;
import vegas.sony.module.combat.NoHitDelay;
import vegas.sony.module.combat.SilentAura;
import vegas.sony.module.combat.TimerRange;
import vegas.sony.module.combat.SprintReset;
import vegas.sony.module.combat.SuperKnockback;
import vegas.sony.module.combat.silentaura.SilentAuraClicker;
import vegas.sony.module.combat.velocity.VelocityPacketMode;
import vegas.sony.module.utility.inventory.InventoryActionModule;
import vegas.sony.module.none.ClientSettings;
import vegas.sony.module.render.Animations;
import vegas.sony.module.render.ArrayListModule;
import vegas.sony.module.render.ClickGui;
import vegas.sony.module.render.CapeManager;
import vegas.sony.module.render.Rotate;
import vegas.sony.module.render.hud.HudModule;
import vegas.sony.module.world.FakeLag;
import vegas.sony.module.MinecraftVersionConstraint;
import vegas.sony.module.Mod;
import vegas.sony.module.misc.Panic;
import vegas.sony.module.SubModule;
import vegas.sony.notification.NotificationType;
import vegas.sony.notification.ReusableTextNotification;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.unmap.ModeSelection;
import vegas.sony.value.ModeValue;
import vegas.sony.value.SubModuleValue;
import vegas.sony.value.Value;
import vegas.sony.wrapper.impl.ForgeVersion;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;

public class ModManager
implements EventListener {
    private static GuiComponent[] legacyCategoryComponents;
    private Map<Class<? extends Mod>, Mod> allModulesByType;
    private ArrayList<Mod> activeModuleList;
    private HashSet<Mod> enabledModules;
    private ReusableTextNotification profileSwitchNotification;
    private Map<Class<? extends Mod>, Mod> activeModulesByType;
    private boolean suppressStateNotifications;

    public Collection<Mod> getTopLevelModules() {
        ArrayList<Mod> topLevelModules = new ArrayList<Mod>();
        for (Mod mod : this.getAllModules()) {
            if (mod instanceof SubModule) continue;
            topLevelModules.add(mod);
        }
        return topLevelModules;
    }

    public void init() {
        GuiComponent[] legacyComponentsSnapshot = Category.getLegacyComponents();
        this.registerModules(Stream.of(new ClientSettings()));
        SilentAura silentAura = new SilentAura();
        this.registerModules(Stream.of(silentAura, new SilentAuraClicker(silentAura), new HitFlick(), new AimAssist(), new LeftClicker(), new HitSelect(), new LagRange(), new TimerRange(), new Criticals()));
        this.registerModules(Stream.of(new VelocityPacketMode(), new SprintReset(),
                new SuperKnockback(), new DiscardedBlink(), new Basquete()));
        this.registerModules(Stream.of(new NoJumpDelay(), new NoHitDelay(), new AutoGapple()));
        this.registerModules(Stream.of(new KeepSprint(), new NoSlowdown()));
        this.registerModules(Stream.of(new Panic()));
        this.registerModules(Stream.of(new ClickGui(), new Animations(), new Rotate(),
                new ArrayListModule()));
        ModRegistrationBuilder.create().setModule(new CapeManager())
                .addVersionConstraint(ForgeVersion.MC_1_16_5_ACTUAL.H()).registerWith(this);
        this.registerModules(Stream.of(new vegas.sony.module.gladiador.HideClans(),
                new vegas.sony.module.gladiador.Target()));
        this.registerModules(Stream.of(
                new vegas.sony.module.rise.RiseWTap(),
                new vegas.sony.module.rise.RiseVelocity(),
                new vegas.sony.module.rise.RiseInvMove(),
                new vegas.sony.module.rise.RiseSnapTap()));
        this.registerModules(Stream.of(new Backtrack()), ModManager::addBacktrackVersionConstraints);
        this.registerModules(Stream.of(new FakeLag()), ModManager::addMinecraft1710Constraint);
        if (legacyComponentsSnapshot == null) {
            GuiComponent.setLegacyComponentState(new GuiComponent[2]);
        }
    }

    public boolean getState(Class<? extends Mod> clazz) {
        for (Mod mod : this.collectMods()) {
            if (!mod.getClass().equals(clazz)) continue;
            return mod.isEnabled();
        }
        return false;
    }

    /**
     * Enables HUD elements that should always be present when the client starts.
     * This runs after the active profile state is applied so an older profile
     * without ArrayList enabled cannot hide the default HUD on injection.
     */
    public void enableDefaultHudModules() {
        boolean previousSuppression = this.suppressStateNotifications;
        this.suppressStateNotifications = true;
        try {
            ArrayListModule arrayList = this.getMod(ArrayListModule.class);
            if (arrayList != null && !arrayList.isEnabled()) {
                arrayList.setEnabled(true, true);
            }
            CapeManager cape = this.getMod(CapeManager.class);
            if (cape != null && !cape.isEnabled()) {
                cape.setEnabled(true, true);
            }
        } finally {
            this.suppressStateNotifications = previousSuppression;
        }
    }

    private static void addMinecraft1206Constraint(ModRegistrationBuilder modRegistrationBuilder) {
        modRegistrationBuilder.addVersionConstraint(ForgeVersion.MC_1_20_6.b());
    }

    public JsonObject getEnabledNonHudModuleStates() {
        JsonObject enabledStates = new JsonObject();
        for (Mod mod : this.getAllModules()) {
            if (mod instanceof HudModule || !mod.isEnabled()) continue;
            enabledStates.addProperty(mod.getName(), Boolean.valueOf(mod.isEnabled()));
        }
        return enabledStates;
    }

    public void applyProfileModuleStates(Profile profile) {
        this.suppressStateNotifications = true;
        JsonObject enabledStates = profile.getEnabledModuleStates();
        int enabledCount = 0;
        for (Mod mod : this.collectMods()) {
            if (mod instanceof HudModule || mod.getCategory().equals(Category.NONE)) continue;
            try {
                JsonElement savedState = findModuleState(enabledStates, mod);
                if (savedState != null) {
                    if (!mod.isVisible()) continue;
                    try {
                        if (mod.isEnabled()) continue;
                        mod.setEnabled(savedState.getAsBoolean());
                        ++enabledCount;
                    }
                    catch (Exception exception) {
                        SonyVegas.logThrowable(exception);
                    }
                    continue;
                }
                if (mod instanceof ClientSettings || mod.getGuiColor() == 0 || !mod.isEnabled()) continue;
                mod.toggle();
            }
            catch (Exception exception) {
                SonyVegas.logThrowable(exception);
            }
        }
        this.suppressStateNotifications = false;
        if (SonyVegas.INSTANCE.getPublicProfileSettings().profileSwitchNotifications.getEffectiveValue().booleanValue()) {
            this.profileSwitchNotification.withTitle("Profile swap to " + vegas.sony.config.ClientSettings.FORMAT_CODE + "6" + profile.getName())
                    .withMessage(enabledCount + " modules enabled").reset();
            SonyVegas.INSTANCE.getNotificationManager().show(this.profileSwitchNotification);
        }
    }

    private static void addModernMinecraftConstraint(ModRegistrationBuilder modRegistrationBuilder) {
        modRegistrationBuilder.addVersionConstraint(ForgeVersion.MC_1_21_4.b());
    }

    public void initializeAllModules() {
        for (Mod mod : this.allModulesByType.values()) {
            mod.I();
        }
    }

    private void registerModules(Stream<Mod> modules, Consumer<ModRegistrationBuilder<?>> customizer) {
        modules.forEach(module -> this.registerModuleWithCustomizer(customizer, module));
    }

    public HashSet<Mod> getMods() {
        return this.enabledModules;
    }

    public static void setLegacyCategoryComponents(GuiComponent[] components) {
        legacyCategoryComponents = components;
    }

    private void registerModules(Stream<Mod> modules) {
        this.registerModules(modules, ModManager::noAdditionalConstraints);
    }

    void registerModule(Mod module, List<List<MinecraftVersionConstraint>> constraintGroups, boolean enableImmediately) {
        this.allModulesByType.put(module.getClass(), module);
        if (!constraintGroups.isEmpty()) {
            boolean supported = false;
            for (List<MinecraftVersionConstraint> constraints : constraintGroups) {
                List<MinecraftVersionConstraint> activeConstraints = MinecraftVersionConstraint.o(constraints);
                if (!activeConstraints.isEmpty()) continue;
                supported = true;
            }
            if (!supported) {
                return;
            }
        }
        this.activeModulesByType.put(module.getClass(), module);
        this.activeModuleList.add(module);
        for (Value<?, ?> value : module.getValues()) {
            if (!(value instanceof ModeValue)) continue;
            ModeValue modeValue = (ModeValue)value;
            for (ModeSelection modeSelection : modeValue.getModes()) {
                SubModuleValue subModuleValue;
                if (!(modeSelection instanceof SubModuleValue) || !((SubModule)(subModuleValue = (SubModuleValue)modeSelection).getInstance()).isSubModuleEnabled()) continue;
                this.registerModule((Mod)subModuleValue.getInstance(), constraintGroups, false);
                module.registerSubModule(new SubModule[]{subModuleValue.getInstance()});
            }
        }
        if (enableImmediately) {
            module.setEnabled(true);
        }
    }

    private void registerModuleWithCustomizer(Consumer consumer, Mod module) {
        ModRegistrationBuilder<Mod> builder = ModRegistrationBuilder.create().setModule(module);
        consumer.accept(builder);
        builder.registerWith(this);
    }

    public void finishModuleInitialization() {
        for (Mod mod : this.activeModulesByType.values()) {
            mod.onFinishModuleInitialization();
        }
    }

    public String buildTranslationTemplate() {
        StringBuilder translations = new StringBuilder();
        for (Mod mod : this.collectMods()) {
            String moduleName = mod.getName();
            String moduleKey = moduleName.replace(" ", "_").toLowerCase();
            translations.append(moduleKey + "=" + moduleName);
            translations.append("\n");
            if (mod.getToolTip() != null && !mod.getToolTip().equals("")) {
                translations.append(moduleKey + ".tooltip=" + mod.getToolTip().replace("\n", " "));
                translations.append("\n");
            }
            for (Value<?, ?> value : mod.getValues()) {
                String valueName = value.getName();
                String valueKey = moduleKey + "." + value.getName().replace(" ", "_").toLowerCase();
                translations.append(valueKey + "=" + valueName);
                translations.append("\n");
                if (value.getDescription() == null || value.getDescription().isEmpty()) continue;
                String description = value.getDescription().replace("\n", " ");
                String tooltipKey = valueKey + ".tooltip";
                translations.append(tooltipKey + "=" + description);
                translations.append("\n");
            }
        }
        return translations.toString();
    }

    private static Exception propagateException(Exception exception) {
        return exception;
    }

    @EventHandler
    public void onModuleStateChanged(EventModStateChange eventModStateChange) {
        Mod mod = eventModStateChange.getModule();
        if (mod.isEnabled()) {
            this.enabledModules.add(mod);
        } else {
            this.enabledModules.remove(mod);
        }
        for (Mod mod2 : this.getMods()) {
            mod2.U(mod);
        }
        if (ClientSettings.INSTANCE.isInputEnabled() && mod.q$src$Z$12h8h4c()
                && SonyVegas.INSTANCE.getPublicProfileSettings().toggleAlerts.getEffectiveValue().booleanValue()
                && !this.suppressStateNotifications && !mod.isBindStateChange()) {
            mod.showToggleNotification();
        }
    }

    @EventHandler
    public void onModuleEnabled(EventModStateChange eventModStateChange) {
        Mod mod = eventModStateChange.getModule();
        if (eventModStateChange.isEnabled()) {
            mod.j();
        }
    }

    private static void addMinecraft1214Constraint(ModRegistrationBuilder modRegistrationBuilder) {
        modRegistrationBuilder.addVersionConstraint(ForgeVersion.MC_1_21_4.n());
    }

    static {
        ModManager.setLegacyCategoryComponents(new GuiComponent[4]);
    }

    public int countEnabledModules(Category category) {
        int count = 0;
        for (Mod mod : this.activeModulesByType.values()) {
            if (mod.getCategory() != category || !mod.isEnabled()) continue;
            ++count;
        }
        return count;
    }

    public JsonArray toJson(boolean includeDefaults) {
        JsonArray serializedModules = new JsonArray();
        for (Mod mod : this.getTopLevelModules()) {
            JsonObject serializedModule = mod.toJson(includeDefaults);
            if (serializedModule == null) continue;
            serializedModules.add((JsonElement)serializedModule);
        }
        return serializedModules;
    }

    public static GuiComponent[] getLegacyCategoryComponents() {
        return legacyCategoryComponents;
    }

    private static void addBacktrackVersionConstraints(ModRegistrationBuilder modRegistrationBuilder) {
        modRegistrationBuilder.addVersionConstraint(ForgeVersion.MC_1_8_9.H()).addVersionConstraint(ForgeVersion.MC_1_21_4.n());
    }

    private static void addMinecraft189Constraint(ModRegistrationBuilder modRegistrationBuilder) {
        modRegistrationBuilder.addVersionConstraint(ForgeVersion.MC_1_8_9.H());
    }

    public <T extends Mod> T getMod(Class<T> clazz) {
        return (T)((Mod)this.activeModulesByType.get(clazz));
    }

    public boolean isOtherInventoryActionActive(Class<? extends InventoryActionModule> moduleType) {
        for (Mod mod : this.activeModulesByType.values()) {
            if (mod.getClass() == moduleType || !(mod instanceof InventoryActionModule)) continue;
            InventoryActionModule inventoryActionModule = (InventoryActionModule)((Object)mod);
            if (!mod.isEnabled() || !inventoryActionModule.isPerformingInventoryAction()) continue;
            return true;
        }
        return false;
    }

    public void disableNonHudModules() {
        this.suppressStateNotifications = true;
        for (Mod mod : this.collectMods()) {
            if (mod.getCategory() == Category.NONE || !mod.isEnabled() || mod instanceof HudModule) continue;
            mod.setEnabled(false);
        }
        this.suppressStateNotifications = false;
    }

    /** Normal module lifecycle only; includes HUD, hidden modules and submodules. */
    public int disableAllModules() {
        boolean previousSuppression = this.suppressStateNotifications;
        this.suppressStateNotifications = true;
        List<Mod> snapshot = new ArrayList<Mod>(this.collectMods());
        try {
            for (Mod mod : snapshot) {
                if (!mod.isEnabled()) continue;
                try {
                    mod.setEnabled(false, true);
                } catch (Throwable error) {
                    SonyVegas.logThrowable(error);
                }
            }
        } finally {
            this.suppressStateNotifications = previousSuppression;
        }
        int remaining = 0;
        for (Mod mod : snapshot) if (mod.isEnabled()) ++remaining;
        return remaining;
    }

    /** Runs a temporary multi-module transition without flooding the HUD. */
    public void runWithStateNotificationsSuppressed(Runnable transition) {
        boolean previousSuppression = this.suppressStateNotifications;
        this.suppressStateNotifications = true;
        try {
            transition.run();
        } finally {
            this.suppressStateNotifications = previousSuppression;
        }
    }

    public Mod getMod(String name) {
        Mod internalMatch = null;
        for (Map.Entry<Class<? extends Mod>, Mod> entry : this.activeModulesByType.entrySet()) {
            if (!((Mod)entry.getValue()).matchesName(name)) continue;
            // Internal helpers can share a display name with a public module.
            // Configs and binds must resolve the public module first.
            if (entry.getValue().getCategory() == Category.NONE) {
                if (internalMatch == null) internalMatch = entry.getValue();
                continue;
            }
            return (Mod)entry.getValue();
        }
        return internalMatch;
    }

    private static void addMinecraft1710Constraint(ModRegistrationBuilder modRegistrationBuilder) {
        modRegistrationBuilder.addVersionConstraint(ForgeVersion.MC_1_7_10.N());
    }

    public List<Mod> getProfileModules(JsonObject enabledModuleStates) {
        ArrayList<Mod> modules = new ArrayList<Mod>();
        for (Mod mod : this.collectMods()) {
            if (findModuleState(enabledModuleStates, mod) == null || !mod.isVisible() || mod.getCategory() == Category.NONE) continue;
            modules.add(mod);
        }
        return modules;
    }

    public Collection<Mod> getAllModules() {
        return this.allModulesByType.values();
    }

    private static void addMinecraft1165Constraint(ModRegistrationBuilder modRegistrationBuilder) {
        modRegistrationBuilder.addVersionConstraint(ForgeVersion.MC_1_16_5.b());
    }

    private static void noAdditionalConstraints(ModRegistrationBuilder modRegistrationBuilder) {
    }

    public Collection<Mod> collectMods() {
        return this.activeModulesByType.values();
    }

    public void loadJson(JsonArray serializedModules) {
        for (int index = 0; index < serializedModules.size(); ++index) {
            JsonObject serializedModule;
            JsonElement element = serializedModules.get(index);
            if (!element.isJsonObject() || element.isJsonNull() || (serializedModule = element.getAsJsonObject()).get("name") == null || serializedModule.get("name").isJsonNull()) continue;
            String moduleName = serializedModule.get("name").getAsString();
            for (Mod mod : this.getTopLevelModules()) {
                try {
                    if (!mod.matchesName(moduleName)) continue;
                    mod.loadJson(serializedModule);
                }
                catch (Exception exception) {
                    SonyVegas.debugLog(mod.getName());
                    SonyVegas.logThrowable(exception);
                }
            }
        }
    }

    private static JsonElement findModuleState(JsonObject states, Mod module) {
        JsonElement direct = states.get(module.getName());
        if (direct != null) {
            return direct;
        }
        for (Map.Entry<String, JsonElement> entry : states.entrySet()) {
            if (module.matchesName(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    public void disableHiddenModules() {
        int disabledCount = 0;
        for (Mod mod : this.collectMods()) {
            if (mod.isVisible() || mod instanceof ClientSettings || mod.getGuiColor() == 0 || !mod.isEnabled()) continue;
            ++disabledCount;
            mod.toggle();
        }
        if (disabledCount > 0) {
            SonyVegas.INSTANCE.getNotificationManager().show("Hidden Disabled", disabledCount + " module(s) have been disabled!", NotificationType.WARNING, 2500L);
        }
    }

    public ModManager() {
        GuiComponent[] legacyComponentsSnapshot = Category.getLegacyComponents();
        Object[] discardedSlotStorage = new Object[877];
        Array.newInstance(Long.TYPE, 837);
        Array.newInstance(Byte.TYPE, 904);
        Array.newInstance(Float.TYPE, 627);
        Array.newInstance(Short.TYPE, 845);
        Array.newInstance(Object.class, 823);
        Array.newInstance(Character.TYPE, 598);
        Array.newInstance(Double.TYPE, 654);
        GuiComponent[] preservedLegacyComponents = legacyComponentsSnapshot;
        Array.newInstance(Integer.TYPE, 556);
        Array.newInstance(Boolean.TYPE, 506);
        this.allModulesByType = new LinkedHashMap();
        this.activeModulesByType = new LinkedHashMap();
        this.enabledModules = new HashSet();
        this.activeModuleList = new ArrayList();
        this.profileSwitchNotification = new ReusableTextNotification(NotificationType.INFO, "", "", 2000L);
        if (GuiComponent.getLegacyComponentState() == null) {
            Category.setLegacyComponents(new GuiComponent[1]);
        }
    }

    public ArrayList<Mod> getActiveModuleList() {
        return this.activeModuleList;
    }

    private static void addBedPlatesVersionConstraints(ModRegistrationBuilder modRegistrationBuilder) {
        modRegistrationBuilder.addVersionConstraints(ForgeVersion.MC_1_7_10.N(), ForgeVersion.MC_1_20_6.b());
    }
}

package vegas.sony.module;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import vegas.sony.SonyVegas;
import vegas.sony.config.ConfigJsonUtils;
import vegas.sony.event.EventBus;
import vegas.sony.event.EventListener;
import vegas.sony.event.IEvent;
import vegas.sony.event.impl.EventKeyPress;
import vegas.sony.event.impl.EventModStateChange;
import vegas.sony.module.none.ClientSettings;
import vegas.sony.input.BindActivationMode;
import vegas.sony.input.InputEventDispatcher;
import vegas.sony.notification.NotificationType;
import vegas.sony.ui.click.frame.impl.main.ClickGuiModuleCardRenderState;
import vegas.sony.unmap.Bendable;
import vegas.sony.unmap.INamed;
import vegas.sony.unmap.ModBendable;
import vegas.sony.unmap.PropertyContainer;
import vegas.sony.utils.EncString;
import vegas.sony.utils.Str;
import vegas.sony.value.ModuleValueDisplayFormatter;
import vegas.sony.value.BooleanValue;
import vegas.sony.value.ModeValue;
import vegas.sony.value.SubModuleValue;
import vegas.sony.value.Value;
import vegas.sony.value.ValueDisplayDescriptor;
import vegas.sony.wrapper.impl.Minecraft;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import org.jetbrains.annotations.Nullable;

public class Mod
extends PropertyContainer
implements INamed,
EventListener {
    private boolean enabled;
    private static EncString staticName;
    private final List<ValueDisplayDescriptor> valueDisplayDescriptors;
    private DelayedModuleToggleTask moduleRunnable;
    private final Bendable bind;
    private final BooleanValue hideFromArrayList;
    protected Category category;
    private final List<Value<?, ?>> values;
    private boolean requiresBind;
    private boolean favorited;
    private final EncString name;
    private final int defaultKeybind;
    private final List<SubModule> subModules;
    private ModuleDisplayScope displayScope;
    private final int guiColor;
    private boolean developmentWarningShown;
    private boolean bindStateChange;
    private boolean defaultVisible = true;
    private boolean visible = true;
    private final List<Value<?, ?>> allValues = new ArrayList();
    private EncString tooltip;

    public ModuleDisplayInfo getModuleDisplayInfo() {
        return null;
    }

    public String getSimpleSuffix() {
        return "";
    }

    public void registerSubModule(SubModule ... subModuleArray) {
        this.getSubModules().addAll(Arrays.asList(subModuleArray));
    }

    public ModuleDisplayScope getModuleDisplayScope() {
        return this.displayScope;
    }

    public void onEnable() {
    }

    public SubModule getSubModule(String string) {
        for (SubModule subModule : this.getSubModules()) {
            if (!subModule.matchesName(string)) continue;
            return subModule;
        }
        return null;
    }

    public void setVisibility(boolean bl) {
        this.visible = bl;
    }

    public boolean Q() {
        return this.getProperty(PropertyContainer.LEGACY_FLAG);
    }

    public void i(ValueDisplayDescriptor ... valueDisplayDescriptorArray) {
        this.valueDisplayDescriptors.clear();
        this.valueDisplayDescriptors.addAll(Arrays.asList(valueDisplayDescriptorArray));
    }

    public String getDetailedSuffix() {
        return "";
    }

    public void loadJson(JsonObject jsonObject) {
        String string;
        if (this.bind.usesOwnKeybindStorage()) {
            this.bind.getBoundInputs().clear();
        }
        if ((string = ConfigJsonUtils.getString(jsonObject, "name")) != null && this.matchesName(string)) {
            Object object;
            JsonArray jsonArray;
            if (this.bind.usesOwnKeybindStorage()) {
                jsonArray = ConfigJsonUtils.getJsonArray(jsonObject, "keybinds_2");
                if (jsonArray != null) {
                    try {
                        this.bind.loadBoundInputs(jsonArray, false);
                    }
                    catch (Exception exception) {}
                } else {
                    jsonArray = ConfigJsonUtils.getJsonArray(jsonObject, "keybinds");
                    if (jsonArray != null) {
                        try {
                            this.bind.loadBoundInputs(jsonArray, true);
                        }
                        catch (Exception exception) {}
                    } else {
                        this.bind.getBoundInputs().clear();
                        if (this.defaultKeybind != 0) {
                            this.bind.getBoundInputs().add(this.defaultKeybind);
                        }
                    }
                }
                if ((object = ConfigJsonUtils.getString(jsonObject, "bind_mode")) != null && this.bind.supportsActivationMode()) {
                    try {
                        this.bind.setActivationMode(BindActivationMode.valueOf((String)object));
                    }
                    catch (IllegalArgumentException illegalArgumentException) {
                        this.bind.setActivationMode(BindActivationMode.TOGGLE);
                    }
                } else {
                    this.bind.setActivationMode(BindActivationMode.TOGGLE);
                }
            }
            if ((jsonArray = jsonObject.getAsJsonArray("values")) != null) {
                java.util.Iterator<JsonElement> valueIterator = jsonArray.iterator();
                while (valueIterator.hasNext()) {
                    JsonElement jsonElement = valueIterator.next();
                    JsonObject jsonObject2 = jsonElement.getAsJsonObject();
                    for (Value<?, ?> value : this.allValues) {
                        if (!value.matchesJsonId(jsonObject2)) continue;
                        value.loadJson(jsonObject2);
                    }
                }
            }
            if ((object = ConfigJsonUtils.getBoolean(jsonObject, "visible")) != null) {
                this.visible = (Boolean)object;
            }
        }
    }

    public Category getCategory() {
        return this.category;
    }

    public void setDefaultVisibility(boolean bl) {
        this.defaultVisible = bl;
        this.visible = bl;
    }

    public void showToggleNotification() {
        SonyVegas.INSTANCE.getNotificationManager()
                .showModuleToggle(this.getName(), this.isEnabled());
    }

    @Nullable
    public JsonObject toJson(boolean bl) {
        JsonArray jsonArray;
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("name", this.getName());
        if (this.bind.usesOwnKeybindStorage()) {
            jsonArray = this.bind.serializeBoundInputs();
            if (jsonArray.size() != (this.defaultKeybind == 0 ? 0 : 1) || jsonArray.size() == 1 && jsonArray.get(0).getAsInt() != this.defaultKeybind) {
                jsonObject.add("keybinds_2", (JsonElement)jsonArray);
            }
            if (this.bind.supportsActivationMode() && this.bind.getActivationMode() != BindActivationMode.TOGGLE) {
                jsonObject.addProperty("bind_mode", this.bind.getActivationMode().name());
            }
        }
        jsonArray = new JsonArray();
        for (Value<?, ?> value : this.allValues) {
            JsonObject jsonObject2;
            if (!value.isSerializable() || value.isDefault() || (jsonObject2 = value.toJson(bl)).entrySet().size() <= 1) continue;
            jsonArray.add((JsonElement)jsonObject2);
        }
        if (jsonArray.size() != 0) {
            jsonObject.add("values", (JsonElement)jsonArray);
        }
        if (this.visible != this.defaultVisible) {
            jsonObject.addProperty("visible", Boolean.valueOf(this.visible));
        }
        if (jsonObject.entrySet().size() == 1) {
            return null;
        }
        return jsonObject;
    }

    public void setSuffix(String string) {
        this.tooltip = Str.enc(string);
    }

    public void syncSubModuleStates(boolean enabled, boolean bypassVisibilityCheck) {
        for (SubModule subModule : this.getSubModules()) {
            if (subModule.isSelectedSubModule()) {
                if (subModule.isEnabled() == enabled) continue;
                subModule.setEnabled(enabled, bypassVisibilityCheck);
                continue;
            }
            if (!subModule.isParentEnabled()) continue;
            subModule.setEnabled(false, bypassVisibilityCheck);
        }
    }

    public String getSuffixForMode(int mode) {
        String string = "";
        if (mode == 0) {
            string = this.getSimpleSuffix();
        }
        if (mode == 1 && ((string = this.getDetailedSuffix()) == null || string.isEmpty())) {
            string = this.getSimpleSuffix();
        }
        return string;
    }

    public Mod(String string, int n) {
        this(string, n, 0, Category.NONE, null);
    }

    public void setFavorite(boolean bl) {
        this.favorited = bl;
    }

    public void v(long l, boolean bl) {
        if (this.moduleRunnable != null) {
            this.moduleRunnable.setRunning(false);
        }
        this.moduleRunnable = new DelayedModuleToggleTask(this, l, bl);
        new Thread(this.moduleRunnable).start();
    }

    public boolean L() {
        return this.getProperty(PropertyContainer.NEW_BADGE);
    }

    public void toggle() {
        if (this.isRequiresBind() && this.bind.usesOwnKeybindStorage() && this.bind.getBoundInputs().isEmpty()) {
            return;
        }
        this.setEnabled(!this.enabled);
    }

    public void onBindActivated() {
        if (this.isRequiresBind() && this.bind.usesOwnKeybindStorage() && this.bind.getBoundInputs().isEmpty()) {
            return;
        }
        this.setEnabledFromBind(!this.enabled);
    }

    private boolean allowsModuleBind() {
        vegas.sony.module.none.ClientSettings settings = vegas.sony.module.none.ClientSettings.INSTANCE;
        return (settings != null && !settings.bindsOnlyInGame.getEffectiveValue())
                || InputEventDispatcher.getInstance().getFocusState().isFocused();
    }

    public void setEnabledFromBind(boolean enabled) {
        if (!this.allowsModuleBind()) return;
        boolean previousState = this.enabled;
        this.bindStateChange = true;
        try {
            this.setEnabled(enabled);
        } finally {
            this.bindStateChange = false;
        }
        if (previousState != this.enabled && this.category != Category.NONE
                && SonyVegas.INSTANCE.getNotificationManager() != null) {
            this.showToggleNotification();
        }
    }

    public boolean isBindStateChange() {
        return this.bindStateChange;
    }

    public void setEnabled(boolean enabled, boolean bypassVisibilityCheck) {
        boolean stateChanged = this.enabled != enabled;
        if (!bypassVisibilityCheck && !this.isVisible() && this.category != Category.NONE && enabled) {
            if (SonyVegas.INSTANCE.getNotificationManager() != null) {
                SonyVegas.INSTANCE.getNotificationManager().show("Hidden Module //$ui", "Attempted to toggle " + this.getName() + "!", NotificationType.WARNING, 2500L);
            }
            return;
        }
        if (enabled && this.isBlatantMod()) {
            SonyVegas.INSTANCE.getPrimaryMappingTaskSet().f();
        }
        if (!enabled) {
            this.onBeforeDisable();
        }
        this.enabled = enabled;
        if (this.enabled) {
            EventBus.getInstance().registerListener(this, this.w());
            new EventModStateChange(this, true).fire();
            this.onEnable();
        } else {
            new EventModStateChange(this, false).fire();
            this.onDisable();
            if (!this.enabled) {
                EventBus.getInstance().unregisterListener(this);
            }
        }
        this.syncSubModuleStates(enabled, bypassVisibilityCheck);
        if (stateChanged && this.category != Category.NONE) {
            SonyVegas.INSTANCE.saveAndStop();
        }
    }

    private static Exception passThrough(Exception exception) {
        return exception;
    }

    public void onFinishModuleInitialization() {
    }

    public String getToolTip() {
        return this.tooltip == null ? null : this.tooltip.get();
    }

    public void p(SubModuleValue subModuleValue, SubModuleValue subModuleValue2) {
        if (this instanceof SubModule) {
            return;
        }
        if (!this.enabled) {
            return;
        }
        Object t = subModuleValue.getInstance();
        Object t2 = subModuleValue2.getInstance();
        if (((SubModule)t).isParentEnabled()) {
            ((Mod)t).setEnabled(false);
        }
        if (!((SubModule)t2).isParentEnabled()) {
            ((Mod)t2).setEnabled(true);
        }
    }

    public boolean q$src$Z$12h8h4c() {
        return this.getCategory() != Category.NONE && !(this instanceof UtilityMod);
    }

    public void K(boolean bl) {
        if (bl) {
            SonyVegas.INSTANCE.getModuleProfileMetadataCodec().addModule(this);
        } else {
            SonyVegas.INSTANCE.getModuleProfileMetadataCodec().removeModule(this);
        }
    }

    public Mod(String string, int n, Category category) {
        this(string, 0, n, category, null);
    }

    public void u(EventKeyPress eventKeyPress) {
        if (eventKeyPress.getKey() <= 0) {
            return;
        }
        if (this.bind.getBoundInputs().isEmpty()) {
            return;
        }
        if (Minecraft.currentScreen().getObject() != null) {
            return;
        }
        if (!this.allowsModuleBind()) {
            // Keyboard input is captured globally. A bind must never toggle a
            // module in a different application or in a second Minecraft.
            return;
        }
        // A module bind must toggle the module without consuming the game key.
        // This keeps bindings such as A/W/S/D usable for normal movement.
        this.bind.handleInput(eventKeyPress.getKey(), eventKeyPress.isDown());
    }

    public void setEnabled(boolean bl) {
        this.setEnabled(bl, false);
    }

    public Mod(String string, int n, Category category, String string2) {
        this(string, 0, n, category, string2);
    }

    public void onBeforeDisable() {
    }

    @Override
    public String getName() {
        return this.name.get();
    }

    public boolean matchesName(String candidate) {
        return this.name.visibleEqualsIgnoreCase(candidate);
    }

    public boolean matchesSearch(String query, boolean exact) {
        return this.name.visibleMatchesSearch(query, exact);
    }

    public String getNamePrefixUppercase(int maxVisibleChars) {
        return this.name.visiblePrefixUppercase(maxVisibleChars);
    }

    public boolean isRequiresBind() {
        return this.requiresBind;
    }

    public boolean isVisible() {
        return this.visible;
    }

    /**
     * Keeps a module usable while removing only its HUD entry.  This is
     * deliberately separate from the legacy visibility flag, which also
     * prevents the module from being enabled from the menu.
     */
    public boolean isHiddenFromArrayList() {
        return this.hideFromArrayList != null
                && this.hideFromArrayList.getEffectiveValue();
    }

    public String toString() {
        return "Module{name='" + this.getName() + '\'' + ", defaultKeybind=" + this.defaultKeybind + ", guiColor=" + this.guiColor + ", values=" + this.values + ", subModules=" + this.subModules + ", guiCategory=" + this.category + ", enabled=" + this.enabled + ", requiresBind=" + this.requiresBind + ", tooltip='" + this.getToolTip() + '\'' + ", moduleRunnable=" + this.moduleRunnable + ", defaultVisible=" + this.defaultVisible + ", visible=" + this.visible + ", favorited=" + this.favorited + '}';
    }

    public List<SubModule> getSubModules() {
        return this.subModules;
    }

    public void U(Value<?, ?> value, MinecraftVersionConstraint ... minecraftVersionConstraintArray) {
        this.addModeFirst(this.allValues, value);
        List<MinecraftVersionConstraint> list = MinecraftVersionConstraint.L(minecraftVersionConstraintArray);
        if (!list.isEmpty()) {
            return;
        }
        this.addModeFirst(this.values, value);
    }

    /**
     * Mode selectors are the entry point for the rest of a module's settings.
     * Submodule values are registered while a ModeValue is being constructed,
     * so a normal append would place the selector after all of its children.
     * Keep every selector at the front while preserving the registration order
     * of the selectors and of the mode-specific values that follow them.
     */
    private void addModeFirst(List<Value<?, ?>> target, Value<?, ?> value) {
        if (!(value instanceof ModeValue)) {
            target.add(value);
            return;
        }
        int firstNonMode = 0;
        while (firstNonMode < target.size()
                && target.get(firstNonMode) instanceof ModeValue) {
            firstNonMode++;
        }
        target.add(firstNonMode, value);
    }

    protected Predicate<IEvent> w() {
        return this::matchesEnabled;
    }

    public boolean isDefaultVisible() {
        return this.defaultVisible;
    }

    public void onScheduledAction() {
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public static String getStaticName() {
        return staticName == null ? null : staticName.get();
    }

    public boolean k() {
        return this.enabled;
    }

    public void X(MinecraftVersionConstraint minecraftVersionConstraint, Value<?, ?> ... valueArray) {
        for (Value<?, ?> value : valueArray) {
            this.U(value, minecraftVersionConstraint);
        }
    }

    public void U(Mod mod) {
    }

    public String f() {
        if (!this.valueDisplayDescriptors.isEmpty()) {
            return ModuleValueDisplayFormatter.formatDescriptorSummary(this.valueDisplayDescriptors);
        }
        if (this.getValues().isEmpty()) {
            return "";
        }
        return ModuleValueDisplayFormatter.formatValueSummary(this.getValues());
    }

    public List<Value<?, ?>> getAllValues() {
        return this.allValues;
    }

    public void setModuleDisplayScope(ModuleDisplayScope moduleDisplayScope) {
        this.displayScope = moduleDisplayScope;
    }

    public Value getValue(String string) {
        for (Value<?, ?> value : this.getAllValues()) {
            if (!value.getId().equalsIgnoreCase(string)
                    && !Str.visibleEqualsIgnoreCase(value.getName(), string)) continue;
            return value;
        }
        return null;
    }

    public int getGuiColor() {
        return this.guiColor;
    }

    public void addValue(Value<?, ?> ... valueArray) {
        for (Value<?, ?> value : valueArray) {
            this.U(value, new MinecraftVersionConstraint[0]);
        }
    }

    public void j() {
        if (!this.developmentWarningShown && this.Q()) {
            this.developmentWarningShown = true;
            SonyVegas.INSTANCE.getNotificationManager().show("Module in development //$ui", this.getName() + " is in development\n\nUse with caution and report issues to support //$ui", NotificationType.WARNING, 10000L);
        }
    }

    static {
        Mod.A("r6YMSc");
    }

    public Bendable getBind() {
        return this.bind;
    }

    public void I() {
    }

    private boolean matchesEnabled(IEvent iEvent) {
        return this.isEnabled();
    }

    public boolean isFavorite() {
        return this.favorited;
    }

    public int getDefaultKeybind() {
        return this.defaultKeybind;
    }

    public List<ValueDisplayDescriptor> getValueDisplayDescriptors() {
        return this.valueDisplayDescriptors;
    }

    public boolean isBlatantMod() {
        return false;
    }

    public static void A(String string) {
        staticName = Str.enc(string);
    }

    public void onDisable() {
    }

    public List<Value<?, ?>> getValues() {
        return this.values;
    }

    public List<ClickGuiModuleCardRenderState> S() {
        if (!this.valueDisplayDescriptors.isEmpty()) {
            return ModuleValueDisplayFormatter.buildDescriptorRenderStates(this.valueDisplayDescriptors);
        }
        if (this.getValues().isEmpty()) {
            return Collections.emptyList();
        }
        return ModuleValueDisplayFormatter.buildValueRenderStates(this.getValues());
    }

    public Mod(String string) {
        this(string, 0, Category.NONE);
    }

    public Mod(String string, int n, int n2, Category category, String string2) {
        this.values = new ArrayList();
        this.subModules = new ArrayList<SubModule>();
        this.valueDisplayDescriptors = new ArrayList<ValueDisplayDescriptor>();
        this.displayScope = ModuleDisplayScope.ALL;
        this.name = Str.enc(string);
        this.defaultKeybind = n;
        this.guiColor = n2;
        this.category = category;
        this.tooltip = Str.enc(string2);
        this.bind = this.C$src$Lvegas_sony_unmap_Bendable_$1we4j6l();
        this.hideFromArrayList = category == Category.NONE ? null
                : BooleanValue.create(this, "Hide from ArrayList //$ui", false,
                        "Keeps this module out of the enabled-module HUD //$ui");
        if (this.hideFromArrayList != null) {
            this.addValue(this.hideFromArrayList);
        }
        if (this.bind.usesOwnKeybindStorage() && n != 0) {
            this.bind.getBoundInputs().add(n);
        }
    }

    public boolean t$src$Z$14g275z() {
        return this.getProperty(PropertyContainer.BETA_BADGE);
    }

    public void P(Value<?, ?> value, MinecraftVersionConstraint ... minecraftVersionConstraintArray) {
        this.allValues.add(0, value);
        List<MinecraftVersionConstraint> list = MinecraftVersionConstraint.L(minecraftVersionConstraintArray);
        if (!list.isEmpty()) {
            return;
        }
        this.values.add(0, value);
    }

    protected Bendable C$src$Lvegas_sony_unmap_Bendable_$1we4j6l() {
        return new ModBendable(this);
    }

    public boolean boolean_r() {
        return this.isEnabled();
    }
}

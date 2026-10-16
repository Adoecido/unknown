package vegas.sony.module.none;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import func.skidline.RectData;
import vegas.sony.SonyVegas;
import vegas.sony.config.PublicProfileSettings;
import vegas.sony.event.EventHandler;
import vegas.sony.event.impl.EventKeyPress;
import vegas.sony.event.impl.EventMouseButton;
import vegas.sony.event.impl.EventPreRenderTick;
import vegas.sony.friend.ui.EnemySettingsFrame;
import vegas.sony.friend.ui.OnlineFriendsFrame;
import vegas.sony.input.KeyboardInput;
import vegas.sony.input.MouseInput;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.module.Category;
import vegas.sony.module.Mod;
import vegas.sony.module.blatant.invwalk.InvWalkSettingsState;
import vegas.sony.module.render.ClickGui;
import vegas.sony.module.utility.inventory.HotbarSlotRuleItemPickerFrame;
import vegas.sony.module.utility.inventory.cleaner.ui.InventoryCleanerPopupFrame;
import vegas.sony.module.utility.inventory.cleaner.ui.InventoryFilterRuleEditorFrame;
import vegas.sony.notification.NotificationManager;
import vegas.sony.render.ShaderGroupRenderStateManager;
import vegas.sony.runtime.NativeBridge;
import vegas.sony.tutorial.TutorialFrame;
import vegas.sony.ui.click.GuiMouseEvent;
import vegas.sony.ui.click.GuiMouseListener;
import vegas.sony.ui.click.MouseButton;
import vegas.sony.ui.click.MousePosition;
import vegas.sony.ui.click.component.DropdownSelectComponent;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.ui.click.component.PopupMenuButtonComponent;
import vegas.sony.ui.click.component.TextInputComponentBase;
import vegas.sony.ui.click.component.ToolTips;
import vegas.sony.ui.click.component.value.ListValueDropdownLayer;
import vegas.sony.ui.click.frame.Frame;
import vegas.sony.ui.click.frame.FrameStackManager;
import vegas.sony.ui.click.frame.PopupFrame;
import vegas.sony.ui.click.frame.impl.ClientSettingsSearchFrame;
import vegas.sony.ui.click.frame.impl.FrameMacros;
import vegas.sony.ui.click.frame.impl.ModuleCategoryFrame;
import vegas.sony.ui.click.frame.impl.ModuleCategoryFrameHeader;
import vegas.sony.ui.click.frame.impl.ModuleSearchFrame;
import vegas.sony.ui.click.frame.impl.SessionSpoofFrame;
import vegas.sony.ui.click.frame.impl.VisibleModuleListFrame;
import vegas.sony.ui.click.frame.impl.hud.ActiveModuleStackFrame;
import vegas.sony.ui.click.frame.impl.hud.AnchoredHudModuleConfigFrame;
import vegas.sony.ui.click.frame.impl.hud.HudModuleConfigFrameBase;
import vegas.sony.ui.click.frame.impl.hud.HudModuleFrameBase;
import vegas.sony.ui.click.frame.impl.hud.HudSettingsFrameBase;
import vegas.sony.ui.click.frame.impl.main.ClickGuiFrameManager;
import vegas.sony.ui.click.frame.impl.main.ClickGuiStyle;
import vegas.sony.ui.click.frame.impl.profile.ProfileSnapshotFrame;
import vegas.sony.ui.click.frame.impl.profile.ProfilesSettingsFrame;
import vegas.sony.ui.click.frame.impl.profile.PublicProfilesFrame;
import vegas.sony.ui.font.FontManager;
import vegas.sony.ui.font.FontSelector;
import vegas.sony.ui.font.SmoothFontRenderer;
import vegas.sony.unmap.Bendable;
import vegas.sony.unmap.ColorUtil;
import vegas.sony.unmap.ModeOption;
import vegas.sony.unmap.ModeSelection;
import vegas.sony.utils.ThreadBoundExecutor;
import vegas.sony.utils.MutableColor;
import vegas.sony.utils.render.BlurRegionRenderer;
import vegas.sony.utils.render.GuiRenderPrimitives;
import vegas.sony.utils.render.OpenGlBackendHolder;
import vegas.sony.utils.render.RenderBatchManager;
import vegas.sony.utils.render.RenderUtils;
import vegas.sony.value.BooleanValue;
import vegas.sony.value.ModeValue;
import vegas.sony.value.NumberValue;
import vegas.sony.value.Value;
import vegas.sony.wrapper.impl.KeyBinding;
import vegas.sony.wrapper.impl.Minecraft;
import java.awt.Color;
import java.awt.Point;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClientSettings
extends Mod {
    private boolean initialized = false;
    public static ClientSettingsSearchFrame settingsSearchFrame;
    public static ClickGuiFrameManager clickGuiFrameManager;
    public final BooleanValue showEnabledCount;
    public final ModeOption floatingSearchBarMode;
    public static GuiComponent activeComponent;
    private float rainbowHue = 0.0f;
    public final BooleanValue showTooltips;
    private double lastGuiScale = -1.0;
    public static boolean moduleSearchActive;
    public final BooleanValue guiBindIndicator;
    public final BooleanValue loaderInvisible;
    public final BooleanValue bindsOnlyInGame;
    public boolean inputEnabled = true;
    public final BooleanValue blurBackground = BooleanValue.create((Object)((Object)this), (String)"Blur background //$ui", (boolean)true, (String)"Blur the background of the GUI //$ui");
    public static ModuleCategoryFrame legitModuleCategoryFrame;
    private final Set<Frame> framesToUpdate;
    private static RectData lastScreenRect;
    public static FrameStackManager hotbarRuleEditorStack;
    public static ClientSettings INSTANCE;
    public final BooleanValue multiKeybinding = BooleanValue.create((Object)((Object)this), (String)"Enable Multi-Keybinding //$ui", (boolean)false, (String)"Allows you to set multiple keys to be held together to toggle modules //$ui");
    private FrameStackManager activeStack;
    public final NumberValue rainbowSpeed;
    public final ModeValue language;
    public static ModuleSearchFrame moduleSearchFrame;
    public final ModeOption integratedSearchBarMode;
    private static final List<FrameStackManager> allStacks;
    private static HashSet<Frame> positionedFrames;
    public static FrameStackManager sessionSpoofStack;
    public static boolean framesInitialized;
    public static final ThreadBoundExecutor UI_EXECUTOR;
    public static FrameStackManager inventoryCleanerStack;
    public final ModeOption noSearchBarMode;
    private static ImmutableList<Frame> frameSnapshot;
    public final ModeValue searchBarStyle;
    public static FrameStackManager mainStack;
    public final BooleanValue smoothFont;
    private final BlurRegionRenderer blurRenderer;
    private static final HashMap<Class<?>, Frame> frameCache;
    public static ToolTips activeTooltips;
    private boolean savedChatKeyState;
    public static FrameStackManager publicProfilesStack;
    private Frame pendingFrame;
    public static FrameStackManager hudEditorStack;
    public static FrameStackManager profileSnapshotStack;
    private boolean layoutRefreshPending;
    private static final List<Frame> allFrames;

    private void renderFrames() {
        for (Frame frame : allFrames) {
            if (!frame.V$src$Z$1xhop3l() || !frame.y$src$Z$1f55jvh()) continue;
            frame.q$src$V$1x8c1kv();
        }
    }

    public static Frame findFrameByName(String name) {
        for (Frame frame : allFrames) {
            if (frame.getName() == null || !frame.getName().equalsIgnoreCase(name)) continue;
            return frame;
        }
        return null;
    }

    private void applyStandaloneMode(PublicProfileSettings publicProfileSettings) {
        ClickGui clickGui = SonyVegas.INSTANCE.getModManager().getMod(ClickGui.class);
        ClickGuiStyle selectedStyle = clickGui == null ? ClickGuiStyle.VAPE : clickGui.getSelectedStyle();
        if (this.activeStack == mainStack || this.activeStack == clickGuiFrameManager) {
            if (publicProfileSettings.centralGuiStyle.isSelected()) {
                this.openCentralClickGui(selectedStyle);
            } else {
                if (this.activeStack == mainStack) {
                    return;
                }
                this.switchFrameStack(mainStack);
            }
        }
    }

    /** Opens the central GUI and applies the style selected by the Render/ClickGui module. */
    public void openCentralClickGui(ClickGuiStyle style) {
        PublicProfileSettings publicProfileSettings = SonyVegas.INSTANCE.getPublicProfileSettings();
        if (!publicProfileSettings.centralGuiStyle.isSelected()) {
            publicProfileSettings.guiStyle.setValue(publicProfileSettings.centralGuiStyle);
        }
        if (clickGuiFrameManager == null) {
            clickGuiFrameManager = new ClickGuiFrameManager();
        }
        clickGuiFrameManager.setStyle(style);
        if (this.activeStack != clickGuiFrameManager) {
            this.switchFrameStack(clickGuiFrameManager);
        }
    }

    public static void positionFrameIfNeeded(Frame frame) {
        boolean overlapsExistingFrame;
        if (frameSnapshot == null) {
            return;
        }
        if (!frameSnapshot.contains((Object)frame)) {
            return;
        }
        if (positionedFrames.contains(frame)) {
            return;
        }
        if (!frame.J$src$Z$1eqdghz() || !frame.l$src$Z$193vdc5()) {
            return;
        }
        double candidateX = 32.0;
        double candidateY = 32.0;
        do {
            overlapsExistingFrame = false;
            for (Frame otherFrame : frameSnapshot) {
                RectData bounds;
                if (!ClientSettings.INSTANCE.activeStack.Y().contains(otherFrame) || otherFrame.equals(frame) || !otherFrame.V$src$Z$1xhop3l() || !(bounds = otherFrame.getBounds().y(2.0, 4.0)).J(candidateX, candidateY)) continue;
                overlapsExistingFrame = true;
            }
            if (!((candidateX += 2.0) + frame.A() > (double)Minecraft.G().getScaledWidth())) continue;
            candidateX = 32.0;
            candidateY += 2.0;
        } while (overlapsExistingFrame);
        frame.K(candidateX);
        frame.S(candidateY);
        frame.l$src$V$1mibm4x();
        positionedFrames.add(frame);
    }

    @Deprecated
    public static void setFrameVisibility(Class<? extends Frame> frameClass, boolean animate) {
        Frame frame = ClientSettings.getFrame(frameClass);
        if (frame != null) {
            frame.t(!frame.V$src$Z$1xhop3l(), animate);
            frame.U();
        }
    }

    public static void refreshCategoryHeader(Category category) {
        for (Frame frame : allFrames) {
            if (!((ModeSelection)ClientSettings.INSTANCE.searchBarStyle.getValue()).equals(ClientSettings.INSTANCE.integratedSearchBarMode) && frame instanceof ClientSettingsSearchFrame || !(frame instanceof ModuleCategoryFrame) || !((ModuleCategoryFrame)frame).G$src$Lvegas_sony_module_Category_$qyt4o7().equals(category)) continue;
            int enabledModuleCount = ((ModuleCategoryFrame)frame).A$src$I$wwnvku();
            if (!(frame.j$src$Lvegas_sony_ui_click_frame_FrameHeaderComponent_$175vsfc() instanceof ModuleCategoryFrameHeader)) continue;
            ((ModuleCategoryFrameHeader)frame.j$src$Lvegas_sony_ui_click_frame_FrameHeaderComponent_$175vsfc()).y(enabledModuleCount);
        }
    }

    public boolean handleSearchShortcut(EventKeyPress eventKeyPress) {
        int keyCode = eventKeyPress.getKey();
        boolean keyDown = eventKeyPress.isDown();
        if ((KeyboardInput.isKeyDown((int)163) || KeyboardInput.isKeyDown((int)162)) && keyCode == 70 && keyDown) {
            ClickGuiFrameManager clickGuiFrameManager;
            if (this.activeStack instanceof ClickGuiFrameManager && (clickGuiFrameManager = (ClickGuiFrameManager)this.activeStack).getMainFrame() != null) {
                return clickGuiFrameManager.getMainFrame().clearActiveSearch();
            }
            if (!this.noSearchBarMode.isSelected()) {
                if (this.floatingSearchBarMode.isSelected()) {
                    moduleSearchFrame.n$src$Lvegas_sony_ui_click_frame_impl_ModuleSearchFrameHe$xia8v2().s();
                    moduleSearchFrame.n$src$Lvegas_sony_ui_click_frame_impl_ModuleSearchFrameHe$xia8v2().A$src$Lvegas_sony_ui_click_component_input_ModuleSearchIn$1efzz7n().requestFocus();
                } else if (this.integratedSearchBarMode.isSelected()) {
                    settingsSearchFrame.o$src$Lvegas_sony_ui_click_frame_impl_ClientSettingsSearc$hz70uz().O$src$Lvegas_sony_ui_click_component_input_ModuleSearchIn$1smhagf().setText("");
                    settingsSearchFrame.K$src$V$1nbah4f();
                    settingsSearchFrame.o$src$Lvegas_sony_ui_click_frame_impl_ClientSettingsSearc$hz70uz().V$src$V$enocyv();
                }
            }
            return true;
        }
        return false;
    }

    public static boolean canReceiveInput(GuiComponent guiComponent) {
        return activeComponent == null || activeComponent instanceof TextInputComponentBase || activeComponent instanceof DropdownSelectComponent || activeComponent instanceof PopupMenuButtonComponent || activeComponent.equals(guiComponent) || activeComponent.f().contains(guiComponent);
    }

    public boolean k() {
        return false;
    }

    public void toggle() {
        if (Minecraft.currentScreen().getObject() != null
                && !Minecraft.currentScreen().isInstance(MappedClasses.u5)) {
            return;
        }
        super.toggle();
        if (this.isEnabled()) {
            Minecraft.R();
            this.savedChatKeyState = Minecraft.gameSettings().m$src$Z$1s8ei5l();
            Minecraft.gameSettings().P(false);
            this.inputEnabled = false;
            if (NativeBridge.ss_2("show") == 0) {
                ClickGui clickGui = SonyVegas.INSTANCE.getModManager().getMod(ClickGui.class);
                this.openCentralClickGui(clickGui == null
                        ? ClickGuiStyle.VAPE : clickGui.getSelectedStyle());
            }
        } else {
            this.closeGui();
        }
    }

    @Override
    public void onBindActivated() {
        // GUI activation needs the cursor/focus transition performed by the
        // specialized toggle above. Mod's default implementation only flips
        // the enabled flag and therefore leaves the in-game GUI invisible.
        this.toggle();
    }

    private static void onSearchBarStyleChanged(ModeValue ignoredValue) {
        settingsSearchFrame.N$src$V$1ncxuwi();
    }

    public void renderFramesAndNotifications() {
        this.updateFrames();
        NotificationManager notificationManager = SonyVegas.INSTANCE.getNotificationManager();
        notificationManager.renderNotifications();
    }

    public void handleMouseButton(int button) {
        if (NativeBridge.isModuleMenuVisible()) {
            return;
        }
        RenderUtils.C();
        this.dispatchMouseButton(button);
    }

    public void handleMouseButton(int button, int mouseX, int mouseY) {
        if (NativeBridge.isModuleMenuVisible()) {
            return;
        }
        RenderUtils.C(mouseX, mouseY);
        try {
            this.dispatchMouseButton(button);
        }
        finally {
            RenderUtils.C();
        }
    }

    private void dispatchMouseButton(int button) {
        MousePosition mousePosition = RenderUtils.h();
        GuiMouseEvent guiMouseEvent = new GuiMouseEvent(mousePosition.O, mousePosition.H, MouseButton.A((int)button));
        if (guiMouseEvent.fire()) {
            return;
        }
        if (activeComponent != null) {
            boolean capturesMouseInput = !(activeComponent instanceof TextInputComponentBase);
            activeComponent.dispatchMouseEvent(guiMouseEvent);
            if (capturesMouseInput) {
                return;
            }
        }
        CopyOnWriteArrayList<Frame> framesInInputOrder = new CopyOnWriteArrayList<Frame>(this.activeStack.Y());
        Collections.reverse(framesInInputOrder);
        if (!(this.activeStack instanceof ClickGuiFrameManager)) {
            for (Frame frame : framesInInputOrder) {
                if (!frame.V$src$Z$1xhop3l() || !frame.t$src$Lvegas_sony_ui_click_component_gui_TextButton_$p5ute3().V$src$Z$1xhop3l() || !frame.t$src$Lvegas_sony_ui_click_component_gui_TextButton_$p5ute3().w$src$Z$e457mb()) continue;
                frame.t$src$Lvegas_sony_ui_click_component_gui_TextButton_$p5ute3().dispatchPrimaryClick();
                return;
            }
        }
        boolean listenerConsumedEvent = false;
        block1: for (Frame frame : framesInInputOrder) {
            if (!frame.V$src$Z$1xhop3l() || this.activeStack instanceof ClickGuiFrameManager && ((ClickGuiFrameManager)this.activeStack).isMainLayerHudFrame(frame)) continue;
            for (GuiMouseListener guiMouseListener : frame.getGlobalMouseListeners()) {
                if (guiMouseListener.Q(new Point(guiMouseEvent.getX(), guiMouseEvent.getY()))) {
                    listenerConsumedEvent = true;
                    break;
                }
                guiMouseListener.g(new Point(guiMouseEvent.getX(), guiMouseEvent.getY()), guiMouseEvent.getAction().G());
            }
            for (GuiMouseListener guiMouseListener : frame.getMouseListeners()) {
                if (!guiMouseListener.Q(new Point(guiMouseEvent.getX(), guiMouseEvent.getY()))) continue;
                listenerConsumedEvent = true;
                continue block1;
            }
        }
        if (!listenerConsumedEvent) {
            for (Frame frame : framesInInputOrder) {
                boolean managedByClickGui;
                if (!frame.V$src$Z$1xhop3l()) continue;
                boolean bl = managedByClickGui = this.activeStack instanceof ClickGuiFrameManager && ((ClickGuiFrameManager)this.activeStack).isMainLayerHudFrame(frame);
                if (!managedByClickGui) {
                    frame.e(guiMouseEvent);
                }
                if (frame.G((double)guiMouseEvent.getX(), (double)guiMouseEvent.getY())) {
                    if (managedByClickGui) {
                        if (guiMouseEvent.getAction() != MouseButton.LEFT_CLICK) break;
                        frame.U();
                        frame.dispatchMouseEvent(guiMouseEvent);
                        break;
                    }
                    if (frame instanceof HudModuleConfigFrameBase) {
                        HudModuleConfigFrameBase hudModuleConfigFrameBase = (HudModuleConfigFrameBase)frame;
                        if (!hudModuleConfigFrameBase.isHudEditorContext()) continue;
                    }
                    frame.U();
                    frame.dispatchMouseEvent(guiMouseEvent);
                    break;
                }
                if (!(frame instanceof HudModuleFrameBase)) continue;
                HudModuleFrameBase hudModuleFrameBase = (HudModuleFrameBase)frame;
                hudModuleFrameBase.setHudEditorSelected(false);
            }
        }
    }
    public void openGui() {
        if (!this.isEnabled()) {
            Minecraft.R();
            this.savedChatKeyState = Minecraft.gameSettings().m$src$Z$1s8ei5l();
            Minecraft.gameSettings().P(false);
            this.setEnabled(true);
        }
        this.setInputEnabled(false);
        if (NativeBridge.ss_2("show") == 0) {
            ClickGui clickGui = SonyVegas.INSTANCE.getModManager().getMod(ClickGui.class);
            this.openCentralClickGui(clickGui == null
                    ? ClickGuiStyle.VAPE : clickGui.getSelectedStyle());
        }
    }

    @EventHandler
    public void onMouseButton(EventMouseButton eventMouseButton) {
        if (!this.inputEnabled) {
            eventMouseButton.setCancelled(true);
        }
    }

    public JsonArray serializeFrameStates() {
        JsonArray jsonArray = new JsonArray();
        for (Frame frame : ClientSettings.getAllFrames()) {
            if (frame.getName().startsWith("sidecar_")) continue;
            jsonArray.add((JsonElement)frame.Z());
        }
        return jsonArray;
    }

    public static void queueFrameOpen(Frame frame) {
        if (!frame.d$src$Z$1lx9d06()) {
            return;
        }
        ClientSettings.INSTANCE.pendingFrame = frame;
    }

    public void requestFrameLayoutRefresh() {
        this.layoutRefreshPending = true;
    }

    public boolean isInputEnabled() {
        return this.inputEnabled;
    }

    public void updateGuiScale() {
        SonyVegas.INSTANCE.getClientSettings().guiColor.applyConfiguredColorTransform();
        double currentGuiScale = SonyVegas.INSTANCE.getClientSettings().getGuiScaleFactor();
        if (this.lastGuiScale != currentGuiScale && this.lastGuiScale != -1.0) {
            FontManager fontManager = SonyVegas.INSTANCE.getFontManager();
            for (Map<Integer, SmoothFontRenderer> map : fontManager.n().values()) {
                for (SmoothFontRenderer smoothFontRenderer : map.values()) {
                    smoothFontRenderer.f();
                }
            }
            this.refreshFrameLayouts();
        }
        this.lastGuiScale = currentGuiScale;
    }

    public static void refreshModuleCategoryHeaders() {
        for (Frame frame : allFrames) {
            if (!(frame instanceof ModuleCategoryFrame) || frame instanceof ClientSettingsSearchFrame) continue;
            int enabledModuleCount = ((ModuleCategoryFrame)frame).A$src$I$wwnvku();
            if (!(frame.j$src$Lvegas_sony_ui_click_frame_FrameHeaderComponent_$175vsfc() instanceof ModuleCategoryFrameHeader)) continue;
            ((ModuleCategoryFrameHeader)frame.j$src$Lvegas_sony_ui_click_frame_FrameHeaderComponent_$175vsfc()).y(enabledModuleCount);
        }
    }

    public static void removePopup(PopupFrame popupFrame) {
        FrameStackManager frameStackManager = INSTANCE.getActiveStack();
        UI_EXECUTOR.execute(() -> ClientSettings.removePopupFromStack(frameStackManager, popupFrame));
        popupFrame.Q$src$Lvegas_sony_ui_click_frame_Frame_$1y8ivjg().s$src$Ljava_util_ArrayList_$1a2240q().remove(popupFrame);
    }

    public void syncRainbowHue() {
        MutableColor mutableColor = SonyVegas.INSTANCE.getClientSettings().guiColor.getMutableColor();
        float[] fArray = new float[3];
        Color.RGBtoHSB(mutableColor.getRed(), mutableColor.getGreen(), mutableColor.getBlue(), fArray);
        this.rainbowHue = fArray[0];
    }

    public void loadFrameStates(JsonArray jsonArray) {
        if (jsonArray.size() == 0) {
            return;
        }
        jsonArray = jsonArray.get(0).getAsJsonArray();
        for (int i = 0; i < jsonArray.size(); ++i) {
            try {
                JsonObject jsonObject;
                JsonElement jsonElement = jsonArray.get(i);
                if (!jsonElement.isJsonObject() || jsonElement.isJsonNull() || (jsonObject = jsonElement.getAsJsonObject()).get("title") == null || jsonObject.get("title").isJsonNull()) continue;
                for (Frame frame : ClientSettings.getAllFrames()) {
                    if (!frame.getName().equals(jsonObject.get("title").getAsString())) continue;
                    frame.t(jsonObject);
                }
                continue;
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }

    public static void removeFramePopups(Frame frame) {
        FrameStackManager frameStackManager = INSTANCE.getActiveStack();
        UI_EXECUTOR.execute(() -> ClientSettings.removePopupsFromStack(frame, frameStackManager));
        frame.L$src$Lvegas_sony_ui_click_frame_Frame_$1djx6sa().s$src$Ljava_util_ArrayList_$1a2240q().clear();
    }

    public static ArrayList<PopupFrame> getCurrentPopups() {
        ArrayList<PopupFrame> popups = new ArrayList<PopupFrame>();
        for (Frame frame : ClientSettings.INSTANCE.activeStack.Y()) {
            popups.addAll(frame.s$src$Ljava_util_ArrayList_$1a2240q());
        }
        return popups;
    }

    public void renderHudFrames() {
        int previousAlphaFunction = OpenGlBackendHolder.backend.getIntegerState(3009);
        float previousAlphaReference = OpenGlBackendHolder.backend.getFloat(3010);
        OpenGlBackendHolder.backend.setAlphaFunction(516, 0.0f);
        if (INSTANCE.isInputEnabled()) {
            OpenGlBackendHolder.backend.pushMatrix();
            double guiScale = SonyVegas.INSTANCE.getClientSettings().getGuiScaleFactor();
            OpenGlBackendHolder.backend.scale(guiScale, guiScale, guiScale);
            if (INSTANCE.isInputEnabled()) {
                this.renderFrames();
                NotificationManager notificationManager = SonyVegas.INSTANCE.getNotificationManager();
                notificationManager.renderNotifications();
            }
            OpenGlBackendHolder.backend.popMatrix();
        }
        OpenGlBackendHolder.backend.setAlphaFunction(previousAlphaFunction, previousAlphaReference);
    }

    public static void toggleCategoryFrame(String categoryName) {
        Frame frame = null;
        for (Frame frame2 : allFrames) {
            if (!(frame2 instanceof ModuleCategoryFrame) || !((ModuleCategoryFrame)frame2).L$src$Ljava_lang_String_$ahld16().equalsIgnoreCase(categoryName)) continue;
            frame2.setVisible(!frame2.V$src$Z$1xhop3l());
            frame = frame2;
        }
        if (frame != null) {
            frame.U();
        }
    }

    public void onFinishModuleInitialization() {
    }

    private static void registerFrame(Frame frame, FrameStackManager ... stacks) {
        allFrames.add(frame);
        for (FrameStackManager stack : stacks) {
            stack.q(frame);
            if (allStacks.contains(stack)) continue;
            allStacks.add(stack);
        }
    }

    private void onStandaloneModeChanged(ModeValue modeValue) {
        this.updateStandaloneState();
    }

    private void removeClosedPopups() {
        ArrayList<PopupFrame> closedPopups = new ArrayList<PopupFrame>();
        for (Frame frame : this.activeStack.Y()) {
            for (PopupFrame popupFrame : frame.s$src$Ljava_util_ArrayList_$1a2240q()) {
                if (!popupFrame.c$src$Z$1kex42k()) continue;
                closedPopups.add(popupFrame);
            }
        }
        for (PopupFrame popupFrame : closedPopups) {
            popupFrame.Q$src$Lvegas_sony_ui_click_frame_Frame_$1y8ivjg().s$src$Ljava_util_ArrayList_$1a2240q().remove(popupFrame);
            this.activeStack.m((Frame)popupFrame);
        }
        if (activeComponent instanceof DropdownSelectComponent) {
            ((DropdownSelectComponent)activeComponent).closePopup();
        }
    }

    private void updateFrames() {
        this.activeStack.A();
        if (this.layoutRefreshPending) {
            for (Frame frame : this.activeStack.Y()) {
                frame.l$src$V$1mibm4x();
            }
            this.layoutRefreshPending = false;
        }
        ArrayList<Frame> activeFrames = new ArrayList<Frame>(this.activeStack.Y());
        for (Frame frame : activeFrames) {
            frame.A$src$V$4ceaf0();
        }
        this.syncRainbowHue();
        CopyOnWriteArrayList<Frame> reverseRenderOrder = new CopyOnWriteArrayList<Frame>(activeFrames);
        CopyOnWriteArrayList<Frame> forwardRenderOrder = new CopyOnWriteArrayList<Frame>(activeFrames);
        Collections.reverse(reverseRenderOrder);
        MouseInput.getState().prepareFrame();
        for (Frame frame : forwardRenderOrder) {
            if (frame instanceof TutorialFrame) continue;
            this.renderFrame(frame, reverseRenderOrder);
        }
        MouseInput.getState().resetScrollDelta();
        if (activeComponent instanceof DropdownSelectComponent || activeComponent instanceof PopupMenuButtonComponent) {
            boolean previousClipState = activeComponent.isShowDisabledOverlay();
            activeComponent.setShowDisabledOverlay(false);
            activeComponent.c();
            activeComponent.setShowDisabledOverlay(previousClipState);
            if (activeComponent.getParentFrameComponent() != null && activeComponent.getParentFrameComponent().k$src$Z$if6xeb()) {
                activeComponent.getParentFrameComponent().M();
            }
        }
        for (Frame frame : forwardRenderOrder) {
            if (frame.V$src$Z$1xhop3l() && frame.a$src$Z$1f30q5a() && !(this.activeStack instanceof ClickGuiFrameManager)) {
                frame.t$src$Lvegas_sony_ui_click_component_gui_TextButton_$p5ute3().setVisible(true);
                frame.t$src$Lvegas_sony_ui_click_component_gui_TextButton_$p5ute3().c();
                if (!frame.t$src$Lvegas_sony_ui_click_component_gui_TextButton_$p5ute3().t()) continue;
                frame.t$src$Lvegas_sony_ui_click_component_gui_TextButton_$p5ute3().J();
                continue;
            }
            frame.t$src$Lvegas_sony_ui_click_component_gui_TextButton_$p5ute3().setVisible(false);
        }
        if (activeTooltips != null) {
            activeTooltips.c();
        }
    }

    private static void removePopupsFromStack(Frame frame, FrameStackManager frameStackManager) {
        for (PopupFrame popupFrame : frame.s$src$Ljava_util_ArrayList_$1a2240q()) {
            frameStackManager.m((Frame)popupFrame);
        }
    }

    public boolean isMainGuiStack() {
        FrameStackManager frameStackManager = this.getActiveStack();
        return frameStackManager.equals(mainStack) || frameStackManager instanceof ClickGuiFrameManager;
    }

    private static void removePopupFromStack(FrameStackManager frameStackManager, PopupFrame popupFrame) {
        frameStackManager.m((Frame)popupFrame);
    }

    public Color getAccentColor() {
        if (SonyVegas.INSTANCE.getClientSettings().guiColor.isRainbowEnabled()) {
            this.rainbowHue = (float)((double)this.rainbowHue - 0.03);
            if (this.rainbowHue <= 0.0f) {
                this.rainbowHue = 1.0f - -this.rainbowHue;
            }
            return ColorUtil.createReadableHsbColor((float)this.rainbowHue, (float)0.9f, (float)1.0f);
        }
        return SonyVegas.INSTANCE.getClientSettings().guiColor.getMutableColor();
    }

    public static synchronized void initializeFrames() {
        vegas.sony.runtime.CheatBreakerDebugLog.info("INIT_MANAGERS initFrames begin //$");
        if (framesInitialized) {
            return;
        }
        for (FrameStackManager frameStackManager : allStacks) {
            frameStackManager.Y().clear();
        }
        allFrames.clear();
        SonyVegas.INSTANCE.initializeRender();
        ClientSettings.registerFrame((Frame)new ActiveModuleStackFrame(), mainStack);
        moduleSearchFrame = new ModuleSearchFrame();
        ClientSettings.registerFrame((Frame)moduleSearchFrame, mainStack);
        settingsSearchFrame = new ClientSettingsSearchFrame();
        ClientSettings.registerFrame((Frame)settingsSearchFrame, mainStack);
        legitModuleCategoryFrame = new ModuleCategoryFrame(Category.COMBAT);
        ClientSettings.registerFrame((Frame)legitModuleCategoryFrame, mainStack);
        SonyVegas sonyvegas = SonyVegas.INSTANCE;
        if (sonyvegas.isFeatureDisabled()) {
        }
        ClientSettings.registerFrame((Frame)new ModuleCategoryFrame(Category.RENDER), mainStack);
        ClientSettings.registerFrame((Frame)new ModuleCategoryFrame(Category.NETWORK), mainStack);
        ClientSettings.registerFrame((Frame)new ModuleCategoryFrame(Category.GLADIADOR), mainStack);
        ClientSettings.registerFrame((Frame)new ModuleCategoryFrame(Category.MISC), mainStack);
        ClientSettings.registerFrame((Frame)new VisibleModuleListFrame(), mainStack);
        ClientSettings.registerFrame((Frame)new ProfilesSettingsFrame(), mainStack);
        ClientSettings.registerFrame((Frame)new FrameMacros(), mainStack);
        ClientSettings.registerFrame((Frame)new OnlineFriendsFrame(), mainStack);
        ClientSettings.registerFrame((Frame)new EnemySettingsFrame(), mainStack);
        /*
         * Keep the picker reference instead of looking it up by canonical
         * name.  Badlion can load the payload through its LaunchClassLoader,
         * where canonical-name lookup may miss the just-registered frame and
         * return null during bootstrap.
         */
        HotbarSlotRuleItemPickerFrame hotbarPicker = new HotbarSlotRuleItemPickerFrame();
        ClientSettings.registerFrame((Frame)hotbarPicker, hotbarRuleEditorStack);
        Frame itemListFrame = hotbarPicker.getItemListFrame();
        if (itemListFrame != null) {
            ClientSettings.registerFrame(itemListFrame, hotbarRuleEditorStack);
        }
        ClientSettings.registerFrame((Frame)new PublicProfilesFrame(), publicProfilesStack);
        ClientSettings.registerFrame((Frame)new SessionSpoofFrame(), sessionSpoofStack);
        ClientSettings.registerFrame((Frame)new ProfileSnapshotFrame(), profileSnapshotStack);
        ClientSettings.registerFrame((Frame)new InventoryCleanerPopupFrame(), inventoryCleanerStack);
        ClientSettings.registerFrame((Frame)new InventoryFilterRuleEditorFrame(), inventoryCleanerStack);
        frameSnapshot = ImmutableList.copyOf(allFrames);
        vegas.sony.runtime.CheatBreakerDebugLog.info("INIT_MANAGERS frames registered //$");
        ClientSettings.refreshModuleCategoryHeaders();
        VisibleModuleListFrame.e();
        vegas.sony.runtime.CheatBreakerDebugLog.info("INIT_MANAGERS headers done //$");
        framesInitialized = true;
        vegas.sony.runtime.CheatBreakerDebugLog.info("INIT_MANAGERS initFrames end //$");
    }

    @EventHandler
    public void processPendingFrame(EventPreRenderTick ignoredEvent) {
        if (this.pendingFrame != null) {
            this.activeStack.v(this.pendingFrame);
            this.pendingFrame.s$src$Ljava_util_ArrayList_$1a2240q().forEach(PopupFrame::A$src$V$4ceaf0);
            this.pendingFrame = null;
        }
    }

    public void refreshFrameLayouts() {
        for (FrameStackManager object : allStacks) {
            for (Frame frame : object.Y()) {
                if (!frame.n$src$Z$1fa61uz()) continue;
                frame.j(true);
                frame.H(true);
            }
        }
        if (clickGuiFrameManager != null) {
            for (Frame frame : clickGuiFrameManager.Y()) {
                if (!frame.n$src$Z$1fa61uz()) continue;
                frame.j(true);
                frame.H(true);
            }
        }
        ClientSettings.getFrame(ModuleSearchFrame.class).p();
    }

    @Deprecated
    public static void showFrame(Class<? extends Frame> frameClass) {
        ClientSettings.setFrameVisibility(frameClass, true);
    }

    public static <T extends PopupFrame> T createPopup(GuiComponent owner, GuiComponent content, Class<T> popupClass) {
        try {
            Constructor<T> constructor = popupClass.getConstructor(GuiComponent.class, GuiComponent.class);
            PopupFrame popupFrame = (PopupFrame)constructor.newInstance(owner, content);
            ClientSettings.INSTANCE.activeStack.Y().add(popupFrame);
            owner.L$src$Lvegas_sony_ui_click_frame_Frame_$1djx6sa().s$src$Ljava_util_ArrayList_$1a2240q().add(popupFrame);
            return (T)popupFrame;
        }
        catch (Exception exception) {
            SonyVegas.logThrowable((Throwable)exception);
            return null;
        }
    }

    public static void closeListDropdowns() {
        for (Frame frame : ClientSettings.INSTANCE.activeStack.Y()) {
            if (!(frame instanceof ListValueDropdownLayer)) continue;
            ((ListValueDropdownLayer)frame).refreshContents();
        }
    }

    public void renderGui() {
        if (NativeBridge.isModuleMenuVisible()
                && NativeBridge.ss_2("render") != 0) {
            return;
        }
        try {
            GuiRenderPrimitives.Y();
            RenderUtils.g();
            RenderUtils.C();
            boolean iceGuiActive = this.activeStack == clickGuiFrameManager
                    && clickGuiFrameManager != null
                    && clickGuiFrameManager.getStyle() == ClickGuiStyle.ICE;
            boolean shouldBlur = this.blurBackground.getEffectiveValue().booleanValue()
                    && !iceGuiActive;
            if (!GuiRenderPrimitives.d()) {
                if (shouldBlur) {
                    ShaderGroupRenderStateManager.getInstance().enable();
                } else {
                    ShaderGroupRenderStateManager.getInstance().disable();
                }
            } else if (shouldBlur) {
                int screenWidth = Minecraft.J();
                int screenHeight = Minecraft.h();
                this.blurRenderer.setDimensions(screenWidth, screenHeight);
                this.blurRenderer.renderBlur(0, 0, 16.0f, 0.0f);
            }
            RectData screenBounds = new RectData(0.0, 0.0, (double)Minecraft.J(), (double)Minecraft.h());
            if (lastScreenRect != null && (screenBounds.e() != lastScreenRect.e() || screenBounds.R() != lastScreenRect.R())) {
                this.refreshFrameLayouts();
            }
            lastScreenRect = screenBounds;
            OpenGlBackendHolder.backend.pushMatrix();
            double guiScale = SonyVegas.INSTANCE.getClientSettings().getGuiScaleFactor();
            OpenGlBackendHolder.backend.scale(guiScale, guiScale, guiScale);
            try {
                UI_EXECUTOR.runPending();
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            this.renderFramesAndNotifications();
            OpenGlBackendHolder.backend.popMatrix();
            RenderUtils.f();
            GuiRenderPrimitives.D();
        }
        catch (Exception exception) {
            SonyVegas.logThrowable((Throwable)exception);
        }
        OpenGlBackendHolder.backend.setColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    public static <T extends Frame> T getFrame(Class<T> frameClass) {
        Frame frame = frameCache.get(frameClass);
        if (frame != null) {
            return (T)frame;
        }
        for (Frame candidate : allFrames) {
            if (!candidate.getClass().getCanonicalName().equals(frameClass.getCanonicalName())) continue;
            frameCache.put(frameClass, candidate);
            return (T)candidate;
        }
        return null;
    }

    private void renderFrame(Frame frame, List<Frame> reverseRenderOrder) {
        if (!frame.V$src$Z$1xhop3l()) {
            return;
        }
        frame.c();
        if (this.activeStack instanceof ClickGuiFrameManager && ((ClickGuiFrameManager)this.activeStack).isMainLayerHudFrame(frame)) {
            if (frame instanceof HudModuleFrameBase) {
                HudModuleFrameBase hudModuleFrameBase = (HudModuleFrameBase)frame;
                boolean hasOverlay = frame.t();
                if (hasOverlay) {
                    boolean isFrontmostOverlay = true;
                    for (Frame otherFrame : reverseRenderOrder) {
                        if (otherFrame.equals(frame)) break;
                        if (!otherFrame.V$src$Z$1xhop3l() || !otherFrame.t()) continue;
                        isFrontmostOverlay = false;
                    }
                    hudModuleFrameBase.setFrontmostOverlay(isFrontmostOverlay);
                } else {
                    hudModuleFrameBase.setFrontmostOverlay(false);
                }
            }
            return;
        }
        if (frame.t()) {
            boolean isFrontmostOverlay = true;
            for (Frame otherFrame : reverseRenderOrder) {
                if (otherFrame.equals(frame)) break;
                if (!otherFrame.V$src$Z$1xhop3l() || !otherFrame.t()) continue;
                isFrontmostOverlay = false;
            }
            if (isFrontmostOverlay) {
                frame.J();
            }
        }
    }

    public static void markFramePositioned(Frame frame) {
        positionedFrames.add(frame);
    }

    public void onDisable() {
        super.onDisable();
        this.disableBlurShader();
        InvWalkSettingsState.setPressed((KeyBinding)Minecraft.gameSettings().y$src$Lvegas_sony_wrapper_impl_KeyBinding_$1hvjjoh(), (boolean)false);
    }

    public void renderHudOverlay() {
        if (this.inputEnabled) {
            this.renderHudFrames();
            if (GuiRenderPrimitives.d()) {
                RenderBatchManager.getInstance().flushGuiBatches(0.0f);
            }
        }
    }

    private void updateStandaloneState() {
        PublicProfileSettings publicProfileSettings = SonyVegas.INSTANCE.getPublicProfileSettings();
        if (publicProfileSettings.guiStyle.isPersistenceSuppressed()) {
            return;
        }
        UI_EXECUTOR.execute(() -> this.applyStandaloneMode(publicProfileSettings));
    }

    public void arrangeFrameGrid() {
        double x = 32.0;
        double y = 32.0;
        double rowHeight = 0.0;
        for (Frame frame : frameSnapshot) {
            if (!frame.J$src$Z$1eqdghz() || !frame.l$src$Z$193vdc5()) continue;
            if (x + frame.A() > (double)Minecraft.G().getScaledWidth()) {
                x = 24.0;
                y += rowHeight + 8.0;
                rowHeight = 0.0;
            }
            if (frame.L() > rowHeight) {
                rowHeight = frame.L();
            }
            frame.K(x);
            frame.S(y);
            frame.l$src$V$1mibm4x();
            x += frame.A() + 2.0;
        }
    }

    public static void clearPositionedFrames() {
        positionedFrames.clear();
    }

    public void replaceFrame(Frame frame, Frame replacement) {
        this.activeStack.R(frame, replacement);
    }

    protected Bendable C$src$Lvegas_sony_unmap_Bendable_$1we4j6l() {
        return new ClientSettingsGuiBindBendable((Mod)this);
    }

    public static List<Frame> getAllFrames() {
        return allFrames;
    }

    public ClientSettings() {
        super("GUI //$ui", 161, 0, Category.NONE, "Shift click any module button to bind it to a key.\n(You can shift click this to bind the GUI)\nHold TAB to view modules binds. //$ui");
        this.guiBindIndicator = BooleanValue.create((Object)((Object)this), (String)"GUI bind indicator //$ui", (boolean)true, (String)"Displays a message indicating your GUI keybind upon injecting.\nI.E. \"Press RSHIFT to open GUI\" //$ui");
        this.loaderInvisible = BooleanValue.create((Object)((Object)this), (String)"Loader invisible //$ui", (boolean)false, (String)"Hides the loader window while it remains open //$ui");
        this.bindsOnlyInGame = BooleanValue.create((Object)((Object)this), (String)"Binds only in game //$ui", (boolean)true, (String)"Module binds only respond while the Minecraft window is focused //$ui");
        this.smoothFont = new ClientSettingsBooleanValue((Object)this, "Smooth Font //$ui", true);
        this.showTooltips = BooleanValue.create((Object)((Object)this), (String)"Show tooltips //$ui", (boolean)true, (String)"Toggles visibility of these //$ui");
        this.rainbowSpeed = NumberValue.create((Object)((Object)this), (String)"Rainbow speed //$ui", (String)"#.#", (String)"", (double)0.1, (double)1.0, (double)10.0, (double)0.1, (String)"Adjusts the speed of rainbow values //$ui");
        this.language = (ModeValue)ModeValue.create((Object)((Object)this), (String)"Language //$ui", (ModeSelection)FontSelector.j, (ModeSelection[])new ModeSelection[]{FontSelector.j, FontSelector.S, FontSelector.c, FontSelector.a, FontSelector.P}).setResettable(false);
        this.showEnabledCount = BooleanValue.create((Object)((Object)this), (String)"Show enabled count //$ui", (boolean)true, (String)"Shows the number of enabled modules in the standalone gui //$ui");
        this.blurRenderer = new BlurRegionRenderer(0, 0);
        this.activeStack = mainStack;
        this.framesToUpdate = new HashSet<Frame>(allFrames);
        INSTANCE = this;
        this.floatingSearchBarMode = new ModeOption("Floating //$ui", 0.8);
        this.integratedSearchBarMode = new ModeOption("Integrated //$ui", 0.8);
        this.noSearchBarMode = new ModeOption("None //$ui", 0.8);
        this.searchBarStyle = ModeValue.create((Object)((Object)this), (String)"Search bar style //$ui", (String)"Switch between search bar styles //$ui", (ModeSelection)this.floatingSearchBarMode, (ModeSelection[])new ModeSelection[]{this.integratedSearchBarMode, this.noSearchBarMode, this.floatingSearchBarMode});
        PublicProfileSettings publicProfileSettings = SonyVegas.INSTANCE.getPublicProfileSettings();
        publicProfileSettings.notifications.addDependentValues(new Value[]{publicProfileSettings.toggleAlerts});
        publicProfileSettings.notifications.addDependentValues(new Value[]{publicProfileSettings.profileSwitchNotifications});
        publicProfileSettings.notifications.addDependentValues(new Value[]{publicProfileSettings.friendNotifications});
        this.searchBarStyle.addChangeListener(ClientSettings::onSearchBarStyleChanged);
        this.addValue(new Value[]{this.blurBackground, this.multiKeybinding, this.guiBindIndicator, this.bindsOnlyInGame, this.loaderInvisible, this.showTooltips, this.rainbowSpeed, this.searchBarStyle});
    }

    public void disableBlurShader() {
        if (((ClientSettings)SonyVegas.INSTANCE.getModManager().getMod(ClientSettings.class)).blurBackground.getEffectiveValue().booleanValue()) {
            ShaderGroupRenderStateManager.getInstance().disable();
        }
    }

    public void switchFrameStack(FrameStackManager frameStackManager) {
        activeComponent = null;
        this.removeClosedPopups();
        activeTooltips = null;
        FrameStackManager frameStackManager2 = this.activeStack;
        if (frameStackManager2 instanceof ClickGuiFrameManager) {
            ClickGuiFrameManager clickGuiFrameManager = (ClickGuiFrameManager)frameStackManager2;
            if (clickGuiFrameManager.getMainFrame() != null) {
                clickGuiFrameManager.getMainFrame().closeActiveOverlay();
            }
            clickGuiFrameManager.closeSidecar();
        }
        for (Frame frame : allFrames) {
            if (!(frame instanceof HudModuleFrameBase)) continue;
            HudModuleFrameBase hudModuleFrameBase = (HudModuleFrameBase)frame;
            hudModuleFrameBase.setHudEditorSelected(false);
            hudModuleFrameBase.hideEditorControls();
            AnchoredHudModuleConfigFrame anchoredHudModuleConfigFrame = hudModuleFrameBase.getAnchoredSettingsFrameInternal();
            if (frameStackManager2 != null) {
                frameStackManager2.m((Frame)anchoredHudModuleConfigFrame);
            }
            frameStackManager.m((Frame)anchoredHudModuleConfigFrame);
        }
        this.activeStack = frameStackManager;
        for (Frame frame : frameStackManager.Y()) {
            if (!frame.n$src$Z$1fa61uz()) continue;
            frame.b$src$V$1f3kin7();
        }
    }

    public void setInputEnabled(boolean inputEnabled) {
        if (inputEnabled && !this.inputEnabled) {
            NativeBridge.ss_2("hide");
        }
        this.inputEnabled = inputEnabled;
    }

    /**
     * Closes the native menu and restores the game-side GUI state together.
     * The native menu can be dismissed by its own hotkey, which does not go
     * through Minecraft's key event path.
     */
    public void closeGui() {
        NativeBridge.ss_2("hide");
        Minecraft.gameSettings().P(this.savedChatKeyState);
        this.inputEnabled = true;
        if (this.isEnabled()) {
            this.setEnabled(false);
        }
        if (this.activeStack instanceof ClickGuiFrameManager) {
            ClickGuiFrameManager manager = (ClickGuiFrameManager)this.activeStack;
            manager.closeSidecar();
            manager.showLayer(vegas.sony.ui.click.frame.impl.main.ClickGuiLayer.MAIN);
        }
        if (this.activeStack != null && !this.activeStack.equals(sessionSpoofStack)) {
            Minecraft.F$src$V$aoypvc();
        }
    }

    public FrameStackManager getActiveStack() {
        return this.activeStack;
    }

    public void onTick() {
        if (!this.initialized) {
            this.initialized = true;
            SonyVegas.INSTANCE.getPublicProfileSettings().guiStyle.addChangeListener(this::onStandaloneModeChanged);
            this.updateStandaloneState();
        }
        try {
            if (Minecraft.currentScreen().getObject() != null) {
                if (Minecraft.currentScreen().isInstance(MappedClasses.u5) && this.getBind().areBoundInputsDown() && this.inputEnabled) {
                    this.toggle();
                    this.switchFrameStack(sessionSpoofStack);
                } else if (this.inputEnabled || !this.getBind().areBoundInputsDown()) {
                    // empty if block
                }
                if (this.activeStack.equals(sessionSpoofStack) && !Minecraft.currentScreen().isInstance(MappedClasses.u5)) {
                    this.switchFrameStack(mainStack);
                }
            } else if (!this.inputEnabled
                    && !NativeBridge.isModuleMenuVisible()) {
                this.closeGui();
                return;
            }
            if (activeComponent != null && !activeComponent.V$src$Z$1xhop3l()) {
                activeComponent = null;
            }
            this.framesToUpdate.clear();
            this.framesToUpdate.addAll(allFrames);
            this.framesToUpdate.addAll(this.activeStack.Y());
            if (ClientSettings.INSTANCE.inputEnabled) {
                for (Frame frame : allFrames) {
                    if (!(frame instanceof HudSettingsFrameBase)) continue;
                    this.framesToUpdate.add(frame);
                }
            }
            for (Frame frame : this.framesToUpdate) {
                try {
                    frame.T$src$V$1wse0de();
                }
                catch (Exception exception) {
                    SonyVegas.debugLog((String)("" + frame.getName()));
                    SonyVegas.logThrowable((Throwable)exception);
                }
            }
        }
        catch (Exception exception) {
            SonyVegas.logThrowable((Throwable)exception);
        }
    }

    public void u(EventKeyPress eventKeyPress) {
        super.u(eventKeyPress);
        if (!this.inputEnabled) {
            if (eventKeyPress.getKey() == 27 && eventKeyPress.isDown()) {
                this.closeGui();
            }
            eventKeyPress.setCancelled(true);
        }
    }

    public static boolean isHudEditorStack() {
        return INSTANCE.getActiveStack().equals(hudEditorStack);
    }

    static {
        allFrames = new ArrayList<Frame>();
        allStacks = new ArrayList<FrameStackManager>();
        mainStack = new FrameStackManager();
        hudEditorStack = new FrameStackManager();
        publicProfilesStack = new FrameStackManager();
        clickGuiFrameManager = new ClickGuiFrameManager();
        hotbarRuleEditorStack = new FrameStackManager();
        sessionSpoofStack = new FrameStackManager();
        profileSnapshotStack = new FrameStackManager();
        inventoryCleanerStack = new FrameStackManager();
        UI_EXECUTOR = new ThreadBoundExecutor();
        positionedFrames = new HashSet();
        frameCache = new HashMap();
    }
}

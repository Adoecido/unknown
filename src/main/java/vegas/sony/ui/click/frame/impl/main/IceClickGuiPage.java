package vegas.sony.ui.click.frame.impl.main;

import vegas.sony.SonyVegas;
import vegas.sony.input.BindCaptureTask;
import vegas.sony.input.MouseInput;
import vegas.sony.module.Category;
import vegas.sony.module.Mod;
import vegas.sony.module.ModuleDisplayScope;
import vegas.sony.ui.click.GuiMouseEvent;
import vegas.sony.ui.click.MouseButton;
import vegas.sony.ui.font.SmoothFontRenderer;
import vegas.sony.unmap.ModeSelection;
import vegas.sony.utils.render.GuiRenderPrimitives;
import vegas.sony.utils.render.RenderUtils;
import vegas.sony.value.BooleanValue;
import vegas.sony.value.ModeValue;
import vegas.sony.value.NumberValue;
import vegas.sony.value.RandomValue;
import vegas.sony.value.Value;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Dark Java adaptation of the supplied ImGui layout. */
public final class IceClickGuiPage extends ClickGuiPanelBase {
    private static final Color BACKGROUND = new Color(19, 19, 19, 255);
    private static final Color SIDEBAR = new Color(19, 19, 19, 255);
    private static final Color ROW = new Color(27, 27, 27, 255);
    private static final Color LINE = new Color(48, 48, 48, 255);
    private static final Color TEXT = new Color(184, 184, 184, 255);
    private static final Color MUTED_TEXT = new Color(125, 125, 125, 255);
    private static final Color ACCENT = new Color(30, 138, 249, 255);
    private static final Color WHITE = new Color(255, 255, 255, 255);
    private static final Category[] CATEGORIES = new Category[]{
            Category.COMBAT, Category.RENDER, Category.NETWORK,
            Category.GLADIADOR, Category.MISC
    };

    /* Open tabs are remembered separately for every category and GUI reopening. */
    private static final Map<Category, List<Mod>> OPENED_MODULES = new LinkedHashMap<Category, List<Mod>>();
    private static final Map<Category, Mod> ACTIVE_MODULES = new IdentityHashMap<Category, Mod>();

    private static final double SIDEBAR_WIDTH = 108.0;
    private static final double ROW_WIDTH = 171.0;
    private static final double ROW_HEIGHT = 21.0;
    private static final double ROW_GAP = 4.0;
    private static final double CONTENT_PADDING = 12.0;
    private static final double SIDEBAR_X = 12.0;
    private static final double SIDEBAR_BUTTON_WIDTH = 84.0;
    private static final double SIDEBAR_BUTTON_HEIGHT = 20.0;
    private static final double TAB_HEIGHT = 22.0;

    private static final class ModuleTabLayout {
        private final Mod module;
        private final double x;
        private final double y;
        private final double width;

        private ModuleTabLayout(Mod module, double x, double y, double width) {
            this.module = module;
            this.x = x;
            this.y = y;
            this.width = width;
        }
    }

    private Category selectedCategory = Category.COMBAT;
    private Mod selectedModule;
    private boolean settingsView;
    private boolean dashboard;
    private Value<?, ?> draggingSlider;
    private double draggingSliderX;
    private boolean draggingMaximumEndpoint;
    private double settingsScroll;
    private double settingsScrollMaximum;
    private volatile BindCaptureTask bindCaptureTask;
    private volatile Mod bindCaptureModule;

    public IceClickGuiPage(double width, double height) {
        super(width, height);
        this.setShowDisabledOverlay(false);
        this.setShadowEnabled(false);
        this.setAcceptsMouseInput(true);
        this.N(false);
        this.restoreCategoryState();
    }

    @Override
    public void H() {
        this.updateDraggingSlider();
        double x = this.G$src$D$1b2f02a();
        double y = this.n();
        double width = this.A();
        double height = this.L();
        double contentX = x + SIDEBAR_WIDTH;
        double contentWidth = width - SIDEBAR_WIDTH;

        GuiRenderPrimitives.C(x, y, width, height, BACKGROUND);
        GuiRenderPrimitives.C(x, y, SIDEBAR_WIDTH, height, SIDEBAR);
        GuiRenderPrimitives.C(contentX, y, 0.5, height, LINE);

        SmoothFontRenderer font = this.getFontRenderer(0.82);
        SmoothFontRenderer small = this.getFontRenderer(0.66);
        SmoothFontRenderer bold = this.getAlternateFontRenderer(0.84);

        small.d("FEATURES", x + SIDEBAR_X, y + 9.0, MUTED_TEXT);
        for (int i = 0; i < CATEGORIES.length; ++i) {
            this.drawCategory(font, x + SIDEBAR_X, y + 24.0 + i * 23.0,
                    CATEGORIES[i], !this.dashboard && CATEGORIES[i] == this.selectedCategory);
        }
        small.d("INFORMATION", x + SIDEBAR_X, y + 190.0, MUTED_TEXT);
        this.drawSidebarButton(font, x + SIDEBAR_X, y + 205.0,
                "Dashboard", this.dashboard, "gui");

        double tabsX = contentX + CONTENT_PADDING;
        double tabsY = y + 7.0;
        if (this.dashboard) {
            this.drawSubTab(font, tabsX, tabsY, "DASHBOARD", true, 66.0);
            GuiRenderPrimitives.C(tabsX, y + 29.0, contentWidth - CONTENT_PADDING * 2.0, 0.5, LINE);
            this.drawDashboard(bold, font, tabsX, y + 35.0, contentWidth - CONTENT_PADDING * 2.0);
            return;
        }

        double modulesTabWidth = this.tabWidth(font, "MODULES");
        this.drawSubTab(font, tabsX, tabsY, "MODULES", !this.settingsView, modulesTabWidth);
        List<ModuleTabLayout> tabLayouts = this.layoutModuleTabs(font, tabsX, tabsY,
                x + width - CONTENT_PADDING, modulesTabWidth);
        for (ModuleTabLayout tab : tabLayouts) {
            this.drawSubTab(font, tab.x, tab.y, this.plainModuleName(tab.module).toUpperCase(),
                    this.settingsView && tab.module == this.selectedModule, tab.width);
        }
        double bodyY = this.getBodyY(y, tabsY, tabLayouts);
        GuiRenderPrimitives.C(tabsX, bodyY - 5.0, contentWidth - CONTENT_PADDING * 2.0, 0.5, LINE);

        if (this.settingsView && this.selectedModule != null) {
            this.updateSettingsScroll(bodyY);
            this.drawValues(font, tabsX, bodyY);
        } else {
            this.settingsScroll = 0.0;
            this.settingsScrollMaximum = 0.0;
            this.drawModules(font, tabsX, bodyY);
        }
    }

    private void drawDashboard(SmoothFontRenderer bold, SmoothFontRenderer font,
                               double x, double y, double width) {
        this.drawBox(x, y, width, 48.0);
        bold.d("Ice ClickGui", x + 8.0, y + 8.0, WHITE);
        font.d("Java adaptation of the supplied ImGui menu", x + 8.0, y + 25.0, TEXT);
        font.d("Use Render > ClickGui to switch between Vape and Ice", x + 8.0, y + 36.0, MUTED_TEXT);
    }

    private void drawModules(SmoothFontRenderer font, double startX, double startY) {
        List<Mod> modules = this.getVisibleModules();
        for (int i = 0; i < modules.size() && i < 24; ++i) {
            Mod module = modules.get(i);
            double rowX = startX + (i % 2) * (ROW_WIDTH + 5.0);
            double rowY = startY + (i / 2) * (ROW_HEIGHT + ROW_GAP);
            this.drawBox(rowX, rowY, ROW_WIDTH, ROW_HEIGHT);
            font.d(this.fit(font, this.plainModuleName(module), 121.0), rowX + 7.0,
                    this.centerTextY(font, rowY, ROW_HEIGHT), TEXT);
            double dividerX = rowX + ROW_WIDTH - 36.0;
            GuiRenderPrimitives.C(dividerX, rowY + 5.0, 0.5, 11.0, LINE);
            String bindText = this.getBindText(font, module);
            font.d(bindText, dividerX - 6.0 - font.N(bindText),
                    this.centerTextY(font, rowY, ROW_HEIGHT),
                    this.bindCaptureModule == module ? ACCENT : MUTED_TEXT);
            String state = module.isEnabled() ? "ON" : "OFF";
            font.d(state, rowX + ROW_WIDTH - 28.0,
                    this.centerTextY(font, rowY, ROW_HEIGHT), module.isEnabled() ? ACCENT : MUTED_TEXT);
        }
    }

    private void drawValues(SmoothFontRenderer font, double startX, double startY) {
        List<Value<?, ?>> values = this.getVisibleValues();
        if (values.isEmpty()) {
            font.d("No settings", startX + 7.0, startY + 7.0, MUTED_TEXT);
            return;
        }
        double viewportHeight = Math.max(0.0, this.n() + this.L() - 4.0 - startY);
        RenderUtils.m(startX, startY, this.A() - SIDEBAR_WIDTH - CONTENT_PADDING * 2.0, viewportHeight);
        for (int i = 0; i < values.size() && i < 24; ++i) {
            Value<?, ?> value = values.get(i);
            double rowX = startX + (i % 2) * (ROW_WIDTH + 5.0);
            double rowY = startY + (i / 2) * (ROW_HEIGHT + ROW_GAP) - this.settingsScroll;
            this.drawBox(rowX, rowY, ROW_WIDTH, ROW_HEIGHT);
            String display = this.valueDisplay(value);
            double displayWidth = font.N(display);
            double dividerX = Math.max(rowX + 100.0, rowX + ROW_WIDTH - displayWidth - 13.0);
            GuiRenderPrimitives.C(dividerX, rowY + 5.0, 0.5, 11.0, LINE);
            font.d(this.fit(font, value.getName(), dividerX - rowX - 13.0), rowX + 7.0,
                    this.centerTextY(font, rowY, ROW_HEIGHT), TEXT);
            font.d(display, rowX + ROW_WIDTH - 7.0 - displayWidth,
                    this.centerTextY(font, rowY, ROW_HEIGHT),
                    value instanceof BooleanValue && ((BooleanValue)value).isEnabled() ? ACCENT : MUTED_TEXT);
            if (value instanceof NumberValue || value instanceof RandomValue) {
                GuiRenderPrimitives.C(rowX + 1.0, rowY + ROW_HEIGHT - 2.0,
                        ROW_WIDTH - 2.0, 1.5, LINE);
            }
            if (value instanceof NumberValue) {
                NumberValue number = (NumberValue)value;
                double range = number.getMaximum() - number.getMinimum();
                double progress = range <= 0.0 ? 0.0 : (number.getValue() - number.getMinimum()) / range;
                progress = Math.max(0.0, Math.min(1.0, progress));
                GuiRenderPrimitives.C(rowX + 1.0, rowY + ROW_HEIGHT - 2.0,
                        (ROW_WIDTH - 2.0) * progress, 1.5, ACCENT);
            } else if (value instanceof RandomValue) {
                RandomValue random = (RandomValue)value;
                double range = random.getAllowedMaximum() - random.getAllowedMinimum();
                double minimumProgress = range <= 0.0 ? 0.0
                        : (random.getMinimumValue() - random.getAllowedMinimum()) / range;
                double maximumProgress = range <= 0.0 ? 0.0
                        : (random.getMaximumValue() - random.getAllowedMinimum()) / range;
                minimumProgress = Math.max(0.0, Math.min(1.0, minimumProgress));
                maximumProgress = Math.max(minimumProgress, Math.min(1.0, maximumProgress));
                double trackWidth = ROW_WIDTH - 2.0;
                GuiRenderPrimitives.C(rowX + 1.0 + trackWidth * minimumProgress,
                        rowY + ROW_HEIGHT - 2.0,
                        trackWidth * (maximumProgress - minimumProgress), 1.5, ACCENT);
            }
        }
        RenderUtils.T();
        if (this.settingsScrollMaximum > 0.0 && viewportHeight > 0.0) {
            double contentHeight = this.getSettingsContentHeight(values.size());
            double handleHeight = Math.max(12.0, viewportHeight * viewportHeight / contentHeight);
            double handleY = startY + (viewportHeight - handleHeight) * this.settingsScroll / this.settingsScrollMaximum;
            GuiRenderPrimitives.C(this.G$src$D$1b2f02a() + this.A() - 4.0, startY, 1.0, viewportHeight, LINE);
            GuiRenderPrimitives.C(this.G$src$D$1b2f02a() + this.A() - 4.0, handleY, 1.0, handleHeight, ACCENT);
        }
    }

    private void drawCategory(SmoothFontRenderer font, double x, double y,
                              Category category, boolean selected) {
        this.drawSidebarButton(font, x, y, category.getName(), selected, category.getIconKey());
    }

    private void drawSidebarButton(SmoothFontRenderer font, double x, double y,
                                   String label, boolean selected, String iconKey) {
        if (selected) {
            GuiRenderPrimitives.B(x, y, SIDEBAR_BUTTON_WIDTH, SIDEBAR_BUTTON_HEIGHT, ACCENT, 2.0f);
        }
        Color color = selected ? WHITE : TEXT;
        GuiRenderPrimitives.F(iconKey, x + 12.0, y + SIDEBAR_BUTTON_HEIGHT / 2.0, 11.0, 11.0, color);
        font.d(label, x + 25.0, this.centerTextY(font, y, SIDEBAR_BUTTON_HEIGHT), color);
    }

    private void drawSubTab(SmoothFontRenderer font, double x, double y,
                            String label, boolean selected, double width) {
        font.d(label, x + 6.0, y + 4.0, selected ? WHITE : MUTED_TEXT);
        if (selected) {
            GuiRenderPrimitives.C(x, y + 20.0, width, 2.0, ACCENT);
        }
    }

    private double tabWidth(SmoothFontRenderer font, String label) {
        return Math.max(56.0, font.N(label.toUpperCase()) + 14.0);
    }

    private List<ModuleTabLayout> layoutModuleTabs(SmoothFontRenderer font, double tabsX,
                                                    double tabsY, double right,
                                                    double modulesTabWidth) {
        ArrayList<ModuleTabLayout> layouts = new ArrayList<ModuleTabLayout>();
        double nextX = tabsX + modulesTabWidth + 3.0;
        double nextY = tabsY;
        for (Mod module : this.getOpenedModules()) {
            double width = this.tabWidth(font, this.plainModuleName(module));
            if (nextX + width > right) {
                nextX = tabsX;
                nextY += TAB_HEIGHT;
            }
            layouts.add(new ModuleTabLayout(module, nextX, nextY, width));
            nextX += width + 3.0;
        }
        return layouts;
    }

    private double getBodyY(double pageY, double tabsY, List<ModuleTabLayout> tabs) {
        double lastTabY = tabsY;
        for (ModuleTabLayout tab : tabs) {
            lastTabY = Math.max(lastTabY, tab.y);
        }
        return pageY + 36.0 + (lastTabY - tabsY);
    }

    private void drawBox(double x, double y, double width, double height) {
        GuiRenderPrimitives.B(x, y, width, height, ROW, 2.0f);
        GuiRenderPrimitives.P(x, y, width, height, LINE, 2.0f, 0.6f, 1.0f);
    }

    @Override
    public void g(GuiMouseEvent event) {
        if (event.getAction() != MouseButton.LEFT_CLICK && event.getAction() != MouseButton.RIGHT_CLICK) {
            return;
        }
        double x = this.G$src$D$1b2f02a();
        double y = this.n();
        double contentX = x + SIDEBAR_WIDTH;

        for (int i = 0; i < CATEGORIES.length; ++i) {
            if (this.hit(event, x + SIDEBAR_X, y + 24.0 + i * 23.0,
                    SIDEBAR_BUTTON_WIDTH, SIDEBAR_BUTTON_HEIGHT)) {
                this.selectedCategory = CATEGORIES[i];
                this.dashboard = false;
                this.draggingSlider = null;
                this.settingsScroll = 0.0;
                this.restoreCategoryState();
                return;
            }
        }
        if (this.hit(event, x + SIDEBAR_X, y + 205.0,
                SIDEBAR_BUTTON_WIDTH, SIDEBAR_BUTTON_HEIGHT)) {
            this.dashboard = true;
            this.settingsView = false;
            this.draggingSlider = null;
            this.settingsScroll = 0.0;
            return;
        }
        if (this.dashboard) {
            return;
        }

        SmoothFontRenderer font = this.getFontRenderer(0.82);
        double tabsX = contentX + CONTENT_PADDING;
        double tabsY = y + 7.0;
        double modulesTabWidth = this.tabWidth(font, "MODULES");
        if (this.hit(event, tabsX, tabsY, modulesTabWidth, TAB_HEIGHT)) {
            this.settingsView = false;
            this.draggingSlider = null;
            this.settingsScroll = 0.0;
            return;
        }
        List<ModuleTabLayout> tabLayouts = this.layoutModuleTabs(font, tabsX, tabsY,
                x + this.A() - CONTENT_PADDING, modulesTabWidth);
        for (ModuleTabLayout tab : tabLayouts) {
            if (this.hit(event, tab.x, tab.y, tab.width, TAB_HEIGHT)) {
                if (event.getAction() == MouseButton.RIGHT_CLICK) {
                    this.getOpenedModules().remove(tab.module);
                    if (this.selectedModule == tab.module) {
                        this.selectedModule = null;
                        this.settingsView = false;
                        this.draggingSlider = null;
                        this.settingsScroll = 0.0;
                        ACTIVE_MODULES.remove(this.selectedCategory);
                    }
                    return;
                }
                this.selectedModule = tab.module;
                ACTIVE_MODULES.put(this.selectedCategory, tab.module);
                this.settingsView = true;
                this.settingsScroll = 0.0;
                return;
            }
        }

        double bodyY = this.getBodyY(y, tabsY, tabLayouts);
        if (this.settingsView && this.selectedModule != null) {
            this.handleValueClick(event, tabsX, bodyY);
        } else {
            this.handleModuleClick(event, tabsX, bodyY);
        }
    }

    private void handleModuleClick(GuiMouseEvent event, double startX, double startY) {
        List<Mod> modules = this.getVisibleModules();
        for (int i = 0; i < modules.size() && i < 24; ++i) {
            double rowX = startX + (i % 2) * (ROW_WIDTH + 5.0);
            double rowY = startY + (i / 2) * (ROW_HEIGHT + ROW_GAP);
            if (!this.hit(event, rowX, rowY, ROW_WIDTH, ROW_HEIGHT)) {
                continue;
            }
            Mod module = modules.get(i);
            if (event.getAction() == MouseButton.RIGHT_CLICK) {
                this.startBindCapture(module);
                return;
            }
            boolean toggleArea = event.getX() >= rowX + ROW_WIDTH - 39.0;
            if (toggleArea || module.getValues().isEmpty()) {
                module.setEnabled(!module.isEnabled(), true);
            } else {
                List<Mod> opened = this.getOpenedModules();
                if (!opened.contains(module)) {
                    opened.add(module);
                }
                this.selectedModule = module;
                ACTIVE_MODULES.put(this.selectedCategory, module);
                this.settingsView = true;
                this.settingsScroll = 0.0;
            }
            return;
        }
    }

    private String getBindText(SmoothFontRenderer font, Mod module) {
        if (this.bindCaptureModule == module && this.bindCaptureTask != null) {
            return "PRESS";
        }
        String text = module.getBind().getBindText();
        return text == null || text.isEmpty() ? "BIND" : this.fit(font, text.toUpperCase(), 52.0);
    }

    private void startBindCapture(final Mod module) {
        if (module == null || BindCaptureTask.isAnyCaptureActive()) {
            return;
        }
        this.bindCaptureModule = module;
        this.bindCaptureTask = new BindCaptureTask(module.getBind()) {
            @Override
            public void onCaptureComplete() {
                bindCaptureTask = null;
                bindCaptureModule = null;
                SonyVegas.INSTANCE.saveAndStop();
            }
        };
        this.bindCaptureTask.run();
    }

    private void handleValueClick(GuiMouseEvent event, double startX, double startY) {
        List<Value<?, ?>> values = this.getVisibleValues();
        for (int i = 0; i < values.size() && i < 24; ++i) {
            double rowX = startX + (i % 2) * (ROW_WIDTH + 5.0);
            double rowY = startY + (i / 2) * (ROW_HEIGHT + ROW_GAP) - this.settingsScroll;
            if (!this.hit(event, rowX, rowY, ROW_WIDTH, ROW_HEIGHT)) {
                continue;
            }
            Value<?, ?> value = values.get(i);
            if (value instanceof BooleanValue) {
                ((BooleanValue)value).toggleIfValid();
            } else if (value instanceof ModeValue) {
                ModeValue mode = (ModeValue)value;
                ModeSelection[] modes = mode.getModes();
                if (modes.length > 0) {
                    int delta = event.getAction() == MouseButton.RIGHT_CLICK ? -1 : 1;
                    int next = (mode.getSelectedIndex() + delta + modes.length) % modes.length;
                    mode.setSelectedIndex(next);
                }
            } else if (value instanceof NumberValue) {
                if (event.getAction() == MouseButton.LEFT_CLICK) {
                    this.draggingSlider = value;
                    this.draggingSliderX = rowX;
                    this.setDraggingSliderValue(event.getX());
                }
            } else if (value instanceof RandomValue) {
                if (event.getAction() == MouseButton.LEFT_CLICK) {
                    RandomValue random = (RandomValue)value;
                    double range = random.getAllowedMaximum() - random.getAllowedMinimum();
                    double clicked = random.getAllowedMinimum()
                            + range * this.sliderProgress(event.getX(), rowX);
                    this.draggingMaximumEndpoint = Math.abs(clicked - random.getMaximumValue())
                            <= Math.abs(clicked - random.getMinimumValue());
                    this.draggingSlider = value;
                    this.draggingSliderX = rowX;
                    this.setDraggingSliderValue(event.getX());
                }
            }
            return;
        }
    }

    private void updateDraggingSlider() {
        if (this.draggingSlider == null) {
            return;
        }
        if (!MouseInput.isButtonDown(MouseButton.LEFT_CLICK.ordinal())) {
            this.draggingSlider = null;
            return;
        }
        this.setDraggingSliderValue(RenderUtils.h().O);
    }

    private void updateSettingsScroll(double startY) {
        List<Value<?, ?>> values = this.getVisibleValues();
        double viewportHeight = Math.max(0.0, this.n() + this.L() - 4.0 - startY);
        this.settingsScrollMaximum = Math.max(0.0, this.getSettingsContentHeight(values.size()) - viewportHeight);
        if (this.settingsScroll > this.settingsScrollMaximum) {
            this.settingsScroll = this.settingsScrollMaximum;
        }
        if (this.settingsScrollMaximum <= 0.0) {
            this.settingsScroll = 0.0;
            return;
        }
        if (this.hit(RenderUtils.h().O, RenderUtils.h().H,
                this.G$src$D$1b2f02a() + SIDEBAR_WIDTH, startY,
                this.A() - SIDEBAR_WIDTH, viewportHeight)) {
            int scrollDelta = MouseInput.getScrollDelta();
            if (scrollDelta != 0) {
                this.settingsScroll = Math.max(0.0,
                        Math.min(this.settingsScrollMaximum, this.settingsScroll - scrollDelta / 15.0));
            }
        }
    }

    private double getSettingsContentHeight(int valueCount) {
        int rows = (valueCount + 1) / 2;
        return rows <= 0 ? 0.0 : rows * ROW_HEIGHT + Math.max(0, rows - 1) * ROW_GAP;
    }

    private void setDraggingSliderValue(double mouseX) {
        double progress = this.sliderProgress(mouseX, this.draggingSliderX);
        if (this.draggingSlider instanceof NumberValue) {
            NumberValue number = (NumberValue)this.draggingSlider;
            number.setValue(number.getMinimum()
                    + (number.getMaximum() - number.getMinimum()) * progress);
        } else if (this.draggingSlider instanceof RandomValue) {
            RandomValue random = (RandomValue)this.draggingSlider;
            double value = random.getAllowedMinimum()
                    + (random.getAllowedMaximum() - random.getAllowedMinimum()) * progress;
            if (this.draggingMaximumEndpoint) {
                random.setClampedMaximumValue(value);
            } else {
                random.setClampedMinimumValue(value);
            }
        }
    }

    private double sliderProgress(double mouseX, double rowX) {
        return Math.max(0.0, Math.min(1.0, (mouseX - rowX) / ROW_WIDTH));
    }

    private List<Mod> getVisibleModules() {
        ArrayList<Mod> modules = new ArrayList<Mod>();
        for (Mod module : SonyVegas.INSTANCE.getModManager().getTopLevelModules()) {
            if (module.getCategory() != this.selectedCategory || !module.isVisible()
                    || module.getModuleDisplayScope() == ModuleDisplayScope.FRAMES_ONLY) {
                continue;
            }
            modules.add(module);
        }
        modules.sort(Comparator.comparing(Mod::getName, String.CASE_INSENSITIVE_ORDER));
        return modules;
    }

    private List<Mod> getOpenedModules() {
        List<Mod> modules = OPENED_MODULES.get(this.selectedCategory);
        if (modules == null) {
            modules = new ArrayList<Mod>();
            OPENED_MODULES.put(this.selectedCategory, modules);
        }
        return modules;
    }

    private void restoreCategoryState() {
        this.selectedModule = ACTIVE_MODULES.get(this.selectedCategory);
        if (this.selectedModule != null && !this.getOpenedModules().contains(this.selectedModule)) {
            this.selectedModule = null;
            ACTIVE_MODULES.remove(this.selectedCategory);
        }
        this.settingsView = false;
    }

    private List<Value<?, ?>> getVisibleValues() {
        ArrayList<Value<?, ?>> values = new ArrayList<Value<?, ?>>();
        if (this.selectedModule == null) {
            return values;
        }
        for (Value<?, ?> value : this.selectedModule.getValues()) {
            if (value == null || value.isHidden() || !value.areConditionsMet()) {
                continue;
            }
            values.add(value);
        }
        return values;
    }

    private String valueDisplay(Value<?, ?> value) {
        if (value instanceof BooleanValue) {
            return ((BooleanValue)value).isEnabled() ? "ON" : "OFF";
        }
        String display = value.getDisplayValue();
        return display == null || display.isEmpty() ? "EDIT" : display.toUpperCase();
    }

    private boolean hit(GuiMouseEvent event, double x, double y, double width, double height) {
        return event.getX() >= x && event.getX() <= x + width
                && event.getY() >= y && event.getY() <= y + height;
    }

    private boolean hit(double mouseX, double mouseY, double x, double y, double width, double height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private double centerTextY(SmoothFontRenderer font, double y, double height) {
        return y + height / 2.0 - font.d("A") / 2.0;
    }

    private String plainModuleName(Mod module) {
        String text = module == null ? "" : module.getName();
        StringBuilder plain = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); ++i) {
            char character = text.charAt(i);
            if (character == '\u00a7' && i + 1 < text.length()) {
                ++i;
                continue;
            }
            if (character == '\u00c2' && i + 2 < text.length()
                    && text.charAt(i + 1) == '\u00a7') {
                i += 2;
                continue;
            }
            plain.append(character);
        }
        return plain.toString();
    }

    private String fit(SmoothFontRenderer font, String text, double width) {
        if (font.N(text) <= width) {
            return text;
        }
        String suffix = "...";
        int end = text.length();
        while (end > 0 && font.N(text.substring(0, end) + suffix) > width) {
            --end;
        }
        return text.substring(0, end) + suffix;
    }
}

package vegas.sony.module.render;

import vegas.sony.SonyVegas;
import vegas.sony.event.EventHandler;
import vegas.sony.event.impl.EventRender2D;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.module.Category;
import vegas.sony.module.Mod;
import vegas.sony.module.gladiador.HideClans;
import vegas.sony.ui.font.SmoothFontRenderer;
import vegas.sony.ui.hud.HudLayout;
import vegas.sony.ui.hud.HudLayoutModel;
import vegas.sony.unmap.ModeOption;
import vegas.sony.utils.render.BlurRegionRenderer;
import vegas.sony.utils.render.GuiRenderPrimitives;
import vegas.sony.utils.render.OpenGlBackendHolder;
import vegas.sony.value.BooleanValue;
import vegas.sony.value.ColorValue;
import vegas.sony.value.ModeValue;
import vegas.sony.value.NumberValue;
import vegas.sony.wrapper.impl.Minecraft;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Enabled-module list, a one-to-one port of the Clawd HUD arraylist.
 *
 * Each row is its own rounded pill inset from the top-right corner, a semibold
 * name over an optional muted tag, coloured by a primary-to-secondary blend that
 * either climbs the list as a sine wave or stands as a static gradient. Rows
 * slide in from the right and the rows beneath them close the gap by the same
 * fraction. Every geometry constant is Clawd's, measured in units of the base
 * font size so the proportions hold at any configured Size.
 */
public final class ArrayListModule extends Mod {
    /** Clawd's Fonts.BASE_SIZE: the font size one geometry unit is measured against. */
    private static final double BASE_SIZE = 9.0;
    /** Cap-height share of that size, Clawd's capRatio for Inter. */
    private static final double CAP_RATIO = 0.727;
    /**
     * Cap height of the base font, in geometry units. The renderers report a
     * height of size * 0.6 / gui scale, so deriving the unit from the
     * configured size instead of the measured one inflates every distance by
     * the gui scale and leaves the rows floating far apart from the glyphs.
     */
    private static final double BASE_CAP = BASE_SIZE * CAP_RATIO;
    /** On-screen name size at Size 100%, in the HUD's GUI space. */
    private static final double DEFAULT_FONT_PX = 14.0;
    private static final double TOP_MARGIN = 4.0;
    /** Rows sit flush against the screen edge, the way most client HUDs anchor them. */
    private static final double RIGHT_MARGIN = 0.0;
    private static final double PADDING = 3.0;
    private static final double ACCENT_GAP = 1.5;
    /** Corner mask bits: top-left | bottom-left, so only the inner edge is round. */
    private static final int LEADING_CORNERS = 9;
    /**
     * Blur strength, in texels of the capture. The capture is at twice the drawn
     * size, so this is half as many pixels on screen. It can be this wide
     * because the capture now spans the whole list: a row no longer blurs
     * against its own edges, it blurs against its neighbours.
     */
    private static final float BLUR_RADIUS = 40.0f;
    /** Width of the accent bar on the screen-edge side of a row. */
    private static final double ACCENT_WIDTH = 1.5;
    /** Tag size relative to the name, so the suffix reads as secondary. */
    private static final double TAG_SCALE = 0.85;
    /** How fast a row slides to a new slot when the order changes. */
    private static final double REORDER_RATE = 18.0;
    private static final double ROW = 13.0;
    private static final double GAP = 1.0;
    private static final double RADIUS = 3.0;
    private static final double SURFACE_ALPHA = 0.82;
    private static final int COMPACT_MAX = 8;
    private static final double COMPACT_SCALE = 1.75;
    /** Compact pulls twice as hard as Clawd's own curve, at every slider position. */
    private static final double COMPACT_BOOST = 2.0;

    private static final double WAVE_SPEED = 1.85;
    private static final double WAVE_STEP = 0.62;
    /** Seconds a row takes to slide fully in or out (Clawd's Motion.NORMAL). */
    private static final double SLIDE_DURATION = 0.24;
    private static final Color SURFACE = new Color(18, 24, 21);
    private static final Color TEXT_MUTED = new Color(151, 167, 159);

    private final ModeOption fontInter = new ModeOption("Inter //$ui");
    private final ModeOption fontPoppins = new ModeOption("Poppins //$ui");
    private final ModeOption fontRoboto = new ModeOption("Roboto //$ui");
    private final ModeOption fontJetBrains = new ModeOption("JetBrains //$ui");
    private final ModeOption fontRajdhani = new ModeOption("Rajdhani //$ui");
    private final ModeOption fontUbuntu = new ModeOption("Ubuntu //$ui");
    private final ModeOption fontPlay = new ModeOption("Play //$ui");
    private final ModeOption fontRaleway = new ModeOption("Raleway //$ui");
    private final ModeValue font = ModeValue.create(
            this, "Font //$ui", this.fontInter,
            this.fontInter, this.fontPoppins, this.fontRoboto, this.fontJetBrains,
            this.fontRajdhani, this.fontUbuntu, this.fontPlay, this.fontRaleway);
    private final NumberValue size = number("Size", "#", "%", 50.0, 100.0, 200.0, 1.0);
    private final NumberValue topOffset = number("Top //$ui", "#", "px", 0.0, TOP_MARGIN, 360.0, 1.0);
    private final NumberValue rightOffset = number("Right //$ui", "#", "px", 0.0, RIGHT_MARGIN, 640.0, 1.0);
    private final BooleanValue background = BooleanValue.create(
            this, "Background //$ui", true);
    private final BooleanValue blur = BooleanValue.create(
            this, "Blur //$ui", false);
    private final BooleanValue shadow = BooleanValue.create(
            this, "Shadow //$ui", false);
    private final NumberValue compact = number(
            "Compact", "#", "", 1.0, 1.0, (double) COMPACT_MAX, 1.0);
    private final NumberValue moduleSpacing = number(
            "Module spacing", "#", "%", 0.0, 100.0, 100.0, 1.0);
    private final BooleanValue lowercase = BooleanValue.create(
            this, "Lowercase //$ui", false);
    private final BooleanValue tags = BooleanValue.create(
            this, "Tags //$ui", true);
    private final BooleanValue wave = BooleanValue.create(
            this, "Wave //$ui", true);
    private final BooleanValue accent = BooleanValue.create(
            this, "Accent bar //$ui", true);
    private final ColorValue primaryColor = ColorValue.create(
            this, "Primary color //$ui", Color.WHITE);
    private final ColorValue secondaryColor = ColorValue.create(
            this, "Secondary color //$ui", new Color(34, 197, 94));

    private final Map<Mod, EntryState> states = new HashMap<Mod, EntryState>();
    private boolean accentOnLeft = false;
    /** One capture for the whole list, painted back only inside the rows. */
    private final BlurRegionRenderer blurRegion = new BlurRegionRenderer(0, 0);
    private long lastFrameNanos = System.nanoTime();
    private double clock;
    private static long lastDevLogNanos;

    private static void devLogThrottle(String message) {
        long now = System.nanoTime();
        if (now - lastDevLogNanos < 10000000000L) {
            return;
        }
        lastDevLogNanos = now;
        vegas.sony.runtime.CheatBreakerDebugLog.info(message);
    }

    public ArrayListModule() {
        super("ArrayList //$ui", 0xFF484E, Category.RENDER,
                "Enabled-module list in the Clawd HUD style //$ui");
        addValue(font, size, topOffset, rightOffset, background, blur, shadow, compact, moduleSpacing, lowercase, tags,
                wave, accent, primaryColor, secondaryColor);
        // Both pickers are read directly and blended by this module, so their own
        // rainbow/fade animation would fight the wave. Keep them plain.
        primaryColor.setRainbowSupported(false);
        secondaryColor.setRainbowSupported(false);
        primaryColor.setFadeSupported(false);
        secondaryColor.setFadeSupported(false);
        // Disable alpha/opacity slider in color picker for this module
        primaryColor.setAlphaSupported(false);
        secondaryColor.setAlphaSupported(false);
        // Secondary color only shows when Wave is enabled
        wave.addDependentValues(secondaryColor);
        size.setDescription("Overall list size //$ui");
        compact.setDescription("Tightens the row height and gap //$ui");
        moduleSpacing.setDescription("Controls the vertical space between modules //$ui");
        tags.setDescription("Shows the module suffix after its name //$ui");
        wave.setDescription("Sends the secondary colour climbing the list as a wave //$ui");
        accent.setDescription("Draws a coloured bar on the screen-edge side of each row //$ui");
        blur.setDescription("Blurs the world behind each row; costs a frame capture per row //$ui");
    }

    private NumberValue number(String name, String format, String suffix,
                               double minimum, double value, double maximum,
                               double increment) {
        return NumberValue.create(this, name, format, suffix,
                minimum, value, maximum, increment);
    }

    @Override
    public String getSimpleSuffix() {
        return "Clawd //$ui";
    }

    @EventHandler(priority = vegas.sony.event.EventPriority.LOWEST)
    public void onRender2D(EventRender2D event) {
        if (HudLayout.isEditing()) {
            devLogThrottle("HOOK_ARRAYLIST editing skip //$");
            return;
        }
        this.renderList(event, false);
    }

    public void renderLayoutPreview(EventRender2D event) {
        this.renderList(event, true);
    }

    private void renderList(EventRender2D event, boolean preview) {
        // GuiChat owns a temporary scaled matrix on legacy runtimes. Waiting for
        // the next regular HUD pass prevents text and backgrounds being scaled
        // independently while chat is open.
        if (!preview && Minecraft.currentScreen().isInstance(MappedClasses.qo)) {
            lastFrameNanos = System.nanoTime();
            return;
        }

        long now = System.nanoTime();
        double delta = Math.min(0.05,
                Math.max(0.001, (now - lastFrameNanos) / 1_000_000_000.0));
        lastFrameNanos = now;
        clock += delta;

        double guiScale = SonyVegas.INSTANCE.getClientSettings().getGuiScaleFactor();
        int logicalSize = Math.max(8,
                (int) Math.round(DEFAULT_FONT_PX * size.getValue() / 100.0));
        FontPair fonts = createFonts(logicalSize, guiScale);
        if (fonts == null) {
            devLogThrottle("HOOK_ARRAYLIST nofonts family=" + familyId() + " //$");
            return;
        }

        List<Entry> entries;
        if (preview) {
            entries = new ArrayList<Entry>();
            for (Mod module : SonyVegas.INSTANCE.getModManager().getTopLevelModules()) {
                if (shouldDisplay(module)) entries.add(createEntry(module, fonts));
            }
            if (entries.isEmpty()) {
                for (String name : new String[]{"ArrayList //$ui", "Target //$ui", "Notifications //$ui"})
                    entries.add(new Entry(this, name, "", fonts.name.width(name), 0.0));
            }
        } else {
            collectEntries();
            updateStates(delta);
            entries = collectAnimatedEntries(fonts);
        }
        if (entries.isEmpty()) return;
        sortEntries(entries);

        // EventRender2D draws in the client's GUI matrix; the physical display
        // width is halved once so the list stays pinned to the top-right corner.
        double right = event.getDisplayWidth() / 2.0;
        renderEntries(entries, fonts, right, event.getDisplayHeight() / 2.0, delta, preview);

        states.entrySet().removeIf(entry -> !entry.getValue().targetVisible
                && entry.getValue().progress <= 0.0005);
    }

    private FontPair createFonts(int logicalSize, double guiScale) {
        // The atlas is rasterised denser than it is drawn so the glyphs stay
        // sharp; TextFont divides that quality factor back out when drawing.
        double quality = Math.max(1.0, guiScale);
        int renderSize = Math.max(8, (int) Math.round(logicalSize * quality));
        String family = familyId();
        int tagSize = Math.max(8, (int) Math.round(logicalSize * TAG_SCALE));
        int tagRenderSize = Math.max(8, (int) Math.round(tagSize * quality));
        SmoothFontRenderer nameBase = hudFont(family, logicalSize, true);
        SmoothFontRenderer nameRender = hudFont(family, renderSize, true);
        SmoothFontRenderer detailBase = hudFont(family, tagSize, false);
        SmoothFontRenderer detailRender = hudFont(family, tagRenderSize, false);
        TextFont name = TextFont.create(nameRender, nameBase);
        TextFont detail = TextFont.create(detailRender, detailBase);
        return name == null || detail == null ? null : new FontPair(name, detail);
    }

    private SmoothFontRenderer hudFont(String family, int size, boolean semibold) {
        return SonyVegas.INSTANCE.getFontManager().getHudFont(family, size, semibold);
    }

    private String familyId() {
        Object selected = font.getValue();
        if (selected == fontPoppins) return "poppins";
        if (selected == fontRoboto) return "roboto";
        if (selected == fontJetBrains) return "jetbrains";
        if (selected == fontRajdhani) return "rajdhani";
        if (selected == fontUbuntu) return "ubuntu";
        if (selected == fontPlay) return "play";
        if (selected == fontRaleway) return "raleway";
        return "inter";
    }

    private void collectEntries() {
        Set<Mod> visibleModules = new HashSet<Mod>();
        for (Mod module : SonyVegas.INSTANCE.getModManager().getTopLevelModules()) {
            if (!shouldDisplay(module)) continue;
            visibleModules.add(module);
            EntryState state = states.get(module);
            if (state == null) {
                state = new EntryState();
                states.put(module, state);
            }
        }
        for (Map.Entry<Mod, EntryState> state : states.entrySet()) {
            state.getValue().targetVisible = visibleModules.contains(state.getKey());
        }
    }

    private List<Entry> collectAnimatedEntries(FontPair fonts) {
        List<Entry> entries = new ArrayList<Entry>();
        for (Map.Entry<Mod, EntryState> state : states.entrySet()) {
            if (state.getValue().progress <= 0.0005) continue;
            entries.add(createEntry(state.getKey(), fonts));
        }
        return entries;
    }

    private boolean shouldDisplay(Mod module) {
        if (module == this || !module.isEnabled() || !module.isVisible()
                || !module.q$src$Z$12h8h4c()
                || module.isHiddenFromArrayList()) {
            return false;
        }
        return true;
    }

    private Entry createEntry(Mod module, FontPair fonts) {
        String name = cased(clean(module.getName()), module);
        String detail = "";
        if (tags.getEffectiveValue()) {
            String suffix = clean(module.getSuffixForMode(1));
            if (!suffix.isEmpty()) {
                detail = " " + cased(suffix, module);
            }
        }
        double nameWidth = fonts.name.width(name);
        double detailWidth = detail.isEmpty() ? 0.0 : fonts.detail.width(detail);
        return new Entry(module, name, detail, nameWidth, detailWidth);
    }

    /**
     * Lower-casing is skipped for HideClans: its suffix is a scoreboard tag with
     * server-defined spelling and its own colour markers, which must stay intact.
     */
    private String cased(String value, Mod module) {
        return lowercase.getEffectiveValue() && !(module instanceof HideClans)
                ? value.toLowerCase(Locale.ROOT) : value;
    }

    private void updateStates(double delta) {
        double step = delta / SLIDE_DURATION;
        for (EntryState state : states.values()) {
            state.progress = clamp01(state.progress
                    + (state.targetVisible ? step : -step));
        }
    }

    private void sortEntries(List<Entry> entries) {
        entries.sort(Comparator.comparingDouble((Entry entry) -> entry.width).reversed());
    }

    private void renderEntries(List<Entry> entries, FontPair fonts,
                               double screenWidth, double screenHeight, double delta, boolean preview) {
        double unit = Math.max(0.01, fonts.name.height()) / BASE_CAP;
        double padding = PADDING * unit;
        double radius = RADIUS * unit;
        double accentWidth = ACCENT_WIDTH * unit;
        double accentGap = ACCENT_GAP * unit;
        boolean drawAccent = accent.getEffectiveValue();
        // The tight end is not a guessed constant: it is the height the renderer
        // itself reports for the text, so at full Compact the rows are exactly
        // the text box and stack with nothing between them. The slider travels
        // from Clawd's airy row to that, at twice the speed per step.
        double travel = Math.min(1.0, (compact.getValue() - 1.0)
                / (COMPACT_MAX - 1) * COMPACT_BOOST);
        double airyRow = ROW * unit;
        double tightRow = fonts.name.height();
        double compactRowHeight = airyRow + (tightRow - airyRow) * travel;
        double compactGap = GAP * unit
                / (compact.getValue() * COMPACT_SCALE * COMPACT_BOOST)
                * (1.0 - travel);
        double spacingFactor = clamp01(moduleSpacing.getValue() / 100.0);
        double rowHeight = tightRow + (compactRowHeight - tightRow) * spacingFactor;
        double gap = compactGap * spacingFactor;
        double maximumWidth = 1.0, listHeight = 0.0;
        for (Entry entry : entries) {
            entry.value = preview ? 1.0 : ease(states.get(entry.module).progress);
            entry.boxWidth = entry.width + padding
                    + (drawAccent ? accentWidth + accentGap : padding);
            maximumWidth = Math.max(maximumWidth, entry.boxWidth);
            listHeight += (rowHeight + gap) * entry.value;
        }
        HudLayoutModel.Bounds bounds = HudLayout.resolve(HudLayout.ARRAY_LIST,
                screenWidth, screenHeight, maximumWidth, Math.max(rowHeight, listHeight - gap),
                screenWidth - rightOffset.getValue() - maximumWidth, topOffset.getValue());
        double top = bounds.y;
        Color primary = awt(primaryColor);
        Color secondary = awt(secondaryColor);
        boolean drawShadow = shadow.getEffectiveValue();
        int total = entries.size();

        this.accentOnLeft = !bounds.right;

        // Where every row sits is settled first, so the blur can be taken in one
        // piece before any row has painted over the world it samples.
        for (Entry entry : entries) {
            EntryState state = states.get(entry.module);
            // The slot is where the row belongs now; the row eases towards it so
            // a width change reorders the list instead of snapping it.
            entry.boxY = preview ? top : slideTo(state, top, delta);
            entry.boxX = HudLayoutModel.rowX(bounds, entry.boxWidth, entry.value);
            top += (rowHeight + gap) * entry.value;
        }

        if (blur.getEffectiveValue()) {
            blurList(entries, rowHeight, radius);
        }

        int index = 0;
        for (Entry entry : entries) {
            Color entryColor = mix(primary, secondary, blend(index, total));
            if (background.getEffectiveValue()) {
                leadingRounded(entry.boxX, entry.boxY, entry.boxWidth, rowHeight,
                        withAlpha(SURFACE, entry.value * SURFACE_ALPHA), radius);
            }
            if (drawAccent) {
                double accentX = this.accentOnLeft ? entry.boxX : entry.boxX + entry.boxWidth - accentWidth;
                square(accentX, entry.boxY, accentWidth, rowHeight, withAlpha(entryColor, entry.value));
            }

            // Keep the same small gap beside the accent on either screen edge.
            double textX = entry.boxX + (this.accentOnLeft && drawAccent
                    ? accentWidth + accentGap : padding);
            double nameY = entry.boxY + (rowHeight - fonts.name.height()) / 2.0;
            fonts.name.draw(entry.name, textX, nameY,
                    withAlpha(entryColor, entry.value), drawShadow);

            if (!entry.detail.isEmpty()) {
                double detailY = entry.boxY + (rowHeight - fonts.detail.height()) / 2.0;
                fonts.detail.draw(entry.detail, textX + entry.nameWidth, detailY,
                        withAlpha(TEXT_MUTED, entry.value), drawShadow);
            }
            index++;
        }
    }

    /** Eases a row towards its slot, and drops it straight in on its first frame. */
    private double slideTo(EntryState state, double target, double delta) {
        if (!state.yInitialized) {
            state.y = target;
            state.yInitialized = true;
        } else {
            state.y += (target - state.y) * (1.0 - Math.exp(-REORDER_RATE * delta));
        }
        return state.y;
    }

    /**
     * Mix factor for one row. With Wave on it is a sine that advances one phase
     * step per entry, so the crest climbs the list; otherwise it is the row's
     * position, a static primary-to-secondary gradient down the list.
     */
    private double blend(int index, int total) {
        if (wave.getEffectiveValue()) {
            return 0.5 + 0.5 * Math.sin(clock * WAVE_SPEED + index * WAVE_STEP);
        }
        return total <= 1 ? 0.0 : (double) index / (total - 1);
    }

    private String clean(String text) {
        return text == null ? "" : text.replace("//$", "").trim();
    }

    /**
     * Row background: only the two corners away from the screen edge are
     * softened, so the list reads as one column anchored to the edge.
     */
    private void leadingRounded(double x, double y, double width, double height,
                                Color color, double radius) {
        if (color.getAlpha() <= 0 || width <= 0 || height <= 0) return;
        GuiRenderPrimitives.p(x, y, width, height, color, false,
                (float) Math.min(radius, Math.min(width, height) / 2.0),
                1.0f, 0.0f, Color.BLACK, this.accentOnLeft ? 6 : LEADING_CORNERS);
    }

    /**
     * Frosts the world behind the whole list in one pass, painted back only
     * inside the rows so the staircase is kept. A row that is still sliding in
     * is left out: the blur has no opacity of its own and would sit at full
     * strength under text that is still fading.
     *
     * BlurRegionRenderer only lines its capture up with its quads when the HUD
     * scale is exactly 1: it multiplies the coordinates by that scale to find
     * the framebuffer region, but divides the quads by it when drawing. At the
     * automatic 1.2 the two disagree and the list shows a slice of the screen
     * from somewhere else. Scaling the matrix by the same factor and dividing
     * the coordinates out puts both back on the same rectangle, and changes
     * nothing at all when the scale is 1.
     */
    private void blurList(List<Entry> entries, double rowHeight, double radius) {
        double hudScale = Math.max(0.01,
                SonyVegas.INSTANCE.getClientSettings().getGuiScaleFactor());
        double left = Double.MAX_VALUE;
        double right = -Double.MAX_VALUE;
        double top = Double.MAX_VALUE;
        double bottom = -Double.MAX_VALUE;
        int rows = 0;
        for (Entry entry : entries) {
            if (entry.value < 1.0) continue;
            ++rows;
            left = Math.min(left, entry.boxX);
            right = Math.max(right, entry.boxX + entry.boxWidth);
            top = Math.min(top, entry.boxY);
            bottom = Math.max(bottom, entry.boxY + rowHeight);
        }
        if (rows == 0) return;
        int regionWidth = (int) Math.round((right - left) * 2.0);
        int regionHeight = (int) Math.round((bottom - top) * 2.0);
        if (regionWidth <= 0 || regionHeight <= 0) return;

        double[] pieces = new double[rows * 4];
        int at = 0;
        for (Entry entry : entries) {
            if (entry.value < 1.0) continue;
            pieces[at++] = entry.boxX / hudScale;
            pieces[at++] = entry.boxY / hudScale;
            pieces[at++] = entry.boxWidth / hudScale;
            pieces[at++] = rowHeight / hudScale;
        }
        blurRegion.setDimensions(regionWidth, regionHeight);
        OpenGlBackendHolder.backend.pushMatrix();
        OpenGlBackendHolder.backend.scale(hudScale, hudScale, 1.0);
        try {
            blurRegion.renderBlurPieces((int) Math.round(left / hudScale),
                    (int) Math.round(top / hudScale), BLUR_RADIUS,
                    (float) (radius * 2.0), pieces);
        } finally {
            OpenGlBackendHolder.backend.popMatrix();
        }
    }

    private void square(double x, double y, double width, double height, Color color) {
        if (color.getAlpha() <= 0 || width <= 0 || height <= 0) return;
        GuiRenderPrimitives.e(x, y, width, height, color, false, 0.0f, 1.0f);
    }

    /** The configured primary colour, opaque, as chosen in the picker. */
    private Color awt(ColorValue value) {
        return new Color(value.toRgb() & 0xFFFFFF
                | value.getMutableColor().getAlpha() << 24, true);
    }

    /** Shared accent used by compact HUD elements such as notifications. */
    public Color getHudAccentColor() {
        return awt(primaryColor);
    }

    /** Font selection shared by the client-owned visual HUDs. */
    public SmoothFontRenderer getVisualFont(int size, boolean semibold) {
        return hudFont(familyId(), Math.max(8, size), semibold);
    }

    /** Secondary ArrayList colour shared by the visual HUDs. */
    public Color getHudSecondaryColor() {
        return awt(secondaryColor);
    }

    public void setLayoutPosition(double x, double y, double screenWidth) {
        topOffset.setValue(Math.max(0.0, Math.min(topOffset.getMaximum(), y)));
        // The active row width changes with enabled modules; keeping its right
        // edge near the drag point is less jumpy than trying to guess a width.
        rightOffset.setValue(Math.max(0.0, Math.min(rightOffset.getMaximum(),
                screenWidth - x)));
    }

    public double getLayoutX(double screenWidth) {
        return Math.max(0.0, screenWidth - rightOffset.getValue() - 145.0);
    }

    public double getLayoutY() {
        return topOffset.getValue();
    }

    private Color mix(Color from, Color to, double t) {
        t = clamp01(t);
        return new Color(
                channel(from.getRed(), to.getRed(), t),
                channel(from.getGreen(), to.getGreen(), t),
                channel(from.getBlue(), to.getBlue(), t),
                channel(from.getAlpha(), to.getAlpha(), t));
    }

    private int channel(int from, int to, double t) {
        return Math.max(0, Math.min(255, (int) Math.round(from + (to - from) * t)));
    }

    private Color withAlpha(Color color, double factor) {
        int alpha = (int) Math.round(color.getAlpha() * clamp01(factor));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(),
                Math.max(0, Math.min(255, alpha)));
    }

    private double ease(double value) {
        value = clamp01(value);
        return 1.0 - Math.pow(1.0 - value, 3.0);
    }

    private double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static final class Entry {
        private final Mod module;
        private final String name;
        private final String detail;
        private final double nameWidth;
        private final double width;
        /** Where this row landed this frame, filled in by the layout pass. */
        private double boxX;
        private double boxY;
        private double boxWidth;
        private double value;

        private Entry(Mod module, String name, String detail,
                      double nameWidth, double detailWidth) {
            this.module = module;
            this.name = name;
            this.detail = detail;
            this.nameWidth = nameWidth;
            this.width = nameWidth + detailWidth;
        }
    }

    private static final class EntryState {
        private double progress;
        private double y;
        private boolean yInitialized;
        private boolean targetVisible;
    }

    private static final class FontPair {
        private final TextFont name;
        private final TextFont detail;

        private FontPair(TextFont name, TextFont detail) {
            this.name = name;
            this.detail = detail;
        }
    }

    private static final class TextFont {
        private final SmoothFontRenderer renderer;
        private final double qualityScale;

        private TextFont(SmoothFontRenderer renderer, double qualityScale) {
            this.renderer = renderer;
            this.qualityScale = qualityScale;
        }

        private static TextFont create(SmoothFontRenderer renderer,
                                       SmoothFontRenderer baseRenderer) {
            if (renderer == null || baseRenderer == null || baseRenderer.M() <= 0) {
                return null;
            }
            return new TextFont(renderer,
                    Math.max(0.01, (double) renderer.M() / baseRenderer.M()));
        }

        private double width(String text) {
            return renderer.N(text) / qualityScale;
        }

        private double height() {
            return renderer.d("Ag") / qualityScale;
        }

        private void draw(String text, double x, double y, Color color,
                          boolean shadow) {
            OpenGlBackendHolder.backend.pushMatrix();
            OpenGlBackendHolder.backend.scale(
                    1.0 / qualityScale, 1.0 / qualityScale, 1.0);
            try {
                double scaledX = x * qualityScale;
                double scaledY = y * qualityScale;
                if (shadow) {
                    renderer.T(text, scaledX, scaledY, color,
                            new Color(0, 0, 0, Math.min(170, color.getAlpha())));
                } else {
                    renderer.d(text, scaledX, scaledY, color);
                }
            } finally {
                OpenGlBackendHolder.backend.popMatrix();
            }
        }
    }
}

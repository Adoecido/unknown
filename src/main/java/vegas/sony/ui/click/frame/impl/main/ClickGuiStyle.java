package vegas.sony.ui.click.frame.impl.main;

import vegas.sony.unmap.ModeOption;

/**
 * Visual presets for the central ClickGui.  Vape keeps the recovered layout,
 * while Ice mirrors the proportions of the supplied ImGui reference.
 */
public enum ClickGuiStyle {
    VAPE("Vape", 400.0, 240.0, 40.0, 136.0, 120.0, 104.0, 0.0),
    ICE("Ice", 480.0, 330.0, 0.0, 108.0, 108.0, 108.0, 0.0);

    private final String displayName;
    private final double frameWidth;
    private final double frameHeight;
    private final double headerHeight;
    private final double frameOverlayWidth;
    private final double contentOverlayWidth;
    private final double sidebarWidth;
    private final double pageInset;

    ClickGuiStyle(String displayName, double frameWidth, double frameHeight,
                  double headerHeight, double frameOverlayWidth,
                  double contentOverlayWidth, double sidebarWidth,
                  double pageInset) {
        this.displayName = displayName;
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.headerHeight = headerHeight;
        this.frameOverlayWidth = frameOverlayWidth;
        this.contentOverlayWidth = contentOverlayWidth;
        this.sidebarWidth = sidebarWidth;
        this.pageInset = pageInset;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public double getFrameWidth() {
        return this.frameWidth;
    }

    public double getFrameHeight() {
        return this.frameHeight;
    }

    public double getHeaderHeight() {
        return this.headerHeight;
    }

    public double getFrameOverlayWidth() {
        return this.frameOverlayWidth;
    }

    public double getContentOverlayWidth() {
        return this.contentOverlayWidth;
    }

    public double getSidebarWidth() {
        return this.sidebarWidth;
    }

    public double getPageInset() {
        return this.pageInset;
    }

    public static ClickGuiStyle fromMode(ModeOption mode) {
        return mode != null && "Ice".equalsIgnoreCase(mode.getName()) ? ICE : VAPE;
    }
}

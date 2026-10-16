package vegas.sony.ui.click.frame.impl.main;

import vegas.sony.SonyVegas;
import vegas.sony.module.none.ClientSettings;
import vegas.sony.ui.click.frame.Frame;
import vegas.sony.ui.click.frame.FrameStackManager;
import vegas.sony.ui.click.frame.impl.hud.HudModuleConfigFrameBase;
import vegas.sony.ui.click.frame.impl.hud.HudModuleFrameBase;
import vegas.sony.ui.click.frame.impl.main.ClickGuiLayer;
import vegas.sony.ui.click.frame.impl.main.ClickGuiMainFrame;
import vegas.sony.wrapper.impl.Minecraft;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

public class ClickGuiFrameManager
extends FrameStackManager {
    private Frame sidecarFrame;
    private ClickGuiStyle style = ClickGuiStyle.VAPE;
    private final Set<HudModuleFrameBase> mainLayerHudFrames = new HashSet<HudModuleFrameBase>();
    private ClickGuiMainFrame mainFrame;

    private void clearMainLayerHudFrames() {
        if (this.mainLayerHudFrames.isEmpty()) {
            return;
        }
        for (HudModuleFrameBase hudModuleFrameBase : this.mainLayerHudFrames) {
            hudModuleFrameBase.setFrontmostOverlay(false);
            this.m(hudModuleFrameBase);
        }
        this.mainLayerHudFrames.clear();
    }


    public Frame getSidecarFrame() {
        return this.sidecarFrame;
    }

    public void setSidecarFrame(@Nullable Frame frame) {
        if (frame != null) {
            if (this.sidecarFrame != null && this.sidecarFrame != frame) {
                this.m(this.sidecarFrame);
            }
            this.sidecarFrame = frame;
            if (!this.Y().contains(frame)) {
                this.q(frame);
            }
            this.R(this.mainFrame, frame);
            frame.t(true, false);
            this.mainFrame.getTransitionOverlay().setActive(true);
        } else {
            if (this.sidecarFrame != null) {
                this.m(this.sidecarFrame);
            }
            this.sidecarFrame = null;
            if (this.mainFrame != null) {
                this.mainFrame.getTransitionOverlay().setActive(false);
            }
        }
    }

    public ClickGuiStyle getStyle() {
        return this.style;
    }

    /** Rebuilds the central frame on the next render after a style change. */
    public void setStyle(ClickGuiStyle style) {
        ClickGuiStyle nextStyle = style == null ? ClickGuiStyle.VAPE : style;
        if (this.style == nextStyle) {
            return;
        }
        this.clearMainLayerHudFrames();
        this.closeSidecar();
        if (this.mainFrame != null) {
            this.m(this.mainFrame);
        }
        this.mainFrame = null;
        this.style = nextStyle;
    }

    @Override
    public void A() {
        if (this.mainFrame == null) {
            this.mainFrame = this.style == ClickGuiStyle.ICE ? new IceClickGui() : new VapeClickGui();
            this.q(this.mainFrame);
            if (this.style == ClickGuiStyle.ICE) {
                this.mainFrame.setVisible(true);
            } else {
                this.showLayer(ClickGuiLayer.MAIN);
            }
        }
        if (this.style == ClickGuiStyle.ICE) {
            double guiScale = SonyVegas.INSTANCE.getClientSettings().getGuiScaleFactor();
            double viewportWidth = (double)Minecraft.J() / guiScale;
            double viewportHeight = (double)Minecraft.h() / guiScale;
            this.mainFrame.K(Math.max(4.0, (viewportWidth - this.mainFrame.A()) / 2.0));
            this.mainFrame.S(Math.max(4.0, (viewportHeight - this.mainFrame.L()) / 2.0));
            this.clearMainLayerHudFrames();
            Iterator<Frame> frames = this.Y().iterator();
            while (frames.hasNext()) {
                if (frames.next() != this.mainFrame) {
                    frames.remove();
                }
            }
            this.sidecarFrame = null;
            return;
        }
        this.syncMainLayerHudFrames();
    }

    public ClickGuiMainFrame getMainFrame() {
        return this.mainFrame;
    }

    public void showLayer(ClickGuiLayer clickGuiLayer) {
        if (this.mainFrame == null) {
            return;
        }
        if (clickGuiLayer == ClickGuiLayer.MAIN) {
            this.mainFrame.setVisible(true);
            HudModuleConfigFrameBase.closeAllHudSettings();
        }
    }

    private void syncMainLayerHudFrames() {
        HashSet<HudModuleFrameBase> visibleFrames = new HashSet<HudModuleFrameBase>();
        for (Frame frame : ClientSettings.getAllFrames()) {
            HudModuleFrameBase hudModuleFrameBase;
            if (!(frame instanceof HudModuleFrameBase) || !(hudModuleFrameBase = (HudModuleFrameBase)frame).V$src$Z$1xhop3l()) continue;
            visibleFrames.add(hudModuleFrameBase);
            if (this.Y().contains(hudModuleFrameBase)) continue;
            int n = this.Y().indexOf(this.mainFrame);
            if (n >= 0) {
                this.Y().add(n, hudModuleFrameBase);
                continue;
            }
            this.q(hudModuleFrameBase);
        }
        Iterator<HudModuleFrameBase> iterator = this.mainLayerHudFrames.iterator();
        while (iterator.hasNext()) {
            HudModuleFrameBase frame = iterator.next();
            if (visibleFrames.contains(frame) && frame.V$src$Z$1xhop3l()) continue;
            this.m(frame);
            iterator.remove();
        }
        this.mainLayerHudFrames.addAll(visibleFrames);
    }

    public void closeSidecar() {
        this.setSidecarFrame(null);
    }

    public boolean isMainLayerHudFrame(Frame frame) {
        return this.mainLayerHudFrames.contains(frame);
    }

}

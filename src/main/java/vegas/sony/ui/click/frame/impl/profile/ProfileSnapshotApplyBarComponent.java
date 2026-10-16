package vegas.sony.ui.click.frame.impl.profile;

import vegas.sony.SonyVegas;
import vegas.sony.config.Profile;
import vegas.sony.config.ProfileSnapshot;
import vegas.sony.module.none.ClientSettings;
import vegas.sony.ui.click.component.ConfirmationDialogComponent;
import vegas.sony.ui.click.component.FlowLayoutComponent;
import vegas.sony.ui.click.component.PanelComponent;
import vegas.sony.ui.click.component.gui.TextLabel;
import vegas.sony.ui.click.component.layout.PaddedComponent;
import vegas.sony.ui.click.frame.FrameStackManager;
import vegas.sony.ui.click.frame.impl.main.ClickGuiFrameManager;
import vegas.sony.ui.click.frame.impl.profile.ProfileSnapshotFrame;
import vegas.sony.ui.font.SmoothFontRenderer;
import org.jetbrains.annotations.Nullable;

public class ProfileSnapshotApplyBarComponent
extends FlowLayoutComponent {
    private final TextLabel actionLabel;
    @Nullable
    private FrameStackManager returnStack;
    private int affectedModuleCount;
    private float leftPadding = 6.0f;
    private boolean countAllModules;
    private ProfileSnapshot snapshot;

    public ProfileSnapshot getSnapshot() {
        return this.snapshot;
    }

    public void setSnapshot(ProfileSnapshot snapshot) {
        this.snapshot = snapshot;
        this.refreshModuleCount();
        this.actionLabel.setVisible(snapshot != null);
        this.l$src$V$1mibm4x();
    }

    @Nullable
    public FrameStackManager getReturnStack() {
        return this.returnStack;
    }

    private void refreshModuleCount() {
        if (this.snapshot != null) {
            this.affectedModuleCount = this.snapshot.getModules(this.countAllModules).size();
        }
    }

    private void handleResetConfirmed(ProfileSnapshotFrame snapshotFrame) {
        snapshotFrame.resetAllSettings();
        this.refreshModuleCount();
    }

    public void setCountAllModules(boolean countAllModules) {
        this.countAllModules = countAllModules;
        this.refreshModuleCount();
    }

    public ProfileSnapshotApplyBarComponent(ProfileSnapshot snapshot, double width, boolean resetAction) {
        super(110.0);
        this.setShowDisabledOverlay(false);
        PanelComponent panelComponent = new PanelComponent(width, 12.0);
        this.addChildren(new PaddedComponent(2.0, panelComponent));
        this.actionLabel = new TextLabel(resetAction ? "Reset all" : "edit all", 0.75, false);
        this.actionLabel.o(20.0);
        this.actionLabel.Y(14.0);
        panelComponent.h(this.actionLabel, "alignright");
        panelComponent.setShowDisabledOverlay(false);
        this.actionLabel.setTextColor(null);
        this.actionLabel.addClickListener(() -> {
            ProfileSnapshotFrame profileSnapshotFrame = ClientSettings.getFrame(ProfileSnapshotFrame.class);
            if (resetAction) {
                ConfirmationDialogComponent.showStandard(this.L$src$Lvegas_sony_ui_click_frame_Frame_$1djx6sa(), "Are you sure you want to reset all settings?", "Reset", "reset_circle", () -> this.handleResetConfirmed(profileSnapshotFrame));
            } else {
                Profile profile = SonyVegas.INSTANCE.getProfilesManager().getActiveProfileOrNull();
                Profile snapshotProfile = this.getSnapshot().getProfile();
                profile.captureCurrentState();
                if (snapshotProfile != null && snapshotProfile.equals(profile)) {
                    this.setSnapshot(snapshotProfile.createSnapshot(false));
                }
                profileSnapshotFrame.setSnapshot(this.getSnapshot());
                if (ClientSettings.INSTANCE.getActiveStack() instanceof ClickGuiFrameManager) {
                    ClickGuiFrameManager clickGuiFrameManager = (ClickGuiFrameManager)ClientSettings.INSTANCE.getActiveStack();
                    profileSnapshotFrame.setReturnStack(clickGuiFrameManager);
                    clickGuiFrameManager.setSidecarFrame(profileSnapshotFrame);
                } else {
                    profileSnapshotFrame.setReturnStack(this.getReturnStack());
                    ClientSettings.INSTANCE.switchFrameStack(ClientSettings.profileSnapshotStack);
                }
            }
        });
        this.setSnapshot(snapshot);
    }

    public void setLeftPadding(float leftPadding) {
        this.leftPadding = leftPadding;
    }

    @Override
    public void c() {
        super.c();
        SmoothFontRenderer smoothFontRenderer = this.getAlternateFontRenderer(0.8);
        SmoothFontRenderer smoothFontRenderer2 = this.getAlternateFontRenderer(0.8);
        double textY = this.actionLabel.getTextY() + 0.5;
        if (this.countAllModules) {
            smoothFontRenderer.d(this.affectedModuleCount + " ", this.G$src$D$1b2f02a() + (double)this.leftPadding, textY, ProfileSnapshotApplyBarComponent.J.A);
            smoothFontRenderer2.d("MODULES", this.G$src$D$1b2f02a() + (double)this.leftPadding + smoothFontRenderer.N(this.affectedModuleCount + " "), textY, ProfileSnapshotApplyBarComponent.J.h);
        } else {
            smoothFontRenderer.d(this.affectedModuleCount + " ", this.G$src$D$1b2f02a() + (double)this.leftPadding, textY, ProfileSnapshotApplyBarComponent.J.A);
            smoothFontRenderer2.d("AFFECTED MODULES", this.G$src$D$1b2f02a() + (double)this.leftPadding + smoothFontRenderer.N(this.affectedModuleCount + " "), textY, ProfileSnapshotApplyBarComponent.J.h);
        }
    }

    public void setReturnStack(@Nullable FrameStackManager returnStack) {
        this.returnStack = returnStack;
    }
}

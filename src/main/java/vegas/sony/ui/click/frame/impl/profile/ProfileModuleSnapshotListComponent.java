package vegas.sony.ui.click.frame.impl.profile;

import vegas.sony.SonyVegas;
import vegas.sony.config.Profile;
import vegas.sony.config.ProfileModuleSnapshot;
import vegas.sony.config.ProfileSnapshot;
import vegas.sony.ui.click.component.FlowLayoutComponent;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.ui.click.component.PanelComponent;
import vegas.sony.ui.click.frame.FrameScrollbarPlacement;
import vegas.sony.ui.click.frame.impl.profile.ProfileModuleSnapshotListStyle;
import vegas.sony.ui.click.frame.impl.profile.ProfileModuleSnapshotRowComponent;
import vegas.sony.ui.click.frame.impl.profile.ProfileSnapshotApplyBarComponent;

public class ProfileModuleSnapshotListComponent
extends FlowLayoutComponent {
    private Profile profile;
    private long lastProfileRevision = -1L;
    private final ProfileSnapshotApplyBarComponent applyBar;
    private final PanelComponent moduleList;
    private ProfileModuleSnapshotListStyle style = ProfileModuleSnapshotListStyle.LEGACY;

    public ProfileModuleSnapshotListStyle getStyle() {
        return this.style;
    }

    public ProfileModuleSnapshotListComponent(Profile profile, double width, double listHeight) {
        super(width);
        this.profile = profile;
        this.setShowDisabledOverlay(false);
        this.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().M("wrap");
        this.moduleList = new PanelComponent(width, listHeight);
        this.moduleList.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().M("wrap");
        this.moduleList.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().M(false);
        this.moduleList.t(this.moduleList.L());
        this.moduleList.setShowDisabledOverlay(false);
        this.moduleList.F(FrameScrollbarPlacement.OUTSIDE);
        this.applyBar = new ProfileSnapshotApplyBarComponent(null, width, false);
        this.h(this.applyBar, new Object[0]);
        this.h(this.moduleList, new Object[0]);
        this.rebuildRows();
        this.lastProfileRevision = profile.getUpdatedAt();
    }

    private void applyStyleToRows() {
        for (GuiComponent guiComponent : this.moduleList.f()) {
            if (!(guiComponent instanceof ProfileModuleSnapshotRowComponent)) continue;
            this.applyStyle((ProfileModuleSnapshotRowComponent)guiComponent);
        }
    }

    private void applyStyle(ProfileModuleSnapshotRowComponent moduleRow) {
        if (this.style == ProfileModuleSnapshotListStyle.MODERN) {
            moduleRow.setColors(ProfileModuleSnapshotListComponent.J.S, ProfileModuleSnapshotListComponent.J.l, ProfileModuleSnapshotListComponent.J.Z, ProfileModuleSnapshotListComponent.J.A);
        } else {
            moduleRow.setColors(ProfileModuleSnapshotListComponent.J.m, ProfileModuleSnapshotListComponent.J.l, ProfileModuleSnapshotListComponent.J.Z, ProfileModuleSnapshotListComponent.J.A);
        }
    }

    public void setProfile(Profile profile) {
        if (profile == null) {
            return;
        }
        this.profile = profile;
        this.rebuildRows();
        this.lastProfileRevision = profile.getUpdatedAt();
    }

    private void rebuildRows() {
        this.moduleList.removeMarkedChildren();
        ProfileSnapshot profileSnapshot = this.profile.createSnapshot(false);
        if (profileSnapshot != null) {
            for (ProfileModuleSnapshot profileModuleSnapshot : profileSnapshot.getSortedModules(false)) {
                ProfileModuleSnapshotRowComponent moduleRow = new ProfileModuleSnapshotRowComponent(this.A(), profileSnapshot, profileModuleSnapshot);
                this.applyStyle(moduleRow);
                this.moduleList.h(moduleRow, new Object[0]);
            }
            this.applyBar.setSnapshot(profileSnapshot);
        } else {
            this.applyBar.setSnapshot(null);
        }
    }

    public void setStyle(ProfileModuleSnapshotListStyle style) {
        this.style = style;
        this.applyStyleToRows();
    }

    @Override
    public void u() {
        if (!this.profile.equals(SonyVegas.INSTANCE.getProfilesManager().getActiveProfile())) {
            return;
        }
        long profileRevision = this.profile.getUpdatedAt();
        if (profileRevision != this.lastProfileRevision) {
            this.lastProfileRevision = profileRevision;
            this.rebuildRows();
        }
    }
}


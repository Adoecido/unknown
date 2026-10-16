package vegas.sony.ui.click.frame.impl.profile;

import vegas.sony.ui.click.component.GuiRefreshListener;
import vegas.sony.ui.click.frame.impl.main.ClickGuiProfilesPage;

public class ProfilesPageRefreshListener
implements GuiRefreshListener {
    final ClickGuiProfilesPage profilesPage;

    @Override
    public void onRefresh() {
        ClickGuiProfilesPage.updateCreateButtonState(this.profilesPage);
    }

    public ProfilesPageRefreshListener(ClickGuiProfilesPage kV) {
        this.profilesPage = kV;
    }
}

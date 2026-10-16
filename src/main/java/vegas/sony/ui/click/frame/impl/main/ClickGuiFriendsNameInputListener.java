package vegas.sony.ui.click.frame.impl.main;

import vegas.sony.module.none.ClientSettings;
import vegas.sony.ui.click.component.LabeledTextInputComponent;
import vegas.sony.ui.click.frame.impl.main.ClickGuiFriendsPage;

public class ClickGuiFriendsNameInputListener
extends LabeledTextInputComponent {
    final ClickGuiFriendsPage page;

    @Override
    public void setText(String text) {
        super.setText(text);
        ClickGuiFriendsPage.setSearchQuery(this.page, text);
        ClientSettings.UI_EXECUTOR.execute(this::refreshResults);
    }

    private void refreshResults() {
        ClickGuiFriendsPage.refreshSearchResults(this.page);
    }

    public ClickGuiFriendsNameInputListener(ClickGuiFriendsPage clickGuiFriendsPage, String string) {
        super(string);
        this.page = clickGuiFriendsPage;
    }
}

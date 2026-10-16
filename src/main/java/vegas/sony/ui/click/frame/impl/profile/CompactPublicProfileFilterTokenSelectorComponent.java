package vegas.sony.ui.click.frame.impl.profile;

import vegas.sony.SonyVegas;
import vegas.sony.config.LegacyPublicProfile;
import vegas.sony.notification.NotificationType;
import vegas.sony.ui.click.component.LabeledTextInputComponent;
import vegas.sony.ui.click.frame.impl.profile.PublicProfileFilterTokenComponent;
import vegas.sony.ui.click.frame.impl.profile.PublicProfileFilterTokenSelectorComponent;
import vegas.sony.value.FriendNameSuggestionProvider;

public class CompactPublicProfileFilterTokenSelectorComponent
extends PublicProfileFilterTokenSelectorComponent {
    public CompactPublicProfileFilterTokenSelectorComponent(String string, double d, double d2) {
        super(string, CompactPublicProfileFilterTokenSelectorComponent::noOp, d, d2, false, false);
        this.getInput().setSuggestionProvider(new FriendNameSuggestionProvider());
        this.getInput().setHorizontalInset(0.0);
        this.getInput().setLeftInset(0.0f);
        this.getInput().setRightInset(1.0f);
        this.getInput().setTextColor(CompactPublicProfileFilterTokenSelectorComponent.J.A);
        this.getInput().setPlaceholderColor(CompactPublicProfileFilterTokenSelectorComponent.J.Z);
        this.getInput().getActionButton().setVisible(false);
        this.getInput().setShowDisabledOverlay(false);
        this.getInput().setBackgroundVisible(false);
        this.getInput().addKeyTypedListener(this::handleTokenDelimiter);
        ((LabeledTextInputComponent)this.getInput()).getSearchIcon().setVisible(false);
    }

    private void handleTokenDelimiter(char c, int n) {
        boolean bl;
        boolean bl2 = bl = c == ',' || n == 13;
        if (bl) {
            String string;
            String string2 = this.getInput().getText().trim();
            if (c == ',') {
                string2 = string2.substring(0, string2.length() - 1);
                this.getInput().setText(string2);
                if (string2.isEmpty()) {
                    return;
                }
            }
            if ((string = LegacyPublicProfile.validateTag(string2 = LegacyPublicProfile.normalizeTag(string2))) != null) {
                SonyVegas.INSTANCE.getNotificationManager().show("Tag Error", string, NotificationType.WARNING, 5000L);
                return;
            }
            if (this.getTokens().size() >= 5) {
                SonyVegas.INSTANCE.getNotificationManager().show("Tag Error", "You can only add up to 5 tags", NotificationType.WARNING, 5000L);
                return;
            }
            this.getInput().setText("");
            this.addToken(new PublicProfileFilterTokenComponent(string2));
        }
    }

    private static void noOp() {
    }

}

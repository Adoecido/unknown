package vegas.sony.ui.click.frame.impl.online;

import vegas.sony.SonyVegas;
import vegas.sony.friend.ui.UsernameEditorPanel;
import vegas.sony.notification.NotificationType;
import vegas.sony.ui.click.component.input.DebouncedTextInputComponent;
import vegas.sony.ui.click.frame.impl.online.OnlineAccountSettingsPageComponent;

public class OnlineAccountSettingsTextInputComponent
extends DebouncedTextInputComponent {
    final OnlineAccountSettingsPageComponent settingsPage;

    private static void lambda$enterEvent$1(String string) {
        SonyVegas.INSTANCE.getNotificationManager().show("Username Change", string, NotificationType.WARNING, 5000L);
    }

    public OnlineAccountSettingsTextInputComponent(OnlineAccountSettingsPageComponent settingsPage, String text, long cooldownMillis) {
        super(text, cooldownMillis);
        this.settingsPage = settingsPage;
    }

    @Override
    public float getVerticalInset() {
        return 0.0f;
    }

    @Override
    public double x() {
        return 82.0;
    }

    @Override
    public float getLeftInset() {
        return 0.0f;
    }

    private void lambda$enterEvent$0(String string) {
        OnlineAccountSettingsPageComponent.H(this.settingsPage);
        SonyVegas.INSTANCE.getNotificationManager().show("Username Change", "Username changed to " + string, NotificationType.INFO, 5000L);
    }

    @Override
    public void handleSubmitCooldown() {
        SonyVegas.INSTANCE.getNotificationManager().show("Error", "You are on cooldown!", NotificationType.WARNING, 5000L);
    }

    @Override
    public void handleSubmitReady() {
        UsernameEditorPanel.submitNameChange(this.settingsPage.fe, this.getText(), this::lambda$enterEvent$0, OnlineAccountSettingsTextInputComponent::lambda$enterEvent$1);
    }

    @Override
    public double C() {
        return 12.0;
    }

    @Override
    public float getRightInset() {
        return super.getRightInset();
    }
}

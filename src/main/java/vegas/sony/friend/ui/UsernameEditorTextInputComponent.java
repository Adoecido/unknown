package vegas.sony.friend.ui;

import vegas.sony.friend.ui.OnlineFriendUiHelper;
import vegas.sony.friend.ui.UsernameEditorPanel;
import vegas.sony.ui.click.component.input.DebouncedTextInputComponent;
import vegas.sony.ui.notification.NotificationMessage;
import vegas.sony.ui.notification.NotificationType;

public class UsernameEditorTextInputComponent
extends DebouncedTextInputComponent {
    final UsernameEditorPanel editorPanel;

    @Override
    public float getVerticalInset() {
        return 0.0f;
    }

    private void handleNameChangeSuccess(String newName) {
        UsernameEditorPanel.toggleEditMode(this.editorPanel);
        OnlineFriendUiHelper.showNotification(new NotificationMessage(NotificationType.SUCCESS, "Name changed to " + newName));
    }

    @Override
    public float getRightInset() {
        return super.getRightInset();
    }

    @Override
    public double C() {
        return 16.0;
    }

    @Override
    public void handleSubmitReady() {
        UsernameEditorPanel.submitNameChange(UsernameEditorPanel.getRequestPending(this.editorPanel), this.getText(), this::handleNameChangeSuccess, UsernameEditorTextInputComponent::handleNameChangeError);
    }

    public UsernameEditorTextInputComponent(UsernameEditorPanel editorPanel, String text, long cooldownMillis) {
        super(text, cooldownMillis);
        this.editorPanel = editorPanel;
    }

    @Override
    public float getLeftInset() {
        return 0.0f;
    }

    @Override
    public void handleSubmitCooldown() {
        OnlineFriendUiHelper.showNotification(NotificationType.WARNING, "You are on cooldown!");
    }

    @Override
    public double x() {
        return 82.0;
    }

    private static void handleNameChangeError(String errorMessage) {
        OnlineFriendUiHelper.showNotification(new NotificationMessage(NotificationType.ERROR, errorMessage));
    }
}

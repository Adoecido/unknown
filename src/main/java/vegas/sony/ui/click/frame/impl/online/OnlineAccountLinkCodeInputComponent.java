package vegas.sony.ui.click.frame.impl.online;

import vegas.sony.friend.ui.OnlineFriendUiHelper;
import vegas.sony.ui.click.component.input.DebouncedTextInputComponent;
import vegas.sony.ui.click.frame.impl.online.OnlineAccountLinkCodePageComponent;
import vegas.sony.ui.notification.NotificationType;

class OnlineAccountLinkCodeInputComponent
extends DebouncedTextInputComponent {
    final OnlineAccountLinkCodePageComponent linkCodePage;
    private static final String lb = "You are on cooldown!";

    @Override
    public void handleSubmitCooldown() {
        OnlineFriendUiHelper.showNotification(NotificationType.WARNING, lb);
    }

    OnlineAccountLinkCodeInputComponent(OnlineAccountLinkCodePageComponent linkCodePage, String text, long cooldownMillis) {
        super(text, cooldownMillis);
        this.linkCodePage = linkCodePage;
    }

    @Override
    public double C() {
        return 24.0;
    }

    @Override
    public double x() {
        return 104.0;
    }

    @Override
    public void handleSubmitReady() {
        OnlineAccountLinkCodePageComponent.r(this.linkCodePage);
    }
}

package vegas.sony.friend.ui;

import vegas.sony.friend.ui.OnlinePlayerPreviewSettingsFrame;
import vegas.sony.ui.click.component.ColorDividerComponent;
import java.awt.Color;

public class OnlinePlayerPreviewDividerComponent
extends ColorDividerComponent {
    final OnlinePlayerPreviewSettingsFrame settingsFrame;

    @Override
    public void I() {
    }

    public OnlinePlayerPreviewDividerComponent(OnlinePlayerPreviewSettingsFrame onlinePlayerPreviewSettingsFrame, Color color) {
        super(color);
        this.settingsFrame = onlinePlayerPreviewSettingsFrame;
    }
}

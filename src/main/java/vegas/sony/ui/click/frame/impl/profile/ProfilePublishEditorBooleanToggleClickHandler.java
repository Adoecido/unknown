package vegas.sony.ui.click.frame.impl.profile;

import vegas.sony.ui.click.GuiMouseListener;
import vegas.sony.ui.click.MouseClickButton;
import vegas.sony.ui.click.frame.impl.profile.ProfilePublishEditorPanel;
import java.awt.Point;

public class ProfilePublishEditorBooleanToggleClickHandler
implements GuiMouseListener {
    private final ProfilePublishEditorPanel editorPanel;

    @Override
    public void g(Point point, MouseClickButton mouseClickButton) {
        this.editorPanel.toggleFriendsOnlyVisibility();
    }


    public ProfilePublishEditorBooleanToggleClickHandler(ProfilePublishEditorPanel profilePublishEditorPanel) {
        this.editorPanel = profilePublishEditorPanel;
    }
}


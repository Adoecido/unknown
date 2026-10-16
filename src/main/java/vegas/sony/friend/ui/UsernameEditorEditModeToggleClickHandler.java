package vegas.sony.friend.ui;

import vegas.sony.friend.ui.UsernameEditorPanel;
import vegas.sony.ui.click.component.GuiClickListener;

public class UsernameEditorEditModeToggleClickHandler
implements GuiClickListener {
    private final UsernameEditorPanel editorPanel;

    public UsernameEditorEditModeToggleClickHandler(UsernameEditorPanel editorPanel) {
        this.editorPanel = editorPanel;
    }

    @Override
    public void onPrimaryClick() {
        UsernameEditorPanel.toggleEditMode(this.editorPanel);
    }
}

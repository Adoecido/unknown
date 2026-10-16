package vegas.sony.friend.ui;

import vegas.sony.friend.ui.UsernameEditorPanel;
import vegas.sony.ui.click.component.GuiClickListener;

public class UsernameEditorEditButtonClickHandler
implements GuiClickListener {
    private final UsernameEditorPanel editorPanel;

    @Override
    public void onPrimaryClick() {
        UsernameEditorPanel.toggleEditMode(this.editorPanel);
    }

    public UsernameEditorEditButtonClickHandler(UsernameEditorPanel editorPanel) {
        this.editorPanel = editorPanel;
    }
}

package vegas.sony.friend.ui;

import vegas.sony.friend.ui.OnlineModeToggleComponent;
import vegas.sony.ui.click.component.GuiClickListener;

public class OnlineModeToggleClickHandler
implements GuiClickListener {
    private final OnlineModeToggleComponent toggle;

    @Override
    public void onPrimaryClick() {
        if (this.toggle.isLeftSelected().booleanValue()) {
            OnlineModeToggleComponent.toggleSelection(this.toggle);
        }
    }

    public OnlineModeToggleClickHandler(OnlineModeToggleComponent onlineModeToggleComponent) {
        this.toggle = onlineModeToggleComponent;
    }

}


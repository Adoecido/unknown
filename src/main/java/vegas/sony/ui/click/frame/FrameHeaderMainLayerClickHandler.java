package vegas.sony.ui.click.frame;

import vegas.sony.module.none.ClientSettings;
import vegas.sony.ui.click.component.GuiClickListener;

public class FrameHeaderMainLayerClickHandler
implements GuiClickListener {
    @Override
    public void onPrimaryClick() {
        ClientSettings.INSTANCE.switchFrameStack(ClientSettings.mainStack);
    }
}

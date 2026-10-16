package vegas.sony.ui.click.component;

import vegas.sony.SonyVegas;
import vegas.sony.module.none.ClientSettings;
import vegas.sony.ui.click.component.GuiKeyTypedListener;

public class GuiKeyTypedDispatcher {
    public static void dispatch(char character, int keyCode) {
        ClientSettings clientSettings = SonyVegas.INSTANCE.getModManager().getMod(ClientSettings.class);
        if (ClientSettings.activeComponent != null) {
            for (GuiKeyTypedListener keyTypedListener : ClientSettings.activeComponent.getKeyTypedListeners()) {
                keyTypedListener.onKeyTyped(character, keyCode);
            }
        }
    }
}

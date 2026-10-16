package vegas.sony.event.listener;

import vegas.sony.SonyVegas;
import vegas.sony.event.EventHandler;
import vegas.sony.event.EventListener;
import vegas.sony.event.impl.EventPreRenderTick;
import vegas.sony.event.impl.EventPreTick;
import vegas.sony.module.none.ClientSettings;
import vegas.sony.utils.render.RenderUtils;

public class ClientSettingsEventForwarder
implements EventListener {
    private ClientSettings clientSettings;


    @EventHandler
    public void onPreRenderTick(EventPreRenderTick eventPreRenderTick) {
        ClientSettings clientSettings = this.getClientSettings();
        if (clientSettings == null) {
            return;
        }
        RenderUtils.C();
        clientSettings.updateGuiScale();
    }

    private ClientSettings getClientSettings() {
        if (this.clientSettings == null) {
            this.clientSettings = SonyVegas.INSTANCE.getModManager().getMod(ClientSettings.class);
        }
        return this.clientSettings;
    }

    @EventHandler
    public void onTick(EventPreTick eventPreTick) {
        ClientSettings clientSettings = this.getClientSettings();
        if (clientSettings == null) {
            return;
        }
        clientSettings.onTick();
    }
}


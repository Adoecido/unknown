package vegas.sony.status;

import vegas.sony.SonyVegas;
import vegas.sony.module.Mod;
import vegas.sony.runtime.NativeBridge;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.utils.TimerUtil;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.ServerData;

public class NativePresenceUpdater {
    private final TimerUtil updateTimer = new TimerUtil();
    private static GuiComponent[] controlFlowMarker;
    private String lastServerDescription;
    private String lastClientDescription;

    public void updatePresence(boolean enabled) {
        if (!this.updateTimer.hasTimeElapsed(1000L)) {
            return;
        }
        this.updateTimer.reset();
        if (!enabled) {
            NativeBridge.updc(null, null);
            return;
        }
        ServerData serverData = Minecraft.H();
        String serverDescription = "Not in a server";
        if (serverData.isNotNull()) {
            serverDescription = "Playing legit on " + serverData.getServerIp();
        }
        String clientDescription = this.getEnabledModuleNames();
        if (clientDescription.length() >= 128) {
            clientDescription = clientDescription.substring(0, 128);
        }
        if (!serverDescription.equals(this.lastServerDescription)
                || !this.lastClientDescription.equals(clientDescription)) {
            NativeBridge.updc(serverDescription, clientDescription);
        }
        this.lastServerDescription = serverDescription;
        this.lastClientDescription = clientDescription;
    }

    private String getEnabledModuleNames() {
        StringBuilder enabledNames = new StringBuilder("  ");
        for (Mod mod : SonyVegas.INSTANCE.getModManager().collectMods()) {
            if (!mod.isEnabled() || mod.getGuiColor() == 0) {
                continue;
            }
            enabledNames.append(mod.getName()).append(", ");
        }
        if (enabledNames.length() > 2) {
            enabledNames.setLength(enabledNames.length() - 2);
        }
        return enabledNames.toString();
    }

    static {
        NativePresenceUpdater.setControlFlowMarker(new GuiComponent[5]);
    }


    public static void setControlFlowMarker(GuiComponent[] marker) {
        controlFlowMarker = marker;
    }

    public static GuiComponent[] getControlFlowMarker() {
        return controlFlowMarker;
    }
}


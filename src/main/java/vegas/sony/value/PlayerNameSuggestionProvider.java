package vegas.sony.value;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.wrapper.impl.EntityPlayer;
import vegas.sony.wrapper.impl.GameProfile;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.PlayerInfo;
import vegas.sony.wrapper.impl.WorldClient;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jetbrains.annotations.UnmodifiableView;

public class PlayerNameSuggestionProvider
extends AbstractListValueSuggestionProvider {

    @Override
    public @UnmodifiableView List<String> getValues() {
        Map<String, String> onlineNames = new LinkedHashMap<String, String>();
        try {
            // The Tab list is the server's complete online-player snapshot. It
            // also contains players outside the client's entity render range.
            if (Minecraft.N() != null && Minecraft.N().isNotNull()
                    && Minecraft.N().getPlayerInfoMap() != null) {
                for (Object playerInfoHandle : Minecraft.N().getPlayerInfoMap()) {
                    if (playerInfoHandle == null) continue;
                    try {
                        GameProfile profile = new PlayerInfo(playerInfoHandle).v();
                        if (profile != null && profile.isNotNull()) {
                            addName(onlineNames, profile.getName());
                        }
                    } catch (Throwable ignored) {
                        // A tab entry may disappear while the server refreshes it.
                    }
                }
            }
        } catch (Throwable ignored) {
            // Unsupported mappings fall back to the loaded world below.
        }

        try {
            WorldClient world = Minecraft.theWorld();
            if (world != null && world.isNotNull()) {
                for (Object entity : world.X()) {
                    if (entity == null || !MappedClasses.Yl.isInstance(entity)) continue;
                    try {
                        addName(onlineNames, new EntityPlayer(entity).getName());
                    } catch (Throwable ignored) {
                        // Entity snapshots may change while a menu state is built.
                    }
                }
            }
        } catch (Throwable ignored) {
            // Manual input must remain available even outside a server.
        }

        ArrayList<String> playerNames = new ArrayList<String>(onlineNames.values());
        playerNames.sort(String.CASE_INSENSITIVE_ORDER);
        return playerNames;
    }

    private static void addName(Map<String, String> names, String rawName) {
        if (rawName == null) return;
        String name = rawName.replaceAll("§.", "").trim();
        if (!name.matches("[A-Za-z0-9_]{1,16}")) return;
        names.putIfAbsent(name.toLowerCase(Locale.ROOT), name);
    }
}


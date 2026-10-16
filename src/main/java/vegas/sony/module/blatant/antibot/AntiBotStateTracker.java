package vegas.sony.module.blatant.antibot;

import vegas.sony.config.ClientSettings;
import vegas.sony.unmap.ModeOption;
import vegas.sony.wrapper.impl.EntityPlayer;
import vegas.sony.wrapper.impl.ForgeVersion;
import vegas.sony.wrapper.impl.GameProfile;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.PlayerInfo;
import vegas.sony.wrapper.impl.ScorePlayerTeam;
import java.util.Map;
import java.util.UUID;

public class AntiBotStateTracker {
    public static final char UNKNOWN_COLOR_CODE = '\u00ff';

    private final Map<ModeOption, Character> colorCodeByOption;

    public AntiBotStateTracker(Map<ModeOption, Character> colorCodeByOption) {
        this.colorCodeByOption = colorCodeByOption;
    }

    public char detectColorCode(EntityPlayer player) {
        char colorCode = this.getDisplayNameColorCode(player);
        if (colorCode == UNKNOWN_COLOR_CODE) {
            colorCode = this.getTeamColorCode(player);
        }
        return colorCode;
    }

    public ModeOption findOptionByColorCode(char colorCode) {
        for (ModeOption option : this.colorCodeByOption.keySet()) {
            if (this.colorCodeByOption.get(option) == colorCode) {
                return option;
            }
        }
        return null;
    }

    public char getDisplayNameColorCode(EntityPlayer player) {
        return this.findColorCodeBeforeName(player.getName(), player.Q().getFormattedText());
    }

    public char getTeamColorCode(EntityPlayer player) {
        if (ForgeVersion.MC_1_8_9.A()) {
            return UNKNOWN_COLOR_CODE;
        }
        UUID playerId = player.c$src$Lvegas_sony_wrapper_impl_GameProfile_$ir8937().getUUID();
        for (Object playerInfoObject : Minecraft.N().getPlayerInfoMap()) {
            PlayerInfo playerInfo = new PlayerInfo(playerInfoObject);
            GameProfile profile = playerInfo.v();
            ScorePlayerTeam team = playerInfo.X();
            if (!profile.getUUID().equals(playerId) || !team.isNotNull()) {
                continue;
            }
            String teamPrefix = team.getPrefix();
            if (!teamPrefix.contains(ClientSettings.FORMAT_CODE)) {
                continue;
            }
            for (int index = teamPrefix.length(); index > 0; --index) {
                String character = String.valueOf(teamPrefix.charAt(index - 1));
                if (character.equals(ClientSettings.FORMAT_CODE)) {
                    char colorCode = teamPrefix.charAt(index);
                    if (colorCode <= 'f') {
                        return colorCode;
                    }
                }
            }
        }
        return UNKNOWN_COLOR_CODE;
    }

    public char findColorCodeBeforeName(String playerName, String formattedName) {
        int nameIndex;
        if (formattedName.contains(ClientSettings.FORMAT_CODE)
                && (nameIndex = formattedName.indexOf(playerName)) > 0) {
            for (int index = nameIndex - 1; index >= 0; --index) {
                String character = String.valueOf(formattedName.charAt(index));
                if (character.equals(ClientSettings.FORMAT_CODE)) {
                    char colorCode = formattedName.charAt(index + 1);
                    if (colorCode <= 'f') {
                        return colorCode;
                    }
                }
            }
        }
        return UNKNOWN_COLOR_CODE;
    }

    public boolean matchesOption(ModeOption option, EntityPlayer player) {
        return this.colorCodeByOption.get(option) == this.detectColorCode(player);
    }
}

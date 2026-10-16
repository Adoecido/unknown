package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MScorePlayerTeam;

public class ScorePlayerTeam
extends Team {
    public static TextComponent formatNameForTeamText(Team team, ITextComponent playerName) {
        if (ForgeVersion.MC_1_16_5.v()) {
            throw new UnsupportedOperationException("This method is only for versions 1.16.5 and above");
        }
        return new TextComponent(MScorePlayerTeam.formatNameForTeam(ScorePlayerTeam.sonyVegasInstance.getMappings().scorePlayerTeam, team.getObject(), playerName.getObject()));
    }

    public static TextComponentBaseBridge formatNameForTeamMutable(Team team, ITextComponent playerName) {
        if (ForgeVersion.MC_1_20_6.v()) {
            throw new UnsupportedOperationException("This method is only for versions 1.16.5 and above");
        }
        return new TextComponentBaseBridge(MScorePlayerTeam.formatNameForTeam(ScorePlayerTeam.sonyVegasInstance.getMappings().scorePlayerTeam, team.getObject(), playerName.getObject()));
    }

    private static UnsupportedOperationException a(UnsupportedOperationException unsupportedOperationException) {
        return unsupportedOperationException;
    }

    public String getPrefix() {
        if (ForgeVersion.MC_1_20_6.d()) {
            ITextComponent playerPrefix = new ITextComponent(ScorePlayerTeam.sonyVegasInstance.getMappings().scorePlayerTeam.getPlayerPrefix(this.getObject()));
            if (playerPrefix.isNull()) {
                return "";
            }
            return playerPrefix.getFormattedText();
        }
        return MScorePlayerTeam.getPrefix(ScorePlayerTeam.sonyVegasInstance.getMappings().scorePlayerTeam, this.getObject());
    }

    public String getSuffix() {
        return MScorePlayerTeam.getSuffix(ScorePlayerTeam.sonyVegasInstance.getMappings().scorePlayerTeam, this.getObject());
    }

    public String getRegisteredName() {
        return MScorePlayerTeam.getRegisteredName(ScorePlayerTeam.sonyVegasInstance.getMappings().scorePlayerTeam, this.getObject());
    }

    public Object getChatFormat() {
        return MScorePlayerTeam.getChatFormat(ScorePlayerTeam.sonyVegasInstance.getMappings().scorePlayerTeam, this.getObject());
    }

    public ScorePlayerTeam(Object object) {
        super(object);
    }

    public TextFormatting getColor() {
        Object color = MScorePlayerTeam.getColor(ScorePlayerTeam.sonyVegasInstance.getMappings().scorePlayerTeam, this.getObject());
        if (color == null) {
            return null;
        }
        return new TextFormatting(color);
    }

    public static String formatPlayerName(Team team, String playerName) {
        if (ForgeVersion.MC_1_16_5.d()) {
            TextComponent textComponent = new TextComponent(ScorePlayerTeam.sonyVegasInstance.getMappings().scorePlayerTeam.formatPlayerNameComponent(team.getObject(), ScorePlayerTeamTextComponent.B(playerName).getObject()));
            return textComponent.U();
        }
        return MScorePlayerTeam.formatPlayerName(ScorePlayerTeam.sonyVegasInstance.getMappings().scorePlayerTeam, team.getObject(), playerName);
    }
}

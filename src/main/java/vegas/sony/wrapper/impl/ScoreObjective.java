package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MScoreObjective;
import vegas.sony.wrapper.Wrapper;

public class ScoreObjective
extends Wrapper {
    public TextComponent getFormattedDisplayName() {
        return new TextComponent(MScoreObjective.getFormattedDisplayName(ScoreObjective.sonyVegasInstance.getMappingsMapperCompat().scoreObjective, this.I));
    }

    public ITextComponent getDisplayNameComponent() {
        ITextComponent displayName = new ITextComponent(MScoreObjective.getDisplayNameComponent(ScoreObjective.sonyVegasInstance.getMappingsMapperCompat().scoreObjective, this.I));
        return displayName;
    }

    public Scoreboard getScoreboard() {
        return new Scoreboard(MScoreObjective.getScoreboard(ScoreObjective.sonyVegasInstance.getMappingsMapperCompat().scoreObjective, this.I));
    }

    public String getDisplayNameText() {
        if (ForgeVersion.MC_1_16_5.d()) {
            return this.getDisplayNameComponent().getFormattedText();
        }
        return MScoreObjective.getDisplayNameText(ScoreObjective.sonyVegasInstance.getMappingsMapperCompat().scoreObjective, this.I);
    }

    public ScoreObjective(Object object) {
        super(object);
    }

}


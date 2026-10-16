package vegas.sony.wrapper.impl;

import com.google.common.collect.Lists;
import vegas.sony.mapping.mappings.MScoreboard;
import vegas.sony.wrapper.Wrapper;

import java.util.ArrayList;
import java.util.Collection;

public class Scoreboard
extends Wrapper {
    public Collection<Score> getPlayerScores(ScoreObjective objective) {
        if (objective == null || objective.isNull()) {
            return Lists.newArrayList();
        }
        Collection scoreHandles = MScoreboard.getPlayerScores(Scoreboard.sonyVegasInstance.getMappings().scoreboard, this.I, objective.getObject());
        ArrayList scores = Lists.newArrayList();
        if (scoreHandles == null) {
            return scores;
        }
        for (Object scoreHandle : scoreHandles) {
            scores.add(new Score(scoreHandle));
        }
        return scores;
    }

    public ScoreObjective getObjectiveInDisplaySlot(int slot) {
        if (Scoreboard.sonyVegasInstance.getMappingsMapperCompat().scoreboard.getObjectiveInDisplaySlotMethod == null
                || Scoreboard.sonyVegasInstance.getMappingsMapperCompat().scoreboard.getObjectiveInDisplaySlotMethod.hasResolutionFailed()) {
            return null;
        }
        Object objective = MScoreboard.getObjectiveInDisplaySlot(
                Scoreboard.sonyVegasInstance.getMappingsMapperCompat().scoreboard, this.I, slot);
        return objective == null ? null : new ScoreObjective(objective);
    }

    public Score getValueFromObjective(String playerName, ScoreObjective objective) {
        if (Scoreboard.sonyVegasInstance.getMappingsMapperCompat().scoreboard.getValueFromObjectiveMethod == null
                || Scoreboard.sonyVegasInstance.getMappingsMapperCompat().scoreboard.getValueFromObjectiveMethod.hasResolutionFailed()) {
            return null;
        }
        if (objective == null || objective.isNull()) {
            return null;
        }
        Object score = MScoreboard.getValueFromObjective(
                Scoreboard.sonyVegasInstance.getMappingsMapperCompat().scoreboard, this.I, playerName, objective.getObject());
        return score == null ? null : new Score(score);
    }

    public Scoreboard(Object object) {
        super(object);
    }

    public ScorePlayerTeam getPlayersTeam(String playerName) {
        MScoreboard mapping = Scoreboard.sonyVegasInstance.getMappings().scoreboard;
        if (mapping.getPlayersTeamMethod == null || mapping.getPlayersTeamMethod.hasResolutionFailed()) {
            return null;
        }
        Object team = MScoreboard.getPlayersTeam(mapping, this.I, playerName);
        return team == null ? null : new ScorePlayerTeam(team);
    }
}

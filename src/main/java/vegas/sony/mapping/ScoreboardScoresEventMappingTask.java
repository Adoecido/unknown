package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.event.impl.EventScoreboardScores;
import vegas.sony.mapping.EventInjectionSpec;
import vegas.sony.mapping.JavassistMappingTask;
import vegas.sony.mapping.MappedClasses;

public class ScoreboardScoresEventMappingTask
extends JavassistMappingTask {
    private static final String SCORES_RETURN_EXPRESSION = "$event.getScores();";

    @Override
    public void transform() {
        EventInjectionSpec eventInjectionSpec = new EventInjectionSpec(SonyVegas.INSTANCE.getMappings().scoreboard.listPlayerScoresMethod, EventScoreboardScores.class);
        eventInjectionSpec.setReturnExpression(SCORES_RETURN_EXPRESSION);
        this.registerEventInjection(eventInjectionSpec);
    }

    public ScoreboardScoresEventMappingTask() {
        super(MappedClasses.F6);
    }
}

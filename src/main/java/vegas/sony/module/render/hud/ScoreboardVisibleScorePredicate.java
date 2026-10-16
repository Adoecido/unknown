package vegas.sony.module.render.hud;

import com.google.common.base.Predicate;
import vegas.sony.module.render.hud.ScoreboardHudModule;
import vegas.sony.wrapper.impl.Score;

class ScoreboardVisibleScorePredicate
implements Predicate<Score> {
    @Override
    public boolean apply(Score score) {
        return score.getOwner() != null && !score.getOwner().startsWith("#");
    }

    ScoreboardVisibleScorePredicate() {
    }
}

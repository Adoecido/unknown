package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MScore;
import vegas.sony.wrapper.Wrapper;

public class Score
extends Wrapper {
    public Score(Object object) {
        super(object);
    }

    public String getOwner() {
        return MScore.getOwner(Score.sonyVegasInstance.getMappingsMapperCompat().score, this.I);
    }

    public int getScore() {
        return MScore.getScore(Score.sonyVegasInstance.getMappingsMapperCompat().score, this.I);
    }
}

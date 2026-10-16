package vegas.sony.wrapper.impl;

import vegas.sony.SonyVegas;
import vegas.sony.mapping.mappings.MStringTextComponent;

public class ScorePlayerTeamTextComponent
extends MutableTextComponent {
    public ScorePlayerTeamTextComponent(Object object) {
        super(object);
    }


    public static ScorePlayerTeamTextComponent P(String string) {
        if (ForgeVersion.MC_1_20_6.v()) {
            SonyVegas.notifyNativeStackTrace();
        }
        return new ScorePlayerTeamTextComponent(MStringTextComponent.s(ScorePlayerTeamTextComponent.sonyVegasInstance.getMappings().Dh, string));
    }

    public String Y() {
        if (ForgeVersion.MC_1_16_5_ACTUAL.v()) {
            SonyVegas.notifyNativeStackTrace();
        }
        return ScorePlayerTeamTextComponent.sonyVegasInstance.getMappings().Dh.W(this.getObject());
    }

    public static ScorePlayerTeamTextComponent B(String string) {
        return new ScorePlayerTeamTextComponent(ScorePlayerTeamTextComponent.sonyVegasInstance.getMappings().Dh.i(string));
    }
}


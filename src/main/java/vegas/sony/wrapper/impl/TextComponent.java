package vegas.sony.wrapper.impl;

import vegas.sony.SonyVegas;
import vegas.sony.mapping.mappings.MTextComponent;

public class TextComponent
extends ITextComponent {
    public static TextComponent p(Object object) {
        return new TextComponent(MTextComponent.n(TextComponent.sonyVegasInstance.getMappingsMapperCompat().RA, object));
    }

    public TextComponent(Object object) {
        super(object);
    }

    public String U() {
        return (String)MTextComponent.s(TextComponent.sonyVegasInstance.getMappingsMapperCompat().RA, this.I);
    }

    public Style w() {
        return new Style(MTextComponent.B(TextComponent.sonyVegasInstance.getMappingsMapperCompat().RA, this.I));
    }

    public TextComponent(ScorePlayerTeam team, String playerName) {
        super(new TextComponent(SonyVegas.INSTANCE.getMappingsMapperCompat().scorePlayerTeam.formatPlayerNameComponent(team.getObject(), ScorePlayerTeamTextComponent.B(playerName).getObject())).getObject());
    }
}

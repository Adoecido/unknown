package vegas.sony.ui.click.frame.impl.profile;

import vegas.sony.SonyVegas;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.ui.click.component.value.BooleanToggleComponent;
import java.util.ArrayList;
import java.util.List;

public class ProfilesSettingsFrameState {
    public static List<GuiComponent> F(boolean bl) {
        ArrayList<GuiComponent> arrayList = new ArrayList<GuiComponent>();
        arrayList.add(new BooleanToggleComponent(SonyVegas.INSTANCE.getPublicProfileSettings().autoLoadModuleStates));
        if (!bl) {
            arrayList.add(new BooleanToggleComponent(SonyVegas.INSTANCE.getPublicProfileSettings().framePositionsPerProfile));
        }
        return arrayList;
    }

}


package vegas.sony.ui.click.component;

import vegas.sony.ui.click.component.GuiComponent;

public class GuiComponentArrayState {
    private static GuiComponent[] components;

    public static GuiComponent[] getComponents() {
        return components;
    }

    public static void setComponents(GuiComponent[] components) {
        GuiComponentArrayState.components = components;
    }

    static {
        if (GuiComponentArrayState.getComponents() != null) {
            GuiComponentArrayState.setComponents(new GuiComponent[4]);
        }
    }
}

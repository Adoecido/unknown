package vegas.sony.module.utility.inventory.cleaner.ui;

import vegas.sony.event.EventHandler;
import vegas.sony.event.EventListener;
import vegas.sony.event.impl.ProfileChangeEvent;
import vegas.sony.module.none.ClientSettings;
import vegas.sony.module.utility.inventory.cleaner.ui.InventoryCleanerProfileValueComponent;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.ui.click.component.module.ModuleComponent;
import vegas.sony.ui.click.frame.Frame;
import vegas.sony.ui.click.frame.impl.ClientSettingsSearchFrame;
import vegas.sony.ui.click.frame.impl.ModuleCategoryFrame;

public class InventoryCleanerProfileValueRefreshListener
implements EventListener {
    @EventHandler
    public void H(ProfileChangeEvent profileChangeEvent) {
        for (Frame frame : ClientSettings.mainStack.Y()) {
            if (!(frame instanceof ModuleCategoryFrame)) continue;
            ModuleCategoryFrame moduleCategoryFrame = (ModuleCategoryFrame)frame;
            for (GuiComponent guiComponent : moduleCategoryFrame.f()) {
                if (!(guiComponent instanceof ModuleComponent)) continue;
                ModuleComponent moduleComponent = (ModuleComponent)guiComponent;
                for (GuiComponent guiComponent2 : moduleComponent.getValueComponents()) {
                    if (!(guiComponent2 instanceof InventoryCleanerProfileValueComponent)) continue;
                    InventoryCleanerProfileValueComponent inventoryCleanerProfileValueComponent = (InventoryCleanerProfileValueComponent)guiComponent2;
                    inventoryCleanerProfileValueComponent.populate();
                }
            }
        }
        ClientSettings.getFrame(ClientSettingsSearchFrame.class).N$src$V$1ncxuwi();
    }

}


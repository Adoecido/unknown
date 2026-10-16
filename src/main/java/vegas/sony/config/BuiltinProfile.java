package vegas.sony.config;

import vegas.sony.SonyVegas;
import vegas.sony.config.ModuleProfileMetadataCodec;
import vegas.sony.config.Profile;
import vegas.sony.manager.ModManager;
import vegas.sony.module.Mod;
import vegas.sony.ui.click.component.GuiComponent;
import java.util.ArrayList;

public abstract class BuiltinProfile
extends Profile {
    private static final String CLIENT_VERSION;
    private static GuiComponent[] sharedGuiComponents;

    private static void clearSelectedModules() {
        ModuleProfileMetadataCodec metadataCodec = SonyVegas.INSTANCE.getModuleProfileMetadataCodec();
        for (Mod module : new ArrayList<Mod>(metadataCodec.getSelectedModules())) {
            metadataCodec.removeModule(module);
        }
    }

    public static GuiComponent[] getSharedGuiComponents() {
        return sharedGuiComponents;
    }

    public final BuiltinProfile applyPreset() {
        BuiltinProfile.clearSelectedModules();
        this.configureModules();
        this.captureCurrentState();
        return this;
    }

    protected final void selectModule(Class<? extends Mod> moduleClass) {
        ModManager modManager = SonyVegas.INSTANCE.getModManager();
        Mod module = modManager.getMod(moduleClass);
        if (module == null) {
            return;
        }
        SonyVegas.INSTANCE.getModuleProfileMetadataCodec().addModule(module);
    }

    protected abstract void configureModules();

    protected BuiltinProfile(String name) {
        super(name, CLIENT_VERSION);
    }

    public abstract boolean isApplicable();


    static {
        BuiltinProfile.setSharedGuiComponents(null);
        CLIENT_VERSION = "4.21";
    }

    public static void setSharedGuiComponents(GuiComponent[] components) {
        sharedGuiComponents = components;
    }
}


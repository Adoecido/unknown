package vegas.sony.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import vegas.sony.SonyVegas;
import vegas.sony.manager.ModManager;
import vegas.sony.module.Mod;
import vegas.sony.ui.click.frame.impl.VisibleModuleListFrame;
import java.util.ArrayList;
import java.util.List;

public class ModuleProfileMetadataCodec {
    private final List<Mod> selectedModules = new ArrayList<Mod>();

    public void addModule(Mod module) {
        if (this.selectedModules.contains(module)) {
            return;
        }
        this.selectedModules.add(module);
        module.setFavorite(true);
        VisibleModuleListFrame.e();
        SonyVegas.INSTANCE.saveAndStop();
    }

    public int getVisibleModuleCount() {
        int visibleCount = 0;
        for (Mod module : this.selectedModules) {
            if (!module.isEnabled()) continue;
            ++visibleCount;
        }
        return visibleCount;
    }

    public List<Mod> getSelectedModules() {
        return this.selectedModules;
    }


    public void removeModule(Mod module) {
        if (!this.selectedModules.contains(module)) {
            return;
        }
        this.selectedModules.remove(module);
        module.setFavorite(false);
        VisibleModuleListFrame.e();
        SonyVegas.INSTANCE.saveAndStop();
    }

    public void loadJson(JsonObject object) {
        if (object.has("modules")) {
            this.selectedModules.clear();
            JsonArray modulesJson = object.get("modules").getAsJsonArray();
            for (JsonElement moduleElement : modulesJson) {
                Mod module = SonyVegas.INSTANCE.getModManager().getMod(moduleElement.getAsString());
                if (module == null) continue;
                this.addModuleWithoutSaving(module);
            }
            VisibleModuleListFrame.e();
        }
    }

    private void addModuleWithoutSaving(Mod module) {
        if (this.selectedModules.contains(module)) {
            return;
        }
        this.selectedModules.add(module);
        module.setFavorite(true);
    }

    public ModuleProfileMetadataCodec() {
    }

    public JsonObject toJson() {
        JsonObject object = new JsonObject();
        JsonArray modulesJson = new JsonArray();
        for (Mod module : this.selectedModules) {
            modulesJson.add((JsonElement)new JsonPrimitive(module.getName()));
        }
        object.add("modules", (JsonElement)modulesJson);
        return object;
    }
}


package vegas.sony.event.listener;

import vegas.sony.event.EventHandler;
import vegas.sony.event.EventListener;
import vegas.sony.event.impl.EventPreTick;
import vegas.sony.module.render.nametags.NameTagsRenderStateTracker;
import vegas.sony.utils.TimerUtil;
import vegas.sony.utils.render.EntityModelRenderCache;
import vegas.sony.utils.render.ItemIconRenderer;
import vegas.sony.utils.render.PotionEffectIconRenderer;
import vegas.sony.wrapper.impl.ForgeVersion;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.ResourceManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SonyVegasClientEventListener
implements EventListener {
    private final TimerUtil resourceCheckTimer = new TimerUtil();
    private Set<String> resourceManagerEntries;
    private List<String> configuredResourcePacks;
    private boolean resourcePackChangePending;
    private Object resourceMap;

    private static Exception identityException(Exception exception) {
        return exception;
    }

    private void clearRenderCaches() {
        ItemIconRenderer.clear();
        EntityModelRenderCache.clear();
        PotionEffectIconRenderer.clear();
        NameTagsRenderStateTracker.INSTANCE.clear();
    }

    private void checkModernResourceState() {
        List<String> configuredPacks = Minecraft.gameSettings().f$src$Ljava_util_List_$1i0ug5l();
        if (configuredPacks != null) {
            boolean resourcePacksChangedWithoutMap;
            Map currentResourceMap = Minecraft.P();
            if (currentResourceMap != null) {
                boolean resourceMapChangedWithoutPackChange;
                boolean resourcePacksChanged;
                ArrayList<String> resourcePackSnapshot = new ArrayList<String>(configuredPacks);
                if (this.configuredResourcePacks == null) {
                    this.configuredResourcePacks = resourcePackSnapshot;
                    this.resourceMap = currentResourceMap;
                    return;
                }
                boolean resourcePacksChangedAlias = resourcePacksChanged = !this.configuredResourcePacks.equals(resourcePackSnapshot);
                if (resourcePacksChanged) {
                    boolean resourceMapChanged;
                    boolean resourceMapChangedAlias = resourceMapChanged = this.resourceMap != null && currentResourceMap != this.resourceMap;
                    if (resourceMapChanged) {
                        this.configuredResourcePacks = resourcePackSnapshot;
                        this.resourcePackChangePending = true;
                        if (this.resourcePackChangePending) {
                            this.resourceMap = currentResourceMap;
                            this.resourcePackChangePending = false;
                            this.clearRenderCaches();
                            return;
                        }
                        if (!this.resourcePackChangePending) {
                            this.resourceMap = currentResourceMap;
                            this.clearRenderCaches();
                            return;
                        }
                        if (this.resourceMap == null) {
                            this.resourceMap = currentResourceMap;
                        }
                        return;
                    }
                    this.configuredResourcePacks = resourcePackSnapshot;
                    this.resourcePackChangePending = true;
                    if (this.resourcePackChangePending) {
                        // empty if block
                    }
                    if (!this.resourcePackChangePending) {
                        // empty if block
                    }
                    if (this.resourceMap == null) {
                        this.resourceMap = currentResourceMap;
                    }
                    return;
                }
                boolean resourceMapChangedAlias = resourceMapChangedWithoutPackChange = this.resourceMap != null && currentResourceMap != this.resourceMap;
                if (resourceMapChangedWithoutPackChange) {
                    if (this.resourcePackChangePending) {
                        this.resourceMap = currentResourceMap;
                        this.resourcePackChangePending = false;
                        this.clearRenderCaches();
                        return;
                    }
                    if (!this.resourcePackChangePending) {
                        this.resourceMap = currentResourceMap;
                        this.clearRenderCaches();
                        return;
                    }
                    if (this.resourceMap == null) {
                        this.resourceMap = currentResourceMap;
                    }
                    return;
                }
                if (this.resourcePackChangePending) {
                    // empty if block
                }
                if (!this.resourcePackChangePending) {
                    // empty if block
                }
                if (this.resourceMap == null) {
                    this.resourceMap = currentResourceMap;
                }
                return;
            }
            ArrayList<String> resourcePackSnapshot = new ArrayList<String>(configuredPacks);
            if (this.configuredResourcePacks == null) {
                this.configuredResourcePacks = resourcePackSnapshot;
                this.resourceMap = currentResourceMap;
                return;
            }
            boolean resourcePacksChangedAlias = resourcePacksChangedWithoutMap = !this.configuredResourcePacks.equals(resourcePackSnapshot);
            if (resourcePacksChangedWithoutMap) {
                boolean resourceMapChanged = false;
                this.configuredResourcePacks = resourcePackSnapshot;
                this.resourcePackChangePending = true;
                if (this.resourcePackChangePending) {
                    // empty if block
                }
                if (!this.resourcePackChangePending) {
                    // empty if block
                }
                if (this.resourceMap == null) {
                    this.resourceMap = currentResourceMap;
                }
                return;
            }
            boolean resourceMapChanged = false;
            if (this.resourcePackChangePending) {
                // empty if block
            }
            if (!this.resourcePackChangePending) {
                // empty if block
            }
            if (this.resourceMap == null) {
                this.resourceMap = currentResourceMap;
            }
            return;
        }
        Map currentResourceMap = Minecraft.P();
        if (currentResourceMap != null) {
            this.checkResourceManagerState();
            return;
        }
        this.checkResourceManagerState();
    }

    private void checkResourceManagerState() {
        ResourceManager packRepository = Minecraft.getResourcePackRepository();
        if (packRepository.isNull()) {
            return;
        }
        Collection<String> entries = packRepository.getSelectedIds();
        if (entries == null) {
            return;
        }
        HashSet<String> entrySnapshot = new HashSet<String>(entries);
        if (this.resourceManagerEntries == null) {
            this.resourceManagerEntries = entrySnapshot;
            return;
        }
        if (!this.resourceManagerEntries.equals(entrySnapshot)) {
            this.resourceManagerEntries = entrySnapshot;
            this.clearRenderCaches();
        }
    }

    @EventHandler
    public void onTick(EventPreTick eventPreTick) {
        if (!this.resourceCheckTimer.hasTimeElapsed(1000L)) {
            return;
        }
        this.resourceCheckTimer.reset();
        try {
            if (ForgeVersion.MC_1_21_10.d()) {
                this.checkModernResourceState();
                return;
            }
            this.checkResourceManagerState();
        }
        catch (Exception exception) {
            // empty catch block
        }
    }
}

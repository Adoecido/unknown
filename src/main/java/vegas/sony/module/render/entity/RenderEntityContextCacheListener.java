package vegas.sony.module.render.entity;

import vegas.sony.event.EventHandler;
import vegas.sony.event.EventListener;
import vegas.sony.event.EventPriority;
import vegas.sony.event.impl.EventPreTick;
import vegas.sony.event.impl.EventWorldChange;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.module.render.entity.RenderEntityContext;
import vegas.sony.module.render.entity.RenderEntityContextCache;
import vegas.sony.wrapper.impl.Entity;
import vegas.sony.wrapper.impl.EntityLivingBase;
import vegas.sony.wrapper.impl.EntityPlayerSP;
import vegas.sony.wrapper.impl.WorldClient;
import java.util.LinkedHashSet;

public class RenderEntityContextCacheListener
implements EventListener {
    private long tickCounter;

    @EventHandler
    public void onWorldChange(EventWorldChange event) {
        RenderEntityContextCache.clear();
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onTick(EventPreTick event) {
        ++this.tickCounter;
        if (this.tickCounter % 10L == 0L) {
            RenderEntityContextCache.clearNameCaches();
        }
        EntityPlayerSP viewer = event.getThePlayer();
        if (viewer.isNull()) {
            return;
        }
        WorldClient world = event.getWorld();
        if (world.isNull()) {
            return;
        }
        LinkedHashSet<Integer> staleEntityIds = new LinkedHashSet<>();
        for (RenderEntityContext context : RenderEntityContextCache.getContexts()) {
            Entity entity = world.V(context.getEntityId());
            if (entity.isNull() || !entity.isInstance(MappedClasses.zm)) {
                staleEntityIds.add(context.getEntityId());
                continue;
            }
            EntityLivingBase livingEntity = new EntityLivingBase(entity.getObject());
            context.update(livingEntity, viewer);
        }
        for (Integer entityId : staleEntityIds) {
            RenderEntityContextCache.remove(entityId);
        }
    }

}


package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.event.impl.EventRenderWorldPassExecutorDrain;
import vegas.sony.mapping.JavassistMappingTask;
import vegas.sony.mapping.MappedClasses;

public class RenderWorldPassExecutorDrainMappingTask
extends JavassistMappingTask {
    private static final String c;

    void V() {
        this.c(SonyVegas.INSTANCE.getMappings().RY.J, EventRenderWorldPassExecutorDrain.class, c);
    }

    public RenderWorldPassExecutorDrainMappingTask() {
        super(MappedClasses.FW);
    }

    @Override
    public void transform() {
        this.V();
    }

    static {
        try {
            c = "$1";
        }
        catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}

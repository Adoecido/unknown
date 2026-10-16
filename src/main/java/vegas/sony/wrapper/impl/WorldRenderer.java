package vegas.sony.wrapper.impl;

import vegas.sony.SonyVegas;
import vegas.sony.mapping.mappings.MWorldRenderer;
import vegas.sony.wrapper.Wrapper;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class WorldRenderer
extends Wrapper {
    public WorldRenderer(Object object) {
        super(object);
    }

    public void Q(boolean bl) {
        MWorldRenderer.X(WorldRenderer.sonyVegasInstance.getMappings().qZ, this.I, bl);
    }


    public IntBuffer O() {
        if (ForgeVersion.MC_1_21_0.d()) {
            SonyVegas.notifyNativeStackTrace();
            return null;
        }
        if (ForgeVersion.MC_1_16_5.d()) {
            ByteBuffer byteBuffer = (ByteBuffer)MWorldRenderer.t(WorldRenderer.sonyVegasInstance.getMappings().qZ, this.I);
            return byteBuffer.asIntBuffer();
        }
        return (IntBuffer)MWorldRenderer.v(WorldRenderer.sonyVegasInstance.getMappings().qZ, this.I);
    }

    public int o(int n) {
        if (ForgeVersion.MC_1_16_5.d()) {
            SonyVegas.notifyNativeStackTrace();
            return -1;
        }
        return MWorldRenderer.G(WorldRenderer.sonyVegasInstance.getMappings().qZ, this.I, n);
    }
}


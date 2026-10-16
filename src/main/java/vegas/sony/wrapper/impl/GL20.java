package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MGL20;
import vegas.sony.wrapper.Wrapper;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class GL20
extends Wrapper {
    public static void w(int n, boolean bl, FloatBuffer floatBuffer) {
        MGL20.X(GL20.sonyVegasInstance.getMappingsMapperCompat().RW, n, bl, floatBuffer);
    }

    public static void x(int n, int n2, IntBuffer intBuffer) {
        MGL20.P(GL20.sonyVegasInstance.getMappingsMapperCompat().RW, n, n2, intBuffer);
    }

    public GL20(Object object) {
        super(object);
    }
}


package vegas.sony.wrapper.impl;

import vegas.sony.SonyVegas;
import vegas.sony.wrapper.Wrapper;

public class GlStateManagerTexGenState
extends Wrapper {
    public static final int F;

    public static boolean p() {
        return SonyVegas.INSTANCE.getMappings().hs != null;
    }

    static {
        long l2 = -7022238366481794461L;
        F = (int)l2;
    }


    public static void I(int n, int n2) {
        if (SonyVegas.INSTANCE.getMappings().hs != null) {
            SonyVegas.INSTANCE.getMappings().hs.K(n, n2);
        }
    }

    public GlStateManagerTexGenState(Object object) {
        super(object);
    }

    public static int J(int n) {
        if (SonyVegas.INSTANCE.getMappings().hs != null) {
            return SonyVegas.INSTANCE.getMappings().hs.F(n);
        }
        return 0;
    }
}


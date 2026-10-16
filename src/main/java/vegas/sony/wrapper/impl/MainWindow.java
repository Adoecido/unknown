package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MMainWindow;
import vegas.sony.wrapper.Wrapper;

public class MainWindow
extends Wrapper {
    public MainWindow(Object object) {
        super(object);
    }

    public float i() {
        return MMainWindow.E(MainWindow.sonyVegasInstance.getMappingsMapperCompat().DU, this.I);
    }

    public float q() {
        return MMainWindow.y(MainWindow.sonyVegasInstance.getMappingsMapperCompat().DU, this.I);
    }

    public int x() {
        return MMainWindow.T(MainWindow.sonyVegasInstance.getMappingsMapperCompat().DU, this.I);
    }

    public int o() {
        return MMainWindow.C(MainWindow.sonyVegasInstance.getMappingsMapperCompat().DU, this.I);
    }

    public float r() {
        return MMainWindow.e(MainWindow.sonyVegasInstance.getMappingsMapperCompat().DU, this.I);
    }

    public float A() {
        return MMainWindow.W(MainWindow.sonyVegasInstance.getMappingsMapperCompat().DU, this.I);
    }
}


package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class Util
extends Wrapper {
    public static long glfwGetCurrentContext() {
        return Util.sonyVegasInstance.getMappingsMapperCompat().Rf.glfwGetCurrentContext();
    }

    public Util(Object wrappedObject) {
        super(wrappedObject);
    }

    public static void glfwPollEvents() {
        Util.sonyVegasInstance.getMappingsMapperCompat().Rf.glfwPollEvents();
    }
}

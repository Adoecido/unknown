package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;
import java.util.ArrayList;
import java.util.List;

public class Lists
extends Wrapper {
    public Lists(Object listsHandle) {
        super(listsHandle);
    }

    public static ArrayList newArrayList(Iterable iterable) {
        return Lists.sonyVegasInstance.getMappingsMapperCompat().lists.newArrayList(iterable);
    }

    public static List reverse(List list) {
        return Lists.sonyVegasInstance.getMappingsMapperCompat().lists.reverse(list);
    }
}

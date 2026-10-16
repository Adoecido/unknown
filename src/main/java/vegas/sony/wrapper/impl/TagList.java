package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MNBTTagList;
import vegas.sony.wrapper.Wrapper;

public class TagList
extends Wrapper {
    public TagCompound getCompoundTagAt(int n) {
        return new TagCompound(MNBTTagList.w(TagList.sonyVegasInstance.getMappingsMapperCompat().I, this.I, n));
    }

    public String a(int n) {
        return MNBTTagList.s(TagList.sonyVegasInstance.getMappingsMapperCompat().I, this.I, n);
    }

    public int tagCount() {
        return MNBTTagList.R(TagList.sonyVegasInstance.getMappingsMapperCompat().I, this.I);
    }

    public TagList(Object object) {
        super(object);
    }
}


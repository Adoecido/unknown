package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class ResourceLocationName
extends Wrapper {
    public String getFriendlyReport(ResourceLocationKey resourceKey) {
        return ResourceLocationName.sonyVegasInstance.getMappingsMapperCompat().resourceLocationName
                .getFriendlyReport(this.I, resourceKey.getObject());
    }

    public String getCompleteReport() {
        return ResourceLocationName.sonyVegasInstance.getMappingsMapperCompat().resourceLocationName
                .getCompleteReport(this.I);
    }

    public ResourceLocationName(Object reportHandle) {
        super(reportHandle);
    }
}

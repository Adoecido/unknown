package vegas.sony.ui.click.frame.impl.target;

import vegas.sony.ui.click.frame.impl.target.TargetInfoEntityPreviewComponent;
import vegas.sony.ui.click.frame.impl.target.TargetInfoPreviewComponent;
import vegas.sony.wrapper.impl.EntityLivingBase;

public class TargetInfoLiveEntityPreviewComponent
extends TargetInfoEntityPreviewComponent {
    private final TargetInfoPreviewComponent preview;

    public TargetInfoLiveEntityPreviewComponent(TargetInfoPreviewComponent targetInfoPreviewComponent, double d, double d2) {
        super(d, d2);
        this.preview = targetInfoPreviewComponent;
    }

    @Override
    public EntityLivingBase getEntity() {
        return this.preview.getTarget();
    }
}

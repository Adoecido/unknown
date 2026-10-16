package vegas.sony.ui.click.frame.impl;

import vegas.sony.ui.click.component.GuiRefreshListener;
import vegas.sony.ui.click.frame.impl.ModuleSearchFrameHeader;

public class ModuleSearchFrameHeaderRefreshListener
implements GuiRefreshListener {
    final ModuleSearchFrameHeader Q;

    public ModuleSearchFrameHeaderRefreshListener(ModuleSearchFrameHeader moduleSearchFrameHeader) {
        this.Q = moduleSearchFrameHeader;
    }

    @Override
    public void onRefresh() {
        this.Q.l$src$V$11ec2hr();
    }
}

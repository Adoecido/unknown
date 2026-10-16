package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingMethod;
import vegas.sony.mapping.mappings.MPacketIdFactory;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.wrapper.impl.ForgeVersion;

public class MCPacketEntityAction
extends Mapping {
    private MappingMethod actionIdConstructor;
    private MappingMethod actionConstructor;

    public MCPacketEntityAction() {
        this(MPacketIdFactory.getPacketMappingControlFlowState());
    }

    private MCPacketEntityAction(GuiComponent[] controlFlowState) {
        super(MappedClasses.Dj);
        if (controlFlowState != null) {
            if (ForgeVersion.MC_1_7_10.Y()) {
                this.actionConstructor = this.Y("<init>", false, Void.TYPE, new Class[]{MappedClasses.zc, MappedClasses.Do});
            } else {
                this.actionIdConstructor = this.Y("<init>", false, Void.TYPE, new Class[]{MappedClasses.zc, Integer.TYPE});
            }
            return;
        }
        this.actionIdConstructor = this.Y("<init>", false, Void.TYPE, new Class[]{MappedClasses.zc, Integer.TYPE});
    }

    public Object createPacket(Object entity, int actionId) {
        return this.actionIdConstructor.newInstance(entity, actionId);
    }

    public Object createPacket(Object entity, Object action) {
        return this.actionConstructor.newInstance(entity, action);
    }
}

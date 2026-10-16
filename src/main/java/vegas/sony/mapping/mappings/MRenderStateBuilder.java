package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingMethod;
import vegas.sony.mapping.mappings.MEnumHandBridge;
import vegas.sony.ui.click.component.GuiComponent;

public class MRenderStateBuilder
extends Mapping {
    private static final String DRAW_WITH_SHADER = "drawWithShader";
    private final MappingMethod drawWithShaderMethod;

    public MRenderStateBuilder() {
        this(MEnumHandBridge.s());
    }

    private MRenderStateBuilder(int controlFlowState) {
        super(MappedClasses.ug);
        this.drawWithShaderMethod = this.registerStaticMethod(
                DRAW_WITH_SHADER, true, Void.TYPE, MappedClasses.qE);
        if (controlFlowState != 0) {
            GuiComponent.setLegacyComponentState(new GuiComponent[1]);
        }
    }

    public void drawWithShader(Object renderStateHandle) {
        this.drawWithShaderMethod.invokeVoid(null, renderStateHandle);
    }
}


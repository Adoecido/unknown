package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingField;
import vegas.sony.mapping.mappings.MTickEventPhase;
import vegas.sony.ui.click.component.GuiComponent;

public class MItems
extends Mapping {
    private MappingField orthographicField;
    private MappingField perspectiveField;


    public static Object getPerspective(MItems mapping) {
        return mapping.readPerspective();
    }

    private Object readPerspective() {
        return this.perspectiveField.getObject(null);
    }

    public MItems() {
        super(MappedClasses.zq);
        Class perspectiveFieldType = MappedClasses.zq;
        boolean perspectiveFieldPublic = true;
        String perspectiveFieldName = "PERSPECTIVE";
        MItems mapping = this;
        this.perspectiveField = mapping.registerStaticField(perspectiveFieldName, perspectiveFieldPublic, perspectiveFieldType);
        Class orthographicFieldType = MappedClasses.zq;
        boolean orthographicFieldPublic = true;
        String orthographicFieldName = "ORTHOGRAPHIC";
        MItems orthographicMapping = this;
        this.orthographicField = orthographicMapping.registerStaticField(orthographicFieldName, orthographicFieldPublic, orthographicFieldType);
        if (MTickEventPhase.P() != null) {
            GuiComponent.setLegacyComponentState(new GuiComponent[5]);
            return;
        }
    }

    private Object readOrthographic() {
        return this.orthographicField.getObject(null);
    }

    public static Object getOrthographic(MItems mapping) {
        return mapping.readOrthographic();
    }
}


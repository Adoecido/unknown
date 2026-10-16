package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingField;
import vegas.sony.mapping.MappingMethod;
import vegas.sony.ui.click.component.GuiComponent;

public class MFontSet
extends Mapping {
    private MappingMethod sourceMethod;
    private MappingField missingGlyphField;
    private static String fontSetControlFlowMarker;

    public static void setFontSetControlFlowMarker(String marker) {
        fontSetControlFlowMarker = marker;
    }

    public Object getMissingGlyph(Object fontSetHandle) {
        return this.missingGlyphField.getObject(fontSetHandle);
    }

    static {
        MFontSet.setFontSetControlFlowMarker("A5Xu0b");
    }


    public MFontSet() {
        this(MFontSet.getFontSetControlFlowMarker());
    }

    private MFontSet(String controlFlowMarker) {
        super(MappedClasses.D9);
        Class missingGlyphFieldType = MappedClasses.qd;
        boolean missingGlyphFieldPublic = true;
        String missingGlyphFieldName = "missingGlyph";
        MFontSet mapping = this;
        this.missingGlyphField = mapping.J(missingGlyphFieldName, missingGlyphFieldPublic, missingGlyphFieldType);
        if (controlFlowMarker != null) {
            Class[] sourceParameterTypes = new Class[]{Boolean.TYPE};
            Class sourceReturnType = MappedClasses.Yx;
            boolean sourcePublic = true;
            String sourceMethodName = "source";
            MFontSet sourceMapping = this;
            this.sourceMethod = sourceMapping.Y(sourceMethodName, sourcePublic, sourceReturnType, sourceParameterTypes);
            return;
        }
        Class[] sourceParameterTypes = new Class[]{Boolean.TYPE};
        Class sourceReturnType = MappedClasses.Yx;
        boolean sourcePublic = true;
        String sourceMethodName = "source";
        MFontSet sourceMapping = this;
        this.sourceMethod = sourceMapping.Y(sourceMethodName, sourcePublic, sourceReturnType, sourceParameterTypes);
        GuiComponent.setLegacyComponentState(new GuiComponent[5]);
    }

    public static String getFontSetControlFlowMarker() {
        return fontSetControlFlowMarker;
    }

    public Object getSource(Object fontSetHandle, boolean filterFishyGlyphs) {
        return this.sourceMethod.invokeObject(fontSetHandle, filterFishyGlyphs);
    }
}


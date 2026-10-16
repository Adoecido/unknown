package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingMethod;
import vegas.sony.mapping.mappings.MOrdering;
import vegas.sony.ui.click.component.GuiComponent;
import java.util.ArrayList;
import java.util.List;

public class MLists
extends Mapping {
    public final MappingMethod reverseMethod;
    public final MappingMethod newArrayListMethod;

    public ArrayList newArrayList(Iterable iterable) {
        return (ArrayList)this.newArrayListMethod.invokeObject(null, iterable);
    }


    public List reverse(List list) {
        return (List)this.reverseMethod.invokeObject(null, list);
    }

    public MLists() {
        this(MOrdering.getControlFlowMarker());
    }

    private MLists(String controlFlowMarker) {
        super(MappedClasses.VZ);
        this.reverseMethod = this.registerStaticMethod("reverse", false, List.class, List.class);
        this.newArrayListMethod = this.registerStaticMethod("newArrayList", false, ArrayList.class, Iterable.class);
        if (GuiComponent.getLegacyComponentState() == null) {
            MOrdering.setControlFlowMarker("Ze85D");
        }
    }
}


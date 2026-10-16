package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;

public class MBiomeRegistrySwitch
extends Mapping {
    private static int[] controlFlowState;

    public MBiomeRegistrySwitch() {
        super(MappedClasses.FU);
    }

    public static void setBiomeRegistryControlFlowState(int[] state) {
        controlFlowState = state;
    }

    public static int[] getBiomeRegistryControlFlowState() {
        return controlFlowState;
    }

    static {
        if (MBiomeRegistrySwitch.getBiomeRegistryControlFlowState() == null) {
            MBiomeRegistrySwitch.setBiomeRegistryControlFlowState(new int[5]);
        }
    }
}

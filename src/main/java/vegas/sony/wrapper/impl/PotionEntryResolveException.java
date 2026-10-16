package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.impl.PotionEntry;

public class PotionEntryResolveException
extends Exception {
    private final PotionEntry potionEntry;

    public PotionEntry getPotionEntry() {
        return this.potionEntry;
    }

    public PotionEntryResolveException(PotionEntry potionEntry) {
        this.potionEntry = potionEntry;
    }
}

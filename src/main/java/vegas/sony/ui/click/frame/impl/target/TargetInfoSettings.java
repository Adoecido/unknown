package vegas.sony.ui.click.frame.impl.target;

import vegas.sony.value.BooleanValue;

public class TargetInfoSettings {
    public final BooleanValue potsUsedComparator;
    public final BooleanValue damageComparator;
    public final BooleanValue showHovered = BooleanValue.create(this, "Show Hovered //$ui", true, "Show information on a hovered entity if not attacking. //$");
    public final BooleanValue hitsComparator;
    public final BooleanValue comboCounter;

    public TargetInfoSettings() {
        this.damageComparator = BooleanValue.create(this, "Damage Comparator //$ui", true, "Measures strength of target compared to yourself\nConsiders armor and weapon damage //$");
        this.comboCounter = BooleanValue.create(this, "Combo Counter //$ui", true, "Shows how many hits in a direct row you've landed, or taken, from target //$");
        this.hitsComparator = BooleanValue.create(this, "Hits Comparator //$ui", false, "Measures how many hits you've landed compared to target //$");
        this.potsUsedComparator = BooleanValue.create(this, "Pots Used Comparator //$ui", false, "Measures how many heal pots you've used compared to target //$");
    }
}

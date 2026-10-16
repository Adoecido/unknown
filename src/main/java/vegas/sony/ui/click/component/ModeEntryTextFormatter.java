package vegas.sony.ui.click.component;

import vegas.sony.ui.click.component.OptionTextFormatter;
import vegas.sony.unmap.ModeSelection;

public class ModeEntryTextFormatter<T extends ModeSelection>
implements OptionTextFormatter<T> {
    public static final OptionTextFormatter<ModeSelection> DEFAULT = new ModeEntryTextFormatter<ModeSelection>();


    public String formatValue(T value) {
        return value != null ? value.toString() : "";
    }

    @Override
    public String format(T value) {
        return this.formatValue(value);
    }
}

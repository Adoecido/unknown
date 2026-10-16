package vegas.sony.ui.click.component.value;

import vegas.sony.SonyVegas;
import vegas.sony.ui.click.GuiMouseListener;
import vegas.sony.ui.click.MouseClickButton;
import vegas.sony.ui.click.component.value.SearchBlockEditorComponent;
import vegas.sony.ui.unmap.SearchBlock;
import java.awt.Point;

public class SearchBlockEditorMouseListener
implements GuiMouseListener {
    final SearchBlockEditorComponent editor;
    final SearchBlock searchBlock;
    private static final String DEBUG_MESSAGE = "Clicked tracers";

    public SearchBlockEditorMouseListener(SearchBlockEditorComponent searchBlockEditorComponent, SearchBlock searchBlock) {
        this.editor = searchBlockEditorComponent;
        this.searchBlock = searchBlock;
    }

    @Override
    public void g(Point point, MouseClickButton mouseClickButton) {
        this.searchBlock.H(SearchBlockEditorComponent.getTracersValueCompat(this.editor).getEffectiveValue());
        SonyVegas.debugLog(DEBUG_MESSAGE);
    }
}

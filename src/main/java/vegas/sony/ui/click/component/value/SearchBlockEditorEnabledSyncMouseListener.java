package vegas.sony.ui.click.component.value;

import vegas.sony.ui.click.GuiMouseListener;
import vegas.sony.ui.click.MouseClickButton;
import vegas.sony.ui.click.component.value.SearchBlockEditorComponent;
import vegas.sony.ui.unmap.SearchBlock;
import java.awt.Point;

public class SearchBlockEditorEnabledSyncMouseListener
implements GuiMouseListener {
    final SearchBlock searchBlock;
    final SearchBlockEditorComponent editor;

    public SearchBlockEditorEnabledSyncMouseListener(SearchBlockEditorComponent searchBlockEditorComponent, SearchBlock searchBlock) {
        this.editor = searchBlockEditorComponent;
        this.searchBlock = searchBlock;
    }

    @Override
    public void g(Point point, MouseClickButton mouseClickButton) {
        this.searchBlock.M(SearchBlockEditorComponent.getEnabledValueCompat(this.editor).getEffectiveValueCompat());
    }
}

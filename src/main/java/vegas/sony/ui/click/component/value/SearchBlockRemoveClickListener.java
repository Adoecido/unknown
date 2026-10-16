package vegas.sony.ui.click.component.value;

import vegas.sony.SonyVegas;
import vegas.sony.ui.click.component.GuiClickListener;
import vegas.sony.ui.click.component.value.SearchBlockListComponent;
import vegas.sony.ui.unmap.SearchBlock;

public class SearchBlockRemoveClickListener
implements GuiClickListener {
    final SearchBlock searchBlock;
    final Runnable afterRemove;
    final SearchBlockListComponent owner;

    @Override
    public void onPrimaryClick() {
        SonyVegas.INSTANCE.getSearch().removeSearchBlock(this.searchBlock);
        this.afterRemove.run();
    }

    public SearchBlockRemoveClickListener(SearchBlockListComponent owner, SearchBlock searchBlock, Runnable afterRemove) {
        this.owner = owner;
        this.searchBlock = searchBlock;
        this.afterRemove = afterRemove;
    }
}

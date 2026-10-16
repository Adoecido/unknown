package vegas.sony.ui.click.component.value;

import vegas.sony.SonyVegas;
import vegas.sony.ui.click.component.GuiClickListener;
import vegas.sony.ui.click.component.value.SearchBlockListDropdownLayer;
import vegas.sony.ui.unmap.SearchBlock;

class SearchBlockRemoveHandler
implements GuiClickListener {
    final SearchBlock searchBlock;
    final SearchBlockListDropdownLayer dropdownLayer;

    @Override
    public void onPrimaryClick() {
        SonyVegas.INSTANCE.getSearch().removeSearchBlock(this.searchBlock);
        this.dropdownLayer.refreshContents();
    }

    SearchBlockRemoveHandler(SearchBlockListDropdownLayer searchBlockListDropdownLayer, SearchBlock searchBlock) {
        this.dropdownLayer = searchBlockListDropdownLayer;
        this.searchBlock = searchBlock;
    }
}

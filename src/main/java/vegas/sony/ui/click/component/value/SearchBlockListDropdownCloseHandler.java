package vegas.sony.ui.click.component.value;

import vegas.sony.ui.click.component.GuiClickListener;
import vegas.sony.ui.click.component.value.SearchBlockListComponent;
import vegas.sony.ui.click.component.value.SearchBlockListDropdownLayer;

class SearchBlockListDropdownCloseHandler
implements GuiClickListener {
    final SearchBlockListDropdownLayer dropdownLayer;
    final SearchBlockListComponent component;

    SearchBlockListDropdownCloseHandler(SearchBlockListDropdownLayer searchBlockListDropdownLayer, SearchBlockListComponent searchBlockListComponent) {
        this.dropdownLayer = searchBlockListDropdownLayer;
        this.component = searchBlockListComponent;
    }

    @Override
    public void onPrimaryClick() {
        this.component.setExpanded(false);
    }
}

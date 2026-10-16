package vegas.sony.ui.click.component.value;

import vegas.sony.SonyVegas;
import vegas.sony.ui.click.component.value.FloatingValueDropdownLayer;
import vegas.sony.ui.click.component.value.SearchBlockEditorComponent;
import vegas.sony.ui.click.component.value.SearchBlockListAddInputComponent;
import vegas.sony.ui.click.component.value.SearchBlockListComponent;
import vegas.sony.ui.click.component.value.SearchBlockListDropdownCloseHandler;
import vegas.sony.ui.click.component.value.SearchBlockRemoveHandler;
import vegas.sony.ui.click.frame.impl.profile.PublicProfilesFrameHeaderActionComponent;
import vegas.sony.ui.unmap.SearchBlock;

public class SearchBlockListDropdownLayer
extends FloatingValueDropdownLayer<SearchBlockListComponent> {
    public SearchBlockListDropdownLayer(SearchBlockListComponent searchBlockListComponent) {
        super(searchBlockListComponent);
        this.Y(new PublicProfilesFrameHeaderActionComponent(this, "allowedicon", searchBlockListComponent.getTitle()).Q(new SearchBlockListDropdownCloseHandler(this, searchBlockListComponent)));
        this.refreshContents();
    }

    @Override
    public void refreshContents() {
        this.removeMarkedChildren();
        SearchBlockListAddInputComponent searchBlockListAddInputComponent = new SearchBlockListAddInputComponent("Block name / ID");
        this.h(searchBlockListAddInputComponent, new Object[0]);
        for (SearchBlock searchBlock : SonyVegas.INSTANCE.getSearch().getSearchBlocks()) {
            SearchBlockEditorComponent searchBlockEditorComponent = new SearchBlockEditorComponent(searchBlock);
            searchBlockEditorComponent.setRemoveClickListener(new SearchBlockRemoveHandler(this, searchBlock));
            this.h(searchBlockEditorComponent, new Object[0]);
        }
    }
}

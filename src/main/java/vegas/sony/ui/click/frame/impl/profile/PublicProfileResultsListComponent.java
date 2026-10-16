package vegas.sony.ui.click.frame.impl.profile;

import vegas.sony.ui.click.component.PagedResultListComponent;

public class PublicProfileResultsListComponent
extends PagedResultListComponent {
    public PublicProfileResultsListComponent(double d, double d2) {
        super(d, d2);
        this.setLoadThreshold(6);
    }
}

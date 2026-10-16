package vegas.sony.runtime.model;

import vegas.sony.runtime.model.DetachedStringTreeEntry;
import vegas.sony.runtime.model.DetachedStringTreeNode;
import java.util.List;

class DetachedStringTreeListEntry
extends DetachedStringTreeEntry {
    final DetachedStringTreeNode listOwnerNode;
    List<String> values;

    DetachedStringTreeListEntry(DetachedStringTreeNode ownerNode) {
        super(ownerNode);
        this.listOwnerNode = ownerNode;
    }
}

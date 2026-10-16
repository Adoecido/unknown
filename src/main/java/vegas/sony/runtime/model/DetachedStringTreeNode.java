package vegas.sony.runtime.model;

import vegas.sony.runtime.model.DetachedStringTree;
import vegas.sony.runtime.model.DetachedStringTreeEntry;
import java.util.List;

class DetachedStringTreeNode {
    List<DetachedStringTreeEntry> entries;
    final DetachedStringTree ownerTree;
    String primaryValue;
    String secondaryValue;

    DetachedStringTreeNode(DetachedStringTree ownerTree) {
        this.ownerTree = ownerTree;
    }
}

package vegas.sony.runtime.model;

import vegas.sony.runtime.model.DetachedStringTreeNode;

class DetachedStringTreeEntry {
    String value;
    final DetachedStringTreeNode ownerNode;

    DetachedStringTreeEntry(DetachedStringTreeNode ownerNode) {
        this.ownerNode = ownerNode;
    }
}

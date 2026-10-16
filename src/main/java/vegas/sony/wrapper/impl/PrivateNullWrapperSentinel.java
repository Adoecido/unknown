package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class PrivateNullWrapperSentinel
extends Wrapper {
    private PrivateNullWrapperSentinel() {
        super(null);
    }
}


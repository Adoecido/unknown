package vegas.sony.utils;

import vegas.sony.utils.TimerUtil;

public class NanoTimerUtil
extends TimerUtil {
    public double getElapsedMilliseconds() {
        return (double)this.getLastMS() / 1000000.0;
    }

    @Override
    public void reset() {
        this.Q = System.nanoTime();
    }

    @Override
    public long getLastMS() {
        return System.nanoTime() - this.Q;
    }


    @Override
    public boolean hasTimeElapsed(long l) {
        boolean bl = this.getLastMS() >= l;
        return bl;
    }
}


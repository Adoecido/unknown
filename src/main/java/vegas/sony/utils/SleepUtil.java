package vegas.sony.utils;

public class SleepUtil {
    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        }
        catch (InterruptedException interruptedException) {
            // empty catch block
        }
    }
}


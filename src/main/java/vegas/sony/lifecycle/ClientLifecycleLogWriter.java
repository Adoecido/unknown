package vegas.sony.lifecycle;

/** Compatibility callback retained with file logging disabled. */
public class ClientLifecycleLogWriter implements ClientLifecycleCallback {
    public ClientLifecycleLogWriter() { }

    @Override
    public void log(String message) { }

    @Override
    public void close() { }
}

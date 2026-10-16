package vegas.sony.lifecycle;

public interface ClientLifecycleCallback {
    void log(String message);

    void close();
}

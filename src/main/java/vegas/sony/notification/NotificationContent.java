package vegas.sony.notification;

public interface NotificationContent {
    double getHeight();

    double getWidth();

    void render(double x, double y);
}

package vegas.sony.notification;

import vegas.sony.SonyVegas;
import vegas.sony.utils.EncString;
import vegas.sony.utils.Str;
import java.awt.Color;

public abstract class AbstractNotification implements INotification {
    private NotificationType type;
    private final Color notificationColor;
    private double currentX;
    private double targetY;
    private EncString title;
    private final NotificationContent content;
    private double currentY;
    private double targetX;

    public Color getRenderColor() {
        return Color.WHITE;
    }

    @Override
    public void setCurrentX(double currentX) {
        this.currentX = currentX;
    }

    @Override
    public void setTargetX(double targetX) {
        this.targetX = targetX;
    }

    @Override
    public NotificationType getType() {
        return this.type;
    }

    @Override
    public double getTargetY() {
        return this.targetY;
    }

    @Override
    public void setTargetY(double targetY) {
        this.targetY = targetY;
    }

    public String getIconResource() {
        return this.type.getIconResource();
    }

    @Override
    public double getHeight() {
        return 25.0 + this.content.getHeight();
    }

    public void setTitle(String title) {
        this.title = Str.enc(title);
    }

    @Override
    public void setCurrentY(double currentY) {
        this.currentY = currentY;
    }

    @Override
    public double getTargetX() {
        return this.targetX;
    }

    public String getTitle() {
        return this.title == null ? null : this.title.get();
    }

    @Override
    public double getWidth() {
        double titleWidth = SonyVegas.INSTANCE.getFontManager().W(0.9, true).N(this.getTitle()) + 2.0;
        return Math.max(titleWidth, this.content.getWidth());
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public AbstractNotification(NotificationType type, String title,
            NotificationContent content, double x, double y) {
        this.currentX = this.targetX = x;
        this.currentY = this.targetY = y;
        this.title = Str.enc(title);
        this.content = content;
        this.type = type;
        this.notificationColor = new Color(type.getColor());
    }

    @Override
    public double getCurrentY() {
        return this.currentY;
    }

    public NotificationContent getContent() {
        return this.content;
    }

    @Override
    public double getCurrentX() {
        return this.currentX;
    }
}

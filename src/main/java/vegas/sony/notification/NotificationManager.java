package vegas.sony.notification;

import vegas.sony.SonyVegas;
import vegas.sony.event.EventHandler;
import vegas.sony.event.EventListener;
import vegas.sony.manager.client.OnlineConnectionManager;
import vegas.sony.notification.FriendNotificationSettings;
import vegas.sony.notification.INotification;
import vegas.sony.notification.Notification;
import vegas.sony.notification.NotificationGroup;
import vegas.sony.notification.NotificationSounds;
import vegas.sony.notification.NotificationType;
import vegas.sony.notification.SoundClip;
import vegas.sony.notification.TextNotificationContent;
import vegas.sony.ui.click.GuiMouseEvent;
import vegas.sony.ui.click.MouseButton;
import vegas.sony.ui.hud.HudLayout;
import vegas.sony.ui.hud.HudLayoutModel;
import vegas.sony.ui.font.SmoothFontRenderer;
import vegas.sony.utils.MathUtil;
import vegas.sony.utils.render.GuiRenderPrimitives;
import vegas.sony.utils.render.OpenGlBackendHolder;
import vegas.sony.value.BooleanValue;
import vegas.sony.wrapper.impl.Minecraft;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

public class NotificationManager
implements EventListener {
    private final Map<NotificationGroup, BooleanValue> groupSettings;
    private final ArrayBlockingQueue<INotification> notifications = new ArrayBlockingQueue<INotification>(20);
    private static final long TOGGLE_BATCH_WINDOW_MILLIS = 85L;
    private static final long TOGGLE_NOTIFICATION_DURATION_MILLIS = 1500L;
    private static final double NOTIFICATION_GAP = 4.0;
    private final ToggleBatch enabledToggleBatch = new ToggleBatch(true);
    private final ToggleBatch disabledToggleBatch = new ToggleBatch(false);
    private long lastRenderTime;
    private static int controlFlowState;
    private double layoutAnchorX;
    private double layoutAnchorY;
    private boolean dockRight = true;
    private boolean dockBottom = true;
    private final Map<NotificationType, SoundClip> notificationSounds = new HashMap<NotificationType, SoundClip>();


    private boolean isAllowedByFriendSettings(INotification notification) {
        FriendNotificationSettings friendNotificationSettings = OnlineConnectionManager.INSTANCE.getSettings().getFriendNotificationSettings();
        if (notification.getType() == NotificationType.FRIENDS_GENERAL && !friendNotificationSettings.general.getEffectiveValue().booleanValue()) {
            return false;
        }
        if (notification.getType() == NotificationType.FRIENDS_NEW_REQUEST && !friendNotificationSettings.friendRequests.getEffectiveValue().booleanValue()) {
            return false;
        }
        if (notification.getType() == NotificationType.FRIENDS_NEW_CHAT && !friendNotificationSettings.chats.getEffectiveValue().booleanValue()) {
            return false;
        }
        if (notification.getType() == NotificationType.FRIENDS_ONLINE && !friendNotificationSettings.friendOnline.getEffectiveValue().booleanValue()) {
            return false;
        }
        if (notification.getType() == NotificationType.FRIENDS_PARTY_GENERAL && !friendNotificationSettings.partyInviteAccepted.getEffectiveValue().booleanValue()) {
            return false;
        }
        return notification.getType() != NotificationType.FRIENDS_PARTY_INVITE || friendNotificationSettings.partyInvites.getEffectiveValue();
    }

    public void show(String title, String message, NotificationType type, long durationMillis, boolean force) {
        this.enqueue(new Notification(type, title, new TextNotificationContent(message), 0.0, 0.0, durationMillis), force);
    }

    public ArrayBlockingQueue<INotification> getNotifications() {
        return this.notifications;
    }

    private boolean isGroupEnabled(NotificationGroup group) {
        if (!this.groupSettings.containsKey(group)) {
            return true;
        }
        return this.groupSettings.get(group).getEffectiveValue();
    }

    static {
        if (NotificationManager.initializeControlFlowState() == 0) {
            NotificationManager.setControlFlowState(82);
        }
    }

    public void renderNotifications() {
        if (!this.areNotificationsEnabled()) {
            this.clearNotifications();
            return;
        }
        int unusedControlFlowState = NotificationManager.getControlFlowState();
        this.flushReadyToggleBatches(System.currentTimeMillis());
        if (Minecraft.theWorld().isNull() || Minecraft.thePlayer().isNull()) {
            return;
        }
        this.updateLayoutAnchor();
        int previousAlphaTestFunction = OpenGlBackendHolder.backend.getIntegerState(3009);
        float previousAlphaTestReference = OpenGlBackendHolder.backend.getFloat(3010);
        OpenGlBackendHolder.backend.setAlphaFunction(516, 0.0f);
        OpenGlBackendHolder.backend.pushMatrix();
        /* Timebomb here (disabled): full-screen black overlay after 2027-03-14 (epoch ms 1805274154878L)
        if (System.currentTimeMillis() > 1805274154878L) {
            float playerYawMagnitude = (float)Math.abs(Minecraft.thePlayer().z());
            Color color = new Color(0, 0, 0, (int)Math.min(240.0f, playerYawMagnitude));
            GuiRenderPrimitives.y(0.0f, 0.0f, Minecraft.J(), Minecraft.h(), color);
        }
        */
        double nextTargetY = -14.0;
        long now = System.currentTimeMillis();
        long elapsedMillis = now - this.lastRenderTime;
        this.lastRenderTime = now;
        ArrayList<INotification> expiredNotifications = new ArrayList<INotification>();
        for (INotification notification : this.notifications) {
            nextTargetY -= notification.getHeight() + NOTIFICATION_GAP;
            notification.setTargetY(nextTargetY);
            int horizontalStep = (int)(Math.abs(notification.getTargetX() - notification.getCurrentX()) * 0.3);
            int verticalStep = (int)(Math.abs(notification.getTargetY() - notification.getCurrentY()) * 0.3);
            double currentX = NotificationManager.interpolateToward(
                    notification.getTargetX(), notification.getCurrentX(), elapsedMillis, horizontalStep);
            double currentY = NotificationManager.interpolateToward(
                    notification.getTargetY(), notification.getCurrentY(), elapsedMillis, verticalStep);
            notification.setCurrentX(currentX);
            notification.setCurrentY(currentY);
            notification.render();
            if (!notification.shouldRemove()) continue;
            expiredNotifications.add(notification);
        }
        for (INotification notification : expiredNotifications) {
            this.notifications.remove(notification);
        }
        OpenGlBackendHolder.backend.setAlphaFunction(previousAlphaTestFunction, previousAlphaTestReference);
        OpenGlBackendHolder.backend.popMatrix();
    }

    public static int initializeControlFlowState() {
        int currentState = NotificationManager.getControlFlowState();
        if (currentState == 0) {
            return 112;
        }
        return 0;
    }

    public void show(INotification notification) {
        this.enqueue(notification, false);
    }

    public void showInfo(String title, String message, long durationMillis) {
        this.show(title, message, NotificationType.INFO, durationMillis);
    }

    public static void setControlFlowState(int state) {
        controlFlowState = state;
    }

    public void show(String title, String message, NotificationType type, long durationMillis) {
        this.show(title, message, type, durationMillis, false);
    }

    public void showModuleToggle(String moduleName, boolean enabled) {
        if (!this.areNotificationsEnabled()) return;
        long now = System.currentTimeMillis();
        ToggleBatch batch = enabled ? this.enabledToggleBatch : this.disabledToggleBatch;
        ToggleSummary completed = null;
        synchronized (this) {
            if (!batch.add(moduleName, now)) {
                completed = batch.drain();
                batch.add(moduleName, now);
            }
        }
        if (completed != null) {
            this.enqueueToggleSummary(completed);
        }
    }

    static String moduleToggleMessage(boolean enabled) {
        return enabled ? "Module has been enabled //$ui"
                : "Module has been disabled //$ui";
    }

    @EventHandler
    public void onMouseClick(GuiMouseEvent event) {
        if (!this.areNotificationsEnabled()) return;
        if (!event.getAction().equals(MouseButton.LEFT_CLICK)) {
            return;
        }
        for (INotification notification : this.notifications) {
            notification.handleClick(event.getX(), event.getY());
        }
    }

    public static double interpolateToward(double target, double current, long elapsedMillis, double maxStep) {
        if (target == current) {
            return target;
        }
        double scaledStep = Math.max(maxStep * (double)Math.max(1L, elapsedMillis) / 16.666666666666668, 0.1);
        return current + MathUtil.clamp(target - current, -scaledStep, scaledStep);
    }

    public static int getControlFlowState() {
        return controlFlowState;
    }

    public void enqueue(INotification notification, boolean force) {
        if (!this.areNotificationsEnabled()) return;
        if (!this.isAllowedByFriendSettings(notification)) {
            return;
        }
        if (!this.isGroupEnabled(notification.getType().getGroup())) {
            return;
        }
        if (this.notificationSounds.containsKey(notification.getType())) {
            SonyVegas.INSTANCE.getNotificationSoundPlayer().queue(this.notificationSounds.get(notification.getType()));
        }
        double initialY = notification.getHeight() + 16.0;
        for (INotification queuedNotification : this.notifications) {
            initialY += queuedNotification.getHeight() + NOTIFICATION_GAP;
        }
        notification.setCurrentY(-initialY);
        notification.setCurrentX(5.0);
        notification.setTargetX(-notification.getWidth() - 8.0);
        if (this.notifications.remainingCapacity() == 0) {
            this.notifications.remove();
        }
        if (!this.notifications.contains(notification)) {
            this.notifications.add(notification);
        }
    }

    private boolean areNotificationsEnabled() {
        return SonyVegas.INSTANCE != null && SonyVegas.INSTANCE.getPublicProfileSettings() != null
                && SonyVegas.INSTANCE.getPublicProfileSettings().notifications.getEffectiveValue();
    }

    public boolean isDockedRight() {
        return this.dockRight;
    }

    public double getNotificationRenderX(double currentX, double width) {
        // Existing notification states run from -width-8 (shown) to +5
        // (hidden). Mirror that travel while keeping the selected edge fixed.
        double travel = currentX + width + 8.0;
        return this.dockRight ? this.layoutAnchorX - width + travel
                : this.layoutAnchorX - travel;
    }

    public double getNotificationRenderY(double currentY, double height) {
        // The first card targets -height-18; subsequent cards add the stack
        // spacing. Reflect that stack when the selected anchor is at the top.
        return this.dockBottom ? this.layoutAnchorY + currentY + 18.0
                : this.layoutAnchorY - currentY - height - 18.0;
    }

    private void updateLayoutAnchor() {
        double scale = Math.max(0.01, SonyVegas.INSTANCE.getClientSettings().getGuiScaleFactor());
        double spaceWidth = Minecraft.J() / 2.0;
        double spaceHeight = Minecraft.h() / 2.0;
        HudLayoutModel.Bounds container = HudLayout.resolve(HudLayout.NOTIFICATIONS,
                spaceWidth, spaceHeight, 170.0, 38.0,
                spaceWidth - 175.0, spaceHeight - 56.0);
        this.dockRight = container.right;
        this.dockBottom = container.bottom;
        this.layoutAnchorX = (container.right ? container.x + container.width : container.x) / scale;
        this.layoutAnchorY = (container.bottom ? container.y + container.height : container.y) / scale;
    }

    /** Layout-editor sample card drawn where dragged notifications will land. */
    public void renderLayoutPreview() {
        double spaceWidth = Minecraft.J() / 2.0;
        double spaceHeight = Minecraft.h() / 2.0;
        HudLayoutModel.Bounds container = HudLayout.resolve(HudLayout.NOTIFICATIONS,
                spaceWidth, spaceHeight, 170.0, 38.0,
                spaceWidth - 175.0, spaceHeight - 56.0);
        double x = container.right ? container.x + container.width - 170.0 : container.x;
        double y = container.bottom ? container.y + container.height - 38.0 : container.y;
        Color background = new Color(8, 8, 8, 230);
        GuiRenderPrimitives.e(x, y, 170.0, 32.0, background, false, 5.0f, 1.0f);
        double progressWidth = 158.0 * 0.75;
        double progressX = container.right ? x + 164.0 - progressWidth : x + 6.0;
        GuiRenderPrimitives.j(progressX, y + 27.0, progressWidth, 2.5, new Color(255, 72, 78, 160));
        SmoothFontRenderer titleFont = SonyVegas.INSTANCE.getFontManager().W(0.82, true);
        titleFont.d("Module has been enabled //$ui", x + 6.0, y + 4.0, new Color(255, 72, 78));
        GuiRenderPrimitives.e(x + 6.0, y + 15.0, 150.0, 5.0, new Color(151, 167, 159, 90), false, 2.0f, 1.0f);
    }

    public synchronized void clearNotifications() {
        this.notifications.clear();
        this.enabledToggleBatch.drain();
        this.disabledToggleBatch.drain();
        this.lastRenderTime = System.currentTimeMillis();
    }

    private void flushReadyToggleBatches(long now) {
        ToggleSummary enabledSummary;
        ToggleSummary disabledSummary;
        synchronized (this) {
            enabledSummary = this.enabledToggleBatch.drainIfReady(now);
            disabledSummary = this.disabledToggleBatch.drainIfReady(now);
        }
        if (enabledSummary != null && disabledSummary != null) {
            if (enabledSummary.firstTimestamp <= disabledSummary.firstTimestamp) {
                this.enqueueToggleSummary(enabledSummary);
                this.enqueueToggleSummary(disabledSummary);
            } else {
                this.enqueueToggleSummary(disabledSummary);
                this.enqueueToggleSummary(enabledSummary);
            }
        } else if (enabledSummary != null) {
            this.enqueueToggleSummary(enabledSummary);
        } else if (disabledSummary != null) {
            this.enqueueToggleSummary(disabledSummary);
        }
    }

    private void enqueueToggleSummary(ToggleSummary summary) {
        if (summary.count == 1) {
            this.show(summary.firstModule, moduleToggleMessage(summary.enabled),
                    NotificationType.INFO, TOGGLE_NOTIFICATION_DURATION_MILLIS);
            return;
        }
        this.show("", summary.count
                        + (summary.enabled ? " mods have been enabled //$ui"
                                : " mods have been disabled //$ui"),
                NotificationType.INFO, TOGGLE_NOTIFICATION_DURATION_MILLIS);
    }

    static final class ToggleBatch {
        private final boolean enabled;
        private final LinkedHashSet<String> moduleNames = new LinkedHashSet<String>();
        private long firstTimestamp;
        private long lastTimestamp;

        ToggleBatch(boolean enabled) {
            this.enabled = enabled;
        }

        boolean add(String moduleName, long timestamp) {
            if (!this.moduleNames.isEmpty()
                    && timestamp - this.lastTimestamp > TOGGLE_BATCH_WINDOW_MILLIS) {
                return false;
            }
            if (this.moduleNames.isEmpty()) {
                this.firstTimestamp = timestamp;
            }
            this.lastTimestamp = timestamp;
            this.moduleNames.add(moduleName);
            return true;
        }

        ToggleSummary drainIfReady(long timestamp) {
            if (this.moduleNames.isEmpty()
                    || timestamp - this.lastTimestamp < TOGGLE_BATCH_WINDOW_MILLIS) {
                return null;
            }
            return this.drain();
        }

        ToggleSummary drain() {
            if (this.moduleNames.isEmpty()) return null;
            ToggleSummary result = new ToggleSummary(this.enabled,
                    this.moduleNames.size(), this.moduleNames.iterator().next(),
                    this.firstTimestamp);
            this.moduleNames.clear();
            this.firstTimestamp = 0L;
            this.lastTimestamp = 0L;
            return result;
        }
    }

    static final class ToggleSummary {
        final boolean enabled;
        final int count;
        final String firstModule;
        final long firstTimestamp;

        ToggleSummary(boolean enabled, int count, String firstModule,
                      long firstTimestamp) {
            this.enabled = enabled;
            this.count = count;
            this.firstModule = firstModule;
            this.firstTimestamp = firstTimestamp;
        }
    }

    public NotificationManager() {
        this.groupSettings = new HashMap<NotificationGroup, BooleanValue>();
        this.notificationSounds.put(NotificationType.FRIENDS_NEW_CHAT, NotificationSounds.MESSAGE_RECEIVED);
        this.notificationSounds.put(NotificationType.FRIENDS_PARTY_INVITE, NotificationSounds.PARTY_INVITE);
        this.groupSettings.put(NotificationGroup.FRIENDS, SonyVegas.INSTANCE.getPublicProfileSettings().friendNotifications);
    }
}


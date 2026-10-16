package vegas.sony.notification;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.concurrent.ArrayBlockingQueue;
import sun.misc.Unsafe;
import vegas.sony.SonyVegas;
import vegas.sony.config.PublicProfileSettings;
import vegas.sony.manager.client.IndependentSettingsManager;

/** Headless checks for same-input module notification aggregation. */
public final class ModuleToggleNotificationProbe {
    private ModuleToggleNotificationProbe() {
    }

    public static void main(String[] arguments) throws Exception {
        NotificationManager.ToggleBatch enabled =
                new NotificationManager.ToggleBatch(true);
        require(enabled.add("Auto Clicker", 1_000L), "first toggle was rejected");
        require(enabled.add("Aim Assist", 1_040L), "same-input toggle was rejected");
        require(enabled.add("Auto Clicker", 1_060L), "duplicate toggle was rejected");
        require(enabled.drainIfReady(1_100L) == null,
                "batch flushed before its quiet window");

        NotificationManager.ToggleSummary grouped = enabled.drainIfReady(1_145L);
        require(grouped != null && grouped.enabled && grouped.count == 2,
                "enabled modules were not grouped uniquely");
        require("Auto Clicker".equals(grouped.firstModule),
                "first module order changed");

        NotificationManager.ToggleBatch disabled =
                new NotificationManager.ToggleBatch(false);
        require(disabled.add("Sprint", 2_000L), "disabled toggle was rejected");
        require(!disabled.add("Reach", 2_100L),
                "separate input was incorrectly merged");
        NotificationManager.ToggleSummary single = disabled.drain();
        require(single != null && !single.enabled && single.count == 1,
                "single disabled module summary changed");
        require(NotificationManager.moduleToggleMessage(true)
                        .contains("Module has been enabled"),
                "enabled copy changed");
        require(NotificationManager.moduleToggleMessage(false)
                        .contains("Module has been disabled"),
                "disabled copy changed");

        verifyGlobalOff();
        System.out.println("Module toggle notifications: grouping and global off clears/suppresses even forced notifications");
    }

    private static void verifyGlobalOff() throws Exception {
        Field access = Unsafe.class.getDeclaredField("theUnsafe");
        access.setAccessible(true);
        Unsafe unsafe = (Unsafe)access.get(null);
        SonyVegas client = (SonyVegas)unsafe.allocateInstance(SonyVegas.class);
        SonyVegas.INSTANCE = client;
        Field registry = SonyVegas.class.getDeclaredField("independentSettingsManager");
        registry.setAccessible(true);
        registry.set(client, new IndependentSettingsManager());
        PublicProfileSettings settings = new PublicProfileSettings();
        settings.notifications.setPersistenceSuppressed(true);
        settings.notifications.setValue(false);
        Field publicSettings = SonyVegas.class.getDeclaredField("publicProfileSettings");
        publicSettings.setAccessible(true);
        publicSettings.set(client, settings);
        NotificationManager manager = (NotificationManager)unsafe.allocateInstance(NotificationManager.class);
        ArrayBlockingQueue<INotification> queue = new ArrayBlockingQueue<INotification>(20);
        setField(manager, "notifications", queue);
        NotificationManager.ToggleBatch enabled = new NotificationManager.ToggleBatch(true);
        NotificationManager.ToggleBatch disabled = new NotificationManager.ToggleBatch(false);
        enabled.add("Aura", 1000);
        disabled.add("Target", 1000);
        setField(manager, "enabledToggleBatch", enabled);
        setField(manager, "disabledToggleBatch", disabled);
        INotification notice = (INotification)Proxy.newProxyInstance(INotification.class.getClassLoader(),
                new Class<?>[]{INotification.class}, (proxy, method, arguments) -> {
                    if ("equals".equals(method.getName())) return proxy == arguments[0];
                    if ("hashCode".equals(method.getName())) return System.identityHashCode(proxy);
                    if ("render".equals(method.getName())) throw new AssertionError("off notifications rendered");
                    return null;
                });
        queue.add(notice);
        manager.renderNotifications(); // Must return before Minecraft/OpenGL or content rendering.
        require(queue.isEmpty() && enabled.drain() == null && disabled.drain() == null,
                "turning notifications off kept queued content or toggle batches");
        manager.enqueue(notice, true);
        manager.showModuleToggle("Aura", true);
        require(queue.isEmpty() && enabled.drain() == null,
                "forced notifications or module toggles bypassed global off");
        settings.notifications.setValue(true);
        require(queue.isEmpty(), "turning notifications back on revived stale notices");
    }

    private static void setField(Object owner, String name, Object value) throws Exception {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(owner, value);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

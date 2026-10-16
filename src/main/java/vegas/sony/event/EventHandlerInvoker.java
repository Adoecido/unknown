package vegas.sony.event;

import vegas.sony.event.EventListener;
import vegas.sony.event.GeneratedEventHandlerInvoker;
import vegas.sony.event.IEvent;
import vegas.sony.event.ReflectiveEventHandlerInvoker;
import java.lang.reflect.Method;

public interface EventHandlerInvoker {
    public <T extends IEvent> void invoke(T event);

    public static EventHandlerInvoker create(EventListener listener, Class<? extends IEvent> eventType, Method method) {
        try {
            return new GeneratedEventHandlerInvoker(listener, eventType, method);
        }
        catch (Throwable throwable) {
            return new ReflectiveEventHandlerInvoker(listener, eventType, method);
        }
    }
}

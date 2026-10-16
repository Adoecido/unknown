package vegas.sony.event;

import vegas.sony.event.EventBus;
import vegas.sony.event.EventListener;
import vegas.sony.event.forge.ForgeClientChatReceivedEvent;
import vegas.sony.event.listener.ClientListenerBootstrapEventListener;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class ClientListenerBootstrap {
    private final List<EventListener> listeners = Arrays.asList(new ForgeClientChatReceivedEvent(), new ClientListenerBootstrapEventListener());

    public void registerListeners() {
        for (EventListener listener : this.listeners) {
            EventBus.getInstance().registerListener(listener, new Predicate[0]);
        }
    }

    public void unregisterListeners() {
        for (EventListener listener : this.listeners) {
            EventBus.getInstance().unregisterListener(listener);
        }
    }
}

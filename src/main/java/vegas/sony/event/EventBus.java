package vegas.sony.event;

import vegas.sony.event.EventDispatchTrace;
import vegas.sony.event.EventListener;
import vegas.sony.event.EventListenerRegistration;
import vegas.sony.event.EventListenerTiming;
import vegas.sony.event.EventListeners;
import vegas.sony.event.EventPriority;
import vegas.sony.event.EventTimingHistory;
import vegas.sony.event.IEvent;
import vegas.sony.event.impl.EventPacketSend;
import vegas.sony.event.impl.EventPreMotion;
import vegas.sony.event.impl.EventPreMove;
import vegas.sony.event.impl.EventPrePlayerTick;
import vegas.sony.event.impl.EventPreRenderTick;
import vegas.sony.event.impl.EventPreTick;
import vegas.sony.event.impl.EventRender2D;
import vegas.sony.module.Mod;
import vegas.sony.runtime.CheatBreakerDebugLog;
import vegas.sony.runtime.NativeBridge;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class EventBus {
    public static boolean timingEnabled;
    private static int obfuscationState;
    private static Method cachedHasListenersMethod;
    private final EventTimingHistory timingHistory;
    private static Method cachedFireMethod;
    private final Map<Class<? extends IEvent>, EventListeners> listenerCountersByEventType = new LinkedHashMap<Class<? extends IEvent>, EventListeners>();
    private final Map<EventListener, EventListenerRegistration> registrationsByListener = new LinkedHashMap<EventListener, EventListenerRegistration>();
    private static final EventBus INSTANCE;
    private final Map<Class<? extends IEvent>, ArrayList<EventListenerRegistration>> registrationsByEventType = new LinkedHashMap<Class<? extends IEvent>, ArrayList<EventListenerRegistration>>();
    private long preTickEvents;
    private long prePlayerTickEvents;
    private long preRenderTickEvents;
    private long preMotionEvents;
    private long preMoveEvents;
    private long packetSendEvents;
    private long render2DEvents;
    private long lastPreTickFlowLog;
    private long lastPrePlayerTickFlowLog;
    private long lastPreRenderTickFlowLog;
    private long lastPreMotionFlowLog;
    private long lastPreMoveFlowLog;
    private long lastPacketSendFlowLog;
    private long lastRender2DFlowLog;
    private long lastDispatchFailureLog;

    /**
     * Flow diagnostics run on CheatBreaker (original) and Badlion (added for
     * the 1.8.9 bring-up; same 5s throttle, logging only). Lunar and all other
     * runtimes are untouched: both flags are false there.
     */
    private static boolean isFlowDiagnosticsEnabled() {
        return NativeBridge.isCheatBreaker189Runtime()
                || NativeBridge.isBadlion189Runtime();
    }

    private static Throwable identityThrowable(Throwable throwable) {
        return throwable;
    }

    public static void setObfuscationState(int state) {
        obfuscationState = state;
    }

    public EventTimingHistory getTimingHistory() {
        return this.timingHistory;
    }

    public static Method getHasListenersMethod() {
        if (cachedHasListenersMethod == null) {
            for (Method method : EventListeners.class.getDeclaredMethods()) {
                if (method.getReturnType() != Boolean.TYPE || method.getParameterCount() != 0) continue;
                cachedHasListenersMethod = method;
                break;
            }
        }
        return cachedHasListenersMethod;
    }

    private EventListeners resolveEventListeners(Class<? extends IEvent> eventType) {
        EventListeners eventListeners = this.listenerCountersByEventType.get(eventType);
        if (eventListeners != null) {
            return eventListeners;
        }
        try {
            Method method = EventBus.findEventListenersAccessor(eventType);
            if (method == null) {
                return null;
            }
            EventListeners resolvedListeners = (EventListeners)method.invoke(null, new Object[0]);
            this.listenerCountersByEventType.put(eventType, resolvedListeners);
            return resolvedListeners;
        }
        catch (Throwable throwable) {
            return null;
        }
    }

    static {
        INSTANCE = new EventBus();
        timingEnabled = false;
        EventBus.setObfuscationState(32);
    }

    public static int getObfuscationState() {
        return obfuscationState;
    }

    public static int getObfuscationZero() {
        int state = EventBus.getObfuscationState();
        return 0;
    }

    public Map<Class<? extends IEvent>, ArrayList<EventListenerRegistration>> getRegistrationsByEventType() {
        return this.registrationsByEventType;
    }

    public static Method findEventListenersAccessor(Class<? extends IEvent> eventType) {
        for (Method method : eventType.getDeclaredMethods()) {
            if (method.getReturnType() != EventListeners.class || method.getParameterCount() != 0 || !Modifier.isStatic(method.getModifiers())) continue;
            return method;
        }
        if (IEvent.class.isAssignableFrom(eventType.getSuperclass())) {
            return EventBus.findEventListenersAccessor(eventType.getSuperclass().asSubclass(IEvent.class));
        }
        return null;
    }

    public <T extends IEvent> T post(T event) {
        EventDispatchTrace dispatchTrace = null;
        if (timingEnabled) {
            dispatchTrace = new EventDispatchTrace(event.getClass());
        }
        try {
            ArrayList<EventListenerRegistration> registrations = this.registrationsByEventType.get(event.getClass());
            if (EventBus.isFlowDiagnosticsEnabled()) {
                long now = System.currentTimeMillis();
                long count = 0L;
                boolean logFlow = false;
                if (event instanceof EventPreTick) {
                    count = ++this.preTickEvents;
                    logFlow = count == 1L || now - this.lastPreTickFlowLog >= 5000L;
                    if (logFlow) this.lastPreTickFlowLog = now;
                } else if (event instanceof EventPrePlayerTick) {
                    count = ++this.prePlayerTickEvents;
                    logFlow = count == 1L || now - this.lastPrePlayerTickFlowLog >= 5000L;
                    if (logFlow) this.lastPrePlayerTickFlowLog = now;
                } else if (event instanceof EventPreRenderTick) {
                    count = ++this.preRenderTickEvents;
                    logFlow = count == 1L || now - this.lastPreRenderTickFlowLog >= 5000L;
                    if (logFlow) this.lastPreRenderTickFlowLog = now;
                } else if (event instanceof EventPreMotion) {
                    count = ++this.preMotionEvents;
                    logFlow = count == 1L || now - this.lastPreMotionFlowLog >= 5000L;
                    if (logFlow) this.lastPreMotionFlowLog = now;
                } else if (event instanceof EventPreMove) {
                    count = ++this.preMoveEvents;
                    logFlow = count == 1L || now - this.lastPreMoveFlowLog >= 5000L;
                    if (logFlow) this.lastPreMoveFlowLog = now;
                } else if (event instanceof EventPacketSend) {
                    count = ++this.packetSendEvents;
                    logFlow = count == 1L || now - this.lastPacketSendFlowLog >= 5000L;
                    if (logFlow) this.lastPacketSendFlowLog = now;
                } else if (event instanceof EventRender2D) {
                    count = ++this.render2DEvents;
                    logFlow = count == 1L || now - this.lastRender2DFlowLog >= 5000L;
                    if (logFlow) this.lastRender2DFlowLog = now;
                }
                if (logFlow) {
                    CheatBreakerDebugLog.info("EVENT_FLOW type=" + diagnosticEventName(event)
                            + " count=" + count
                            + " listeners=" + (registrations == null ? 0 : registrations.size()));
                }
            }
            if (registrations != null && !registrations.isEmpty()) {
                ArrayList<EventListenerRegistration> matchingRegistrations = new ArrayList<EventListenerRegistration>();
                for (int index = 0; index < registrations.size(); ++index) {
                    EventListenerRegistration registration = registrations.get(index);
                    try {
                        if (!registration.passesFilters(event)) continue;
                    } catch (Throwable throwable) {
                        this.logDispatchFailure(event, registration, throwable);
                        continue;
                    }
                    matchingRegistrations.add(registration);
                }
                if (!matchingRegistrations.isEmpty()) {
                    for (EventPriority priority : EventPriority.values()) {
                        for (int index = 0; index < matchingRegistrations.size(); ++index) {
                            try {
                                EventListenerRegistration registration = matchingRegistrations.get(index);
                                EventListenerTiming listenerTiming = null;
                                if (timingEnabled) {
                                    listenerTiming = new EventListenerTiming(registration);
                                }
                                registration.dispatch(event, priority);
                                if (!timingEnabled) continue;
                                listenerTiming.finish();
                                dispatchTrace.addListenerTiming(listenerTiming);
                                continue;
                            }
                            catch (Throwable throwable) {
                                this.logDispatchFailure(event, matchingRegistrations.get(index), throwable);
                            }
                        }
                    }
                }
            }
        }
        catch (Throwable throwable) {
            this.logDispatchFailure(event, null, throwable);
        }
        if (timingEnabled) {
            dispatchTrace.finish();
            this.timingHistory.addTrace(dispatchTrace);
        }
        return event;
    }

    private void logDispatchFailure(IEvent event, EventListenerRegistration registration, Throwable failure) {
        if (!EventBus.isFlowDiagnosticsEnabled()) return;
        long now = System.currentTimeMillis();
        if (now - this.lastDispatchFailureLog < 2000L) return;
        this.lastDispatchFailureLog = now;
        CheatBreakerDebugLog.throwable("EVENT_DISPATCH type=" + diagnosticEventName(event)
                + " listener=" + (registration == null ? "unknown"
                : registration.getListener().getClass().getName()), failure);
    }

    private static String diagnosticEventName(IEvent event) {
        if (event instanceof EventPreTick) return "PreTick";
        if (event instanceof EventPrePlayerTick) return "PrePlayerTick";
        if (event instanceof EventPreRenderTick) return "PreRenderTick";
        if (event instanceof EventPreMotion) return "PreMotion";
        if (event instanceof EventPreMove) return "PreMove";
        if (event instanceof EventPacketSend) return "PacketSend";
        if (event instanceof EventRender2D) return "Render2D";
        return event.getClass().getName();
    }

    private static ArrayList<EventListenerRegistration> createRegistrationList(Class<? extends IEvent> eventType) {
        return new ArrayList<>();
    }

    public boolean unregisterListener(EventListener eventListener) {
        if (eventListener == null) {
            return false;
        }
        EventListenerRegistration registration = this.registrationsByListener.remove(eventListener);
        if (registration == null) {
            return false;
        }
        Collection<Class<? extends IEvent>> eventTypes = registration.getEventTypes();
        if (eventTypes == null || eventTypes.isEmpty()) {
            return false;
        }
        for (Class<? extends IEvent> eventType : eventTypes) {
            List<EventListenerRegistration> registrations = this.registrationsByEventType.get(eventType);
            if (registrations == null) continue;
            registrations.remove(registration);
            this.resolveEventListeners(eventType).decrementListenerCount();
        }
        return true;
    }

    public static EventBus getInstance() {
        return INSTANCE;
    }

    public EventBus() {
        this.timingHistory = new EventTimingHistory();
    }

    @SafeVarargs
    public final void registerListener(EventListener eventListener, Predicate<IEvent> ... filters) {
        if (this.registrationsByListener.containsKey(eventListener)) {
            return;
        }
        EventListenerRegistration registration = new EventListenerRegistration(eventListener, filters);
        this.registrationsByListener.put(eventListener, registration);
        if ((NativeBridge.isCheatBreaker189Runtime() || NativeBridge.isBadlion189Runtime()) && eventListener instanceof Mod) {
            Mod module = (Mod)eventListener;
            CheatBreakerDebugLog.info("EVENT_REGISTER module=" + module.getName()
                    + " events=" + registration.getEventTypes().size());
        }
        for (Class<? extends IEvent> eventType : registration.getEventTypes()) {
            this.registrationsByEventType.computeIfAbsent(eventType, EventBus::createRegistrationList).add(registration);
            this.resolveEventListeners(eventType).incrementListenerCount();
        }
    }

    public static Method getFireMethod(Class<?> eventType) {
        if (cachedFireMethod == null) {
            for (Method method : IEvent.class.getDeclaredMethods()) {
                if (method.getReturnType() != Boolean.TYPE || method.getParameterCount() != 0) continue;
                cachedFireMethod = method;
                break;
            }
        }
        return cachedFireMethod;
    }
}

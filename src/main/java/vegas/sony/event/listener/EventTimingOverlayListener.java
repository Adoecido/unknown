package vegas.sony.event.listener;

import vegas.sony.SonyVegas;
import vegas.sony.event.EventBus;
import vegas.sony.event.EventDispatchTrace;
import vegas.sony.event.EventHandler;
import vegas.sony.event.EventListener;
import vegas.sony.event.EventListenerTiming;
import vegas.sony.event.impl.EventPreTick;
import vegas.sony.event.impl.EventRender2D;
import vegas.sony.event.listener.EventTimingDisplayLine;
import vegas.sony.ui.font.SmoothFontRenderer;
import vegas.sony.utils.TimerUtil;
import vegas.sony.utils.render.GuiRenderPrimitives;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class EventTimingOverlayListener
implements EventListener {
    private static String[] obfuscationState;
    private final TimerUtil refreshTimer = new TimerUtil();
    private List<String> displayLines = new ArrayList<String>();
    public static EventTimingOverlayListener INSTANCE;

    @EventHandler
    public void onRender2D(EventRender2D eventRender2D) {
        for (int index = 0; index < this.displayLines.size(); ++index) {
            SmoothFontRenderer fontRenderer = SonyVegas.INSTANCE.getFontManager().D(12, false);
            double width = fontRenderer.N(this.displayLines.get(index));
            GuiRenderPrimitives.C(10.0, 8 + index * 8, width, 8.0, Color.BLACK);
            fontRenderer.g(this.displayLines.get(index), 10.0, 8 + index * 8, -1);
        }
    }

    private static Map<Class<?>, List<Long>> createEventTimings(Class<?> listenerType) {
        return new ConcurrentHashMap<Class<?>, List<Long>>();
    }

    static {
        INSTANCE = new EventTimingOverlayListener();
        EventTimingOverlayListener.setObfuscationState(null);
    }

    public static void setObfuscationState(String[] state) {
        obfuscationState = state;
    }

    @EventHandler
    public void onTick(EventPreTick eventPreTick) {
        if (this.refreshTimer.hasTimeElapsed(3000L)) {
            this.refreshDisplayLines();
            this.refreshTimer.reset();
        }
    }

    public static String[] getObfuscationState() {
        return obfuscationState;
    }


    private void refreshDisplayLines() {
        this.displayLines.clear();
        ConcurrentHashMap<Class<?>, Map<Class<?>, List<Long>>> timingsByListener = new ConcurrentHashMap<Class<?>, Map<Class<?>, List<Long>>>();
        for (EventDispatchTrace trace : EventBus.getInstance().getTimingHistory().getTraces().keySet()) {
            for (EventListenerTiming listenerTiming : trace.getListenerTimings()) {
                Class<?> listenerType = listenerTiming.getRegistration().getListener().getClass();
                timingsByListener.computeIfAbsent(listenerType, EventTimingOverlayListener::createEventTimings).computeIfAbsent(trace.getEventType(), EventTimingOverlayListener::createDurationList).add(listenerTiming.getDurationNanos());
            }
        }
        ArrayList<EventTimingDisplayLine> timingLines = new ArrayList<EventTimingDisplayLine>();
        for (Map.Entry<Class<?>, Map<Class<?>, List<Long>>> listenerEntry : timingsByListener.entrySet()) {
            for (Map.Entry<Class<?>, List<Long>> eventEntry : listenerEntry.getValue().entrySet()) {
                long totalMicros = eventEntry.getValue().stream().mapToLong(Long::longValue).sum() / 1000L;
                long averageMicros = (long)(eventEntry.getValue().stream().mapToLong(Long::longValue).average().orElse(0.0) / 1000.0);
                timingLines.add(new EventTimingDisplayLine(totalMicros + " " + averageMicros + " " + listenerEntry.getKey().getSimpleName() + " " + eventEntry.getKey().getName(), totalMicros));
            }
        }
        timingLines.sort(Comparator.comparingLong(EventTimingDisplayLine::getTotalMicros).reversed());
        this.displayLines = timingLines.stream().map(EventTimingDisplayLine::getText).collect(Collectors.toList());
    }

    private static List<Long> createDurationList(Class<?> eventType) {
        return new ArrayList<Long>();
    }
}

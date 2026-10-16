package vegas.sony.runtime;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

/** Small local-only diagnostic sink for mapping/rotation failures. */
public final class CheatBreakerDebugLog {
    private static final Object LOCK = new Object();
    private static final long MAX_BYTES = 2L * 1024L * 1024L;
    private static volatile File logFile;

    private CheatBreakerDebugLog() { }

    public static void initIfNeeded() {
        if (logFile != null) return;
        synchronized (LOCK) {
            if (logFile == null) {
                logFile = new File(System.getProperty("java.io.tmpdir", "."),
                        "SonyVegas-rotation-debug.log");
                appendInternal("SESSION_START");
            }
        }
    }

    public static void info(String message) {
        if (!isRelevant(message)) return;
        initIfNeeded();
        appendInternal(message == null ? "<null>" : message);
    }

    public static void missingClass(String name, Class<?> resolved) {
        if (resolved == null) {
            info("MISSING_CLASS requested=" + name);
        }
    }

    public static void missingMember(String kind, String owner, String name, String detail) {
        info("MISSING_MEMBER kind=" + kind + " owner=" + owner + " name=" + name
                + " detail=" + detail);
    }

    public static void throwable(String context, Throwable error) {
        initIfNeeded();
        synchronized (LOCK) {
            if (logFile.length() >= MAX_BYTES) return;
            PrintWriter writer = openWriter();
            if (writer == null) return;
            try {
                writer.println(prefix() + "ERROR " + context);
                Throwable current = error;
                int causes = 0;
                while (current != null && causes++ < 4) {
                    writer.println(prefix() + current.getClass().getName() + ": " + current.getMessage());
                    StackTraceElement[] stack = current.getStackTrace();
                    for (int i = 0; i < stack.length && i < 16; ++i) writer.println("    at " + stack[i]);
                    current = current.getCause();
                }
            } finally { writer.close(); }
        }
    }

    public static File getLogFile() {
        initIfNeeded();
        return logFile;
    }

    private static boolean isRelevant(String message) {
        if (message == null) return true;
        return message.startsWith("MOTION_") || message.startsWith("ROTATION_")
                || message.startsWith("TASK_") || message.startsWith("MISSING_")
                || message.startsWith("HOOK_") || message.startsWith("CONFIG_")
                || message.startsWith("ACCOUNT_INFO_") || message.startsWith("EVENT_")
                || message.startsWith("MAPPED_")
                || message.startsWith("INIT_MANAGERS");
    }

    private static void appendInternal(String message) {
        synchronized (LOCK) {
            if (logFile == null || logFile.length() >= MAX_BYTES) return;
            PrintWriter writer = openWriter();
            if (writer == null) return;
            try { writer.println(prefix() + message); } finally { writer.close(); }
        }
    }

    private static PrintWriter openWriter() {
        try {
            return new PrintWriter(new OutputStreamWriter(
                    new FileOutputStream(logFile, true), StandardCharsets.UTF_8));
        } catch (Throwable ignored) { return null; }
    }

    private static String prefix() {
        return "[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()) + "] ";
    }
}

package vegas.sony.remote;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

final class DefaultBrowserLauncher {
    private static final int MAX_REGISTRY_OUTPUT = 16384;

    private DefaultBrowserLauncher() {}

    static boolean openPrivate(String url) {
        if (url == null || !url.startsWith("http://localhost:")) return false;
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) return false;
        try {
            String progId = readDefaultBrowserProgId("http");
            if (progId == null) progId = readDefaultBrowserProgId("https");
            if (progId == null || progId.length() > 160 || !progId.matches("[A-Za-z0-9._-]+")) return false;

            String openCommand = readRegistryValue("HKEY_CLASSES_ROOT\\" + progId + "\\shell\\open\\command", "/ve", null);
            String executable = extractExecutable(openCommand);
            if (executable == null || !new File(executable).isFile()) {
                executable = findInstalledBrowser(progId);
            }
            if (executable == null) return false;

            List<String> command = privateCommand(executable, progId, url);
            if (command == null) return false;
            new ProcessBuilder(command).start();
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static String readDefaultBrowserProgId(String protocol) throws IOException, InterruptedException {
        String key = "HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\Shell\\Associations\\UrlAssociations\\"
                + protocol + "\\UserChoice";
        return readRegistryValue(key, "/v", "ProgId");
    }

    private static String readRegistryValue(String key, String valueSwitch, String valueName)
            throws IOException, InterruptedException {
        List<String> command = new ArrayList<String>();
        command.add("reg.exe");
        command.add("query");
        command.add(key);
        command.add(valueSwitch);
        if (valueName != null) command.add(valueName);
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null && output.length() < MAX_REGISTRY_OUTPUT) {
                output.append(line).append('\n');
            }
        }
        if (!process.waitFor(2L, TimeUnit.SECONDS)) {
            process.destroy();
            return null;
        }
        if (process.exitValue() != 0) return null;
        for (String line : output.toString().split("\\r?\\n")) {
            int typeIndex = line.indexOf("REG_SZ");
            int typeLength = 6;
            if (typeIndex < 0) {
                typeIndex = line.indexOf("REG_EXPAND_SZ");
                typeLength = 13;
            }
            if (typeIndex >= 0) {
                String value = line.substring(typeIndex + typeLength).trim();
                return value.isEmpty() ? null : expandEnvironment(value);
            }
        }
        return null;
    }

    private static String expandEnvironment(String value) {
        String expanded = value;
        for (String key : System.getenv().keySet()) {
            String marker = "%" + key + "%";
            if (expanded.toLowerCase(Locale.ROOT).contains(marker.toLowerCase(Locale.ROOT))) {
                expanded = replaceIgnoreCase(expanded, marker, System.getenv(key));
            }
        }
        return expanded;
    }

    private static String replaceIgnoreCase(String source, String target, String replacement) {
        String lowerSource = source.toLowerCase(Locale.ROOT);
        String lowerTarget = target.toLowerCase(Locale.ROOT);
        StringBuilder result = new StringBuilder();
        int position = 0;
        int match;
        while ((match = lowerSource.indexOf(lowerTarget, position)) >= 0) {
            result.append(source, position, match).append(replacement);
            position = match + target.length();
        }
        return result.append(source.substring(position)).toString();
    }

    private static String extractExecutable(String command) {
        if (command == null) return null;
        String trimmed = command.trim();
        if (trimmed.startsWith("\"")) {
            int end = trimmed.indexOf('"', 1);
            return end > 1 ? trimmed.substring(1, end) : null;
        }
        int executableEnd = trimmed.toLowerCase(Locale.ROOT).indexOf(".exe");
        return executableEnd >= 0 ? trimmed.substring(0, executableEnd + 4).trim() : null;
    }

    private static String findInstalledBrowser(String progId) throws IOException, InterruptedException {
        String identity = progId == null ? "" : progId.toLowerCase(Locale.ROOT);
        List<String> names = new ArrayList<String>();
        if (identity.contains("edge")) names.add("msedge.exe");
        if (identity.contains("brave")) names.add("brave.exe");
        if (identity.contains("chrome")) names.add("chrome.exe");
        if (identity.contains("firefox")) names.add("firefox.exe");
        if (identity.contains("vivaldi")) names.add("vivaldi.exe");
        if (identity.contains("opera")) names.add("opera.exe");
        String[] fallbackNames = {"brave.exe", "chrome.exe", "msedge.exe", "firefox.exe", "vivaldi.exe", "opera.exe"};
        for (String fallback : fallbackNames) {
            if (!names.contains(fallback)) names.add(fallback);
        }
        String[] roots = {
                "HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\App Paths\\",
                "HKEY_LOCAL_MACHINE\\Software\\Microsoft\\Windows\\CurrentVersion\\App Paths\\",
                "HKEY_LOCAL_MACHINE\\Software\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion\\App Paths\\"
        };
        for (String name : names) {
            for (String root : roots) {
                String path = readRegistryValue(root + name, "/ve", null);
                if (path != null) {
                    path = path.replace("\"", "").trim();
                    if (new File(path).isFile()) return path;
                }
            }
        }
        return null;
    }

    private static List<String> privateCommand(String executable, String progId, String url) {
        String identity = new File(executable).getName().toLowerCase(Locale.ROOT);
        List<String> command = new ArrayList<String>();
        command.add(executable);
        if (identity.contains("firefox")) {
            command.add("-private-window");
        } else if (identity.contains("msedge") || identity.contains("microsoftedge")) {
            command.add("-inprivate");
            command.add("--new-window");
        } else if (identity.contains("opera")) {
            command.add("--private");
        } else if (identity.contains("chrome") || identity.contains("chromium")
                || identity.contains("brave") || identity.contains("vivaldi")) {
            command.add("--incognito");
            command.add("--new-window");
        } else {
            return null;
        }
        command.add(url);
        return command;
    }
}

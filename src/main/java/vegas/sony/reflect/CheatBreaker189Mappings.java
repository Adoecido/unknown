package vegas.sony.reflect;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import vegas.sony.runtime.PayloadResources;
import vegas.sony.runtime.NativeBridge;

/**
 * CheatBreaker 1.8.9 Allatori remaps. Classes and members are renamed; notch
 * names from vanilla 1.8.9 do not resolve. Tables are generated from reviewed
 * bytecode matches via {@code tools/update_cb_mappings.py}; see
 * {@code tools/cheatbreaker-mappings.md} for the patch fingerprint and workflow.
 */
public final class CheatBreaker189Mappings {
    private static final String CURRENT_RESOURCE_ROOT = "/mappings/cheatbreaker189/";
    private static final String PREVIOUS_RESOURCE_ROOT =
            "/mappings/cheatbreaker189-previous/";
    private static final String LEGACY_09DD73_RESOURCE_ROOT =
            "/mappings/cheatbreaker189-09dd73/";
    private static final String[] RESOURCE_ROOTS = {
            CURRENT_RESOURCE_ROOT,
            PREVIOUS_RESOURCE_ROOT,
            LEGACY_09DD73_RESOURCE_ROOT
    };
    private static final String[] MARKER_RESOURCES = {
            "com/cheatbreaker/client/CheatBreaker.class",
            "com/cheatbreaker/main/CheatBreaker.class",
            "cheatbreaker/CheatBreaker.class",
            "com/cheatbreaker/client/network/messages/Message.class"
    };

    private static volatile MappingData data;
    private static volatile Properties anchors;
    private static volatile String activeResourceRoot = CURRENT_RESOURCE_ROOT;
    private static volatile String activeProfile = "current";

    private CheatBreaker189Mappings() {
    }

    public static boolean isRuntimePresent(ClassLoader... preferredLoaders) {
        Set<ClassLoader> loaders = candidateLoaders(preferredLoaders);
        String resolvableProfile = null;
        Properties resolvableAnchors = null;
        for (String resourceRoot : RESOURCE_ROOTS) {
            Properties runtimeAnchors = loadAnchors(resourceRoot);
            String minecraftClass = runtimeAnchors.getProperty("minecraftClass");
            String getter = runtimeAnchors.getProperty("minecraftGetter");
            String instanceField = runtimeAnchors.getProperty("minecraftInstanceField");
            if (minecraftClass == null || getter == null || instanceField == null) {
                continue;
            }
            for (ClassLoader loader : loaders) {
                Class<?> minecraft = resolveRuntimeClass(minecraftClass, loader);
                if (minecraft == null) {
                    continue;
                }
                if (resolvableProfile == null) {
                    resolvableProfile = resourceRoot;
                    resolvableAnchors = runtimeAnchors;
                }
                // Allatori Minecraft class + static self getter/field is enough:
                // vanilla/Badlion keep notch `ave` and never expose this type.
                if (matchesMinecraftStructure(minecraft, getter, instanceField)) {
                    activateProfile(resourceRoot, runtimeAnchors);
                    return true;
                }
            }
            Class<?> loadedMinecraft = resolveLoadedRuntimeClass(minecraftClass);
            if (loadedMinecraft != null) {
                if (resolvableProfile == null) {
                    resolvableProfile = resourceRoot;
                    resolvableAnchors = runtimeAnchors;
                }
                if (matchesMinecraftStructure(
                        loadedMinecraft, getter, instanceField)) {
                    activateProfile(resourceRoot, runtimeAnchors);
                    return true;
                }
            }
        }
        // Some CheatBreaker launchers expose the game classes before their
        // transformed member metadata is complete. A unique CB marker plus a
        // resolvable mapped Minecraft class still identifies the right profile.
        if (resolvableProfile != null && hasCheatBreakerMarker(loaders)) {
            activateProfile(resolvableProfile, resolvableAnchors);
            return true;
        }
        return false;
    }

    public static String getActiveProfile() {
        return activeProfile;
    }

    public static boolean hasMarker(ClassLoader... preferredLoaders) {
        return hasCheatBreakerMarker(preferredLoaders);
    }

    public static String remapClassName(String sourceClassName) {
        String normalized = normalizeInternalName(sourceClassName);
        if (normalized == null) {
            return null;
        }
        return getData().classNames.get(normalized);
    }

    public static Class<?> resolveClass(String sourceClassName,
                                        ClassLoader... preferredLoaders) {
        String normalized = normalizeInternalName(sourceClassName);
        if (normalized == null || normalized.isEmpty()) {
            return null;
        }
        Set<String> candidates = new LinkedHashSet<String>();
        String remapped = getData().classNames.get(normalized);
        if (remapped != null) {
            candidates.add(remapped);
        }
        // Also accept dotted MCP keys stored in the table.
        String dotted = normalized.replace('/', '.');
        String remappedDotted = getData().classNames.get(dotted);
        if (remappedDotted != null) {
            candidates.add(remappedDotted);
        }
        candidates.add(normalized);

        for (String candidate : candidates) {
            for (ClassLoader loader : candidateLoaders(preferredLoaders)) {
                Class<?> resolved = resolveRuntimeClass(candidate, loader);
                if (resolved != null) {
                    return resolved;
                }
            }
            Class<?> loaded = resolveLoadedRuntimeClass(candidate);
            if (loaded != null) {
                return loaded;
            }
        }
        return null;
    }

    static String lookupFieldSrgName(Field field) {
        if (field == null) {
            return null;
        }
        String key = memberKey(field.getDeclaringClass().getName(), field.getName());
        return getData().fieldNames.get(key);
    }

    static String lookupMethodSrgName(Method method) {
        if (method == null) {
            return null;
        }
        String ownerAndName = memberKey(
                method.getDeclaringClass().getName(), method.getName());
        String descriptor = ParameterResolver.methodDescriptor(
                method.getParameterTypes(), method.getReturnType());
        return getData().methodNames.get(ownerAndName + ' ' + descriptor);
    }

    /**
     * Resolves a vanilla/MCP member name to its Allatori runtime name.  The
     * reverse tables are used by diagnostics; the forward tables are what the
     * reflective wrapper path needs on CheatBreaker.
     */
    public static String lookupRuntimeMethodName(Class<?> owner, String sourceName) {
        if (owner == null || sourceName == null) {
            return null;
        }
        return getData().forwardMethodNames.get(memberKey(owner.getName(), sourceName));
    }

    public static String lookupRuntimeFieldName(Class<?> owner, String sourceName) {
        if (owner == null || sourceName == null) {
            return null;
        }
        return getData().forwardFieldNames.get(memberKey(owner.getName(), sourceName));
    }

    public static Map<String, String> getClassNameMap() {
        return getData().classNames;
    }

    public static int getClassMappingCount() {
        return getData().classNames.size();
    }

    public static int getFieldMappingCount() {
        return getData().fieldNames.size();
    }

    public static int getMethodMappingCount() {
        return getData().methodNames.size();
    }

    private static boolean hasCheatBreakerMarker(ClassLoader... preferredLoaders) {
        return hasCheatBreakerMarker(candidateLoaders(preferredLoaders));
    }

    private static boolean hasCheatBreakerMarker(Set<ClassLoader> loaders) {
        for (ClassLoader loader : loaders) {
            for (String marker : MARKER_RESOURCES) {
                try {
                    URL resource = loader.getResource(marker);
                    if (resource != null) {
                        return true;
                    }
                }
                catch (RuntimeException ignored) {
                }
                catch (LinkageError ignored) {
                }
            }
            // Fallback: scan loaded packages via known Allatori Minecraft only.
        }
        for (String marker : MARKER_RESOURCES) {
            String className = marker.substring(0, marker.length() - ".class".length());
            if (resolveLoadedRuntimeClass(className) != null) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesMinecraftStructure(Class<?> minecraftClass,
                                                     String getterName,
                                                     String instanceFieldName) {
        try {
            Method getter = minecraftClass.getDeclaredMethod(getterName);
            Field instance = minecraftClass.getDeclaredField(instanceFieldName);
            return Modifier.isStatic(getter.getModifiers())
                    && getter.getReturnType() == minecraftClass
                    && Modifier.isStatic(instance.getModifiers())
                    && instance.getType() == minecraftClass;
        }
        catch (NoSuchMethodException ignored) {
            return false;
        }
        catch (NoSuchFieldException ignored) {
            return false;
        }
        catch (SecurityException ignored) {
            return false;
        }
        catch (LinkageError ignored) {
            return false;
        }
    }

    private static MappingData getData() {
        MappingData result = data;
        if (result != null) {
            return result;
        }
        synchronized (CheatBreaker189Mappings.class) {
            result = data;
            if (result == null) {
                result = loadMappings();
                data = result;
            }
        }
        return result;
    }

    private static Properties getAnchors() {
        Properties result = anchors;
        if (result != null) {
            return result;
        }
        synchronized (CheatBreaker189Mappings.class) {
            result = anchors;
            if (result == null) {
                result = loadAnchors(activeResourceRoot);
                anchors = result;
            }
        }
        return result;
    }

    private static Properties loadAnchors(String resourceRoot) {
        Properties properties = new Properties();
        try (InputStream stream = PayloadResources.open(
                CheatBreaker189Mappings.class,
                resourceRoot + "anchors.properties")) {
            if (stream == null) {
                throw new FileNotFoundException(
                        "Missing mapping resource: "
                                + resourceRoot + "anchors.properties");
            }
            properties.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
        catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to load CheatBreaker 1.8.9 anchors", exception);
        }
        return properties;
    }

    private static void activateProfile(String resourceRoot,
                                        Properties runtimeAnchors) {
        synchronized (CheatBreaker189Mappings.class) {
            if (!resourceRoot.equals(activeResourceRoot)) {
                activeResourceRoot = resourceRoot;
                data = null;
            }
            activeProfile = loadProfileLabel(resourceRoot);
            anchors = runtimeAnchors;
        }
    }

    private static String loadProfileLabel(String resourceRoot) {
        // Read the resource byte-by-byte. InputStreamReader/BufferedReader uses
        // an 8 KiB character buffer and previously materialized almost the
        // complete mapping report as one searchable String in the target JVM.
        try (InputStream stream = PayloadResources.open(
                CheatBreaker189Mappings.class,
                resourceRoot + "mapping-update.json")) {
            char[] prefix = new char[8];
            int hexRun = 0;
            int current;
            while ((current = stream.read()) >= 0) {
                boolean hex = current >= '0' && current <= '9'
                        || current >= 'a' && current <= 'f'
                        || current >= 'A' && current <= 'F';
                if (!hex) {
                    hexRun = 0;
                    continue;
                }
                if (hexRun < prefix.length) prefix[hexRun] = (char)current;
                if (++hexRun == 64) return new String(prefix);
            }
        }
        catch (Exception ignored) {
            // Mapping resources still work when an older profile predates metadata.
        }
        return resourceRoot.equals(PREVIOUS_RESOURCE_ROOT)
                ? "previous" : resourceRoot.equals(LEGACY_09DD73_RESOURCE_ROOT)
                ? "09dd73" : "current";
    }

    private static MappingData loadMappings() {
        Map<String, String> classNames = new LinkedHashMap<String, String>();
        Map<String, String> fieldNames = new LinkedHashMap<String, String>();
        Map<String, String> methodNames = new LinkedHashMap<String, String>();
        Map<String, String> forwardFieldNames = new LinkedHashMap<String, String>();
        Map<String, String> forwardMethodNames = new LinkedHashMap<String, String>();

        try (BufferedReader reader = openReader("classes.tsv")) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }
                String[] columns = line.split("\t", -1);
                if (columns.length >= 2) {
                    String source = normalizeInternalName(columns[0]);
                    String runtime = normalizeInternalName(columns[1]);
                    if (source != null && runtime != null) {
                        classNames.put(source, runtime);
                        classNames.put(source.replace('/', '.'), runtime);
                    }
                }
            }
        }
        catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to load CheatBreaker 1.8.9 class maps", exception);
        }

        try (BufferedReader reader = openReader("fields.tsv")) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }
                String[] columns = line.split("\t", -1);
                if (columns.length >= 2) {
                    // allatoriOwner/allatoriName -> mcp/readable
                    fieldNames.put(columns[0], columns[1]);
                }
            }
        }
        catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to load CheatBreaker 1.8.9 field maps", exception);
        }

        try (BufferedReader reader = openReader("methods.tsv")) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }
                String[] columns = line.split("\t", -1);
                if (columns.length >= 2) {
                    // "owner/name desc" -> mcp/readable
                    methodNames.put(columns[0], columns[1]);
                }
            }
        }
        catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to load CheatBreaker 1.8.9 method maps", exception);
        }

        try (BufferedReader reader = openReader("fields_fwd.tsv")) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }
                String[] columns = line.split("\\t", -1);
                if (columns.length >= 3) {
                    forwardFieldNames.put(memberKey(columns[0], columns[1]), columns[2]);
                }
            }
        }
        catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to load CheatBreaker 1.8.9 forward field maps", exception);
        }

        try (BufferedReader reader = openReader("methods_fwd.tsv")) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }
                String[] columns = line.split("\\t", -1);
                if (columns.length >= 3) {
                    String key = memberKey(columns[0], columns[1]);
                    String runtimeName = columns[2];
                    String existing = forwardMethodNames.get(key);
                    if (existing == null || existing.equals(runtimeName)) {
                        forwardMethodNames.put(key, runtimeName);
                    }
                }
            }
        }
        catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to load CheatBreaker 1.8.9 forward method maps", exception);
        }

        if (classNames.isEmpty() || fieldNames.isEmpty() || methodNames.isEmpty()
                || forwardFieldNames.isEmpty() || forwardMethodNames.isEmpty()) {
            throw new IllegalStateException(
                    "Incomplete CheatBreaker 1.8.9 mappings");
        }
        return new MappingData(classNames, fieldNames, methodNames,
                forwardFieldNames, forwardMethodNames);
    }

    private static BufferedReader openReader(String fileName) throws IOException {
        InputStream stream = PayloadResources.open(
                CheatBreaker189Mappings.class, activeResourceRoot + fileName);
        if (stream == null) {
            throw new FileNotFoundException(
                    "Missing mapping resource: " + activeResourceRoot + fileName);
        }
        return new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }

    private static Class<?> resolveRuntimeClass(String runtimeInternalName,
                                                 ClassLoader loader) {
        if (runtimeInternalName == null || loader == null) {
            return null;
        }
        String binaryName = runtimeInternalName.replace('/', '.');
        try {
            return Class.forName(binaryName, false, loader);
        }
        catch (ClassNotFoundException ignored) {
            return null;
        }
        catch (LinkageError ignored) {
            return null;
        }
    }

    private static Class<?> resolveLoadedRuntimeClass(String runtimeInternalName) {
        if (runtimeInternalName == null) {
            return null;
        }
        try {
            return NativeBridge.glc(runtimeInternalName);
        }
        catch (UnsatisfiedLinkError ignored) {
            return null;
        }
        catch (RuntimeException ignored) {
            return null;
        }
        catch (LinkageError ignored) {
            return null;
        }
    }

    private static Set<ClassLoader> candidateLoaders(ClassLoader... preferredLoaders) {
        Set<ClassLoader> loaders = new LinkedHashSet<ClassLoader>();
        if (preferredLoaders != null) {
            for (ClassLoader loader : preferredLoaders) {
                if (loader != null) {
                    loaders.add(loader);
                }
            }
        }
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        if (contextLoader != null) {
            loaders.add(contextLoader);
        }
        ClassLoader profileLoader = CheatBreaker189Mappings.class.getClassLoader();
        if (profileLoader != null) {
            loaders.add(profileLoader);
        }
        try {
            ClassLoader systemLoader = ClassLoader.getSystemClassLoader();
            if (systemLoader != null) {
                loaders.add(systemLoader);
            }
        }
        catch (SecurityException ignored) {
        }
        return loaders;
    }

    private static String normalizeInternalName(String className) {
        if (className == null) {
            return null;
        }
        String normalized = className.trim().replace('.', '/');
        if (normalized.length() > 2 && normalized.charAt(0) == 'L'
                && normalized.charAt(normalized.length() - 1) == ';') {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        return normalized;
    }

    private static String memberKey(String ownerName, String memberName) {
        return ownerName.replace('.', '/') + '/' + memberName;
    }

    private static final class MappingData {
        final Map<String, String> classNames;
        final Map<String, String> fieldNames;
        final Map<String, String> methodNames;
        final Map<String, String> forwardFieldNames;
        final Map<String, String> forwardMethodNames;

        MappingData(Map<String, String> classNames, Map<String, String> fieldNames,
                    Map<String, String> methodNames,
                    Map<String, String> forwardFieldNames,
                    Map<String, String> forwardMethodNames) {
            this.classNames = Collections.unmodifiableMap(classNames);
            this.fieldNames = Collections.unmodifiableMap(fieldNames);
            this.methodNames = Collections.unmodifiableMap(methodNames);
            this.forwardFieldNames = Collections.unmodifiableMap(forwardFieldNames);
            this.forwardMethodNames = Collections.unmodifiableMap(forwardMethodNames);
        }
    }
}

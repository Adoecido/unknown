import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Keeps an embedded JAR in memory and exposes its entries to the native
 * bootstrap. Classes are deliberately not defined by this loader: the native
 * side defines them in the game ClassLoader, while this child remains the
 * thread context loader so resource streams continue to work.
 */
public final class MemoryJarLoader extends ClassLoader {
    private final HashMap<String, byte[]> classData =
            new HashMap<String, byte[]>();
    private final HashMap<String, byte[]> resourceData =
            new HashMap<String, byte[]>();

    public MemoryJarLoader(byte[] jarData, ClassLoader parent)
            throws IOException {
        super(parent);
        ZipInputStream zip = new ZipInputStream(
                new ByteArrayInputStream(jarData));
        try {
            byte[] buffer = new byte[8192];
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                try {
                    if (entry.isDirectory()) {
                        continue;
                    }
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    int read;
                    while ((read = zip.read(buffer)) != -1) {
                        if (read != 0) {
                            output.write(buffer, 0, read);
                        }
                    }
                    String entryName = entry.getName();
                    byte[] bytes = output.toByteArray();
                    resourceData.put(entryName, bytes);
                    if (isLoadableClassEntry(entryName)) {
                        String className = entryName.substring(
                                0, entryName.length() - ".class".length())
                                .replace('/', '.');
                        classData.put(className, bytes);
                    }
                } finally {
                    zip.closeEntry();
                }
            }
        } finally {
            zip.close();
        }
    }

    /** Returns binary class names with superclasses and interfaces first. */
    public String[] getClassNames() {
        ArrayList<String> ordered = new ArrayList<String>(classData.size());
        HashSet<String> visiting = new HashSet<String>();
        HashSet<String> visited = new HashSet<String>();
        for (String name : classData.keySet()) {
            addInDefinitionOrder(name, visiting, visited, ordered);
        }
        return ordered.toArray(new String[ordered.size()]);
    }

    public byte[] getClassBytes(String binaryName) {
        return classData.get(binaryName);
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        InputStream parentStream = super.getResourceAsStream(name);
        if (parentStream != null) {
            return parentStream;
        }
        byte[] bytes = resourceData.get(normalizeResourceName(name));
        return bytes == null ? null : new ByteArrayInputStream(bytes);
    }

    @Override
    protected URL findResource(String name) {
        final byte[] bytes = resourceData.get(normalizeResourceName(name));
        if (bytes == null) {
            return null;
        }
        try {
            return new URL(null,
                    "memoryjar:/" + normalizeResourceName(name),
                    new URLStreamHandler() {
                        @Override
                        protected URLConnection openConnection(URL url) {
                            return new URLConnection(url) {
                                @Override
                                public void connect() {
                                    connected = true;
                                }

                                @Override
                                public InputStream getInputStream() {
                                    connect();
                                    return new ByteArrayInputStream(bytes);
                                }

                                @Override
                                public int getContentLength() {
                                    return bytes.length;
                                }
                            };
                        }
                    });
        }
        catch (IOException ignored) {
            return null;
        }
    }

    @Override
    protected Enumeration<URL> findResources(String name) {
        URL resource = findResource(name);
        return resource == null
                ? Collections.<URL>emptyEnumeration()
                : Collections.enumeration(Collections.singleton(resource));
    }

    private static String normalizeResourceName(String name) {
        return name != null && name.startsWith("/")
                ? name.substring(1) : name;
    }

    private static boolean isLoadableClassEntry(String name) {
        return name.endsWith(".class")
                && !name.endsWith("module-info.class")
                && !name.startsWith("META-INF/versions/");
    }

    private void addInDefinitionOrder(String name,
            HashSet<String> visiting, HashSet<String> visited,
            ArrayList<String> ordered) {
        if (visited.contains(name) || !visiting.add(name)) {
            return;
        }
        byte[] bytes = classData.get(name);
        if (bytes != null) {
            try {
                String[] dependencies = directHierarchyDependencies(bytes);
                for (int index = 0; index < dependencies.length; ++index) {
                    String dependency = dependencies[index];
                    if (dependency != null && classData.containsKey(dependency)) {
                        addInDefinitionOrder(dependency, visiting, visited,
                                ordered);
                    }
                }
            } catch (IOException ignored) {
                // Native DefineClass retries unresolved entries, so malformed
                // hierarchy metadata does not make the complete JAR unusable.
            }
        }
        visiting.remove(name);
        visited.add(name);
        ordered.add(name);
    }

    private static String[] directHierarchyDependencies(byte[] bytes)
            throws IOException {
        DataInputStream input = new DataInputStream(
                new ByteArrayInputStream(bytes));
        if (input.readInt() != 0xCAFEBABE) {
            throw new IOException("invalid class header");
        }
        input.readUnsignedShort();
        input.readUnsignedShort();
        int constantPoolCount = input.readUnsignedShort();
        String[] utf8 = new String[constantPoolCount];
        int[] classNameIndexes = new int[constantPoolCount];
        for (int index = 1; index < constantPoolCount; ++index) {
            int tag = input.readUnsignedByte();
            switch (tag) {
                case 1:
                    utf8[index] = input.readUTF();
                    break;
                case 3:
                case 4:
                    input.readInt();
                    break;
                case 5:
                case 6:
                    input.readLong();
                    ++index;
                    break;
                case 7:
                    classNameIndexes[index] = input.readUnsignedShort();
                    break;
                case 8:
                case 16:
                case 19:
                case 20:
                    input.readUnsignedShort();
                    break;
                case 9:
                case 10:
                case 11:
                case 12:
                case 17:
                case 18:
                    input.readUnsignedShort();
                    input.readUnsignedShort();
                    break;
                case 15:
                    input.readUnsignedByte();
                    input.readUnsignedShort();
                    break;
                default:
                    throw new IOException("unsupported constant-pool tag " + tag);
            }
        }
        input.readUnsignedShort();
        input.readUnsignedShort();
        int superClass = input.readUnsignedShort();
        int interfaceCount = input.readUnsignedShort();
        String[] result = new String[interfaceCount + (superClass == 0 ? 0 : 1)];
        int outputIndex = 0;
        if (superClass != 0) {
            result[outputIndex++] = className(
                    superClass, classNameIndexes, utf8);
        }
        for (int index = 0; index < interfaceCount; ++index) {
            result[outputIndex++] = className(input.readUnsignedShort(),
                    classNameIndexes, utf8);
        }
        return result;
    }

    private static String className(int classIndex,
            int[] classNameIndexes, String[] utf8) {
        if (classIndex <= 0 || classIndex >= classNameIndexes.length) {
            return null;
        }
        int nameIndex = classNameIndexes[classIndex];
        if (nameIndex <= 0 || nameIndex >= utf8.length
                || utf8[nameIndex] == null) {
            return null;
        }
        return utf8[nameIndex].replace('/', '.');
    }
}

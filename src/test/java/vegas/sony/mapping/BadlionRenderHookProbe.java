package vegas.sony.mapping;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

/** Exercises the shipped, renamed hook against real LWJGL bytecode, without a game. */
public final class BadlionRenderHookProbe {
    public static void main(String[] args) throws Exception {
        Path jar = Paths.get(args[0]);
        Properties names = new Properties();
        try (InputStream input = Files.newInputStream(Paths.get(args[1]))) {
            names.load(input);
        }
        String taskName = renamed(names, "mapping/Badlion189Render2DMappingTask");
        String callbackName = renamed(names, "event/impl/EventRender2D");
        String tickTaskName = renamed(names, "mapping/MinecraftTickEventMappingTask");
        String packetTaskName = renamed(names, "mapping/NetworkPacketEventMappingTask");
        String mouseTaskName = renamed(names, "asm/transform/impl/MinecraftMouseActionTransformer");
        Map<String, String> nativeNames = new HashMap<>();
        for (String line : Files.readAllLines(Paths.get(args[2]), StandardCharsets.UTF_8)) {
            String[] pair = line.split("\\s+", 2);
            if (pair.length == 2) nativeNames.put(pair[0], pair[1]);
        }
        String[] representatives = {"event/impl/EventRender2D", "mapping/EventPreTickCallback",
                "input/ChatCommandHandler"};
        for (String representative : representatives) {
            String sourcePackage = "vegas/sony/" + representative.substring(0, representative.lastIndexOf('/') + 1);
            String target = renamed(names, representative);
            String targetPackage = target.substring(0, target.lastIndexOf('/') + 1);
            require(targetPackage.equals(nativeNames.get(sourcePackage)),
                    "Native callback package is out of sync: " + sourcePackage);
        }
        int verifiedLinks = verifyInternalMethodLinks(jar);
        // No parent: the task must run with the classes and relocated ASM that
        // actually ship in the DLL, not with development classes on the test path.
        try (URLClassLoader loader = new URLClassLoader(
                new URL[]{jar.toUri().toURL()}, null)) {
            Class<?> task = Class.forName(taskName.replace('/', '.'), true, loader);
            Class<?> nodeType = loader.loadClass("lIlllIl.tree.ClassNode");
            Method install = Arrays.stream(task.getDeclaredMethods())
                    .filter(method -> Modifier.isStatic(method.getModifiers())
                            && Arrays.equals(method.getParameterTypes(), new Class<?>[]{nodeType})
                            && method.getReturnType() == void.class)
                    .findFirst().orElseThrow(() -> new AssertionError("Missing hook installer"));
            install.setAccessible(true);
            Class<?> callback = loader.loadClass(callbackName.replace('/', '.'));
            require(Modifier.isStatic(callback.getMethod("create").getModifiers()),
                    "Final payload cannot resolve the render callback");
            require(loader.getResource(renamed(names, "reflect/Badlion189Mappings") + ".class") != null,
                    "Missing Badlion profile");
            require(loader.getResource("mappings/vanilla189/joined.srg") != null,
                    "Missing shared 1.8.9 mappings");

            ClassNode primary = read(resource(loader,
                    renamed(names, "mapping/PrimaryMappingTaskSet") + ".class"));
            boolean renderRegistered = false;
            boolean tickRegistered = false;
            boolean packetRegistered = false;
            boolean mouseRegistered = false;
            for (MethodNode method : primary.methods) {
                for (AbstractInsnNode instruction : method.instructions) {
                    if (instruction.getOpcode() != Opcodes.NEW) continue;
                    String allocated = ((TypeInsnNode) instruction).desc;
                    if (taskName.equals(allocated)) renderRegistered = true;
                    if (tickTaskName.equals(allocated)) tickRegistered = true;
                    if (packetTaskName.equals(allocated)) packetRegistered = true;
                    if (mouseTaskName.equals(allocated)) mouseRegistered = true;
                }
            }
            require(renderRegistered, "Badlion task is missing from the primary mapping set");
            require(tickRegistered, "Badlion payload omitted the client tick hook");
            require(packetRegistered, "Badlion payload omitted the packet hook");
            require(mouseRegistered, "Badlion payload omitted the mouse hook");

            byte[] original = resource(loader, "org/lwjgl/opengl/Display.class");
            byte[] transformed = apply(loader, nodeType, install, original);
            String nativeRenderPrefix = nativeNames.get("vegas/sony/event/impl/");
            require(new String(transformed, StandardCharsets.ISO_8859_1).contains(nativeRenderPrefix),
                    "Native retention cannot identify the renamed render callback");
            require(!new String(original, StandardCharsets.ISO_8859_1).contains(nativeRenderPrefix),
                    "Callback-free rollback would keep a retained render callback");
            ClassNode display = read(transformed);
            int callbacks = 0;
            for (MethodNode method : display.methods) {
                for (AbstractInsnNode instruction : method.instructions) {
                    if (!isCall(instruction, callbackName, "create", "()V")) continue;
                    ++callbacks;
                    require("update".equals(method.name) && "(Z)V".equals(method.desc),
                            "Callback landed in the wrong update overload");
                    require(isCall(instruction.getNext(), "org/lwjgl/opengl/Display", "swapBuffers", "()V"),
                            "Callback must execute immediately before the completed frame is swapped");
                }
            }
            require(callbacks == 1, "Expected exactly one callback in the final Display class");
            require(Arrays.equals(transformed, apply(loader, nodeType, install, transformed)),
                    "Applying the hook twice changed or duplicated the callback");

            ClassNode noUpdate = read(original);
            noUpdate.methods.removeIf(method -> "update".equals(method.name) && "(Z)V".equals(method.desc));
            expectRejected(loader, nodeType, install, write(noUpdate), "missing update(boolean)");
            ClassNode noSwap = read(original);
            for (MethodNode method : noSwap.methods) {
                if (!"update".equals(method.name) || !"(Z)V".equals(method.desc)) continue;
                for (AbstractInsnNode instruction : method.instructions.toArray()) {
                    if (isCall(instruction, "org/lwjgl/opengl/Display", "swapBuffers", "()V")) {
                        method.instructions.remove(instruction);
                    }
                }
            }
            expectRejected(loader, nodeType, install, write(noSwap), "missing swapBuffers");
            ClassNode duplicate = read(transformed);
            for (MethodNode method : duplicate.methods) {
                if ("update".equals(method.name) && "(Z)V".equals(method.desc)) {
                    method.instructions.insert(new MethodInsnNode(Opcodes.INVOKESTATIC,
                            callbackName, "create", "()V", false));
                }
            }
            expectRejected(loader, nodeType, install, write(duplicate), "duplicate callback");
        }
        System.out.println("PASS: Badlion hook in final payload, real LWJGL frame position, renamed callback, "
                + "native package sync, " + verifiedLinks + " internal method links, callback-free rollback, "
                + "idempotence, unsupported bytecode and duplicate rejection");
    }

    /**
     * Checks calls after the production rename pass. This catches the exact
     * failure that used to make settings vanish from the Badlion panel: a call
     * site retained one method name while the target declaration received a
     * different one, producing NoSuchMethodError only inside the injected JVM.
     */
    private static int verifyInternalMethodLinks(Path jarPath) throws Exception {
        Map<String, ClassNode> classes = new HashMap<>();
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().endsWith(".class")) continue;
                try (InputStream input = jar.getInputStream(entry)) {
                    ClassNode node = new ClassNode();
                    new ClassReader(input).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                    classes.put(node.name, node);
                }
            }
        }

        int checked = 0;
        for (ClassNode caller : classes.values()) {
            for (MethodNode method : caller.methods) {
                for (AbstractInsnNode instruction : method.instructions) {
                    if (!(instruction instanceof MethodInsnNode)) continue;
                    MethodInsnNode call = (MethodInsnNode)instruction;
                    if (!classes.containsKey(call.owner)) continue;
                    ++checked;
                    if (!resolvesInternalMethod(classes, call.owner, call.name, call.desc,
                            new HashSet<String>())) {
                        throw new AssertionError("Broken method link in final payload: "
                                + caller.name + '.' + method.name + method.desc + " -> "
                                + call.owner + '.' + call.name + call.desc);
                    }
                }
            }
        }
        return checked;
    }

    private static boolean resolvesInternalMethod(Map<String, ClassNode> classes, String owner,
            String name, String descriptor, Set<String> visited) {
        if (!visited.add(owner)) return false;
        ClassNode node = classes.get(owner);
        if (node == null) return resolvesExternalMethod(owner, name, descriptor);
        for (MethodNode method : node.methods) {
            if (name.equals(method.name) && descriptor.equals(method.desc)) return true;
        }
        if (node.superName != null
                && resolvesInternalMethod(classes, node.superName, name, descriptor, visited)) return true;
        if (node.interfaces != null) {
            for (String iface : node.interfaces) {
                if (resolvesInternalMethod(classes, iface, name, descriptor, visited)) return true;
            }
        }
        return false;
    }

    private static boolean resolvesExternalMethod(String owner, String name, String descriptor) {
        try {
            Class<?> type = Class.forName(owner.replace('/', '.'), false,
                    BadlionRenderHookProbe.class.getClassLoader());
            for (Class<?> current = type; current != null; current = current.getSuperclass()) {
                for (Method method : current.getDeclaredMethods()) {
                    if (name.equals(method.getName())
                            && descriptor.equals(Type.getMethodDescriptor(method))) return true;
                }
            }
            for (Class<?> iface : type.getInterfaces()) {
                if (resolvesExternalMethod(iface.getName().replace('.', '/'), name, descriptor)) return true;
            }
            return false;
        } catch (Throwable unavailable) {
            // Minecraft and Badlion classes are deliberately absent from this
            // headless test. Calls owned by those external classes are outside
            // the rename pass and cannot be validated here.
            return true;
        }
    }

    private static byte[] apply(ClassLoader loader, Class<?> nodeType, Method install,
                                byte[] bytes) throws Exception {
        Class<?> visitorType = loader.loadClass("lIlllIl.ClassVisitor");
        Class<?> readerType = loader.loadClass("lIlllIl.ClassReader");
        Class<?> writerType = loader.loadClass("lIlllIl.ClassWriter");
        Object node = nodeType.getConstructor().newInstance();
        Object reader = readerType.getConstructor(byte[].class).newInstance((Object) bytes);
        readerType.getMethod("accept", visitorType, int.class).invoke(reader, node, 0);
        install.invoke(null, node);
        Object writer = writerType.getConstructor(int.class).newInstance(0);
        nodeType.getMethod("accept", visitorType).invoke(node, writer);
        return (byte[]) writerType.getMethod("toByteArray").invoke(writer);
    }

    private static void expectRejected(ClassLoader loader, Class<?> nodeType, Method install,
                                       byte[] bytes, String scenario) throws Exception {
        try {
            apply(loader, nodeType, install, bytes);
        } catch (InvocationTargetException error) {
            require(error.getCause() instanceof IllegalStateException,
                    "Unexpected failure for " + scenario + ": " + error.getCause());
            return;
        }
        throw new AssertionError("Accepted " + scenario);
    }

    private static String renamed(Properties names, String suffix) {
        String name = names.getProperty("vegas/sony/" + suffix);
        require(name != null, "Missing rename map entry: " + suffix);
        return name;
    }

    private static byte[] resource(ClassLoader loader, String name) throws Exception {
        try (InputStream input = loader.getResourceAsStream(name)) {
            require(input != null, "Missing payload resource: " + name);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }

    private static ClassNode read(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        return node;
    }

    private static byte[] write(ClassNode node) {
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static boolean isCall(AbstractInsnNode instruction, String owner, String name, String descriptor) {
        if (!(instruction instanceof MethodInsnNode)) return false;
        MethodInsnNode call = (MethodInsnNode) instruction;
        return call.getOpcode() == Opcodes.INVOKESTATIC && owner.equals(call.owner)
                && name.equals(call.name) && descriptor.equals(call.desc);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

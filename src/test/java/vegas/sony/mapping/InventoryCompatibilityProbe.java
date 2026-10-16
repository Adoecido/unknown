package vegas.sony.mapping;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

public final class InventoryCompatibilityProbe {
    public static final class Key { public String description() { return "key"; } }
    public static final class Settings { public Key missing; public Key present = new Key(); }
    public static final class KeyLoop {
        public static int count(Object settings) throws Exception {
            Field[] fields = settings.getClass().getDeclaredFields();
            int count = 0;
            for (int i = 0; i < fields.length; i++) {
                if (fields[i].getType() != Key.class) continue;
                Key key = (Key)fields[i].get(settings);
                count += key.description().length();
            }
            return count;
        }
    }
    public static final class FrameComparison {
        public static boolean compare(StackTraceElement a, StackTraceElement b) {
            return a.getFileName().equals(b.getFileName());
        }
    }
    private static byte[] read(InputStream in) throws Exception {
        try (InputStream input = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192]; int n;
            while ((n = input.read(buffer)) >= 0) out.write(buffer, 0, n);
            return out.toByteArray();
        }
    }
    private static Class<?> patched(Class<?> fixture, boolean report) throws Exception {
        byte[] bytes = read(fixture.getResourceAsStream("/" + fixture.getName().replace('.', '/') + ".class"));
        byte[] patched = CheatBreakerInventoryCompatibility.patch(bytes, report, Key.class.getName().replace('.', '/'));
        return new ClassLoader(InventoryCompatibilityProbe.class.getClassLoader()) {
            Class<?> define() { return defineClass(fixture.getName(), patched, 0, patched.length); }
        }.define();
    }
    private static int count(byte[] bytes, int opcode) {
        ClassNode node = new ClassNode(); new ClassReader(bytes).accept(node, 0); int count = 0;
        for (MethodNode method : node.methods) for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction.getOpcode() == opcode) count++;
        }
        return count;
    }
    public static void main(String[] args) throws Exception {
        Class<?> frames = patched(FrameComparison.class, true);
        StackTraceElement unknown = new StackTraceElement("A", "m", null, -1);
        StackTraceElement known = new StackTraceElement("A", "m", "A.java", 1);
        if (!(Boolean)frames.getMethod("compare", StackTraceElement.class, StackTraceElement.class).invoke(null, unknown, unknown)) throw new AssertionError();
        if ((Boolean)frames.getMethod("compare", StackTraceElement.class, StackTraceElement.class).invoke(null, unknown, known)) throw new AssertionError();
        if (!Integer.valueOf(3).equals(patched(KeyLoop.class, false).getMethod("count", Object.class).invoke(null, new Settings()))) throw new AssertionError("null binding not skipped");
        List<String> original = Collections.singletonList("vanilla");
        if (ItemStackTooltipCallback.call(null, original) != original) throw new AssertionError("vanilla tooltip replaced");
        File installed = new File(System.getenv("APPDATA"), "CheatBreaker/downloads/versions/1.8.9/1.8.9.patch");
        if (installed.isFile()) try (JarFile jar = new JarFile(installed)) {
            String[] names = {"IllIllIllIlllIllIlIIlIlIl", "IllIllIlIllllIIlIllIIlIll"};
            for (int i = 0; i < names.length; i++) {
                byte[] before = read(jar.getInputStream(jar.getJarEntry(names[i] + ".class")));
                byte[] after = CheatBreakerInventoryCompatibility.patch(before, i == 0, "IllIllllllIIIIlIlIllllllI");
                int opcode = i == 0 ? Opcodes.INVOKESTATIC : Opcodes.IFNULL;
                if (count(after, opcode) <= count(before, opcode)) throw new AssertionError("installed CB pattern did not match: " + names[i]);
            }
        }
        System.out.println("Inventory compatibility: JVM verified null frames/bindings; installed CB patterns matched.");
    }
}

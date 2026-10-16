import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Static-only normalization for comparison; never executes the input classes. */
public final class NormalizeCheatBreaker implements Opcodes {
    static Integer integer(AbstractInsnNode n) {
        if (n == null) return null;
        int op = n.getOpcode();
        if (op >= ICONST_M1 && op <= ICONST_5) return op - ICONST_0;
        if (n instanceof IntInsnNode && (op == BIPUSH || op == SIPUSH)) return ((IntInsnNode)n).operand;
        if (n instanceof LdcInsnNode && ((LdcInsnNode)n).cst instanceof Integer) return (Integer)((LdcInsnNode)n).cst;
        return null;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("input.patch output.jar");
        Map<String, ClassNode> classes = new LinkedHashMap<>();
        try (JarFile jar = new JarFile(args[0])) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry e = entries.nextElement();
                if (!e.getName().endsWith(".class")) continue;
                ClassNode node = new ClassNode();
                try (InputStream in = jar.getInputStream(e)) {
                    new ClassReader(in).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                }
                classes.put(node.name, node);
            }
        }
        int folded = 0, restored = 0;
        for (ClassNode cls : classes.values()) for (MethodNode method : cls.methods) {
            boolean changed;
            do {
                changed = false;
                for (AbstractInsnNode n : method.instructions.toArray()) {
                    int op = n.getOpcode();
                    if (op != IAND && op != IOR && op != IXOR && op != IADD && op != ISUB && op != IMUL
                            && op != ISHL && op != ISHR && op != IUSHR) continue;
                    AbstractInsnNode b = n.getPrevious(), a = b == null ? null : b.getPrevious();
                    Integer x = integer(a), y = integer(b);
                    if (x == null || y == null) continue;
                    int value;
                    switch (op) {
                        case IAND: value = x & y; break;
                        case IOR: value = x | y; break;
                        case IXOR: value = x ^ y; break;
                        case IADD: value = x + y; break;
                        case ISUB: value = x - y; break;
                        case IMUL: value = x * y; break;
                        case ISHL: value = x << y; break;
                        case ISHR: value = x >> y; break;
                        default: value = x >>> y;
                    }
                    method.instructions.set(n, new LdcInsnNode(value));
                    method.instructions.remove(a);
                    method.instructions.remove(b);
                    folded++; changed = true;
                }
            } while (changed);
        }
        Map<String, Map<Integer, String>> tables = new HashMap<>();
        Set<String> holders = new HashSet<>();
        for (ClassNode cls : classes.values()) {
            if (cls.fields.size() != 1 || cls.methods.size() != 1) continue;
            FieldNode field = cls.fields.get(0);
            MethodNode init = cls.methods.get(0);
            if (!field.desc.equals("[Ljava/lang/String;") || !init.name.equals("<clinit>")) continue;
            Map<Integer, String> values = new HashMap<>();
            for (AbstractInsnNode n : init.instructions.toArray()) {
                if (n.getOpcode() != AASTORE) continue;
                AbstractInsnNode value = n.getPrevious();
                Integer index = integer(value == null ? null : value.getPrevious());
                if (index != null && value instanceof LdcInsnNode && ((LdcInsnNode)value).cst instanceof String)
                    values.put(index, (String)((LdcInsnNode)value).cst);
            }
            if (!values.isEmpty()) {
                tables.put(cls.name + "/" + field.name, values);
                holders.add(cls.name);
            }
        }
        for (ClassNode cls : classes.values()) for (MethodNode method : cls.methods) {
            for (AbstractInsnNode n : method.instructions.toArray()) {
                if (n.getOpcode() != AALOAD) continue;
                AbstractInsnNode indexNode = n.getPrevious();
                AbstractInsnNode fieldNode = indexNode == null ? null : indexNode.getPrevious();
                Integer index = integer(indexNode);
                if (index == null || !(fieldNode instanceof FieldInsnNode) || fieldNode.getOpcode() != GETSTATIC) continue;
                FieldInsnNode field = (FieldInsnNode)fieldNode;
                Map<Integer, String> table = tables.get(field.owner + "/" + field.name);
                if (table == null || !table.containsKey(index)) continue;
                method.instructions.set(n, new LdcInsnNode(table.get(index)));
                method.instructions.remove(indexNode);
                method.instructions.remove(fieldNode);
                restored++;
            }
        }
        Path output = Paths.get(args[1]);
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(output, StandardOpenOption.CREATE_NEW))) {
            for (ClassNode cls : classes.values()) {
                // Holder classes are intentionally retained for descriptor validation.
                // Replacements only reduce stack usage. Preserve original limits;
                // recomputing without the obfuscator's frames may underestimate them.
                ClassWriter writer = new ClassWriter(0);
                cls.accept(writer);
                jar.putNextEntry(new JarEntry(cls.name + ".class"));
                jar.write(writer.toByteArray());
                jar.closeEntry();
            }
        }
        System.out.printf("Normalized %d classes: %d constant expressions, %d string loads (%d tables)%n",
                classes.size(), folded, restored, holders.size());
    }
}

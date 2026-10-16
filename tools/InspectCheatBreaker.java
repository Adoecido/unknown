import java.io.PrintWriter;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

/** Print selected bytecode without initializing/loading game classes. */
public final class InspectCheatBreaker {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) throw new IllegalArgumentException("jar class [method-regex] [descriptor-regex]");
        try (JarFile jar = new JarFile(args[0])) {
            ClassNode cls = new ClassNode();
            new ClassReader(jar.getInputStream(jar.getJarEntry(args[1] + ".class")))
                    .accept(cls, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            for (MethodNode method : cls.methods) {
                if (args.length > 2 && !method.name.matches(args[2])) continue;
                if (args.length > 3 && !method.desc.matches(args[3])) continue;
                System.out.println(cls.name + "/" + method.name + " " + method.desc);
                Textifier text = new Textifier();
                method.accept(new TraceMethodVisitor(text));
                PrintWriter writer = new PrintWriter(System.out);
                text.print(writer);
                writer.flush();
            }
        }
    }
}

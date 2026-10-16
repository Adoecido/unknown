package vegas.sony.mapping;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import vegas.sony.reflect.CheatBreaker189Mappings;
import vegas.sony.runtime.ClassBytecodeCache;
import vegas.sony.runtime.NativeBridge;

/** Small, structurally checked repairs for the installed CB 1.8.9 client. */
public final class CheatBreakerInventoryCompatibility implements MappingTask {
    private final Class<?> target;
    private final boolean crashReport;
    private byte[] original;
    private byte[] patched;
    private boolean applied;

    private CheatBreakerInventoryCompatibility(Class<?> target, boolean crashReport) {
        this.target = target;
        this.crashReport = crashReport;
    }

    static void register(java.util.List<MappingTask> tasks) {
        if (!NativeBridge.isCheatBreaker189Runtime()) return;
        ClassLoader loader = MappedClasses.uP.getClassLoader();
        Class<?> report = CheatBreaker189Mappings.resolveClass("net.minecraft.crash.CrashReportCategory", loader);
        Class<?> keys = CheatBreaker189Mappings.resolveClass("IllIllIlIllllIIlIllIIlIll", loader);
        if (report != null) tasks.add(new CheatBreakerInventoryCompatibility(report, true));
        if (keys != null) tasks.add(new CheatBreakerInventoryCompatibility(keys, false));
    }

    public static byte[] patch(byte[] bytes, boolean crashReport, String keyClass) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        for (MethodNode method : node.methods) {
            if (crashReport) {
                boolean comparesFrames = false;
                for (AbstractInsnNode insn : method.instructions.toArray()) {
                    if (insn instanceof MethodInsnNode) {
                        MethodInsnNode call = (MethodInsnNode)insn;
                        comparesFrames |= call.owner.equals("java/lang/StackTraceElement") && call.name.equals("getFileName");
                    }
                }
                if (!comparesFrames) continue;
                for (AbstractInsnNode insn : method.instructions.toArray()) {
                    if (!(insn instanceof MethodInsnNode)) continue;
                    MethodInsnNode call = (MethodInsnNode)insn;
                    if (call.owner.equals("java/lang/String") && call.name.equals("equals")
                            && call.desc.equals("(Ljava/lang/Object;)Z")) {
                        method.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC,
                                "java/util/Objects", "equals", "(Ljava/lang/Object;Ljava/lang/Object;)Z", false));
                    }
                }
            } else {
                for (AbstractInsnNode insn : method.instructions.toArray()) {
                    if (!(insn instanceof TypeInsnNode) || insn.getOpcode() != Opcodes.CHECKCAST
                            || !((TypeInsnNode)insn).desc.equals(keyClass)) continue;
                    AbstractInsnNode before = previousInstruction(insn);
                    AbstractInsnNode after = nextInstruction(insn);
                    if (!(before instanceof MethodInsnNode) || !((MethodInsnNode)before).owner.equals("java/lang/reflect/Field")
                            || !((MethodInsnNode)before).name.equals("get") || !(after instanceof VarInsnNode)
                            || after.getOpcode() != Opcodes.ASTORE) continue;
                    // The existing loop continuation is already a framed branch target.
                    for (AbstractInsnNode tail = after.getNext(); tail != null; tail = tail.getNext()) {
                        if (!(tail instanceof IincInsnNode)) continue;
                        LabelNode continuation = null;
                        for (AbstractInsnNode prev = tail.getPrevious(); prev != null && prev.getOpcode() < 0; prev = prev.getPrevious()) {
                            if (prev instanceof LabelNode) continuation = (LabelNode)prev;
                        }
                        if (continuation != null) {
                            InsnList guard = new InsnList();
                            guard.add(new VarInsnNode(Opcodes.ALOAD, ((VarInsnNode)after).var));
                            guard.add(new JumpInsnNode(Opcodes.IFNULL, continuation));
                            method.instructions.insert(after, guard);
                        }
                        break;
                    }
                }
            }
        }
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static AbstractInsnNode nextInstruction(AbstractInsnNode node) {
        do { node = node.getNext(); } while (node != null && node.getOpcode() < 0);
        return node;
    }
    private static AbstractInsnNode previousInstruction(AbstractInsnNode node) {
        do { node = node.getPrevious(); } while (node != null && node.getOpcode() < 0);
        return node;
    }
    @Override public void prepare() { original = ClassBytecodeCache.getClassBytecode(target, true); }
    @Override public void transform() { patched = patch(original, crashReport, MappedClasses.DR.getName().replace('.', '/')); }
    @Override public void serialize() { }
    @Override public int commit() {
        int result = ClassBytecodeCache.setClassBytecode(target, patched);
        applied = result == 0;
        return result;
    }
    @Override public void rollback() { if (applied) ClassBytecodeCache.setClassBytecode(target, original); }
    @Override public boolean isApplied() { return applied; }
    @Override public Class<?> getTargetClass() { return target; }
}

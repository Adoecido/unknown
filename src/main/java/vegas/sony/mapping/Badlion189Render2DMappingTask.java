package vegas.sony.mapping;

import vegas.sony.asm.transform.ClassTransformer;
import vegas.sony.event.impl.EventRender2D;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Renders immediately before LWJGL swaps Badlion 1.8.9's completed frame. */
public final class Badlion189Render2DMappingTask extends ClassTransformer {
    private static final String DISPLAY_CLASS = "org.lwjgl.opengl.Display";
    private static final String DISPLAY_OWNER = "org/lwjgl/opengl/Display";
    // Resolve the actual class name: the injection build renames product classes,
    // but cannot rewrite an owner embedded as a string in a runtime ASM hook.
    private static final String CALLBACK_OWNER =
            EventRender2D.class.getName().replace('.', '/');

    public Badlion189Render2DMappingTask() {
        super(resolveDisplayClass());
    }

    @Override
    public void transform() {
        installCallback(this.classNode);
    }

    static void installCallback(ClassNode displayClass) {
        MethodNode updateMethod = null;
        for (MethodNode method : displayClass.methods) {
            if ("update".equals(method.name) && "(Z)V".equals(method.desc)) {
                updateMethod = method;
                break;
            }
        }
        if (updateMethod == null) {
            throw new IllegalStateException(
                    "LWJGL Display.update(boolean) is unavailable");
        }

        int callbackCount = countCallbacks(updateMethod);
        if (callbackCount > 1) {
            throw new IllegalStateException(
                    "Expected one Badlion display callback, found " + callbackCount);
        }
        if (callbackCount == 1) {
            return;
        }
        for (AbstractInsnNode instruction = updateMethod.instructions.getFirst();
             instruction != null; instruction = instruction.getNext()) {
            if (!(instruction instanceof MethodInsnNode)) {
                continue;
            }
            MethodInsnNode call = (MethodInsnNode)instruction;
            if (call.getOpcode() == Opcodes.INVOKESTATIC
                    && DISPLAY_OWNER.equals(call.owner)
                    && "swapBuffers".equals(call.name)
                    && "()V".equals(call.desc)) {
                updateMethod.instructions.insertBefore(call,
                        new MethodInsnNode(Opcodes.INVOKESTATIC,
                                CALLBACK_OWNER, "create", "()V", false));
                return;
            }
        }
        throw new IllegalStateException(
                "LWJGL Display.update(boolean) does not call swapBuffers()");
    }

    private static int countCallbacks(MethodNode method) {
        int count = 0;
        for (AbstractInsnNode instruction = method.instructions.getFirst();
             instruction != null; instruction = instruction.getNext()) {
            if (!(instruction instanceof MethodInsnNode)) {
                continue;
            }
            MethodInsnNode call = (MethodInsnNode)instruction;
            if (call.getOpcode() == Opcodes.INVOKESTATIC
                    && CALLBACK_OWNER.equals(call.owner)
                    && "create".equals(call.name)
                    && "()V".equals(call.desc)) {
                ++count;
            }
        }
        return count;
    }

    private static Class<?> resolveDisplayClass() {
        try {
            return Class.forName(DISPLAY_CLASS, false,
                    Badlion189Render2DMappingTask.class.getClassLoader());
        }
        catch (ClassNotFoundException | LinkageError error) {
            throw new IllegalStateException(
                    "Unable to resolve Badlion's runtime LWJGL Display class", error);
        }
    }
}

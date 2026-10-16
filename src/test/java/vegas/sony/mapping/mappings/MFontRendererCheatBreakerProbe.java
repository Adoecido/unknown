package vegas.sony.mapping.mappings;

import java.lang.reflect.Field;
import sun.misc.Unsafe;
import vegas.sony.mapping.MappingMethod;

/** Exercises coordinate boxing without starting Minecraft or initializing the mappings. */
public final class MFontRendererCheatBreakerProbe {
    private static final class CapturingMethod extends MappingMethod {
        Object[] arguments;

        private CapturingMethod() {
            super(null, Object.class, "unused", false, false, false, Integer.TYPE);
        }

        @Override
        public int invokeInt(Object receiver, Object... arguments) {
            this.arguments = arguments;
            return 42;
        }
    }

    public static void main(String[] args) throws Exception {
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Unsafe unsafe = (Unsafe) unsafeField.get(null);
        MFontRenderer renderer = (MFontRenderer) unsafe.allocateInstance(MFontRenderer.class);
        CapturingMethod method = (CapturingMethod) unsafe.allocateInstance(CapturingMethod.class);
        renderer.h = method;
        if (renderer.u(null, "test", 12, 34, 0x123456) != 42
                || !(method.arguments[1] instanceof Integer) || !(method.arguments[2] instanceof Integer)) {
            throw new AssertionError("Vanilla integer-coordinate call changed");
        }
        Field floatCoordinates = MFontRenderer.class.getDeclaredField("drawStringUsesFloatCoordinates");
        floatCoordinates.setAccessible(true);
        floatCoordinates.setBoolean(renderer, true);
        if (renderer.u(null, "test", 12, 34, 0x123456) != 42
                || !Float.valueOf(12).equals(method.arguments[1])
                || !Float.valueOf(34).equals(method.arguments[2])
                || !Integer.valueOf(0x123456).equals(method.arguments[3])) {
            throw new AssertionError("CheatBreaker coordinates must be Float; color must remain Integer");
        }
        System.out.println("PASS: vanilla and CheatBreaker drawString argument types");
    }
}

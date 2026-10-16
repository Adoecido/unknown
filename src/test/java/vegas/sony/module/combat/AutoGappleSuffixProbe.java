package vegas.sony.module.combat;

/** Verifies the ArrayList suffix reports time until the next apple is used. */
public final class AutoGappleSuffixProbe {
    public static void main(String[] args) {
        assertSuffix(200, 100, "5s");
        assertSuffix(130, 100, "1.5s");
        assertSuffix(101, 100, "0.1s");
        assertSuffix(80, 100, "0s");
        System.out.println("Gapple suffix reports time until eating.");
    }

    private static void assertSuffix(int regenerationTicks, int triggerTicks,
            String expected) {
        String actual = AutoGapple.formatTicksUntilEat(regenerationTicks,
                triggerTicks);
        if (!expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }
}

package vegas.sony.module.combat;

import vegas.sony.module.Category;
import vegas.sony.module.Mod;
import vegas.sony.module.rise.RiseVelocity;
import vegas.sony.unmap.ModeSelection;
import vegas.sony.value.ModeValue;
import vegas.sony.value.NumberValue;
import vegas.sony.value.Value;

/** Verifies that the two adapted modules expose the intended stable config surface. */
public final class OpenExpoCombatModulesProbe {
    public static void main(String[] args) {
        assertModule(new SprintReset(), "SprintReset",
                "Mode", "Interval", "Require target damage", "Duration");
        RiseVelocity velocity = new RiseVelocity();
        assertModule(velocity, "Velocity", "Mode", "Chance", "Velocity percentage",
                "Require sprinting", "Disable while holding S");
        assertModesFirst(velocity);
        assertVelocityReduceMode(velocity);
        assertModeVisibility(velocity);
        assertVelocityPercentageRange(velocity);
        Criticals criticals = new Criticals();
        assertModule(criticals, "Criticals", "Mode", "Variação de ping", "Chance");
        assertModeOptions(criticals, "Lag", "Timer");
        System.out.println("SprintReset, Velocity Reduce and Criticals registration surface passed.");
    }

    private static void assertVelocityPercentageRange(RiseVelocity velocity) {
        for (Value<?, ?> value : velocity.getAllValues()) {
            if (!(value instanceof NumberValue)
                    || !"Velocity percentage".equals(clean(value.getName()))) continue;
            NumberValue percentage = (NumberValue)value;
            check(percentage.getMinimum() == 1.0, "Velocity percentage minimum must be 1");
            check(percentage.getMaximum() == 100.0, "Velocity percentage maximum must be 100");
            return;
        }
        throw new AssertionError("Velocity is missing the percentage control");
    }

    private static void assertVelocityReduceMode(RiseVelocity velocity) {
        for (Value<?, ?> value : velocity.getAllValues()) {
            if (!(value instanceof ModeValue) || !clean(value.getName()).equals("Mode")) continue;
            for (ModeSelection option : ((ModeValue)value).getModes()) {
                if ("Reduce".equals(clean(option.getName()))) return;
            }
        }
        throw new AssertionError("Velocity is missing the Reduce mode");
    }

    private static void assertModeOptions(Mod module, String... expectedModes) {
        for (Value<?, ?> value : module.getAllValues()) {
            if (!(value instanceof ModeValue) || !"Mode".equals(clean(value.getName()))) continue;
            ModeValue mode = (ModeValue)value;
            for (String expected : expectedModes) {
                boolean found = false;
                for (ModeSelection option : mode.getModes()) {
                    if (expected.equals(clean(option.getName()))) {
                        found = true;
                        break;
                    }
                }
                check(found, module.getName() + " is missing mode " + expected);
            }
            return;
        }
        throw new AssertionError(module.getName() + " is missing its Mode value");
    }

    private static void assertModeVisibility(RiseVelocity velocity) {
        ModeValue mode = null;
        for (Value<?, ?> value : velocity.getAllValues()) {
            if (value instanceof ModeValue && clean(value.getName()).equals("Mode")) {
                mode = (ModeValue)value;
                break;
            }
        }
        if (mode == null) throw new AssertionError("Velocity is missing its Mode value");
        mode.setPersistenceSuppressed(true);
        try {
            for (ModeSelection option : mode.getModes()) {
                mode.setValue(option);
                int activeBranchValues = 0;
                for (Value<?, ?> value : velocity.getAllValues()) {
                    if (!(value instanceof ModeValue) && value.areConditionsMet()
                            && value.getParent() == mode) activeBranchValues++;
                }
                String selected = clean(option.getName());
                int expected = "Reduce".equals(selected) ? 4
                        : "MMC".equals(selected) ? 0 : 2;
                check(activeBranchValues == expected,
                        "Velocity mode " + selected + " exposed " + activeBranchValues
                                + " branch values instead of " + expected);
            }
        } finally {
            mode.setPersistenceSuppressed(false);
        }
    }

    private static void assertModesFirst(Mod module) {
        boolean nonModeSeen = false;
        for (Value<?, ?> value : module.getAllValues()) {
            if (value instanceof ModeValue) {
                check(!nonModeSeen, module.getName() + " registered a mode after another value");
            } else {
                nonModeSeen = true;
            }
        }
    }

    private static void assertModule(Mod module, String name, String... values) {
        check(clean(module.getName()).equals(name), "unexpected module name: " + module.getName());
        check(module.getCategory() == Category.COMBAT, name + " is not in Combat");
        for (String expected : values) {
            boolean found = false;
            for (Value<?, ?> value : module.getAllValues()) {
                if (clean(value.getName()).equals(expected)) {
                    found = true;
                    break;
                }
            }
            check(found, name + " is missing " + expected);
        }
    }

    private static String clean(String value) {
        return value.replace(" //$", "");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

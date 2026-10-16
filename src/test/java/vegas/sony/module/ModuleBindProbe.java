package vegas.sony.module;

/** Verifies that a stored module bind invokes the module's real toggle path. */
public final class ModuleBindProbe {
    public static void main(String[] args) {
        Mod module = new Mod("Bind probe", 0);
        module.getBind().getBoundInputs().add(75); // K in the Win32 virtual-key contract

        if (!module.getBind().handleInput(75, true) || !module.isEnabled()) {
            throw new AssertionError("key bind did not enable its module");
        }
        module.getBind().handleInput(75, false);
        if (!module.getBind().handleInput(75, true) || module.isEnabled()) {
            throw new AssertionError("key bind did not disable its module");
        }

        module.getBind().getBoundInputs().clear();
        module.getBind().getBoundInputs().add(-97); // Mouse 4
        Mod otherMouseModule = new Mod("Other mouse bind probe", 0);
        otherMouseModule.getBind().getBoundInputs().add(-96); // Mouse 5
        if (!module.getBind().handleInput(-97, true) || !module.isEnabled()) {
            throw new AssertionError("mouse bind did not enable its module");
        }
        if (otherMouseModule.getBind().handleInput(-97, true) || otherMouseModule.isEnabled()) {
            throw new AssertionError("Mouse 4 incorrectly toggled the Mouse 5 module");
        }
        System.out.println("Module key and mouse binds toggle the real module state.");
    }
}

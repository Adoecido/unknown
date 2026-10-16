package vegas.sony.wrapper.impl;

import vegas.sony.SonyVegas;
import vegas.sony.config.ClientSettings;
import vegas.sony.wrapper.Wrapper;

public class KeyBinding
extends Wrapper {

    public void setPressed(boolean bl) {
        if (bl) {
            this.I();
        } else {
            this.e();
        }
    }

    public static void H() {
        SonyVegas.INSTANCE.getMappings().hJ.R();
    }

    public void Z() {
        boolean bl;
        boolean bl2 = ClientSettings.isPhysicalKeyDown(this);
        boolean bl3 = bl = this.isKeyDown() != bl2;
        if (bl) {
            this.setPressed(bl2);
        }
    }

    public boolean u() {
        return KeyBinding.sonyVegasInstance.getMappings().hJ.a(this.I);
    }

    public void e() {
        if (ForgeVersion.MC_1_16_5.d()) {
            KeyBinding.sonyVegasInstance.getMappings().hJ.S(this.I);
            return;
        }
        this.onTick(0);
        KeyBinding.setKeyBindState(this, false);
    }

    public boolean isKeyDown() {
        return KeyBinding.sonyVegasInstance.getMappings().hJ.S$src$Z$wvm5qt(this.I);
    }

    public static void setKeyBindState(KeyBinding keyBinding, boolean bl) {
        if (ForgeVersion.MC_1_16_5.d()) {
            KeyBinding.sonyVegasInstance.getMappings().hJ.d(keyBinding.u$src$Lvegas_sony_wrapper_impl_InputMappingsInput_$1c10gpv().getObject(), bl);
            return;
        }
        KeyBinding.sonyVegasInstance.getMappings().hJ.g(keyBinding.getKeyCode(), bl);
    }

    public boolean isPressed() {
        return KeyBinding.sonyVegasInstance.getMappings().hJ.M(this.I);
    }

    public void onTick(int n) {
        KeyBinding.sonyVegasInstance.getMappings().hJ.q(this.I, n);
    }

    public KeyBinding(Object object) {
        super(object);
    }

    public int getKeyCode() {
        if (ForgeVersion.MC_1_16_5.d()) {
            return this.u$src$Lvegas_sony_wrapper_impl_InputMappingsInput_$1c10gpv().getKeyCode();
        }
        return KeyBinding.sonyVegasInstance.getMappings().hJ.B(this.I);
    }

    private InputMappingsInput u$src$Lvegas_sony_wrapper_impl_InputMappingsInput_$1c10gpv() {
        return new InputMappingsInput(KeyBinding.sonyVegasInstance.getMappings().hJ.l(this.I));
    }

    public static void onTick(KeyBinding keyBinding) {
        if (ForgeVersion.MC_1_16_5.d()) {
            KeyBinding.sonyVegasInstance.getMappings().hJ.x(keyBinding.u$src$Lvegas_sony_wrapper_impl_InputMappingsInput_$1c10gpv().getObject());
            return;
        }
        KeyBinding.sonyVegasInstance.getMappings().hJ.s(keyBinding.getKeyCode());
    }

    public void setCancelled(boolean bl) {
        KeyBinding.sonyVegasInstance.getMappings().hJ.o(this.I, bl);
    }

    public void I() {
        KeyBinding.setKeyBindState(this, true);
        KeyBinding.onTick(this);
    }

    public int V() {
        return KeyBinding.sonyVegasInstance.getMappings().hJ.n(this.I);
    }
}


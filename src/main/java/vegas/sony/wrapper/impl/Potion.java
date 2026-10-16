package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MPotion;
import vegas.sony.wrapper.Wrapper;

import java.util.ArrayList;

public class Potion
extends Wrapper {
    public Potion(Object object) {
        super(object);
    }

    public boolean n() {
        if (ForgeVersion.MC_1_16_5.d()) {
            for (Object e : Potion.sonyVegasInstance.getMappings().qU.W(this.I)) {
                PotionEffect potionEffect = new PotionEffect(e);
                if (potionEffect.i().p()) continue;
                return true;
            }
            return false;
        }
        return Potion.sonyVegasInstance.getMappings().qU.E(this.I);
    }

    public int getId() {
        if (ForgeVersion.MC_1_16_5.d()) {
            return -1;
        }
        if (ForgeVersion.MC_1_12_2.d()) {
            return MPotion.R(Potion.sonyVegasInstance.getMappings().qU, this.getObject());
        }
        return MPotion.g(Potion.sonyVegasInstance.getMappings().qU, this.I);
    }

    public boolean isBadEffect() {
        return MPotion.u(Potion.sonyVegasInstance.getMappings().qU, this.I);
    }

    public static Potion[] getPotionTypes() {
        if (ForgeVersion.MC_1_12_2.d()) {
            Iterable iterable = (Iterable)MPotion.x(Potion.sonyVegasInstance.getMappings().qU);
            ArrayList<Potion> arrayList = new ArrayList<Potion>();
            arrayList.add(null);
            for (Object t : iterable) {
                Potion potion = new Potion(t);
                arrayList.add(potion);
            }
            return arrayList.toArray(new Potion[arrayList.size()]);
        }
        Object[] objectArray = MPotion.i(Potion.sonyVegasInstance.getMappings().qU);
        Potion[] potionArray = new Potion[objectArray.length];
        for (int i = 0; i < objectArray.length; ++i) {
            potionArray[i] = new Potion(objectArray[i]);
        }
        return potionArray;
    }


    public int y() {
        if (ForgeVersion.MC_1_16_5.d()) {
            return -1;
        }
        return MPotion.H(Potion.sonyVegasInstance.getMappings().qU, this.I);
    }

    public String y$src$Ljava_lang_String_$yl6pfj() {
        return MPotion.q(Potion.sonyVegasInstance.getMappings().qU, this.I);
    }

    public static Potion getPotionById(int n) {
        if (ForgeVersion.MC_1_12_2.d()) {
            return new Potion(MPotion.Q(Potion.sonyVegasInstance.getMappings().qU, n));
        }
        return Potion.getPotionTypes()[n];
    }
}


package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MItem;
import vegas.sony.wrapper.Wrapper;

import java.util.List;
import java.util.Map;

public class Item
extends Wrapper {
    private static String T;

    public void D(Item item, List list) {
        if (ForgeVersion.MC_1_20_6.d()) {
            return;
        }
        MItem.V(Item.sonyVegasInstance.getMappings().CF, this.I, item.getObject(), TooltipFlagBridge.searchTab().getObject(), list);
    }

    private static Throwable a(Throwable throwable) {
        return throwable;
    }

    public static Item T(int n) {
        return new Item(MItem.W(Item.sonyVegasInstance.getMappings().CF, n));
    }

    public String O() {
        return MItem.k(Item.sonyVegasInstance.getMappings().CF, this.getObject());
    }

    public static Map y() {
        if (ForgeVersion.MC_1_8_9.d()) {
            return MItem.U(Item.sonyVegasInstance.getMappings().CF);
        }
        return null;
    }

    public boolean p() {
        if (ForgeVersion.MC_1_16_5.d()) {
            return false;
        }
        return MItem.N(Item.sonyVegasInstance.getMappings().CF, this.I);
    }

    public Holder Q() {
        return new Holder(MItem.F(Item.sonyVegasInstance.getMappings().CF, this.I));
    }

    public int P() {
        return Item.f(this);
    }

    public Item(Object object) {
        super(object);
    }

    static {
        if (Item.L() == null) {
            Item.H("fCfOlb");
        }
    }

    public static RegistryNamespaced Y() {
        return new RegistryNamespaced(MItem.Z(Item.sonyVegasInstance.getMappings().CF));
    }

    public static String L() {
        return T;
    }

    public static int f(Item item) {
        return MItem.l(Item.sonyVegasInstance.getMappings().CF, item.getObject());
    }

    public static Item L(String string) {
        if (ForgeVersion.MC_1_16_5.d()) {
            ResourceLocation resourceLocation = ResourceLocation.create(string);
            Object object = Item.Y().S(resourceLocation.getObject());
            return object == null ? null : new Item(object);
        }
        if (ForgeVersion.MC_1_7_10.L()) {
            try {
                int n = Integer.parseInt(string);
                return Item.T(n);
            }
            catch (NumberFormatException numberFormatException) {
                return null;
            }
        }
        Object object = MItem.r(Item.sonyVegasInstance.getMappings().CF, string);
        return object == null ? null : new Item(object);
    }

    public int a() {
        if (ForgeVersion.MC_1_20_6.d()) {
            return this.b().y();
        }
        return MItem.d(Item.sonyVegasInstance.getMappings().CF, this.I);
    }

    public DataComponentMap g() {
        return new DataComponentMap(MItem.K(Item.sonyVegasInstance.getMappings().CF, this.I));
    }

    public int I(ItemStack itemStack, EntityLivingBase entityLivingBase) {
        if (ForgeVersion.MC_1_21_4.d()) {
            return Item.sonyVegasInstance.getMappings().CF.c(this.I, itemStack.getObject(), entityLivingBase.getObject());
        }
        return MItem.Q(Item.sonyVegasInstance.getMappings().CF, this.I, itemStack.getObject());
    }

    public String A() {
        return MItem.X(Item.sonyVegasInstance.getMappings().CF, this.I);
    }

    public String h(ItemStack itemStack) {
        if (ForgeVersion.MC_1_16_5.d()) {
            ITextComponent iTextComponent = new ITextComponent(MItem.h(Item.sonyVegasInstance.getMappings().CF, this.getObject(), itemStack.getObject()));
            return iTextComponent.getFormattedText();
        }
        return MItem.h(Item.sonyVegasInstance.getMappings().CF, this.getObject(), itemStack.getObject()).toString();
    }

    public static void H(String string) {
        T = string;
    }

    public String Y$src$Ljava_lang_String_$uel3xb() {
        if (ForgeVersion.MC_1_21_0.d()) {
            return ITextComponent.P(this.A()).getFormattedText();
        }
        ITextComponent iTextComponent = new ITextComponent(MItem.i(Item.sonyVegasInstance.getMappings().CF, this.I));
        return iTextComponent.getFormattedText();
    }

    public ItemStack b() {
        try {
            return new ItemStack(MItem.z(Item.sonyVegasInstance.getMappings().CF, this.I));
        }
        catch (Throwable throwable) {
            if (ForgeVersion.MC_26_1.d()) {
                return ItemStack.S(this);
            }
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException)throwable;
            }
            if (throwable instanceof Error) {
                throw (Error)throwable;
            }
            throw new RuntimeException(throwable);
        }
    }

    public String getItemStackDisplayName(ItemStack itemStack) {
        return MItem.o(Item.sonyVegasInstance.getMappings().CF, this.getObject(), itemStack.getObject());
    }
}

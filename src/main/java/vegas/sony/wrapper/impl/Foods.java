package vegas.sony.wrapper.impl;

import vegas.sony.wrapper.Wrapper;

public class Foods
extends Wrapper {
    public Foods(Object handle) {
        super(handle);
    }

    public static FoodProperties apple() {
        return wrap(Foods.sonyVegasInstance.getMappings().foods.apple());
    }

    public static FoodProperties bakedPotato() {
        return wrap(Foods.sonyVegasInstance.getMappings().foods.bakedPotato());
    }

    public static FoodProperties beef() {
        return wrap(Foods.sonyVegasInstance.getMappings().foods.beef());
    }

    public static FoodProperties beetroot() {
        return wrap(Foods.sonyVegasInstance.getMappings().foods.beetroot());
    }

    public static FoodProperties beetrootSoup() {
        return wrap(Foods.sonyVegasInstance.getMappings().foods.beetrootSoup());
    }

    public static FoodProperties bread() {
        return wrap(Foods.sonyVegasInstance.getMappings().foods.bread());
    }

    public static FoodProperties carrot() {
        return wrap(Foods.sonyVegasInstance.getMappings().foods.carrot());
    }

    public static FoodProperties chicken() {
        return wrap(Foods.sonyVegasInstance.getMappings().foods.chicken());
    }

    public static FoodProperties chorusFruit() {
        return wrap(Foods.sonyVegasInstance.getMappings().foods.chorusFruit());
    }

    public static FoodProperties cod() {
        return wrap(Foods.sonyVegasInstance.getMappings().foods.cod());
    }

    public static FoodProperties cookedBeef() {
        return wrap(Foods.sonyVegasInstance.getMappings().foods.cookedBeef());
    }

    public static FoodProperties cookedChicken() {
        return wrap(Foods.sonyVegasInstance.getMappings().foods.cookedChicken());
    }

    public static FoodProperties cookedCod() {
        return wrap(Foods.sonyVegasInstance.getMappings().foods.cookedCod());
    }

    public static FoodProperties cookedMutton() {
        return wrap(Foods.sonyVegasInstance.getMappings().foods.cookedMutton());
    }

    public static FoodProperties cookedPorkchop() {
        return wrap(Foods.sonyVegasInstance.getMappings().foods.cookedPorkchop());
    }

    public static FoodProperties cookedRabbit() {
        return wrap(Foods.sonyVegasInstance.getMappings().foods.cookedRabbit());
    }

    public static FoodProperties cookedSalmon() {
        return wrap(Foods.sonyVegasInstance.getMappings().foods.cookedSalmon());
    }

    public static FoodProperties cookie() {
        return wrap(Foods.sonyVegasInstance.getMappings().foods.cookie());
    }

    public static FoodProperties driedKelp() {
        return wrap(Foods.sonyVegasInstance.getMappings().foods.driedKelp());
    }

    public static FoodProperties enchantedGoldenApple() {
        return wrap(Foods.sonyVegasInstance.getMappings().foods.enchantedGoldenApple());
    }

    public static FoodProperties goldenApple() {
        return wrap(Foods.sonyVegasInstance.getMappings().foods.goldenApple());
    }

    public static FoodProperties goldenCarrot() {
        return wrap(Foods.sonyVegasInstance.getMappings().foods.goldenCarrot());
    }

    public static FoodProperties honeyBottle() {
        return wrap(Foods.sonyVegasInstance.getMappings().foods.honeyBottle());
    }

    public static FoodProperties melonSlice() {
        return wrap(Foods.sonyVegasInstance.getMappings().foods.melonSlice());
    }

    public static FoodProperties mushroomStew() {
        return wrap(Foods.sonyVegasInstance.getMappings().foods.mushroomStew());
    }

    public static FoodProperties mutton() {
        return wrap(Foods.sonyVegasInstance.getMappings().foods.mutton());
    }

    public static FoodProperties poisonousPotato() {
        return wrap(Foods.sonyVegasInstance.getMappings().foods.poisonousPotato());
    }

    public static FoodProperties porkchop() {
        return wrap(Foods.sonyVegasInstance.getMappings().foods.porkchop());
    }

    public static FoodProperties potato() {
        return wrap(Foods.sonyVegasInstance.getMappings().foods.potato());
    }

    public static FoodProperties pufferfish() {
        return wrap(Foods.sonyVegasInstance.getMappings().foods.pufferfish());
    }

    public static FoodProperties pumpkinPie() {
        return wrap(Foods.sonyVegasInstance.getMappings().foods.pumpkinPie());
    }

    public static FoodProperties rabbit() {
        return wrap(Foods.sonyVegasInstance.getMappings().foods.rabbit());
    }

    public static FoodProperties rabbitStew() {
        return wrap(Foods.sonyVegasInstance.getMappings().foods.rabbitStew());
    }

    public static FoodProperties rottenFlesh() {
        return wrap(Foods.sonyVegasInstance.getMappings().foods.rottenFlesh());
    }

    public static FoodProperties salmon() {
        return wrap(Foods.sonyVegasInstance.getMappings().foods.salmon());
    }

    public static FoodProperties spiderEye() {
        return wrap(Foods.sonyVegasInstance.getMappings().foods.spiderEye());
    }

    public static FoodProperties suspiciousStew() {
        return wrap(Foods.sonyVegasInstance.getMappings().foods.suspiciousStew());
    }

    public static FoodProperties sweetBerries() {
        return wrap(Foods.sonyVegasInstance.getMappings().foods.sweetBerries());
    }

    public static FoodProperties glowBerries() {
        return wrap(Foods.sonyVegasInstance.getMappings().foods.glowBerries());
    }

    public static FoodProperties tropicalFish() {
        return wrap(Foods.sonyVegasInstance.getMappings().foods.tropicalFish());
    }

    private static FoodProperties wrap(Object handle) {
        return new FoodProperties(handle);
    }
}

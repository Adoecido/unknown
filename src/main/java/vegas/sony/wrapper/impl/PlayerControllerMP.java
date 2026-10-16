package vegas.sony.wrapper.impl;

import vegas.sony.mapping.mappings.MPlayerControllerMP;
import vegas.sony.wrapper.Wrapper;

public class PlayerControllerMP
extends Wrapper {
    public void Q() {
        MPlayerControllerMP.v(PlayerControllerMP.sonyVegasInstance.getMappingsMapperCompat().hj, this.I);
    }

    public void attackEntity(EntityPlayer entityPlayer, Entity entity) {
        MPlayerControllerMP.attackEntity(PlayerControllerMP.sonyVegasInstance.getMappingsMapperCompat().hj, this.I, entityPlayer.getObject(), entity.getObject());
    }

    public void onStoppedUsingItem(EntityPlayer entityPlayer) {
        PlayerControllerMP.sonyVegasInstance.getMappingsMapperCompat().hj.onStoppedUsingItem(this.I, entityPlayer.getObject());
    }

    public void B() {
        PlayerControllerMP.sonyVegasInstance.getMappingsMapperCompat().hj.o(this.I);
    }


    public boolean a() {
        return MPlayerControllerMP.K(PlayerControllerMP.sonyVegasInstance.getMappingsMapperCompat().hj, this.I);
    }

    public float c() {
        return MPlayerControllerMP.l(PlayerControllerMP.sonyVegasInstance.getMappingsMapperCompat().hj, this.I);
    }

    public ItemStack O(int n, int n2, int n3, int n4, EntityPlayer entityPlayer) {
        if (ForgeVersion.MC_1_17.d()) {
            PlayerControllerMP.sonyVegasInstance.getMappingsMapperCompat().hj.N(this.I, n, n2, n3, ClickType.VALUES[n4].getObject(), entityPlayer.getObject());
            return null;
        }
        if (ForgeVersion.MC_1_12_2.d()) {
            return new ItemStack(PlayerControllerMP.sonyVegasInstance.getMappingsMapperCompat().hj.N(this.I, n, n2, n3, ClickType.VALUES[n4].getObject(), entityPlayer.getObject()));
        }
        return new ItemStack(PlayerControllerMP.sonyVegasInstance.getMappingsMapperCompat().hj.S(this.I, n, n2, n3, n4, entityPlayer.getObject()));
    }

    public PlayerControllerMP(Object object) {
        super(object);
    }

    public boolean X() {
        return false;
    }

    public float N() {
        if (ForgeVersion.MC_1_20_6.d()) {
            return PlayerControllerMP.sonyVegasInstance.getMappingsMapperCompat().hj.z(Minecraft.a_xH_J().getObject());
        }
        return PlayerControllerMP.sonyVegasInstance.getMappingsMapperCompat().hj.z(this.I);
    }

    public NetHandlerPlayClientImpl n() {
        return new NetHandlerPlayClientImpl(MPlayerControllerMP.w(PlayerControllerMP.sonyVegasInstance.getMappingsMapperCompat().hj, this.I));
    }

    public boolean sendUseItem(EntityPlayer entityPlayer, World world, ItemStack itemStack) {
        if (ForgeVersion.MC_1_12_2.d()) {
            EnumActionResult enumActionResult = new EnumActionResult(PlayerControllerMP.sonyVegasInstance.getMappingsMapperCompat().hj.H(this.I, entityPlayer.getObject(), world.getObject(), EnumHand.mainHand().getObject()));
            return enumActionResult.equals(EnumActionResult.success());
        }
        return PlayerControllerMP.sonyVegasInstance.getMappingsMapperCompat().hj.Q(this.I, entityPlayer.getObject(), world.getObject(), itemStack.getObject());
    }
}


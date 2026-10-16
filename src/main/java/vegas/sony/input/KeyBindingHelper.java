package vegas.sony.input;

import vegas.sony.utils.AxisAlignedBBDistanceComparator;
import vegas.sony.utils.datas.BlockData;
import vegas.sony.wrapper.impl.AxisAlignedBB;
import vegas.sony.wrapper.impl.EntityPlayerSP;
import vegas.sony.wrapper.impl.ForgeVersion;
import vegas.sony.wrapper.impl.KeyBinding;
import vegas.sony.wrapper.impl.Minecraft;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class KeyBindingHelper {
    public static void updateKeyBinding(KeyBinding keyBinding, boolean pressed, boolean triggerTick) {
        KeyBinding.setKeyBindState(keyBinding, pressed);
        if (triggerTick) {
            KeyBinding.onTick(keyBinding);
        }
    }

    public static void incrementPressTime(KeyBinding keyBinding) {
        keyBinding.onTick(1);
    }

    public static BlockData findSupportingBlock() {
        AxisAlignedBB playerBounds;
        EntityPlayerSP player = Minecraft.thePlayer();
        if (player.isNull()) {
            return null;
        }
        if (ForgeVersion.MC_1_8_9.d()) {
            playerBounds = player.R$src$Lvegas_sony_wrapper_impl_AxisAlignedBB_$r19dfl();
        } else {
            AxisAlignedBB currentBounds = player.R$src$Lvegas_sony_wrapper_impl_AxisAlignedBB_$r19dfl();
            playerBounds = currentBounds.copy();
        }
        double verticalOffset = ForgeVersion.MC_1_20_6.d() ? 1.0 : -1.0;
        AxisAlignedBB supportSearchBounds = playerBounds.k(0.0, verticalOffset, 0.0);
        List collisions = Minecraft.theWorld().i(player, supportSearchBounds);
        ArrayList<AxisAlignedBB> collisionBounds = new ArrayList<AxisAlignedBB>();
        for (Object collision : collisions) {
            collisionBounds.add(new AxisAlignedBB(collision));
        }
        if (collisionBounds.size() == 0) {
            return null;
        }
        if (collisionBounds.size() == 1) {
            return BlockData.P(collisionBounds.get(0));
        }
        Collections.sort(collisionBounds, new AxisAlignedBBDistanceComparator(player));
        return BlockData.P(collisionBounds.get(0));
    }

    public static void setPressedAndTick(KeyBinding keyBinding, boolean pressed) {
        KeyBindingHelper.updateKeyBinding(keyBinding, pressed, true);
    }


    public static void sendKeyBindingState(KeyBinding keyBinding, boolean pressed) {
        if (ForgeVersion.MC_1_21_4.v()) {
            KeyBindingHelper.updateKeyBinding(keyBinding, pressed, pressed);
            return;
        }
        Minecraft.s().L(Minecraft.p().e$src$J$14hgru1(), keyBinding.getKeyCode(), pressed);
    }
}


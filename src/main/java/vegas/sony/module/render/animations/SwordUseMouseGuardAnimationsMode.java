package vegas.sony.module.render.animations;

import vegas.sony.config.ClientSettings;
import vegas.sony.event.EventHandler;
import vegas.sony.event.impl.EventMouseButton;
import vegas.sony.module.Mod;
import vegas.sony.module.render.Animations;
import vegas.sony.rotation.RotationManager;
import vegas.sony.utils.ItemStackScoreUtil;
import vegas.sony.wrapper.impl.EntityPlayerSP;
import vegas.sony.wrapper.impl.ItemStack;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.RayTraceResult;
import vegas.sony.wrapper.impl.RayTraceResult_type;

public class SwordUseMouseGuardAnimationsMode
extends AnimationsMode {
    private int targetStreak = 1;

    public SwordUseMouseGuardAnimationsMode(Mod parent, String name) {
        super(parent, name);
    }

    @Override
    public boolean isBlocking() {
        return this.shouldBlock();
    }

    @EventHandler
    public void onMouseButton(EventMouseButton event) {
        if (!((Animations)this.getParent()).isHoldingSword()) {
            return;
        }
        int buttonBinding = -100 + event.getButton();
        if (event.getButtonState() && buttonBinding == Minecraft.gameSettings().b$src$Lvegas_sony_wrapper_impl_KeyBinding_$1yi3362().getKeyCode() && ((Animations)this.getParent()).requiresMouseDown() && ClientSettings.isAttackButtonDown()) {
            event.setCancelled(true);
            return;
        }
    }


    private boolean shouldBlockSwordUse() {
        if (((Animations)this.getParent()).requiresMouseDown() && !ClientSettings.isUseItemButtonDown()) {
            return false;
        }
        boolean shouldBlock = true;
        EntityPlayerSP player = Minecraft.thePlayer();
        ItemStack itemStack = player.B$src$Lvegas_sony_wrapper_impl_ItemStack_$impdvt();
        if (itemStack.isNull() || itemStack.getItem().isNull() || !ItemStackScoreUtil.h(itemStack.getItem())) {
            return false;
        }
        boolean targetingEntity = false;
        RayTraceResult rayTraceResult = RotationManager.INSTANCE.getExtendedReachRayTrace();
        if (rayTraceResult.isNotNull() && rayTraceResult.getTypeOfHit().equals(RayTraceResult_type.entity())) {
            targetingEntity = true;
        }
        if (this.targetStreak != 1 || !targetingEntity) {
            shouldBlock = false;
            int maxTargetStreak = 3;
            if (this.targetStreak >= maxTargetStreak) {
                this.targetStreak = 0;
            }
        }
        this.targetStreak = targetingEntity ? ++this.targetStreak : 1;
        return shouldBlock;
    }

    @Override
    public boolean shouldBlock() {
        return this.shouldBlockSwordUse();
    }
}
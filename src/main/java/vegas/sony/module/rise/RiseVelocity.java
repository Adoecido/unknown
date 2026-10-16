package vegas.sony.module.rise;

import vegas.sony.event.EventHandler;
import vegas.sony.event.impl.EventPacketReceive;
import vegas.sony.event.impl.EventPreLocalPlayerTick;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.module.Category;
import vegas.sony.module.Mod;
import vegas.sony.module.rise.velocity.RiseVelocityDelayMode;
import vegas.sony.module.rise.velocity.RiseVelocityJumpResetMode;
import vegas.sony.module.rise.velocity.RiseVelocityMMCMode;
import vegas.sony.module.rise.velocity.RiseVelocityPolarMode;
import vegas.sony.module.rise.velocity.RiseVelocityReduceMode;
import vegas.sony.module.rise.velocity.RiseVelocityReduceV2Mode;
import vegas.sony.unmap.ModeSelection;
import vegas.sony.value.BooleanValue;
import vegas.sony.value.ModeValue;
import vegas.sony.value.SubModuleValue;
import vegas.sony.wrapper.impl.EntityPlayerSP;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.Packet;
import vegas.sony.wrapper.impl.SPacketEntityVelocity;

public class RiseVelocity extends Mod {
    private final SubModuleValue<RiseVelocityPolarMode> polarMode =
            new RiseVelocityPolarMode(this, "Polar").getSelectionValue();
    private final SubModuleValue<RiseVelocityJumpResetMode> jumpResetMode =
            new RiseVelocityJumpResetMode(this, "Jump Reset").getSelectionValue();
    private final SubModuleValue<RiseVelocityMMCMode> mmcMode =
            new RiseVelocityMMCMode(this, "MMC").getSelectionValue();
    private final SubModuleValue<RiseVelocityDelayMode> delayMode =
            new RiseVelocityDelayMode(this, "Delay").getSelectionValue();
    private final SubModuleValue<RiseVelocityReduceMode> reduceMode =
            new RiseVelocityReduceMode(this, "Reduce").getSelectionValue();
    private final SubModuleValue<RiseVelocityReduceV2Mode> reduceV2Mode =
            new RiseVelocityReduceV2Mode(this, "Reduce V2").getSelectionValue();
    private final ModeValue mode = ModeValue.create(this, "Mode //$ui", this.polarMode,
            this.polarMode, this.jumpResetMode, this.mmcMode, this.delayMode,
            this.reduceMode, this.reduceV2Mode);
    private final BooleanValue onSwing = BooleanValue.create(this, "On Swing //$ui", false);
    private int offGroundTicks;
    private volatile int velocityTicks = Integer.MAX_VALUE;

    public RiseVelocity() {
        super("Velocity //$ui", 9133823, Category.COMBAT,
                "Velocity modes: Polar, Jump Reset, MMC, Delay, Reduce and Reduce V2 //$ui");
        this.addValue(this.mode, this.onSwing);
    }

    @EventHandler
    public void onLocalPlayerTick(EventPreLocalPlayerTick event) {
        EntityPlayerSP player = event.getPlayer();
        this.offGroundTicks = player.b$src$Z$fqlxe4() ? 0 : this.offGroundTicks + 1;
        if (this.velocityTicks < Integer.MAX_VALUE) {
            ++this.velocityTicks;
        }
    }

    @EventHandler
    public void onPacketReceive(EventPacketReceive event) {
        EntityPlayerSP player = Minecraft.thePlayer();
        if (event.isCanceled() || player.isNull()) {
            return;
        }
        Packet.n(event.getPacket(), packet -> {
            if (packet.isInstance(MappedClasses.YX)
                    && new SPacketEntityVelocity(packet.getObject()).getEntityId() == player.S()) {
                // Rise's EntityTickComponent resets the custom Entity#ae counter here.
                this.velocityTicks = 0;
            }
        });
    }

    public boolean canRunForSwing(EntityPlayerSP player) {
        return !this.onSwing.getEffectiveValue().booleanValue() || player.Y$src$Z$154rldp();
    }

    public int getOffGroundTicks() {
        return this.offGroundTicks;
    }

    public int getVelocityTicks() {
        return this.velocityTicks;
    }

    public BooleanValue getOnSwing() {
        return this.onSwing;
    }

    @Override
    public void onEnable() {
        this.offGroundTicks = 0;
        this.velocityTicks = Integer.MAX_VALUE;
    }

    @Override
    public void onDisable() {
        this.offGroundTicks = 0;
        this.velocityTicks = Integer.MAX_VALUE;
    }

    @Override
    public String getSimpleSuffix() {
        return ((ModeSelection)this.mode.getValue()).getName();
    }
}

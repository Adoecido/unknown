package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingMethod;
import vegas.sony.mapping.MappingMethodBuilder;
import vegas.sony.wrapper.impl.ForgeVersion;

public class MNetHandlerPlayClient
extends Mapping {
    private final MappingMethod handleEntityTeleportMethod;

    public static void handleEntityTeleport(MNetHandlerPlayClient mapping, Object packetListenerHandle, Object teleportPacketHandle) {
        mapping.invokeHandleEntityTeleport(packetListenerHandle, teleportPacketHandle);
    }

    public MNetHandlerPlayClient() {
        super(MappedClasses.Df);
        Class[] parameterTypes = new Class[]{MappedClasses.s};
        Class<Void> returnType = Void.TYPE;
        String methodName = "handleEntityTeleport";
        MNetHandlerPlayClient mapping = this;
        this.handleEntityTeleportMethod = ((MappingMethodBuilder)((MappingMethodBuilder)mapping.methodBuilder(methodName, returnType, parameterTypes).setNameForVersion(ForgeVersion.MC_1_16_5.n(), "handleTeleportEntity")).setOwnerClassForVersion(ForgeVersion.MC_1_21_4.n(), MappedClasses.z2)).buildMethod();
    }

    private void invokeHandleEntityTeleport(Object packetListenerHandle, Object teleportPacketHandle) {
        this.handleEntityTeleportMethod.invokeVoid(packetListenerHandle, teleportPacketHandle);
    }
}


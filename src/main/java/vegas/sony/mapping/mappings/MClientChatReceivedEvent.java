package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingField;
import vegas.sony.mapping.MappingMethod;
import vegas.sony.wrapper.Wrapper;
import vegas.sony.wrapper.impl.ForgeVersion;

public class MClientChatReceivedEvent
extends Mapping {
    private final MappingMethod n;
    private final MappingField a;

    public Object V(Object object) {
        return this.n.invokeObject(object, new Object[0]);
    }

    public MClientChatReceivedEvent() {
        this(MSPacketEntityVelocity.getPacketMappingControlFlowState());
    }

    private MClientChatReceivedEvent(int[] nArray) {
        super(MappedClasses.Zu);
        int[] nArray2 = nArray;
        if (Wrapper.sonyVegasInstance.isVanillaMinecraftPresent() || ForgeVersion.MC_1_8_9.Y()) {
            if (ForgeVersion.MC_1_7_10.L()) {
                Class[] classArray = new Class[]{};
                Class clazz = MappedClasses.Yr;
                boolean bl = true;
                String string = "func_148915_c";
                MClientChatReceivedEvent mClientChatReceivedEvent = this;
                this.n = mClientChatReceivedEvent.Y(string, bl, clazz, classArray);
            } else {
                Class[] classArray = new Class[]{};
                Class clazz = MappedClasses.Yr;
                boolean bl = true;
                String string = "getChatComponent";
                MClientChatReceivedEvent mClientChatReceivedEvent = this;
                this.n = mClientChatReceivedEvent.Y(string, bl, clazz, classArray);
            }
            Class clazz = MappedClasses.Yr;
            boolean bl = true;
            String string = "chatComponent";
            MClientChatReceivedEvent mClientChatReceivedEvent = this;
            this.a = mClientChatReceivedEvent.J(string, bl, clazz);
        } else {
            Class[] classArray = new Class[]{};
            Class clazz = MappedClasses.Yr;
            boolean bl = Wrapper.isNativeAvailable;
            String string = "func_148915_c";
            MClientChatReceivedEvent mClientChatReceivedEvent = this;
            this.n = mClientChatReceivedEvent.Y(string, bl, clazz, classArray); 
            Class clazz2 = MappedClasses.Yr;
            boolean bl2 = Wrapper.isNativeAvailable;
            String string2 = "field_148919_a";
            MClientChatReceivedEvent mClientChatReceivedEvent2 = this;
            this.a = this.J(string2, bl2, clazz2);
        }
    }

    public void i(Object object, Object object2) {
        this.a.setObject(object, object2);
    }

}


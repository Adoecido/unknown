package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.event.impl.EventChatMessageRender;
import vegas.sony.mapping.EventInjectionSpec;
import vegas.sony.mapping.JavassistMappingTask;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.MappingMethod;

public class ChatMessageRenderMappingTask
extends JavassistMappingTask {
    @Override
    public void transform() {
        this.registerChatMessageRenderEvent();
    }

    public ChatMessageRenderMappingTask() {
        super(MappedClasses.d);
    }

    private void registerChatMessageRenderEvent() {
        MappingMethod mappingMethod = SonyVegas.INSTANCE.getMappings().chatMessageRenderTarget.addMessageMethod;
        if (mappingMethod != null && !mappingMethod.hasResolutionFailed()) {
            EventInjectionSpec eventInjectionSpec = new EventInjectionSpec(mappingMethod, EventChatMessageRender.class);
            eventInjectionSpec.setConstructorArguments("$0, $1, $2, $3");
            eventInjectionSpec.setAfterCode("$1 = (" + MappedClasses.Yr.getName() + ") $event.getOutputContentComponent();");
            this.registerEventInjection(eventInjectionSpec);
        }
    }

}


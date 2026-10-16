package vegas.sony.friend.ui;

import vegas.sony.friend.OnlineFriend;
import vegas.sony.friend.ui.OnlineChatSender;
import vegas.sony.protocol.ZeusConnectionManager;
import vegas.sony.protocol.packet.ChatToFriendResponsePacket;
import vegas.sony.ui.click.component.GuiComponent;
import java.util.function.BiConsumer;

public class DirectFriendChatSender
implements OnlineChatSender {
    private final OnlineFriend friend;
    private static GuiComponent[] obfuscationComponents;

    private void handleChatResponse(BiConsumer<OnlineFriend, String> responseConsumer, ChatToFriendResponsePacket response) {
        responseConsumer.accept(this.friend, response.X());
    }

    public static void setObfuscationComponents(GuiComponent[] components) {
        obfuscationComponents = components;
    }

    @Override
    public void sendChatMessage(String message, BiConsumer<OnlineFriend, String> responseConsumer) {
        ZeusConnectionManager.T().u().p(this.friend.getUser(), message, response -> this.handleChatResponse(responseConsumer, (ChatToFriendResponsePacket)response));
    }

    public static GuiComponent[] getObfuscationComponents() {
        return obfuscationComponents;
    }

    public DirectFriendChatSender(OnlineFriend friend) {
        this.friend = friend;
    }

    static {
        if (DirectFriendChatSender.getObfuscationComponents() != null) {
            DirectFriendChatSender.setObfuscationComponents(new GuiComponent[3]);
        }
    }
}

package vegas.sony.friend.ui;

import vegas.sony.friend.OnlineFriend;
import vegas.sony.friend.ui.OnlineChatSender;
import vegas.sony.protocol.ZeusConnectionManager;
import vegas.sony.protocol.packet.GroupChatResponsePacket;
import java.util.function.BiConsumer;

public class PartyChatSender
implements OnlineChatSender {
    @Override
    public void sendChatMessage(String message, BiConsumer<OnlineFriend, String> responseConsumer) {
        ZeusConnectionManager.T().u().L(message, PartyChatSender::handleChatResponse);
    }

    private static void handleChatResponse(GroupChatResponsePacket response) {
    }
}

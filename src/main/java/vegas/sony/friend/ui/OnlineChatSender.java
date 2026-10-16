package vegas.sony.friend.ui;

import vegas.sony.friend.OnlineFriend;
import java.util.function.BiConsumer;

public interface OnlineChatSender {
    void sendChatMessage(String message, BiConsumer<OnlineFriend, String> responseConsumer);
}

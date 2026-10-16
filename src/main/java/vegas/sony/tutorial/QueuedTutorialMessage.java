package vegas.sony.tutorial;

import vegas.sony.utils.EncString;
import vegas.sony.utils.Str;

public class QueuedTutorialMessage {
    private final EncString title;
    private final EncString message;

    public QueuedTutorialMessage(String title, String message) {
        this.title = Str.enc(title);
        this.message = Str.enc(message);
    }

    public String getTitle() {
        return this.title == null ? null : this.title.get();
    }

    public String getMessage() {
        return this.message == null ? null : this.message.get();
    }
}

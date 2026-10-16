package vegas.sony.value;

import com.google.gson.JsonObject;
import java.lang.reflect.Method;
import java.util.Arrays;
import vegas.sony.remote.RemoteControlClient;

/** Regression probe for Piercing's live, multi-select manual player panel. */
public final class PiercingManualPlayersValueProbe {
    private PiercingManualPlayersValueProbe() {
    }

    public static void main(String[] args) throws Exception {
        PiercingManualPlayersValue value = PiercingManualPlayersValue.create(null);
        value.setPersistenceSuppressed(true);
        value.updateCandidates(Arrays.asList("Closer", "Farther", "closer", "bad-name!"));
        require(value.getCandidates().size() == 2,
                "manual candidates were not cleaned and deduplicated");
        require(value.selectFromFlags("10") && value.isSelected("closer")
                        && !value.isSelected("Farther"),
                "selecting one player did not mark only that player as hidden");

        Method serialize = RemoteControlClient.class.getDeclaredMethod(
                "serializeValue", Value.class);
        serialize.setAccessible(true);
        JsonObject serialized = (JsonObject)serialize.invoke(
                RemoteControlClient.INSTANCE, value);
        require("player-flags".equals(serialized.get("type").getAsString())
                        && "10".equals(serialized.get("value").getAsString())
                        && serialized.getAsJsonArray("options").size() == 2,
                "manual players were not serialized as avatar choices");

        value.updateCandidates(Arrays.asList("Farther", "Closer"));
        serialized = (JsonObject)serialize.invoke(RemoteControlClient.INSTANCE, value);
        require("01".equals(serialized.get("value").getAsString()),
                "selection did not follow a reordered live candidate list");

        PiercingManualPlayersValue restored = PiercingManualPlayersValue.create(null);
        restored.setPersistenceSuppressed(true);
        restored.parse("Closer\nFarther");
        restored.updateCandidates(Arrays.asList("Closer", "Farther"));
        require(restored.isSelected("Closer") && restored.isSelected("farther"),
                "saved manual selections were not restored");
        System.out.println("Piercing manual players: live avatar flags and persistence verified");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

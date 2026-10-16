package vegas.sony.value;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import vegas.sony.module.gladiador.Target;
import vegas.sony.remote.RemoteControlClient;

/** Regression probe for the ordered, single-select Invsee ranking. */
public final class InvseeTargetCandidatesValueProbe {
    private InvseeTargetCandidatesValueProbe() {
    }

    public static void main(String[] args) throws Exception {
        InvseeTargetCandidatesValue value = InvseeTargetCandidatesValue.create(null);
        value.setPersistenceSuppressed(true);
        value.updatePlayers(Arrays.asList(
                new InvseeTargetCandidatesValue.RankedPlayer("Kak4shiHakai", 6),
                new InvseeTargetCandidatesValue.RankedPlayer("Rektaay", 4),
                new InvseeTargetCandidatesValue.RankedPlayer("Defow", 4),
                new InvseeTargetCandidatesValue.RankedPlayer("rektAay", 9),
                new InvseeTargetCandidatesValue.RankedPlayer("invalid-name!", 1)));
        require(value.getCandidates().size() == 3, "ranking players were not normalised");
        require("Rektaay".equals(value.getPlayers().get(0).getName())
                        && "Defow".equals(value.getPlayers().get(1).getName())
                        && value.getPlayers().get(2).getSets() == 6,
                "ranking was not sorted stably from fewer to more sets");
        require(value.getValue().isEmpty(),
                "a fresh ranking selected a player before Manual mode was chosen");
        require(value.selectLowestPlayer(),
                "automatic mode could not select from the lowest tie");
        String current = value.getValue();
        String other = current.equalsIgnoreCase("Rektaay") ? "Defow" : "Rektaay";
        require(value.selectPlayer(other), "clicking the other tied player failed");
        require(!current.equalsIgnoreCase(value.getValue()), "the selection did not change");
        require(value.selectPlayer("Kak4shiHakai")
                        && "Kak4shiHakai".equals(value.getValue()),
                "a player outside the lowest tie could not be selected");

        value.clearCandidates();
        require(value.getCandidates().isEmpty() && value.getValue().isEmpty(),
                "clearing an unavailable report kept a stale target");

        Target target = new Target();
        target.invseeSynchronization.setPersistenceSuppressed(true);
        target.invseeSynchronization.setValue(true);
        require(target.invseeMode.areConditionsMet(),
                "the Invsee Manual/Automatic selector was hidden while synchronization was enabled");
        require(target.invseeCandidates.areConditionsMet(),
                "the Invsee ranking was hidden while synchronization was enabled");
        require(!target.automatic.areConditionsMet() && !target.playerName.areConditionsMet(),
                "normal Target controls remained visible during Invsee synchronization");

        target.invseeCandidates.setPersistenceSuppressed(true);
        target.invseeCandidates.updatePlayers(Arrays.asList(
                new InvseeTargetCandidatesValue.RankedPlayer("Rektaay", 4),
                new InvseeTargetCandidatesValue.RankedPlayer("Defow", 4),
                new InvseeTargetCandidatesValue.RankedPlayer("Kak4shiHakai", 6)));
        target.invseeCandidates.clearSelection();
        require(target.invseeCandidates.getValue().isEmpty(),
                "Manual mode did not clear the synchronized selection");
        require(target.invseeCandidates.selectPlayer("Defow")
                        && "Defow".equals(target.invseeCandidates.getValue()),
                "Manual mode could not select a clicked player");
        Method serialize = RemoteControlClient.class.getDeclaredMethod(
                "serializeValue", Value.class, String.class);
        serialize.setAccessible(true);
        JsonObject serialized = (JsonObject)serialize.invoke(
                RemoteControlClient.INSTANCE, target.invseeCandidates,
                target.invseeCandidates.getId());
        JsonArray players = serialized.getAsJsonArray("players");
        require("invsee-ranking".equals(serialized.get("type").getAsString())
                        && players.size() == 3
                        && players.get(0).getAsJsonObject().get("sets").getAsInt() == 4
                        && "Kak4shiHakai".equals(
                                players.get(2).getAsJsonObject().get("name").getAsString()),
                "the native ranking payload lost a player or set count");
        String selected = target.invseeCandidates.getValue();
        String next = selected.equalsIgnoreCase("Kak4shiHakai")
                ? "Defow" : "Kak4shiHakai";
        Method apply = RemoteControlClient.class.getDeclaredMethod("applyValue",
                vegas.sony.module.Mod.class, String.class, String.class);
        apply.setAccessible(true);
        require((Boolean)apply.invoke(RemoteControlClient.INSTANCE, target,
                        target.invseeCandidates.getId(), next)
                        && next.equals(target.invseeCandidates.getValue()),
                "clicking an Invsee ranking row did not replace the target");
        Method parsePlayers = Target.class.getDeclaredMethod("parseInvseePlayers",
                JsonElement.class, boolean.class, int.class);
        parsePlayers.setAccessible(true);
        JsonArray report = new JsonParser().parse("[{\"name\":\"rarecrit\",\"sets\":8,"
                + "\"helmet\":8,\"chestplate\":8,\"leggings\":8,\"boots\":8},"
                + "{\"name\":\"SpareParts\",\"sets\":0,\"helmet\":3,"
                + "\"chestplate\":0,\"leggings\":2,\"boots\":1}]").getAsJsonArray();
        @SuppressWarnings("unchecked")
        List<InvseeTargetCandidatesValue.RankedPlayer> parsedPlayers =
                (List<InvseeTargetCandidatesValue.RankedPlayer>)parsePlayers.invoke(
                        null, report, false, 0);
        target.invseeCandidates.updatePlayers(parsedPlayers);
        JsonObject armorPayload = (JsonObject)serialize.invoke(RemoteControlClient.INSTANCE,
                target.invseeCandidates, target.invseeCandidates.getId());
        JsonArray armorPlayers = armorPayload.getAsJsonArray("players");
        require(armorPlayers.get(0).getAsJsonObject().get("helmet").getAsInt() == 3
                        && armorPlayers.get(0).getAsJsonObject().get("chestplate").getAsInt() == 0,
                "individual piece counts were incorrectly replaced by total sets");
        JsonObject eightSets = armorPlayers.get(1).getAsJsonObject();
        for (String field : Arrays.asList("sets", "helmet", "chestplate", "leggings", "boots")) {
            require(eightSets.get(field).getAsInt() == 8,
                    "eight-set report lost " + field + " before reaching the native menu");
        }
        System.out.println("Invsee ranking: ordered, individually selectable and stale-safe");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

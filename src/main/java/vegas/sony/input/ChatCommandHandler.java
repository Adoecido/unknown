package vegas.sony.input;

import vegas.sony.SonyVegas;
import vegas.sony.config.Profile;
import vegas.sony.friend.Friend;
import vegas.sony.friend.FriendEntry;
import vegas.sony.manager.client.FriendManager;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.module.Mod;
import vegas.sony.module.gladiador.HideClans;
import vegas.sony.module.misc.Panic;
import vegas.sony.notification.NotificationType;
import vegas.sony.utils.Str;
import vegas.sony.utils.StringWipe;
import vegas.sony.value.Value;
import vegas.sony.wrapper.impl.Entity;
import vegas.sony.wrapper.impl.EntityLivingBase;
import vegas.sony.wrapper.impl.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.LinkedHashSet;

public class ChatCommandHandler {

    private static String d(String e, int k) { return Str.dec(e, k); }

    public static boolean process(String message) {
        if (message == null || !message.startsWith(d("TQ==", 0x63C4AF19))) return false;
        if (Panic.isActive()) return false;
        String stripped = message.substring(1).trim();
        if (stripped.isEmpty()) return false;

        String[] parts = stripped.split("\\s+", 3);
        String command = parts[0].toLowerCase();

        if ("friend".equals(command)) {
            handleFriend(parts);
        } else if ("friends".equals(command) || "frends".equals(command)) {
            handleFriends(parts);
        } else if (d("PPn9xQ==", 0x5E9093A1).equals(command)) {
            handleBind(parts);
        } else if (d("K1FZ6DBb", 0x5E3F3B81).equals(command)) {
            handleUnbind(parts);
        } else if (d("RA==", 0x30912AE0).equals(command) || d("Cr1f3RK3", 0x7ED238BA).equals(command)) {
            handleToggle(parts);
        } else if (d("J18oYA==", 0x4F3A4410).equals(command)) {
            showHelp();
        } else {
            Mod module = SonyVegas.INSTANCE.getModManager().getMod(command);
            if (module != null && parts.length >= 3) {
                handleValue(module, parts[1], parts[2]);
            } else {
                notify(d("JkSN6QtPj6gBTpPrCkWI7QZChOc=", 0x652BE088),
                       d("G/VMw2DuTI8+plmCPOcJlSv0CYw9pkqMI+dHhyH1Bw==", 0x4E8629E3),
                       NotificationType.WARNING);
            }
        }
        StringWipe.wipe(message);
        StringWipe.wipe(stripped);
        StringWipe.wipe(command);
        for (String p : parts) StringWipe.wipe(p);
        return true;
    }

    private static void handleFriend(String[] parts) {
        if (parts.length < 3) {
            notify("Friends", "Use .friend add <nick> ou .friend remove <nick>",
                    NotificationType.WARNING);
            return;
        }
        String action = parts[1].toLowerCase(Locale.ROOT);
        String nickname = cleanNickname(parts[2]);
        if (nickname.isEmpty()) {
            notify("Friends", "Nick invalido", NotificationType.WARNING);
            return;
        }
        FriendManager manager = SonyVegas.INSTANCE.getFriendManager();
        if ("add".equals(action)) {
            for (FriendEntry existing : manager.getFriendsByName(nickname)) {
                if (existing.isPersistent()) {
                    notify("Friends", nickname + " ja esta na lista", NotificationType.INFO);
                    return;
                }
            }
            String storedName = new String(nickname.toCharArray());
            manager.addFriend(new Friend(storedName,
                    new String(storedName.toCharArray())));
            saveActiveProfile();
            notify("Friends", "Adicionado: " + nickname, NotificationType.INFO);
            return;
        }
        if ("remove".equals(action)) {
            int removed = removePersistentFriends(manager, nickname);
            if (removed > 0) saveActiveProfile();
            notify("Friends", removed > 0 ? "Removido: " + nickname
                    : nickname + " nao esta na lista", removed > 0
                    ? NotificationType.INFO : NotificationType.WARNING);
            return;
        }
        notify("Friends", "Use add ou remove", NotificationType.WARNING);
    }

    private static void handleFriends(String[] parts) {
        if (parts.length < 2) {
            notify("Friends", "Use clear, addall <tag> ou list",
                    NotificationType.WARNING);
            return;
        }
        String action = parts[1].toLowerCase(Locale.ROOT);
        FriendManager manager = SonyVegas.INSTANCE.getFriendManager();
        if ("clear".equals(action)) {
            int removed = 0;
            for (FriendEntry entry : new ArrayList<FriendEntry>(manager.getFriends())) {
                if (!entry.isPersistent()) continue;
                manager.removeFriend(entry);
                ++removed;
            }
            if (removed > 0) saveActiveProfile();
            notify("Friends", removed == 0 ? "A lista ja esta vazia"
                    : removed + (removed == 1 ? " amigo removido" : " amigos removidos"),
                    NotificationType.INFO);
            return;
        }
        if ("list".equals(action)) {
            List<String> names = new ArrayList<String>();
            for (FriendEntry entry : manager.getFriends()) {
                String name = entry.getName();
                if (name != null && !name.trim().isEmpty() && entry.isTargeted())
                    names.add(name.trim());
            }
            names.sort(String.CASE_INSENSITIVE_ORDER);
            notify("Friends (" + names.size() + ")", names.isEmpty()
                    ? "Nenhum amigo adicionado" : String.join(", ", names),
                    NotificationType.INFO);
            return;
        }
        if ("addall".equals(action)) {
            if (parts.length < 3) {
                notify("Friends", "Use .friends addall <tag>",
                        NotificationType.WARNING);
                return;
            }
            addAllFromClanTag(manager, parts[2]);
            return;
        }
        notify("Friends", "Use clear, addall ou list", NotificationType.WARNING);
    }

    private static void addAllFromClanTag(FriendManager manager, String rawTag) {
        String tag = normalizeClanTag(rawTag);
        if (tag.length() < 2 || Minecraft.theWorld().isNull()
                || Minecraft.thePlayer().isNull()) {
            notify("Friends", "Tag invalida ou mundo indisponivel",
                    NotificationType.WARNING);
            return;
        }
        int added = 0;
        LinkedHashSet<String> matchingNames = new LinkedHashSet<String>();
        // Tab contains the complete online player list, including clan members
        // outside the local chunk/render distance.
        matchingNames.addAll(HideClans.collectTabClanMembers(
                "tag:" + tag, Minecraft.thePlayer().getName()));
        for (Object handle : Minecraft.theWorld().z()) {
            try {
                Entity entity = new Entity(handle);
                if (!entity.isInstance(MappedClasses.Yl)
                        || handle == Minecraft.thePlayer().getObject()) continue;
                EntityLivingBase player = new EntityLivingBase(handle);
                if (!("tag:" + tag).equals(HideClans.getClanIdentity(player))) continue;
                String nickname = cleanNickname(player.getName());
                if (!nickname.isEmpty()) matchingNames.add(nickname);
            } catch (Throwable ignored) {}
        }
        for (String candidate : matchingNames) {
            String nickname = cleanNickname(candidate);
            if (nickname.isEmpty() || hasPersistentFriend(manager, nickname)) continue;
            String storedName = new String(nickname.toCharArray());
            manager.addFriend(new Friend(storedName,
                    new String(storedName.toCharArray())));
            ++added;
        }
        if (added > 0) saveActiveProfile();
        notify("Friends", added == 0 ? "Nenhum jogador encontrado com a tag " + rawTag
                : added + (added == 1 ? " jogador adicionado" : " jogadores adicionados"),
                added > 0 ? NotificationType.INFO : NotificationType.WARNING);
    }

    private static boolean hasPersistentFriend(FriendManager manager, String nickname) {
        for (FriendEntry entry : manager.getFriendsByName(nickname)) {
            if (entry.isPersistent()) return true;
        }
        return false;
    }

    private static int removePersistentFriends(FriendManager manager, String nickname) {
        int removed = 0;
        for (FriendEntry entry : new ArrayList<FriendEntry>(
                manager.getFriendsByName(nickname))) {
            if (!entry.isPersistent()) continue;
            manager.removeFriend(entry);
            ++removed;
        }
        return removed;
    }

    private static String cleanNickname(String raw) {
        if (raw == null) return "";
        String clean = raw.replaceAll("§.", "").trim();
        return clean.matches("[A-Za-z0-9_]{1,16}") ? clean : "";
    }

    static String normalizeClanTag(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("§.", "").replaceAll("[^A-Za-z0-9]", "")
                .toLowerCase(Locale.ROOT);
    }

    private static void handleBind(String[] parts) {
        if (parts.length < 3) {
            notify(d("Zr9v", 0x33CC00B0),
                   d("F+QzBV2mZgZW4i8HVrh6V03jOQdYuA==", 0x39865A6B),
                   NotificationType.WARNING);
            return;
        }
        String moduleName = parts[1];
        String keyName = parts[2].toUpperCase();
        Mod module = SonyVegas.INSTANCE.getModManager().getMod(moduleName);
        if (module == null) {
            notify(d("Huqj+z/q5+Ay6ufrPeao4Cf3puo8", 0x5385C78E), moduleName, NotificationType.WARNING);
            return;
        }
        if (!module.getBind().usesOwnKeybindStorage()) {
            notify(d("P3iWzF14lswUYojHE3iOzRE=", 0x7D11F8A8),
                   module.getName() + d("WR3on1kA/IAWAf2RWRHgnh1d", 0x797389F0),
                   NotificationType.WARNING);
            return;
        }
        int keyCode = resolveKey(keyName);
        if (keyCode == -1) {
            notify(d("HRHWfShU3H8/Fdl4LRU=", 0x4974B511), keyName, NotificationType.WARNING);
            return;
        }
        module.getBind().getBoundInputs().clear();
        if (keyCode != 0) module.getBind().getBoundInputs().add(keyCode);
        module.getBind().setActivationMode(BindActivationMode.TOGGLE);
        saveActiveProfile();
        notify(d("BlsfN2RWFDUtXBg3Kw==", 0x44327153),
               module.getName() + d("fFIvQQ==", 0x5C7F1161) + keyName,
               NotificationType.INFO);
    }

    private static void handleUnbind(String[] parts) {
        if (parts.length < 2) {
            notify(d("Zr9v", 0x33CC00B0),
                   d("Ac1YQEbWUgIT1VlGWtRZHA==", 0x2FB83622),
                   NotificationType.WARNING);
            return;
        }
        String moduleName = parts[1];
        Mod module = SonyVegas.INSTANCE.getModManager().getMod(moduleName);
        if (module == null) {
            notify(d("Huqj+z/q5+Ay6ufrPeao4Cf3puo8", 0x5385C78E), moduleName, NotificationType.WARNING);
            return;
        }
        module.getBind().getBoundInputs().clear();
        saveActiveProfile();
        notify(d("Ai10aWA2f2AvMnNpLw==", 0x40441A0D), module.getName(), NotificationType.INFO);
    }

    private static void handleToggle(String[] parts) {
        if (parts.length < 2) {
            notify(d("Zr9v", 0x33CC00B0),
                   d("ehxN9TkHCbw4B1M=", 0x54686DC9),
                   NotificationType.WARNING);
            return;
        }
        String moduleName = parts[1];
        Mod module = SonyVegas.INSTANCE.getModManager().getMod(moduleName);
        if (module == null) {
            notify(d("Huqj+z/q5+Ay6ufrPeao4Cf3puo8", 0x5385C78E), moduleName, NotificationType.WARNING);
            return;
        }
        module.toggle();
    }

    private static void handleValue(Mod module, String valueName, String serializedValue) {
        Value<?, ?> value = module.getValue(valueName);
        if (value == null) {
            notify("Valor nao encontrado", module.getName() + ": " + valueName,
                    NotificationType.WARNING);
            return;
        }
        try {
            value.parse(serializedValue);
            notify("Configuracao alterada",
                    module.getName() + " " + value.getName() + " = " + value.getDisplayValue(),
                    NotificationType.INFO);
        } catch (RuntimeException invalidValue) {
            notify("Valor invalido", serializedValue, NotificationType.WARNING);
        }
    }

    private static void showHelp() {
        notify("Comandos",
                ".bind, .unbind, .toggle, .friend add/remove, .friends clear/addall/list",
                NotificationType.INFO);
    }

    private static void notify(String title, String message, NotificationType type) {
        if (SonyVegas.INSTANCE.getNotificationManager() != null) {
            SonyVegas.INSTANCE.getNotificationManager().show(title, message, type, 3000L);
        }
    }

    private static void saveActiveProfile() {
        Profile active = SonyVegas.INSTANCE.getProfilesManager().getActiveProfileOrNull();
        if (active != null) active.captureCurrentState();
        SonyVegas.INSTANCE.saveAndStop();
    }

    private static int resolveKey(String name) {
        if (name.length() == 1) {
            char c = name.charAt(0);
            if (c >= 'A' && c <= 'Z') return c;
            if (c >= '0' && c <= '9') return c;
        }
        if (name.charAt(0) == 'F' && name.length() >= 2 && name.length() <= 3) {
            try {
                int n = Integer.parseInt(name.substring(1));
                if (n >= 1 && n <= 12) return 111 + n;
            } catch (NumberFormatException ignored) {}
        }
        switch (name) {
            case "SHIFT": case "LSHIFT": return 160;
            case "RSHIFT": return 161;
            case "CTRL": case "LCTRL": return 162;
            case "RCTRL": return 163;
            case "ALT": case "LALT": return 164;
            case "RALT": return 165;
            case "SPACE": return 32;
            case "TAB": return 9;
            case "CAPS": case "CAPSLOCK": return 20;
            case "ENTER": case "RETURN": return 13;
            case "BACKSPACE": return 8;
            case "DELETE": case "DEL": return 46;
            case "INSERT": case "INS": return 45;
            case "HOME": return 36;
            case "END": return 35;
            case "PAGEUP": return 33;
            case "PAGEDOWN": return 34;
            case "UP": return 38;
            case "DOWN": return 40;
            case "LEFT": return 37;
            case "RIGHT": return 39;
            case "GRAVE": return 192;
            case "MINUS": return 189;
            case "EQUALS": return 187;
            case "MOUSE3": case "M3": return -98;
            case "MOUSE4": case "M4": return -97;
            case "MOUSE5": case "M5": return -96;
            case "NONE": return 0;
            default: return -1;
        }
    }
}

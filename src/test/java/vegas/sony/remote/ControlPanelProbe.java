package vegas.sony.remote;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.GZIPInputStream;
import vegas.sony.runtime.PayloadResources;

/** Generates fixtures from the production renderer without starting Minecraft or using cloud credentials. */
public final class ControlPanelProbe {
    public static void main(String[] args) throws Exception {
        Path output = Paths.get("build/control-probe");
        Files.createDirectories(output);
        JsonObject profile = json("{\"active\":\"local\",\"profiles\":[{\"id\":\"local\",\"name\":\"Meu perfil\"}]}");
        JsonObject configs = json("{\"configs\":[{\"slot\":0,\"name\":\"Minha config & <teste>\",\"version\":3,\"updatedAt\":\"2026-09-03T05:00:00Z\"}]}");
        JsonObject party = json("{\"code\":\"ABC123\",\"myRole\":\"leader\",\"members\":[{\"name\":\"Jogador <1>\",\"role\":\"leader\"}]}");
        JsonObject community = json("{\"configs\":[{\"id\":\"cc_0123456789abcdef0123456789abcdef\",\"owner\":\"Jogador <1>\",\"name\":\"Legit & Main\",\"description\":\"Teste <seguro>\",\"downloads\":7,\"mine\":true}]}");
        write(output.resolve("active.html"), render(profile, configs, party, community));
        write(output.resolve("empty.html"), render(profile, json("{\"configs\":[]}"), null, json("{\"configs\":[]}")));
        write(output.resolve("offline.html"), render(profile, json("{\"error\":\"Falha ao consultar.\"}"), null, json("{\"error\":\"Community offline.\"}")));
        String javascript = panelAsset("144");
        String stylesheet = panelAsset("145");
        write(output.resolve("app.js"), javascript);
        write(output.resolve("app.css"), stylesheet);
        if (!javascript.contains("id==='phone'")) {
            throw new AssertionError("mobile section is not treated as a utility panel");
        }
        if (!javascript.contains("(pointer: coarse)")) {
            throw new AssertionError("single-tap module opening is not disabled on touch screens");
        }
        if (javascript.contains("fetch(form.action")) throw new AssertionError("DOM action collision regressed");
        if (javascript.contains("setTimeout(function(){location.reload();},300)")) {
            throw new AssertionError("live state synchronization still reloads the page");
        }
        if (!javascript.contains("fetchLiveState") || !javascript.contains("patchModule")) {
            throw new AssertionError("live state patching is missing");
        }
        if (!javascript.contains("KeyK:75") || javascript.contains("upper.charCodeAt(0)")) {
            throw new AssertionError("browser binds are not translated to Win32 virtual-key codes");
        }
        if (!javascript.contains("_saveQueued")) {
            throw new AssertionError("queued form persistence is missing");
        }
        String formattedJavascript = new String(Files.readAllBytes(output.resolve("app.js")), StandardCharsets.UTF_8);
        if (!formattedJavascript.contains("formatSliderValue") || !formattedJavascript.contains("data-decimals")) {
            throw new AssertionError("numeric unit formatting is missing");
        }
        if (formattedJavascript.contains("toggleBoundModules")) {
            throw new AssertionError("the browser must not execute game module binds");
        }
        if (!formattedJavascript.contains("refreshDrawerRangeFill")
                || !formattedJavascript.contains("refreshDrawerValueFormat")) {
            throw new AssertionError("drawer range controls are not refreshed when opened");
        }
        if (!formattedJavascript.contains("player-list-add-button")
                || !formattedJavascript.contains("value-suggestion")) {
            throw new AssertionError("player name autocomplete/list editor is missing");
        }
        String activeHtml = new String(Files.readAllBytes(output.resolve("active.html")), StandardCharsets.UTF_8);
        if (!activeHtml.contains("loader-visibility")) {
            throw new AssertionError("loader visibility control is missing");
        }
        if (!activeHtml.contains("Notifications") || !activeHtml.contains("action=\"notifications\"")) {
            throw new AssertionError("notification visibility switch is missing");
        }
        if (!activeHtml.contains("id=\"mobile-qr\"") || !activeHtml.contains("data-panel=\"phone\"")) {
            throw new AssertionError("mobile QR panel is missing");
        }
        if (activeHtml.contains("mobile-qr.png") || activeHtml.contains("http://")) {
            throw new AssertionError("the phone URL must be assembled client-side");
        }
        if (!stylesheet.contains("#module-drawer{transition:none!important}")) {
            throw new AssertionError("drawer opening still has a delayed transition");
        }
        if (!stylesheet.contains("#module-drawer .value.condition-hidden")) {
            throw new AssertionError("drawer condition visibility can be overridden by layout rules");
        }
        String mobileJavascript = javascript;
        if (!mobileJavascript.contains("touchend") || !mobileJavascript.contains("contextmenu")
                || !mobileJavascript.contains("copy-url")) {
            throw new AssertionError("mobile double-tap or copy-link support is missing");
        }
        // Probes run without the injected loader, so the native control
        // bridge must report the session as unavailable and every request
        // must surface the documented 401 instead of touching the cloud.
        if (ControlApi.isAvailable()) throw new AssertionError("Control API is live without the loader");
        try {
            ControlApi.request("status", null);
            throw new AssertionError("Control request without a loader must fail");
        } catch (ControlRequestException unavailable) {
            if (unavailable.status != 401
                    || !"session_unavailable".equals(unavailable.safeCode())) {
                throw new AssertionError("Control request did not surface the documented 401");
            }
        }
        ControlRequestException safe = new ControlRequestException(502, "secret-raw-error");
        if (safe.getMessage().contains("secret-raw-error")) throw new AssertionError("Raw error exposed");
        System.out.println("Control panel renderer/token/error probe passed; browser fixtures ready.");
    }

    private static String render(JsonObject profile, JsonObject configs, JsonObject party,
            JsonObject community) throws Exception {
        StringBuilder html = new StringBuilder("<!doctype html><html lang=\"pt-BR\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><link rel=\"stylesheet\" href=\"app.css\"><script>window.fetch=function(path){return Promise.resolve(new Response(path==='state-version'?'0':'',{status:200}));};</script><script defer src=\"app.js\"></script></head><body><div class=\"shell\"><header><div class=\"brand\"><span class=\"mark\">SV</span><div><small>SONYVEGAS</small><h1>Control Center</h1></div></div><span class=\"online\"><i></i>Conectado</span></header><main><nav class=\"tabs\"><button class=\"tab\" data-category=\"phone\">Celular</button><button class=\"tab\" data-category=\"friends\">Friends</button><button class=\"tab\" data-category=\"category-0\">Modules</button><button class=\"tab\" data-category=\"configs\">Configs</button><button class=\"tab\" data-category=\"party\">Party</button><button class=\"tab\" data-category=\"community\">Community</button></nav><div class=\"workspace\"><div id=\"search-empty\"></div>");
        Method configRenderer = LocalControlServer.class.getDeclaredMethod("appendConfigPanel", StringBuilder.class, JsonObject.class);
        configRenderer.setAccessible(true);
        configRenderer.invoke(LocalControlServer.INSTANCE, html, configs);
        Method settingsRenderer = LocalControlServer.class.getDeclaredMethod("appendSettingsPanel", StringBuilder.class, JsonObject.class);
        settingsRenderer.setAccessible(true);
        settingsRenderer.invoke(LocalControlServer.INSTANCE, html, profile);
        Method phoneRenderer = LocalControlServer.class.getDeclaredMethod("appendPhonePanel", StringBuilder.class);
        phoneRenderer.setAccessible(true);
        phoneRenderer.invoke(LocalControlServer.INSTANCE, html);
        Method partyRenderer = LocalControlServer.class.getDeclaredMethod("appendPartyPanel", StringBuilder.class, JsonObject.class, String.class, com.google.gson.JsonArray.class);
        partyRenderer.setAccessible(true);
        com.google.gson.JsonArray modules = com.google.gson.JsonParser.parseString("[{\"name\":\"Reach\",\"enabled\":false,\"values\":[],\"bind\":[82]}]").getAsJsonArray();
        partyRenderer.invoke(LocalControlServer.INSTANCE, html, party, "Jogador <1>", modules);
        Method friendsRenderer = LocalControlServer.class.getDeclaredMethod("appendFriendsPanel", StringBuilder.class, JsonObject.class);
        friendsRenderer.setAccessible(true);
        friendsRenderer.invoke(LocalControlServer.INSTANCE, html, json("{\"names\":[\"Steve\"],\"enabled\":true,\"middle\":false}"));
        html.append("<section class=\"category-panel module-panel\" data-panel=\"category-0\"><div class=\"section-title\"><h2>Combat</h2><span>Módulos disponíveis</span></div><div class=\"grid\">");
        Method moduleRenderer = LocalControlServer.class.getDeclaredMethod("appendModule", StringBuilder.class, JsonObject.class);
        moduleRenderer.setAccessible(true);
        moduleRenderer.invoke(LocalControlServer.INSTANCE, html, modules.get(0).getAsJsonObject());
        Method serialize = RemoteControlClient.class.getDeclaredMethod("serializeValue",
                vegas.sony.value.Value.class, String.class);
        serialize.setAccessible(true);
        vegas.sony.module.combat.HitSelect hitSelect = new vegas.sony.module.combat.HitSelect();
        JsonObject hitModule = new JsonObject();
        hitModule.addProperty("name", "HitSelect"); hitModule.addProperty("enabled", false);
        com.google.gson.JsonArray hitValues = new com.google.gson.JsonArray();
        for (vegas.sony.value.Value<?, ?> setting : hitSelect.getValues()) {
            JsonObject data = (JsonObject)serialize.invoke(RemoteControlClient.INSTANCE,
                    setting, setting.getId());
            if (data != null) hitValues.add(data);
            if (setting instanceof vegas.sony.value.ModeValue && ((vegas.sony.value.ModeValue)setting).getModes()[0] instanceof vegas.sony.value.SubModuleValue) {
                vegas.sony.value.ModeValue mode = (vegas.sony.value.ModeValue)setting;
                mode.setPersistenceSuppressed(true);
                for (int index = 0; index < mode.getModes().length; index++) {
                    mode.parse(mode.getModes()[index].getSerializedName());
                    if (mode.getSelectedIndex() != index) throw new AssertionError("HitSelect choice did not switch");
                }
                mode.setSelectedIndex(0);
                if (data.getAsJsonArray("options").size() != 2 || !data.getAsJsonObject("optionLabels").toString().contains("Burst")) throw new AssertionError("missing HitSelect choices");
            }
        }
        JsonObject itemOptions = new JsonObject();
        itemOptions.addProperty("id", "fixture-items"); itemOptions.addProperty("name", "Only items");
        itemOptions.addProperty("type", "list"); itemOptions.addProperty("value", "swords");
        com.google.gson.JsonArray itemChoices = new com.google.gson.JsonArray();
        itemChoices.add("swords"); itemChoices.add("axes"); itemChoices.add("custom item");
        itemOptions.add("options", itemChoices); hitValues.add(itemOptions);
        hitModule.add("values", hitValues);
        moduleRenderer.invoke(LocalControlServer.INSTANCE, html, hitModule);
        html.append("</div></section>");
        Method communityRenderer = LocalControlServer.class.getDeclaredMethod("appendCommunityPanel", StringBuilder.class, JsonObject.class, String.class);
        communityRenderer.setAccessible(true);
        communityRenderer.invoke(LocalControlServer.INSTANCE, html, community, "Jogador <1>");
        html.append("</div></main><div id=\"module-drawer\" aria-hidden=\"true\">")
                .append("<div class=\"drawer-header\"><div class=\"drawer-topbar\"><button type=\"button\" class=\"expand-close\">Fechar</button></div>")
                .append("<div class=\"drawer-module-row\"><span id=\"drawer-icon\" class=\"mod-icon\"></span><div><div id=\"drawer-title\"></div><div id=\"drawer-desc\"></div></div>")
                .append("<label class=\"ios-switch drawer-toggle\"><input type=\"checkbox\" id=\"drawer-toggle\"><span class=\"ios-slider\"></span></label></div></div>")
                .append("<div class=\"drawer-tabs\"><button class=\"exp-tab active\">Geral</button></div><div id=\"drawer-content\" class=\"drawer-content\"></div>")
                .append("<div class=\"drawer-footer\"><button type=\"button\" class=\"btn-reset\" onclick=\"closePanel()\">Fechar</button></div></div>");
        return html.append("</div><div id=\"toast\" class=\"toast\"></div></body></html>").toString();
    }

    private static String panelAsset(String name) throws Exception {
        byte[] packed = PayloadResources.readBytes(LocalControlServer.class,
                "/assets/lIlIIlIl/" + name);
        if (packed == null) throw new AssertionError("Missing packaged panel asset: " + name);
        try (GZIPInputStream input = new GZIPInputStream(new ByteArrayInputStream(packed));
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static JsonObject json(String value) { return JsonParser.parseString(value).getAsJsonObject(); }
    private static void write(Path path, String value) throws Exception {
        Files.write(path, value.getBytes(StandardCharsets.UTF_8));
    }
}

package vegas.sony.remote;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import vegas.sony.SonyVegas;
import vegas.sony.config.Profile;
import vegas.sony.event.impl.EventTickBase;
import vegas.sony.friend.OnlineFriend;
import vegas.sony.friend.PartyState;
import vegas.sony.input.BindSet;
import vegas.sony.manager.ModManager;
import vegas.sony.module.Category;
import vegas.sony.module.Mod;
import vegas.sony.module.combat.Basquete;
import vegas.sony.module.gladiador.Target;
import vegas.sony.module.misc.Panic;
import vegas.sony.module.none.ClientSettings;
import vegas.sony.module.render.ClickGui;
import vegas.sony.runtime.NativeBridge;
import vegas.sony.unmap.ItemLimitData;
import vegas.sony.unmap.ModeSelection;
import vegas.sony.value.BooleanValue;
import vegas.sony.value.ColorValue;
import vegas.sony.value.ConditionalValue;
import vegas.sony.value.EntityTargetFilterValue;
import vegas.sony.value.InvseeTargetCandidatesValue;
import vegas.sony.value.PiercingManualPlayersValue;
import vegas.sony.value.PlayerNameSuggestionProvider;
import vegas.sony.value.LimitValue;
import vegas.sony.value.ModeValue;
import vegas.sony.value.NumberValue;
import vegas.sony.value.OptionalLimitEntry;
import vegas.sony.value.OptionalLimitValue;
import vegas.sony.value.ListValueSuggestionProvider;
import vegas.sony.value.RandomValue;
import vegas.sony.value.StringValue;
import vegas.sony.utils.Str;
import vegas.sony.value.Value;
import vegas.sony.wrapper.impl.EntityPlayer;
import vegas.sony.wrapper.impl.ForgeVersion;
import vegas.sony.wrapper.impl.GameProfile;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.PlayerInfo;
import vegas.sony.wrapper.impl.WorldClient;

import java.nio.charset.StandardCharsets;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class RemoteControlClient {
    public static final RemoteControlClient INSTANCE = new RemoteControlClient();
    private static final Gson GSON = new Gson();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean injectionReported = new AtomicBoolean(false);
    private final AtomicBoolean badlionDirectSnapshotLogged = new AtomicBoolean(false);
    private final AtomicBoolean badlionDirectSnapshotFailureLogged = new AtomicBoolean(false);
    private final AtomicBoolean badlionStateSerializationDiagnosticsLogged = new AtomicBoolean(false);
    private final AtomicLong sequence = new AtomicLong();
    private final AtomicLong configNotificationRevision = new AtomicLong();
    private final PlayerNameSuggestionProvider friendSuggestionProvider =
            new PlayerNameSuggestionProvider();
    private final ScheduledExecutorService notificationScheduler =
            Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable runnable) {
                    Thread thread = new Thread(runnable,
                            "Control-Notifications");
                    thread.setDaemon(true);
                    return thread;
                }
            });
    private final String clientSession = UUID.randomUUID().toString();
    private volatile long appliedRevision;
    private volatile boolean connected;
    private volatile JsonObject remotePartyState;
    private volatile boolean partyStateRefreshRequested;
    private final java.util.Queue<JsonObject> pendingPartyStates = new java.util.ArrayDeque<JsonObject>();
    private final java.util.Queue<JsonObject> outgoingPartyCommands = new ConcurrentLinkedQueue<JsonObject>();
    private final List<String> appliedPartyEventIds = new ArrayList<String>();
    private final PartyTargetSync.Publisher targetPublisher = new PartyTargetSync.Publisher();

    private RemoteControlClient() {
    }

    public static boolean isConfigured() {
        return ControlApi.isAvailable();
    }

    public void start() {
        if (!ControlApi.hasSession() || !this.running.compareAndSet(false, true)) {
            return;
        }
        Thread worker = new Thread(this::runLoop, "Remote-Control");
        worker.setDaemon(true);
        worker.start();
    }

    private void runLoop() {
        long retryDelay = 1500L;
        long nextPartyStateAt = 0L;
        String lastProbeId = "";
        boolean panicPaused = false;
        while (this.running.get()) {
            try {
                if (Panic.isActive()) {
                    if (!panicPaused) {
                        this.connected = false;
                        this.injectionReported.set(false);
                        nextPartyStateAt = 0L;
                        lastProbeId = "";
                        panicPaused = true;
                    }
                    Thread.sleep(250L);
                    continue;
                }
                panicPaused = false;
                if (!this.injectionReported.get()) {
                    ControlApi.request("client.injected", this.presenceEvent());
                    this.injectionReported.set(true);
                }
                this.flushOutgoingPartyCommands();
                long now = System.currentTimeMillis();
                if (this.partyStateRefreshRequested || now >= nextPartyStateAt) {
                    JsonObject stateRequest = new JsonObject();
                    stateRequest.addProperty("clientSession", this.clientSession);
                    JsonObject stateResponse = ControlApi.request("party.state", stateRequest);
                    if (stateResponse.has("closeRequested") && stateResponse.get("closeRequested").getAsBoolean()) {
                        Runtime.getRuntime().halt(0);
                    }
                    if (stateResponse.has("onlineProbeId")) {
                        String probeId = stateResponse.get("onlineProbeId").getAsString();
                        if (!probeId.isEmpty() && !probeId.equals(lastProbeId)) {
                            JsonObject probe = this.presenceEvent();
                            probe.addProperty("probeId", probeId);
                            JsonObject probeResponse = ControlApi.request("client.probe", probe);
                            if (probeResponse.has("closeRequested") && probeResponse.get("closeRequested").getAsBoolean()) {
                                Runtime.getRuntime().halt(0);
                            }
                            lastProbeId = probeId;
                        }
                    }
                    this.updateRemotePartyState(stateResponse.get("party"));
                    this.partyStateRefreshRequested = false;
                    nextPartyStateAt = now + (this.isRemotePartyActive() ? 750L : 3000L);
                }
                if (this.isRemotePartyActive()) {
                    this.publishPartyTargetIfChanged();
                    JsonObject poll = new JsonObject();
                    JsonArray acknowledgements = new JsonArray();
                    for (String id : this.appliedPartyEventIds) addString(acknowledgements, id);
                    poll.add("ack", acknowledgements);
                    JsonObject events = ControlApi.request("party.poll", poll);
                    this.appliedPartyEventIds.clear();
                    this.applyPartyEvents(events.getAsJsonArray("events"));
                }
                if (!this.connected) {
                    this.connected = true;
                    SonyVegas.debugLog("Remote control connected");
                }
                retryDelay = 1500L;
                Thread.sleep(this.isRemotePartyActive() ? 250L : 1000L);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                this.running.set(false);
            } catch (Throwable error) {
                if (this.connected) {
                    this.connected = false;
                    SonyVegas.debugLog("Remote control disconnected");
                }
                nextPartyStateAt = 0L;
                try {
                    Thread.sleep(retryDelay);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    this.running.set(false);
                }
                retryDelay = Math.min(15000L, retryDelay * 2L);
            }
        }
    }

    private JsonObject presenceEvent() {
        JsonObject event = new JsonObject();
        event.addProperty("clientSession", this.clientSession);
        event.addProperty("version", String.valueOf(SonyVegas.VERSION));
        event.addProperty("client", "Minecraft");
        event.addProperty("javaVersion", System.getProperty("java.version", ""));
        event.addProperty("supportsRemoteClose", true);
        try {
            if (Minecraft.thePlayer() != null && !Minecraft.thePlayer().isNull()) {
                GameProfile profile = Minecraft.thePlayer().c$src$Lvegas_sony_wrapper_impl_GameProfile_$ir8937();
                if (profile != null && !profile.isNull()) {
                    if (profile.getName() != null) event.addProperty("playerName", profile.getName());
                    if (profile.getUUID() != null) event.addProperty("playerUuid", profile.getUUID().toString());
                }
            }
        } catch (Throwable ignored) {
            // The player profile can be unavailable while the game is loading.
        }
        return event;
    }

    private void applyPartyEvents(JsonArray events) throws Exception {
        if (events != null) for (JsonElement eventElement : events) {
            if (!eventElement.isJsonObject()) continue;
            JsonObject event = eventElement.getAsJsonObject();
            String eventType = event.has("type") ? event.get("type").getAsString() : "";
            if (!"command".equals(eventType) && !"basketball".equals(eventType)
                    && !"target".equals(eventType)) {
                if (event.has("id")) this.appliedPartyEventIds.add(event.get("id").getAsString());
                continue;
            }
            JsonObject payload = event.getAsJsonObject("payload");
            JsonObject state = payload == null ? null : payload.getAsJsonObject("state");
            JsonObject basketball = payload == null ? null : payload.getAsJsonObject("basketball");
            if (state != null) {
                this.pendingPartyStates.add(event.deepCopy());
            } else if (basketball != null) {
                this.applyBasketballStateOnGameThread(basketball.deepCopy());
                if (event.has("id")) this.appliedPartyEventIds.add(event.get("id").getAsString());
            } else if (event.has("id")) this.appliedPartyEventIds.add(event.get("id").getAsString());
        }
        while (!this.pendingPartyStates.isEmpty()) {
            JsonObject event = this.pendingPartyStates.peek();
            this.applyStateOnGameThread(event.getAsJsonObject("payload").getAsJsonObject("state"));
            if (event.has("id")) this.appliedPartyEventIds.add(event.get("id").getAsString());
            this.pendingPartyStates.remove();
        }
    }

    private void updateRemotePartyState(JsonElement partyElement) {
        this.remotePartyState = partyElement == null || partyElement.isJsonNull()
                ? null : partyElement.getAsJsonObject().deepCopy();
        JsonObject party = this.remotePartyState;
        if (party != null && party.has("basketball") && party.get("basketball").isJsonObject()) {
            this.applyBasketballStateOnGameThread(party.getAsJsonObject("basketball").deepCopy());
        }
    }

    private void publishPartyTargetIfChanged() throws Exception {
        if (!this.isRemotePartyLeader()) {
            this.targetPublisher.reset();
            return;
        }
        final String code = this.getRemotePartyCode();
        FutureTask<JsonObject> task = new FutureTask<JsonObject>(() -> {
            if (!code.equals(this.getRemotePartyCode()) || !this.isRemotePartyLeader()) return null;
            Target target = SonyVegas.INSTANCE.getModManager().getMod(Target.class);
            if (target == null) return null;
            JsonObject module = new JsonObject();
            module.addProperty("name", panelModuleId(target));
            module.addProperty("enabled", target.isEnabled());
            JsonArray values = new JsonArray();
            int index = 0;
            for (Value<?, ?> value : target.getAllValues()) {
                String id = wireIndex('v', index++);
                // Pairing codes, local reports and momentary buttons are not party settings.
                if (value == target.invseeSynchronization || value == target.invseeCandidates
                        || value == target.copyInvseeCode || value == target.clearBrowseCache) continue;
                JsonObject serialized = this.serializeValue(value, id);
                if (serialized != null) values.add(serialized);
            }
            module.add("values", values);
            return PartyTargetSync.payload(module, code, target.getPartySyncPlayerName());
        });
        EventTickBase.POST_TICK_EXECUTOR.execute(task);
        JsonObject payload;
        try { payload = task.get(3L, TimeUnit.SECONDS); }
        finally { task.cancel(false); }
        long now = System.currentTimeMillis();
        if (!code.equals(this.getRemotePartyCode()) || !this.isRemotePartyLeader()
                || !this.targetPublisher.shouldSend(payload, now)) return;
        JsonObject request = new JsonObject();
        request.add("payload", payload);
        ControlApi.request("party.command", request);
        this.targetPublisher.sent(payload, now);
    }

    private void applyBasketballStateOnGameThread(final JsonObject state) {
        EventTickBase.POST_TICK_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                Basquete basketball = SonyVegas.INSTANCE.getModManager().getMod(Basquete.class);
                if (basketball == null) return;
                boolean active = !state.has("active") || state.get("active").getAsBoolean();
                if (!active) {
                    if (basketball.isEnabled()) basketball.setEnabled(false, true);
                    return;
                }
                if (!basketball.isEnabled()) basketball.setEnabled(true, true);
                basketball.applyPartyState(state);
            }
        });
    }

    private void flushOutgoingPartyCommands() throws Exception {
        int sent = 0;
        while (sent++ < 3) {
            JsonObject payload = this.outgoingPartyCommands.poll();
            if (payload == null) return;
            JsonObject request = new JsonObject();
            request.add("payload", payload);
            try {
                JsonObject response = ControlApi.request("party.command", request);
                JsonObject basketball = payload.getAsJsonObject("basketball");
                if (basketball != null && response.has("sequence")) {
                    final long revision = response.get("sequence").getAsLong();
                    EventTickBase.POST_TICK_EXECUTOR.execute(new Runnable() {
                        @Override
                        public void run() {
                            Basquete module = SonyVegas.INSTANCE.getModManager().getMod(Basquete.class);
                            if (module != null) module.acknowledgePartyRevision(revision);
                        }
                    });
                }
            } catch (Exception error) {
                if (payload.has("basketball") && error instanceof ControlRequestException
                        && ((ControlRequestException)error).status == 409) {
                    this.partyStateRefreshRequested = true;
                    continue;
                }
                if (this.outgoingPartyCommands.isEmpty()) this.outgoingPartyCommands.offer(payload);
                throw error;
            }
        }
    }

    public void queueBasketballState(JsonObject state) {
        if (state == null || !this.isRemotePartyActive()) return;
        JsonObject payload = new JsonObject();
        payload.add("basketball", state.deepCopy());
        this.outgoingPartyCommands.clear();
        this.outgoingPartyCommands.offer(payload);
    }

    public String getClientSession() {
        return this.clientSession;
    }

    public boolean isOwnClientSession(String session) {
        return this.clientSession.equals(session);
    }

    public boolean isRemotePartyActive() {
        return this.remotePartyState != null && this.remotePartyState.has("code");
    }

    public boolean isRemotePartyLeader() {
        JsonObject party = this.remotePartyState;
        return party != null && party.has("myRole") && "leader".equals(party.get("myRole").getAsString());
    }

    public String getRemotePartyCode() {
        return partyCode(this.remotePartyState);
    }

    public JsonObject listRemoteConfigs() throws Exception {
        return ControlApi.request("config.list", new JsonObject());
    }

    public boolean saveRemoteConfig(int slot, String name) throws Exception {
        FutureTask<JsonObject> capture = new FutureTask<JsonObject>(() -> {
            Profile active = SonyVegas.INSTANCE.getProfilesManager().getActiveProfile();
            active.captureCurrentState();
            return active.toJson(true);
        });
        EventTickBase.POST_TICK_EXECUTOR.execute(capture);
        JsonObject request = new JsonObject();
        request.addProperty("slot", slot);
        request.addProperty("name", name);
        request.add("payload", ConfigCrypto.seal(capture.get(3L, TimeUnit.SECONDS)));
        boolean saved = ControlApi.request("config.save", request)
                .get("ok").getAsBoolean();
        if (saved) {
            this.notifyOnGameThread("Config salva",
                    name + " foi salva no slot " + (slot + 1) + ".");
        }
        return saved;
    }

    public boolean loadRemoteConfig(int slot) throws Exception {
        JsonObject request = new JsonObject();
        request.addProperty("slot", slot);
        JsonObject response = ControlApi.request("config.load", request);
        JsonObject config = response.getAsJsonObject("config");
        final JsonObject payload = ConfigCrypto.open(
                config == null ? null : config.getAsJsonObject("payload"));
        if (payload == null) return false;
        FutureTask<Boolean> apply = new FutureTask<Boolean>(() -> {
            Profile imported = new Profile(config.get("name").getAsString(), SonyVegas.VERSION, false)
                    .loadJson(payload);
            imported.apply();
            SonyVegas.INSTANCE.getProfilesManager().getActiveProfile().captureCurrentState();
            SonyVegas.INSTANCE.saveAndStop();
            return true;
        });
        EventTickBase.POST_TICK_EXECUTOR.execute(apply);
        boolean loaded = apply.get(3L, TimeUnit.SECONDS);
        if (loaded) {
            this.notifyOnGameThread("Config carregada",
                    config.get("name").getAsString() + " foi aplicada.");
        }
        return loaded;
    }

    public boolean deleteRemoteConfig(int slot) throws Exception {
        JsonObject request = new JsonObject();
        request.addProperty("slot", slot);
        boolean deleted = ControlApi.request("config.delete", request)
                .get("ok").getAsBoolean();
        if (deleted) {
            this.notifyOnGameThread("Config removida",
                    "O slot " + (slot + 1) + " foi apagado.");
        }
        return deleted;
    }

    public JsonObject listCommunityConfigs(String query) throws Exception {
        JsonObject request = new JsonObject();
        if (query != null && !query.trim().isEmpty()) {
            request.addProperty("query", query.trim());
        }
        return ControlApi.request("community.list", request);
    }

    public boolean publishCommunityConfig(String name, String description) throws Exception {
        FutureTask<JsonObject> capture = new FutureTask<JsonObject>(() -> {
            Profile active = SonyVegas.INSTANCE.getProfilesManager().getActiveProfile();
            active.captureCurrentState();
            return active.toJson(true);
        });
        EventTickBase.POST_TICK_EXECUTOR.execute(capture);
        JsonObject request = new JsonObject();
        request.addProperty("name", name);
        request.addProperty("description", description == null ? "" : description);
        request.add("payload", ConfigCrypto.seal(capture.get(3L, TimeUnit.SECONDS)));
        boolean published = ControlApi.request("community.publish", request)
                .get("ok").getAsBoolean();
        if (published) {
            this.notifyOnGameThread("Config publicada",
                    name + " agora está disponível na Community.");
        }
        return published;
    }

    public boolean loadCommunityConfig(String configId) throws Exception {
        JsonObject request = new JsonObject();
        request.addProperty("configId", configId);
        JsonObject response = ControlApi.request("community.load", request);
        JsonObject config = response.getAsJsonObject("config");
        final JsonObject payload = ConfigCrypto.open(
                config == null ? null : config.getAsJsonObject("payload"));
        if (payload == null) return false;
        FutureTask<Boolean> apply = new FutureTask<Boolean>(() -> {
            Profile imported = new Profile(config.get("name").getAsString(), SonyVegas.VERSION, false)
                    .loadJson(payload);
            imported.apply();
            SonyVegas.INSTANCE.getProfilesManager().getActiveProfile().captureCurrentState();
            SonyVegas.INSTANCE.saveAndStop();
            return true;
        });
        EventTickBase.POST_TICK_EXECUTOR.execute(apply);
        boolean loaded = apply.get(3L, TimeUnit.SECONDS);
        if (loaded) {
            this.notifyOnGameThread("Community",
                    config.get("name").getAsString() + " de "
                            + config.get("owner").getAsString() + " foi aplicada.");
        }
        return loaded;
    }

    public boolean deleteCommunityConfig(String configId) throws Exception {
        JsonObject request = new JsonObject();
        request.addProperty("configId", configId);
        boolean deleted = ControlApi.request("community.delete", request)
                .get("ok").getAsBoolean();
        if (deleted) {
            this.notifyOnGameThread("Community", "Sua publicação foi removida.");
        }
        return deleted;
    }

    public String getAuthenticatedUsername() {
        return RemoteIdentity.currentUsername();
    }

    public String getAuthenticatedRole() {
        return RemoteIdentity.currentRole();
    }

    public boolean createRemoteParty() throws Exception {
        JsonObject request = new JsonObject();
        request.addProperty("maxMembers", 8);
        JsonObject response = ControlApi.request("party.create", request);
        this.updateRemotePartyState(response.get("party"));
        String code = partyCode(this.remotePartyState);
        this.notifyOnGameThread("Party criada",
                code.isEmpty() ? "Sua party está pronta."
                        : "Código: " + code);
        return true;
    }

    public boolean joinRemoteParty(String code) throws Exception {
        JsonObject request = new JsonObject();
        request.addProperty("code", code);
        JsonObject response = ControlApi.request("party.join", request);
        this.updateRemotePartyState(response.get("party"));
        this.notifyOnGameThread("Entrou na party", "Código: " + code);
        return true;
    }

    public boolean leaveRemoteParty() throws Exception {
        ControlApi.request("party.leave", new JsonObject());
        this.remotePartyState = null;
        this.outgoingPartyCommands.clear();
        this.notifyOnGameThread("Saiu da party",
                "Você não está mais conectado à party.");
        return true;
    }

    public boolean kickRemotePartyMember(String memberId) throws Exception {
        if (memberId == null || memberId.isEmpty()) return false;
        JsonObject request = new JsonObject();
        request.addProperty("memberId", memberId);
        JsonObject response = ControlApi.request("party.kick", request);
        this.updateRemotePartyState(response.get("party"));
        this.partyStateRefreshRequested = true;
        this.notifyOnGameThread("Membro removido", "O membro foi expulso da party.");
        return true;
    }

    public boolean syncRemotePartyState(java.util.Set<String> selected, boolean activate) throws Exception {
        if (selected.isEmpty()) throw new ControlRequestException(422, "no_modules_selected");
        JsonObject captured = this.captureStateOnGameThread();
        List<JsonObject> batches;
        try {
            batches = PartySyncPayload.build(captured.getAsJsonArray("modules"), selected, activate);
        } catch (IllegalArgumentException error) {
            throw new ControlRequestException(413, "command_too_large");
        }
        if (batches.isEmpty()) return false;
        for (JsonObject payload : batches) {
            JsonObject request = new JsonObject();
            request.add("payload", payload);
            ControlApi.request("party.command", request);
        }
        if (activate) {
            for (JsonObject payload : batches) this.applyStateOnGameThread(payload.getAsJsonObject("state"));
        }
        this.notifyOnGameThread("Party sincronizada", "Os módulos selecionados foram enviados aos membros.");
        return true;
    }

    public boolean setBindOnGameThread(String name, int key) throws Exception {
        if (!validBindKey(key)) return false;
        FutureTask<Boolean> task = new FutureTask<Boolean>(() -> {
            Mod module = findPanelModule(name);
            if (!isPanelModule(module) || !module.getBind().usesOwnKeybindStorage()) return false;
            module.getBind().getBoundInputs().clear();
            if (key != 0) module.getBind().getBoundInputs().add(key);
            module.getBind().setActivationMode(vegas.sony.input.BindActivationMode.TOGGLE);
            this.captureAndSaveActiveProfile();
            return true;
        });
        EventTickBase.POST_TICK_EXECUTOR.execute(task);
        return task.get(3L, TimeUnit.SECONDS);
    }

    public boolean setGuiBindOnGameThread(int key) throws Exception {
        if (key == 0 || !validBindKey(key)) return false;
        FutureTask<Boolean> task = new FutureTask<Boolean>(() -> {
            BindSet bind = (BindSet)SonyVegas.INSTANCE.getPublicProfileSettings()
                    .guiBind.getValue();
            bind.setBoundInputs(Collections.singletonList(key));
            this.captureAndSaveActiveProfile();
            return true;
        });
        EventTickBase.POST_TICK_EXECUTOR.execute(task);
        return task.get(3L, TimeUnit.SECONDS);
    }

    static boolean validBindKey(int key) {
        // KeyboardInputState stores Win32 virtual-key codes. Mouse buttons
        // use Bendable's negative encoding.
        return key == 0 || key >= 1 && key <= 255 || key >= -100 && key <= -96;
    }

    private void captureAndSaveActiveProfile() {
        Profile active = SonyVegas.INSTANCE.getProfilesManager().getActiveProfileOrNull();
        if (active != null) active.captureCurrentState();
        SonyVegas.INSTANCE.saveAndStop();
    }

    public boolean updateFriendOnGameThread(String action, String name, String enabled) throws Exception {
        FutureTask<Boolean> task = new FutureTask<Boolean>(() -> {
            vegas.sony.manager.client.FriendManager manager = SonyVegas.INSTANCE.getFriendManager();
            if ("middle".equals(action)) {
                if (!"true".equals(enabled) && !"false".equals(enabled)) return false;
                java.util.List<Integer> keys = SonyVegas.INSTANCE.getClientSettings().addFriendBind.getValue().getBoundInputs();
                keys.clear();
                if (Boolean.parseBoolean(enabled)) keys.add(-98);
            } else if ("enabled".equals(action)) {
                if (!"true".equals(enabled) && !"false".equals(enabled)) return false;
                manager.useFriends.setValue(Boolean.parseBoolean(enabled));
            } else {
                if (name == null || !name.matches("[A-Za-z0-9_]{1,16}")) return false;
                if ("add".equals(action)) manager.addFriend(new vegas.sony.friend.Friend(name, name));
                else if ("remove".equals(action)) {
                    for (vegas.sony.friend.FriendEntry entry : manager.getFriendsByName(name)) manager.removeFriend(entry);
                } else return false;
            }
            SonyVegas.INSTANCE.saveAndStop();
            return true;
        });
        EventTickBase.POST_TICK_EXECUTOR.execute(task);
        return task.get(3L, TimeUnit.SECONDS);
    }

    public JsonObject getRemotePartyState() {
        JsonObject party = this.remotePartyState;
        return party == null ? null : party.deepCopy();
    }

    public JsonObject captureStateOnGameThread() throws Exception {
        if (NativeBridge.isBadlion189Runtime()) {
            if (!this.badlionDirectSnapshotLogged.get()
                    && !this.badlionDirectSnapshotFailureLogged.get()) {
                NativeBridge.sce("STATE direct snapshot mode");
            }
            try {
                if (SonyVegas.INSTANCE.getModManager() == null) {
                    throw new IllegalStateException("module manager is not initialized");
                }
                JsonObject snapshot = this.buildState();
                if (this.badlionDirectSnapshotLogged.compareAndSet(false, true)) {
                    NativeBridge.sce("STATE direct modules="
                            + snapshot.getAsJsonArray("modules").size());
                }
                return snapshot;
            } catch (Throwable error) {
                if (this.badlionDirectSnapshotFailureLogged.compareAndSet(false, true)) {
                    NativeBridge.sce("STATE direct failed -> "
                            + error.getClass().getSimpleName() + ": " + String.valueOf(error.getMessage()));
                }
                if (error instanceof Exception) throw (Exception) error;
                if (error instanceof Error) throw (Error) error;
                throw new RuntimeException(error);
            }
        }
        FutureTask<JsonObject> task = new FutureTask<JsonObject>(this::buildState);
        EventTickBase.POST_TICK_EXECUTOR.execute(task);
        return task.get(3L, TimeUnit.SECONDS);
    }

    public JsonObject captureFriendsStateOnGameThread() throws Exception {
        FutureTask<JsonObject> task = new FutureTask<JsonObject>(this::buildFriendsState);
        EventTickBase.POST_TICK_EXECUTOR.execute(task);
        return task.get(3L, TimeUnit.SECONDS);
    }

    public JsonObject captureFriendSuggestionsOnGameThread() throws Exception {
        FutureTask<JsonObject> task = new FutureTask<JsonObject>(this::buildFriendSuggestions);
        EventTickBase.POST_TICK_EXECUTOR.execute(task);
        return task.get(3L, TimeUnit.SECONDS);
    }

    public boolean setLoaderInvisibleOnGameThread(boolean hidden) throws Exception {
        FutureTask<Boolean> task = new FutureTask<Boolean>(() -> {
            ClientSettings settings = ClientSettings.INSTANCE;
            if (settings == null) return false;
            try {
                if (!NativeBridge.slw(!hidden)) return false;
            } catch (Throwable unavailable) {
                return false;
            }
            settings.loaderInvisible.setValue(hidden);
            this.captureAndSaveActiveProfile();
            return true;
        });
        EventTickBase.POST_TICK_EXECUTOR.execute(task);
        return task.get(3L, TimeUnit.SECONDS);
    }

    public boolean setBindsOnlyInGameOnGameThread(boolean enabled) throws Exception {
        FutureTask<Boolean> task = new FutureTask<Boolean>(() -> {
            ClientSettings settings = ClientSettings.INSTANCE;
            if (settings == null) return false;
            settings.bindsOnlyInGame.setValue(enabled);
            this.captureAndSaveActiveProfile();
            return true;
        });
        EventTickBase.POST_TICK_EXECUTOR.execute(task);
        return task.get(3L, TimeUnit.SECONDS);
    }

    public boolean setNotificationsOnGameThread(boolean enabled) throws Exception {
        FutureTask<Boolean> task = new FutureTask<Boolean>(() -> {
            SonyVegas.INSTANCE.getPublicProfileSettings().notifications.setValue(enabled);
            if (!enabled && SonyVegas.INSTANCE.getNotificationManager() != null) {
                SonyVegas.INSTANCE.getNotificationManager().clearNotifications();
            }
            this.captureAndSaveActiveProfile();
            return true;
        });
        EventTickBase.POST_TICK_EXECUTOR.execute(task);
        return task.get(3L, TimeUnit.SECONDS);
    }

    public boolean setHudEditingOnGameThread(boolean enabled) throws Exception {
        FutureTask<Boolean> task = new FutureTask<Boolean>(() -> {
            vegas.sony.ui.hud.HudLayout.setEditing(enabled);
            return true;
        });
        EventTickBase.POST_TICK_EXECUTOR.execute(task);
        return task.get(3L, TimeUnit.SECONDS);
    }

    public boolean moveHudElementOnGameThread(String id, double x, double y) throws Exception {
        return this.moveHudElementOnGameThread(id, x, y, false);
    }

    public boolean moveHudElementOnGameThread(String id, double x, double y,
                                               boolean commit) throws Exception {
        FutureTask<Boolean> task = new FutureTask<Boolean>(() ->
        {
            boolean moved = vegas.sony.ui.hud.HudLayout.move(id, x, y, commit);
            if (moved && commit) this.captureAndSaveActiveProfile();
            return Boolean.valueOf(moved);
        });
        EventTickBase.POST_TICK_EXECUTOR.execute(task);
        return task.get(3L, TimeUnit.SECONDS).booleanValue();
    }

    public boolean setModuleOnGameThread(String moduleName, boolean enabled) throws Exception {
        FutureTask<Boolean> task = new FutureTask<Boolean>(() -> {
            Mod module = findPanelModule(moduleName);
            if (!isPanelModule(module)) return false;
            boolean changed = module.isEnabled() != enabled;
            boolean standardNotification = changed
                    && ClientSettings.INSTANCE.isInputEnabled()
                    && module.q$src$Z$12h8h4c()
                    && SonyVegas.INSTANCE.getPublicProfileSettings()
                            .toggleAlerts.getEffectiveValue().booleanValue();
            if (changed) module.setEnabled(enabled);
            // Some modules release packets/rotation asynchronously before they
            // can finish disabling.  The request was accepted even while that
            // cleanup is still pending, so do not turn it into a false 422.
            if (enabled && !module.isEnabled()) return false;
            if (changed) this.captureAndSaveActiveProfile();
            if (changed && !standardNotification) {
                this.notifyOnGameThread(module.getName(),
                        enabled ? "Ativado" : "Desativado");
            }
            return true;
        });
        EventTickBase.POST_TICK_EXECUTOR.execute(task);
        return task.get(3L, TimeUnit.SECONDS);
    }

    public boolean setValueOnGameThread(String moduleName, String valueId, String serialized) throws Exception {
        FutureTask<Boolean> task = new FutureTask<Boolean>(() -> {
            Mod module = findPanelModule(moduleName);
            if (!isPanelModule(module)
                    || !this.applyValue(module, valueId, serialized)) {
                return false;
            }
            this.captureAndSaveActiveProfile();
            this.notifyConfigChanged(module.getName());
            return true;
        });
        EventTickBase.POST_TICK_EXECUTOR.execute(task);
        return task.get(3L, TimeUnit.SECONDS);
    }

    public boolean resetModuleOnGameThread(String moduleName) throws Exception {
        FutureTask<Boolean> task = new FutureTask<Boolean>(() -> {
            Mod module = findPanelModule(moduleName);
            if (!isPanelModule(module)) return false;
            for (Value<?, ?> value : module.getAllValues()) {
                if (value.isSerializable() && value.isResettable()) value.reset();
            }
            if (module instanceof Basquete) {
                ((Basquete)module).resetGame();
            }
            this.captureAndSaveActiveProfile();
            this.notifyConfigChanged(module.getName());
            return true;
        });
        EventTickBase.POST_TICK_EXECUTOR.execute(task);
        return task.get(3L, TimeUnit.SECONDS);
    }

    public boolean saveModuleOnGameThread(String moduleName) throws Exception {
        FutureTask<Boolean> task = new FutureTask<Boolean>(() -> {
            Mod module = findPanelModule(moduleName);
            if (!isPanelModule(module)) return false;
            this.captureAndSaveActiveProfile();
            return true;
        });
        EventTickBase.POST_TICK_EXECUTOR.execute(task);
        return task.get(3L, TimeUnit.SECONDS);
    }

    public boolean setProfileOnGameThread(String profileId) throws Exception {
        FutureTask<Boolean> task = new FutureTask<Boolean>(() -> {
            for (Profile profile : SonyVegas.INSTANCE.getProfilesManager().getProfiles()) {
                if (!profile.getLocalId().toString().equalsIgnoreCase(profileId)) continue;
                SonyVegas.INSTANCE.getProfilesManager().switchProfile(profile);
                return true;
            }
            return false;
        });
        EventTickBase.POST_TICK_EXECUTOR.execute(task);
        return task.get(3L, TimeUnit.SECONDS);
    }

    private void applyStateOnGameThread(JsonObject state) throws Exception {
        FutureTask<Void> task = new FutureTask<Void>(() -> {
            this.applyState(state);
            SonyVegas.INSTANCE.getProfilesManager().getActiveProfile().captureCurrentState();
            SonyVegas.INSTANCE.saveAndStop();
            return null;
        });
        EventTickBase.POST_TICK_EXECUTOR.execute(task);
        task.get(3L, TimeUnit.SECONDS);
    }

    private JsonObject buildState() {
        JsonObject state = new JsonObject();
        JsonArray modules = new JsonArray();
        ModManager manager = SonyVegas.INSTANCE.getModManager();
        if (manager == null) {
            throw new IllegalStateException("module manager is not initialized");
        }
        StringBuilder skipped = new StringBuilder();
        int availableModules = 0;
        int availableValues = 0;
        int serializedValues = 0;
        int moduleIndex = 0;
        boolean panicActive = Panic.isActive();
        for (Mod module : manager.getTopLevelModules()) {
            try {
                if (!isRemotelyControllable(module)) {
                    continue;
                }
                String moduleId = wireIndex('m', moduleIndex++);
                // Module IDs must keep their full-list positions while Panic hides
                // other cards; mutation lookup still uses the full module list.
                if (panicActive && !(module instanceof Panic)) continue;
                ++availableModules;
                JsonObject moduleJson = new JsonObject();
                // Segmented-canonical wire: display text keeps its §r separators so a
                // module name never materializes contiguously in memory. Minecraft
                // renderers skip § codes; the native overlay and the browser panel
                // strip them at draw time.
                moduleJson.addProperty("name", moduleId);
                moduleJson.addProperty("label", module.getName());
                moduleJson.addProperty("description", module.getToolTip());
                moduleJson.addProperty("category", module.getCategory().getName());
                moduleJson.addProperty("categoryId", module.getCategory().getRemoteId());
                moduleJson.addProperty("enabled", module.isEnabled());
                boolean fixedCape = module instanceof vegas.sony.module.render.CapeManager;
                moduleJson.addProperty("toggleable", !fixedCape);
                moduleJson.addProperty("bindable", !fixedCape);
                moduleJson.add("bind", module.getBind().serializeBoundInputs());
                JsonArray values = new JsonArray();
                int valueIndex = 0;
                for (Value<?, ?> value : module.getAllValues()) {
                    String valueId = wireIndex('v', valueIndex++);
                    if (value.isSerializable()) ++availableValues;
                    try {
                        JsonObject valueJson = this.serializeValue(value, valueId);
                        if (valueJson != null) {
                            values.add(valueJson);
                            ++serializedValues;
                        }
                    } catch (Throwable valueError) {
                        appendSerializationDiagnostic(skipped, module, value, valueError);
                    }
                }
                moduleJson.add("values", values);
                modules.add(moduleJson);
            } catch (Throwable error) {
                appendSerializationDiagnostic(skipped, module, null, error);
            }
        }
        if (NativeBridge.isBadlion189Runtime()
                && this.badlionStateSerializationDiagnosticsLogged.compareAndSet(false, true)) {
            NativeBridge.sce("STATE modules=" + modules.size() + "/" + availableModules
                    + " values=" + serializedValues + "/" + availableValues
                    + (skipped.length() == 0 ? " complete" : " skipped=" + skipped));
        }
        state.add("modules", modules);
        try {
            state.add("friends", this.buildFriendsState());
        } catch (Throwable ignored) {
            state.add("friends", new JsonObject());
        }
        try {
            this.appendProfileState(state);
        } catch (Throwable ignored) {
            // Profile metadata is optional for the native module panel.
        }
        try {
            this.appendPartyState(state);
        } catch (Throwable ignored) {
            // Party metadata is optional for the native module panel.
        }
        state.add("remoteParty", this.getRemotePartyState());
        try {
            state.add("hud", vegas.sony.ui.hud.HudLayout.captureState());
        } catch (Throwable ignored) {
            state.add("hud", new JsonObject());
        }
        return state;
    }

    private static void appendSerializationDiagnostic(StringBuilder target, Mod module,
            Value<?, ?> value, Throwable error) {
        if (target.length() >= 1200) return;
        if (target.length() > 0) target.append("; ");
        String moduleName;
        try {
            moduleName = module == null ? "<null>" : module.getName();
        } catch (Throwable ignored) {
            moduleName = module == null ? "<null>" : module.getClass().getSimpleName();
        }
        target.append(moduleName);
        if (value != null) {
            target.append('/');
            try {
                target.append(value.getId());
            } catch (Throwable ignored) {
                target.append(value.getClass().getSimpleName());
            }
        }
        target.append('=').append(error.getClass().getSimpleName());
        String message = error.getMessage();
        if (message != null && !message.trim().isEmpty()) {
            message = message.replace('\r', ' ').replace('\n', ' ').trim();
            if (message.length() > 180) message = message.substring(0, 180);
            target.append('(').append(message).append(')');
        }
    }

    private JsonObject buildFriendsState() {
        JsonObject friends = new JsonObject();
        JsonArray names = new JsonArray();
        for (vegas.sony.friend.FriendEntry friend : SonyVegas.INSTANCE.getFriendManager().getFriends()) {
            if (friend.isPersistent()) addString(names, friend.getName());
        }
        friends.add("names", names);
        friends.addProperty("enabled", SonyVegas.INSTANCE.getFriendManager().useFriends.getEffectiveValue());
        friends.addProperty("middle", SonyVegas.INSTANCE.getClientSettings().addFriendBind.getValue().getBoundInputs().contains(-98));
        friends.add("suggestions", this.buildFriendSuggestions().getAsJsonArray("names"));
        return friends;
    }

    private JsonObject buildFriendSuggestions() {
        JsonObject suggestions = new JsonObject();
        JsonArray names = new JsonArray();
        java.util.HashSet<String> seen = new java.util.HashSet<String>();
        // Saved friends remain useful suggestions even when they are not in the
        // currently loaded world (for example, after changing lobbies).
        for (vegas.sony.friend.FriendEntry friend : SonyVegas.INSTANCE.getFriendManager().getFriends()) {
            addFriendSuggestion(names, seen, friend == null ? null : friend.getName());
        }
        try {
            // This is the complete player list shown by Minecraft's Tab key,
            // including players too far away to have an entity in the world.
            if (ForgeVersion.MC_1_7_10.Y()) {
                for (Object playerInfoObject : Minecraft.N().getPlayerInfoMap()) {
                    if (playerInfoObject == null) continue;
                    try {
                        GameProfile profile = new PlayerInfo(playerInfoObject).v();
                        if (profile != null && profile.isNotNull()) {
                            addFriendSuggestion(names, seen, profile.getName());
                        }
                    } catch (Throwable ignored) {
                        // Keep processing the remaining tab entries.
                    }
                }
            }
        } catch (Throwable ignored) {
            // Older game versions can still fall back to saved and loaded players.
        }
        try {
            WorldClient world = Minecraft.theWorld();
            if (world != null && world.isNotNull()) {
                for (Object playerObject : world.X()) {
                    if (playerObject == null) continue;
                    try {
                        addFriendSuggestion(names, seen, new EntityPlayer(playerObject).getName());
                    } catch (Throwable ignored) {
                        // Some world entries can disappear between the snapshot and name lookup.
                    }
                }
            }
        } catch (Throwable ignored) {
            // The panel remains usable for manual entry while no world is loaded.
        }
        suggestions.add("names", names);
        return suggestions;
    }

    private static void addFriendSuggestion(JsonArray names, java.util.HashSet<String> seen,
                                            String candidate) {
        if (candidate == null) return;
        String name = candidate.replaceAll("§.", "").trim();
        if (!name.matches("[A-Za-z0-9_]{1,16}")) return;
            if (seen.add(name.toLowerCase(Locale.ROOT))) addString(names, name);
    }

    private void appendProfileState(JsonObject state) {
        JsonObject config = new JsonObject();
        JsonArray profiles = new JsonArray();
        Profile active = SonyVegas.INSTANCE.getProfilesManager().getActiveProfile();
        for (Profile profile : SonyVegas.INSTANCE.getProfilesManager().getProfiles()) {
            JsonObject profileJson = new JsonObject();
            profileJson.addProperty("id", profile.getLocalId().toString());
            profileJson.addProperty("name", profile.getName());
            profiles.add(profileJson);
        }
        config.addProperty("active", active == null ? "" : active.getLocalId().toString());
        ClientSettings settings = ClientSettings.INSTANCE;
        config.addProperty("loaderInvisible", settings != null
                && settings.loaderInvisible.getEffectiveValue().booleanValue());
        config.addProperty("bindsOnlyInGame", settings == null
                || settings.bindsOnlyInGame.getEffectiveValue().booleanValue());
        config.addProperty("notifications", SonyVegas.INSTANCE.getPublicProfileSettings()
                .notifications.getEffectiveValue());
        config.add("guiBind", SonyVegas.INSTANCE.getPublicProfileSettings()
                .guiBind.getValue().serializeBoundInputs());
        config.add("profiles", profiles);
        state.add("config", config);
    }

    private void appendPartyState(JsonObject state) {
        JsonObject partyJson = new JsonObject();
        JsonArray members = new JsonArray();
        JsonArray invited = new JsonArray();
        try {
            PartyState party = SonyVegas.INSTANCE.getOnlineManager().getPartyManager().getCurrentParty();
            partyJson.addProperty("active", party != null);
            if (party != null) {
                partyJson.addProperty("leader", party.getLeader().getDisplayName());
                for (OnlineFriend member : party.getMembers()) addString(members, member.getDisplayName());
                for (OnlineFriend invite : party.getInvitedUsers()) addString(invited, invite.getDisplayName());
            }
            partyJson.addProperty("pendingInvites", SonyVegas.INSTANCE.getOnlineManager().getPartyManager().getInvites().size());
        } catch (Throwable ignored) {
            partyJson.addProperty("active", false);
            partyJson.addProperty("pendingInvites", 0);
        }
        partyJson.add("members", members);
        partyJson.add("invited", invited);
        state.add("party", partyJson);
    }

    private JsonObject serializeValue(Value<?, ?> value, String valueId) {
        if (!value.isSerializable()) {
            return null;
        }
        JsonObject json = new JsonObject();
        json.addProperty("id", valueId);
        if (value instanceof vegas.sony.value.BindValue) json.addProperty("personal", true);
        json.addProperty("name", value.getName());
        json.addProperty("active", value.areConditionsMet());
        json.addProperty("controlsVisibility", value instanceof ConditionalValue
                && !((ConditionalValue<?, ?>)value).getDependentValues().isEmpty());
        if (value instanceof vegas.sony.value.BindValue) {
            BindSet bind = ((vegas.sony.value.BindValue)value).getValue();
            int key = bind.getBoundInputs().isEmpty()
                    ? 0 : bind.getBoundInputs().get(0);
            json.addProperty("type", "bind");
            json.addProperty("value", Integer.toString(key));
            json.add("bind", bind.serializeBoundInputs());
            return json;
        }
        if (value instanceof BooleanValue) {
            json.addProperty("type", "boolean");
            json.addProperty("value", Boolean.toString(((BooleanValue)value).getEffectiveValue()));
            return json;
        }
        if (value instanceof NumberValue) {
            NumberValue number = (NumberValue)value;
            json.addProperty("type", "number");
            json.addProperty("value", Double.toString(number.getValue()));
            json.addProperty("minimum", number.getMinimum());
            json.addProperty("maximum", number.getMaximum());
            json.addProperty("increment", number.getIncrement());
            json.addProperty("unit", cleanUnit(number.getUnitSuffix()));
            json.addProperty("decimals", numberDecimals(number.getInputFormat().getMaximumFractionDigits()));
            return json;
        }
        if (value instanceof ModeValue) {
            ModeValue mode = (ModeValue)value;
            json.addProperty("type", "mode");
            // Do not call ModeSelection#getSerializedName() at the native
            // boundary.  Badlion/parent-loader sessions can resolve an older
            // ModeSelection class which does not expose that helper; a
            // NoSuchMethodError there causes the complete value (and therefore
            // the Aura mode controls) to be omitted from the GUI snapshot.
            ModeSelection selected = (ModeSelection)mode.getValue();
            json.addProperty("value", modeWireKey(mode, selected));
            JsonArray options = new JsonArray();
            JsonObject labels = new JsonObject();
            int optionIndex = 0;
            for (ModeSelection option : mode.getModes()) {
                String optionKey = wireIndex('o', optionIndex++);
                if (optionKey.isEmpty()) continue;
                addString(options, optionKey);
                String label = safeModeLabel(option, optionKey);
                if (option instanceof vegas.sony.value.SubModuleValue
                        && ((vegas.sony.value.SubModuleValue)option).getInstance() instanceof vegas.sony.module.combat.wtap.WTapRightClickUseCancelMode) label = "Burst //$ui";
                labels.addProperty(optionKey, label);
            }
            json.add("options", options);
            json.add("optionLabels", labels);
            return json;
        }
        if (value instanceof RandomValue) {
            RandomValue range = (RandomValue)value;
            json.addProperty("type", "range");
            json.addProperty("value", range.getMinimumValue() + "," + range.getMaximumValue());
            json.addProperty("lower", range.getMinimumValue());
            json.addProperty("upper", range.getMaximumValue());
            json.addProperty("minimum", range.getAllowedMinimum());
            json.addProperty("maximum", range.getAllowedMaximum());
            json.addProperty("increment", range.getIncrement());
            json.addProperty("unit", cleanUnit(range.getSuffix()));
            json.addProperty("decimals", numberDecimals(range.getEndpointFormat().getMaximumFractionDigits()));
            return json;
        }
        if (value instanceof ColorValue) {
            ColorValue colorValue = (ColorValue)value;
            // getMutableColor() contains the current Fade/Rainbow animation.
            // Publishing that transient frame as the configured primary color
            // makes every state refresh feed the animation back into the value
            // until the player's chosen color has drifted away. Always expose
            // the stable HSB channels here.
            Color color = configuredColor(colorValue);
            String hex = String.format(Locale.ROOT, "#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
            json.addProperty("type", "color");
            json.addProperty("color", hex);
            json.addProperty("alpha", color.getAlpha());
            json.addProperty("rainbow", colorValue.isRainbowEnabled());
            if (!colorValue.isRainbowSupported()) {
                json.addProperty("noRainbow", true);
            }
            if (!colorValue.isFadeSupported()) {
                json.addProperty("noFade", true);
            }
            if (!colorValue.isAlphaSupported()) {
                json.addProperty("noAlpha", true);
            }
            json.addProperty("fade", colorValue.isFadeEnabled());
            Color fadeColor = colorValue.getFadeColor();
            String fadeHex = String.format(Locale.ROOT, "#%02X%02X%02X", fadeColor.getRed(), fadeColor.getGreen(), fadeColor.getBlue());
            json.addProperty("fadeColor", fadeHex);
            String mode = colorValue.isFadeEnabled() ? "fade" : colorValue.isRainbowEnabled() ? "rainbow" : "fixed";
            json.addProperty("value", hex + "," + color.getAlpha() + "," + mode + "," + fadeHex);
            return json;
        }
        if (value instanceof EntityTargetFilterValue) {
            EntityTargetFilterValue filters = (EntityTargetFilterValue)value;
            json.addProperty("type", "flags");
            json.addProperty("value", filters.toString());
            JsonArray options = new JsonArray();
            // Keep this wire schema independent from helper methods on the
            // injected class. Those helpers may have different obfuscated names
            // when Badlion resolves a parent-loader copy first.
            for (String label : new String[]{"Players //$ui", "Mobs //$ui",
                    "Peaceful //$ui", "Neutral //$ui", "Ignore naked //$ui",
                    "Ignore invisible //$ui", "Ignore behind walls //$ui"}) {
                addString(options, label);
            }
            json.add("options", options);
            return json;
        }
        if (value instanceof InvseeTargetCandidatesValue) {
            InvseeTargetCandidatesValue candidates = (InvseeTargetCandidatesValue)value;
            json.addProperty("type", "invsee-ranking");
            json.addProperty("value", String.valueOf(candidates.getValue()));
            json.addProperty("minimumSets", candidates.getMinimumSets());
            JsonArray players = new JsonArray();
            for (InvseeTargetCandidatesValue.RankedPlayer candidate : candidates.getPlayers()) {
                JsonObject player = new JsonObject();
                player.addProperty("name", candidate.getName());
                player.addProperty("sets", candidate.getSets());
                player.addProperty("helmet", candidate.getHelmet());
                player.addProperty("chestplate", candidate.getChestplate());
                player.addProperty("leggings", candidate.getLeggings());
                player.addProperty("boots", candidate.getBoots());
                players.add(player);
            }
            json.add("players", players);
            return json;
        }
        if (value instanceof vegas.sony.value.FriendsValue) {
            vegas.sony.value.FriendsValue friends =
                    (vegas.sony.value.FriendsValue)value;
            json.addProperty("type", "friends-list");
            json.addProperty("value", vegas.sony.value.FriendsValue.serialize(
                    friends.getFriends()));
            JsonArray players = new JsonArray();
            for (String name : friends.getFriends()) {
                JsonObject player = new JsonObject();
                player.addProperty("name", name);
                players.add(player);
            }
            json.add("players", players);
            this.appendSuggestions(json, this.friendSuggestionProvider);
            return json;
        }
        if (value instanceof PiercingManualPlayersValue) {
            PiercingManualPlayersValue players = (PiercingManualPlayersValue)value;
            json.addProperty("type", "player-flags");
            StringBuilder flags = new StringBuilder();
            JsonArray options = new JsonArray();
            for (String candidate : players.getCandidates()) {
                addString(options, candidate);
                flags.append(players.isSelected(candidate) ? '1' : '0');
            }
            json.addProperty("value", flags.toString());
            json.add("options", options);
            return json;
        }
        if (value instanceof StringValue) {
            json.addProperty("type", "text");
            json.addProperty("value", String.valueOf(value.getValue()));
            this.appendSuggestions(json, ((StringValue)value).getSuggestionProvider());
            return json;
        }
        if (value instanceof OptionalLimitValue && ((OptionalLimitValue)value).hasPlayerNameEntries()) {
            OptionalLimitValue names = (OptionalLimitValue)value;
            json.addProperty("type", "player-list");
            StringBuilder entries = new StringBuilder();
            for (String name : names.getEnabledValues()) {
                if (entries.length() > 0) entries.append('\n');
                entries.append(name);
            }
            json.addProperty("value", entries.toString());
            this.appendSuggestions(json, names.getSuggestionProvider());
            return json;
        }
        if (value instanceof LimitValue) {
            LimitValue limit = (LimitValue)value;
            json.addProperty("type", "list");
            StringBuilder entries = new StringBuilder();
            for (Object rawEntry : limit.getValue()) {
                ItemLimitData entry = (ItemLimitData)rawEntry;
                if (entries.length() > 0) entries.append('\n');
                entries.append(String.valueOf(entry));
            }
            json.addProperty("value", entries.toString());
            java.util.Set<String> choices = new java.util.LinkedHashSet<String>(java.util.Arrays.asList(
                    "swords", "axes", "pickaxes", "shovels", "blocks", "fists", "foods", "potions", "beds", "bow"));
            for (ItemLimitData entry : limit.getValue()) choices.add(String.valueOf(entry));
            JsonArray options = new JsonArray();
            for (String choice : choices) addString(options, choice);
            json.add("options", options);
            return json;
        }
        json.addProperty("type", "json");
        json.addProperty("value", GSON.toJson(value.toJson(true)));
        return json;
    }

    private static String modeWireKey(ModeValue mode, ModeSelection selection) {
        if (mode == null || selection == null) return "";
        ModeSelection[] options = mode.getModes();
        for (int index = 0; index < options.length; ++index) {
            if (options[index] == selection || options[index].equals(selection)) {
                return wireIndex('o', index);
            }
        }
        return "";
    }

    private static String safeModeLabel(ModeSelection selection, String fallback) {
        if (selection == null) return fallback;
        try {
            String label = selection.getName();
            if (label != null && !label.trim().isEmpty()) return label;
        } catch (Throwable ignored) {
        }
        return fallback;
    }

    private static String wireIndex(char kind, int index) {
        return String.valueOf(kind) + Integer.toString(index, 36);
    }

    private static void addString(JsonArray array, String value) {
        array.add((JsonElement)new JsonPrimitive(value == null ? "" : value));
    }

    private void applyState(JsonObject state) {
        JsonArray modules = state.getAsJsonArray("modules");
        if (modules == null) {
            return;
        }
        JsonObject partyTarget = state.has("partyTarget") && state.get("partyTarget").isJsonObject()
                ? state.getAsJsonObject("partyTarget") : null;
        if (partyTarget != null && (!PartyTargetSync.validSelection(partyTarget) || this.isRemotePartyLeader()
                || !this.getRemotePartyCode().equals(partyTarget.get("code").getAsString()))) return;
        for (JsonElement moduleElement : modules) {
            if (!moduleElement.isJsonObject()) continue;
            JsonObject remoteModule = moduleElement.getAsJsonObject();
            if (!remoteModule.has("name")) continue;
            Mod module = findPanelModule(remoteModule.get("name").getAsString());
            if (!isRemotelyControllable(module)) continue;
            JsonArray values = remoteModule.getAsJsonArray("values");
            if (values != null) {
                for (JsonElement valueElement : values) {
                    if (valueElement.isJsonObject()) this.applyValue(module, valueElement.getAsJsonObject());
                }
            }
            if (partyTarget != null && module instanceof Target) {
                ((Target)module).acceptPartyTargetSelection(
                        partyTarget.get("code").getAsString(),
                        partyTarget.get("name").getAsString(), System.currentTimeMillis());
            }
            if (remoteModule.has("enabled")) {
                boolean enabled = remoteModule.get("enabled").getAsBoolean();
                if (module.isEnabled() != enabled) module.setEnabled(enabled);
            }
        }
        LocalControlServer.notifyExternalStateChanged();
    }

    private boolean applyValue(Mod module, JsonObject remote) {
        if (!remote.has("id") || !remote.has("value")) return false;
        return this.applyValue(module, remote.get("id").getAsString(), remote.get("value").getAsString());
    }

    private boolean applyValue(Mod module, String id, String serialized) {
        Value<?, ?> value = findValue(module, id);
        if (value != null) {
            try {
                if (value instanceof vegas.sony.value.BindValue) {
                    int key = Integer.parseInt(serialized);
                    if (!validBindKey(key)) return false;
                    BindSet bind = ((vegas.sony.value.BindValue)value).getValue();
                    if (bind.getBoundInputs().size() == (key == 0 ? 0 : 1)
                            && (key == 0 || bind.getBoundInputs().get(0) == key)) return true;
                    bind.getBoundInputs().clear();
                    if (key != 0) bind.getBoundInputs().add(key);
                } else if (value instanceof BooleanValue) {
                    if (!"true".equals(serialized) && !"false".equals(serialized)) return false;
                    boolean next = Boolean.parseBoolean(serialized);
                    BooleanValue booleanValue = (BooleanValue)value;
                    if (booleanValue.getEffectiveValue().booleanValue() == next) return true;
                    if (!booleanValue.isChangeValid(next)) return false;
                    booleanValue.setValue(next);
                } else if (value instanceof NumberValue) {
                    NumberValue number = (NumberValue)value;
                    double next = Double.parseDouble(serialized);
                    if (Double.compare(number.getValue(), next) == 0) return true;
                    if (!Double.isFinite(next) || next < number.getMinimum() || next > number.getMaximum()
                            || !number.isChangeValid(next)) return false;
                    number.setValue(next);
                } else if (value instanceof ModeValue) {
                    ModeValue mode = (ModeValue)value;
                    ModeSelection next = null;
                    int optionIndex = 0;
                    for (ModeSelection option : mode.getModes()) {
                        if (wireIndex('o', optionIndex++).equalsIgnoreCase(serialized)) {
                            next = option;
                            break;
                        }
                    }
                    if (next != null && mode.getValue() == next) return true;
                    if (next == null || !mode.isChangeValid(next)) return false;
                    mode.setValue(next);
                } else if (value instanceof RandomValue) {
                    String[] endpoints = serialized.split(",", -1);
                    if (endpoints.length != 2) return false;
                    double lower = Double.parseDouble(endpoints[0]);
                    double upper = Double.parseDouble(endpoints[1]);
                    RandomValue range = (RandomValue)value;
                    double[] next = new double[]{lower, upper};
                    if (Double.compare(range.getMinimumValue(), lower) == 0
                            && Double.compare(range.getMaximumValue(), upper) == 0) return true;
                    if (!Double.isFinite(lower) || !Double.isFinite(upper) || lower > upper
                            || lower < range.getAllowedMinimum() || upper > range.getAllowedMaximum()
                            || !range.isChangeValid(next)) return false;
                    range.setRange(next);
                } else if (value instanceof ColorValue) {
                    String[] parts = serialized.split(",", -1);
                    if ((parts.length != 3 && parts.length != 4) || !parts[0].matches("#[0-9A-Fa-f]{6}")) return false;
                    int alpha = Integer.parseInt(parts[1]);
                    if (alpha < 0 || alpha > 255) return false;
                    boolean legacy = parts.length == 3;
                    boolean rainbow = legacy ? Boolean.parseBoolean(parts[2]) : "rainbow".equals(parts[2]);
                    boolean fade = !legacy && "fade".equals(parts[2]);
                    if ((!legacy && !"fixed".equals(parts[2]) && !rainbow && !fade)
                            || (!legacy && !parts[3].matches("#[0-9A-Fa-f]{6}"))) return false;
                    int rgb = Integer.parseInt(parts[0].substring(1), 16);
                    int fadeRgb = !legacy ? Integer.parseInt(parts[3].substring(1), 16) : rgb;
                    ColorValue color = (ColorValue)value;
                    Color current = configuredColor(color);
                    if (current.getRed() == (rgb >> 16 & 255)
                            && current.getGreen() == (rgb >> 8 & 255)
                            && current.getBlue() == (rgb & 255)
                            && current.getAlpha() == alpha
                            && color.isRainbowEnabled() == rainbow
                            && color.isFadeEnabled() == fade
                            && (color.getFadeColor().getRGB() & 0x00FFFFFF) == fadeRgb) return true;
                    color.setColorAndEffect(
                            new Color((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, alpha),
                            rainbow, fade, new Color((fadeRgb >> 16) & 255, (fadeRgb >> 8) & 255, fadeRgb & 255));
                } else if (value instanceof EntityTargetFilterValue) {
                    EntityTargetFilterValue filters = (EntityTargetFilterValue)value;
                    if (!serialized.matches("[01]{7}")) return false;
                    if (serialized.equals(filters.toString())) return true;
                    filters.parse(serialized);
                } else if (value instanceof InvseeTargetCandidatesValue) {
                    InvseeTargetCandidatesValue ranking =
                            (InvseeTargetCandidatesValue)value;
                    boolean legacyFlags = serialized.matches("[01]*")
                            && serialized.length() == ranking.getCandidates().size();
                    if (!(legacyFlags ? ranking.selectFromFlags(serialized)
                            : ranking.selectPlayer(serialized))) return false;
                } else if (value instanceof vegas.sony.value.FriendsValue) {
                    if (!((vegas.sony.value.FriendsValue)value).replaceSerialized(serialized)) return false;
                } else if (value instanceof PiercingManualPlayersValue) {
                    if (!((PiercingManualPlayersValue)value).selectFromFlags(serialized)) return false;
                } else if (value instanceof StringValue) {
                    StringValue string = (StringValue)value;
                    if (serialized.equals(String.valueOf(string.getValue()))) return true;
                    if (serialized.length() > 2048 || !string.isChangeValid(serialized)) return false;
                    string.setValue(serialized);
                } else if (value instanceof OptionalLimitValue
                        && ((OptionalLimitValue)value).hasPlayerNameEntries()) {
                    String[] rawNames = serialized.split("(?:\\r?\\n|,)");
                    List<OptionalLimitEntry> entries = new ArrayList<OptionalLimitEntry>();
                    java.util.HashSet<String> seen = new java.util.HashSet<String>();
                    for (String rawName : rawNames) {
                        String name = rawName.replaceAll("§.", "").trim();
                        if (name.isEmpty()) continue;
                        if (!name.matches("[A-Za-z0-9_]{1,16}")
                                || entries.size() >= 64
                                || !seen.add(name.toLowerCase(Locale.ROOT))) return false;
                        entries.add(new OptionalLimitEntry(name));
                    }
                    OptionalLimitValue names = (OptionalLimitValue)value;
                    if (!names.isChangeValid(entries)) return false;
                    names.replaceEntries(entries);
                } else if (value instanceof LimitValue) {
                    String[] names = serialized.split("(?:\\r?\\n|,)");
                    List<ItemLimitData> entries = new ArrayList<ItemLimitData>();
                    for (String rawName : names) {
                        String name = rawName.trim();
                        if (name.isEmpty()) continue;
                        if (name.length() > 80 || entries.size() >= 64) return false;
                        ItemLimitData selected = null;
                        for (ItemLimitData existing : ((LimitValue)value).getValue()) {
                            if (String.valueOf(existing).equalsIgnoreCase(name)) { selected = existing; break; }
                        }
                        entries.add(selected == null ? new ItemLimitData(name) : selected);
                    }
                    LimitValue limit = (LimitValue)value;
                    if (!limit.isChangeValid(entries)) return false;
                    limit.replaceEntries(entries);
                } else {
                    JsonObject object = GSON.fromJson(serialized, JsonObject.class);
                    if (object == null) return false;
                    object.addProperty("id", value.getId());
                    if (!value.loadJson(object)) return false;
                }
            } catch (RuntimeException ignored) {
                return false;
            }
            return true;
        }
        return false;
    }

    private static boolean isRemotelyControllable(Mod module) {
        // HUD visibility is independent from whether a module is available in
        // the configuration panel. Hidden modules must still be listed in the
        // native and web configuration UIs so they can be enabled again.
        return isPanelModule(module);
    }

    private static boolean isPanelModule(Mod module) {
        return module != null && !(module instanceof ClickGui)
                && module.getCategory() != Category.NONE;
    }

    static Color configuredColor(ColorValue value) {
        int rgb = value.toRgb();
        int alpha = ((Double)value.getAlphaValue().getValue()).intValue();
        return new Color((rgb >> 16) & 255, (rgb >> 8) & 255,
                rgb & 255, alpha);
    }

    private static Mod findPanelModule(String name) {
        int index = 0;
        for (Mod module : SonyVegas.INSTANCE.getModManager().getTopLevelModules()) {
            if (!isPanelModule(module)) continue;
            if (wireIndex('m', index++).equalsIgnoreCase(name)
                    || module.matchesName(name)) return module;
        }
        return null;
    }

    private static String panelModuleId(Mod expected) {
        int index = 0;
        for (Mod module : SonyVegas.INSTANCE.getModManager().getTopLevelModules()) {
            if (!isPanelModule(module)) continue;
            String id = wireIndex('m', index++);
            if (module == expected) return id;
        }
        return "";
    }

    private static Value<?, ?> findValue(Mod module, String id) {
        int index = 0;
        for (Value<?, ?> value : module.getAllValues()) {
            String wireId = wireIndex('v', index++);
            if (value.isSerializable() && (wireId.equalsIgnoreCase(id)
                    || value.getId().equalsIgnoreCase(id)
                    || Str.visibleEqualsIgnoreCase(value.getName(), id))) {
                return value;
            }
        }
        return null;
    }

    private void appendSuggestions(JsonObject json, ListValueSuggestionProvider provider) {
        if (provider == null) return;
        JsonArray suggestions = new JsonArray();
        java.util.HashSet<String> seen = new java.util.HashSet<String>();
        try {
            for (String raw : provider.getValues()) {
                if (raw == null) continue;
                String candidate = raw.replaceAll("§.", "").trim();
                if (!candidate.matches("[A-Za-z0-9_]{1,16}")) continue;
                if (seen.add(candidate.toLowerCase(Locale.ROOT))) addString(suggestions, candidate);
            }
        } catch (Throwable ignored) {
            // Suggestions are optional; the value remains editable if the world changes mid-scan.
        }
        json.add("suggestions", suggestions);
    }

    private void notifyConfigChanged(String moduleName) {
        final long revision = this.configNotificationRevision.incrementAndGet();
        this.notificationScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                if (configNotificationRevision.get() != revision) return;
                notifyOnGameThread(moduleName, "Atualizado");
            }
        }, 650L, TimeUnit.MILLISECONDS);
    }

    private void notifyOnGameThread(final String title, final String message) {
        EventTickBase.POST_TICK_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                if (SonyVegas.INSTANCE.getNotificationManager() != null) {
                    SonyVegas.INSTANCE.getNotificationManager()
                            .showInfo(title, message, 3000L);
                }
            }
        });
    }

    private static String partyCode(JsonObject party) {
        return party != null && party.has("code")
                ? party.get("code").getAsString() : "";
    }

    private static String cleanUnit(String value) {
        if (value == null) return "";
        // No § stripping: units keep their wire form (§r-separated when the
        // literal is //$ui-marked). Only build-time markers are removed.
        String unit = value.trim();
        int marker = unit.indexOf("//$");
        if (marker >= 0) unit = unit.substring(0, marker).trim();
        if ("deg".equalsIgnoreCase(unit) || "degree".equalsIgnoreCase(unit)
                || "degrees".equalsIgnoreCase(unit)) return "°";
        return unit;
    }

    /**
     * Precision the setting is declared with. Widening this to cover a
     * fractional increment is what let settings written as whole numbers
     * ("#", e.g. 20 ticks) report a decimal place and drift to 19.8.
     */
    private static int numberDecimals(int formatDecimals) {
        return Math.max(0, Math.min(8, formatDecimals));
    }

}

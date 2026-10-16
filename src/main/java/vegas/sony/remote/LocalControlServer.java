package vegas.sony.remote;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import vegas.sony.SonyVegas;
import vegas.sony.event.EventBus;
import vegas.sony.event.EventHandler;
import vegas.sony.event.EventListener;
import vegas.sony.event.impl.EventModStateChange;
import vegas.sony.module.none.ClientSettings;
import vegas.sony.runtime.NativeBridge;
import vegas.sony.runtime.PayloadResources;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class LocalControlServer implements EventListener {
    public static final LocalControlServer INSTANCE = new LocalControlServer();
    private static final int DEFAULT_PORT = 4210;
    private static final int MAX_BODY_BYTES = 64 * 1024;
    private static final int MAX_FORM_FIELDS = 128;
    private static final int MAX_HEAD_BYTES = 32 * 1024;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final Map<String, String> moduleAliases = new LinkedHashMap<String, String>();
    private final Map<String, String> aliasesToModule = new LinkedHashMap<String, String>();
    private final Map<String, String> valueAliases = new LinkedHashMap<String, String>();
    private final Map<String, String> aliasesToValue = new LinkedHashMap<String, String>();
    private final Map<String, String> routeAliases = new LinkedHashMap<String, String>();
    private final Map<String, String> aliasesToRoute = new LinkedHashMap<String, String>();
    private final SecureRandom aliasRandom = new SecureRandom();
    /* Session secrets never live as Strings: the session root and the CSRF
     * token are stored XOR-masked and only unmasked into short-lived byte
     * buffers while a request is being served. */
    private final byte[] secretMask = new byte[16];
    private byte[] sessionRootMasked;
    private int sessionRootLength;
    private final byte[] csrfMasked;
    private final AtomicLong stateVersion = new AtomicLong();
    private volatile ServerSocket server;
    private volatile int port;
    private volatile byte[] lanAddress;

    private LocalControlServer() {
        SecureRandom secrets = new SecureRandom();
        secrets.nextBytes(this.secretMask);
        byte[] csrfPlain = randomTokenBytes(32);
        this.csrfMasked = maskSecret(csrfPlain);
        Arrays.fill(csrfPlain, (byte)0);
    }

    private byte[] maskSecret(byte[] plain) {
        byte[] masked = new byte[plain.length];
        for (int index = 0; index < plain.length; ++index) {
            masked[index] = (byte)(plain[index] ^ this.secretMask[index & 15]);
        }
        return masked;
    }

    private byte[] unmaskSecret(byte[] masked, int length) {
        byte[] plain = new byte[length];
        for (int index = 0; index < length; ++index) {
            plain[index] = (byte)(masked[index] ^ this.secretMask[index & 15]);
        }
        return plain;
    }

    private int csrfLength() {
        return this.csrfMasked.length;
    }

    /** Installs the per-session root path. Test seam for the session probe. */
    void installSessionRoot(String root) {
        byte[] plain = root.getBytes(StandardCharsets.US_ASCII);
        this.sessionRootLength = plain.length;
        this.sessionRootMasked = maskSecret(plain);
        Arrays.fill(plain, (byte)0);
    }

    /** Called by game-side managers so the native menu can refresh its snapshot. */
    public static void notifyExternalStateChanged() {
        INSTANCE.stateVersion.incrementAndGet();
    }

    public void start() {
        if (!this.started.compareAndSet(false, true)) return;
        try {
            byte[] rootPlain = new byte[3 + 64 + 1];
            rootPlain[0] = '/';
            rootPlain[1] = 'c';
            rootPlain[2] = '/';
            System.arraycopy(randomTokenBytes(48), 0, rootPlain, 3, 64);
            rootPlain[rootPlain.length - 1] = '/';
            this.sessionRootLength = rootPlain.length;
            this.sessionRootMasked = maskSecret(rootPlain);
            Arrays.fill(rootPlain, (byte)0);
            this.lanAddress = findLanAddress();
            synchronized (this.moduleAliases) {
                this.moduleAliases.clear();
                this.aliasesToModule.clear();
                this.valueAliases.clear();
                this.aliasesToValue.clear();
                this.routeAliases.clear();
                this.aliasesToRoute.clear();
            }
            this.initializeRouteAliases();
            int requestedPort = readPort();
            InetAddress loopbackIpv4 = InetAddress.getByAddress(new byte[]{127, 0, 0, 1});
            ServerSocket localServer;
            try {
                localServer = new ServerSocket(requestedPort, 16,
                        loopbackIpv4);
            } catch (IOException portUnavailable) {
                localServer = new ServerSocket(0, 16,
                        loopbackIpv4);
            }
            this.port = localServer.getLocalPort();
            final ExecutorService pool = Executors.newFixedThreadPool(2, new ThreadFactory() {
                private int counter;

                @Override
                public Thread newThread(Runnable runnable) {
                    Thread thread = new Thread(runnable, "Local-Control-" + ++this.counter);
                    thread.setDaemon(true);
                    return thread;
                }
            });
            final ServerSocket bound = localServer;
            Thread acceptor = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (!bound.isClosed()) {
                        try {
                            Socket client = bound.accept();
                            client.setSoTimeout(20000);
                            client.setTcpNoDelay(true);
                            pool.execute(new PanelConnection(client));
                        } catch (IOException closed) {
                            return;
                        }
                    }
                }
            }, "Local-Control-Acceptor");
            acceptor.setDaemon(true);
            acceptor.start();
            this.server = localServer;
            EventBus.getInstance().registerListener(this);
            this.publishNativeMenuBootstrap();
            this.applySavedLoaderVisibility();
            SonyVegas.debugLog("Local control ready");
        } catch (Throwable error) {
            this.started.set(false);
            SonyVegas.debugLog("Local control failed to start");
            SonyVegas.logThrowable(error);
        }
    }

    /**
     * Minimal HTTP/1.1 surface for the loopback panel. Parsing is byte-wise:
     * the request target never becomes a URL object, and responses are
     * buffered and written once. Every connection is closed after one
     * request, matching both the native menu transport and browser usage.
     */
    private static final class PanelHeaders {
        private final Map<String, byte[]> values = new LinkedHashMap<String, byte[]>();

        void set(String name, String value) {
            setBytes(name, value.getBytes(StandardCharsets.UTF_8));
        }

        void setBytes(String name, byte[] value) {
            this.values.put(name.toLowerCase(Locale.ROOT), value);
        }

        String getFirst(String name) {
            byte[] value = getFirstBytes(name);
            return value == null ? null : new String(value, StandardCharsets.UTF_8);
        }

        byte[] getFirstBytes(String name) {
            return this.values.get(name.toLowerCase(Locale.ROOT));
        }
    }

    private static final class PanelUri {
        private final byte[] path;
        private final byte[] rawQuery;

        PanelUri(byte[] target) {
            int queryAt = -1;
            for (int index = 0; index < target.length; ++index) {
                if (target[index] == '?') {
                    queryAt = index;
                    break;
                }
            }
            if (queryAt < 0) {
                this.path = target;
                this.rawQuery = null;
            } else {
                this.path = Arrays.copyOfRange(target, 0, queryAt);
                this.rawQuery = Arrays.copyOfRange(target, queryAt + 1, target.length);
            }
        }

        byte[] pathBytes() {
            return this.path;
        }

        byte[] rawQueryBytes() {
            return this.rawQuery;
        }

        String getQuery() {
            return this.rawQuery == null ? null
                    : new String(this.rawQuery, StandardCharsets.UTF_8);
        }
    }

    private static final class PanelExchange {
        private final Socket socket;
        private final String method;
        private final PanelUri uri;
        private final PanelHeaders requestHeaders = new PanelHeaders();
        private final PanelHeaders responseHeaders = new PanelHeaders();
        private byte[] body = new byte[0];
        private final ByteArrayOutputStream bodyBuffer = new ByteArrayOutputStream();
        private int status = 200;
        private boolean responseStarted = false;
        private boolean noBody = false;

        PanelExchange(Socket socket, String method, byte[] target,
                Map<String, byte[]> headers, byte[] body) {
            this.socket = socket;
            this.method = method;
            this.uri = new PanelUri(target);
            this.requestHeaders.values.putAll(headers);
            this.body = body;
        }

        String getRequestMethod() {
            return this.method;
        }

        PanelUri getRequestURI() {
            return this.uri;
        }

        PanelHeaders getRequestHeaders() {
            return this.requestHeaders;
        }

        PanelHeaders getResponseHeaders() {
            return this.responseHeaders;
        }

        byte[] bodyBytes() {
            return this.body;
        }

        boolean isNativeClient() {
            // The native menu transport speaks plain HTTP without a Host
            // header; browsers always send one.
            return this.requestHeaders.getFirstBytes("host") == null;
        }

        void sendResponseHeaders(int statusCode, long length) throws IOException {
            this.status = statusCode;
            this.noBody = length < 0;
            this.responseStarted = true;
        }

        OutputStream getResponseBody() {
            return this.bodyBuffer;
        }

        void close() throws IOException {
            byte[] payload = this.bodyBuffer.toByteArray();
            StringBuilder head = new StringBuilder(256);
            head.append("HTTP/1.1 ").append(this.status).append("\r\n");
            boolean hasContentType = false;
            for (Map.Entry<String, byte[]> header : this.responseHeaders.values.entrySet()) {
                if (header.getKey().equals("location")) continue;
                if (header.getKey().equals("content-type")) hasContentType = true;
                head.append(header.getKey()).append(": ")
                        .append(new String(header.getValue(), StandardCharsets.ISO_8859_1))
                        .append("\r\n");
            }
            byte[] location = this.responseHeaders.getFirstBytes("location");
            if (location != null) {
                head.append("Location: ")
                        .append(new String(location, StandardCharsets.ISO_8859_1))
                        .append("\r\n");
            }
            head.append("Connection: close\r\n");
            if (!hasContentType && payload.length > 0 && !this.noBody) {
                head.append("Content-Type: text/plain; charset=utf-8\r\n");
            }
            if (this.noBody) {
                head.append("Content-Length: 0\r\n\r\n");
            } else {
                head.append("Content-Length: ").append(payload.length).append("\r\n\r\n");
            }
            OutputStream output = this.socket.getOutputStream();
            output.write(head.toString().getBytes(StandardCharsets.ISO_8859_1));
            if (!this.noBody && payload.length > 0) output.write(payload);
            output.flush();
            this.socket.close();
            Arrays.fill(payload, (byte)0);
            Arrays.fill(this.body, (byte)0);
        }
    }

    private final class PanelConnection implements Runnable {
        private final Socket socket;

        PanelConnection(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            PanelExchange exchange = null;
            try {
                exchange = readRequest(this.socket);
                if (exchange == null) return;
                handle(exchange);
            } catch (Throwable error) {
                SonyVegas.logThrowable(error);
                if (exchange != null) {
                    try {
                        exchange.sendResponseHeaders(500, 0);
                        exchange.getResponseBody().write(
                                "Falha ao processar a solicitacao.".getBytes(StandardCharsets.UTF_8));
                        exchange.close();
                    } catch (IOException ignored) {}
                }
            } finally {
                try {
                    this.socket.close();
                } catch (IOException ignored) {}
            }
        }
    }

    /** Byte-wise request-line and header parsing with hard size limits. */
    private static PanelExchange readRequest(Socket socket) throws IOException {
        InputStream input = socket.getInputStream();
        ByteArrayOutputStream head = new ByteArrayOutputStream();
        int last = 0;
        int current;
        while (head.size() < MAX_HEAD_BYTES) {
            current = input.read();
            if (current < 0) return null;
            head.write(current);
            if (last == '\r' && current == '\n') {
                if (head.size() >= 4) {
                    byte[] buffer = head.toByteArray();
                    int size = head.size();
                    if (buffer[size - 2] == '\r' && buffer[size - 3] == '\n') break;
                }
            }
            last = current;
        }
        byte[] headBytes = head.toByteArray();
        int lineEnd = 0;
        while (lineEnd + 1 < headBytes.length && !(headBytes[lineEnd] == '\r'
                && headBytes[lineEnd + 1] == '\n')) ++lineEnd;
        String requestLine = new String(headBytes, 0, lineEnd, StandardCharsets.ISO_8859_1);
        String[] parts = requestLine.split(" ");
        if (parts.length != 3 || parts[2].isEmpty()) return null;
        Map<String, byte[]> headers = new LinkedHashMap<String, byte[]>();
        int cursor = lineEnd + 2;
        while (cursor + 1 < headBytes.length) {
            if (headBytes[cursor] == '\r' && headBytes[cursor + 1] == '\n') break;
            int colon = cursor;
            while (colon < headBytes.length && headBytes[colon] != ':') ++colon;
            if (colon >= headBytes.length) return null;
            String name = new String(headBytes, cursor, colon - cursor,
                    StandardCharsets.ISO_8859_1).trim().toLowerCase(Locale.ROOT);
            int valueStart = colon + 1;
            while (valueStart < headBytes.length
                    && (headBytes[valueStart] == ' ' || headBytes[valueStart] == '\t')) {
                ++valueStart;
            }
            int valueEnd = valueStart;
            while (valueEnd + 1 < headBytes.length
                    && !(headBytes[valueEnd] == '\r' && headBytes[valueEnd + 1] == '\n')) {
                ++valueEnd;
            }
            byte[] value = Arrays.copyOfRange(headBytes, valueStart, valueEnd);
            headers.put(name, value);
            cursor = valueEnd + 2;
        }
        Arrays.fill(headBytes, (byte)0);
        int contentLength = 0;
        byte[] lengthHeader = headers.get("content-length");
        if (lengthHeader != null) {
            try {
                contentLength = Integer.parseInt(new String(lengthHeader,
                        StandardCharsets.ISO_8859_1).trim());
            } catch (NumberFormatException invalid) {
                return null;
            }
            if (contentLength < 0 || contentLength > MAX_BODY_BYTES) return null;
        }
        byte[] body = new byte[contentLength];
        int filled = 0;
        while (filled < contentLength) {
            int read = input.read(body, filled, contentLength - filled);
            if (read < 0) break;
            filled += read;
        }
        byte[] target = parts[1].getBytes(StandardCharsets.ISO_8859_1);
        return new PanelExchange(socket, parts[0], target, headers, body);
    }

    private void handle(PanelExchange exchange) throws Exception {
        try {
            applySecurityHeaders(exchange.getResponseHeaders());
            if (!validHost(exchange)) {
                sendText(exchange, 421, "Host invalido.", "text/plain; charset=utf-8");
                return;
            }
            String method = exchange.getRequestMethod();
            byte[] pathBytes = exchange.getRequestURI().pathBytes();
            if ("GET".equals(method) && this.isSessionRootWithoutSlash(pathBytes)) {
                redirectToSessionRoot(exchange);
                return;
            }
            String path = routePath(pathBytes);
            if (path == null) {
                sendText(exchange, 404, "Sessao inexistente ou expirada.",
                        "text/plain; charset=utf-8");
                return;
            }
            if ("GET".equals(method) && "/health".equals(path)) {
                sendText(exchange, 200, "ok", "text/plain; charset=utf-8");
                return;
            }
            if ("GET".equals(method) && "/state-version".equals(path)) {
                sendText(exchange, 200, String.valueOf(this.stateVersion.get()), "text/plain; charset=utf-8");
                return;
            }
            if ("GET".equals(method) && "/state".equals(path)) {
                JsonObject state = this.obfuscateState(
                        RemoteControlClient.INSTANCE.captureStateOnGameThread());
                state.addProperty("_v", this.stateVersion.get());
                sendText(exchange, 200, state.toString(), "application/json; charset=utf-8");
                return;
            }
            if ("GET".equals(method) && "/configs".equals(path)) {
                sendText(exchange, 200,
                        this.captureCloudConfigs(exchange.getRequestURI().getQuery()).toString(),
                        "application/json; charset=utf-8");
                return;
            }
            if ("GET".equals(method) && "/friends-state".equals(path)) {
                sendText(exchange, 200, RemoteControlClient.INSTANCE.captureFriendsStateOnGameThread().toString(), "application/json; charset=utf-8");
                return;
            }
            if ("GET".equals(method) && "/friend-suggestions".equals(path)) {
                sendText(exchange, 200, RemoteControlClient.INSTANCE.captureFriendSuggestionsOnGameThread().toString(), "application/json; charset=utf-8");
                return;
            }
            if ("GET".equals(method) && "/app.js".equals(path)) {
                sendJavascript(exchange);
                return;
            }
            if ("GET".equals(method) && "/app.css".equals(path)) {
                sendStylesheet(exchange);
                return;
            }
            if ("GET".equals(method) && "/".equals(path)) {
                renderHome(exchange, queryStatus(exchange));
                return;
            }
            if ("POST".equals(method) && ("/module".equals(path) || "/module-reset".equals(path)
                    || "/module-save".equals(path) || "/value".equals(path)
                    || "/loader-visibility".equals(path)
                    || "/binds-in-game".equals(path)
                    || "/notifications".equals(path)
                    || "/gui-bind".equals(path)
                    || "/profile".equals(path) || "/remote-config".equals(path)
                    || "/party".equals(path) || "/community".equals(path) || "/friends".equals(path)
                    || "/bind".equals(path) || "/hud".equals(path))) {
                handleMutation(exchange, path);
                return;
            }
            sendText(exchange, 404, "Nao encontrado.", "text/plain; charset=utf-8");
        } catch (ControlRequestException error) {
            sendText(exchange, error.status, error.getMessage(), "text/plain; charset=utf-8");
        } catch (java.util.concurrent.TimeoutException error) {
            sendText(exchange, 504, "O jogo demorou para responder. Confira o estado antes de tentar novamente.", "text/plain; charset=utf-8");
        } catch (java.net.SocketTimeoutException error) {
            sendText(exchange, 504, "A nuvem demorou para responder. Atualize a pagina para conferir o resultado.", "text/plain; charset=utf-8");
        } catch (Throwable error) {
            if (NativeBridge.isBadlion189Runtime()) {
                NativeBridge.sce("LOCAL_CONTROL " + error.getClass().getName()
                        + ": " + String.valueOf(error.getMessage()));
            }
            SonyVegas.logThrowable(error);
            try {
                sendText(exchange, 500, "Falha ao processar a solicitacao.", "text/plain; charset=utf-8");
            } catch (IOException ignored) {}
        } finally {
            try {
                exchange.close();
            } catch (IOException ignored) {}
        }
    }

    @EventHandler
    public void onModuleStateChanged(EventModStateChange event) {
        if (event != null && event.getModule() != null) {
            this.stateVersion.incrementAndGet();
        }
    }

    private void handleMutation(PanelExchange exchange, String path) throws Exception {
        if (!validOrigin(exchange)) {
            sendText(exchange, 403, "Origem invalida.", "text/plain; charset=utf-8");
            return;
        }
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.toLowerCase().startsWith("application/x-www-form-urlencoded")) {
            sendText(exchange, 415, "Formulario invalido.", "text/plain; charset=utf-8");
            return;
        }
        Map<String, String> form = parseForm(readBody(exchange));
        if (!csrfMatches(form.get("csrf"))) {
            sendText(exchange, 403, "Token invalido.", "text/plain; charset=utf-8");
            return;
        }
        String module = form.get("module");
        if (usesModuleAlias(path)) {
            module = resolveModuleAlias(module);
            if (module == null) {
                sendText(exchange, 404, "Modulo invalido.", "text/plain; charset=utf-8");
                return;
            }
            form.put("module", module);
        }
        if ("/value".equals(path)) {
            String valueId = resolveValueAlias(module, form.get("valueId"));
            if (valueId == null) {
                sendText(exchange, 404, "Valor invalido.", "text/plain; charset=utf-8");
                return;
            }
            form.put("valueId", valueId);
        }
        boolean changed = false;
        if ("/friends".equals(path)) {
            changed = RemoteControlClient.INSTANCE.updateFriendOnGameThread(form.get("action"), form.get("name"), form.get("enabled"));
        } else if ("/hud".equals(path)) {
            String hudAction = form.get("action");
            if ("enable".equals(hudAction) || "disable".equals(hudAction)) {
                changed = RemoteControlClient.INSTANCE.setHudEditingOnGameThread("enable".equals(hudAction));
            } else if ("move".equals(hudAction)) {
                try {
                    changed = RemoteControlClient.INSTANCE.moveHudElementOnGameThread(
                            form.get("id"), Double.parseDouble(form.getOrDefault("x", "")),
                            Double.parseDouble(form.getOrDefault("y", "")),
                            Boolean.parseBoolean(form.get("commit")));
                } catch (NumberFormatException ignored) { }
            }
        } else if ("/bind".equals(path)) {
            try {
                changed = module != null && module.length() <= 80
                        && RemoteControlClient.INSTANCE.setBindOnGameThread(module, Integer.parseInt(form.get("key")));
            } catch (NumberFormatException ignored) { }
        } else if ("/remote-config".equals(path)) {
            changed = handleRemoteConfigMutation(form);
        } else if ("/party".equals(path)) {
            changed = handlePartyMutation(form);
        } else if ("/community".equals(path)) {
            changed = handleCommunityMutation(form);
        } else if ("/loader-visibility".equals(path)) {
            String hidden = form.get("hidden");
            if ("true".equals(hidden) || "false".equals(hidden)) {
                changed = RemoteControlClient.INSTANCE.setLoaderInvisibleOnGameThread(
                        Boolean.parseBoolean(hidden));
            }
        } else if ("/binds-in-game".equals(path)) {
            String enabled = form.get("enabled");
            if ("true".equals(enabled) || "false".equals(enabled)) {
                changed = RemoteControlClient.INSTANCE.setBindsOnlyInGameOnGameThread(
                        Boolean.parseBoolean(enabled));
            }
        } else if ("/notifications".equals(path)) {
            String enabled = form.get("enabled");
            if ("true".equals(enabled) || "false".equals(enabled)) {
                changed = RemoteControlClient.INSTANCE.setNotificationsOnGameThread(Boolean.parseBoolean(enabled));
            }
        } else if ("/gui-bind".equals(path)) {
            try {
                changed = RemoteControlClient.INSTANCE.setGuiBindOnGameThread(
                        Integer.parseInt(form.get("key")));
            } catch (NumberFormatException ignored) { }
        } else if ("/profile".equals(path)) {
            String profileId = form.get("profileId");
            if (profileId != null && profileId.length() <= 64) {
                changed = RemoteControlClient.INSTANCE.setProfileOnGameThread(profileId);
            }
        } else if (module != null && module.length() <= 80 && "/module-reset".equals(path)) {
            changed = RemoteControlClient.INSTANCE.resetModuleOnGameThread(module);
        } else if (module != null && module.length() <= 80 && "/module-save".equals(path)) {
            changed = RemoteControlClient.INSTANCE.saveModuleOnGameThread(module);
        } else if (module != null && module.length() <= 80 && "/module".equals(path)) {
            String enabled = form.get("enabled");
            if ("true".equals(enabled) || "false".equals(enabled)) {
                changed = RemoteControlClient.INSTANCE.setModuleOnGameThread(module, Boolean.parseBoolean(enabled));
            }
        } else if (module != null && module.length() <= 80 && "/value".equals(path)) {
            String valueId = form.get("valueId");
            String value = form.get("value");
            SonyVegas.debugLog("Slider/Value change: " + module + " -> " + valueId + " = " + value);
            if (valueId != null && value != null && valueId.length() <= 160 && value.length() <= 16384) {
                changed = RemoteControlClient.INSTANCE.setValueOnGameThread(module, valueId, value);
            }
        }
        if (changed) this.stateVersion.incrementAndGet();
        if ("1".equals(exchange.getRequestHeaders().getFirst("X-Panel-Sync"))) {
            sendText(exchange, changed ? 200 : 422, changed ? "ok" : "Nao foi possivel aplicar. Confira os campos e tente novamente.", "text/plain; charset=utf-8");
        } else {
            redirect(exchange, changed ? "saved" : "invalid");
        }
    }

    /**
     * Slots and community listing in one payload for the in game menu. Both
     * legs are cloud calls, so the menu only asks for it when the user opens
     * those pages or hits refresh - never on the state poll.
     */
    private JsonObject captureCloudConfigs(String query) {
        JsonObject result = new JsonObject();
        String search = "";
        if (query != null) {
            int marker = query.indexOf("q=");
            if (marker >= 0) {
                search = query.substring(marker + 2);
                int end = search.indexOf('&');
                if (end >= 0) search = search.substring(0, end);
                try {
                    search = URLDecoder.decode(search, "UTF-8");
                } catch (Exception ignored) {
                    search = "";
                }
            }
        }
        try {
            JsonObject slots = RemoteControlClient.INSTANCE.listRemoteConfigs();
            if (slots != null && slots.has("configs")) {
                result.add("slots", slots.get("configs"));
            }
        } catch (Throwable unavailable) {
            result.addProperty("slotsError", true);
            if (unavailable instanceof ControlRequestException) {
                result.addProperty("slotsErrorCode", ((ControlRequestException) unavailable).safeCode());
                result.addProperty("slotsErrorStatus", ((ControlRequestException) unavailable).status);
            }
        }
        try {
            JsonObject community = RemoteControlClient.INSTANCE
                    .listCommunityConfigs(search);
            if (community != null && community.has("configs")) {
                result.add("community", community.get("configs"));
            }
            if (community != null && community.has("owner")) {
                result.add("owner", community.get("owner"));
            }
        } catch (Throwable unavailable) {
            result.addProperty("communityError", true);
            if (unavailable instanceof ControlRequestException) {
                result.addProperty("communityErrorCode", ((ControlRequestException) unavailable).safeCode());
                result.addProperty("communityErrorStatus", ((ControlRequestException) unavailable).status);
            }
        }
        result.addProperty("cloudError",
                result.has("slotsError") || result.has("communityError"));
        String user = RemoteControlClient.INSTANCE.getAuthenticatedUsername();
        result.addProperty("user", user == null ? "" : user);
        return result;
    }

    private boolean handleRemoteConfigMutation(Map<String, String> form) throws Exception {
        String action = form.get("action");
        int slot;
        try {
            slot = Integer.parseInt(form.get("slot"));
        } catch (RuntimeException ignored) {
            return false;
        }
        if (slot < 0 || slot > 3) return false;
        if ("save".equals(action)) {
            String name = form.get("name");
            if (name == null || name.trim().isEmpty() || name.length() > 32) return false;
            return RemoteControlClient.INSTANCE.saveRemoteConfig(slot, name.trim());
        }
        if ("load".equals(action)) return RemoteControlClient.INSTANCE.loadRemoteConfig(slot);
        if ("delete".equals(action)) return RemoteControlClient.INSTANCE.deleteRemoteConfig(slot);
        return false;
    }

    private boolean handlePartyMutation(Map<String, String> form) throws Exception {
        String action = form.get("action");
        if ("create".equals(action)) return RemoteControlClient.INSTANCE.createRemoteParty();
        if ("leave".equals(action)) return RemoteControlClient.INSTANCE.leaveRemoteParty();
        if ("kick".equals(action)) return RemoteControlClient.INSTANCE.kickRemotePartyMember(form.get("memberId"));
        if ("sync".equals(action)) {
            String mode = form.get("mode");
            if (!"activate".equals(mode) && !"config".equals(mode)) return false;
            java.util.Set<String> selected = new java.util.HashSet<String>();
            for (Map.Entry<String, String> entry : form.entrySet()) {
                if (entry.getKey().startsWith("share:") && "on".equals(entry.getValue())) {
                    String candidate = entry.getKey().substring(6);
                    String resolved = resolveModuleAlias(candidate);
                    selected.add(resolved == null ? candidate : resolved);
                }
            }
            return RemoteControlClient.INSTANCE.syncRemotePartyState(selected, "activate".equals(mode));
        }
        if ("join".equals(action)) {
            String code = form.get("code");
            return code != null && code.matches("(?i)[A-Z0-9]{6}")
                    && RemoteControlClient.INSTANCE.joinRemoteParty(code.toUpperCase());
        }
        return false;
    }

    private boolean handleCommunityMutation(Map<String, String> form) throws Exception {
        String action = form.get("action");
        if ("publish".equals(action)) {
            String name = form.get("name");
            String description = form.get("description");
            if (name == null || name.trim().isEmpty() || name.length() > 32
                    || description == null || description.length() > 160) {
                return false;
            }
            return RemoteControlClient.INSTANCE.publishCommunityConfig(
                    name.trim(), description.trim());
        }
        if ("load".equals(action) || "delete".equals(action)) {
            String configId = form.get("configId");
            if (configId == null || !configId.matches("cc_[0-9a-f]{32}")) return false;
            return "load".equals(action)
                    ? RemoteControlClient.INSTANCE.loadCommunityConfig(configId)
                    : RemoteControlClient.INSTANCE.deleteCommunityConfig(configId);
        }
        return false;
    }

    private void renderHome(PanelExchange exchange, String status) throws Exception {
        JsonObject state = RemoteControlClient.INSTANCE.captureStateOnGameThread();
        StringBuilder html = new StringBuilder(64 * 1024);
        html.append("<!doctype html><html lang=\"pt-BR\"><head><meta charset=\"utf-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">")
                .append("<title>SonyVegas Control</title><link rel=\"stylesheet\" href=\"")
                .append(attribute(this.routeToken("/app.css")))
                .append("\"><script>window.sve=function(i){return[")
                .append(jsString(this.routeToken("/bind"))).append(",")
                .append(jsString(this.routeToken("/state"))).append(",")
                .append(jsString(this.routeToken("/state-version"))).append(",")
                .append(jsString(this.routeToken("/module-reset"))).append(",")
                .append(jsString(this.routeToken("/module-save"))).append(",")
                .append(jsString(this.routeToken("/friends-state"))).append(",")
                .append(jsString(this.routeToken("/friend-suggestions"))).append(",")
                .append(jsString(this.routeToken("/friends"))).append(",")
                .append("][i];};window.svc=function(t){return String(t||'').replace(/\\u00a7./g,'')};</script><script src=\"").append(attribute(this.routeToken("/app.js"))).append("\" defer></script></head><body>")
                .append("<div class=\"shell\"><header><div class=\"brand\"><span class=\"mark\">SV</span><div><small>SONYVEGAS</small>")
                .append("<h1>Control Center</h1><p>Controle local em tempo real</p></div></div><span class=\"online\"><i></i> conectado</span></header><main>")
                .append("<div id=\"toast\" class=\"toast\" role=\"status\"></div>");
        if ("saved".equals(status)) html.append("<div class=\"notice ok\">Alteracao aplicada.</div>");
        if ("invalid".equals(status)) html.append("<div class=\"notice bad\">Valor recusado pelo modulo.</div>");
        JsonArray modules = state.getAsJsonArray("modules");
        Map<String, List<JsonObject>> categories = new LinkedHashMap<String, List<JsonObject>>();
        for (JsonElement element : modules) {
            JsonObject module = element.getAsJsonObject();
            String category = module.get("category").getAsString();
            List<JsonObject> categoryModules = categories.get(category);
            if (categoryModules == null) {
                categoryModules = new ArrayList<JsonObject>();
                categories.put(category, categoryModules);
            }
            categoryModules.add(module);
        }
        html.append("<nav class=\"tabs\" aria-label=\"Categorias\"><small class=\"nav-label\">MODULOS</small>");
        int categoryIndex = 0;
        for (Map.Entry<String, List<JsonObject>> entry : categories.entrySet()) {
            html.append("<button type=\"button\" class=\"tab").append(categoryIndex == 0 ? " active" : "")
                    .append("\" data-category=\"category-").append(categoryIndex).append("\"><span>")
                    .append(categoryIcon(entry.getKey())).append("</span>").append(escape(entry.getKey()))
                    .append("<b>").append(entry.getValue().size()).append("</b></button>");
            categoryIndex++;
        }
        html.append("<small class=\"nav-label nav-tools\">GERENCIAR</small>")
                .append("<button type=\"button\" class=\"tab special-tab\" data-category=\"phone\"><span>▣</span>Celular</button>")
                .append("<button type=\"button\" class=\"tab special-tab\" data-category=\"configs\"><span>◆</span>Configs</button>")
                .append("<button type=\"button\" class=\"tab special-tab\" data-category=\"friends\"><span>+</span>Friends</button>")
                .append("<button type=\"button\" class=\"tab special-tab\" data-category=\"party\"><span>◎</span>Party</button>")
                .append("<button type=\"button\" class=\"tab special-tab\" data-category=\"community\"><span>◇</span>Community</button>")
                .append("<button type=\"button\" class=\"tab special-tab\" data-category=\"settings\"><span>⚙</span>Settings</button>")
                .append("<aside class=\"sidebar-tip\"><b><i>⚡</i>Dica rápida</b><span>Clique com o botão direito em um módulo para abrir suas configurações.</span></aside>");
        html.append("</nav><div class=\"workspace\">");
        html.append("<div class=\"module-toolbar\"><div class=\"search-box\"><span>⌕</span><input id=\"module-search\" type=\"search\" autocomplete=\"off\" placeholder=\"Buscar módulo...\"><kbd>ESC</kbd></div><label class=\"sort-box\"><span>Ordenar:</span><select id=\"module-sort\" aria-label=\"Ordenar módulos\"><option value=\"az\">A-Z</option><option value=\"za\">Z-A</option></select></label><div class=\"view-switch\" aria-label=\"Modo de visualização\"><button type=\"button\" class=\"view-button active\" data-view=\"grid\" aria-label=\"Grade\" aria-pressed=\"true\">▦</button><button type=\"button\" class=\"view-button\" data-view=\"list\" aria-label=\"Lista\" aria-pressed=\"false\">☰</button></div></div>")
                .append("<div id=\"search-empty\" class=\"empty-search\">Nenhum modulo encontrado.</div>");
        categoryIndex = 0;
        for (Map.Entry<String, List<JsonObject>> entry : categories.entrySet()) {
            html.append("<section class=\"category-panel").append(categoryIndex == 0 ? " active" : "")
                    .append(" module-panel\" data-panel=\"category-").append(categoryIndex).append("\"><div class=\"section-title module-section-title\"><span class=\"category-hero-icon\">")
                    .append(moduleIcon("", entry.getKey())).append("</span><div><small>CATEGORIA</small><h2>")
                    .append(escape(entry.getKey())).append("</h2><p>").append(entry.getValue().size()).append(" módulos disponíveis</p></div></div><div class=\"grid\">");
            for (JsonObject module : entry.getValue()) appendModule(html, module);
            html.append("</div></section>");
            categoryIndex++;
        }
        appendFriendsPanel(html, state.getAsJsonObject("friends"));
        appendPhonePanel(html);
        JsonObject remoteConfigs = null;
        if (RemoteControlClient.isConfigured()) {
            try {
                remoteConfigs = RemoteControlClient.INSTANCE.listRemoteConfigs();
            } catch (Throwable error) {
                SonyVegas.debugLog("Could not list remote configs");
                remoteConfigs = new JsonObject();
                remoteConfigs.addProperty("error", error instanceof ControlRequestException
                        ? error.getMessage() : "Nao foi possivel consultar suas configs. Atualize a pagina para tentar novamente.");
            }
        }
        appendConfigPanel(html, remoteConfigs);
        appendSettingsPanel(html, state.getAsJsonObject("config"));
        String authenticatedUsername =
                RemoteControlClient.INSTANCE.getAuthenticatedUsername();
        appendPartyPanel(html, RemoteControlClient.INSTANCE.getRemotePartyState(),
                authenticatedUsername, state.getAsJsonArray("modules"));
        JsonObject communityConfigs = null;
        if (RemoteControlClient.isConfigured()) {
            try {
                communityConfigs = RemoteControlClient.INSTANCE.listCommunityConfigs("");
            } catch (Throwable error) {
                SonyVegas.debugLog("Could not list community configs");
                communityConfigs = new JsonObject();
                communityConfigs.addProperty("error", error instanceof ControlRequestException
                        ? error.getMessage() : "Nao foi possivel consultar a Community agora.");
            }
        }
        appendCommunityPanel(html, communityConfigs, authenticatedUsername);
        html.append("</div></main><div id=\"module-drawer\" aria-hidden=\"true\"><div class=\"drawer-header\"><div class=\"drawer-topbar\" style=\"justify-content:flex-end\"><button type=\"button\" class=\"expand-close\" aria-label=\"Fechar\">\u2715</button></div><div class=\"drawer-module-row\"><span id=\"drawer-icon\" class=\"mod-icon\"></span><div style=\"flex:1;min-width:0\"><div id=\"drawer-title\" style=\"font-size:18px;font-weight:800;color:#fff;line-height:1.2\"></div><div id=\"drawer-desc\" style=\"font-size:12px;color:#8A8F98;line-height:1.4\">Modulo de mira assistida</div></div><label class=\"ios-switch drawer-toggle\"><input type=\"checkbox\" id=\"drawer-toggle\"><span class=\"ios-slider\"></span></label></div></div><div class=\"drawer-tabs\"><button class=\"exp-tab active\" data-tab=\"geral\">Geral</button><button class=\"exp-tab\" data-tab=\"avancado\">Avancado</button><button class=\"exp-tab\" data-tab=\"visual\">Visual</button></div><div id=\"drawer-content\" class=\"drawer-content\"></div><div class=\"drawer-footer\"><button type=\"button\" class=\"btn-reset\" data-drawer-action=\"reset\">Resetar</button><button type=\"button\" class=\"btn-save\" data-drawer-action=\"save\">Salvar configuracoes</button></div></div><footer><span>Disponivel somente na sua rede local</span><span>Alteracoes salvas automaticamente</span></footer></div></body></html>");
        sendText(exchange, 200, this.obfuscateFormActions(html.toString()), "text/html; charset=utf-8");
    }

    private String bindDisplayName(int key, JsonArray bind) {
        if (bind != null && bind.size() > 1) return "Bind: combo";
        switch (key) {
            case 0: return "Bind";
            case 8: return "Bind: Backspace";
            case 9: return "Bind: Tab";
            case 13: return "Bind: Enter";
            case 27: return "Bind: Esc";
            case 32: return "Bind: Espaco";
            case 37: return "Bind: Esquerda";
            case 38: return "Bind: Cima";
            case 39: return "Bind: Direita";
            case 40: return "Bind: Baixo";
            case 45: return "Bind: Insert";
            case 46: return "Bind: Delete";
            case 112: return "Bind: F1";
            case 113: return "Bind: F2";
            case 114: return "Bind: F3";
            case 115: return "Bind: F4";
            case 116: return "Bind: F5";
            case 117: return "Bind: F6";
            case 118: return "Bind: F7";
            case 119: return "Bind: F8";
            case 120: return "Bind: F9";
            case 121: return "Bind: F10";
            case 122: return "Bind: F11";
            case 123: return "Bind: F12";
            case 160: return "Bind: Shift esquerdo";
            case 161: return "Bind: Shift direito";
            case 162: return "Bind: Ctrl esquerdo";
            case 163: return "Bind: Ctrl direito";
            case 164: return "Bind: Alt esquerdo";
            case 165: return "Bind: Alt direito";
            case -100: return "Bind: M1";
            case -99: return "Bind: M2";
            case -98: return "Bind: M3";
            case -97: return "Bind: M4";
            case -96: return "Bind: M5";
            default:
                if (key >= 48 && key <= 57 || key >= 65 && key <= 90) {
                    return "Bind: " + (char) key;
                }
                return "Bind: Tecla " + key;
        }
    }

    private String moduleIconSvg(String name, String category) {
        String n = name == null ? "" : name;
        String c = category == null ? "" : category;
        // Compact inline paths based on the open-source Lucide visual language (ISC).
        if (visibleContains(n, "autoclick") || visibleContains(n, "clicker"))
            return iconSvg("<rect x='5' y='2' width='14' height='20' rx='7'/><path d='M12 6v4'/>");
        if (visibleContains(n, "gapple") || visibleContains(n, "apple"))
            return iconSvg("<path d='M12 7c-2-2-6-1-7 2-1.3 4 2.2 11 5 11 1 0 1.4-.7 2-.7s1 .7 2 .7c2.8 0 6.3-7 5-11-1-3-5-4-7-2Z'/><path d='M12 7c0-2 1-4 4-4'/><path d='M12 5c-1.5-1.7-3-2-4.5-2'/>");
        if (visibleEquals(n, "aim") || visibleContains(n, "aimassist") || visibleEquals(n, "target"))
            return iconSvg("<circle cx='12' cy='12' r='7'/><circle cx='12' cy='12' r='2'/><path d='M12 2v3M12 19v3M2 12h3M19 12h3'/>");
        if (visibleContains(n, "aura"))
            return iconSvg("<circle cx='12' cy='12' r='9'/><circle cx='12' cy='12' r='5'/><path d='M12 12 18 6'/><path d='M12 3v2M3 12h2'/>");
        if (visibleContains(n, "kbdisplacement") || visibleContains(n, "hitflick"))
            return iconSvg("<path d='M20 11a8 8 0 1 0-2.3 5.7'/><path d='M20 4v7h-7'/><path d='m9 12 2 2 4-5'/>");
        if (visibleContains(n, "hitselect"))
            return iconSvg("<path d='M4 9V4h5M15 4h5v5M20 15v5h-5M9 20H4v-5'/><circle cx='12' cy='12' r='3'/><path d='m14.5 14.5 3 3'/>");
        if (visibleContains(n, "critical"))
            return iconSvg("<path d='M14.5 17.5 3 6V3h3l11.5 11.5M13 19l6-6M16 16v6h6'/><path d='m18 2 .6 1.7L20.5 4l-1.9.4L18 6l-.6-1.6-1.9-.4 1.9-.3L18 2Z'/>");
        if (visibleContains(n, "blockhit") || visibleContains(n, "noitemrelease"))
            return iconSvg("<path d='M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10Z'/><path d='m8 15 7-7M10 7l7 7'/>");
        if (visibleContains(n, "keepsprint"))
            return iconSvg("<circle cx='14' cy='4' r='2'/><path d='m10 8 3 2 2 4 4 2'/><path d='m13 10-3 5-5 3M10 15l3 5M7 8H3'/>");
        if (visibleContains(n, "sprintreset") || visibleContains(n, "snaptap"))
            return iconSvg("<path d='M3 12a9 9 0 0 1 15-6.7L21 8'/><path d='M21 3v5h-5'/><path d='M21 12a9 9 0 0 1-15 6.7L3 16'/><path d='M3 21v-5h5'/>");
        if (visibleEquals(n, "wtap") || visibleContains(n, "keystroke"))
            return iconSvg("<rect x='3' y='5' width='18' height='14' rx='3'/><path d='m7 10 1.5 5 2-4 2 4 1.5-5M17 10v5'/>");
        if (visibleContains(n, "lagrange") || visibleContains(n, "fakelag"))
            return iconSvg("<circle cx='12' cy='12' r='2'/><path d='M8.5 8.5a5 5 0 0 0 0 7M15.5 8.5a5 5 0 0 1 0 7M5 5a10 10 0 0 0 0 14M19 5a10 10 0 0 1 0 14'/>");
        if (visibleContains(n, "timer") || visibleEquals(n, "clock"))
            return iconSvg("<circle cx='12' cy='13' r='8'/><path d='M12 9v4l3 2M9 2h6M12 2v3'/>");
        if (visibleContains(n, "velocity") || visibleContains(n, "knockback"))
            return iconSvg("<path d='M3 8h11a3 3 0 1 0-3-3M3 12h15a3 3 0 1 1-3 3M3 16h7'/>");
        if (visibleContains(n, "backtrack"))
            return iconSvg("<path d='M3 12a9 9 0 1 0 3-6.7L3 8'/><path d='M3 3v5h5M12 7v5l3 2'/>");
        if (visibleEquals(n, "blink"))
            return iconSvg("<path d='M10 6H6a3 3 0 0 0-3 3v6a3 3 0 0 0 3 3h4M14 6h4a3 3 0 0 1 3 3v6a3 3 0 0 1-3 3h-4M8 12h8'/><path d='m5 3 14 18'/>");
        if (visibleContains(n, "hitbox"))
            return iconSvg("<path d='m12 2 9 5-9 5-9-5 9-5Z'/><path d='m3 7 9 5 9-5M3 7v10l9 5 9-5V7M12 12v10'/>");
        if (visibleContains(n, "invwalk") || visibleContains(n, "invmove"))
            return iconSvg("<rect x='4' y='5' width='16' height='16' rx='2'/><path d='M8 5a4 4 0 0 1 8 0M8 11h8M12 8v6'/>");
        if (visibleContains(n, "nojump"))
            return iconSvg("<path d='M12 20V5M6 11l6-6 6 6'/><path d='M5 20h14'/>");
        if (visibleContains(n, "nohitdelay"))
            return iconSvg("<path d='M14.5 17.5 3 6V3h3l11.5 11.5M13 19l6-6M16 16v6h6'/><path d='M20 3v4M18 5h4'/>");
        if (visibleContains(n, "basquete"))
            return iconSvg("<circle cx='12' cy='12' r='9'/><path d='M3.5 9h17M3.5 15h17M9 3.5c4 4 4 13 0 17M15 3.5c-4 4-4 13 0 17'/>");
        if (visibleEquals(n, "Panic //$ui"))
            return iconSvg("<circle cx='12' cy='12' r='9'/><path d='M12 7v6M12 17h.01'/>");
        if (visibleContains(n, "noslow") || visibleContains(n, "speed"))
            return iconSvg("<path d='M4 17a8 8 0 1 1 16 0'/><path d='m12 13 4-4M7 17h10'/>");
        if (visibleContains(n, "strafe"))
            return iconSvg("<path d='M7 7 3 12l4 5M17 7l4 5-4 5M3 12h18'/>");
        if (visibleContains(n, "clickgui") || visibleEquals(n, "gui"))
            return iconSvg("<rect x='3' y='3' width='18' height='18' rx='2'/><path d='M9 3v18M9 9h12'/>");
        if (visibleContains(n, "animation"))
            return iconSvg("<path d='m12 3-1.7 4.3L6 9l4.3 1.7L12 15l1.7-4.3L18 9l-4.3-1.7L12 3Z'/><path d='m5 15-.8 2.2L2 18l2.2.8L5 21l.8-2.2L8 18l-2.2-.8L5 15ZM19 13l-.7 1.8-1.8.7 1.8.7L19 18l.7-1.8 1.8-.7-1.8-.7L19 13Z'/>");
        if (visibleContains(n, "rotate"))
            return iconSvg("<path d='M21 12a9 9 0 1 1-2.6-6.4L21 8'/><path d='M21 3v5h-5'/>");
        if (visibleContains(n, "arraylist"))
            return iconSvg("<path d='M8 6h13M8 12h13M8 18h13'/><circle cx='4' cy='6' r='1'/><circle cx='4' cy='12' r='1'/><circle cx='4' cy='18' r='1'/>");
        if (visibleContains(n, "cape"))
            return iconSvg("<path d='M6 3h12l-2 18-4-3-4 3L6 3Z'/><path d='M9 7h6'/>");
        if (visibleContains(n, "hideclan"))
            return iconSvg("<path d='M3 3l18 18M10.6 10.7a2 2 0 0 0 2.7 2.7M9.9 4.2A10.8 10.8 0 0 1 12 4c7 0 10 8 10 8a17 17 0 0 1-2.1 3.2M6.6 6.6C3.5 8.7 2 12 2 12s3 8 10 8a10 10 0 0 0 4.1-.8'/>");
        if (visibleContains(n, "reach") || visibleContains(n, "range"))
            return iconSvg("<path d='M21 3 3 21M15 3h6v6M9 21H3v-6'/>");
        if (visibleContains(c, "combat"))
            return iconSvg("<path d='M14.5 17.5 3 6V3h3l11.5 11.5M13 19l6-6M16 16v6h6'/>");
        if (visibleContains(c, "network"))
            return iconSvg("<path d='M5 12.5a10 10 0 0 1 14 0M8.5 16a5 5 0 0 1 7 0M12 20h.01'/>");
        if (visibleContains(c, "movement"))
            return iconSvg("<path d='M5 12h14M12 5l7 7-7 7'/>");
        if (visibleContains(c, "render"))
            return iconSvg("<path d='M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z'/><circle cx='12' cy='12' r='3'/>");
        if (visibleContains(c, "gladiador"))
            return iconSvg("<path d='M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10Z'/><path d='m9 12 2 2 4-5'/>");
        return iconSvg("<rect x='3' y='3' width='7' height='7' rx='1'/><rect x='14' y='3' width='7' height='7' rx='1'/><rect x='3' y='14' width='7' height='7' rx='1'/><rect x='14' y='14' width='7' height='7' rx='1'/>");
    }

    private String iconSvg(String body) {
        return "<svg width='18' height='18' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='1.8' stroke-linecap='round' stroke-linejoin='round' aria-hidden='true'>" + body + "</svg>";
    }
    private String moduleIcon(String name, String category) {
        return moduleIconSvg(name, category);
    }

    /** Case-insensitive contains that skips § formatting pairs without ever
     *  materializing the clean string. Used for icon/description matching so
     *  segmented display names never become contiguous in memory. */
    private static boolean visibleContains(String haystack, String needle) {
        if (haystack == null || needle == null || needle.isEmpty()) return false;
        for (int start = 0; start < haystack.length(); ++start) {
            int ti = start;
            int ni = 0;
            while (true) {
                while (ti < haystack.length() && haystack.charAt(ti) == '§' && ti + 1 < haystack.length()) ti += 2;
                if (ni >= needle.length()) return true;
                if (ti >= haystack.length()
                        || Character.toLowerCase(haystack.charAt(ti))
                        != Character.toLowerCase(needle.charAt(ni))) break;
                ++ti;
                ++ni;
            }
        }
        return false;
    }

    private static boolean visibleEquals(String a, String b) {
        return vegas.sony.utils.Str.visibleEqualsIgnoreCase(a, b);
    }

    private void appendModule(StringBuilder html, JsonObject module) {
        String name = module.get("name").getAsString();
        String displayName = module.has("label") && !module.get("label").isJsonNull()
                ? module.get("label").getAsString() : name;
        String moduleAlias = this.moduleAlias(name);
        String description = "Configuracoes do modulo";
        if (visibleEquals(displayName, "aim") || visibleEquals(displayName, "aimassist")) description = "Módulo de mira assistida";
        String category = module.has("category") && !module.get("category").isJsonNull()
                ? module.get("category").getAsString() : "";
        boolean enabled = module.get("enabled").getAsBoolean();
        JsonArray values = module.getAsJsonArray("values");
        JsonArray bind = module.has("bind") ? module.getAsJsonArray("bind") : null;
        int bindKey = bind != null && bind.size() == 1 ? bind.get(0).getAsInt() : 0;
        String bindLabel = bindDisplayName(bindKey, bind);
        html.append("<article class=\"card").append(enabled ? " enabled" : "").append(" has-values")
                .append("\" data-module-name=\"").append(attribute(moduleAlias))
                .append("\" data-module-search=\"").append(attribute(displayName.toLowerCase()))
                .append("\" data-module-description=\"").append(attribute(description)).append("\"><form class=\"toggle ajax-form\" method=\"post\" action=\"module\">")
                .append(hidden("csrf", this.csrfFormValue())).append(hidden("module", moduleAlias))
                .append(hidden("enabled", Boolean.toString(!enabled))).append("<span class=\"module-card-icon\" aria-hidden=\"true\">")
                .append(moduleIcon(displayName, category)).append("</span><div class=\"module-card-copy\"><strong>")
                .append(escape(displayName)).append("</strong><small>").append(enabled ? "Ativo" : "Desativado")
                .append("</small></div><button type=\"button\" class=\"bind-btn\" data-module=\"").append(attribute(moduleAlias)).append("\" data-bind=\"").append(String.valueOf(bindKey)).append("\" data-csrf=\"").append(attribute(this.csrfFormValue())).append("\" aria-label=\"Definir bind\">").append(escape(bindLabel)).append("</button><button type=\"submit\" aria-label=\"Alternar modulo\"><i></i></button></form>");
        html.append("<div class=\"values\">");
        for (JsonElement valueElement : values) appendValue(html, displayName, name,
                moduleAlias, valueElement.getAsJsonObject());
        html.append("</div><span class=\"mod-icon drawer-source-icon\" hidden>")
                .append(moduleIcon(displayName, category)).append("</span>");
        html.append("</article>");
    }

    private void appendBind(StringBuilder html, String name, JsonArray bind) {
        int key = bind != null && bind.size() == 1 ? bind.get(0).getAsInt() : 0;
        html.append("<form class=\"value ajax-form\" method=\"post\" action=\"bind\">")
                .append(hidden("csrf", this.csrfFormValue())).append(hidden("module", this.moduleAlias(name)))
                .append("<label><span>Bind - ativar / desativar</span><select name=\"key\">");
        if (bind != null && bind.size() > 1) html.append("<option value=\"\" selected disabled>Combinacao atual: ").append(escape(bind.toString())).append("</option>");
        Map<Integer, String> keys = new LinkedHashMap<Integer, String>();
        keys.put(0, "Sem bind");
        for (int code = 65; code <= 90; code++) keys.put(code, Character.toString((char)code));
        for (int code = 48; code <= 57; code++) keys.put(code, Character.toString((char)code));
        for (int code = 112; code <= 123; code++) keys.put(code, "F" + (code - 111));
        keys.put(32, "Espaco"); keys.put(9, "Tab"); keys.put(160, "Shift esquerdo"); keys.put(161, "Shift direito"); keys.put(162, "Ctrl esquerdo"); keys.put(163, "Ctrl direito"); keys.put(164, "Alt esquerdo"); keys.put(165, "Alt direito");
        keys.put(-100, "Mouse esquerdo"); keys.put(-99, "Mouse direito"); keys.put(-98, "Middle-click");
        keys.put(-97, "Mouse 4"); keys.put(-96, "Mouse 5");
        if (!keys.containsKey(key)) keys.put(key, "Tecla atual: " + key);
        for (Map.Entry<Integer, String> option : keys.entrySet()) {
            html.append("<option value=\"").append(option.getKey()).append("\"")
                    .append(option.getKey() == key && (bind == null || bind.size() <= 1) ? " selected" : "").append(">")
                    .append(escape(option.getValue())).append("</option>");
        }
        html.append("</select></label></form>");
    }

    private void appendFriendsPanel(StringBuilder html, JsonObject friends) {
        html.append("<section class=\"category-panel utility-panel\" data-panel=\"friends\"><div class=\"section-title\"><div><small>SOCIAL</small><h2>Friends</h2></div></div><div class=\"utility-card\">");
        for (String setting : new String[]{"enabled", "middle"}) {
            boolean on = friends.get(setting).getAsBoolean();
            html.append("<form class=\"ajax-form friend-setting\" data-reload=\"1\" method=\"post\" action=\"friends\">")
                    .append(hidden("csrf", this.csrfFormValue())).append(hidden("action", setting)).append(hidden("enabled", Boolean.toString(!on)))
                    .append("<span>").append("middle".equals(setting) ? "Middle-click no jogador para adicionar/remover" : "Usar friends (excluir dos alvos)")
                    .append("</span><button type=\"submit\">").append(on ? "Desativar" : "Ativar").append("</button></form>");
        }
        html.append("<form class=\"ajax-form friend-setting friend-add-form\" data-reload=\"1\" method=\"post\" action=\"friends\">")
                .append(hidden("csrf", this.csrfFormValue())).append(hidden("action", "add"))
                .append("<div class=\"friend-name-field\"><input name=\"name\" aria-label=\"Nome do jogador\" placeholder=\"Nome do jogador\" pattern=\"[A-Za-z0-9_]{1,16}\" maxlength=\"16\" autocomplete=\"off\" role=\"combobox\" aria-autocomplete=\"list\" aria-expanded=\"false\" aria-controls=\"friend-suggestions\" required><div id=\"friend-suggestions\" class=\"friend-suggestions\" role=\"listbox\"></div></div><button type=\"submit\">Adicionar friend</button></form><div class=\"friend-list\" data-friend-list>");
        for (JsonElement name : friends.getAsJsonArray("names")) {
            String friendName = name.getAsString();
            String avatar = friendName.substring(0, Math.min(2, friendName.length())).toUpperCase(Locale.ROOT);
            html.append("<form class=\"ajax-form friend-setting friend-entry\" data-reload=\"1\" method=\"post\" action=\"friends\">")
                    .append(hidden("csrf", this.csrfFormValue())).append(hidden("action", "remove")).append(hidden("name", friendName))
                    .append("<span class=\"friend-avatar\" aria-hidden=\"true\">").append(escape(avatar))
                    .append("</span><div class=\"friend-entry-copy\"><strong>").append(escape(friendName))
                    .append("</strong><small>ADICIONADO</small></div><span class=\"friend-entry-state\">NA LISTA</span><button type=\"submit\" aria-label=\"Remover friend ")
                    .append(attribute(friendName)).append("\">Remover</button></form>");
        }
        if (friends.getAsJsonArray("names").size() == 0) html.append("<p class=\"friend-empty\">Nenhum friend adicionado.</p>");
        html.append("</div></div></section>");
    }

    private String drawerValueLabel(String moduleName, String name) {
        String key = name == null ? "" : name.trim().toLowerCase();
        if (visibleEquals(key, "mode")) return "Modo de ativação";
        if (visibleContains(key, "target settings")) return "Prioridade de alvo";
        if (visibleEquals(key, "max angle") || visibleEquals(key, "fov")) return "FOV";
        if (visibleEquals(key, "distance") || visibleEquals(key, "reach")) return "Alcance";
        if (visibleEquals(key, "horizontal speed") && (visibleEquals(moduleName, "aim") || visibleEquals(moduleName, "aimassist"))) return "Suavidade";
        return name;
    }

    private String drawerOptionLabel(String option) {
        if (option == null) return "";
        String text = option.trim().replaceFirst("(?i)\\s+settings\\s+.*$", "");
        text = text.replaceAll("([a-z])([A-Z])", "$1 $2").replaceAll("\\s+", " ").trim();
        return text;
    }

    private String numberUnit(JsonObject value) {
        String unit = value.has("unit") && !value.get("unit").isJsonNull()
                ? value.get("unit").getAsString().trim() : "";
        if (unit.indexOf("//$") >= 0) unit = unit.substring(0, unit.indexOf("//$")).trim();
        if ("deg".equalsIgnoreCase(unit) || "degree".equalsIgnoreCase(unit)
                || "degrees".equalsIgnoreCase(unit)) return "°";
        if (!unit.isEmpty()) return unit;

        String name = value.has("name") && !value.get("name").isJsonNull()
                ? value.get("name").getAsString() : "";
        String key = name.toLowerCase(Locale.ROOT);
        if (visibleContains(key, "fov") || visibleContains(key, "angle")) return "°";
        if (visibleContains(key, "chance") || visibleContains(key, "accuracy") || visibleContains(key, "opacity")
                || visibleContains(key, "percentage") || visibleContains(key, "percent")) return "%";
        return "";
    }

    /**
     * Digits this setting is actually defined with. The client sends them in the
     * state payload; ignoring them and rounding everything to whole numbers is
     * what left sliders posting 19.837 while the panel showed 20.
     */
    private int numberDecimals(JsonObject value) {
        if (value.has("decimals") && !value.get("decimals").isJsonNull()) {
            return Math.max(0, Math.min(8, value.get("decimals").getAsInt()));
        }
        return 0;
    }

    /** Slider step: a whole-number setting never gets a fractional one. */
    private String numberStep(JsonObject value) {
        double increment = value.get("increment").getAsDouble();
        if (numberDecimals(value) == 0) {
            return Long.toString(Math.max(1L, Math.round(increment)));
        }
        return formatNumber(Double.toString(increment), numberDecimals(value));
    }

    /**
     * Slider bound. Whole-number settings round inwards so every reachable step
     * stays inside the range the module will accept.
     */
    private String numberBound(JsonObject value, String key, boolean upper) {
        double bound = value.get(key).getAsDouble();
        if (numberDecimals(value) == 0) {
            return Long.toString((long)(upper ? Math.floor(bound) : Math.ceil(bound)));
        }
        return formatNumber(Double.toString(bound), numberDecimals(value));
    }

    /** Current value snapped to the precision the control exposes. */
    private String numberControlValue(String raw, JsonObject value) {
        return formatNumber(raw, numberDecimals(value));
    }

    private String formatNumber(String raw, int decimals) {
        try {
            double number = Double.parseDouble(raw);
            if (Double.isNaN(number) || Double.isInfinite(number)) return raw;
            int places = Math.max(0, Math.min(8, decimals));
            String formatted = String.format(Locale.ROOT, "%." + places + "f", number);
            if (formatted.indexOf('.') >= 0) {
                formatted = formatted.replaceAll("0+$", "").replaceAll("\\.$", "");
            }
            return "-0".equals(formatted) ? "0" : formatted;
        } catch (NumberFormatException ignored) {
            return raw;
        }
    }

    private String formatNumberWithUnit(String raw, JsonObject value) {
        String formatted = formatNumber(raw, numberDecimals(value));
        String unit = numberUnit(value);
        if (unit.isEmpty()) return formatted;
        return ("°".equals(unit) || "%".equals(unit)) ? formatted + unit : formatted + " " + unit;
    }

    private void appendValue(StringBuilder html, String moduleDisplayName,
            String moduleWireName, String moduleAlias, JsonObject value) {
        String id = value.get("id").getAsString();
        String name = value.get("name").getAsString();
        String type = value.get("type").getAsString();
        String current = value.get("value").getAsString();
        boolean active = !value.has("active") || value.get("active").getAsBoolean();
        boolean controlsVisibility = value.has("controlsVisibility") && value.get("controlsVisibility").getAsBoolean();
        html.append("<form class=\"value ajax-form").append(active ? "" : " condition-hidden").append("\" method=\"post\" action=\"value\"")
                .append(controlsVisibility ? " data-controls-visibility=\"1\"" : "")
                .append(" data-value-name=\"").append(attribute(name.toLowerCase())).append("\">")
                .append(hidden("csrf", this.csrfFormValue())).append(hidden("module", moduleAlias)).append(hidden("valueId", this.valueAlias(moduleWireName, id)))
                .append("<div class=\"value-body\"><span class=\"value-label\">").append(escape(drawerValueLabel(moduleDisplayName, name))).append("</span>");
        if ("boolean".equals(type)) {
            html.append("<input type=\"hidden\" name=\"value\" value=\"").append(attribute(current)).append("\"><label class=\"ios-switch\"><input type=\"checkbox\" ").append("true".equals(current) ? "checked" : "").append("><span class=\"ios-slider\"></span></label><span class=\"toggle-label\">").append("true".equals(current) ? "Ativo" : "Desativado").append("</span>");
        } else if ("mode".equals(type)) {
            html.append("<select name=\"value\">");
            for (JsonElement option : value.getAsJsonArray("options")) {
                String optionText = option.getAsString();
                html.append("<option value=\"").append(attribute(optionText)).append("\"")
                        .append(optionText.equals(current) ? " selected" : "").append(">")
                        .append(escape(value.has("optionLabels") && value.getAsJsonObject("optionLabels").has(optionText)
                                ? value.getAsJsonObject("optionLabels").get(optionText).getAsString() : optionText)).append("</option>");
            }
            html.append("</select>");
        } else if ("number".equals(type)) {
            String unit = numberUnit(value);
            int decimals = numberDecimals(value);
            String minimum = numberBound(value, "minimum", false);
            String maximum = numberBound(value, "maximum", true);
            String step = numberStep(value);
            html.append("<div class=\"range-row\" data-value-id=\"").append(attribute(id))
                    .append("\" data-unit=\"").append(attribute(unit)).append("\" data-decimals=\"").append(decimals)
                    .append("\"><div class=\"range-wrap\" style=\"flex:1\"><input name=\"value\" type=\"range\" required value=\"").append(attribute(numberControlValue(current, value)))
                    .append("\" min=\"").append(minimum).append("\" max=\"")
                    .append(maximum).append("\" step=\"")
                    .append(step).append("\" data-min=\"").append(minimum).append("\" data-max=\"").append(maximum).append("\"></div><output data-output data-unit=\"").append(attribute(unit)).append("\" data-decimals=\"").append(decimals).append("\" style=\"min-width:56px;text-align:center;background:#1A1D24;border:1px solid rgba(255,255,255,0.06);border-radius:8px;color:#ff6b8a;padding:4px 8px\">").append(escape(formatNumberWithUnit(current, value))).append("</output></div>");
        } else if ("range".equals(type)) {
            String unit = numberUnit(value);
            int decimals = numberDecimals(value);
            html.append(hidden("value", current)).append("<div class=\"dual-range\" data-value-id=\"").append(attribute(id)).append("\">")
                    .append("<div class=\"endpoint\" data-endpoint=\"lower\" data-unit=\"").append(attribute(unit)).append("\" data-decimals=\"").append(decimals).append("\"><div style=\"display:flex;justify-content:space-between;align-items:center;width:100%\"><small>Minimo</small><output data-output-lower data-unit=\"").append(attribute(unit)).append("\" data-decimals=\"").append(decimals).append("\" style=\"min-width:48px;text-align:center;background:#1A1D24;border:1px solid rgba(255,255,255,0.06);border-radius:8px;color:#ff6b8a;padding:4px 8px\">").append(escape(formatNumberWithUnit(Double.toString(value.get("lower").getAsDouble()), value))).append("</output></div><input data-endpoint=\"lower\" type=\"range\" value=\"")
                    .append(numberControlValue(Double.toString(value.get("lower").getAsDouble()), value)).append("\" min=\"").append(numberBound(value, "minimum", false)).append("\" max=\"").append(numberBound(value, "maximum", true)).append("\" step=\"").append(numberStep(value)).append("\"></div>")
                    .append("<div class=\"endpoint\" data-endpoint=\"upper\" data-unit=\"").append(attribute(unit)).append("\" data-decimals=\"").append(decimals).append("\"><div style=\"display:flex;justify-content:space-between;align-items:center;width:100%\"><small>Maximo</small><output data-output-upper data-unit=\"").append(attribute(unit)).append("\" data-decimals=\"").append(decimals).append("\" style=\"min-width:48px;text-align:center;background:#1A1D24;border:1px solid rgba(255,255,255,0.06);border-radius:8px;color:#ff6b8a;padding:4px 8px\">").append(escape(formatNumberWithUnit(Double.toString(value.get("upper").getAsDouble()), value))).append("</output></div><input data-endpoint=\"upper\" type=\"range\" value=\"")
                    .append(numberControlValue(Double.toString(value.get("upper").getAsDouble()), value)).append("\" min=\"").append(numberBound(value, "minimum", false)).append("\" max=\"").append(numberBound(value, "maximum", true)).append("\" step=\"").append(numberStep(value)).append("\"></div></div>");
        } else if ("color".equals(type)) {
            boolean fade = value.has("fade") && value.get("fade").getAsBoolean();
            boolean rainbow = value.has("rainbow") && value.get("rainbow").getAsBoolean();
            boolean rainbowAllowed = !value.has("noRainbow") || !value.get("noRainbow").getAsBoolean();
            String fadeColor = value.has("fadeColor") ? value.get("fadeColor").getAsString() : value.get("color").getAsString();
            html.append(hidden("value", current)).append("<div class=\"color-control").append(fade ? " fade-active" : "").append("\"><div class=\"color-swatch\"><input class=\"color-picker\" type=\"color\" aria-label=\"Escolher cor\" value=\"").append(attribute(value.get("color").getAsString())).append("\"></div>")
                    .append("<div class=\"alpha-control\"><span>Opacidade</span><input class=\"color-alpha\" type=\"range\" min=\"0\" max=\"255\" step=\"1\" value=\"").append(value.get("alpha").getAsInt()).append("\"><output>").append(value.get("alpha").getAsInt()).append("</output></div>")
                    .append("<select class=\"color-mode\"><option value=\"fixed\"").append(!fade && !rainbow ? " selected" : "").append(">Cor fixa</option>")
                    .append(rainbowAllowed ? "<option value=\"rainbow\"" + (rainbow ? " selected" : "") + ">Rainbow</option>" : "")
                    .append("<option value=\"fade\"").append(fade ? " selected" : "").append(">Fade</option></select>")
                    .append("<label class=\"color-fade-picker\"><span>Segunda cor</span><input class=\"fade-picker\" type=\"color\" aria-label=\"Escolher segunda cor\" value=\"").append(attribute(fadeColor)).append("\"></label></div>");
        } else if ("flags".equals(type)) {
            int selected = 0;
            for (int index = 0; index < current.length(); index++) if (current.charAt(index) == '1') selected++;
            html.append(hidden("value", current)).append("<details class=\"multi-select\"><summary><span>Escolher alvos</span><b>")
                    .append(selected).append(" selecionado").append(selected == 1 ? "" : "s").append("</b></summary><div class=\"flag-grid\">");
            JsonArray options = value.getAsJsonArray("options");
            for (int index = 0; index < options.size(); index++) {
                String optionText = options.get(index).getAsString();
                html.append("<span class=\"flag-pill\"><input type=\"checkbox\" data-flag=\"").append(index).append("\"")
                        .append(index < current.length() && current.charAt(index) == '1' ? " checked" : "")
                        .append(">").append(escape(drawerOptionLabel(optionText))).append("</span>");
            }
            html.append("</div></details>");
        } else if ("text".equals(type)) {
            if (value.has("suggestions")) {
                html.append("<div class=\"player-autocomplete\" data-suggestions=\"")
                        .append(attribute(value.getAsJsonArray("suggestions").toString())).append("\"><input class=\"text-control player-name-input\" name=\"value\" type=\"text\" maxlength=\"16\" autocomplete=\"off\" role=\"combobox\" aria-autocomplete=\"list\" aria-expanded=\"false\" value=\"")
                        .append(attribute(current)).append("\"><div class=\"value-suggestions\" role=\"listbox\"></div></div>");
            } else {
                html.append("<input class=\"text-control\" name=\"value\" type=\"text\" maxlength=\"2048\" value=\"").append(attribute(current)).append("\">");
            }
        } else if ("player-list".equals(type)) {
            html.append(hidden("value", current)).append("<div class=\"player-list-editor\" data-suggestions=\"")
                    .append(attribute(value.has("suggestions") ? value.getAsJsonArray("suggestions").toString() : "[]"))
                    .append("\"><div class=\"player-list-add\"><div class=\"player-autocomplete\"><input class=\"player-list-input\" type=\"text\" maxlength=\"16\" autocomplete=\"off\" role=\"combobox\" aria-autocomplete=\"list\" aria-expanded=\"false\" placeholder=\"Nome do jogador\"><div class=\"value-suggestions\" role=\"listbox\"></div></div><button type=\"button\" class=\"player-list-add-button\">Adicionar</button></div><div class=\"player-list-entries\"></div></div>");
        } else if ("list".equals(type)) {
            html.append(hidden("value", current)).append("<details class=\"multi-select\"><summary>Escolher itens</summary><div class=\"item-grid flag-grid\">");
            java.util.Set<String> selectedItems = new java.util.HashSet<String>(java.util.Arrays.asList(current.split("\\r?\\n")));
            JsonArray choices = value.has("options") ? value.getAsJsonArray("options") : new JsonArray();
            for (JsonElement choice : choices) {
                String item = choice.getAsString();
                html.append("<span class=\"flag-pill\"><input type=\"checkbox\" data-item=\"").append(attribute(item)).append("\"")
                        .append(selectedItems.contains(item) ? " checked" : "").append(">").append(escape(drawerOptionLabel(item))).append("</span>");
            }
            html.append("</div></details>");
        } else {
            html.append("<textarea class=\"json-control\" name=\"value\" maxlength=\"16384\" rows=\"4\" spellcheck=\"false\">").append(escape(current)).append("</textarea><small class=\"advanced-note\">Configuracao avancada em JSON</small>");
        }
        html.append("</div><span class=\"saved-indicator\">salvo</span></form>");
    }

    private void appendPhonePanel(StringBuilder html) {
        // The phone URL is assembled by the browser from the obfuscated LAN
        // hint; the game process never holds a printable IP or URL.
        html.append("<section class=\"category-panel utility-panel\" data-panel=\"phone\"><div class=\"section-title\"><div><small>CONEXAO</small><h2>Celular</h2></div><span>mesma rede Wi-Fi</span></div>")
                .append("<div class=\"utility-card mobile-access\"><div class=\"qr-wrap\"><canvas id=\"mobile-qr\" width=\"94\" height=\"94\" role=\"img\" aria-label=\"QR Code para abrir o painel no celular\"></canvas><span>Escaneie</span></div><div class=\"mobile-copy\"><small>ACESSO PELO CELULAR</small>")
                .append("<strong>Escaneie o QR ou abra o endereco</strong><p>O jogo e o celular precisam estar conectados ao mesmo roteador.</p><code id=\"mobile-url\" data-hint=\"")
                .append(attribute(lanHintBase64())).append("\" data-port=\"").append(this.port).append("\"></code></div><button id=\"copy-url\" type=\"button\">Copiar link</button></div></section>");
    }

    private void appendConfigPanel(StringBuilder html, JsonObject remoteConfigs) {
        html.append("<section class=\"category-panel utility-panel\" data-panel=\"configs\"><div class=\"section-title\"><div><small>PERFIS</small><h2>Configs</h2></div><span>configs na nuvem</span></div><div class=\"utility-card\">");
        appendCloudConfigSlots(html, remoteConfigs);
        html
                .append("<div class=\"tip-card\"><b>Como configurar</b><span>Clique com o botao direito em qualquer modulo para abrir ou fechar suas opcoes.</span></div></div></section>");
    }

    /** Client-wide switches that are not tied to a single module. */
    private void appendSettingsPanel(StringBuilder html, JsonObject config) {
        boolean loaderInvisible = config != null && config.has("loaderInvisible")
                && config.get("loaderInvisible").getAsBoolean();
        boolean bindsOnlyInGame = config == null || !config.has("bindsOnlyInGame")
                || config.get("bindsOnlyInGame").getAsBoolean();
        boolean notifications = config == null || !config.has("notifications")
                || config.get("notifications").getAsBoolean();
        html.append("<section class=\"category-panel utility-panel\" data-panel=\"settings\"><div class=\"section-title\"><div><small>CLIENTE</small><h2>Settings</h2></div><span>loader + binds</span></div><div class=\"utility-card\">");
        appendSettingsToggle(html, "loader-visibility", "hidden", loaderInvisible,
                "Ocultar janela do loader",
                "Disponivel enquanto o loader estiver aberto.");
        appendSettingsToggle(html, "binds-in-game", "enabled", bindsOnlyInGame,
                "Binds somente no jogo",
                "Ignora os binds dos modulos enquanto o Minecraft nao esta em foco.");
        appendSettingsToggle(html, "notifications", "enabled", notifications,
                "Notifications", "Mostra as notificacoes do client.");
        html.append("</div></section>");
    }

    private void appendSettingsToggle(StringBuilder html, String action, String field,
                                      boolean checked, String label, String hint) {
        html.append("<form class=\"ajax-form friend-setting loader-setting\" method=\"post\" action=\"")
                .append(attribute(action)).append("\">")
                .append(hidden("csrf", this.csrfFormValue())).append(hidden(field, Boolean.toString(checked)))
                .append("<span>").append(escape(label)).append("</span>")
                .append("<label class=\"ios-switch\" title=\"").append(attribute(hint)).append("\"><input type=\"checkbox\" ")
                .append(checked ? "checked" : "").append("><span class=\"ios-slider\"></span></label>")
                .append("<small>").append(escape(hint)).append("</small></form>");
    }

    private void appendCloudConfigSlots(StringBuilder html, JsonObject response) {
        JsonObject[] slots = new JsonObject[4];
        boolean available = response != null && response.has("configs");
        if (available) {
            for (JsonElement element : response.getAsJsonArray("configs")) {
                JsonObject config = element.getAsJsonObject();
                int slot = config.get("slot").getAsInt();
                if (slot >= 0 && slot < slots.length) slots[slot] = config;
            }
        }
        int savedCount = 0;
        int firstEmptySlot = -1;
        for (int slot = 0; slot < slots.length; ++slot) {
            if (slots[slot] == null) {
                if (firstEmptySlot < 0) firstEmptySlot = slot;
            } else {
                ++savedCount;
            }
        }
        html.append("<div class=\"cloud-heading\"><div><small>BACKUP PESSOAL</small><strong>Configs salvas</strong></div><span>")
                .append(savedCount).append("/4</span></div>");
        if (!available) {
            String message = response != null && response.has("error") ? response.get("error").getAsString()
                    : "Sessao remota indisponivel. Abra o jogo pelo loader autenticado.";
            html.append("<p class=\"notice bad\" role=\"alert\">").append(escape(message))
                    .append(" <a href=\"./\">Atualizar</a></p>");
        }
        if (available && firstEmptySlot >= 0) {
            html.append("<form class=\"cloud-slot cloud-save-new ajax-form\" data-reload=\"1\" method=\"post\" action=\"remote-config\">")
                    .append(hidden("csrf", this.csrfFormValue())).append(hidden("slot", Integer.toString(firstEmptySlot)))
                    .append("<label class=\"slot-name\"><span>Nova configuração</span><input type=\"text\" name=\"name\" maxlength=\"32\" required placeholder=\"Nome da configuração\"></label>")
                    .append("<div class=\"slot-actions\"><button name=\"action\" value=\"save\" type=\"submit\" data-success=\"Config salva na nuvem.\">Salvar configuração</button></div></form>");
        }
        html.append("<div class=\"config-slots\">");
        for (int slot = 0; slot < slots.length; slot++) {
            JsonObject config = slots[slot];
            if (config == null) continue;
            String name = config.get("name").getAsString();
            html.append("<form class=\"cloud-slot ajax-form\" data-reload=\"1\" method=\"post\" action=\"remote-config\">")
                    .append(hidden("csrf", this.csrfFormValue())).append(hidden("slot", Integer.toString(slot)))
                    .append(hidden("name", name))
                    .append("<div class=\"slot-heading\"><small>CONFIGURAÇÃO SALVA</small><span class=\"slot-status stored\">SALVA</span></div>")
                    .append("<strong class=\"saved-config-name\">").append(escape(name)).append("</strong>");
            html.append("<p class=\"slot-meta\">");
            html.append("Versao ").append(config.has("version") ? config.get("version").getAsInt() : 1);
            if (config.has("updatedAt")) {
                String updatedAt = config.get("updatedAt").getAsString();
                html.append(" · <time datetime=\"").append(attribute(updatedAt)).append("\">")
                        .append(escape(updatedAt)).append("</time>");
            }
            html.append("</p><div class=\"slot-actions\"><button name=\"action\" value=\"load\" type=\"submit\" formnovalidate data-success=\"Config carregada no jogo.\">Carregar</button>")
                    .append("<button name=\"action\" value=\"save\" type=\"submit\" data-confirm=\"Atualizar esta configuração com os módulos atuais?\" data-success=\"Config atualizada.\">Atualizar</button>")
                    .append("<button class=\"danger\" name=\"action\" value=\"delete\" type=\"submit\" formnovalidate data-confirm=\"Excluir a configuração salva?\" data-success=\"Config excluida da nuvem.\">Excluir</button>");
            html.append("</div></form>");
        }
        if (savedCount == 0 && available) {
            html.append("<p class=\"cloud-offline\">Nenhuma configuração salva ainda.</p>");
        }
        html.append("</div>");
    }

    private void appendPartyPanel(StringBuilder html, JsonObject party,
            String authenticatedUsername, JsonArray modules) {
        boolean active = party != null && party.has("code");
        html.append("<section class=\"category-panel utility-panel\" data-panel=\"party\"><div class=\"section-title\"><div><small>SOCIAL</small><h2>Party</h2></div><span>")
                .append(authenticatedUsername == null || authenticatedUsername.isEmpty()
                        ? active ? "conectada" : "sem party"
                        : "conectado como " + escape(authenticatedUsername))
                .append("</span></div><div class=\"utility-card party-card\">");
        if (!active) {
            html.append("<div class=\"empty-state\"><i>◎</i><strong>Nenhuma party ativa</strong><p>Crie uma sala ou entre usando o codigo de 6 caracteres.</p></div>")
                    .append("<div class=\"party-actions\"><form class=\"ajax-form\" data-reload=\"1\" method=\"post\" action=\"party\">")
                    .append(hidden("csrf", this.csrfFormValue())).append("<button name=\"action\" value=\"create\" type=\"submit\" data-success=\"Party criada. Compartilhe o codigo com seus amigos.\">Criar party</button></form>")
                    .append("<form class=\"ajax-form party-join\" data-reload=\"1\" method=\"post\" action=\"party\">")
                    .append(hidden("csrf", this.csrfFormValue())).append("<input type=\"text\" name=\"code\" maxlength=\"6\" pattern=\"[A-Za-z0-9]{6}\" placeholder=\"CODIGO\" aria-label=\"Codigo da party\" autocomplete=\"off\" required><button name=\"action\" value=\"join\" type=\"submit\" data-success=\"Voce entrou na party.\">Entrar</button></form></div>");
        } else {
            html.append("<div class=\"party-heading\"><small>CODIGO DA PARTY</small><strong>").append(escape(party.get("code").getAsString())).append("</strong></div><div class=\"member-list\">");
            boolean leader = party.has("myRole") && "leader".equals(party.get("myRole").getAsString());
            for (JsonElement memberElement : party.getAsJsonArray("members")) {
                JsonObject member = memberElement.getAsJsonObject();
                String role = member.get("role").getAsString();
                String memberName = member.get("name").getAsString();
                html.append("<span><i></i>").append(escape(memberName))
                        .append(" <small>").append(escape(role));
                if (memberName.equals(authenticatedUsername)) html.append(" · voce");
                html.append("</small>");
                if (leader && !"leader".equals(role) && member.has("id")) {
                    html.append("<form class=\"ajax-form\" method=\"post\" action=\"party\">")
                            .append(hidden("csrf", this.csrfFormValue()))
                            .append("<input type=\"hidden\" name=\"memberId\" value=\"")
                            .append(attribute(member.get("id").getAsString()))
                            .append("\"><button class=\"danger\" name=\"action\" value=\"kick\" type=\"submit\">Expulsar</button></form>");
                }
                html.append("</span>");
            }
            html.append("</div><div class=\"party-actions\">");
            if (leader) {
                html.append("<form class=\"ajax-form\" method=\"post\" action=\"party\">").append(hidden("csrf", this.csrfFormValue()))
                        .append("<label class=\"sync-mode\">Compartilhar como<select name=\"mode\"><option value=\"config\">Only config</option><option value=\"activate\">Ativar for all</option></select></label>")
                        .append("<p class=\"cloud-description\">Only config preserva os modulos ligados/desligados de cada membro. Ativar for all liga os selecionados.</p><fieldset class=\"share-modules\"><legend>Modulos para compartilhar</legend>");
                for (JsonElement element : modules) {
                    JsonObject module = element.getAsJsonObject();
                    String name = module.get("name").getAsString();
                    String label = module.has("label") && !module.get("label").isJsonNull()
                            ? module.get("label").getAsString() : name;
                    html.append("<label><input type=\"checkbox\" name=\"share:").append(attribute(name)).append("\"> ").append(escape(label)).append("</label>");
                }
                html.append("</fieldset><button name=\"action\" value=\"sync\" type=\"submit\" data-success=\"Sincronizacao enviada aos membros da party.\">Sincronizar modulos</button></form>");
            }
            html.append("<form class=\"ajax-form\" data-reload=\"1\" method=\"post\" action=\"party\">").append(hidden("csrf", this.csrfFormValue()))
                    .append("<button class=\"danger\" name=\"action\" value=\"leave\" type=\"submit\" data-success=\"Voce saiu da party.\">Sair da party</button></form></div>");
        }
        html.append("</div></section>");
    }

    private void appendCommunityPanel(StringBuilder html, JsonObject remote,
            String authenticatedUsername) {
        boolean available = remote != null && !remote.has("error");
        JsonArray configs = available && remote.has("configs")
                ? remote.getAsJsonArray("configs") : new JsonArray();
        html.append("<section class=\"category-panel utility-panel\" data-panel=\"community\"><div class=\"section-title\"><div><small>SOCIAL</small><h2>Community</h2></div><span>")
                .append(configs.size()).append(" configs publicadas</span></div>")
                .append("<div class=\"utility-card community-card\"><div class=\"community-intro\"><div><small>PUBLICAR CONFIG</small><strong>Compartilhe seu perfil ativo</strong><p>Publicado como ")
                .append(escape(authenticatedUsername == null || authenticatedUsername.isEmpty()
                        ? "usuario autenticado" : authenticatedUsername))
                .append(". Outros usuarios poderao aplicar a config no jogo.</p></div>")
                .append("<form class=\"ajax-form community-publish\" data-reload=\"1\" method=\"post\" action=\"community\">")
                .append(hidden("csrf", this.csrfFormValue()))
                .append("<input type=\"text\" name=\"name\" maxlength=\"32\" placeholder=\"Nome da config\" required>")
                .append("<input type=\"text\" name=\"description\" maxlength=\"160\" placeholder=\"Descricao curta (opcional)\">")
                .append("<button name=\"action\" value=\"publish\" type=\"submit\"")
                .append(available ? "" : " disabled")
                .append(" data-success=\"Config publicada na Community.\">Publicar perfil ativo</button></form></div>");
        if (!available) {
            html.append("<div class=\"community-empty\">Community indisponivel: ")
                    .append(escape(remote != null && remote.has("error")
                            ? remote.get("error").getAsString() : "sessao nao configurada"))
                    .append("</div>");
        } else if (configs.size() == 0) {
            html.append("<div class=\"community-empty\">Ainda nao existem configs publicadas. Seja o primeiro.</div>");
        } else {
            html.append("<div class=\"community-list\">");
            for (JsonElement element : configs) {
                JsonObject config = element.getAsJsonObject();
                boolean mine = config.has("mine") && config.get("mine").getAsBoolean();
                html.append("<article class=\"community-item\"><div class=\"community-item-head\"><div><strong>")
                        .append(escape(config.get("name").getAsString())).append("</strong><small>por ")
                        .append(escape(config.get("owner").getAsString())).append("</small></div><span>")
                        .append(config.get("downloads").getAsInt()).append(" downloads</span></div><p>")
                        .append(escape(config.get("description").getAsString())).append("</p>")
                        .append("<form class=\"ajax-form community-actions\" data-reload=\"1\" method=\"post\" action=\"community\">")
                        .append(hidden("csrf", this.csrfFormValue()))
                        .append(hidden("configId", config.get("id").getAsString()))
                        .append("<button name=\"action\" value=\"load\" type=\"submit\" data-success=\"Config da Community aplicada.\">Usar config</button>");
                if (mine) {
                    html.append("<button class=\"danger\" name=\"action\" value=\"delete\" type=\"submit\" data-confirm=\"Remover esta config da Community?\" data-success=\"Publicacao removida.\">Excluir</button>");
                }
                html.append("</form></article>");
            }
            html.append("</div>");
        }
        html.append("</div></section>");
    }

    private boolean validHost(PanelExchange exchange) {
        byte[] hostHeader = exchange.getRequestHeaders().getFirstBytes("host");
        if (hostHeader == null || hostHeader.length == 0) {
            // The native menu never sends a Host header; it already proved
            // knowledge of the session root. Browsers always send one.
            return exchange.isNativeClient();
        }
        if (hostHeader.length > 255) return false;
        return hostPortMatches(hostHeader, 0, hostHeader.length);
    }

    private boolean validOrigin(PanelExchange exchange) {
        byte[] origin = exchange.getRequestHeaders().getFirstBytes("origin");
        if (origin == null || origin.length == 0) return true;
        // Chromium-based embedded browsers may use an opaque origin for a
        // local form navigation. The per-process CSRF token is still
        // mandatory below.
        if (origin.length == 4 && origin[0] == 'n' && origin[1] == 'u'
                && origin[2] == 'l' && origin[3] == 'l') return true;
        // Scheme must be http:// (7 bytes) followed by host[:port].
        if (origin.length < 8 || origin[0] != 'h' || origin[1] != 't'
                || origin[2] != 't' || origin[3] != 'p' || origin[4] != ':'
                || origin[5] != '/' || origin[6] != '/') return false;
        return hostPortMatches(origin, 7, origin.length);
    }

    /**
     * Byte-wise host[:port] validation. Candidates (loopback, site-local
     * interface, and the generic local hostname) are built digit-by-digit
     * from address bytes, so no IP or URL text is ever materialized.
     */
    private boolean hostPortMatches(byte[] value, int from, int to) {
        int colon = -1;
        for (int index = to - 1; index >= from; --index) {
            if (value[index] == ':') {
                colon = index;
                break;
            }
            if (value[index] == ']' || value[index] == '/') return false;
        }
        if (colon < 0) return false;
        int port = 0;
        for (int index = colon + 1; index < to; ++index) {
            if (value[index] < '0' || value[index] > '9') return false;
            port = port * 10 + (value[index] - '0');
            if (port > 65535) return false;
        }
        if (port != this.port) return false;
        int hostLength = colon - from;
        if (hostLength <= 0 || hostLength > 253) return false;
        byte[] candidate;
        candidate = ipv4Candidate(new byte[]{127, 0, 0, 1});
        if (hostEquals(value, from, colon, candidate)) return true;
        candidate = ipv4Candidate(this.lanAddress);
        if (candidate != null && hostEquals(value, from, colon, candidate)) return true;
        candidate = "localhost".getBytes(StandardCharsets.US_ASCII);
        return hostEquals(value, from, colon, candidate);
    }

    private static boolean hostEquals(byte[] value, int from, int to, byte[] candidate) {
        if (to - from != candidate.length) return false;
        for (int index = 0; index < candidate.length; ++index) {
            if (value[from + index] != candidate[index]) return false;
        }
        return true;
    }

    /** Decimal IPv4 text built from raw address bytes, never a constant. */
    private static byte[] ipv4Candidate(byte[] address) {
        if (address == null || address.length != 4) return null;
        ByteArrayOutputStream digits = new ByteArrayOutputStream(15);
        for (int index = 0; index < 4; ++index) {
            int octet = address[index] & 0xFF;
            if (octet >= 100) {
                digits.write('0' + octet / 100);
                octet %= 100;
            }
            if (octet >= 10) {
                digits.write('0' + octet / 10);
                octet %= 10;
            }
            digits.write('0' + octet);
            if (index < 3) digits.write('.');
        }
        return digits.toByteArray();
    }

    /** Site-local IPv4 of the first usable LAN interface, as raw bytes. */
    private static byte[] findLanAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface network = interfaces.nextElement();
                if (!network.isUp() || network.isLoopback() || network.isVirtual()) continue;
                Enumeration<InetAddress> addresses = network.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address.isSiteLocalAddress()) {
                        byte[] raw = address.getAddress();
                        if (raw.length == 4) return raw;
                    }
                }
            }
        } catch (Throwable ignored) {
            // The loopback panel remains available when no LAN adapter exists.
        }
        return null;
    }

    /**
     * XOR-obfuscated LAN hint for the browser: the browser reassembles the
     * phone URL client-side, so the game process never holds a URL or a
     * printable IP. Empty when there is no LAN adapter (the browser falls
     * back to its own loopback address).
     */
    private String lanHintBase64() {
        byte[] lan = this.lanAddress;
        if (lan == null) return "";
        int high = (this.port >> 8) & 0xFF;
        int low = this.port & 0xFF;
        byte[] masked = new byte[4];
        for (int index = 0; index < 4; ++index) {
            masked[index] = (byte)(lan[index] ^ (index % 2 == 0 ? high : low));
        }
        return Base64.getEncoder().encodeToString(masked);
    }

    private void applySavedLoaderVisibility() {
        try {
            if (ClientSettings.INSTANCE == null) {
                return;
            }
            if (ClientSettings.INSTANCE.loaderInvisible.getEffectiveValue().booleanValue()) {
                NativeBridge.slw(false);
            }
        } catch (Throwable ignored) {
            // The local panel also runs without the native loader bridge.
        }
    }

    private static String categoryIcon(String category) {
        if (category == null || category.isEmpty()) return "•";
        return escape(category.substring(0, 1).toUpperCase());
    }

    private static byte[] readBody(PanelExchange exchange) throws IOException {
        return exchange.bodyBytes();
    }

    private static Map<String, String> parseForm(byte[] body) throws IOException {
        Map<String, String> values = new LinkedHashMap<String, String>();
        if (body == null || body.length == 0) return values;
        String decoded = new String(body, StandardCharsets.UTF_8);
        Arrays.fill(body, (byte)0);
        String[] fields = decoded.split("&", -1);
        if (fields.length > MAX_FORM_FIELDS) throw new IOException("too many form fields");
        for (String field : fields) {
            String[] parts = field.split("=", 2);
            String key = URLDecoder.decode(parts[0], "UTF-8");
            String value = URLDecoder.decode(parts.length == 2 ? parts[1] : "", "UTF-8");
            int maximumLength = "value".equals(key) ? 16384 : 256;
            if (key.length() > 32 || value.length() > maximumLength) throw new IOException("form field too large");
            values.put(key, value);
        }
        return values;
    }

    /** CSRF check in bytes: the stored token is unmasked only for the comparison. */
    private boolean csrfMatches(String submitted) {
        if (submitted == null) return false;
        byte[] expected = unmaskSecret(this.csrfMasked, this.csrfLength());
        byte[] actual = submitted.getBytes(StandardCharsets.UTF_8);
        boolean equal = MessageDigest.isEqual(expected, actual);
        Arrays.fill(expected, (byte)0);
        return equal;
    }

    /** CSRF value for an HTML form; short-lived and consumed by the response. */
    private String csrfFormValue() {
        byte[] plain = unmaskSecret(this.csrfMasked, this.csrfLength());
        String value = new String(plain, StandardCharsets.US_ASCII);
        Arrays.fill(plain, (byte)0);
        return value;
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        if (actual == null) return false;
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }

    private static String queryStatus(PanelExchange exchange) {
        byte[] query = exchange.getRequestURI().rawQueryBytes();
        if (query == null) return "";
        if (query.length == 12 && new String(query, StandardCharsets.US_ASCII).equals("status=saved")) return "saved";
        if (query.length == 13 && new String(query, StandardCharsets.US_ASCII).equals("status=invalid")) return "invalid";
        return "";
    }

    private static String hidden(String name, String value) {
        return "<input type=\"hidden\" name=\"" + attribute(name) + "\" value=\"" + attribute(value) + "\">";
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String attribute(String value) {
        return escape(value).replace("`", "&#96;");
    }

    private void redirect(PanelExchange exchange, String status) throws IOException {
        byte[] location = rootBytesWith("?status=" + status);
        exchange.getResponseHeaders().setBytes("Location", location);
        exchange.sendResponseHeaders(303, -1);
    }

    private void redirectToSessionRoot(PanelExchange exchange) throws IOException {
        byte[] location = rootBytesWith("");
        exchange.getResponseHeaders().setBytes("Location", location);
        exchange.sendResponseHeaders(308, -1);
    }

    /** Session root plus a suffix, unmasked only long enough to answer. */
    private byte[] rootBytesWith(String suffix) {
        byte[] plain = unmaskSecret(this.sessionRootMasked, this.sessionRootLength);
        byte[] extended = Arrays.copyOf(plain, plain.length
                + suffix.getBytes(StandardCharsets.US_ASCII).length);
        System.arraycopy(suffix.getBytes(StandardCharsets.US_ASCII), 0, extended,
                plain.length, extended.length - plain.length);
        Arrays.fill(plain, (byte)0);
        return extended;
    }

    private boolean isSessionRootWithoutSlash(byte[] pathBytes) {
        if (pathBytes == null || pathBytes.length != this.sessionRootLength - 1) return false;
        byte[] plain = unmaskSecret(this.sessionRootMasked, this.sessionRootLength);
        boolean equal = true;
        for (int index = 0; index < pathBytes.length; ++index) {
            if (pathBytes[index] != plain[index]) {
                equal = false;
                break;
            }
        }
        Arrays.fill(plain, (byte)0);
        return equal;
    }

    private String routePath(byte[] pathBytes) {
        int rootLength = this.sessionRootLength;
        if (pathBytes == null || pathBytes.length < rootLength) return null;
        byte[] rootPlain = unmaskSecret(this.sessionRootMasked, rootLength);
        try {
            for (int index = 0; index < rootLength; ++index) {
                if (pathBytes[index] != rootPlain[index]) return null;
            }
            if (pathBytes.length == rootLength) return "/";
            int suffixLength = pathBytes.length - rootLength;
            if (suffixLength > 32) return null;
            byte[] suffix = Arrays.copyOfRange(pathBytes, rootLength,
                    pathBytes.length);
            String alias = new String(suffix, StandardCharsets.US_ASCII);
            Arrays.fill(suffix, (byte)0);
            synchronized (this.moduleAliases) {
                return this.aliasesToRoute.get(alias);
            }
        } finally {
            Arrays.fill(rootPlain, (byte)0);
        }
    }

    /**
     * Gives the injected native overlay the one-time local session details as
     * a compact binary descriptor (port, session root, CSRF token, per-route
     * aliases and the GUI bind). Nothing on either side holds a URL.
     */
    private void publishNativeMenuBootstrap() {
        final int guiBind = resolveGuiBind();
        Thread publisher = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int attempt = 0; attempt < 8; ++attempt) {
                    byte[] payload = buildMenuBootstrap(guiBind);
                    try {
                        NativeBridge.ss(payload);
                    } catch (Throwable unavailable) {
                        Arrays.fill(payload, (byte)0);
                        return;
                    }
                    // The native side wipes the array after consuming it, so
                    // every retry rebuilds the descriptor from the masks.
                    Arrays.fill(payload, (byte)0);
                    try {
                        Thread.sleep(350L);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }, "Crimson-Native-Menu-Bootstrap");
        publisher.setDaemon(true);
        publisher.start();
    }

    private int resolveGuiBind() {
        try {
            List<Integer> inputs = SonyVegas.INSTANCE.getPublicProfileSettings()
                    .guiBind.getValue().getBoundInputs();
            if (inputs != null && !inputs.isEmpty()) return inputs.get(0);
        } catch (Throwable ignored) {
            // RSHIFT remains the recovery bind until the first state snapshot.
        }
        return 161;
    }

    /**
     * Binary layout shared with the native parser (module_menu.cpp):
     * 'SVUB' | u16 version=2 | u16 port | i16 gui_bind | u8 root_len |
     * u8 csrf_len | u8 alias_count | root | csrf | 16 x (u8 len + bytes).
     */
    private byte[] buildMenuBootstrap(int guiBind) {
        String[] routes = { "/state", "/state-version", "/module", "/value",
                "/bind", "/gui-bind", "/module-reset", "/module-save",
                "/binds-in-game", "/configs", "/remote-config", "/community",
                "/party", "/notifications", "/friends", "/hud" };
        ByteArrayOutputStream payload = new ByteArrayOutputStream(320);
        payload.write('S');
        payload.write('V');
        payload.write('U');
        payload.write('B');
        writeU16(payload, 2);
        writeU16(payload, this.port);
        writeU16(payload, guiBind & 0xFFFF);
        byte[] rootPlain = unmaskSecret(this.sessionRootMasked, this.sessionRootLength);
        byte[] csrfPlain = unmaskSecret(this.csrfMasked, this.csrfLength());
        payload.write(rootPlain.length);
        payload.write(csrfPlain.length);
        payload.write(routes.length);
        payload.write(rootPlain, 0, rootPlain.length);
        Arrays.fill(rootPlain, (byte)0);
        payload.write(csrfPlain, 0, csrfPlain.length);
        Arrays.fill(csrfPlain, (byte)0);
        for (String route : routes) {
            byte[] token = routeToken(route).getBytes(StandardCharsets.US_ASCII);
            payload.write(token.length);
            payload.write(token, 0, token.length);
        }
        return payload.toByteArray();
    }

    private static void writeU16(ByteArrayOutputStream payload, int value) {
        payload.write(value & 0xFF);
        payload.write((value >> 8) & 0xFF);
    }

    private static boolean usesModuleAlias(String path) {
        return "/module".equals(path) || "/module-reset".equals(path)
                || "/module-save".equals(path) || "/value".equals(path)
                || "/bind".equals(path);
    }

    private String moduleAlias(String moduleName) {
        synchronized (this.moduleAliases) {
            String existing = this.moduleAliases.get(moduleName);
            if (existing != null) return existing;
            String alias = nextAliasToken();
            this.moduleAliases.put(moduleName, alias);
            this.aliasesToModule.put(alias, moduleName);
            return alias;
        }
    }

    private String resolveModuleAlias(String alias) {
        if (alias == null || !alias.matches("[a-z]{14}")) return null;
        synchronized (this.moduleAliases) {
            return this.aliasesToModule.get(alias);
        }
    }

    private void initializeRouteAliases() {
        String[] routes = { "/health", "/state-version", "/state", "/friends-state",
                "/friend-suggestions", "/app.js", "/app.css", "/module",
                "/module-reset", "/module-save", "/value", "/loader-visibility",
                "/binds-in-game", "/notifications", "/gui-bind", "/profile", "/remote-config", "/party", "/community",
                "/friends", "/bind", "/hud" };
        for (String route : routes) this.routeToken(route);
    }

    private String routeToken(String route) {
        synchronized (this.moduleAliases) {
            String existing = this.routeAliases.get(route);
            if (existing != null) return existing;
            String token = nextAliasToken();
            this.routeAliases.put(route, token);
            this.aliasesToRoute.put(token, route);
            return token;
        }
    }

    private String valueAlias(String moduleName, String valueId) {
        String key = moduleName + '\u0000' + valueId;
        synchronized (this.moduleAliases) {
            String existing = this.valueAliases.get(key);
            if (existing != null) return existing;
            String token = nextAliasToken();
            this.valueAliases.put(key, token);
            this.aliasesToValue.put(moduleName + '\u0000' + token, valueId);
            return token;
        }
    }

    private String resolveValueAlias(String moduleName, String alias) {
        if (moduleName == null || alias == null || !alias.matches("[a-z]{14}")) return null;
        synchronized (this.moduleAliases) {
            return this.aliasesToValue.get(moduleName + '\u0000' + alias);
        }
    }

    private String nextAliasToken() {
        String token;
        do {
            StringBuilder builder = new StringBuilder(14);
            for (int index = 0; index < 14; ++index) {
                builder.append((char)('a' + this.aliasRandom.nextInt(26)));
            }
            token = builder.toString();
        } while (this.aliasesToModule.containsKey(token)
                || this.aliasesToRoute.containsKey(token)
                || containsValueAlias(token));
        return token;
    }

    private boolean containsValueAlias(String token) {
        for (String key : this.aliasesToValue.keySet()) {
            if (key.endsWith("\u0000" + token)) return true;
        }
        return false;
    }

    private JsonObject obfuscateState(JsonObject source) {
        /*
         * The state is freshly built for this request, so it is safe to apply
         * the wire aliases in place. Badlion 1.8.9 can expose an older Gson
         * JsonObject through its parent class loader; calling deepCopy() then
         * raises NoSuchMethodError and turns every /state response into HTTP
         * 500 before the native menu receives any modules.
         */
        JsonObject result = source;
        JsonArray modules = result.getAsJsonArray("modules");
        if (modules == null) return result;
        for (JsonElement element : modules) {
            if (element == null || !element.isJsonObject()) continue;
            JsonObject module = element.getAsJsonObject();
            JsonElement nameElement = module.get("name");
            if (nameElement == null || nameElement.isJsonNull()) continue;
            String name = nameElement.getAsString();
            if (name == null || name.isEmpty()) continue;
            if (!module.has("label") || module.get("label").isJsonNull()) {
                module.addProperty("label", name);
            }
            module.addProperty("name", this.moduleAlias(name));
            JsonArray values = module.getAsJsonArray("values");
            if (values == null) continue;
            for (JsonElement valueElement : values) {
                if (valueElement == null || !valueElement.isJsonObject()) continue;
                JsonObject value = valueElement.getAsJsonObject();
                JsonElement idElement = value.get("id");
                if (idElement == null || idElement.isJsonNull()) continue;
                String id = idElement.getAsString();
                if (id == null || id.isEmpty()) continue;
                value.addProperty("id", this.valueAlias(name, id));
            }
        }
        return result;
    }

    private String obfuscateFormActions(String html) {
        synchronized (this.moduleAliases) {
            for (Map.Entry<String, String> entry : this.routeAliases.entrySet()) {
                String route = entry.getKey();
                html = html.replace("action=\"" + route.substring(1) + "\"",
                        "action=\"" + entry.getValue() + "\"");
                html = html.replace("src=\"" + route.substring(1) + "\"",
                        "src=\"" + entry.getValue() + "\"");
            }
        }
        return html;
    }

    private static String jsString(String token) {
        return "\"" + token + "\"";
    }

    private static String randomToken(int byteCount) {
        byte[] token = randomTokenBytes(byteCount);
        return new String(token, StandardCharsets.US_ASCII);
    }

    private static byte[] randomTokenBytes(int byteCount) {
        byte[] token = new byte[byteCount];
        new SecureRandom().nextBytes(token);
        return Base64.getUrlEncoder().withoutPadding().encode(token);
    }

    private static volatile byte[] compressedJavascript;
    private static volatile byte[] compressedStylesheet;

    private static void sendJavascript(PanelExchange exchange) throws IOException {
        byte[] bytes = compressedJavascript;
        if (bytes == null) {
            synchronized (LocalControlServer.class) {
                bytes = compressedJavascript;
                if (bytes == null) {
                    bytes = PayloadResources.readBytes(LocalControlServer.class,
                            "/assets/lIlIIlIl/144");
                    if (bytes == null) throw new IOException("144");
                    compressedJavascript = bytes;
                }
            }
        }
        exchange.getResponseHeaders().set("Content-Encoding", "gzip");
        sendBytes(exchange, 200, bytes, "application/javascript; charset=utf-8");
    }

    private static void sendStylesheet(PanelExchange exchange) throws IOException {
        byte[] bytes = compressedStylesheet;
        if (bytes == null) {
            synchronized (LocalControlServer.class) {
                bytes = compressedStylesheet;
                if (bytes == null) {
                    bytes = PayloadResources.readBytes(LocalControlServer.class,
                            "/assets/lIlIIlIl/145");
                    if (bytes == null) throw new IOException("145");
                    compressedStylesheet = bytes;
                }
            }
        }
        exchange.getResponseHeaders().set("Content-Encoding", "gzip");
        sendBytes(exchange, 200, bytes, "text/css; charset=utf-8");
    }

    private static void sendText(PanelExchange exchange, int status, String body, String contentType) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        sendBytes(exchange, status, bytes, contentType);
    }

    private static void sendBytes(PanelExchange exchange, int status, byte[] bytes, String contentType) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    private static void applySecurityHeaders(PanelHeaders headers) {
        headers.set("Cache-Control", "no-store, max-age=0");
        headers.set("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'; script-src 'self'; connect-src 'self'; img-src 'self'; form-action 'self'; frame-ancestors 'none'; base-uri 'none'");
        headers.set("Referrer-Policy", "no-referrer");
        headers.set("X-Content-Type-Options", "nosniff");
        headers.set("X-Frame-Options", "DENY");
    }

    private static int readPort() {
        String raw = System.getProperty("sonyvegas.localControlPort");
        if (raw == null || raw.trim().isEmpty()) raw = System.getenv("SONYVEGAS_LOCAL_CONTROL_PORT");
        if (raw == null || raw.trim().isEmpty()) return DEFAULT_PORT;
        try {
            int parsed = Integer.parseInt(raw.trim());
            return parsed >= 1024 && parsed <= 65535 ? parsed : DEFAULT_PORT;
        } catch (NumberFormatException ignored) {
            return DEFAULT_PORT;
        }
    }

    // Display text arrives §r-segmented (see RemoteControlClient) so module
    // names never sit contiguous in memory. Browsers cannot interpret § codes,
    // so visible nodes are cleaned once here, in the browser heap only.
    /* Main dashboard only. Drawer rules intentionally live in separate constants below. */
}

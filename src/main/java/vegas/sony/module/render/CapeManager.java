package vegas.sony.module.render;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import vegas.sony.event.EventHandler;
import vegas.sony.event.impl.EventPreTick;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.module.Category;
import vegas.sony.module.Mod;
import vegas.sony.remote.CapePresenceClient;
import vegas.sony.unmap.ModeOption;
import vegas.sony.value.BooleanValue;
import vegas.sony.value.ModeValue;
import vegas.sony.wrapper.impl.EntityLivingBase;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.ServerData;

/** Selects and shares a bundled cape between authenticated client users. */
public final class CapeManager extends Mod {
    private static final long PRESENCE_INTERVAL_MILLIS = 10000L;
    private final BundledCapeTexture textures = new BundledCapeTexture();
    private final ExecutorService presenceExecutor = Executors.newSingleThreadExecutor(
            new ThreadFactory() {
                @Override
                public Thread newThread(Runnable runnable) {
                    Thread thread = new Thread(runnable, "122");
                    thread.setDaemon(true);
                    return thread;
                }
            });
    private final AtomicBoolean presenceInFlight = new AtomicBoolean(false);
    private volatile Map<String, String> remoteCapes = Collections.emptyMap();
    private volatile long nextPresenceAt;
    private String failedStyle;
    private long retryAfterNanos;
    private final ModeOption tampinhasGroup = new ModeOption("Tampinhas Group //$ui");
    private final ModeOption tampinhasLogo = new ModeOption("Tampinhas Logo //$ui");
    private final ModeOption heart = new ModeOption("Heart //$ui");
    private final ModeOption redMen = new ModeOption("Red Men //$ui");
    private final ModeOption purpleMen = new ModeOption("Purple Men //$ui");
    private final ModeValue style = ModeValue.create(this, "Style //$ui", this.tampinhasGroup,
            this.tampinhasGroup, this.tampinhasLogo, this.heart, this.redMen, this.purpleMen);
    private final BooleanValue showCapes = BooleanValue.create(this, "Show capes //$ui", true,
            "Turns off every bundled cape locally and stops announcing yours //$ui");
    private final ModeOption onlyMe = new ModeOption("Only Me //$ui");
    private final ModeOption othersOnly = new ModeOption("Others Only //$ui");
    private final ModeOption meAndOthers = new ModeOption("Me and Others //$ui");
    private final ModeValue visibility = ModeValue.create(this, "Visibility //$ui", this.meAndOthers,
            this.onlyMe, this.othersOnly, this.meAndOthers);

    public CapeManager() {
        super("Cape //$ui", 0, Category.RENDER,
                "Shares a bundled cape with authenticated client users //$ui");
        this.addValue(this.showCapes, this.style, this.visibility);
        this.style.addChangeListener(value -> this.nextPresenceAt = 0L);
        this.visibility.addChangeListener(value -> this.nextPresenceAt = 0L);
        this.showCapes.addChangeListener(value -> {
            this.remoteCapes = Collections.emptyMap();
            this.nextPresenceAt = 0L;
        });
    }

    @Override
    public void setEnabled(boolean enabled, boolean bypassVisibilityCheck) {
        // Keep the manager registered so the persisted Show capes option is
        // always available.  Rendering and presence are controlled by that
        // explicit option rather than an un-toggleable module state.
        if (!enabled) return;
        super.setEnabled(true, bypassVisibilityCheck);
    }

    @Override
    public String getSimpleSuffix() {
        return this.style.getDisplayValue();
    }

    @Override
    public void onEnable() {
        this.failedStyle = null;
        this.retryAfterNanos = 0L;
        this.nextPresenceAt = 0L;
    }

    @EventHandler
    public void onTick(EventPreTick event) {
        long now = System.currentTimeMillis();
        boolean capesEnabled = this.showCapes.getEffectiveValue().booleanValue();
        if (!capesEnabled) {
            this.remoteCapes = Collections.emptyMap();
        }
        if (now < this.nextPresenceAt
                || !this.presenceInFlight.compareAndSet(false, true)) return;
        if (Minecraft.thePlayer().isNull()) {
            this.remoteCapes = Collections.emptyMap();
            this.nextPresenceAt = now + PRESENCE_INTERVAL_MILLIS;
            this.presenceInFlight.set(false);
            return;
        }
        String player = cleanName(Minecraft.thePlayer().getName());
        ServerData server = Minecraft.H();
        String address = server == null || server.isNull() ? "" : server.getServerIp();
        if (player.isEmpty() || address == null || address.trim().isEmpty()) {
            this.remoteCapes = Collections.emptyMap();
            this.nextPresenceAt = now + PRESENCE_INTERVAL_MILLIS;
            this.presenceInFlight.set(false);
            return;
        }
        final String serverKey = sha256(address.trim().toLowerCase(Locale.ROOT));
        final String localName = player;
        final String selectedStyle = this.getSelectedResourceName();
        final boolean announce = capesEnabled && shouldPublishOwnCape(this.visibility.getSelectedIndex());
        final boolean showRemote = capesEnabled;
        this.nextPresenceAt = now + PRESENCE_INTERVAL_MILLIS;
        this.presenceExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Map<String, String> snapshot = CapePresenceClient.exchange(serverKey,
                            localName, selectedStyle, announce);
                    remoteCapes = showRemote ? snapshot : Collections.emptyMap();
                } catch (Throwable ignored) {
                    // Preserve the most recent authenticated snapshot through
                    // short network interruptions.
                } finally {
                    presenceInFlight.set(false);
                }
            }
        });
    }

    public Object overrideCape(Object playerHandle, Object originalLocation) {
        String name = this.getCapeForPlayer(playerHandle);
        if (name == null) return originalLocation;
        if (name.equals(this.failedStyle) && System.nanoTime() < this.retryAfterNanos) {
            return originalLocation;
        }
        try {
            Object result = this.textures.getOrLoad(Minecraft.getTextureManager().getObject(),
                    MappedClasses.zC, name);
            this.failedStyle = null;
            return result;
        } catch (Throwable error) {
            this.failedStyle = name;
            this.retryAfterNanos = System.nanoTime() + 5000000000L;
            return originalLocation;
        }
    }

    public boolean isManagedCapeLocation(Object location) {
        return this.showCapes.getEffectiveValue().booleanValue()
                && this.textures.ownsLocation(location);
    }

    public boolean shouldForceCapeVisible(Object playerHandle) {
        if (!this.isEnabled()) return false;
        if (playerHandle == null || Minecraft.thePlayer().isNull()) return false;
        // The texture hook runs for every rendered player. Only the local
        // handle or a nick returned by authenticated presence is accepted.
        return this.getCapeForPlayer(playerHandle) != null;
    }

    @Override
    public void onDisable() {
        this.remoteCapes = Collections.emptyMap();
        this.nextPresenceAt = 0L;
    }

    private String getCapeForPlayer(Object playerHandle) {
        if (!this.isEnabled() || !this.showCapes.getEffectiveValue().booleanValue()
                || playerHandle == null
                || Minecraft.thePlayer().isNull()) return null;
        Object localHandle = Minecraft.thePlayer().getObject();
        if (playerHandle == localHandle || hasLocalPlayerName(playerHandle, localHandle)) {
            return shouldRenderOwnCape(this.visibility.getSelectedIndex())
                    ? this.getSelectedResourceName() : null;
        }
        try {
            String name = cleanName(new EntityLivingBase(playerHandle).getName());
            return name.isEmpty() ? null
                    : this.remoteCapes.get(name.toLowerCase(Locale.ROOT));
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Badlion can hand a render hook a proxy/wrapped player object. In that
     * case reference equality with Minecraft.thePlayer() is false even
     * though the rendered player is the local account. Match the stable
     * in-game name as a fallback so the local cape is never rejected before
     * the texture hook gets a chance to register it.
     */
    private static boolean hasLocalPlayerName(Object playerHandle, Object localHandle) {
        if (playerHandle == null || localHandle == null) return false;
        try {
            String playerName = cleanName(new EntityLivingBase(playerHandle).getName());
            String localName = cleanName(new EntityLivingBase(localHandle).getName());
            return !playerName.isEmpty() && playerName.equalsIgnoreCase(localName);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static String cleanName(String raw) {
        if (raw == null) return "";
        String name = raw.replaceAll("§.", "").trim();
        return name.matches("[A-Za-z0-9_]{1,16}") ? name : "";
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte current : digest) {
                hex.append(String.format(Locale.ROOT, "%02x", current & 255));
            }
            return hex.toString();
        } catch (Exception impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private String getSelectedResourceName() {
        if (this.style.getValue() == this.tampinhasLogo) return "123";
        if (this.style.getValue() == this.heart) return "718";
        if (this.style.getValue() == this.redMen) return "436";
        if (this.style.getValue() == this.purpleMen) return "953";
        return "122";
    }

    static boolean shouldRenderOwnCape(int visibilityMode) {
        return visibilityMode != 1;
    }

    static boolean shouldPublishOwnCape(int visibilityMode) {
        return visibilityMode != 0;
    }
}

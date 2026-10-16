package vegas.sony.ui.font;

import vegas.sony.SonyVegas;
import vegas.sony.ui.font.BufferedSmoothFontRenderer;
import vegas.sony.ui.font.FontFamily;
import vegas.sony.ui.font.FontFamilySwitchMap;
import vegas.sony.ui.font.LegacySmoothFontRenderer;
import vegas.sony.ui.font.SmoothFontRenderer;
import vegas.sony.ui.font.StbSmoothFontRenderer;
import vegas.sony.utils.render.GuiRenderPrimitives;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class FontManager {
    public static final double r = 0.9375;
    byte[] I;
    private final ConcurrentMap<Integer, SmoothFontRenderer> p;
    public static final double K = 0.5;
    byte[] J;
    public static ArrayList<String> y = new ArrayList();
    byte[] w;
    private final ConcurrentMap<Integer, SmoothFontRenderer> q;
    private final String l;
    private final ConcurrentMap<Integer, SmoothFontRenderer> N;
    public static final double z = 0.875;
    private final ConcurrentMap<Integer, SmoothFontRenderer> b = new ConcurrentHashMap<Integer, SmoothFontRenderer>();
    /** HUD typefaces in medium and semibold, keyed "<family>_<weight>_<size>". */
    private final ConcurrentMap<String, SmoothFontRenderer> hudFonts = new ConcurrentHashMap<String, SmoothFontRenderer>();
    private final Map<String, byte[]> hudFontData = new HashMap<String, byte[]>();
    byte[] V;
    private boolean X = false;
    public static final double C = 0.6875;
    private final ConcurrentMap<Integer, SmoothFontRenderer> u;
    private final ConcurrentMap<Integer, SmoothFontRenderer> U;
    public static final double t = 0.5625;
    private final ConcurrentMap<Integer, SmoothFontRenderer> M = new ConcurrentHashMap<Integer, SmoothFontRenderer>();
    public static final double i = 0.75;
    private final ConcurrentMap<Integer, SmoothFontRenderer> P = new ConcurrentHashMap<Integer, SmoothFontRenderer>();
    public static final double s = 0.8125;
    public static final double x = 0.625;
    public static final double Z = 16.0;
    private static final String[] HUD_FAMILIES = {"inter", "poppins", "roboto", "jetbrains",
            "rajdhani", "ubuntu", "play", "raleway"};

    public boolean p() {
        return this.X;
    }

    public SmoothFontRenderer n(FontFamily fontFamily, int n, boolean bl) {
        if (fontFamily != null) {
            switch (FontFamilySwitchMap.d[fontFamily.ordinal()]) {
                case 1: {
                    return this.z(n, bl);
                }
                case 2: {
                    return this.a(n, bl);
                }
                case 3: {
                    return this.o(n);
                }
                case 4: {
                    return this.x(n);
                }
            }
        }
        return this.D(n, bl);
    }

    public SmoothFontRenderer D(int n, boolean bl) {
        FontFamily fontFamily = SonyVegas.INSTANCE.getFontSelector().W().b();
        if (fontFamily != null) {
            return this.n(fontFamily, n, bl);
        }
        return this.z(n, bl);
    }

    public SmoothFontRenderer Y() {
        return this.H(false);
    }

    public SmoothFontRenderer W(double d, boolean bl) {
        int n = (int)(16.0 * d);
        double d2 = SonyVegas.INSTANCE.getClientSettings().getGuiScaleFactor();
        n = (int)((double)n * d2);
        return this.D(n, true);
    }

    public SmoothFontRenderer Y(double d) {
        return this.E(d, true);
    }

    public void U() {
        this.b.clear();
        this.M.clear();
        this.P.clear();
        this.N.clear();
        this.q.clear();
        this.U.clear();
        this.u.clear();
        this.p.clear();
        this.hudFonts.clear();
    }

    private SmoothFontRenderer a(int n, boolean bl) {
        ConcurrentMap<Integer, SmoothFontRenderer> concurrentMap = bl ? this.q : this.b;
        SmoothFontRenderer smoothFontRenderer = (SmoothFontRenderer)concurrentMap.get(n);
        if (smoothFontRenderer != null) {
            return smoothFontRenderer;
        }
        String string = bl ? "arialbd.ttf" : "arial.ttf";
        SmoothFontRenderer smoothFontRenderer2 = GuiRenderPrimitives.d() ? new BufferedSmoothFontRenderer(string, n) : new LegacySmoothFontRenderer(string, n);
        concurrentMap.put(n, smoothFontRenderer2);
        return smoothFontRenderer2;
    }

    private SmoothFontRenderer z(int n, boolean bl) {
        ConcurrentMap<Integer, SmoothFontRenderer> concurrentMap;
        ConcurrentMap<Integer, SmoothFontRenderer> concurrentMap2 = concurrentMap = bl ? this.U : this.M;
        if (!concurrentMap.containsKey(n)) {
            SmoothFontRenderer smoothFontRenderer;
            byte[] fontData = bl ? this.J : this.w;
            String fontName = bl ? "Proxima Bold" : "Proxima";
            if (GuiRenderPrimitives.d()) {
                smoothFontRenderer = new BufferedSmoothFontRenderer(fontData, n, fontName);
            } else {
                smoothFontRenderer = new LegacySmoothFontRenderer(fontData, n, fontName);
            }
            concurrentMap.put(n, smoothFontRenderer);
            return smoothFontRenderer;
        }
        return (SmoothFontRenderer)concurrentMap.get(n);
    }

    public SmoothFontRenderer p(double d) {
        int n = (int)(16.0 * d);
        double d2 = SonyVegas.INSTANCE.getClientSettings().getGuiScaleFactor();
        n = (int)((double)n * d2);
        return this.x(n);
    }

    public void e() {
        String string = "proxima.ttf";
        this.w = SonyVegas.readResource(string);
        String string2 = "proximabd.ttf";
        this.J = SonyVegas.readResource(string2);
        String string3 = "noto.ttf";
        this.V = SonyVegas.readResource(string3);
        String string4 = "poppins_rg.ttf";
        this.I = SonyVegas.readResource(string4);
        for (String family : HUD_FAMILIES) {
            this.hudFontData.put(family + "_medium",
                    SonyVegas.readResource(family + "_medium.ttf"));
            this.hudFontData.put(family + "_semibold",
                    SonyVegas.readResource(family + "_semibold.ttf"));
        }
        this.X = true;
    }

    public SmoothFontRenderer b(FontFamily fontFamily, float f, boolean bl) {
        int n = (int)(16.0f * f);
        double d = SonyVegas.INSTANCE.getClientSettings().getGuiScaleFactor();
        n = (int)((double)n * d);
        return this.n(fontFamily, n, bl);
    }

    public FontManager() {
        this.l = "arial";
        this.N = new ConcurrentHashMap<Integer, SmoothFontRenderer>();
        this.q = new ConcurrentHashMap<Integer, SmoothFontRenderer>();
        this.U = new ConcurrentHashMap<Integer, SmoothFontRenderer>();
        this.u = new ConcurrentHashMap<Integer, SmoothFontRenderer>();
        this.p = new ConcurrentHashMap<Integer, SmoothFontRenderer>();
    }

    public SmoothFontRenderer x(int n) {
        if (!GuiRenderPrimitives.d()) {
            return this.z(n, false);
        }
        SmoothFontRenderer smoothFontRenderer = (SmoothFontRenderer)this.p.get(n);
        if (smoothFontRenderer != null) {
            return smoothFontRenderer;
        }
        StbSmoothFontRenderer stbSmoothFontRenderer = new StbSmoothFontRenderer(n);
        this.p.put(n, stbSmoothFontRenderer);
        return stbSmoothFontRenderer;
    }


    public SmoothFontRenderer w() {
        return this.D(12, false);
    }

    public SmoothFontRenderer K(double d, boolean bl) {
        int n = (int)(16.0 * d);
        double d2 = SonyVegas.INSTANCE.getClientSettings().getGuiScaleFactor();
        SmoothFontRenderer smoothFontRenderer = (SmoothFontRenderer)this.P.get(n = (int)((double)n * d2));
        if (smoothFontRenderer != null) {
            return smoothFontRenderer;
        }
        SmoothFontRenderer smoothFontRenderer2 = GuiRenderPrimitives.d() ? new BufferedSmoothFontRenderer("bahnschrift", n) : new LegacySmoothFontRenderer("bahnschrift", n);
        this.P.put(n, smoothFontRenderer2);
        return smoothFontRenderer2;
    }

    public SmoothFontRenderer E(double d, boolean bl) {
        int n = (int)(16.0 * d);
        double d2 = SonyVegas.INSTANCE.getClientSettings().getGuiScaleFactor();
        n = (int)((double)n * d2);
        return this.D(n, false);
    }

    public SmoothFontRenderer H(boolean bl) {
        return this.D(16, bl);
    }

    private SmoothFontRenderer o(int n) {
        SmoothFontRenderer smoothFontRenderer = (SmoothFontRenderer)this.N.get(n);
        if (smoothFontRenderer != null) {
            return smoothFontRenderer;
        }
        SmoothFontRenderer smoothFontRenderer2 = GuiRenderPrimitives.d() ? new BufferedSmoothFontRenderer(this.V, n, "Noto") : new LegacySmoothFontRenderer(this.V, n, "Noto");
        this.N.put(n, smoothFontRenderer2);
        return smoothFontRenderer2;
    }

    /** One weight (medium or semibold) of a HUD typeface at a pixel size. */
    public SmoothFontRenderer getHudFont(String family, int n, boolean semibold) {
        String weightKey = family + (semibold ? "_semibold" : "_medium");
        String cacheKey = weightKey + "_" + n;
        SmoothFontRenderer cached = (SmoothFontRenderer)this.hudFonts.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        byte[] fontData = this.hudFontData.get(weightKey);
        if (fontData == null) {
            return this.z(n, semibold);
        }
        String fontName = "Hud " + weightKey;
        SmoothFontRenderer renderer = GuiRenderPrimitives.d() ? new BufferedSmoothFontRenderer(fontData, n, fontName) : new LegacySmoothFontRenderer(fontData, n, fontName);
        this.hudFonts.put(cacheKey, renderer);
        return renderer;
    }

    public SmoothFontRenderer a(int n) {
        SmoothFontRenderer smoothFontRenderer = (SmoothFontRenderer)this.u.get(n);
        if (smoothFontRenderer != null) {
            return smoothFontRenderer;
        }
        SmoothFontRenderer smoothFontRenderer2 = GuiRenderPrimitives.d() ? new BufferedSmoothFontRenderer(this.I, n, "Poppins") : new LegacySmoothFontRenderer(this.I, n, "Poppins");
        this.u.put(n, smoothFontRenderer2);
        return smoothFontRenderer2;
    }

    public Map<String, Map<Integer, SmoothFontRenderer>> n() {
        LinkedHashMap<String, Map<Integer, SmoothFontRenderer>> linkedHashMap = new LinkedHashMap<String, Map<Integer, SmoothFontRenderer>>();
        linkedHashMap.put("fonts", this.b);
        linkedHashMap.put("modernFonts", this.M);
        linkedHashMap.put("bahnschriftFont", this.P);
        linkedHashMap.put("notoFont", this.N);
        linkedHashMap.put("boldFonts", this.q);
        linkedHashMap.put("boldModernFonts", this.U);
        linkedHashMap.put("poppinsFont", this.u);
        return linkedHashMap;
    }
}

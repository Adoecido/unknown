package vegas.sony.module;

import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.unmap.INamed;
import vegas.sony.utils.EncString;
import vegas.sony.utils.Str;
import java.util.ArrayList;
import java.util.List;

public class Category
implements INamed {
    public static Category COMBAT;
    public static Category RENDER;
    public static Category NONE;
    private final EncString name;
    private static List<Category> values;
    private final int color;
    private final EncString description;
    public static Category HIDDEN;
    private static GuiComponent[] components;
    private final EncString iconKey;
    public static Category FAVORITES;
    public static Category NETWORK;
    public static Category GLADIADOR;
    public static Category MISC;

    public static GuiComponent[] getLegacyComponents() {
        return components;
    }

    public int getColor() {
        return this.color;
    }

    private Category(String name, String iconKey, String description, int color) {
        this.name = Str.enc(name);
        this.iconKey = Str.enc(iconKey);
        this.description = Str.enc(description);
        this.color = color;
    }

    static {
        if (Category.getLegacyComponents() == null) {
            Category.setLegacyComponents(new GuiComponent[4]);
        }
        NONE = new Category("None //$ui", "newfavorites //$", -2914273);
        COMBAT = new Category("Combat //$ui", "combat //$", "Combat advantage modules //$ui", -2745594);
        RENDER = new Category("Render //$ui", "render //$", "All kinds of visual goodies //$ui", -7926107);
        NETWORK = new Category("Network //$ui", "network //$", "Packet manipulation modules //$ui", -14651232);
        GLADIADOR = new Category("Gladiador //$ui", "world //$", "Gladiador modules //$ui", -12170992);
        MISC = new Category("Misc //$ui", "render //$", "Miscellaneous modules //$ui", -14970556);
        FAVORITES = new Category("Favorites //$ui", "newfavorites //$", "", 0);
        HIDDEN = new Category("Hidden //$ui", "favorites //$", -2419323);
        values = new ArrayList<Category>();
        values.add(FAVORITES);
        values.add(COMBAT);
        values.add(RENDER);
        values.add(NETWORK);
        values.add(GLADIADOR);
        values.add(MISC);
        values.add(NONE);
    }

    private Category(String name, String iconKey, int color) {
        this(name, iconKey, "", color);
    }

    public String toString() {
        return this.name.get();
    }

    public static void setLegacyComponents(GuiComponent[] legacyComponents) {
        components = legacyComponents;
    }

    @Override
    public String getName() {
        return this.name.get();
    }

    public static List<Category> values() {
        return values;
    }

    public static void destruct() {
        values.clear();
        values = null;
        NONE = null;
        COMBAT = null;
        RENDER = null;
        NETWORK = null;
        GLADIADOR = null;
        MISC = null;
        FAVORITES = null;
    }

    public String getDescription() {
        return this.description == null ? null : this.description.get();
    }

    public String getIconKey() {
        return this.iconKey == null ? null : this.iconKey.get();
    }

    /** Stable wire identifier; category display names remain segmented. */
    public int getRemoteId() {
        if (this == COMBAT) return 122;
        if (this == NETWORK) return 123;
        if (this == GLADIADOR) return 124;
        if (this == RENDER) return 125;
        if (this == MISC) return 126;
        if (this == FAVORITES) return 127;
        if (this == HIDDEN) return 128;
        return 0;
    }
}


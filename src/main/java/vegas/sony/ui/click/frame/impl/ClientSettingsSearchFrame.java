package vegas.sony.ui.click.frame.impl;

import com.google.gson.JsonObject;
import vegas.sony.SonyVegas;
import vegas.sony.friend.ui.OnlineFriendsFrame;
import vegas.sony.module.Category;
import vegas.sony.module.Mod;
import vegas.sony.module.none.ClientSettings;
import vegas.sony.ui.click.GuiMouseEvent;
import vegas.sony.ui.click.component.ColorDividerComponent;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.ui.click.component.SimpleTextLabelComponent;
import vegas.sony.ui.click.component.SpacerComponent;
import vegas.sony.ui.click.component.module.ModuleComponent;
import vegas.sony.ui.click.frame.Frame;
import vegas.sony.ui.click.frame.FrameNavigationButtonComponent;
import vegas.sony.ui.click.frame.ModuleCategoryNavigationButtonComponent;
import vegas.sony.ui.click.frame.OutlinedFrameBase;
import vegas.sony.ui.click.frame.impl.profile.ProfilesFrameNavigationButtonComponent;
import vegas.sony.ui.click.frame.impl.profile.ProfilesSettingsFrame;
import vegas.sony.unmap.ModeSelection;
import java.util.ArrayList;
import java.util.List;

public class ClientSettingsSearchFrame
extends ModuleCategoryFrame {
    private static final double MENU_WIDTH = 110.0;
    private final ClientSettingsSearchFrameHeader Ij;
    private final List<GuiComponent> II = new ArrayList<GuiComponent>();

    @Override
    public String getName() {
        return "GUI";
    }

    @Override
    public double A() {
        return MENU_WIDTH;
    }

    private void g(GuiComponent guiComponent) {
        this.h(guiComponent, new Object[0]);
        this.II.add(guiComponent);
    }

    public void K$src$V$1nbah4f() {
        this.removeMarkedChildren();
        if (((ModeSelection)ClientSettings.INSTANCE.searchBarStyle.getValue()).equals(ClientSettings.INSTANCE.integratedSearchBarMode) && this.Ij.O$src$Lvegas_sony_ui_click_component_input_ModuleSearchIn$1smhagf().getText().length() > 0) {
            for (Mod mod : SonyVegas.INSTANCE.getModManager().collectMods()) {
                if (mod.getCategory().equals(Category.NONE)) continue;
                String query = this.Ij.O$src$Lvegas_sony_ui_click_component_input_ModuleSearchIn$1smhagf().getText();
                boolean bl = mod.matchesSearch(query, false);
                if (!bl) continue;
                ModuleComponent moduleComponent = new ModuleComponent(this, mod);
                this.h(moduleComponent, new Object[0]);
                moduleComponent.buildValueComponents();
            }
        } else {
            this.o$src$V$1nv32hf();
        }
    }

    @Override
    public void dispatchMouseEvent(GuiMouseEvent guiMouseEvent) {
        super.dispatchMouseEvent(guiMouseEvent);
    }

    public ClientSettingsSearchFrame() {
        super(Category.NONE);
        this.setDisabledOverlayColor(ClientSettingsSearchFrame.J.r);
        this.K(32.0);
        this.S(32.0);
        this.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().M(false);
        this.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().M("wrap");
        this.Ij = new ClientSettingsSearchFrameHeader(this);
        this.Y(this.Ij);
        this.K$src$V$1nbah4f();
        this.L(false, true);
    }

    public ClientSettingsSearchFrameHeader o$src$Lvegas_sony_ui_click_frame_impl_ClientSettingsSearc$hz70uz() {
        return this.Ij;
    }

    @Override
    public void u() {
        if (!this.V$src$Z$1xhop3l()) {
            this.t(true, false);
        }
    }

    @Override
    public void J() {
        super.J();
    }

    private void o$src$V$1nv32hf() {
        if (this.II.isEmpty()) {
            this.g(new ColorDividerComponent(ClientSettingsSearchFrame.J.m));
            this.g(new ModuleCategoryNavigationButtonComponent("Combat", Category.COMBAT.getIconKey()));
            this.g(new ModuleCategoryNavigationButtonComponent("Render", Category.RENDER.getIconKey()));
            this.g(new ModuleCategoryNavigationButtonComponent("Network", Category.NETWORK.getIconKey()));
            this.g(new ModuleCategoryNavigationButtonComponent("Gladiador", Category.GLADIADOR.getIconKey()));
            this.g(new ModuleCategoryNavigationButtonComponent("Misc", Category.MISC.getIconKey()).Q(-1));
            SonyVegas sonyvegas = SonyVegas.INSTANCE;
            if (sonyvegas.isFeatureDisabled()) {
                this.g(new ModuleCategoryNavigationButtonComponent("Other", "other").Q(1));
            }
            this.g(new SpacerComponent(1.0, 2.0));
            this.g(new SimpleTextLabelComponent("  MISC", 0.625));
            this.g(new SpacerComponent(1.0, 2.0));
            this.g(new ColorDividerComponent(ClientSettingsSearchFrame.J.m));
            this.g(new FrameNavigationButtonComponent("Friends", null, OnlineFriendsFrame.class).addClickListener(new ClientSettingsSearchFrameClassOpenClickHandler(this, OnlineFriendsFrame.class)));
            this.g(new ProfilesFrameNavigationButtonComponent().addClickListener(new ClientSettingsSearchFrameClassOpenClickHandler(this, ProfilesSettingsFrame.class)));
            this.g(new FrameNavigationButtonComponent("Macros", null, FrameMacros.class).addClickListener(new ClientSettingsSearchFrameClassOpenClickHandler(this, FrameMacros.class)));
            this.g(new ColorDividerComponent(ClientSettingsSearchFrame.J.m));
        } else {
            for (GuiComponent guiComponent : this.II) {
                this.h(guiComponent, new Object[0]);
            }
        }
    }

    @Override
    public void t(JsonObject jsonObject) {
        super.t(jsonObject);
        this.K$src$V$1nbah4f();
        ClientSettings.refreshModuleCategoryHeaders();
    }

    @Override
    public void v() {
    }

    public void N$src$V$1ncxuwi() {
        ClientSettings.UI_EXECUTOR.execute(this::lambda$resetSearch$0);
    }

    public static long V(String string) {
        byte[] byArray;
        long l = 3988292384L;
        long l2 = 0xFFFFFFFFL;
        byte[] byArray2 = byArray = string.getBytes();
        boolean bl = OutlinedFrameBase.S$src$Z$g9yfcv();
        for (byte by : byArray2) {
            l2 ^= (long)by & 0xFFL;
            for (int i = 0; i < 8; ++i) {
                if ((l2 & 1L) == 1L) {
                    l2 = l2 >>> 1 ^ l;
                    continue;
                }
                l2 >>>= 1;
            }
        }
        return l2 ^ 0xFFFFFFFFL;
    }

    private void lambda$resetSearch$0() {
        this.Ij.O$src$Lvegas_sony_ui_click_component_input_ModuleSearchIn$1smhagf().setText("");
        this.K$src$V$1nbah4f();
    }

    @Override
    public void Y() {
    }

    private static Throwable a(Throwable throwable) {
        return throwable;
    }
}

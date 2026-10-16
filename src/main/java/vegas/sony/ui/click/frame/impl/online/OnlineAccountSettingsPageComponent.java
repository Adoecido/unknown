package vegas.sony.ui.click.frame.impl.online;

import vegas.sony.SonyVegas;
import vegas.sony.account.AccountInfo;
import vegas.sony.manager.client.OnlineConnectionManager;
import vegas.sony.manager.client.OnlineSettings;
import vegas.sony.notification.NotificationType;
import vegas.sony.ui.click.component.FilledSpacerComponent;
import vegas.sony.ui.click.component.GlyphIconComponent;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.ui.click.component.PanelComponent;
import vegas.sony.ui.click.component.SimpleTextLabelComponent;
import vegas.sony.ui.click.component.SpacerComponent;
import vegas.sony.ui.click.component.WrappingTextLabelComponent;
import vegas.sony.ui.click.component.gui.AnimatedCenteredTextLabelComponent;
import vegas.sony.ui.click.component.gui.TextButton;
import vegas.sony.ui.click.component.gui.TextLabel;
import vegas.sony.ui.click.component.input.DebouncedTextInputComponent;
import vegas.sony.ui.click.component.layout.PaddedComponent;
import vegas.sony.ui.click.component.value.BooleanToggleComponent;
import vegas.sony.ui.click.frame.FrameComponent;
import vegas.sony.ui.click.frame.impl.online.LinkedBooleanSettingsToggleComponent;
import vegas.sony.ui.click.frame.impl.online.OnlineAccountSettingsTextInputComponent;
import vegas.sony.ui.click.frame.impl.online.OnlineConnectionSettingsFrame;
import vegas.sony.ui.click.frame.impl.online.OnlineConnectionSettingsPageComponent;
import vegas.sony.ui.click.frame.impl.profile.PublicProfileUserAvatarComponent;
import vegas.sony.utils.ClipboardUtil;
import vegas.sony.utils.render.ImageRenderer;
import vegas.sony.value.BooleanValue;
import java.awt.Color;
import java.util.concurrent.atomic.AtomicBoolean;

public class OnlineAccountSettingsPageComponent
extends OnlineConnectionSettingsPageComponent {
    public final AtomicBoolean fe;
    private static boolean fs;
    private final PanelComponent fH;
    private final PanelComponent fS;
    private final DebouncedTextInputComponent fi;
    private final PanelComponent fJ;
    private final PanelComponent fE;
    private final TextButton f2;
    private SimpleTextLabelComponent f6 = new SimpleTextLabelComponent("User");
    private final PublicProfileUserAvatarComponent fL;
    private final PanelComponent f3;

    private BooleanToggleComponent A(BooleanValue booleanValue) {
        BooleanToggleComponent booleanToggleComponent = new BooleanToggleComponent(booleanValue.getDisplayName(), 0.8, booleanValue);
        booleanToggleComponent.setExplicitWidth(this.fE.A());
        booleanToggleComponent.setUseExplicitWidth(true);
        booleanToggleComponent.setExplicitHeight(10.0);
        booleanToggleComponent.setUseExplicitHeight(true);
        booleanToggleComponent.setDisabledOverlayColor(OnlineAccountSettingsPageComponent.J.t);
        return booleanToggleComponent;
    }

    @Override
    public void s() {
        this.f6.setText(currentUsername());
        this.fL.W(currentUserId());
    }

    @Override
    public void c() {
        super.c();
        this.f6.setText(currentUsername());
        this.f2.setLabelText("  Logout");
        ImageRenderer.drawImage(OnlineAccountSettingsPageComponent.J.W, (float)this.f2.G$src$D$1b2f02a() + 6.0f, (float)this.f2.n() + 2.0f, "signout", 5.0f, 5.0f, false);
    }

    static {
        OnlineAccountSettingsPageComponent.C(false);
    }

    public static void H(OnlineAccountSettingsPageComponent onlineAccountSettingsPageComponent) {
        onlineAccountSettingsPageComponent.N$src$V$102858o();
    }

    public static boolean p() {
        return fs;
    }

    private static void lambda$new$1() {
        OnlineConnectionSettingsFrame.getInstance().p();
    }

    public static boolean R$src$Z$104fbpk() {
        boolean bl = OnlineAccountSettingsPageComponent.p();
        return true;
    }

    private static void lambda$new$0() {
        String username = currentUsername();
        ClipboardUtil.setText(username);
        SonyVegas.INSTANCE.getNotificationManager().show("Copied", "Copied " + username, NotificationType.INFO, 5000L);
    }

    private static String currentUsername() {
        AccountInfo accountInfo = SonyVegas.INSTANCE == null ? null : SonyVegas.INSTANCE.getAccountInfo();
        return accountInfo == null || accountInfo.getUsername() == null ? "User" : accountInfo.getUsername();
    }

    private static long currentUserId() {
        AccountInfo accountInfo = SonyVegas.INSTANCE == null ? null : SonyVegas.INSTANCE.getAccountInfo();
        return accountInfo == null ? -1L : accountInfo.getUserId();
    }


    public OnlineAccountSettingsPageComponent() {
        this.f2 = new TextButton("Logout", Color.RED);
        this.fS = new PanelComponent(104.0, 65.0);
        this.f3 = new PanelComponent(96.0, 18.0);
        this.fE = new PanelComponent(96.0, 50.0);
        this.fe = new AtomicBoolean(false);
        this.fi = new OnlineAccountSettingsTextInputComponent(this, "Enter username", 10000L);
        this.fS.setShowDisabledOverlay(false);
        this.f3.setShowDisabledOverlay(true);
        this.fE.setShowDisabledOverlay(true);
        this.fS.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().M("wrap");
        this.fL = new PublicProfileUserAvatarComponent(null, 16.0, 16.0);
        this.fL.setShowBorder(true);
        PanelComponent panelComponent = new PanelComponent(104.0, 24.0);
        this.fH = new PanelComponent(panelComponent.A(), 24.0);
        this.fH.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().M("widthwrap");
        panelComponent.h(this.fH, new Object[0]);
        this.f6 = new WrappingTextLabelComponent(currentUsername(), 1.0, Color.WHITE);
        this.f6.o(104.0);
        this.f6.Y(12.0);
        this.f6.setBold(true);
        this.fH.h(new PaddedComponent(0.0, 2.0, 0.0, 0.0, this.f6), "wrap");
        this.fJ = new PanelComponent(panelComponent.A(), 24.0);
        this.fJ.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().M("wrap");
        this.fJ.setVisible(false);
        panelComponent.h(this.fJ, new Object[0]);
        this.fi.getActionButton().setIconResource("newnext");
        this.fi.setBackgroundVisible(false);
        this.fi.setMaxLength(16);
        this.fJ.h(new SpacerComponent((this.fJ.A() - this.fi.A()) / 2.0, 0.0), "widthwrap");
        this.fJ.h(this.fi, new Object[0]);
        this.fJ.h(new SpacerComponent((this.fJ.A() - this.fi.A()) / 2.0 + 4.0, 0.0), "widthwrap");
        this.fJ.h(new FilledSpacerComponent(this.fi.A() - 20.0, 1.0, OnlineAccountSettingsPageComponent.J.y), new Object[0]);
        TextLabel textLabel = new TextLabel("Cancel", 0.8, false, OnlineAccountSettingsPageComponent.J.l);
        textLabel.setExplicitHeight(10.0);
        textLabel.setExplicitWidth(22.0);
        textLabel.addClickListener(this::N$src$V$102858o);
        this.fJ.h(new SpacerComponent((this.fJ.A() - textLabel.A()) / 2.0, 3.0), new Object[0]);
        this.fJ.h(new SpacerComponent((this.fJ.A() - textLabel.A()) / 2.0, 0.0), "widthwrap");
        this.fJ.h(textLabel, new Object[0]);
        AnimatedCenteredTextLabelComponent animatedCenteredTextLabelComponent = new AnimatedCenteredTextLabelComponent("COPY", OnlineAccountSettingsPageComponent.J.l);
        animatedCenteredTextLabelComponent.o(19.0);
        animatedCenteredTextLabelComponent.Y(10.0);
        animatedCenteredTextLabelComponent.setFontScale(0.6);
        animatedCenteredTextLabelComponent.setBorderAlpha(0.75f);
        animatedCenteredTextLabelComponent.setUseAlternateFont(true);
        animatedCenteredTextLabelComponent.addClickListener(OnlineAccountSettingsPageComponent::lambda$new$0);
        GlyphIconComponent glyphIconComponent = new GlyphIconComponent("newedit", 4.0, 4.0, 10.0, 10.0, OnlineAccountSettingsPageComponent.J.W, Color.WHITE, OnlineAccountSettingsPageComponent.J.l);
        glyphIconComponent.setCenterVertically(true);
        glyphIconComponent.setCenterHorizontally(true);
        glyphIconComponent.setOffsetY(-0.5);
        glyphIconComponent.addClickListener(this::N$src$V$102858o);
        this.fH.h(new SpacerComponent(35.0, 0.0), new Object[0]);
        this.fH.h(animatedCenteredTextLabelComponent, new Object[0]);
        this.fH.h(new SpacerComponent(3.0, 0.0), new Object[0]);
        this.fH.h(glyphIconComponent, new Object[0]);
        this.fS.h(new PaddedComponent(this.A() / 2.0 - this.fL.A() / 2.0, 6.0, this.fL), new Object[0]);
        PanelComponent panelComponent2 = new PanelComponent(104.0, 26.0);
        panelComponent2.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().M("warp");
        this.fS.h(panelComponent, new Object[0]);
        this.f3.setDisabledOverlayColor(OnlineAccountSettingsPageComponent.J.m);
        SimpleTextLabelComponent simpleTextLabelComponent = new SimpleTextLabelComponent("Status", 0.8, OnlineAccountSettingsPageComponent.J.A);
        simpleTextLabelComponent.Y(this.f3.L());
        this.f3.h(simpleTextLabelComponent, new Object[0]);
        this.f2.addClickListener(OnlineConnectionManager.INSTANCE::disconnect);
        this.f2.setExplicitHeight(10.0);
        this.f2.setExplicitWidth(45.0);
        this.f2.setNormalTextColor(OnlineAccountSettingsPageComponent.J.Z);
        this.f2.setDeriveTextColorFromBackground(false);
        this.f2.setCornerRadius(5.0f);
        this.f2.setUseAlternateFont(true);
        this.f2.setFontScale(0.8f);
        this.f2.setBackgroundAnimationColors(OnlineAccountSettingsPageComponent.J.l, OnlineAccountSettingsPageComponent.J.y);
        this.f3.h(new PaddedComponent(4.0, 4.0, this.f2), "alignRight");
        this.fE.setDisabledOverlayColor(OnlineAccountSettingsPageComponent.J.m);
        this.fE.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().M("wrap");
        this.fE.h(new SpacerComponent(0.0, 4.0), new Object[0]);
        this.fE.h(this.A(OnlineConnectionManager.INSTANCE.getSettings().getAutoLogin()), new Object[0]);
        Object object = OnlineConnectionManager.INSTANCE.getSettings();
        BooleanValue[] booleanValueArray = new BooleanValue[]{((OnlineSettings)object).getShareUsername(), ((OnlineSettings)object).getShareServer(), ((OnlineSettings)object).getShareInventory()};
        LinkedBooleanSettingsToggleComponent linkedBooleanSettingsToggleComponent = new LinkedBooleanSettingsToggleComponent(this, "Privacy settings", 0.8, null, booleanValueArray);
        linkedBooleanSettingsToggleComponent.setExplicitWidth(this.fE.A());
        linkedBooleanSettingsToggleComponent.setUseExplicitWidth(true);
        linkedBooleanSettingsToggleComponent.setDisabledOverlayColor(OnlineAccountSettingsPageComponent.J.t);
        this.fE.h(linkedBooleanSettingsToggleComponent, new Object[0]);
        TextButton textButton = new TextButton("View all settings", 0.8, OnlineAccountSettingsPageComponent.J.t, OnlineAccountSettingsPageComponent.J.z, OnlineAccountSettingsPageComponent.J.l, 7.0f, 1.0f, this.fE.A() - 8.0, 14.0);
        textButton.setDeriveTextColorFromBackground(false);
        textButton.setNormalTextColor(OnlineAccountSettingsPageComponent.J.A);
        this.fE.h(new PaddedComponent(4.0, 2.0, textButton), new Object[0]);
        textButton.addClickListener(OnlineAccountSettingsPageComponent::lambda$new$1);
        object = new PanelComponent(96.0, 75.0);
        ((FrameComponent)object).l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().M("wrap");
        ((FrameComponent)object).h(this.f3, new Object[0]);
        ((FrameComponent)object).h(new SpacerComponent(0.0, 4.0), new Object[0]);
        ((FrameComponent)object).h(this.fE, new Object[0]);
        this.addChildren(this.fS, new PaddedComponent(4.0, 0.0, (GuiComponent)object));
    }

    public static void C(boolean bl) {
        fs = bl;
    }

    private void N$src$V$102858o() {
        this.fH.setVisible(!this.fH.V$src$Z$1xhop3l());
        this.fJ.setVisible(!this.fJ.V$src$Z$1xhop3l());
    }
}

package vegas.sony.ui.click.frame.impl;

import vegas.sony.account.MinecraftSessionWrapper;
import vegas.sony.ui.click.component.FlowLayoutComponent;
import vegas.sony.ui.click.component.SimpleTextLabelComponent;
import vegas.sony.ui.click.component.SpacerComponent;
import vegas.sony.ui.click.component.TextInputComponentBase;
import vegas.sony.ui.click.component.gui.TextButton;
import vegas.sony.ui.click.frame.Frame;
import vegas.sony.ui.click.frame.FrameComponent;
import vegas.sony.ui.click.frame.impl.SessionSpoofApplyButtonComponent;
import vegas.sony.ui.click.frame.impl.SessionSpoofApplyClickHandler;
import vegas.sony.ui.click.frame.impl.SessionSpoofUsernameInputComponent;
import vegas.sony.utils.render.GuiRenderPrimitives;
import vegas.sony.wrapper.impl.Minecraft;
import java.awt.Color;

public class SessionSpoofFrame
extends Frame {
    private MinecraftSessionWrapper nu;
    private String n4;
    private TextButton nR;
    private TextInputComponentBase nZ;
    private FrameComponent n8 = new FlowLayoutComponent(150.0);

    static TextButton i(SessionSpoofFrame sessionSpoofFrame) {
        return sessionSpoofFrame.nR;
    }

    private void W() {
        this.n4 = this.nZ.getText();
        if (this.nu == null) {
            this.nu = Minecraft.Q$src$Lvegas_sony_account_MinecraftSessionWrapper_$1ftnn3u();
        }
        this.nR.setLabelText("Offline as " + this.n4 + "\nReset (" + this.nu.getUsername() + ")");
        MinecraftSessionWrapper minecraftSessionWrapper = MinecraftSessionWrapper.createLegacy(this.n4, "00000000-0000-0000-0000-000000000000", "", "mojang");
        Minecraft.w(minecraftSessionWrapper);
    }

    public static void M(SessionSpoofFrame sessionSpoofFrame) {
        sessionSpoofFrame.p();
    }

    public SessionSpoofFrame() {
        this.nZ = new SessionSpoofUsernameInputComponent(this, "Username");
        this.nR = new SessionSpoofApplyButtonComponent(this, "Reset", 0.9, SessionSpoofFrame.J.m, SessionSpoofFrame.J.l);
        this.o(150.0);
        this.Y(56.0);
        this.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().M(false);
        this.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().t(false);
        this.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().I(false);
        this.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().U(false);
        this.Y(false);
        this.setVisible(true);
        this.L(false, true);
        this.g(true);
        this.nZ.getActionButton().setIconResource("newnext");
        this.nR.addClickListener(new SessionSpoofApplyClickHandler(this));
        this.n8.addChildren(new SimpleTextLabelComponent("Offline account login"), this.nZ, new SpacerComponent(5.0, 10.0), this.nR);
        this.h(this.n8, new Object[0]);
    }

    @Override
    public String getName() {
        return "alts";
    }

    private void p() {
        if (this.nu != null) {
            Minecraft.w(this.nu);
            this.nu = null;
        }
    }

    public static MinecraftSessionWrapper t(SessionSpoofFrame sessionSpoofFrame) {
        return sessionSpoofFrame.nu;
    }

    @Override
    public void v() {
    }

    @Override
    public void Y() {
        GuiRenderPrimitives.y(0.0f, 0.0f, Minecraft.J(), Minecraft.h(), new Color(0, 0, 0, 255));
    }


    static void W(SessionSpoofFrame sessionSpoofFrame) {
        sessionSpoofFrame.W();
    }
}

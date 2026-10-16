package vegas.sony.ui.click.frame.impl.main;

import vegas.sony.SonyVegas;
import vegas.sony.friend.OnlineFriend;
import vegas.sony.ui.click.component.GlyphIconComponent;
import vegas.sony.ui.click.component.PanelComponent;
import vegas.sony.ui.click.component.SimpleTextLabelComponent;
import vegas.sony.ui.click.component.layout.PaddedComponent;
import vegas.sony.ui.click.frame.impl.main.ClickGuiOverlaySpec;
import vegas.sony.ui.click.frame.impl.main.ClickGuiOverlayTransitionMode;
import vegas.sony.ui.click.frame.impl.profile.PublicProfileUserAvatarComponent;
import vegas.sony.ui.theme.ThemeColors;

public final class ClickGuiFriendsFriendCardFactory {
    public static ClickGuiOverlaySpec createLocalUserOverlay() {
        return ClickGuiFriendsFriendCardFactory.createOverlay(null);
    }

    public static ClickGuiOverlaySpec createOverlay(OnlineFriend onlineFriend) {
        String string = onlineFriend != null && onlineFriend.getDisplayName() != null ? onlineFriend.getDisplayName() : "Client User";
        return ClickGuiOverlaySpec.builder().title(string).sidecarIcon("user").initializeContent(panel -> ClickGuiFriendsFriendCardFactory.populate(panel, onlineFriend)).transitionMode(ClickGuiOverlayTransitionMode.REPLACE).build();
    }

    private static void populate(PanelComponent panelComponent, OnlineFriend onlineFriend) {
        long l = SonyVegas.INSTANCE.getAccountInfo() != null ? SonyVegas.INSTANCE.getAccountInfo().getUserId() : -1L;
        String string = "Username";
        String string2 = "Offline";
        if (onlineFriend != null) {
            if (onlineFriend.getUser() != null) {
                l = onlineFriend.getUser().getId();
            }
            string = onlineFriend.getDisplayName() != null ? onlineFriend.getDisplayName() : string;
            string2 = onlineFriend.getStatus() != null ? onlineFriend.getStatus().getDisplayName() : string2;
        }
        PublicProfileUserAvatarComponent publicProfileUserAvatarComponent = new PublicProfileUserAvatarComponent(l, 28.0, 28.0);
        publicProfileUserAvatarComponent.setShowBorder(true);
        PaddedComponent paddedComponent = new PaddedComponent(6.0, publicProfileUserAvatarComponent);
        SimpleTextLabelComponent simpleTextLabelComponent = new SimpleTextLabelComponent(string, 1.0, ThemeColors.J.A, true);
        SimpleTextLabelComponent simpleTextLabelComponent2 = new SimpleTextLabelComponent(string2, 0.75, ThemeColors.J.C);
        GlyphIconComponent glyphIconComponent = new GlyphIconComponent("chat", 6.0, 6.0, 40.0, 14.0, ThemeColors.J.W, ThemeColors.J.f, ThemeColors.J.l);
        GlyphIconComponent glyphIconComponent2 = new GlyphIconComponent("add friends@2x", 6.0, 6.0, 40.0, 14.0, ThemeColors.J.B, ThemeColors.J.B, ThemeColors.J.l);
        PanelComponent panelComponent2 = new PanelComponent(panelComponent.A(), 0.0);
        panelComponent2.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().M("wrap");
        panelComponent2.addChildren(paddedComponent, simpleTextLabelComponent, simpleTextLabelComponent2);
        PanelComponent panelComponent3 = new PanelComponent(panelComponent.A(), 0.0);
        panelComponent3.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().M("wrap");
        panelComponent3.addChildren(new PaddedComponent(2.0, glyphIconComponent), new PaddedComponent(2.0, glyphIconComponent2));
        panelComponent.addChildren(new PaddedComponent(6.0, panelComponent2), new PaddedComponent(8.0, panelComponent3));
    }

    private ClickGuiFriendsFriendCardFactory() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}


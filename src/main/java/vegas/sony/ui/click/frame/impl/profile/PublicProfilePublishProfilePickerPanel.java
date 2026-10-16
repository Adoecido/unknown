package vegas.sony.ui.click.frame.impl.profile;

import vegas.sony.SonyVegas;
import vegas.sony.config.Profile;
import vegas.sony.module.none.ClientSettings;
import vegas.sony.ui.click.GuiMouseEvent;
import vegas.sony.ui.click.component.IconTextActionRowComponent;
import vegas.sony.ui.click.component.PanelComponent;
import vegas.sony.ui.click.component.SimpleTextLabelComponent;
import vegas.sony.ui.click.component.SpacerComponent;
import vegas.sony.ui.click.component.WrappingTextLabelComponent;
import vegas.sony.ui.click.frame.impl.profile.PublicProfilesFrame;
import java.awt.Color;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PublicProfilePublishProfilePickerPanel
extends PanelComponent {
    private final PublicProfilesFrame profilesFrame;


    public PublicProfilePublishProfilePickerPanel(PublicProfilesFrame publicProfilesFrame) {
        super(108.0, 155.0);
        this.profilesFrame = publicProfilesFrame;
        this.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().M("wrap");
        this.setShowDisabledOverlay(true);
        this.I(true);
        this.setDisabledOverlayColor(PublicProfilePublishProfilePickerPanel.J.B);
        this.h(new SpacerComponent(0.0, 10.0), new Object[0]);
        WrappingTextLabelComponent wrappingTextLabelComponent = new WrappingTextLabelComponent("Create from...", 0.9, Color.WHITE);
        wrappingTextLabelComponent.setBold(true);
        wrappingTextLabelComponent.Y(12.0);
        wrappingTextLabelComponent.o(this.A());
        this.h(wrappingTextLabelComponent, new Object[0]);
        this.h(new SpacerComponent(0.0, 5.0), new Object[0]);
        IconTextActionRowComponent iconTextActionRowComponent = new IconTextActionRowComponent("Current settings");
        iconTextActionRowComponent.o(this.A());
        iconTextActionRowComponent.setClickListener(() -> {
            UUID uUID = SonyVegas.INSTANCE.getProfilesManager().getActiveProfile().getOnlineId();
            Profile profile = new Profile("Current settings", "4.21");
            profile.setDraft(true);
            profile.setOnlineId(uUID);
            profile.captureCurrentState();
            publicProfilesFrame.O(null);
            publicProfilesFrame.e(profile);
        });
        this.h(iconTextActionRowComponent, "wrap");
        this.h(new SpacerComponent(0.0, 5.0), new Object[0]);
        SimpleTextLabelComponent simpleTextLabelComponent = new SimpleTextLabelComponent("     PRIVATE PROFILES", 0.65, new Color(255, 255, 255, 153));
        simpleTextLabelComponent.setBold(true);
        simpleTextLabelComponent.Y(12.0);
        simpleTextLabelComponent.o(this.A());
        this.h(simpleTextLabelComponent, new Object[0]);
        this.h(new SpacerComponent(0.0, 2.0), new Object[0]);
        PanelComponent panelComponent = new PanelComponent(this.A(), 90.0);
        panelComponent.setShowDisabledOverlay(false);
        panelComponent.setDisabledOverlayColor(this.getDisabledOverlayColor());
        panelComponent.I(true);
        panelComponent.l$src$Lvegas_sony_ui_click_layout_ComponentLayout_$di1tij().M("wrap");
        panelComponent.t(panelComponent.L());
        this.h(panelComponent, "widthwrap");
        List<Profile> list = SonyVegas.INSTANCE.getPublicProfileManager().getDerivedProfiles();
        for (Profile profile : SonyVegas.INSTANCE.getProfilesManager().getProfiles()) {
            if (list.contains(profile)) continue;
            IconTextActionRowComponent iconTextActionRowComponent2 = new IconTextActionRowComponent(profile.getName());
            iconTextActionRowComponent2.o(this.A());
            iconTextActionRowComponent2.setClickListener(() -> {
                publicProfilesFrame.O(null);
                publicProfilesFrame.e(profile);
            });
            panelComponent.h(iconTextActionRowComponent2, "wrap");
        }
    }

    private void lambda$onMouseGlobal$2() {
        this.profilesFrame.O(null);
    }

    @Override
    public void U(GuiMouseEvent guiMouseEvent) {
        boolean bl = this.getBounds().J(guiMouseEvent.getX(), guiMouseEvent.getY());
        if (!bl) {
            CompletableFuture.runAsync(this::lambda$onMouseGlobal$2, ClientSettings.UI_EXECUTOR);
        }
    }

}

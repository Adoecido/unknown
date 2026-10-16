package vegas.sony.friend.ui;

import vegas.sony.SonyVegas;
import vegas.sony.event.EventHandler;
import vegas.sony.event.EventListener;
import vegas.sony.event.impl.EventMouseButton;
import vegas.sony.event.impl.EventPreAttack;
import vegas.sony.friend.OnlineFriendActivityState;
import vegas.sony.friend.PartyState;
import vegas.sony.friend.ui.OnlineActivitySettingsFrame;
import vegas.sony.input.MouseClickRateTracker;
import vegas.sony.mapping.MappedClasses;
import vegas.sony.module.none.ClientSettings;
import vegas.sony.value.BooleanValue;
import vegas.sony.wrapper.impl.Minecraft;
import java.util.ArrayList;
import java.util.List;

public class OnlineActivityPanelOptions
implements EventListener {
    public static final OnlineActivityPanelOptions INSTANCE = new OnlineActivityPanelOptions();
    private final BooleanValue cpsDisplay;
    private final BooleanValue renderBackground = BooleanValue.create(this, "Render Background //$ui", true);

    public BooleanValue getCpsDisplay() {
        return this.cpsDisplay;
    }

    @EventHandler
    public void onPreAttack(EventPreAttack event) {
        OnlineActivitySettingsFrame settingsFrame = ClientSettings.getFrame(OnlineActivitySettingsFrame.class);
        if (settingsFrame == null) {
            return;
        }
        if (Minecraft.currentScreen().isNotNull()) {
            return;
        }
        if (event.getTarget().isInstance(MappedClasses.zc)) {
            SonyVegas.INSTANCE.getOnlineManager().getLocalFriend().getActivityState().recordAttack(event.getTarget().S());
        }
    }


    public OnlineActivityPanelOptions() {
        this.cpsDisplay = BooleanValue.create(this, "CPS Display //$ui", true);
    }

    public BooleanValue getRenderBackground() {
        return this.renderBackground;
    }

    public List<OnlineFriendActivityState> getPartyActivities() {
        PartyState partyState = SonyVegas.INSTANCE.getOnlineManager().getPartyManager().getCurrentParty();
        if (partyState == null) {
            return new ArrayList<OnlineFriendActivityState>();
        }
        ArrayList<OnlineFriendActivityState> partyActivities = new ArrayList<OnlineFriendActivityState>();
        for (OnlineFriendActivityState activityState : SonyVegas.INSTANCE.getOnlineManager().getActivityManager().getActivityStates()) {
            if (!activityState.hasData() || !partyState.getMembers().contains(activityState.getFriend())) continue;
            partyActivities.add(activityState);
        }
        return partyActivities;
    }

    @EventHandler
    public void onMouseButton(EventMouseButton event) {
        OnlineActivitySettingsFrame settingsFrame = ClientSettings.getFrame(OnlineActivitySettingsFrame.class);
        if (settingsFrame == null) {
            return;
        }
        if (event.getButtonState()) {
            SonyVegas.INSTANCE.getOnlineManager().getLocalFriend().getActivityState().setAfkTicks(0);
            if (event.getButton() == 0 && Minecraft.currentScreen().isNull()) {
                MouseClickRateTracker.recordClick();
            }
        }
    }
}


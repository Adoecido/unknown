package vegas.sony.friend;

import vegas.sony.SonyVegas;
import vegas.sony.friend.GroupUserModel;
import vegas.sony.friend.OnlineFriend;
import vegas.sony.friend.PartyStateModel;
import vegas.sony.friend.UserModel;
import vegas.sony.friend.ui.OnlineFriendUiHelper;
import vegas.sony.friend.ui.PartyMemberRow;
import vegas.sony.friend.ui.PartyPanel;
import vegas.sony.protocol.packet.GroupOption;
import vegas.sony.value.BooleanValue;
import vegas.sony.value.Value;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

public class PartyState {
    private Map<GroupOption, Value<?, ?>> options;
    private PartyPanel partyPanel;
    private OnlineFriend leader;
    private final List<OnlineFriend> members = new ArrayList<OnlineFriend>();
    private final List<OnlineFriend> invitedUsers = new ArrayList<OnlineFriend>();
    private final List<PartyMemberRow> chatRows = new ArrayList<PartyMemberRow>();
    private final BooleanValue openInvites = BooleanValue.create(null, "Open Party //$ui", false);

    private static OnlineFriend lambda$handle$0(GroupUserModel groupUserModel) {
        return new OnlineFriend(groupUserModel);
    }

    public Map<GroupOption, Value<?, ?>> getOptions() {
        if (this.options == null) {
            this.options = new LinkedHashMap();
            this.options.put(GroupOption.OPEN_INVITES, this.openInvites);
            for (Map.Entry<GroupOption, Value<?, ?>> entry : this.options.entrySet()) {
                ((Value)entry.getValue()).setValue(entry.getKey().getDefaultValue());
            }
        }
        return this.options;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void addMember(OnlineFriend onlineFriend) {
        List<OnlineFriend> list = this.members;
        synchronized (list) {
            this.members.add(onlineFriend);
            if (this.partyPanel != null) {
                this.partyPanel.refreshMembers();
            }
        }
    }

    public PartyState(OnlineFriend onlineFriend) {
        this.leader = onlineFriend;
        this.leader.setGroupRole(0);
        this.members.add(onlineFriend);
    }

    public @UnmodifiableView List<PartyMemberRow> getChatRows() {
        return this.chatRows;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void removeInvitedUser(OnlineFriend onlineFriend) {
        List<OnlineFriend> list = this.invitedUsers;
        synchronized (list) {
            this.invitedUsers.remove(onlineFriend);
            if (this.partyPanel != null) {
                this.partyPanel.refreshMembers();
            }
        }
    }

    @Nullable
    public OnlineFriend findMemberById(long userId) {
        for (OnlineFriend onlineFriend : this.members) {
            if (onlineFriend.getUser().getId() != userId) continue;
            return onlineFriend;
        }
        return null;
    }

    public void setLeader(OnlineFriend onlineFriend) {
        this.leader = onlineFriend;
        this.members.remove(onlineFriend);
        OnlineFriend onlineFriend2 = this.members.set(0, onlineFriend);
        if (onlineFriend2 != null) {
            this.members.add(onlineFriend2);
        }
        if (this.partyPanel != null) {
            this.partyPanel.refreshMembers();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void removeInvitedUserSilently(OnlineFriend onlineFriend) {
        List<OnlineFriend> list = this.invitedUsers;
        synchronized (list) {
            this.invitedUsers.remove(onlineFriend);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void addChatRow(PartyMemberRow partyMemberRow) {
        List<PartyMemberRow> list = this.chatRows;
        synchronized (list) {
            this.chatRows.add(partyMemberRow);
        }
        OnlineFriendUiHelper.addPartyChatMessage(partyMemberRow);
    }


    public PartyState(PartyStateModel partyStateModel) {
        this.leader = this.resolveGroupUser(partyStateModel.getLeader());
        for (GroupUserModel groupUserModel : partyStateModel.getMembers()) {
            this.members.add(this.resolveGroupUser(groupUserModel));
        }
        for (GroupUserModel groupUserModel : partyStateModel.getInvitedUsers()) {
            this.invitedUsers.add(this.resolveGroupUser(groupUserModel));
        }
    }

    public BooleanValue getOpenInvites() {
        return this.openInvites;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void addInvitedUser(OnlineFriend onlineFriend) {
        List<OnlineFriend> list = this.invitedUsers;
        synchronized (list) {
            this.invitedUsers.add(onlineFriend);
            if (this.partyPanel != null) {
                this.partyPanel.refreshMembers();
            }
        }
    }

    @Nullable
    public OnlineFriend findMember(UserModel userModel) {
        return this.findMemberById(userModel.getId());
    }

    public @UnmodifiableView List<OnlineFriend> getInvitedUsers() {
        return this.invitedUsers;
    }

    public boolean canInvite() {
        if (this.openInvites.getEffectiveValue().booleanValue()) {
            return true;
        }
        return this.getLeader().equals(SonyVegas.INSTANCE.getOnlineManager().getLocalFriend());
    }

    private OnlineFriend resolveGroupUser(GroupUserModel groupUserModel) {
        OnlineFriend onlineFriend = SonyVegas.INSTANCE.getOnlineManager().getFriendCache().getOrCreateFriend(groupUserModel.getUserId(), () -> PartyState.lambda$handle$0(groupUserModel));
        onlineFriend.setGroupRole(groupUserModel.getGroupRole());
        return onlineFriend;
    }

    public void setPartyPanel(PartyPanel partyPanel) {
        this.partyPanel = partyPanel;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void removeMember(OnlineFriend onlineFriend) {
        List<OnlineFriend> list = this.members;
        synchronized (list) {
            this.members.remove(onlineFriend);
            if (this.partyPanel != null) {
                this.partyPanel.refreshMembers();
            }
        }
    }

    public OnlineFriend getLeader() {
        return this.leader;
    }

    public @UnmodifiableView List<OnlineFriend> getMembers() {
        return this.members;
    }
}

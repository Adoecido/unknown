package vegas.sony.friend;

import vegas.sony.SonyVegas;
import vegas.sony.friend.FriendRequest;
import vegas.sony.friend.IncomingFriendRequest;
import vegas.sony.friend.OnlineFriend;
import vegas.sony.friend.OutgoingFriendRequest;
import vegas.sony.friend.ui.FriendRequestListPanel;
import vegas.sony.friend.ui.OnlineFriendsFrame;
import vegas.sony.manager.client.OnlineConnectionManager;
import vegas.sony.module.none.ClientSettings;
import vegas.sony.notification.NotificationType;
import vegas.sony.protocol.ZeusConnectionManager;
import vegas.sony.protocol.packet.FriendRequestUpdateResponsePacket;
import vegas.sony.protocol.packet.FriendRequestUpdateStatus;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

public class FriendRequestManager {
    private final Map<OnlineFriend, IncomingFriendRequest> incomingRequests = new LinkedHashMap<OnlineFriend, IncomingFriendRequest>();
    private final Map<OnlineFriend, OutgoingFriendRequest> outgoingRequests = new LinkedHashMap<OnlineFriend, OutgoingFriendRequest>();

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void removeRequest(FriendRequest friendRequest) {
        if (friendRequest instanceof IncomingFriendRequest) {
            Map<OnlineFriend, IncomingFriendRequest> map = this.incomingRequests;
            synchronized (map) {
                this.incomingRequests.remove(friendRequest.getFriend());
                this.getRequestListPanel().removeRequest(friendRequest);
            }
        }
        if (friendRequest instanceof OutgoingFriendRequest) {
            Map<OnlineFriend, OutgoingFriendRequest> map = this.outgoingRequests;
            synchronized (map) {
                this.outgoingRequests.remove(friendRequest.getFriend());
                this.getRequestListPanel().removeRequest(friendRequest);
            }
        }
    }

    private void lambda$cancelOutgoingRequest$2(OutgoingFriendRequest outgoingFriendRequest, FriendRequestUpdateResponsePacket friendRequestUpdateResponsePacket) {
        if (friendRequestUpdateResponsePacket.getStatus() != FriendRequestUpdateStatus.DECLINED) {
            this.addRequest(outgoingFriendRequest);
        }
    }

    public void removeRequestsForFriend(OnlineFriend onlineFriend) {
        OutgoingFriendRequest outgoingFriendRequest;
        IncomingFriendRequest incomingFriendRequest = this.incomingRequests.get(onlineFriend);
        if (incomingFriendRequest != null) {
            this.removeRequest(incomingFriendRequest);
        }
        if ((outgoingFriendRequest = this.outgoingRequests.get(onlineFriend)) != null) {
            this.removeRequest(outgoingFriendRequest);
        }
    }

    public @UnmodifiableView Set<FriendRequest> getAllRequests() {
        LinkedHashSet<FriendRequest> linkedHashSet = new LinkedHashSet<FriendRequest>();
        linkedHashSet.addAll(this.incomingRequests.values());
        linkedHashSet.addAll(this.outgoingRequests.values());
        return linkedHashSet;
    }

    public boolean hasOutgoingRequest(OnlineFriend onlineFriend) {
        return this.outgoingRequests.containsKey(onlineFriend);
    }

    public void clear() {
        ArrayList<FriendRequest> requests = new ArrayList<FriendRequest>();
        requests.addAll(this.incomingRequests.values());
        requests.addAll(this.outgoingRequests.values());
        for (FriendRequest friendRequest : requests) {
            this.removeRequest(friendRequest);
        }
    }

    private FriendRequestListPanel getRequestListPanel() {
        return ClientSettings.getFrame(OnlineFriendsFrame.class).getFriendRequestsPanel().getRequestListPanel();
    }

    public void declineIncomingRequest(IncomingFriendRequest incomingFriendRequest) {
        this.removeRequest(incomingFriendRequest);
        ZeusConnectionManager.T().u().Y(incomingFriendRequest.getId(), false, response -> this.lambda$declineIncomingRequest$1(incomingFriendRequest, response));
    }


    public void removeRequestById(long requestId) {
        FriendRequest friendRequest = null;
        for (IncomingFriendRequest friendRequest2 : this.incomingRequests.values()) {
            if (friendRequest2.getId() != requestId) continue;
            friendRequest = friendRequest2;
            break;
        }
        if (friendRequest != null) {
            this.removeRequest(friendRequest);
        } else {
            for (OutgoingFriendRequest outgoingFriendRequest : this.outgoingRequests.values()) {
                if (outgoingFriendRequest.getId() != requestId) continue;
                friendRequest = outgoingFriendRequest;
                break;
            }
            if (friendRequest == null) {
                return;
            }
            this.removeRequest(friendRequest);
        }
    }

    public @UnmodifiableView Set<FriendRequest> getIncomingRequests() {
        return new LinkedHashSet<FriendRequest>(this.incomingRequests.values());
    }

    private void lambda$declineIncomingRequest$1(IncomingFriendRequest incomingFriendRequest, FriendRequestUpdateResponsePacket friendRequestUpdateResponsePacket) {
        if (friendRequestUpdateResponsePacket.getStatus() != FriendRequestUpdateStatus.DECLINED) {
            this.addRequest(incomingFriendRequest);
        }
    }

    public void cancelOutgoingRequest(OutgoingFriendRequest outgoingFriendRequest) {
        this.removeRequest(outgoingFriendRequest);
        ZeusConnectionManager.T().u().Y(outgoingFriendRequest.getId(), false, response -> this.lambda$cancelOutgoingRequest$2(outgoingFriendRequest, response));
    }

    public void acceptIncomingRequest(IncomingFriendRequest incomingFriendRequest) {
        this.removeRequest(incomingFriendRequest);
        ZeusConnectionManager.T().u().Y(incomingFriendRequest.getId(), true, response -> this.lambda$acceptIncomingRequest$0(incomingFriendRequest, response));
    }

    private void lambda$acceptIncomingRequest$0(IncomingFriendRequest incomingFriendRequest, FriendRequestUpdateResponsePacket friendRequestUpdateResponsePacket) {
        if (friendRequestUpdateResponsePacket.getStatus() != FriendRequestUpdateStatus.ACCEPTED) {
            this.addRequest(incomingFriendRequest);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void addRequest(FriendRequest friendRequest) {
        if (friendRequest instanceof IncomingFriendRequest) {
            Map<OnlineFriend, IncomingFriendRequest> map = this.incomingRequests;
            synchronized (map) {
                this.incomingRequests.put(friendRequest.getFriend(), (IncomingFriendRequest)friendRequest);
                this.getRequestListPanel().addRequest(friendRequest);
                if (OnlineConnectionManager.INSTANCE.getFriendRequestNotificationTimer().hasTimeElapsed(5000L)) {
                    SonyVegas.INSTANCE.getNotificationManager().show("Friend request", "Incoming friend request from " + friendRequest.getFriend().getDisplayName(), NotificationType.FRIENDS_NEW_REQUEST, 4000L);
                }
            }
        }
        if (friendRequest instanceof OutgoingFriendRequest) {
            Map<OnlineFriend, OutgoingFriendRequest> map = this.outgoingRequests;
            synchronized (map) {
                this.outgoingRequests.put(friendRequest.getFriend(), (OutgoingFriendRequest)friendRequest);
                this.getRequestListPanel().addRequest(friendRequest);
            }
        }
    }

    @Nullable
    public FriendRequest getRequestForFriend(OnlineFriend onlineFriend) {
        IncomingFriendRequest incomingFriendRequest = this.incomingRequests.get(onlineFriend);
        if (incomingFriendRequest != null) {
            return incomingFriendRequest;
        }
        OutgoingFriendRequest outgoingFriendRequest = this.outgoingRequests.get(onlineFriend);
        return outgoingFriendRequest;
    }
}


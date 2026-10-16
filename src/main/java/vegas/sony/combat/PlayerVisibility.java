package vegas.sony.combat;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.wrapper.impl.EntityLivingBase;
import vegas.sony.wrapper.impl.EntityPlayer;
import vegas.sony.wrapper.impl.GameProfile;
import vegas.sony.wrapper.impl.Minecraft;
import vegas.sony.wrapper.impl.PlayerInfo;

/**
 * Resolves whether a remote player can actually be interacted with locally.
 * A vanished player can remain in the world entity list after their tab-list
 * entry was removed, so {@code Entity#isInvisible()} alone is not sufficient.
 */
public final class PlayerVisibility {
    private PlayerVisibility() {
    }

    public static boolean isInteractable(EntityLivingBase candidate) {
        try {
            if (candidate == null || candidate.isNull()) {
                return false;
            }
            if (candidate.isDead() || candidate.isInvisible()) {
                return false;
            }
            // Non-player living entities have no tab-list identity to compare.
            if (!candidate.isInstance(MappedClasses.Yl)) {
                return true;
            }
            EntityPlayer player = new EntityPlayer(candidate.getObject());
            GameProfile profile = player.c$src$Lvegas_sony_wrapper_impl_GameProfile_$ir8937();
            if (profile == null || profile.isNull() || Minecraft.N().isNull()
                    || Minecraft.N().getPlayerInfoMap() == null
                    || Minecraft.N().getPlayerInfoMap().isEmpty()) {
                // Keep gameplay working if a mapping or the connection is not
                // available yet; the normal invisibility check above still runs.
                return true;
            }
            for (Object infoObject : Minecraft.N().getPlayerInfoMap()) {
                if (infoObject == null) {
                    continue;
                }
                PlayerInfo info = new PlayerInfo(infoObject);
                GameProfile listedProfile = info.v();
                if (listedProfile != null && listedProfile.isNotNull()
                        && (profile.equals(listedProfile)
                        || profile.getName().equalsIgnoreCase(listedProfile.getName()))) {
                    return true;
                }
            }
            // The entity is present in the world but not in the connection's
            // player list: treat it as vanished and do not acquire it.
            return false;
        }
        catch (Throwable unavailable) {
            return true;
        }
    }
}

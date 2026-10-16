package vegas.sony.config;

import vegas.sony.config.ProfileModuleSnapshot;
import vegas.sony.config.ProfileSnapshot;
import java.util.Comparator;

public class ProfileModuleSnapshotOrderComparator
implements Comparator<ProfileModuleSnapshot> {
    @Override
    public int compare(ProfileModuleSnapshot left, ProfileModuleSnapshot right) {
        return Integer.compare(right.getSortPriority(), left.getSortPriority());
    }
}

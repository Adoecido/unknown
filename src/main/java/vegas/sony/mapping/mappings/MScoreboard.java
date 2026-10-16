package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingMethod;
import vegas.sony.wrapper.Wrapper;
import vegas.sony.wrapper.impl.ForgeVersion;
import java.util.Collection;

public class MScoreboard
extends Mapping {
    public MappingMethod listPlayerScoresMethod;
    private static int[] scoreboardControlFlowState;
    public MappingMethod getPlayersTeamMethod;
    public MappingMethod getObjectiveInDisplaySlotMethod;
    public MappingMethod getValueFromObjectiveMethod;

    private Collection getPlayerScores(Object scoreboard, Object objective) {
        return (Collection)this.listPlayerScoresMethod.invokeObject(scoreboard, objective);
    }

    private Object getObjectiveInDisplaySlot(Object scoreboard, int slot) {
        return this.getObjectiveInDisplaySlotMethod.invokeObject(scoreboard, slot);
    }

    private Object getValueFromObjective(Object scoreboard, String playerName, Object objective) {
        return this.getValueFromObjectiveMethod.invokeObject(scoreboard, playerName, objective);
    }

    public static Object getObjectiveInDisplaySlot(MScoreboard mapping, Object scoreboard, int slot) {
        return mapping.getObjectiveInDisplaySlot(scoreboard, slot);
    }

    public static void setScoreboardControlFlowState(int[] state) {
        scoreboardControlFlowState = state;
    }

    public static Collection getPlayerScores(MScoreboard mapping, Object scoreboard, Object objective) {
        return mapping.getPlayerScores(scoreboard, objective);
    }

    public static Object getValueFromObjective(MScoreboard mapping, Object scoreboard, String playerName, Object objective) {
        return mapping.getValueFromObjective(scoreboard, playerName, objective);
    }


    public MScoreboard() {
        this(MScoreboard.getScoreboardControlFlowState());
    }

    private MScoreboard(int[] nArray) {
        super(MappedClasses.F6);
        int[] nArray2 = nArray;
        if (ForgeVersion.MC_1_7_10.L() && !Wrapper.sonyVegasInstance.isVanillaMinecraftPresent()) {
            Class[] classArray = new Class[]{MappedClasses.Y};
            Class<Collection> clazz = Collection.class;
            boolean bl = Wrapper.isNativeAvailable;
            String string = "func_96534_i";
            MScoreboard mScoreboard = this;
            this.listPlayerScoresMethod = mScoreboard.Y(string, bl, clazz, classArray);
        } else if (ForgeVersion.MC_1_20_6.d()) {
            Class[] classArray = new Class[]{MappedClasses.Y};
            Class<Collection> clazz = Collection.class;
            boolean bl = true;
            String string = "listPlayerScores";
            MScoreboard mScoreboard = this;
            this.listPlayerScoresMethod = mScoreboard.Y(string, bl, clazz, classArray);
        } else if (ForgeVersion.MC_1_17.d()) {
            Class[] classArray = new Class[]{MappedClasses.Y};
            Class<Collection> clazz = Collection.class;
            boolean bl = Wrapper.isNativeAvailable;
            String string = "m_83498_";
            MScoreboard mScoreboard = this;
            this.listPlayerScoresMethod = mScoreboard.Y(string, bl, clazz, classArray);
        } else if (ForgeVersion.MC_1_16_5.d()) {
            Class[] classArray = new Class[]{MappedClasses.Y};
            Class<Collection> clazz = Collection.class;
            boolean bl = Wrapper.isNativeAvailable;
            String string = "getSortedScores";
            MScoreboard mScoreboard = this;
            this.listPlayerScoresMethod = mScoreboard.Y(string, bl, clazz, classArray);
        } else {
            Class[] classArray = new Class[]{MappedClasses.Y};
            Class<Collection> clazz = Collection.class;
            boolean bl = true;
            String string = "getSortedScores";
            MScoreboard mScoreboard = this;
            this.listPlayerScoresMethod = mScoreboard.Y(string, bl, clazz, classArray);
        }
        Class[] classArray = new Class[]{String.class};
        Class clazz = MappedClasses.u6;
        boolean bl = true;
        String string = "getPlayersTeam";
        MScoreboard mScoreboard = this;
        this.getPlayersTeamMethod = mScoreboard.Y(string, bl, clazz, classArray);
        Class[] classArray2 = new Class[]{Integer.TYPE};
        Class clazz2 = MappedClasses.Y;
        boolean bl2 = true;
        String string2;
        if (ForgeVersion.MC_1_7_10.L() && !Wrapper.sonyVegasInstance.isVanillaMinecraftPresent()) {
            string2 = "func_96539_a";
        } else if (ForgeVersion.MC_1_20_6.d()) {
            string2 = "getDisplayObjective";
        } else if (ForgeVersion.MC_1_17.d()) {
            string2 = "m_83308_";
        } else if (ForgeVersion.MC_1_16_5.d()) {
            string2 = "getDisplayObjective";
        } else {
            string2 = "func_96539_a";
        }
        MScoreboard mScoreboard2 = this;
        this.getObjectiveInDisplaySlotMethod = mScoreboard2.Y(string2, bl2, clazz2, classArray2);
        
        // getValueFromObjective(String, ScoreObjective) -> Score
        Class[] classArray3 = new Class[]{String.class, MappedClasses.Y};
        Class clazz3 = MappedClasses.Yh;
        boolean bl3 = true;
        String string3;
        if (ForgeVersion.MC_1_7_10.L() && !Wrapper.sonyVegasInstance.isVanillaMinecraftPresent()) {
            string3 = "func_96538_b";
        } else if (ForgeVersion.MC_1_20_6.d()) {
            string3 = "getOrCreateScore";
        } else if (ForgeVersion.MC_1_17.d()) {
            string3 = "m_83497_";
        } else if (ForgeVersion.MC_1_16_5.d()) {
            string3 = "getOrCreateScore";
        } else {
            string3 = "getOrCreateScore";
        }
        MScoreboard mScoreboard3 = this;
        this.getValueFromObjectiveMethod = mScoreboard3.Y(string3, bl3, clazz3, classArray3);
    }

    private Object getPlayersTeam(Object scoreboard, String playerName) {
        return this.getPlayersTeamMethod.invokeObject(scoreboard, playerName);
    }

    public static int[] getScoreboardControlFlowState() {
        return scoreboardControlFlowState;
    }

    static {
        MScoreboard.setScoreboardControlFlowState(new int[2]);
    }

    public static Object getPlayersTeam(MScoreboard mapping, Object scoreboard, String playerName) {
        return mapping.getPlayersTeam(scoreboard, playerName);
    }
}

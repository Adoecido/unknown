package vegas.sony.mapping.mappings;

import vegas.sony.mapping.MappedClasses;
import vegas.sony.mapping.Mapping;
import vegas.sony.mapping.MappingField;
import vegas.sony.mapping.MappingMethod;
import vegas.sony.mapping.MappingMethodBuilder;
import vegas.sony.ui.click.component.GuiComponent;
import vegas.sony.wrapper.Wrapper;
import vegas.sony.wrapper.impl.ForgeVersion;

public class MScorePlayerTeam
extends Mapping {
    private MappingField prefixField;
    private MappingField suffixField;
    private MappingField registeredNameField;
    private MappingField colorField;
    private MappingMethod formatPlayerNameMethod;
    private MappingMethod formatNameForTeamMethod;
    private MappingMethod getChatFormatMethod;

    private String formatPlayerName(Object team, String playerName) {
        return (String)this.formatPlayerNameMethod.invokeObject(null, team, playerName);
    }

    public static Object formatNameForTeam(MScorePlayerTeam mapping, Object team, Object playerName) {
        return mapping.formatNameForTeam(team, playerName);
    }

    public Object getPlayerPrefix(Object team) {
        return this.prefixField.getObject(team);
    }

    public Object getPlayerSuffix(Object team) {
        return this.suffixField.getObject(team);
    }

    public Object getPlayerRegisteredName(Object team) {
        return this.registeredNameField.getObject(team);
    }

    public Object getPlayerColor(Object team) {
        return this.colorField.getObject(team);
    }

    public static Object getColor(MScorePlayerTeam mapping, Object team) {
        return mapping.getPlayerColor(team);
    }

    public static String getPrefix(MScorePlayerTeam mapping, Object team) {
        return (String) mapping.getPlayerPrefix(team);
    }

    public static String getSuffix(MScorePlayerTeam mapping, Object team) {
        return (String) mapping.getPlayerSuffix(team);
    }

    public static String getRegisteredName(MScorePlayerTeam mapping, Object team) {
        return (String) mapping.getPlayerRegisteredName(team);
    }

    public static Object getChatFormat(MScorePlayerTeam mapping, Object team) {
        return mapping.getChatFormat(team);
    }

    private Object getChatFormat(Object team) {
        return this.getChatFormatMethod.invokeObject(team);
    }

    public static String formatPlayerName(MScorePlayerTeam mapping, Object team, String playerName) {
        return mapping.formatPlayerName(team, playerName);
    }

    public MScorePlayerTeam() {
        this(MScoreboard.getScoreboardControlFlowState());
    }

    private MScorePlayerTeam(int[] nArray) {
        super(MappedClasses.u6);
        if (nArray != null) {
            if (ForgeVersion.MC_1_16_5.d()) {
                Class[] classArray = new Class[]{MappedClasses.Yh, MappedClasses.Yr};
                Class clazz = MappedClasses.YO;
                boolean bl = Wrapper.isNativeAvailable;
                String string = "func_237500_a_";
                MScorePlayerTeam mScorePlayerTeam = this;
                this.formatPlayerNameMethod = mScorePlayerTeam.registerStaticMethod(string, bl, clazz, classArray);
            } else {
                Class[] classArray = new Class[]{MappedClasses.Yh, String.class};
                Class<String> clazz = String.class;
                boolean bl = true;
                String string = "formatPlayerName";
                MScorePlayerTeam mScorePlayerTeam = this;
                this.formatPlayerNameMethod = mScorePlayerTeam.registerStaticMethod(string, bl, clazz, classArray);
            }
            if (ForgeVersion.MC_1_20_6.d()) {
                Class clazz = MappedClasses.Yr;
                boolean bl = true;
                String string = "playerPrefix";
                MScorePlayerTeam mScorePlayerTeam = this;
                this.prefixField = mScorePlayerTeam.J(string, bl, clazz);
                Class clazz2 = MappedClasses.l5;
                boolean bl2 = true;
                String string2 = "color";
                MScorePlayerTeam mScorePlayerTeam2 = this;
                this.colorField = this.J(string2, bl2, clazz2);
            } else {
                Class<String> clazz = String.class;
                boolean bl = true;
                String string = ForgeVersion.c() >= 23 ? "prefix" : "namePrefixSPT";
                MScorePlayerTeam mScorePlayerTeam = this;
                this.prefixField = mScorePlayerTeam.J(string, bl, clazz);
                boolean cheatBreaker = ForgeVersion.MC_1_7_10.L() && !Wrapper.sonyVegasInstance.isVanillaMinecraftPresent();
                String string2 = ForgeVersion.c() >= 23 ? "suffix" : cheatBreaker ? "colorSuffix" : "nameSuffixSPT";
                this.suffixField = mScorePlayerTeam.J(string2, bl, clazz);
                String string3 = ForgeVersion.c() >= 23 || cheatBreaker ? "registeredName" : "registeredNameSPT";
                this.registeredNameField = mScorePlayerTeam.J(string3, bl, clazz);
            }
            if (ForgeVersion.MC_1_16_5.d()) {
                Class[] classArray = new Class[]{MappedClasses.Yh, MappedClasses.Yr};
                Class clazz = MappedClasses.YO;
                String string = "formatNameForTeam";
                MScorePlayerTeam mScorePlayerTeam = this;
                this.formatNameForTeamMethod = ((MappingMethodBuilder)((MappingMethodBuilder)mScorePlayerTeam.methodBuilder(string, clazz, classArray).setTypeForVersion(ForgeVersion.MC_1_20_6.n(), MappedClasses.uM)).setStaticMember(true)).buildMethod();
            }
            if (ForgeVersion.MC_1_16_5.d()) {
                String string = "getChatFormat";
                Class clazz = MappedClasses.YO;
                MScorePlayerTeam mScorePlayerTeam = this;
                this.getChatFormatMethod = mScorePlayerTeam.Y(string, true, clazz, new Class[0]);
            }
            if (GuiComponent.getLegacyComponentState() == null) {
                MScoreboard.setScoreboardControlFlowState(new int[4]);
            }
            return;
        }
        Class[] classArray = new Class[]{MappedClasses.Yh, String.class};
        Class<String> clazz = String.class;
        boolean bl = true;
        String string = "formatPlayerName";
        MScorePlayerTeam mScorePlayerTeam = this;
        this.formatPlayerNameMethod = mScorePlayerTeam.registerStaticMethod(string, bl, clazz, classArray);
        if (ForgeVersion.MC_1_20_6.d()) {
            Class clazz3 = MappedClasses.Yr;
            boolean bl3 = true;
            String string3 = "playerPrefix";
            MScorePlayerTeam mScorePlayerTeam3 = this;
            this.prefixField = this.J(string3, bl3, clazz3);
            Class clazz4 = MappedClasses.l5;
            boolean bl4 = true;
            String string4 = "color";
            MScorePlayerTeam mScorePlayerTeam4 = this;
            this.colorField = this.J(string4, bl4, clazz4);
        }
        Class<String> clazz5 = String.class;
        boolean bl5 = true;
        String string5 = ForgeVersion.c() >= 23 ? "prefix" : "namePrefixSPT";
        MScorePlayerTeam mScorePlayerTeam5 = this;
        this.prefixField = this.J(string5, bl5, clazz5);
        boolean cheatBreaker = ForgeVersion.MC_1_7_10.L() && !Wrapper.sonyVegasInstance.isVanillaMinecraftPresent();
        String string6 = ForgeVersion.c() >= 23 ? "suffix" : cheatBreaker ? "colorSuffix" : "nameSuffixSPT";
        this.suffixField = this.J(string6, bl5, clazz5);
        String string7 = ForgeVersion.c() >= 23 || cheatBreaker ? "registeredName" : "registeredNameSPT";
        this.registeredNameField = this.J(string7, bl5, clazz5);
        if (ForgeVersion.MC_1_16_5.d()) {
            Class[] classArray2 = new Class[]{MappedClasses.Yh, MappedClasses.Yr};
            Class clazz6 = MappedClasses.YO;
            String string8 = "formatNameForTeam";
            MScorePlayerTeam mScorePlayerTeam6 = this;
            this.formatNameForTeamMethod = ((MappingMethodBuilder)((MappingMethodBuilder)this.methodBuilder(string8, clazz6, classArray2).setTypeForVersion(ForgeVersion.MC_1_20_6.n(), MappedClasses.uM)).setStaticMember(true)).buildMethod();
            String string9 = "getChatFormat";
            Class clazz7 = MappedClasses.YO;
            MScorePlayerTeam mScorePlayerTeam7 = this;
            this.getChatFormatMethod = mScorePlayerTeam7.Y(string9, true, clazz7, new Class[0]);
        }
        if (GuiComponent.getLegacyComponentState() == null) {
            MScoreboard.setScoreboardControlFlowState(new int[4]);
        }
    }

    private Object formatNameForTeam(Object team, Object playerName) {
        return this.formatNameForTeamMethod.invokeObject(null, team, playerName);
    }

    public Object formatPlayerNameComponent(Object team, Object playerName) {
        return this.formatPlayerNameMethod.invokeObject(null, team, playerName);
    }
}


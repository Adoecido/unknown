package vegas.sony.module.gladiador;

/** Regression checks for clan-decorated names used by Browse All Players. */
public final class TargetNameNormalizationProbe {
    private TargetNameNormalizationProbe() {
    }

    public static void main(String[] args) {
        require("PlayerOne".equals(Target.cleanPlayerName("\u00a7a[TAG] \u00a7fPlayerOne")),
                "prefix clan tag was kept");
        require("PlayerTwo".equals(Target.cleanPlayerName("\u00a7fPlayerTwo \u00a77[TAG]")),
                "suffix clan tag was kept");
        require("PlayerThree".equals(Target.cleanPlayerName("PlayerThree")),
                "plain player name changed");
        require("PlayerFour".equals(Target.cleanPlayerName("[TAG]PlayerFour[ALT]")),
                "multiple adjacent tags were kept");
        System.out.println("Target browse names: formatting and clan tags removed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

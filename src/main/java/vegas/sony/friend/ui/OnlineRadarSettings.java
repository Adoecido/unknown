package vegas.sony.friend.ui;

import vegas.sony.unmap.ModeOption;
import vegas.sony.unmap.ModeSelection;
import vegas.sony.value.BooleanValue;
import vegas.sony.value.ColorValue;
import vegas.sony.value.ModeValue;
import vegas.sony.value.NumberValue;
import java.awt.Color;

public class OnlineRadarSettings {
    public final ModeOption customColorMode;
    public final ColorValue enemyColor;
    public final ModeValue colorMode;
    public final ModeValue radarMode;
    public final NumberValue dotSize;
    public final ModeOption twoDimensionalRadarMode = new ModeOption("2D Radar //$ui");
    public final BooleanValue clampRadar;
    public final ColorValue friendlyColor;
    public final ModeOption circleRadarStyle;
    public final NumberValue radarSize;
    public final BooleanValue showBackground;
    public final ModeOption relationshipColorMode;
    public final NumberValue radarScale;
    public final ModeValue radarStyle;
    public final ModeOption teamColorMode;
    public final ModeOption squareDotStyle;
    public final NumberValue maxDistance;
    public final ModeValue dotStyle;
    public final NumberValue maxShown;
    public final ColorValue customColor;
    public final ModeOption textRadarMode = new ModeOption("Text Radar //$ui");
    public final BooleanValue showCross;
    public final ModeOption squareRadarStyle;
    public final ModeOption circleDotStyle;

    public OnlineRadarSettings() {
        this.radarMode = ModeValue.create((Object)this, "Mode //$ui", this.twoDimensionalRadarMode, this.twoDimensionalRadarMode, this.textRadarMode);
        this.radarSize = NumberValue.createWithDescription(this, "Radar Size //$ui", "#.#", "", 25.0, 110.0, 500.0, "The size of the radar. //$");
        this.dotSize = NumberValue.create(this, "Dot Size //$ui", "#.#", "", 0.5, 3.0, 10.0, 0.1, "The size of the radar. //$");
        this.radarScale = NumberValue.create(this, "Radar Scale //$ui", "#.##", "", 0.1, 0.5, 5.0, 0.01, "The size of the radar. //$");
        this.maxDistance = NumberValue.create(this, "Max Distance //$ui", "#", "m", 0.0, 0.0, 100.0, 5.0, "Maximum distance to show.\nUse 0 to ignore distance requirement. //$");
        this.maxShown = NumberValue.create(this, "Max Shown //$ui", "#", "", 0.0, 25.0, 50.0, 1.0, "Maximum players to show at once.\nUse 0 to show all players. //$");
        this.teamColorMode = new ModeOption("Team //$ui");
        this.customColorMode = new ModeOption("Custom //$ui");
        this.relationshipColorMode = new ModeOption("Relationship //$ui");
        this.colorMode = ModeValue.create((Object)this, "ColorMode1 //$ui", "Color Mode //$", "", (ModeSelection)this.teamColorMode, this.teamColorMode, this.customColorMode, this.relationshipColorMode);
        this.circleDotStyle = new ModeOption("Circles //$ui");
        this.squareDotStyle = new ModeOption("Squares //$ui");
        this.dotStyle = ModeValue.create((Object)this, "DotStyle1 //$ui", "Dot Style //$", "", (ModeSelection)this.circleDotStyle, this.circleDotStyle, this.squareDotStyle);
        this.squareRadarStyle = new ModeOption("Square //$ui");
        this.circleRadarStyle = new ModeOption("Circle //$ui");
        this.radarStyle = ModeValue.create((Object)this, "Radar Style //$ui", this.squareRadarStyle, this.squareRadarStyle, this.circleRadarStyle);
        this.customColor = ColorValue.create(this, "Custom Color //$ui", Color.WHITE);
        this.friendlyColor = ColorValue.create(this, "Friendly Color //$ui", Color.GREEN);
        this.enemyColor = ColorValue.create(this, "Enemy Color //$ui", Color.RED);
        this.showCross = BooleanValue.create(this, "Show Cross //$ui", true, "Render a center cross on the radar. //$");
        this.showBackground = BooleanValue.create(this, "Show Background //$ui", true, "Render a background behind the radar. //$");
        this.clampRadar = BooleanValue.create(this, "Clamp Radar //$ui", true, "Clamps dots to the given size and shape. //$");
        this.radarMode.addModeDependentValues(this.twoDimensionalRadarMode, this.radarSize, this.radarScale, this.colorMode, this.radarStyle, this.dotStyle, this.dotSize, this.showCross, this.clampRadar);
        this.radarMode.addModeDependentValues(this.textRadarMode, this.maxDistance, this.maxShown);
        this.colorMode.addModeDependentValues(this.customColorMode, this.customColor);
        this.colorMode.addModeDependentValues(this.relationshipColorMode, this.friendlyColor, this.enemyColor);
    }
}

package vegas.sony.module.world.cheststeal;

import vegas.sony.SonyVegas;
import vegas.sony.mapping.mappings.MTextComponentTranslation;
import vegas.sony.unmap.TextComponentBase;
import vegas.sony.wrapper.impl.ForgeVersion;
import vegas.sony.wrapper.impl.TextComponent;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public class ChestStealInventoryState
extends TextComponentBase {
    private static final String UNSUPPORTED_MESSAGE = "This method is available on 1.20.6 and later.";

    public static ChestStealInventoryState createTranslation(String translationKey, Object ... formatArguments) {
        return new ChestStealInventoryState(MTextComponentTranslation.B(ChestStealInventoryState.sonyVegasInstance.getMappings().D_, translationKey, formatArguments));
    }

    public String getTranslationKey() {
        if (ForgeVersion.MC_1_16_5.v()) {
            SonyVegas.notifyNativeStackTrace();
        }
        return MTextComponentTranslation.C(ChestStealInventoryState.sonyVegasInstance.getMappings().D_, this.getObject());
    }

    public String getFallback() {
        if (ForgeVersion.MC_1_20_6.v()) {
            SonyVegas.notifyNativeStackTrace();
        }
        return MTextComponentTranslation.a(ChestStealInventoryState.sonyVegasInstance.getMappings().D_, this.getObject());
    }

    public static ChestStealInventoryState createTranslationWithFallback(String translationKey, @Nullable String fallback, Object[] formatArguments) {
        if (ForgeVersion.MC_1_20_6.v()) {
            SonyVegas.notifyNativeStackTrace();
            throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
        }
        return new ChestStealInventoryState(MTextComponentTranslation.k(ChestStealInventoryState.sonyVegasInstance.getMappings().D_, translationKey, fallback, formatArguments));
    }

    @Override
    public String getFormattedText() {
        if (ForgeVersion.MC_1_20_6.d()) {
            TextComponent textComponent = TextComponent.p(this.I);
            return textComponent.getFormattedText();
        }
        return super.getFormattedText();
    }

    public Object[] getFormatArguments() {
        if (ForgeVersion.MC_1_16_5.v()) {
            SonyVegas.notifyNativeStackTrace();
        }
        return MTextComponentTranslation.x(ChestStealInventoryState.sonyVegasInstance.getMappings().D_, this.getObject());
    }

    public ChestStealInventoryState(Object component) {
        super(component);
    }

    public List<?> getSiblings() {
        return ChestStealInventoryState.sonyVegasInstance.getMappings().D_.k(this.getObject());
    }
}


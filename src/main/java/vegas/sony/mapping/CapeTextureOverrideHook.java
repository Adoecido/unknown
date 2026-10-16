package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.module.render.CapeManager;

public final class CapeTextureOverrideHook {
    private CapeTextureOverrideHook() {
    }

    public static Object override(Object playerHandle, Object originalLocation) {
        CapeRenderHook.markCustomCape(false);
        try {
            CapeManager manager = SonyVegas.INSTANCE.getModManager().getMod(CapeManager.class);
            Object result = manager == null
                    ? originalLocation : manager.overrideCape(playerHandle, originalLocation);
            // The host may cache our ResourceLocation and return it as the
            // original value on later frames. Identity comparison would then
            // disable the explicit renderer after its first successful frame.
            CapeRenderHook.markCustomCape(manager != null
                    && manager.isManagedCapeLocation(result));
            return result;
        } catch (Throwable error) {
            CapeRenderHook.markCustomCape(false);
            return originalLocation;
        }
    }

    public static boolean forceCapeVisible(Object playerHandle, Object modelPart, boolean original) {
        try {
            if (!(modelPart instanceof Enum)) {
                return original;
            }
            Enum<?> part = (Enum<?>) modelPart;
            // Vanilla puts CAPE at ordinal zero. Badlion's transformed enum
            // may use a different order, so also recognize the stable enum
            // constant name and keep the ordinal check as a compatibility
            // fallback for obfuscated constants.
            String partName = part.name();
            boolean capePart = "CAPE".equalsIgnoreCase(partName)
                    || partName.toLowerCase(java.util.Locale.ROOT).contains("cape")
                    || part.ordinal() == 0;
            if (!capePart) return original;
            CapeManager manager = SonyVegas.INSTANCE.getModManager().getMod(CapeManager.class);
            boolean forced = manager != null && manager.shouldForceCapeVisible(playerHandle);
            return forced || original;
        } catch (Throwable error) {
            return original;
        }
    }
}

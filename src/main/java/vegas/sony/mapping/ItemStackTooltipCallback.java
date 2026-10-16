package vegas.sony.mapping;

import vegas.sony.SonyVegas;
import vegas.sony.wrapper.impl.*;
import java.util.ArrayList;
import java.util.List;

/** Decorates the completed vanilla tooltip without recursively invoking getTooltip. */
public final class ItemStackTooltipCallback {
    public static List<?> call(Object stack, List<?> tooltip) {
        if (stack == null || tooltip == null || SonyVegas.INSTANCE == null
                || SonyVegas.INSTANCE.getClientSettings() == null
                || !SonyVegas.INSTANCE.getClientSettings().showNbtTags.getEffectiveValue()) return tooltip;
        try {
            ItemStack itemStack = new ItemStack(stack);
            if (itemStack.l() == null) return tooltip;
            ArrayList<String> arrayList = new ArrayList<String>();
            for (String string : new TagCompound(itemStack.l()).getKeySet()) {
                if (string.equals("display") || string.equals("Unbreakable") || string.equals("HideFlags") || string.equals("overrideMeta") || string.equals("AttributeModifiers")) continue;
                TagBase tagBase = new TagCompound(itemStack.l()).getTag(string);
                arrayList.add("");
                if (tagBase.isInstance(MappedClasses.Yg)) {
                    TagCompound tagCompound = new TagCompound(tagBase);
                    arrayList.add("\u00a79" + string);
                    for (String string2 : tagCompound.getKeySet()) {
                        TagBase tagBase2 = tagCompound.getTag(string2);
                        if (tagBase2.isInstance(MappedClasses.Yg) || tagBase2.isInstance(MappedClasses.qt)) continue;
                        arrayList.add(string2 + ": " + tagBase2.getObject().toString());
                    }
                    continue;
                }
                arrayList.add(string + ": " + tagBase.getObject().toString());
            }
            if (arrayList.isEmpty()) return tooltip;
            List<Object> result = new ArrayList<Object>(tooltip);
            for (String line : arrayList) {
                result.add(ForgeVersion.MC_1_16_5.d() ? TextComponentString.create(line).getObject() : line);
            }
            return result;
        } catch (RuntimeException ignored) {
            // Optional NBT decoration must preserve the vanilla tooltip on failure.
            return tooltip;
        }
    }
}

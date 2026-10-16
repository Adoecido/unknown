package vegas.sony.ui.font;

import java.util.Arrays;
import java.util.List;

public final class FormattedTextWrappingProbe {
    private static String visible(String text) { return text.replaceAll("\u00a7.", ""); }
    private static double width(String text) {
        String visible = visible(text);
        return visible.codePoints().mapToDouble(c -> c == 'M' ? 9.0 : c == 'i' ? 2.0 : 6.0).sum();
    }
    private static void check(String text, double limit) {
        List<String> lines = FormattedTextWrapping.wrap(Arrays.asList(text), limit, FormattedTextWrappingProbe::width);
        String actual = visible(String.join("", lines));
        if (!actual.equals(visible(text))) throw new AssertionError("Lost characters: " + text + " -> " + actual);
        if (lines.size() > text.length() + 1) throw new AssertionError("Wrapping did not progress");
        for (String line : lines) {
            if (line.endsWith("\u00a7")) throw new AssertionError("Split a color marker");
            if (limit > 0 && width(line) > limit && visible(line).codePointCount(0, visible(line).length()) > 1) {
                throw new AssertionError("Line too wide: " + line);
            }
        }
    }
    public static void main(String[] args) {
        for (double limit : new double[]{0, 1, 8, 12, 20, 35, 100}) {
            for (String text : new String[]{"Module", "module", "MODULE", "All modules disabled", "before verylongmoduleO after",
                    "M\u00a7ro\u00a7rd\u00a7ru\u00a7rl\u00a7re", "\u00a7aModule enabled", "\u00a7lWiWiModuleO",
                    "m\u00f3dulo \ud83d\ude03", "", "   "}) check(text, limit);
        }
        List<String> blank = FormattedTextWrapping.wrap(Arrays.asList("first", "", "last"), 100, FormattedTextWrappingProbe::width);
        if (!blank.equals(Arrays.asList("first", "", "last"))) throw new AssertionError("Newlines changed");
        System.out.println("PASS: Module/O preservation, narrow boxes, proportional widths, formatting, Unicode and blank lines");
    }
}

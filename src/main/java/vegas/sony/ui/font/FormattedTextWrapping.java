package vegas.sony.ui.font;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;

/** Pixel-width wrapping that never splits a formatting pair or a Unicode code point. */
public final class FormattedTextWrapping {
    private FormattedTextWrapping() { }

    public static List<String> wrap(List<String> source, double width, ToDoubleFunction<String> measure) {
        List<String> result = new ArrayList<String>();
        if (!(width > 0.0) || !Double.isFinite(width)) {
            result.addAll(source);
            return result;
        }
        for (String original : source) {
            String remaining = original;
            while (!remaining.isEmpty() && measure.applyAsDouble(remaining) > width) {
                int end = 0;
                int wordBreak = -1;
                boolean visible = false;
                for (int index = 0; index < remaining.length();) {
                    int next = nextUnit(remaining, index);
                    boolean formatting = remaining.charAt(index) == '\u00a7' && next == index + 2;
                    if (!formatting && visible && measure.applyAsDouble(remaining.substring(0, next)) > width) break;
                    end = next;
                    if (!formatting) {
                        visible = true;
                        if (Character.isWhitespace(remaining.codePointAt(index))) wordBreak = next;
                    }
                    index = next;
                }
                // Always make progress, even when a single glyph is wider than the box.
                if (end == 0 || end == remaining.length()) break;
                if (wordBreak > 0) end = wordBreak;
                String line = remaining.substring(0, end);
                result.add(line);
                remaining = activeFormatting(line) + remaining.substring(end);
            }
            result.add(remaining);
        }
        return result;
    }

    private static int nextUnit(String text, int index) {
        if (text.charAt(index) == '\u00a7' && index + 1 < text.length()) return index + 2;
        return index + Character.charCount(text.codePointAt(index));
    }

    private static String activeFormatting(String text) {
        StringBuilder active = new StringBuilder();
        for (int index = 0; index + 1 < text.length(); ++index) {
            if (text.charAt(index) != '\u00a7') continue;
            char code = Character.toLowerCase(text.charAt(++index));
            if ("0123456789abcdefr".indexOf(code) >= 0) active.setLength(0);
            if (code != 'r') active.append('\u00a7').append(code);
        }
        return active.toString();
    }
}

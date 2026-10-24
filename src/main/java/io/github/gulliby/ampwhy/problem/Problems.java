package io.github.gulliby.ampwhy.problem;

import io.github.gulliby.ampwhy.base.Text;

/** Formats the checking diagnostics for character-reference problems. */
public final class Problems {
    private Problems() {
    }

    public static String message(String code, String source, int[] result, boolean mapped) {
        return switch (code) {
            case "AW001", "AW004" -> "missing semicolon after \"" + source + "\"";
            case "AW002" -> "unknown name \"" + source + ";\"";
            case "AW003" -> "no digits in \"" + source + "\"";
            case "AW005" -> "\"" + source + "\" is the null character, read as " + Text.hex(result[0]);
            case "AW006" -> "\"" + source + "\" is above U+10FFFF, read as " + Text.hex(result[0]);
            case "AW007" -> "\"" + source + "\" is a surrogate, read as " + Text.hex(result[0]);
            case "AW008" -> "\"" + source + "\" is a noncharacter, kept as " + Text.hex(result[0]);
            case "AW009" -> "\"" + source + "\" is a control character, "
                    + (mapped ? "read as " : "kept as ") + Text.hex(result[0]);
            default -> throw new IllegalArgumentException("unknown problem code: " + code);
        };
    }
}

package io.github.gulliby.ampwhy.base;

public final class Text {
    private Text() {
    }

    public static String show(String text) {
        return "\"" + plain(text) + "\"";
    }

    public static String plain(String text) {
        StringBuilder result = new StringBuilder();
        for (int cp : cps(text)) {
            if (cp >= 0x20 && cp <= 0x7E) {
                result.append((char) cp);
            } else {
                result.append('?');
            }
        }
        return result.toString();
    }

    public static String plural(int n, String word) {
        return n + " " + word + (n == 1 ? "" : "s");
    }

    public static String hex(int cp) {
        String digits = Integer.toHexString(cp);
        StringBuilder result = new StringBuilder("U+");
        for (int i = digits.length(); i < 4; i++) {
            result.append('0');
        }
        for (int i = 0; i < digits.length(); i++) {
            char digit = digits.charAt(i);
            result.append(digit >= 'a' && digit <= 'f' ? (char) (digit - ('a' - 'A')) : digit);
        }
        return result.toString();
    }

    public static boolean isDigit(int cp) {
        return cp >= '0' && cp <= '9';
    }

    public static boolean isHex(int cp) {
        return isDigit(cp) || cp >= 'A' && cp <= 'F' || cp >= 'a' && cp <= 'f';
    }

    public static boolean isAlnum(int cp) {
        return isDigit(cp) || cp >= 'A' && cp <= 'Z' || cp >= 'a' && cp <= 'z';
    }

    public static int[] cps(String text) {
        return text.codePoints().toArray();
    }

    public static String str(int[] cps, int from, int to) {
        return new String(cps, from, to - from);
    }
}

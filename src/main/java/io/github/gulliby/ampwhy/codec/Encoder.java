package io.github.gulliby.ampwhy.codec;

import io.github.gulliby.ampwhy.base.Text;

public final class Encoder {
    private Encoder() {
    }

    public static String encode(int[] text, String mode, boolean attr) {
        boolean named = "named".equals(mode);
        boolean numeric = "numeric".equals(mode);
        StringBuilder result = new StringBuilder();
        for (int cp : text) {
            if (cp == '&') {
                result.append("&amp;");
            } else if (cp == '<') {
                result.append("&lt;");
            } else if (cp == '>') {
                result.append("&gt;");
            } else if (cp == '"' && attr) {
                result.append("&quot;");
            } else if (named || numeric) {
                String name = Namer.nameFor(cp);
                if (name != null) {
                    result.append('&').append(name).append(';');
                } else if (numeric && (cp < 0x20 && cp != '\t' && cp != '\n' || cp > 0x7E)) {
                    result.append("&#x").append(hex(cp)).append(';');
                } else {
                    result.append(Text.str(new int[]{cp}, 0, 1));
                }
            } else {
                result.append(Text.str(new int[]{cp}, 0, 1));
            }
        }
        return result.toString();
    }

    private static String hex(int cp) {
        String digits = Integer.toHexString(cp);
        StringBuilder result = new StringBuilder(digits.length());
        for (int i = 0; i < digits.length(); i++) {
            char digit = digits.charAt(i);
            result.append(digit >= 'a' && digit <= 'f' ? (char) (digit - ('a' - 'A')) : digit);
        }
        return result.toString();
    }
}

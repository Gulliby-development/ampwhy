package io.github.gulliby.ampwhy.codec;

import io.github.gulliby.ampwhy.base.Text;
import io.github.gulliby.ampwhy.scan.Occurrence;
import io.github.gulliby.ampwhy.scan.Scanner;

public final class Decoder {
    private Decoder() {
    }

    public static String decode(int[] text, boolean attr) {
        StringBuilder decoded = new StringBuilder();
        int cursor = 0;
        for (Occurrence occurrence : Scanner.scan(text, attr)) {
            int start = occurrence.start();
            var reading = occurrence.reading();
            decoded.append(Text.str(text, cursor, start));

            int end = reading.end();
            int[] result = reading.result();
            decoded.append(result == null
                    ? Text.str(text, start, end)
                    : Text.str(result, 0, result.length));
            cursor = end;
        }
        decoded.append(Text.str(text, cursor, text.length));
        return decoded.toString();
    }
}

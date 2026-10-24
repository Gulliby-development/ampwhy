package io.github.gulliby.ampwhy.read;

import io.github.gulliby.ampwhy.base.Text;
import io.github.gulliby.ampwhy.data.Entities;
import io.github.gulliby.ampwhy.data.Entities.Entity;
import io.github.gulliby.ampwhy.value.Value;

import java.util.ArrayList;
import java.util.List;

/** Makes the authoritative character-reference recognition and consumption decisions. */
public final class Reader {
    private static final int MAX_CODE_POINT = 0x10FFFF;
    private static final int SATURATED_VALUE = 0x110000;

    private Reader() {
    }

    public static Reading read(int[] text, int at, boolean attr) {
        int length = text.length;
        int afterAmpersand = at + 1;
        if (afterAmpersand >= length) {
            return null;
        }

        int first = text[afterAmpersand];
        if (first == '#') {
            int cursor = afterAmpersand + 1;
            int radix = 10;
            String kind = "dec";
            if (cursor < length && (text[cursor] == 'x' || text[cursor] == 'X')) {
                radix = 16;
                kind = "hex";
                cursor++;
            }

            int digitStart = cursor;
            int value = 0;
            while (cursor < length) {
                int cp = text[cursor];
                int digit = -1;
                if (Text.isDigit(cp)) {
                    digit = cp - '0';
                } else if (radix == 16 && cp >= 'A' && cp <= 'F') {
                    digit = cp - 'A' + 10;
                } else if (radix == 16 && cp >= 'a' && cp <= 'f') {
                    digit = cp - 'a' + 10;
                }
                if (digit < 0) {
                    break;
                }
                if (value < SATURATED_VALUE) {
                    if (value > (MAX_CODE_POINT - digit) / radix) {
                        value = SATURATED_VALUE;
                    } else {
                        value = value * radix + digit;
                    }
                }
                cursor++;
            }

            if (cursor == digitStart) {
                return new Reading("none", cursor, null, List.of("AW003"), false);
            }

            boolean semicolon = cursor < length && text[cursor] == ';';
            if (semicolon) {
                cursor++;
            }

            List<String> codes = new ArrayList<>(2);
            if (!semicolon) {
                codes.add("AW004");
            }
            Value.Result interpreted = Value.of(value);
            if (interpreted.code() != null) {
                codes.add(interpreted.code());
            }
            return new Reading(kind, cursor, interpreted.cps(), codes, interpreted.mapped());
        }

        if (!Text.isAlnum(first)) {
            return null;
        }

        int nameStart = afterAmpersand;
        int runEnd = nameStart;
        while (runEnd < length && Text.isAlnum(text[runEnd])) {
            runEnd++;
        }

        Entity best = null;
        int bestEnd = -1;
        for (int nameEnd = nameStart + 1; nameEnd <= runEnd; nameEnd++) {
            Entity candidate = Entities.find(Text.str(text, nameStart, nameEnd));
            if (candidate == null) {
                continue;
            }

            boolean semicolon = nameEnd < length && text[nameEnd] == ';';
            if (!semicolon && !candidate.legacy()) {
                continue;
            }

            int candidateEnd = nameEnd + (semicolon ? 1 : 0);
            if (candidateEnd > bestEnd) {
                best = candidate;
                bestEnd = candidateEnd;
            }
        }

        if (best == null) {
            List<String> codes = runEnd < length && text[runEnd] == ';'
                    ? List.of("AW002")
                    : List.of();
            return new Reading("unknown", runEnd, null, codes, false);
        }

        boolean semicolon = text[bestEnd - 1] == ';';
        if (semicolon) {
            return new Reading("name", bestEnd, best.cps(), List.of(), false);
        }

        if (attr && bestEnd < length
                && (Text.isAlnum(text[bestEnd]) || text[bestEnd] == '=')) {
            return new Reading("kept", bestEnd, null, List.of(), false);
        }

        return new Reading("legacy", bestEnd, best.cps(), List.of("AW001"), false);
    }


}

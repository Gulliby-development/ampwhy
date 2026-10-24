package io.github.gulliby.ampwhy.scan;

import io.github.gulliby.ampwhy.read.Reader;
import io.github.gulliby.ampwhy.read.Reading;

import java.util.ArrayList;
import java.util.List;

/** Finds references in code-point order using the authoritative reader. */
public final class Scanner {
    private Scanner() {
    }

    public static List<Occurrence> scan(int[] text, boolean attr) {
        List<Occurrence> occurrences = new ArrayList<>();
        int index = 0;
        while (index < text.length) {
            if (text[index] != '&') {
                index++;
                continue;
            }

            Reading reading = Reader.read(text, index, attr);
            if (reading == null) {
                index++;
            } else {
                occurrences.add(new Occurrence(index, reading));
                index = reading.end();
            }
        }
        return List.copyOf(occurrences);
    }
}

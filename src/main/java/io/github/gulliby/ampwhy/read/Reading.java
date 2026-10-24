package io.github.gulliby.ampwhy.read;

import java.util.List;

/** The outcome of reading one ampersand and the code points it consumes. */
public record Reading(String kind, int end, int[] result, List<String> codes, boolean mapped) {
    public Reading {
        codes = List.copyOf(codes);
    }
}

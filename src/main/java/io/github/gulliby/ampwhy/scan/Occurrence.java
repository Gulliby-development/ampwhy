package io.github.gulliby.ampwhy.scan;

import io.github.gulliby.ampwhy.read.Reading;

/** One reference occurrence found while scanning code points. */
public record Occurrence(int start, Reading reading) {
}

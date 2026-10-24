package io.github.gulliby.ampwhy.check;

import io.github.gulliby.ampwhy.base.Inputs;
import io.github.gulliby.ampwhy.base.Output;
import io.github.gulliby.ampwhy.base.Text;
import io.github.gulliby.ampwhy.lines.Lines;
import io.github.gulliby.ampwhy.problem.Problems;
import io.github.gulliby.ampwhy.scan.Occurrence;
import io.github.gulliby.ampwhy.scan.Scanner;

import java.util.List;

/** Builds an ordered report for the problems found in source text. */
public final class Checker {
    private Checker() {
    }

    public static Output check(List<Inputs.Source> sources, boolean attr) {
        StringBuilder report = new StringBuilder();
        int problemCount = 0;
        int affectedSourceCount = 0;
        int referenceCount = 0;

        for (Inputs.Source source : sources) {
            int[] text = Text.cps(source.text());
            Lines lines = new Lines(text);
            List<Occurrence> occurrences = Scanner.scan(text, attr);
            referenceCount += occurrences.size();
            boolean affected = false;

            for (Occurrence occurrence : occurrences) {
                var reading = occurrence.reading();
                String consumed = Text.str(text, occurrence.start(), reading.end());
                for (String code : reading.codes()) {
                    report.append(source.display())
                            .append(':')
                            .append(lines.line(occurrence.start()))
                            .append(':')
                            .append(lines.col(occurrence.start()))
                            .append(": ")
                            .append(code)
                            .append(' ')
                            .append(Problems.message(code, consumed, reading.result(), reading.mapped()))
                            .append('\n');
                    problemCount++;
                    affected = true;
                }
            }

            if (affected) {
                affectedSourceCount++;
            }
        }

        if (problemCount > 0) {
            report.append(Text.plural(problemCount, "problem"))
                    .append(" in ")
                    .append(Text.plural(affectedSourceCount, "file"))
                    .append('\n');
            return new Output(report.toString(), 1);
        }

        report.append("ok, ")
                .append(Text.plural(referenceCount, "reference"))
                .append(" in ")
                .append(Text.plural(sources.size(), "file"))
                .append('\n');
        return new Output(report.toString(), 0);
    }
}

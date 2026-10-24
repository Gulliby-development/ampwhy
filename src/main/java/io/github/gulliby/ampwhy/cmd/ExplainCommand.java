package io.github.gulliby.ampwhy.cmd;

import io.github.gulliby.ampwhy.base.Failure;
import io.github.gulliby.ampwhy.base.Inputs;
import io.github.gulliby.ampwhy.base.Output;
import io.github.gulliby.ampwhy.base.Text;
import io.github.gulliby.ampwhy.lines.Grid;
import io.github.gulliby.ampwhy.lines.Lines;
import io.github.gulliby.ampwhy.read.Reading;
import io.github.gulliby.ampwhy.scan.Occurrence;
import io.github.gulliby.ampwhy.scan.Scanner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ExplainCommand {
    private ExplainCommand() {
    }

    public static Output run(List<String> args, Set<String> flags, Map<String, String> values, Inputs inputs) {
        if (args.isEmpty()) {
            throw new Failure("missing FILE");
        }
        if (args.size() > 1) {
            throw new Failure("too many arguments");
        }

        Inputs.Source source = inputs.read(args.get(0));
        int[] text = Text.cps(source.text());
        List<Occurrence> occurrences = Scanner.scan(text, flags.contains("--attr"));
        if (occurrences.isEmpty()) {
            return new Output("no references\n", 0);
        }

        Lines lines = new Lines(text);
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("AT", "SOURCE", "HOW", "RESULT", "CODES"));
        for (Occurrence occurrence : occurrences) {
            Reading reading = occurrence.reading();
            int[] result = reading.result();
            String renderedResult;
            if (result == null) {
                renderedResult = "text";
            } else {
                StringBuilder resultText = new StringBuilder();
                for (int cp : result) {
                    if (!resultText.isEmpty()) {
                        resultText.append(' ');
                    }
                    resultText.append(Text.hex(cp));
                }
                renderedResult = resultText.toString();
            }

            rows.add(List.of(
                    lines.line(occurrence.start()) + ":" + lines.col(occurrence.start()),
                    Text.str(text, occurrence.start(), reading.end()),
                    reading.kind(),
                    renderedResult,
                    reading.codes().isEmpty() ? "-" : String.join(",", reading.codes())));
        }
        return new Output(Grid.render(rows), 0);
    }
}

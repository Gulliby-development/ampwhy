package io.github.gulliby.ampwhy.cmd;

import io.github.gulliby.ampwhy.base.Failure;
import io.github.gulliby.ampwhy.base.Inputs;
import io.github.gulliby.ampwhy.base.Output;
import io.github.gulliby.ampwhy.base.Text;
import io.github.gulliby.ampwhy.codec.Decoder;
import io.github.gulliby.ampwhy.scan.Occurrence;
import io.github.gulliby.ampwhy.scan.Scanner;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DecodeCommand {
    private DecodeCommand() {
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
        boolean attr = flags.contains("--attr");
        String decoded = Decoder.decode(text, attr);
        if (!flags.contains("--strict")) {
            return new Output(decoded, 0);
        }

        boolean hasProblems = false;
        for (Occurrence occurrence : Scanner.scan(text, attr)) {
            if (!occurrence.reading().codes().isEmpty()) {
                hasProblems = true;
                break;
            }
        }
        return new Output(decoded, hasProblems ? 1 : 0);
    }
}

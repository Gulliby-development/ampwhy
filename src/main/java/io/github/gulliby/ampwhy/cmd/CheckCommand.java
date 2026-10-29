package io.github.gulliby.ampwhy.cmd;

import io.github.gulliby.ampwhy.base.Failure;
import io.github.gulliby.ampwhy.base.Inputs;
import io.github.gulliby.ampwhy.base.Output;
import io.github.gulliby.ampwhy.check.Checker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Adapts command arguments and inputs to the shared checker. */
public final class CheckCommand {
    private CheckCommand() {
    }

    public static Output run(List<String> args, Set<String> flags, Map<String, String> values,
                             Inputs inputs) {
        if (args.isEmpty()) {
            throw new Failure("missing FILE");
        }

        List<Inputs.Source> sources = new ArrayList<>(args.size());
        for (String arg : args) {
            sources.add(inputs.read(arg));
        }
        Output report = Checker.check(sources, flags.contains("--attr"));
        if (!flags.contains("--summary")) {
            return report;
        }
        String text = report.out();
        int start = text.lastIndexOf('\n', text.length() - 2) + 1;
        return new Output(text.substring(start), report.code());
    }
}

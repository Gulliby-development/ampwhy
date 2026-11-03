package io.github.gulliby.ampwhy.cmd;

import io.github.gulliby.ampwhy.base.Failure;
import io.github.gulliby.ampwhy.base.Inputs;
import io.github.gulliby.ampwhy.base.Output;
import io.github.gulliby.ampwhy.base.Text;
import io.github.gulliby.ampwhy.check.Checker;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Adapts command arguments and inputs to the shared checker. */
public final class CheckCommand {
    private static final Set<String> CODES = Set.of("AW001", "AW002", "AW003", "AW004", "AW005", "AW006",
            "AW007", "AW008", "AW009");

    private CheckCommand() {
    }

    private static Set<String> codes(String value) {
        Set<String> only = new LinkedHashSet<>();
        for (String code : value.split(",", -1)) {
            if (!CODES.contains(code)) {
                throw new Failure("invalid --code " + Text.show(value)
                        + " (expected problem codes AW001 to AW009 separated by commas)");
            }
            only.add(code);
        }
        return only;
    }

    public static Output run(List<String> args, Set<String> flags, Map<String, String> values,
                             Inputs inputs) {
        if (args.isEmpty()) {
            throw new Failure("missing FILE");
        }

        Set<String> only = values.containsKey("--code") ? codes(values.get("--code")) : null;
        List<Inputs.Source> sources = new ArrayList<>(args.size());
        for (String arg : args) {
            sources.add(inputs.read(arg));
        }
        Output report = Checker.check(sources, flags.contains("--attr"), only);
        if (!flags.contains("--summary")) {
            return report;
        }
        String text = report.out();
        int start = text.lastIndexOf('\n', text.length() - 2) + 1;
        return new Output(text.substring(start), report.code());
    }
}

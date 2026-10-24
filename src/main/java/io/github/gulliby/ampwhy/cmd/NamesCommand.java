package io.github.gulliby.ampwhy.cmd;

import io.github.gulliby.ampwhy.base.Failure;
import io.github.gulliby.ampwhy.base.Inputs;
import io.github.gulliby.ampwhy.base.Output;
import io.github.gulliby.ampwhy.base.Text;
import io.github.gulliby.ampwhy.data.Entities;
import io.github.gulliby.ampwhy.lines.Grid;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Lists the supported named character references. */
public final class NamesCommand {
    private NamesCommand() {
    }

    public static Output run(List<String> args, Set<String> flags, Map<String, String> values, Inputs inputs) {
        if (args.size() > 1) {
            throw new Failure("too many arguments");
        }

        String prefix = args.isEmpty() ? "" : args.get(0);
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("NAME", "FORM", "CODE POINTS"));

        int matches = 0;
        for (Entities.Entity entity : Entities.all()) {
            if (!entity.name().startsWith(prefix)) {
                continue;
            }
            matches++;
            List<String> codePoints = new ArrayList<>();
            for (int cp : entity.cps()) {
                codePoints.add(Text.hex(cp));
            }
            rows.add(List.of(entity.name(), entity.legacy() ? "legacy" : "plain",
                    String.join(" ", codePoints)));
        }

        if (matches == 0) {
            return new Output("no name starts with " + Text.show(prefix) + "\n", 1);
        }
        return new Output(Grid.render(rows), 0);
    }
}

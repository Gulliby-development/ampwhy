package io.github.gulliby.ampwhy.cmd;

import io.github.gulliby.ampwhy.base.Failure;
import io.github.gulliby.ampwhy.base.Inputs;
import io.github.gulliby.ampwhy.base.Output;
import io.github.gulliby.ampwhy.base.Text;
import io.github.gulliby.ampwhy.codec.Encoder;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EncodeCommand {
    private EncodeCommand() {
    }

    public static Output run(List<String> args, Set<String> flags, Map<String, String> values, Inputs inputs) {
        if (args.isEmpty()) {
            throw new Failure("missing FILE");
        }
        if (args.size() > 1) {
            throw new Failure("too many arguments");
        }

        String mode = values.getOrDefault("--mode", "minimal");
        if (!mode.equals("minimal") && !mode.equals("named") && !mode.equals("numeric")) {
            throw new Failure("invalid mode " + Text.show(mode) + " (expected minimal, named or numeric)");
        }

        Inputs.Source source = inputs.read(args.get(0));
        String encoded = Encoder.encode(Text.cps(source.text()), mode, flags.contains("--attr"));
        return new Output(encoded, 0);
    }
}

package io.github.gulliby.ampwhy.cmd;

import io.github.gulliby.ampwhy.base.Failure;
import io.github.gulliby.ampwhy.base.Inputs;
import io.github.gulliby.ampwhy.base.Options;
import io.github.gulliby.ampwhy.base.Output;
import io.github.gulliby.ampwhy.base.Text;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Cli {
    private static final String HELP = "usage: ampwhy decode [--attr] [--strict] FILE\n"
            + "       ampwhy explain [--attr] FILE\n"
            + "       ampwhy check [--attr] [--summary] FILE...\n"
            + "       ampwhy encode [--mode minimal|named|numeric] [--attr] FILE\n"
            + "       ampwhy names [PREFIX]\n";

    private Cli() {
    }

    public static int run(List<String> argv, Inputs.Io io) {
        try {
            if (argv.isEmpty()) {
                throw new Failure("missing command (decode, explain, check, encode or names)");
            }

            String command = argv.get(0);
            if (command.equals("--help")) {
                if (argv.size() == 1) {
                    io.out(HELP);
                    return 0;
                }
                throw new Failure("unknown command " + Text.show(command));
            }

            Options.Parsed parsed;
            switch (command) {
                case "decode" -> parsed = Options.parse(argv.subList(1, argv.size()),
                        Set.of("--attr", "--strict"), Set.of());
                case "explain" -> parsed = Options.parse(argv.subList(1, argv.size()),
                        Set.of("--attr"), Set.of());
                case "check" -> parsed = Options.parse(argv.subList(1, argv.size()),
                        Set.of("--attr", "--summary"), Set.of());
                case "encode" -> parsed = Options.parse(argv.subList(1, argv.size()),
                        Set.of("--attr"), Set.of("--mode"));
                case "names" -> parsed = Options.parse(argv.subList(1, argv.size()), Set.of(), Set.of());
                default -> throw new Failure("unknown command " + Text.show(command));
            }

            Inputs inputs = new Inputs(io);
            Output output = switch (command) {
                case "decode" -> DecodeCommand.run(parsed.args(), parsed.flags(), parsed.values(), inputs);
                case "explain" -> ExplainCommand.run(parsed.args(), parsed.flags(), parsed.values(), inputs);
                case "check" -> CheckCommand.run(parsed.args(), parsed.flags(), parsed.values(), inputs);
                case "encode" -> EncodeCommand.run(parsed.args(), parsed.flags(), parsed.values(), inputs);
                case "names" -> NamesCommand.run(parsed.args(), parsed.flags(), parsed.values(), inputs);
                default -> throw new AssertionError("validated command");
            };
            io.out(output.out());
            return output.code();
        } catch (Failure failure) {
            io.err("ampwhy: " + failure.getMessage() + "\n");
            return 2;
        }
    }
}

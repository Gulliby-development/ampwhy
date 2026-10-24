package io.github.gulliby.ampwhy.base;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Options {
    private Options() {
    }

    public static Parsed parse(List<String> argv, Set<String> flags, Set<String> valued) {
        Set<String> parsedFlags = new LinkedHashSet<>();
        Map<String, String> parsedValues = new LinkedHashMap<>();
        List<String> args = new ArrayList<>();

        for (int i = 0; i < argv.size(); i++) {
            String arg = argv.get(i);
            int[] codePoints = Text.cps(arg);
            if (codePoints.length >= 2 && codePoints[0] == '-' && codePoints[1] == '-') {
                int equals = -1;
                for (int j = 0; j < codePoints.length; j++) {
                    if (codePoints[j] == '=') {
                        equals = j;
                        break;
                    }
                }

                String name = Text.str(codePoints, 0, equals < 0 ? codePoints.length : equals);
                if (valued.contains(name)) {
                    if (equals >= 0) {
                        parsedValues.put(name, Text.str(codePoints, equals + 1, codePoints.length));
                    } else if (i + 1 < argv.size()) {
                        parsedValues.put(name, argv.get(++i));
                    } else {
                        throw new Failure(name + " needs a value");
                    }
                } else if (flags.contains(arg)) {
                    parsedFlags.add(arg);
                } else {
                    throw new Failure("unknown option " + Text.show(arg));
                }
            } else {
                args.add(arg);
            }
        }

        return new Parsed(parsedFlags, parsedValues, args);
    }

    public record Parsed(Set<String> flags, Map<String, String> values, List<String> args) {
        public Parsed {
            flags = Collections.unmodifiableSet(new LinkedHashSet<>(flags));
            values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
            args = List.copyOf(args);
        }
    }
}

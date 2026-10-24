package io.github.gulliby.ampwhy.cmd;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.gulliby.ampwhy.base.Inputs;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CliTest {
    static final class FakeIo implements Inputs.Io {
        final Map<String, byte[]> files = new HashMap<>();
        byte[] stdin = new byte[0];
        final StringBuilder out = new StringBuilder();
        final StringBuilder err = new StringBuilder();

        @Override public byte[] readStdin() { return stdin; }

        @Override
        public byte[] readFile(String path) throws IOException {
            byte[] data = files.get(path);
            if (data == null) {
                throw new IOException("no such file");
            }
            return data;
        }

        @Override public void out(String text) { out.append(text); }

        @Override public void err(String text) { err.append(text); }
    }

    @Test
    void helpAndCommandFailures() {
        FakeIo help = new FakeIo();
        assertEquals(0, Cli.run(List.of("--help"), help));
        assertEquals("usage: ampwhy decode [--attr] [--strict] FILE\n"
                + "       ampwhy explain [--attr] FILE\n"
                + "       ampwhy check [--attr] FILE...\n"
                + "       ampwhy encode [--mode minimal|named|numeric] [--attr] FILE\n"
                + "       ampwhy names [PREFIX]\n", help.out.toString());
        assertEquals("", help.err.toString());

        FakeIo missing = new FakeIo();
        assertEquals(2, Cli.run(List.of(), missing));
        assertEquals("", missing.out.toString());
        assertEquals("ampwhy: missing command (decode, explain, check, encode or names)\n", missing.err.toString());

        FakeIo unknown = new FakeIo();
        assertEquals(2, Cli.run(List.of("show"), unknown));
        assertEquals("", unknown.out.toString());
        assertEquals("ampwhy: unknown command \"show\"\n", unknown.err.toString());

        FakeIo helpOperand = new FakeIo();
        assertEquals(2, Cli.run(List.of("--help", "x"), helpOperand));
        assertEquals("", helpOperand.out.toString());
        assertEquals("ampwhy: unknown command \"--help\"\n", helpOperand.err.toString());
    }

    @Test
    void dispatchesDecodeExplainAndCheck() {
        FakeIo strict = new FakeIo();
        strict.files.put("f", "&amp".getBytes(StandardCharsets.UTF_8));
        assertEquals(1, Cli.run(List.of("decode", "--strict", "f"), strict));
        assertEquals("&", strict.out.toString());
        assertEquals("", strict.err.toString());

        FakeIo repeatedFlag = new FakeIo();
        repeatedFlag.files.put("f", "&amp".getBytes(StandardCharsets.UTF_8));
        assertEquals(0, Cli.run(List.of("decode", "--attr", "--attr", "f"), repeatedFlag));
        assertEquals("&", repeatedFlag.out.toString());
        assertEquals("", repeatedFlag.err.toString());

        FakeIo explain = new FakeIo();
        explain.files.put("empty", new byte[0]);
        assertEquals(0, Cli.run(List.of("explain", "empty"), explain));
        assertEquals("no references\n", explain.out.toString());
        assertEquals("", explain.err.toString());

        FakeIo check = new FakeIo();
        check.files.put("empty", new byte[0]);
        assertEquals(0, Cli.run(List.of("check", "empty"), check));
        assertEquals("ok, 0 references in 1 file\n", check.out.toString());
        assertEquals("", check.err.toString());
    }

    @Test
    void dispatchesEncodeAndNegativeNames() {
        FakeIo encode = new FakeIo();
        encode.stdin = "&".getBytes(StandardCharsets.UTF_8);
        assertEquals(0, Cli.run(List.of("encode", "--mode=full", "--mode", "minimal", "-"), encode));
        assertEquals("&amp;", encode.out.toString());
        assertEquals("", encode.err.toString());

        FakeIo names = new FakeIo();
        assertEquals(1, Cli.run(List.of("names", "Z"), names));
        assertEquals("no name starts with \"Z\"\n", names.out.toString());
        assertEquals("", names.err.toString());
    }

    @Test
    void validatesCommandOptionsBeforeArity() {
        FakeIo command = new FakeIo();
        assertEquals(2, Cli.run(List.of("show", "--all"), command));
        assertEquals("", command.out.toString());
        assertEquals("ampwhy: unknown command \"show\"\n", command.err.toString());

        FakeIo unknownOption = new FakeIo();
        assertEquals(2, Cli.run(List.of("decode", "--all"), unknownOption));
        assertEquals("", unknownOption.out.toString());
        assertEquals("ampwhy: unknown option \"--all\"\n", unknownOption.err.toString());

        FakeIo wrongCommandFlag = new FakeIo();
        assertEquals(2, Cli.run(List.of("explain", "--strict", "absent"), wrongCommandFlag));
        assertEquals("", wrongCommandFlag.out.toString());
        assertEquals("ampwhy: unknown option \"--strict\"\n", wrongCommandFlag.err.toString());

        FakeIo valuedFlag = new FakeIo();
        assertEquals(2, Cli.run(List.of("decode", "--attr=1", "absent"), valuedFlag));
        assertEquals("", valuedFlag.out.toString());
        assertEquals("ampwhy: unknown option \"--attr=1\"\n", valuedFlag.err.toString());

        FakeIo namesOption = new FakeIo();
        assertEquals(2, Cli.run(List.of("names", "--attr"), namesOption));
        assertEquals("", namesOption.out.toString());
        assertEquals("ampwhy: unknown option \"--attr\"\n", namesOption.err.toString());

        FakeIo missingValue = new FakeIo();
        assertEquals(2, Cli.run(List.of("encode", "--mode"), missingValue));
        assertEquals("", missingValue.out.toString());
        assertEquals("ampwhy: --mode needs a value\n", missingValue.err.toString());
    }

    @Test
    void validatesArityBeforeModeAndReportsArityFailures() {
        FakeIo arityBeforeMode = new FakeIo();
        assertEquals(2, Cli.run(List.of("encode", "--mode", "full"), arityBeforeMode));
        assertEquals("", arityBeforeMode.out.toString());
        assertEquals("ampwhy: missing FILE\n", arityBeforeMode.err.toString());

        FakeIo invalidMode = new FakeIo();
        assertEquals(2, Cli.run(List.of("encode", "--mode", "full", "absent"), invalidMode));
        assertEquals("", invalidMode.out.toString());
        assertEquals("ampwhy: invalid mode \"full\" (expected minimal, named or numeric)\n", invalidMode.err.toString());

        FakeIo tooManyDecode = new FakeIo();
        assertEquals(2, Cli.run(List.of("decode", "f", "f"), tooManyDecode));
        assertEquals("", tooManyDecode.out.toString());
        assertEquals("ampwhy: too many arguments\n", tooManyDecode.err.toString());

        FakeIo missingCheck = new FakeIo();
        assertEquals(2, Cli.run(List.of("check"), missingCheck));
        assertEquals("", missingCheck.out.toString());
        assertEquals("ampwhy: missing FILE\n", missingCheck.err.toString());

        FakeIo tooManyNames = new FakeIo();
        assertEquals(2, Cli.run(List.of("names", "a", "b"), tooManyNames));
        assertEquals("", tooManyNames.out.toString());
        assertEquals("ampwhy: too many arguments\n", tooManyNames.err.toString());
    }

    @Test
    void keepsCheckFailureAtomicAndSanitizesPaths() {
        FakeIo lateFailure = new FakeIo();
        lateFailure.files.put("f", "&amp".getBytes(StandardCharsets.UTF_8));
        assertEquals(2, Cli.run(List.of("check", "f", "nofile.txt"), lateFailure));
        assertEquals("", lateFailure.out.toString());
        assertEquals("ampwhy: nofile.txt: cannot read the file\n", lateFailure.err.toString());

        FakeIo unicodePath = new FakeIo();
        assertEquals(2, Cli.run(List.of("decode", "café.txt"), unicodePath));
        assertEquals("", unicodePath.out.toString());
        assertEquals("ampwhy: caf?.txt: cannot read the file\n", unicodePath.err.toString());

        FakeIo emptyPath = new FakeIo();
        assertEquals(2, Cli.run(List.of("decode", ""), emptyPath));
        assertEquals("", emptyPath.out.toString());
        assertEquals("ampwhy: : cannot read the file\n", emptyPath.err.toString());
    }

    @Test
    void handlesDuplicateAndMalformedStdinAndEmptySuccess() {
        FakeIo duplicate = new FakeIo();
        duplicate.stdin = "x".getBytes(StandardCharsets.UTF_8);
        assertEquals(2, Cli.run(List.of("check", "-", "-"), duplicate));
        assertEquals("", duplicate.out.toString());
        assertEquals("ampwhy: standard input is named twice\n", duplicate.err.toString());

        FakeIo malformed = new FakeIo();
        malformed.stdin = new byte[] {0x61, (byte) 0xFF, 0x0A};
        assertEquals(2, Cli.run(List.of("check", "-"), malformed));
        assertEquals("", malformed.out.toString());
        assertEquals("ampwhy: <stdin>: input is not valid UTF-8\n", malformed.err.toString());

        FakeIo empty = new FakeIo();
        empty.stdin = new byte[0];
        assertEquals(0, Cli.run(List.of("decode", "-"), empty));
        assertEquals("", empty.out.toString());
        assertEquals("", empty.err.toString());
    }
}

package io.github.gulliby.ampwhy.cmd;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.gulliby.ampwhy.base.Inputs;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CheckCodeTest {
    static final class FakeIo implements Inputs.Io {
        final Map<String, byte[]> files = new HashMap<>();
        final StringBuilder out = new StringBuilder();
        final StringBuilder err = new StringBuilder();

        @Override public byte[] readStdin() { return new byte[0]; }

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

    private static final String PAGE = "Fish &amp chips &zzz; &#x; &#65 &#0;\n&#x80; &#x110000; &copy 2024\n";

    private static FakeIo io() {
        FakeIo io = new FakeIo();
        io.files.put("page.html", PAGE.getBytes(StandardCharsets.UTF_8));
        io.files.put("clean.html", "a &lt; b".getBytes(StandardCharsets.UTF_8));
        return io;
    }

    private static void check(int code, String out, String err, String... argv) {
        FakeIo io = io();
        assertEquals(code, Cli.run(List.of(argv), io), String.join(" ", argv));
        assertEquals(out, io.out.toString(), String.join(" ", argv));
        assertEquals(err, io.err.toString(), String.join(" ", argv));
    }

    @Test
    void reportsOnlyTheNamedCodes() {
        check(1, "page.html:1:6: AW001 missing semicolon after \"&amp\"\n"
                + "page.html:1:28: AW004 missing semicolon after \"&#65\"\n"
                + "page.html:2:19: AW001 missing semicolon after \"&copy\"\n"
                + "3 problems in 1 file\n", "", "check", "--code", "AW001,AW004", "page.html");
        check(1, "page.html:1:17: AW002 unknown name \"&zzz;\"\n1 problem in 1 file\n", "",
                "check", "--code=AW002", "page.html");
    }

    @Test
    void theListOrderAndRepeatsDoNotMatter() {
        String expected = "page.html:1:17: AW002 unknown name \"&zzz;\"\n"
                + "page.html:1:23: AW003 no digits in \"&#x\"\n2 problems in 1 file\n";
        check(1, expected, "", "check", "--code", "AW003,AW002", "page.html");
        check(1, expected, "", "check", "--code", "AW002,AW003,AW002", "page.html");
    }

    @Test
    void aFileWithoutTheNamedCodesIsOkAndKeepsTheReferenceCount() {
        check(0, "ok, 8 references in 1 file\n", "", "check", "--code", "AW008", "page.html");
        check(0, "ok, 1 reference in 1 file\n", "", "check", "--code", "AW001", "clean.html");
    }

    @Test
    void combinesWithSummaryAndAttr() {
        check(1, "2 problems in 1 file\n", "", "check", "--summary", "--code", "AW005,AW006", "page.html");
        check(0, "ok, 8 references in 1 file\n", "", "check", "--summary", "--code", "AW007", "page.html");
    }

    @Test
    void rejectsAnInvalidList() {
        String hint = " (expected problem codes AW001 to AW009 separated by commas)\n";
        check(2, "", "ampwhy: invalid --code \"AW010\"" + hint, "check", "--code", "AW010", "page.html");
        check(2, "", "ampwhy: invalid --code \"AW001,\"" + hint, "check", "--code", "AW001,", "page.html");
        check(2, "", "ampwhy: invalid --code \"\"" + hint, "check", "--code=", "page.html");
        check(2, "", "ampwhy: invalid --code \"aw001\"" + hint, "check", "--code", "aw001", "page.html");
        check(2, "", "ampwhy: invalid --code \"AW001, AW002\"" + hint, "check", "--code", "AW001, AW002", "page.html");
        check(2, "", "ampwhy: --code needs a value\n", "check", "page.html", "--code");
    }

    @Test
    void theListIsCheckedBeforeAnyFileIsRead() {
        check(2, "", "ampwhy: invalid --code \"X\"" + " (expected problem codes AW001 to AW009 separated by commas)\n",
                "check", "--code", "X", "missing.html");
    }

    @Test
    void otherCommandsDoNotTakeTheOption() {
        check(2, "", "ampwhy: unknown option \"--code\"\n", "explain", "--code", "AW001", "page.html");
        check(2, "", "ampwhy: unknown option \"--code=AW001\"\n", "decode", "--code=AW001", "page.html");
    }
}

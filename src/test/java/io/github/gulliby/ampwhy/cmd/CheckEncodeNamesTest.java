package io.github.gulliby.ampwhy.cmd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.gulliby.ampwhy.base.Failure;
import io.github.gulliby.ampwhy.base.Inputs;
import io.github.gulliby.ampwhy.base.Output;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CheckEncodeNamesTest {
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
    void checkReportsCleanAndOrderedMultiSourceResults() {
        FakeIo cleanIo = new FakeIo();
        cleanIo.files.put("a.html", "a &lt; b &amp;&amp; c &#x1F600;".getBytes(StandardCharsets.UTF_8));
        assertEquals(new Output("ok, 4 references in 1 file\n", 0),
                CheckCommand.run(List.of("a.html"), Set.of(), Map.of(), new Inputs(cleanIo)));
        assertEquals("", cleanIo.out.toString());
        assertEquals("", cleanIo.err.toString());

        FakeIo reportIo = new FakeIo();
        reportIo.files.put("a.html", "&lt;b&gt; &amp;".getBytes(StandardCharsets.UTF_8));
        reportIo.files.put("b.html", "x&copy y&#65".getBytes(StandardCharsets.UTF_8));
        assertEquals(new Output("b.html:1:2: AW001 missing semicolon after \"&copy\"\nb.html:1:9: AW004 missing semicolon after \"&#65\"\n2 problems in 1 file\n", 1),
                CheckCommand.run(List.of("a.html", "b.html"), Set.of(), Map.of(), new Inputs(reportIo)));
        assertEquals("", reportIo.out.toString());
        assertEquals("", reportIo.err.toString());
    }

    @Test
    void checkPreservesDuplicateFilesAndForwardsAttributeMode() {
        FakeIo duplicateIo = new FakeIo();
        duplicateIo.files.put("a.html", "&amp;".getBytes(StandardCharsets.UTF_8));
        assertEquals(new Output("ok, 2 references in 2 files\n", 0),
                CheckCommand.run(List.of("a.html", "a.html"), Set.of(), Map.of(), new Inputs(duplicateIo)));
        assertEquals("", duplicateIo.out.toString());
        assertEquals("", duplicateIo.err.toString());

        FakeIo attrIo = new FakeIo();
        attrIo.files.put("a.html", "&ampx".getBytes(StandardCharsets.UTF_8));
        assertEquals(new Output("ok, 1 reference in 1 file\n", 0),
                CheckCommand.run(List.of("a.html"), Set.of("--attr"), Map.of(), new Inputs(attrIo)));
        assertEquals("", attrIo.out.toString());
        assertEquals("", attrIo.err.toString());
    }

    @Test
    void checkRejectsMissingFilesAndAtomicInputFailures() {
        FakeIo missingIo = new FakeIo();
        assertEquals("missing FILE", assertThrows(Failure.class,
                () -> CheckCommand.run(List.of(), Set.of(), Map.of(), new Inputs(missingIo))).getMessage());
        assertEquals("", missingIo.out.toString());
        assertEquals("", missingIo.err.toString());

        FakeIo laterIo = new FakeIo();
        laterIo.files.put("f", "&amp".getBytes(StandardCharsets.UTF_8));
        assertEquals("nofile.txt: cannot read the file", assertThrows(Failure.class,
                () -> CheckCommand.run(List.of("f", "nofile.txt"), Set.of(), Map.of(), new Inputs(laterIo))).getMessage());
        assertEquals("", laterIo.out.toString());
        assertEquals("", laterIo.err.toString());

        FakeIo duplicateStdinIo = new FakeIo();
        duplicateStdinIo.stdin = "x".getBytes(StandardCharsets.UTF_8);
        assertEquals("standard input is named twice", assertThrows(Failure.class,
                () -> CheckCommand.run(List.of("-", "-"), Set.of(), Map.of(), new Inputs(duplicateStdinIo))).getMessage());
        assertEquals("", duplicateStdinIo.out.toString());
        assertEquals("", duplicateStdinIo.err.toString());
    }

    @Test
    void checkRejectsMalformedStdinAtomically() {
        FakeIo io = new FakeIo();
        io.files.put("f", "&amp".getBytes(StandardCharsets.UTF_8));
        io.stdin = new byte[] {0x61,(byte)0xFF,0x0A};

        assertEquals("<stdin>: input is not valid UTF-8", assertThrows(Failure.class,
                () -> CheckCommand.run(List.of("f", "-"), Set.of(), Map.of(), new Inputs(io))).getMessage());
        assertEquals("", io.out.toString());
        assertEquals("", io.err.toString());
    }

    @Test
    void encodeChecksArityBeforeMode() {
        FakeIo missingIo = new FakeIo();
        assertEquals("missing FILE", assertThrows(Failure.class,
                () -> EncodeCommand.run(List.of(), Set.of(), Map.of("--mode", "full"), new Inputs(missingIo))).getMessage());
        assertEquals("", missingIo.out.toString());
        assertEquals("", missingIo.err.toString());

        FakeIo excessIo = new FakeIo();
        assertEquals("too many arguments", assertThrows(Failure.class,
                () -> EncodeCommand.run(List.of("f", "g"), Set.of(), Map.of("--mode", "full"), new Inputs(excessIo))).getMessage());
        assertEquals("", excessIo.out.toString());
        assertEquals("", excessIo.err.toString());
    }

    @Test
    void encodeValidatesEveryInvalidModeBeforeInput() {
        FakeIo fullIo = new FakeIo();
        assertEquals("invalid mode \"full\" (expected minimal, named or numeric)", assertThrows(Failure.class,
                () -> EncodeCommand.run(List.of("f"), Set.of(), Map.of("--mode", "full"), new Inputs(fullIo))).getMessage());
        assertEquals("", fullIo.out.toString());
        assertEquals("", fullIo.err.toString());

        FakeIo emptyIo = new FakeIo();
        assertEquals("invalid mode \"\" (expected minimal, named or numeric)", assertThrows(Failure.class,
                () -> EncodeCommand.run(List.of("f"), Set.of(), Map.of("--mode", ""), new Inputs(emptyIo))).getMessage());
        assertEquals("", emptyIo.out.toString());
        assertEquals("", emptyIo.err.toString());

        FakeIo caseIo = new FakeIo();
        assertEquals("invalid mode \"NAMED\" (expected minimal, named or numeric)", assertThrows(Failure.class,
                () -> EncodeCommand.run(List.of("f"), Set.of(), Map.of("--mode", "NAMED"), new Inputs(caseIo))).getMessage());
        assertEquals("", caseIo.out.toString());
        assertEquals("", caseIo.err.toString());

        FakeIo sanitizedIo = new FakeIo();
        assertEquals("invalid mode \"??\" (expected minimal, named or numeric)", assertThrows(Failure.class,
                () -> EncodeCommand.run(List.of("f"), Set.of(), Map.of("--mode", "é\n"), new Inputs(sanitizedIo))).getMessage());
        assertEquals("", sanitizedIo.out.toString());
        assertEquals("", sanitizedIo.err.toString());

        FakeIo validIo = new FakeIo();
        assertEquals("f: cannot read the file", assertThrows(Failure.class,
                () -> EncodeCommand.run(List.of("f"), Set.of(), Map.of("--mode", "minimal"), new Inputs(validIo))).getMessage());
        assertEquals("", validIo.out.toString());
        assertEquals("", validIo.err.toString());
    }

    @Test
    void encodeMinimalAndAttributeMinimalExamples() {
        FakeIo minimalIo = new FakeIo();
        minimalIo.files.put("f", "&amp;<>'\"é".getBytes(StandardCharsets.UTF_8));
        assertEquals(new Output("&amp;amp;&lt;&gt;'\"é", 0),
                EncodeCommand.run(List.of("f"), Set.of(), Map.of(), new Inputs(minimalIo)));
        assertEquals("", minimalIo.out.toString());
        assertEquals("", minimalIo.err.toString());

        FakeIo attrIo = new FakeIo();
        attrIo.files.put("f", "&<>\"".getBytes(StandardCharsets.UTF_8));
        assertEquals(new Output("&amp;&lt;&gt;&quot;", 0),
                EncodeCommand.run(List.of("f"), Set.of("--attr"), Map.of("--mode", "minimal"), new Inputs(attrIo)));
        assertEquals("", attrIo.out.toString());
        assertEquals("", attrIo.err.toString());
    }

    @Test
    void encodeNamedAndNumericExamples() {
        FakeIo namedIo = new FakeIo();
        namedIo.files.put("f", "é €ć😀'\"".getBytes(StandardCharsets.UTF_8));
        assertEquals(new Output("&eacute; &euro;ć😀&apos;&quot;", 0),
                EncodeCommand.run(List.of("f"), Set.of(), Map.of("--mode", "named"), new Inputs(namedIo)));
        assertEquals("", namedIo.out.toString());
        assertEquals("", namedIo.err.toString());

        FakeIo numericIo = new FakeIo();
        numericIo.files.put("f", "é €ć😀\r".getBytes(StandardCharsets.UTF_8));
        assertEquals(new Output("&eacute; &euro;&#x107;&#x1F600;&#xD;", 0),
                EncodeCommand.run(List.of("f"), Set.of(), Map.of("--mode", "numeric"), new Inputs(numericIo)));
        assertEquals("", numericIo.out.toString());
        assertEquals("", numericIo.err.toString());
    }

    @Test
    void encodePreservesBomLineEndingsAndEmptyStdin() {
        FakeIo preservationIo = new FakeIo();
        preservationIo.files.put("f", "\uFEFFx\r\n".getBytes(StandardCharsets.UTF_8));
        assertEquals(new Output("\uFEFFx\r\n", 0),
                EncodeCommand.run(List.of("f"), Set.of(), Map.of(), new Inputs(preservationIo)));
        assertEquals("", preservationIo.out.toString());
        assertEquals("", preservationIo.err.toString());

        FakeIo emptyIo = new FakeIo();
        emptyIo.stdin = new byte[0];
        assertEquals(new Output("", 0),
                EncodeCommand.run(List.of("-"), Set.of(), Map.of(), new Inputs(emptyIo)));
        assertEquals("", emptyIo.out.toString());
        assertEquals("", emptyIo.err.toString());
    }

    @Test
    void namesRendersExactPrefixesAndLeavesStdinUnread() {
        FakeIo io = new FakeIo();
        io.stdin = "untouched".getBytes(StandardCharsets.UTF_8);
        Inputs inputs = new Inputs(io);
        assertEquals(new Output("NAME   FORM    CODE POINTS\nnot    legacy  U+00AC\nnotin  plain   U+2209\nnotni  plain   U+220C\n", 0),
                NamesCommand.run(List.of("not"), Set.of(), Map.of(), inputs));
        assertEquals(new Output("NAME      FORM    CODE POINTS\npara      legacy  U+00B6\nparallel  plain   U+2225\nplusmn    legacy  U+00B1\npound     legacy  U+00A3\n", 0),
                NamesCommand.run(List.of("p"), Set.of(), Map.of(), inputs));
        assertEquals(new Output("NAME  FORM   CODE POINTS\nnvlt  plain  U+003C U+20D2\n", 0),
                NamesCommand.run(List.of("nvlt"), Set.of(), Map.of(), inputs));
        assertEquals(new Output("NAME  FORM    CODE POINTS\nAMP   legacy  U+0026\n", 0),
                NamesCommand.run(List.of("AMP"), Set.of(), Map.of(), inputs));
        assertEquals(new Inputs.Source("<stdin>", "untouched"), inputs.read("-"));
        assertEquals("", io.out.toString());
        assertEquals("", io.err.toString());
    }

    @Test
    void namesReturnsSanitizedNegativeAnswersAndRejectsExcessArguments() {
        FakeIo io = new FakeIo();
        Inputs inputs = new Inputs(io);
        assertEquals(new Output("no name starts with \"Z\"\n", 1),
                NamesCommand.run(List.of("Z"), Set.of(), Map.of(), inputs));
        assertEquals(new Output("no name starts with \"Amp\"\n", 1),
                NamesCommand.run(List.of("Amp"), Set.of(), Map.of(), inputs));
        assertEquals(new Output("no name starts with \"??\"\n", 1),
                NamesCommand.run(List.of("é\n"), Set.of(), Map.of(), inputs));
        assertEquals("too many arguments", assertThrows(Failure.class,
                () -> NamesCommand.run(List.of("a", "b"), Set.of(), Map.of(), inputs)).getMessage());
        assertEquals("", io.out.toString());
        assertEquals("", io.err.toString());
    }

    @Test
    void namesAbsentAndEmptyPrefixesReturnTheExactFullTable() {
        List<String> expected = List.of(
                "NAME      FORM    CODE POINTS",
                "AMP       legacy  U+0026",
                "GT        legacy  U+003E",
                "LT        legacy  U+003C",
                "QUOT      legacy  U+0022",
                "acute     legacy  U+00B4",
                "amp       legacy  U+0026",
                "ap        plain   U+2248",
                "apos      plain   U+0027",
                "cent      legacy  U+00A2",
                "copy      legacy  U+00A9",
                "copysr    plain   U+2117",
                "deg       legacy  U+00B0",
                "divide    legacy  U+00F7",
                "eacute    legacy  U+00E9",
                "euro      plain   U+20AC",
                "gt        legacy  U+003E",
                "gtcc      plain   U+2AA7",
                "hearts    plain   U+2665",
                "hellip    plain   U+2026",
                "laquo     legacy  U+00AB",
                "larr      plain   U+2190",
                "ldquo     plain   U+201C",
                "lsquo     plain   U+2018",
                "lt        legacy  U+003C",
                "ltri      plain   U+25C3",
                "mdash     plain   U+2014",
                "micro     legacy  U+00B5",
                "nbsp      legacy  U+00A0",
                "ndash     plain   U+2013",
                "not       legacy  U+00AC",
                "notin     plain   U+2209",
                "notni     plain   U+220C",
                "nvlt      plain   U+003C U+20D2",
                "ouml      legacy  U+00F6",
                "para      legacy  U+00B6",
                "parallel  plain   U+2225",
                "plusmn    legacy  U+00B1",
                "pound     legacy  U+00A3",
                "quot      legacy  U+0022",
                "raquo     legacy  U+00BB",
                "rarr      plain   U+2192",
                "rdquo     plain   U+201D",
                "reg       legacy  U+00AE",
                "rsquo     plain   U+2019",
                "sect      legacy  U+00A7",
                "shy       legacy  U+00AD",
                "szlig     legacy  U+00DF",
                "times     legacy  U+00D7",
                "timesb    plain   U+22A0",
                "trade     plain   U+2122",
                "yen       legacy  U+00A5");
        FakeIo absentIo = new FakeIo();
        Output absent = NamesCommand.run(List.of(), Set.of(), Map.of(), new Inputs(absentIo));
        assertEquals(0, absent.code());
        assertEquals(expected, absent.out().lines().toList());
        assertTrue(absent.out().endsWith("\n"));
        assertFalse(absent.out().endsWith(" \n"));
        assertEquals("", absentIo.out.toString());
        assertEquals("", absentIo.err.toString());

        FakeIo emptyIo = new FakeIo();
        Output empty = NamesCommand.run(List.of(""), Set.of(), Map.of(), new Inputs(emptyIo));
        assertEquals(0, empty.code());
        assertEquals(expected, empty.out().lines().toList());
        assertEquals(absent.out(), empty.out());
        assertTrue(empty.out().endsWith("\n"));
        assertFalse(empty.out().endsWith(" \n"));
        assertEquals("", emptyIo.out.toString());
        assertEquals("", emptyIo.err.toString());
    }
}

package io.github.gulliby.ampwhy.cmd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

class DecodeExplainTest {
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
    void validatesArityBeforeReadingForBothCommands() {
        FakeIo io = new FakeIo();
        Inputs inputs = new Inputs(io);

        assertEquals("missing FILE", assertThrows(Failure.class, () -> DecodeCommand.run(List.of(), Set.of(), Map.of(), inputs)).getMessage());
        assertEquals("too many arguments", assertThrows(Failure.class, () -> DecodeCommand.run(List.of("nofile.txt", "nofile.txt"), Set.of(), Map.of(), inputs)).getMessage());
        assertEquals("missing FILE", assertThrows(Failure.class, () -> ExplainCommand.run(List.of(), Set.of(), Map.of(), inputs)).getMessage());
        assertEquals("too many arguments", assertThrows(Failure.class, () -> ExplainCommand.run(List.of("nofile.txt", "nofile.txt"), Set.of(), Map.of(), inputs)).getMessage());
        assertEquals("", io.out.toString());
        assertEquals("", io.err.toString());
    }

    @Test
    void reportsUnreadableAndSanitizedPathsForBothCommands() {
        FakeIo io = new FakeIo();

        assertEquals("nofile.txt: cannot read the file", assertThrows(Failure.class, () -> DecodeCommand.run(List.of("nofile.txt"), Set.of(), Map.of(), new Inputs(io))).getMessage());
        assertEquals("caf?.txt: cannot read the file", assertThrows(Failure.class, () -> DecodeCommand.run(List.of("café.txt"), Set.of(), Map.of(), new Inputs(io))).getMessage());
        assertEquals("nofile.txt: cannot read the file", assertThrows(Failure.class, () -> ExplainCommand.run(List.of("nofile.txt"), Set.of(), Map.of(), new Inputs(io))).getMessage());
        assertEquals("caf?.txt: cannot read the file", assertThrows(Failure.class, () -> ExplainCommand.run(List.of("café.txt"), Set.of(), Map.of(), new Inputs(io))).getMessage());
        assertEquals("", io.out.toString());
        assertEquals("", io.err.toString());
    }

    @Test
    void rejectsMalformedStdinForBothCommands() {
        FakeIo io = new FakeIo();
        io.stdin = new byte[] {0x61, (byte) 0xFF, 0x0A};

        assertEquals("<stdin>: input is not valid UTF-8", assertThrows(Failure.class, () -> DecodeCommand.run(List.of("-"), Set.of(), Map.of(), new Inputs(io))).getMessage());
        assertEquals("<stdin>: input is not valid UTF-8", assertThrows(Failure.class, () -> ExplainCommand.run(List.of("-"), Set.of(), Map.of(), new Inputs(io))).getMessage());
        assertEquals("", io.out.toString());
        assertEquals("", io.err.toString());
    }

    @Test
    void decodesOrdinaryMixedAndCleanStrictInputs() {
        FakeIo io = new FakeIo();
        io.files.put("ordinary.html", "x &amp; y".getBytes(StandardCharsets.UTF_8));
        io.files.put("mixed.html", "&notit; &zzz; &#x; &#65 &#0;".getBytes(StandardCharsets.UTF_8));
        io.files.put("clean.html", "&amp; &#65;".getBytes(StandardCharsets.UTF_8));

        assertEquals(new Output("x & y", 0), DecodeCommand.run(List.of("ordinary.html"), Set.of(), Map.of(), new Inputs(io)));
        assertEquals(new Output("¬it; &zzz; &#x; A �", 0), DecodeCommand.run(List.of("mixed.html"), Set.of(), Map.of(), new Inputs(io)));
        assertEquals(new Output("& A", 0), DecodeCommand.run(List.of("clean.html"), Set.of("--strict"), Map.of(), new Inputs(io)));
        assertEquals("", io.out.toString());
        assertEquals("", io.err.toString());
    }

    @Test
    void strictDecodeKeepsConvertedTextAndAttributeRulesApply() {
        FakeIo io = new FakeIo();
        io.files.put("mixed.html", "&notit; &zzz; &#x; &#65 &#0;".getBytes(StandardCharsets.UTF_8));
        io.files.put("q.html", "&copy=2&amp;b=3&notit=4&lt".getBytes(StandardCharsets.UTF_8));
        io.files.put("retained.html", "&ampx &zzz".getBytes(StandardCharsets.UTF_8));

        assertEquals(new Output("¬it; &zzz; &#x; A �", 1), DecodeCommand.run(List.of("mixed.html"), Set.of("--strict"), Map.of(), new Inputs(io)));
        assertEquals(new Output("©=2&b=3¬it=4<", 0), DecodeCommand.run(List.of("q.html"), Set.of(), Map.of(), new Inputs(io)));
        assertEquals(new Output("&copy=2&b=3&notit=4<", 1), DecodeCommand.run(List.of("q.html"), Set.of("--attr", "--strict"), Map.of(), new Inputs(io)));
        assertEquals(new Output("&ampx &zzz", 0), DecodeCommand.run(List.of("retained.html"), Set.of("--attr", "--strict"), Map.of(), new Inputs(io)));
        assertEquals("", io.out.toString());
        assertEquals("", io.err.toString());
    }

    @Test
    void decodesEmptyStdinAndPreservesCodePointsAndLineEndings() {
        FakeIo io = new FakeIo();
        io.stdin = new byte[0];

        assertEquals(new Output("", 0), DecodeCommand.run(List.of("-"), Set.of(), Map.of(), new Inputs(io)));
        io.stdin = "\uFEFF😀\u0000\r\n&amp;\r".getBytes(StandardCharsets.UTF_8);
        assertEquals(new Output("\uFEFF😀\u0000\r\n&\r", 0), DecodeCommand.run(List.of("-"), Set.of(), Map.of(), new Inputs(io)));
        assertEquals("", io.out.toString());
        assertEquals("", io.err.toString());
    }

    @Test
    void explainsNamesUnknownAndNoneAndKeptOccurrences() {
        FakeIo io = new FakeIo();
        io.files.put("names.html", "&amp; &amp &zzz; &#x;".getBytes(StandardCharsets.UTF_8));
        io.files.put("kept.html", "&copy=2 &amp;".getBytes(StandardCharsets.UTF_8));

        assertEquals(new Output("AT    SOURCE  HOW      RESULT  CODES\n1:1   &amp;   name     U+0026  -\n1:7   &amp    legacy   U+0026  AW001\n1:12  &zzz    unknown  text    AW002\n1:18  &#x     none     text    AW003\n", 0), ExplainCommand.run(List.of("names.html"), Set.of(), Map.of(), new Inputs(io)));
        assertEquals(new Output("AT   SOURCE  HOW   RESULT  CODES\n1:1  &copy   kept  text    -\n1:9  &amp;   name  U+0026  -\n", 0), ExplainCommand.run(List.of("kept.html"), Set.of("--attr"), Map.of(), new Inputs(io)));
        assertEquals("", io.out.toString());
        assertEquals("", io.err.toString());
    }

    @Test
    void explainsNumericValuesOrderedCodesAndMultipleCodePoints() {
        FakeIo io = new FakeIo();
        io.files.put("numeric.html", "&#65 &#x80 &#x1F600; &nvlt;".getBytes(StandardCharsets.UTF_8));

        assertEquals(new Output("AT    SOURCE     HOW   RESULT         CODES\n1:1   &#65       dec   U+0041         AW004\n1:6   &#x80      hex   U+20AC         AW004,AW009\n1:12  &#x1F600;  hex   U+1F600        -\n1:22  &nvlt;     name  U+003C U+20D2  -\n", 0), ExplainCommand.run(List.of("numeric.html"), Set.of(), Map.of(), new Inputs(io)));
        assertEquals("", io.out.toString());
        assertEquals("", io.err.toString());
    }

    @Test
    void explainsCodePointPositionsAcrossSupplementaryCrAndLf() {
        FakeIo io = new FakeIo();
        io.files.put("positions.html", "😀\r&amp;\n&#65;".getBytes(StandardCharsets.UTF_8));

        assertEquals(new Output("AT   SOURCE  HOW   RESULT  CODES\n1:3  &amp;   name  U+0026  -\n2:1  &#65;   dec   U+0041  -\n", 0), ExplainCommand.run(List.of("positions.html"), Set.of(), Map.of(), new Inputs(io)));
        assertEquals("", io.out.toString());
        assertEquals("", io.err.toString());
    }

    @Test
    void explainsNoReferenceFallbackForEmptyAndBareAmpersands() {
        FakeIo io = new FakeIo();
        io.files.put("empty.html", new byte[0]);
        io.files.put("bare.html", "& x &; &".getBytes(StandardCharsets.UTF_8));

        assertEquals(new Output("no references\n", 0), ExplainCommand.run(List.of("empty.html"), Set.of(), Map.of(), new Inputs(io)));
        assertEquals(new Output("no references\n", 0), ExplainCommand.run(List.of("bare.html"), Set.of(), Map.of(), new Inputs(io)));
        assertEquals("", io.out.toString());
        assertEquals("", io.err.toString());
    }
}

package io.github.gulliby.ampwhy.cmd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.gulliby.ampwhy.base.Failure;
import io.github.gulliby.ampwhy.base.Inputs;
import io.github.gulliby.ampwhy.base.Output;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CheckSummaryTest {
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

    private static Inputs inputs(String... namesAndTexts) {
        FakeIo io = new FakeIo();
        for (int i = 0; i < namesAndTexts.length; i += 2) {
            io.files.put(namesAndTexts[i], namesAndTexts[i + 1].getBytes(StandardCharsets.UTF_8));
        }
        return new Inputs(io);
    }

    @Test
    void summaryKeepsOnlyTheLastLine() {
        Inputs problems = inputs("a.html", "x&copy y&#65", "b.html", "&lt;");
        assertEquals(new Output("2 problems in 1 file\n", 1),
                CheckCommand.run(List.of("a.html", "b.html"), Set.of("--summary"), Map.of(), problems));
        Inputs clean = inputs("a.html", "a &lt; b &amp;&amp; c &#x1F600;");
        assertEquals(new Output("ok, 4 references in 1 file\n", 0),
                CheckCommand.run(List.of("a.html"), Set.of("--summary"), Map.of(), clean));
        Inputs empty = inputs("e.html", "");
        assertEquals(new Output("ok, 0 references in 1 file\n", 0),
                CheckCommand.run(List.of("e.html"), Set.of("--summary"), Map.of(), empty));
    }

    @Test
    void summaryCombinesWithAttrAndKeepsTheFullReportOtherwise() {
        Inputs attr = inputs("q.html", "<a href=\"?a=1&copy=2&amp;b=3\">");
        assertEquals(new Output("ok, 2 references in 1 file\n", 0),
                CheckCommand.run(List.of("q.html"), Set.of("--attr", "--summary"), Map.of(), attr));
        Inputs plain = inputs("a.html", "x&copy y");
        assertEquals(new Output("a.html:1:2: AW001 missing semicolon after \"&copy\"\n1 problem in 1 file\n", 1),
                CheckCommand.run(List.of("a.html"), Set.of(), Map.of(), plain));
    }

    @Test
    void cliAcceptsTheOptionOnlyForCheck() {
        FakeIo io = new FakeIo();
        io.files.put("a.html", "x&copy y".getBytes(StandardCharsets.UTF_8));
        assertEquals(1, Cli.run(List.of("check", "--summary", "a.html"), io));
        assertEquals("1 problem in 1 file\n", io.out.toString());
        assertEquals("", io.err.toString());

        FakeIo other = new FakeIo();
        other.files.put("a.html", "x".getBytes(StandardCharsets.UTF_8));
        assertEquals(2, Cli.run(List.of("explain", "--summary", "a.html"), other));
        assertEquals("", other.out.toString());
        assertEquals("ampwhy: unknown option \"--summary\"\n", other.err.toString());

        FakeIo value = new FakeIo();
        assertEquals(2, Cli.run(List.of("check", "--summary=1", "a.html"), value));
        assertEquals("", value.out.toString());
    }

    @Test
    void missingFileIsStillAnError() {
        Inputs none = inputs();
        Failure failure = assertThrows(Failure.class,
                () -> CheckCommand.run(List.of("nofile.txt"), Set.of("--summary"), Map.of(), none));
        assertEquals("nofile.txt: cannot read the file", failure.getMessage());
    }

    @Test
    void pomDeclaresTheMainClassOfTheJar() throws IOException {
        String pom = Files.readString(Path.of("pom.xml"), StandardCharsets.UTF_8);
        assertTrue(pom.contains("<artifactId>maven-jar-plugin</artifactId>"));
        assertTrue(pom.contains("<mainClass>io.github.gulliby.ampwhy.Main</mainClass>"));
    }
}

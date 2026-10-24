package io.github.gulliby.ampwhy.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class InputsTest {
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
    void readsStdinOnceAndRejectsItsSecondName() {
        FakeIo io = new FakeIo();
        io.stdin = "x".getBytes(StandardCharsets.UTF_8);
        Inputs inputs = new Inputs(io);

        assertEquals(new Inputs.Source("<stdin>", "x"), inputs.read("-"));
        assertEquals("standard input is named twice", assertThrows(Failure.class, () -> inputs.read("-")).getMessage());
        assertEquals("", io.out.toString());
        assertEquals("", io.err.toString());
    }

    @Test
    void stdinRemainsReservedAfterFailedDecode() {
        FakeIo io = new FakeIo();
        io.stdin = new byte[] {0x61, (byte) 0xFF, 0x0A};
        Inputs inputs = new Inputs(io);

        assertEquals("<stdin>: input is not valid UTF-8", assertThrows(Failure.class, () -> inputs.read("-")).getMessage());
        assertEquals("standard input is named twice", assertThrows(Failure.class, () -> inputs.read("-")).getMessage());
        assertEquals("", io.out.toString());
        assertEquals("", io.err.toString());
    }

    @Test
    void rereadsFilesUsingTheirCurrentBytes() {
        FakeIo io = new FakeIo();
        io.files.put("a.html", "x &amp; y".getBytes(StandardCharsets.UTF_8));
        Inputs inputs = new Inputs(io);

        assertEquals(new Inputs.Source("a.html", "x &amp; y"), inputs.read("a.html"));
        io.files.put("a.html", "z".getBytes(StandardCharsets.UTF_8));
        assertEquals(new Inputs.Source("a.html", "z"), inputs.read("a.html"));
        assertEquals("", io.out.toString());
        assertEquals("", io.err.toString());
    }

    @Test
    void looksUpRawUnicodePathAndSanitizesItsDisplay() {
        FakeIo io = new FakeIo();
        io.files.put("café😀.txt", "ok".getBytes(StandardCharsets.UTF_8));

        assertEquals(new Inputs.Source("caf??.txt", "ok"), new Inputs(io).read("café😀.txt"));
        assertEquals("", io.out.toString());
        assertEquals("", io.err.toString());
    }

    @Test
    void reportsMissingPathsWithSanitizedDisplays() {
        FakeIo io = new FakeIo();
        Inputs inputs = new Inputs(io);

        assertEquals("nofile.txt: cannot read the file", assertThrows(Failure.class, () -> inputs.read("nofile.txt")).getMessage());
        assertEquals("caf?.txt: cannot read the file", assertThrows(Failure.class, () -> inputs.read("café.txt")).getMessage());
        assertEquals(": cannot read the file", assertThrows(Failure.class, () -> inputs.read("")).getMessage());
        assertEquals("", io.out.toString());
        assertEquals("", io.err.toString());
    }

    @Test
    void acceptsEmptyStdin() {
        FakeIo io = new FakeIo();
        io.stdin = new byte[] {};

        assertEquals(new Inputs.Source("<stdin>", ""), new Inputs(io).read("-"));
        assertEquals("", io.out.toString());
        assertEquals("", io.err.toString());
    }

    @Test
    void preservesBomMultibyteNulAndLineEndings() {
        FakeIo io = new FakeIo();
        String text = "\uFEFFaé€😀\u0000\r\n\r\n";
        io.files.put("a.html", "\uFEFFaé€😀\u0000\r\n\r\n".getBytes(StandardCharsets.UTF_8));

        assertEquals(new Inputs.Source("a.html", text), new Inputs(io).read("a.html"));
        assertEquals("", io.out.toString());
        assertEquals("", io.err.toString());
    }

    @Test
    void rejectsMalformedUtf8ByteSequences() {
        FakeIo io = new FakeIo();
        io.files.put("invalid-lead", new byte[] {(byte) 0xFF});
        io.files.put("overlong", new byte[] {(byte) 0xC0, (byte) 0xAF});
        io.files.put("surrogate", new byte[] {(byte) 0xED, (byte) 0xA0, (byte) 0x80});
        io.files.put("above-maximum", new byte[] {(byte) 0xF4, (byte) 0x90, (byte) 0x80, (byte) 0x80});
        io.files.put("cut-sequence", new byte[] {(byte) 0xE2, (byte) 0x82});
        io.files.put("invalid-continuation", new byte[] {(byte) 0xE2, 0x28, (byte) 0xA1});
        Inputs inputs = new Inputs(io);

        assertEquals("invalid-lead: input is not valid UTF-8", assertThrows(Failure.class, () -> inputs.read("invalid-lead")).getMessage());
        assertEquals("overlong: input is not valid UTF-8", assertThrows(Failure.class, () -> inputs.read("overlong")).getMessage());
        assertEquals("surrogate: input is not valid UTF-8", assertThrows(Failure.class, () -> inputs.read("surrogate")).getMessage());
        assertEquals("above-maximum: input is not valid UTF-8", assertThrows(Failure.class, () -> inputs.read("above-maximum")).getMessage());
        assertEquals("cut-sequence: input is not valid UTF-8", assertThrows(Failure.class, () -> inputs.read("cut-sequence")).getMessage());
        assertEquals("invalid-continuation: input is not valid UTF-8", assertThrows(Failure.class, () -> inputs.read("invalid-continuation")).getMessage());
        assertEquals("", io.out.toString());
        assertEquals("", io.err.toString());
    }

    @Test
    void rejectsMalformedStdinBytes() {
        FakeIo io = new FakeIo();
        io.stdin = new byte[] {0x61, (byte) 0xFF, 0x0A};

        assertEquals("<stdin>: input is not valid UTF-8", assertThrows(Failure.class, () -> new Inputs(io).read("-")).getMessage());
        assertEquals("", io.out.toString());
        assertEquals("", io.err.toString());
    }
}

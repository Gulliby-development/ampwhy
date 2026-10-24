package io.github.gulliby.ampwhy.base;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

public final class Inputs {
    private final Io io;
    private boolean stdinNamed;

    public Inputs(Io io) {
        this.io = io;
    }

    public Source read(String arg) {
        boolean stdin = arg.equals("-");
        String display = stdin ? "<stdin>" : Text.plain(arg);
        if (stdin && stdinNamed) {
            throw new Failure("standard input is named twice");
        }
        if (stdin) {
            stdinNamed = true;
        }

        byte[] bytes;
        try {
            bytes = stdin ? io.readStdin() : io.readFile(arg);
        } catch (IOException exception) {
            throw new Failure(display + ": cannot read the file");
        }
        return new Source(display, decode(bytes, display));
    }

    private static String decode(byte[] bytes, String display) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException exception) {
            throw new Failure(display + ": input is not valid UTF-8");
        }
    }

    public interface Io {
        byte[] readStdin() throws IOException;

        byte[] readFile(String path) throws IOException;

        void out(String text);

        void err(String text);
    }

    public record Source(String display, String text) {
    }
}

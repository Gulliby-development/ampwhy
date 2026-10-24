package io.github.gulliby.ampwhy;

import io.github.gulliby.ampwhy.base.Inputs;
import io.github.gulliby.ampwhy.cmd.Cli;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        Inputs.Io io = new Inputs.Io() {
            @Override
            public byte[] readStdin() throws IOException {
                return System.in.readAllBytes();
            }

            @Override
            public byte[] readFile(String path) throws IOException {
                try {
                    return Files.readAllBytes(Path.of(path));
                } catch (InvalidPathException exception) {
                    throw new IOException(exception);
                }
            }

            @Override
            public void out(String text) {
                try {
                    System.out.write(text.getBytes(StandardCharsets.UTF_8));
                    System.out.flush();
                } catch (IOException exception) {
                    throw new UncheckedIOException(exception);
                }
            }

            @Override
            public void err(String text) {
                try {
                    System.err.write(text.getBytes(StandardCharsets.UTF_8));
                    System.err.flush();
                } catch (IOException exception) {
                    throw new UncheckedIOException(exception);
                }
            }
        };
        System.exit(Cli.run(java.util.Arrays.asList(args), io));
    }
}

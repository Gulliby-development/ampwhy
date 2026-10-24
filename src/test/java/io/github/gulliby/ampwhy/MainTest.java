package io.github.gulliby.ampwhy;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTest {
    @Test
    void readmeHasRequiredHeadings() throws Exception {
        List<String> lines = Files.readAllLines(Path.of("README.md"), StandardCharsets.UTF_8);

        assertTrue(lines.contains("## Install"));
        assertTrue(lines.contains("## Usage"));
        assertTrue(lines.contains("## References"));
        assertTrue(lines.contains("## Development"));
    }

    @Test
    void malformedUtf8OnStdinFailsInExecutable() throws Exception {
        Path java = Path.of(System.getProperty("java.home"), "bin", "java");
        if (!Files.exists(java)) {
            java = java.resolveSibling("java.exe");
        }
        Process process = new ProcessBuilder(
                java.toString(), "-cp", "target/classes", "io.github.gulliby.ampwhy.Main", "check", "-")
                .directory(Path.of(System.getProperty("user.dir")).toFile())
                .start();
        byte[] stdout;
        byte[] stderr;
        int exitCode;
        try {
            process.getOutputStream().write(new byte[] {0x61, (byte) 0xFF, 0x0A});
            process.getOutputStream().close();
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), "process should finish within 30 seconds");
            exitCode = process.exitValue();
            stdout = process.getInputStream().readAllBytes();
            stderr = process.getErrorStream().readAllBytes();
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly().waitFor(30, TimeUnit.SECONDS);
            }
            process.getOutputStream().close();
            process.getInputStream().close();
            process.getErrorStream().close();
        }

        assertEquals(2, exitCode);
        assertArrayEquals(new byte[0], stdout);
        assertArrayEquals(
                "ampwhy: <stdin>: input is not valid UTF-8\n".getBytes(StandardCharsets.UTF_8),
                stderr);
    }
}

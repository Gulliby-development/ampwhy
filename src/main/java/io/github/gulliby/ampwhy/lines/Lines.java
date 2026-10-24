package io.github.gulliby.ampwhy.lines;

public final class Lines {
    private final int[] lines;
    private final int[] cols;

    public Lines(int[] text) {
        lines = new int[text.length + 1];
        cols = new int[text.length + 1];

        int line = 1;
        int col = 1;
        for (int index = 0; index < text.length; index++) {
            lines[index] = line;
            cols[index] = col;
            if (text[index] == '\n') {
                line++;
                col = 1;
            } else {
                col++;
            }
        }
        lines[text.length] = line;
        cols[text.length] = col;
    }

    public int line(int index) {
        return lines[index];
    }

    public int col(int index) {
        return cols[index];
    }
}

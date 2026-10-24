package io.github.gulliby.ampwhy.lines;

import java.util.List;

public final class Grid {
    private Grid() {
    }

    public static String render(List<List<String>> rows) {
        if (rows.isEmpty()) {
            return "";
        }

        int columns = rows.get(0).size();
        int[] widths = new int[columns];
        for (List<String> row : rows) {
            for (int column = 0; column < columns; column++) {
                widths[column] = Math.max(widths[column], row.get(column).length());
            }
        }

        StringBuilder output = new StringBuilder();
        for (List<String> row : rows) {
            for (int column = 0; column < columns; column++) {
                if (column > 0) {
                    output.append("  ");
                }
                String cell = row.get(column);
                output.append(cell);
                if (column < columns - 1) {
                    output.append(" ".repeat(widths[column] - cell.length()));
                }
            }
            output.append('\n');
        }
        return output.toString();
    }
}

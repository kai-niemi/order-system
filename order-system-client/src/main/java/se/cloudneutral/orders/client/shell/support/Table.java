package se.cloudneutral.orders.client.shell.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Plain text table with left-aligned columns sized to their widest cell.
 */
public class Table {
    private final List<String[]> rows = new ArrayList<>();

    public Table(String... header) {
        rows.add(header);
    }

    public void addRow(String... cells) {
        rows.add(cells);
    }

    public String render() {
        int[] widths = new int[rows.getFirst().length];
        rows.forEach(row -> {
            for (int i = 0; i < row.length; i++) {
                widths[i] = Math.max(widths[i], row[i].length());
            }
        });

        String separator = Arrays.stream(widths)
                .mapToObj("-"::repeat)
                .collect(Collectors.joining("-+-"));

        List<String> lines = new ArrayList<>();
        rows.forEach(row -> {
            lines.add(format(row, widths));
            if (lines.size() == 1) {
                lines.add(separator);
            }
        });
        return String.join(System.lineSeparator(), lines);
    }

    private static String format(String[] row, int[] widths) {
        List<String> cells = new ArrayList<>();
        for (int i = 0; i < row.length; i++) {
            cells.add(String.format("%-" + widths[i] + "s", row[i]));
        }
        return String.join(" | ", cells).stripTrailing();
    }
}

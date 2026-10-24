package io.github.gulliby.ampwhy.scan;

import io.github.gulliby.ampwhy.base.Text;
import io.github.gulliby.ampwhy.lines.Grid;
import io.github.gulliby.ampwhy.lines.Lines;
import io.github.gulliby.ampwhy.read.Reading;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScannerTest {
    @Test
    void scansOrderedNameUnknownAndNoneReadings() {
        List<Occurrence> occurrences = Scanner.scan(Text.cps("&&amp;&zzz;&#x;"), false);

        assertEquals(3, occurrences.size());
        assertEquals(1, occurrences.get(0).start());
        Reading name = occurrences.get(0).reading();
        assertEquals(6, name.end());
        assertEquals("name", name.kind());
        assertArrayEquals(new int[]{38}, name.result());
        assertEquals(List.of(), name.codes());
        assertFalse(name.mapped());

        assertEquals(6, occurrences.get(1).start());
        Reading unknown = occurrences.get(1).reading();
        assertEquals(10, unknown.end());
        assertEquals("unknown", unknown.kind());
        assertNull(unknown.result());
        assertEquals(List.of("AW002"), unknown.codes());
        assertFalse(unknown.mapped());

        assertEquals(11, occurrences.get(2).start());
        Reading none = occurrences.get(2).reading();
        assertEquals(14, none.end());
        assertEquals("none", none.kind());
        assertNull(none.result());
        assertEquals(List.of("AW003"), none.codes());
        assertFalse(none.mapped());
    }

    @Test
    void resumesAfterPartialLegacyName() {
        List<Occurrence> occurrences = Scanner.scan(Text.cps("&notit;&amp;"), false);

        assertEquals(2, occurrences.size());
        assertEquals(0, occurrences.get(0).start());
        Reading legacy = occurrences.get(0).reading();
        assertEquals(4, legacy.end());
        assertEquals("legacy", legacy.kind());
        assertArrayEquals(new int[]{172}, legacy.result());
        assertEquals(List.of("AW001"), legacy.codes());
        assertFalse(legacy.mapped());

        assertEquals(7, occurrences.get(1).start());
        Reading name = occurrences.get(1).reading();
        assertEquals(12, name.end());
        assertEquals("name", name.kind());
        assertArrayEquals(new int[]{38}, name.result());
        assertEquals(List.of(), name.codes());
        assertFalse(name.mapped());
    }

    @Test
    void keepsNumericReadingsAuthoritative() {
        List<Occurrence> occurrences = Scanner.scan(Text.cps("&#65;&#x80;"), false);

        assertEquals(2, occurrences.size());
        assertEquals(0, occurrences.get(0).start());
        Reading decimal = occurrences.get(0).reading();
        assertEquals(5, decimal.end());
        assertEquals("dec", decimal.kind());
        assertArrayEquals(new int[]{65}, decimal.result());
        assertEquals(List.of(), decimal.codes());
        assertFalse(decimal.mapped());

        assertEquals(5, occurrences.get(1).start());
        Reading hexadecimal = occurrences.get(1).reading();
        assertEquals(11, hexadecimal.end());
        assertEquals("hex", hexadecimal.kind());
        assertArrayEquals(new int[]{8364}, hexadecimal.result());
        assertEquals(List.of("AW009"), hexadecimal.codes());
        org.junit.jupiter.api.Assertions.assertTrue(hexadecimal.mapped());
    }

    @Test
    void skipsPlainAmpersandsAndUsesCodePointOffsets() {
        assertEquals(List.of(), Scanner.scan(Text.cps("& x &; &"), false));
        assertEquals(List.of(), Scanner.scan(Text.cps(""), false));

        List<Occurrence> occurrences = Scanner.scan(Text.cps("😀&amp;"), false);
        assertEquals(1, occurrences.size());
        assertEquals(1, occurrences.get(0).start());
        Reading name = occurrences.get(0).reading();
        assertEquals(6, name.end());
        assertEquals("name", name.kind());
        assertArrayEquals(new int[]{38}, name.result());
        assertEquals(List.of(), name.codes());
        assertFalse(name.mapped());
    }

    @Test
    void returnsImmutableListsForBothAttributeModes() {
        List<Occurrence> textMode = Scanner.scan(Text.cps("&ampx"), false);
        assertEquals(1, textMode.size());
        assertEquals(0, textMode.get(0).start());
        Reading legacy = textMode.get(0).reading();
        assertEquals(4, legacy.end());
        assertEquals("legacy", legacy.kind());
        assertArrayEquals(new int[]{38}, legacy.result());
        assertEquals(List.of("AW001"), legacy.codes());
        assertFalse(legacy.mapped());

        List<Occurrence> attributeMode = Scanner.scan(Text.cps("&ampx"), true);
        assertEquals(1, attributeMode.size());
        assertEquals(0, attributeMode.get(0).start());
        Reading kept = attributeMode.get(0).reading();
        assertEquals(4, kept.end());
        assertEquals("kept", kept.kind());
        assertNull(kept.result());
        assertEquals(List.of(), kept.codes());
        assertFalse(kept.mapped());

        assertThrows(UnsupportedOperationException.class, () -> textMode.add(null));
        assertThrows(UnsupportedOperationException.class, textMode::clear);
        assertThrows(UnsupportedOperationException.class, () -> Scanner.scan(Text.cps(""), false).add(null));
    }

    @Test
    void countsSupplementaryCharactersAndCrBeforeLf() {
        Lines lines = new Lines(Text.cps("é😀\r&\n&"));

        assertEquals(1, lines.line(3));
        assertEquals(4, lines.col(3));
        assertEquals(1, lines.line(4));
        assertEquals(5, lines.col(4));
        assertEquals(2, lines.line(5));
        assertEquals(1, lines.col(5));
    }

    @Test
    void treatsBomAndNulAsColumnsAndCountsEmptyLines() {
        Lines ordinary = new Lines(Text.cps("\uFEFF\u0000&"));
        assertEquals(1, ordinary.line(2));
        assertEquals(3, ordinary.col(2));

        Lines emptyLines = new Lines(Text.cps("\n\n&"));
        assertEquals(3, emptyLines.line(2));
        assertEquals(1, emptyLines.col(2));
    }

    @Test
    void supportsEmptyTextAndPositionAfterTrailingLf() {
        Lines empty = new Lines(Text.cps(""));
        assertEquals(1, empty.line(0));
        assertEquals(1, empty.col(0));

        Lines trailingLf = new Lines(Text.cps("a\n"));
        assertEquals(2, trailingLf.line(2));
        assertEquals(1, trailingLf.col(2));
    }

    @Test
    void rendersThreeColumnTablesWithExactPadding() {
        assertEquals("NAME    FORM\nx       plain\nlonger  l\n",
                Grid.render(List.of(List.of("NAME", "FORM"), List.of("x", "plain"), List.of("longer", "l"))));
        assertEquals("AT   SOURCE  CODES\n1:1  &amp;   -\n",
                Grid.render(List.of(List.of("AT", "SOURCE", "CODES"), List.of("1:1", "&amp;", "-"))));
    }

    @Test
    void rendersSingleColumnAndEmptyTables() {
        assertEquals("HEADER\nx\n", Grid.render(List.of(List.of("HEADER"), List.of("x"))));
        assertEquals("", Grid.render(List.of()));
    }
}

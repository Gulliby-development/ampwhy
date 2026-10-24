package io.github.gulliby.ampwhy.read;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.gulliby.ampwhy.base.Text;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ReaderNamedTest {
    @Test
    void readingCopiesAndFreezesOrderedCodesAndAllowsNullResult() {
        List<String> supplied = new ArrayList<>(List.of("AW004", "AW005"));
        Reading reading = new Reading("dec", 4, null, supplied, false);
        supplied.add("AW009");
        assertEquals(List.of("AW004", "AW005"), reading.codes());
        assertNull(reading.result());
        assertThrows(UnsupportedOperationException.class, () -> reading.codes().add("AW009"));

        Reading empty = new Reading("name", 2, new int[]{38}, List.of(), false);
        assertEquals(List.of(), empty.codes());
        assertThrows(UnsupportedOperationException.class, () -> empty.codes().add("AW001"));
    }

    @ParameterizedTest
    @CsvSource({
            "'& x',false", "'& x',true", "'&;',false", "'&;',true",
            "'&',false", "'&',true", "'&&amp;',false", "'&&amp;',true",
            "'&é',false", "'&é',true", "'&٣',false", "'&٣',true",
            "'&Ａ',false", "'&Ａ',true"
    })
    void returnsNullWhenAmpersandIsNotFollowedByAsciiAlphanumericOrHash(String input, boolean attr) {
        assertNull(Reader.read(Text.cps(input), 0, attr));
    }

    @Test
    void usesCodePointIndexesAndDoesNotLookBehind() {
        Reading afterSupplementary = Reader.read(Text.cps("😀&amp;"), 1, false);
        assertEquals("name", afterSupplementary.kind());
        assertEquals(6, afterSupplementary.end());
        assertArrayEquals(new int[]{38}, afterSupplementary.result());
        assertEquals(List.of(), afterSupplementary.codes());
        assertFalse(afterSupplementary.mapped());

        Reading afterAmpersand = Reader.read(Text.cps("&&amp;"), 1, false);
        assertEquals("name", afterAmpersand.kind());
        assertEquals(6, afterAmpersand.end());
        assertArrayEquals(new int[]{38}, afterAmpersand.result());
        assertEquals(List.of(), afterAmpersand.codes());
        assertFalse(afterAmpersand.mapped());
    }

    @ParameterizedTest
    @CsvSource({
            "'&ltri;',name,6,true,9667,-",
            "'&ltri',legacy,3,true,60,AW001",
            "'&lt;',name,4,true,60,-",
            "'&notin;',name,7,true,8713,-",
            "'&notin',legacy,4,true,172,AW001",
            "'&notit;',legacy,4,true,172,AW001",
            "'&parallel',legacy,5,true,182,AW001",
            "'&ampx',legacy,4,true,38,AW001",
            "'&amp1;',legacy,4,true,38,AW001",
            "'&AMP',legacy,4,true,38,AW001",
            "'&copyright;',legacy,5,true,169,AW001",
            "'&times;',name,7,true,215,-",
            "'&times 2',legacy,6,true,215,AW001",
            "'&hearts;',name,8,true,9829,-"
    })
    void selectsTheLongestEligibleNamedCandidate(String input, String kind, int end,
                                                   boolean hasResult, int expectedResult,
                                                   String expectedCode) {
        Reading reading = Reader.read(Text.cps(input), 0, false);
        assertEquals(kind, reading.kind());
        assertEquals(end, reading.end());
        if (hasResult) {
            assertArrayEquals(new int[]{expectedResult}, reading.result());
        } else {
            assertNull(reading.result());
        }
        assertEquals(expectedCode.equals("-") ? List.of() : List.of(expectedCode), reading.codes());
        assertFalse(reading.mapped());
    }

    @ParameterizedTest
    @CsvSource({
            "'&Amp;',4,AW002",
            "'&zzz;',4,AW002",
            "'&1;',2,AW002",
            "'&Copy',5,-",
            "'&hearts',7,-",
            "'&zzz',4,-",
            "'&zzz ;',4,-"
    })
    void unknownNamesConsumeOnlyTheAsciiRunAndReportOnlyAnImmediateSemicolon(
            String input, int end, String expectedCode) {
        Reading reading = Reader.read(Text.cps(input), 0, false);
        assertEquals("unknown", reading.kind());
        assertEquals(end, reading.end());
        assertNull(reading.result());
        assertEquals(expectedCode.equals("-") ? List.of() : List.of(expectedCode), reading.codes());
        assertFalse(reading.mapped());
    }

    @ParameterizedTest
    @CsvSource({
            "'&Amp;',false,4,AW002", "'&Amp;',true,4,AW002",
            "'&zzz;',false,4,AW002", "'&zzz;',true,4,AW002",
            "'&1;',false,2,AW002", "'&1;',true,2,AW002",
            "'&Copy',false,5,-", "'&Copy',true,5,-",
            "'&hearts',false,7,-", "'&hearts',true,7,-",
            "'&zzz',false,4,-", "'&zzz',true,4,-",
            "'&zzz ;',false,4,-", "'&zzz ;',true,4,-"
    })
    void unknownNameSemicolonBehaviorIsTheSameInAttributeMode(
            String input, boolean attr, int end, String expectedCode) {
        Reading reading = Reader.read(Text.cps(input), 0, attr);
        assertEquals("unknown", reading.kind());
        assertEquals(end, reading.end());
        assertNull(reading.result());
        assertEquals(expectedCode.equals("-") ? List.of() : List.of(expectedCode), reading.codes());
        assertFalse(reading.mapped());
    }

    @ParameterizedTest
    @CsvSource({
            "'&ampx',4", "'&amp1;',4", "'&amp=',4", "'&notit;',4"
    })
    void attributeModeKeepsSelectedLegacyNamesBeforeAsciiSuffixes(String input, int end) {
        Reading reading = Reader.read(Text.cps(input), 0, true);
        assertEquals("kept", reading.kind());
        assertEquals(end, reading.end());
        assertNull(reading.result());
        assertEquals(List.of(), reading.codes());
        assertFalse(reading.mapped());
    }

    @ParameterizedTest
    @CsvSource({
            "'&amp;x',name,5,true,38,-",
            "'&amp',legacy,4,true,38,AW001",
            "'&ampé',legacy,4,true,38,AW001",
            "'&amp٣',legacy,4,true,38,AW001",
            "'&ampＡ',legacy,4,true,38,AW001"
    })
    void attributeModeUsesSemicolonsAndAsciiOnlySuffixBoundaries(
            String input, String kind, int end, boolean hasResult, int expectedResult,
            String expectedCode) {
        Reading reading = Reader.read(Text.cps(input), 0, true);
        assertEquals(kind, reading.kind());
        assertEquals(end, reading.end());
        if (hasResult) {
            assertArrayEquals(new int[]{expectedResult}, reading.result());
        } else {
            assertNull(reading.result());
        }
        assertEquals(expectedCode.equals("-") ? List.of() : List.of(expectedCode), reading.codes());
        assertFalse(reading.mapped());
    }

    @Test
    void attributeModeDoesNotKeepLegacyNameBeforeSpaceOrAtEof() {
        Reading reading = Reader.read(Text.cps("&amp "), 0, true);
        assertEquals("legacy", reading.kind());
        assertEquals(4, reading.end());
        assertArrayEquals(new int[]{38}, reading.result());
        assertEquals(List.of("AW001"), reading.codes());
        assertFalse(reading.mapped());
    }

    @Test
    void nvltPreservesItsOrderedTwoCodePointResult() {
        Reading reading = Reader.read(Text.cps("&nvlt;"), 0, false);
        assertEquals("name", reading.kind());
        assertEquals(6, reading.end());
        assertArrayEquals(new int[]{60, 8402}, reading.result());
        assertEquals(List.of(), reading.codes());
        assertFalse(reading.mapped());
    }
}

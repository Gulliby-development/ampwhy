package io.github.gulliby.ampwhy.base;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TextTest {
    @Test
    void sharedFailureAndOutputContracts() {
        Failure failure = new Failure("broken");
        Output output = new Output("x\r\n", 1);

        assertEquals("broken", failure.getMessage());
        assertTrue(failure instanceof RuntimeException);
        assertEquals("x\r\n", output.out());
        assertEquals(1, output.code());
    }

    @Test
    void sanitizesByCodePointAndQuotesWithoutEscapingQuotes() {
        assertEquals("A\"???? ~", Text.plain("A\"é😀\n\u007F ~"));
        assertEquals("\"a\"b\"", Text.show("a\"b"));
        assertEquals("\"\"", Text.show(""));
    }

    @Test
    void formatsPluralCountsAndHexadecimalCodePoints() {
        assertEquals("1 file", Text.plural(1, "file"));
        assertEquals("0 files", Text.plural(0, "file"));
        assertEquals("2 files", Text.plural(2, "file"));
        assertEquals("U+0026", Text.hex(0x26));
        assertEquals("U+1F600", Text.hex(0x1F600));
    }

    @Test
    void convertsAndSlicesCodePoints() {
        assertArrayEquals(new int[]{233, 128512, 65279, 13, 10, 0}, Text.cps("é😀\uFEFF\r\n\u0000"));
        assertEquals("😀", Text.str(new int[]{65, 128512, 66}, 1, 2));
        assertEquals("", Text.str(new int[]{65}, 0, 0));
    }

    @Test
    void classifiesOnlyAsciiDigitsHexDigitsAndLetters() {
        assertTrue(Text.isDigit(48));
        assertTrue(Text.isDigit(57));
        assertFalse(Text.isDigit(47));
        assertFalse(Text.isDigit(58));
        assertFalse(Text.isDigit(0x0663));
        assertTrue(Text.isHex(65));
        assertTrue(Text.isHex(70));
        assertTrue(Text.isHex(97));
        assertTrue(Text.isHex(102));
        assertFalse(Text.isHex(71));
        assertFalse(Text.isHex(103));
        assertTrue(Text.isAlnum(90));
        assertTrue(Text.isAlnum(122));
        assertTrue(Text.isAlnum(48));
        assertFalse(Text.isAlnum(64));
        assertFalse(Text.isAlnum(91));
        assertFalse(Text.isAlnum(96));
        assertFalse(Text.isAlnum(123));
        assertFalse(Text.isAlnum(0xE9));
        assertFalse(Text.isAlnum(0xFF21));
    }

    @Test
    void parsesOptionsLeftToRightAndKeepsLastValue() {
        Options.Parsed parsed = Options.parse(
                List.of("f", "--attr", "--attr", "-", "-5", "--mode=first", "--mode", "last"),
                Set.of("--attr"), Set.of("--mode"));

        assertEquals(Set.of("--attr"), parsed.flags());
        assertEquals(Map.of("--mode", "last"), parsed.values());
        assertEquals(List.of("f", "-", "-5"), parsed.args());
    }

    @Test
    void parsesFirstEqualsSuffixAndEmptyValue() {
        assertEquals(Map.of("--mode", "a=b"),
                Options.parse(List.of("--mode=a=b"), Set.of("--attr"), Set.of("--mode")).values());
        assertEquals(Map.of("--mode", ""),
                Options.parse(List.of("--mode="), Set.of("--attr"), Set.of("--mode")).values());
    }

    @Test
    void valuedOptionConsumesOptionLookingValueAndPrecedesFlag() {
        Options.Parsed optionLookingValue = Options.parse(
                List.of("--mode", "--attr"), Set.of("--attr"), Set.of("--mode"));
        Options.Parsed overlappingOption = Options.parse(
                List.of("--mode", "named"), Set.of("--mode"), Set.of("--mode"));

        assertEquals(Set.of(), optionLookingValue.flags());
        assertEquals(Map.of("--mode", "--attr"), optionLookingValue.values());
        assertEquals(Set.of(), overlappingOption.flags());
        assertEquals(Map.of("--mode", "named"), overlappingOption.values());
        assertEquals(List.of(), overlappingOption.args());
    }

    @Test
    void missingValuedOptionReportsExactFailure() {
        Failure failure = assertThrows(Failure.class,
                () -> Options.parse(List.of("--mode"), Set.of("--attr"), Set.of("--mode")));

        assertEquals("--mode needs a value", failure.getMessage());
    }

    @Test
    void unknownOptionsAreSanitizedAndFirstErrorWins() {
        assertEquals("unknown option \"--\"", assertThrows(Failure.class,
                () -> Options.parse(List.of("--"), Set.of("--attr"), Set.of("--mode"))).getMessage());
        assertEquals("unknown option \"--help\"", assertThrows(Failure.class,
                () -> Options.parse(List.of("--help"), Set.of("--attr"), Set.of("--mode"))).getMessage());
        assertEquals("unknown option \"--attr=1\"", assertThrows(Failure.class,
                () -> Options.parse(List.of("--attr=1"), Set.of("--attr"), Set.of("--mode"))).getMessage());
        assertEquals("unknown option \"--??\"", assertThrows(Failure.class,
                () -> Options.parse(List.of("--é😀"), Set.of("--attr"), Set.of("--mode"))).getMessage());
        assertEquals("unknown option \"--bad\"", assertThrows(Failure.class,
                () -> Options.parse(List.of("--bad", "--mode"), Set.of("--attr"), Set.of("--mode"))).getMessage());
    }

    @Test
    void parsedResultsAreImmutableAndEmptyCollectionsAreNonNull() {
        Options.Parsed parsed = Options.parse(
                List.of("--attr", "--mode=named", "a.html"), Set.of("--attr"), Set.of("--mode"));
        Options.Parsed empty = Options.parse(List.of(), Set.of("--attr"), Set.of("--mode"));

        assertThrows(UnsupportedOperationException.class, () -> parsed.flags().add("--other"));
        assertThrows(UnsupportedOperationException.class, () -> parsed.values().put("--other", "x"));
        assertThrows(UnsupportedOperationException.class, () -> parsed.args().add("b.html"));
        assertNotNull(empty.flags());
        assertNotNull(empty.values());
        assertNotNull(empty.args());
        assertEquals(Set.of(), empty.flags());
        assertEquals(Map.of(), empty.values());
        assertEquals(List.of(), empty.args());
    }

    @Test
    void directlyConstructedParsedDefensivelyCopiesCollections() {
        Set<String> flags = new HashSet<>(Set.of("--attr"));
        Map<String, String> values = new HashMap<>(Map.of("--mode", "named"));
        List<String> args = new ArrayList<>(List.of("a.html"));
        Options.Parsed parsed = new Options.Parsed(flags, values, args);

        flags.add("--other");
        values.put("--other", "x");
        args.add("b.html");
        assertEquals(Set.of("--attr"), parsed.flags());
        assertEquals(Map.of("--mode", "named"), parsed.values());
        assertEquals(List.of("a.html"), parsed.args());
        assertThrows(UnsupportedOperationException.class, () -> parsed.flags().add("--other"));
        assertThrows(UnsupportedOperationException.class, () -> parsed.values().put("--other", "x"));
        assertThrows(UnsupportedOperationException.class, () -> parsed.args().add("b.html"));
    }
}

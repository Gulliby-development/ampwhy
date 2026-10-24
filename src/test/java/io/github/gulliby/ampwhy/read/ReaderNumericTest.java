package io.github.gulliby.ampwhy.read;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ReaderNumericTest {
    @ParameterizedTest
    @CsvSource(value = {
            "&#;|2|false",
            "&#;|2|true",
            "&#|2|true",
            "&#|2|false",
            "&#x;|3|false",
            "&#x;|3|true",
            "&#X|3|true",
            "&#X|3|false",
            "&#xg;|3|false",
            "&#xg;|3|true",
            "&#٣;|2|true",
            "&#٣;|2|false",
            "&#xＡ;|3|false",
            "&#xＡ;|3|true"
    }, delimiter = '|')
    void missingDigitsConsumeOnlyTheNumericPrefix(String input, int end, boolean attr) {
        Reading reading = Reader.read(input.codePoints().toArray(), 0, attr);
        assertEquals("none", reading.kind());
        assertEquals(end, reading.end());
        assertNull(reading.result());
        assertEquals(List.of("AW003"), reading.codes());
        assertFalse(reading.mapped());
    }

    @ParameterizedTest
    @CsvSource(value = {
            "&#65;|5|dec|65|-|false|false",
            "&#65;|5|dec|65|-|false|true",
            "&#65|4|dec|65|AW004|false|true",
            "&#65|4|dec|65|AW004|false|false",
            "&#65a|4|dec|65|AW004|false|false",
            "&#65a|4|dec|65|AW004|false|true",
            "&#x41g|5|hex|65|AW004|false|true",
            "&#x41g|5|hex|65|AW004|false|false",
            "&#x41a;|7|hex|1050|-|false|false",
            "&#x41a;|7|hex|1050|-|false|true",
            "&#X4a;|6|hex|74|-|false|true",
            "&#X4a;|6|hex|74|-|false|false"
    }, delimiter = '|')
    void decimalAndHexadecimalDigitsStopAtTheirAsciiBoundaries(
            String input, int end, String kind, int result, String expectedCodes,
            boolean mapped, boolean attr) {
        Reading reading = Reader.read(input.codePoints().toArray(), 0, attr);
        assertEquals(kind, reading.kind());
        assertEquals(end, reading.end());
        assertArrayEquals(new int[]{result}, reading.result());
        assertEquals(expectedCodes.equals("-") ? List.of() : List.of(expectedCodes.split(",")), reading.codes());
        assertEquals(mapped, reading.mapped());
    }

    @ParameterizedTest
    @CsvSource(value = {
            "&#00000000065;|14|dec|65|-|false",
            "&#1114111;|10|dec|1114111|AW008|false",
            "&#1114111;|10|dec|1114111|AW008|true",
            "&#1114112;|10|dec|65533|AW006|false",
            "&#x110000;|10|hex|65533|AW006|false",
            "&#x110000;|10|hex|65533|AW006|true",
            "&#99999999999999999999;|23|dec|65533|AW006|false",
            "&#xFFFFFFFFFFFFFFFFFFFF;|24|hex|65533|AW006|false",
            "&#xFFFFFFFFFFFFFFFFFFFF;|24|hex|65533|AW006|true",
            "&#99999999999999999999|22|dec|65533|AW004,AW006|false"
    }, delimiter = '|')
    void leadingZerosBoundariesAndLongRunsSaturateWithoutLosingConsumption(
            String input, int end, String kind, int result, String expectedCodes, boolean attr) {
        Reading reading = Reader.read(input.codePoints().toArray(), 0, attr);
        assertEquals(kind, reading.kind());
        assertEquals(end, reading.end());
        assertArrayEquals(new int[]{result}, reading.result());
        assertEquals(expectedCodes.equals("-") ? List.of() : List.of(expectedCodes.split(",")), reading.codes());
        assertFalse(reading.mapped());
    }

    @ParameterizedTest
    @CsvSource(value = {
            "&#0;|4|dec|AW005|false",
            "&#x0;|5|hex|AW005|true",
            "&#0|3|dec|AW004,AW005|false"
    }, delimiter = '|')
    void zeroReplacementAndMissingSemicolonCodesRemainOrdered(
            String input, int end, String kind, String expectedCodes, boolean attr) {
        Reading reading = Reader.read(input.codePoints().toArray(), 0, attr);
        assertEquals(kind, reading.kind());
        assertEquals(end, reading.end());
        assertArrayEquals(new int[]{65533}, reading.result());
        assertEquals(List.of(expectedCodes.split(",")), reading.codes());
        assertFalse(reading.mapped());
    }

    @ParameterizedTest
    @CsvSource(value = {
            "&#xD7FF;|8|55295|-|false",
            "&#xD800;|8|65533|AW007|true",
            "&#xDFFF;|8|65533|AW007|false",
            "&#xE000;|8|57344|-|true"
    }, delimiter = '|')
    void surrogateBoundariesUseDelegatedReplacement(
            String input, int end, int result, String expectedCodes, boolean attr) {
        Reading reading = Reader.read(input.codePoints().toArray(), 0, attr);
        assertEquals("hex", reading.kind());
        assertEquals(end, reading.end());
        assertArrayEquals(new int[]{result}, reading.result());
        assertEquals(expectedCodes.equals("-") ? List.of() : List.of(expectedCodes.split(",")), reading.codes());
        assertFalse(reading.mapped());
    }

    @ParameterizedTest
    @CsvSource(value = {
            "&#xFDCF;|8|64975|-",
            "&#xFDD0;|8|64976|AW008",
            "&#xFDEF;|8|65007|AW008",
            "&#xFDF0;|8|65008|-",
            "&#xFFFE;|8|65534|AW008",
            "&#xFFFF;|8|65535|AW008",
            "&#x10FFFE;|10|1114110|AW008"
    }, delimiter = '|')
    void noncharacterBoundariesKeepTheirCodePoints(
            String input, int end, int result, String expectedCodes) {
        Reading reading = Reader.read(input.codePoints().toArray(), 0, false);
        assertEquals("hex", reading.kind());
        assertEquals(end, reading.end());
        assertArrayEquals(new int[]{result}, reading.result());
        assertEquals(expectedCodes.equals("-") ? List.of() : List.of(expectedCodes.split(",")), reading.codes());
        assertFalse(reading.mapped());
    }

    @ParameterizedTest
    @CsvSource(value = {
            "&#9;|4|dec|9|-|false|false",
            "&#10;|5|dec|10|-|false|true",
            "&#11;|5|dec|11|AW009|false|false",
            "&#12;|5|dec|12|-|false|true",
            "&#13;|5|dec|13|AW009|false|false",
            "&#x1F;|6|hex|31|AW009|false|true",
            "&#x7F;|6|hex|127|AW009|false|false",
            "&#x80;|6|hex|8364|AW009|true|true",
            "&#x80|5|hex|8364|AW004,AW009|true|false",
            "&#x80|5|hex|8364|AW004,AW009|true|true",
            "&#x81;|6|hex|129|AW009|false|false",
            "&#150;|6|dec|8211|AW009|true|true",
            "&#x9D;|6|hex|157|AW009|false|false",
            "&#x9F;|6|hex|376|AW009|true|true"
    }, delimiter = '|')
    void controlsExemptionsAndWindows1252MappingsArePropagated(
            String input, int end, String kind, int result, String expectedCodes, boolean mapped, boolean attr) {
        Reading reading = Reader.read(input.codePoints().toArray(), 0, attr);
        assertEquals(kind, reading.kind());
        assertEquals(end, reading.end());
        assertArrayEquals(new int[]{result}, reading.result());
        assertEquals(expectedCodes.equals("-") ? List.of() : List.of(expectedCodes.split(",")), reading.codes());
        assertEquals(mapped, reading.mapped());
    }

    @ParameterizedTest
    @CsvSource(value = {
            "&#x20;|6|hex|32|false",
            "&#126;|6|dec|126|true",
            "&#x10000;|9|hex|65536|false",
            "&#x10FFFD;|10|hex|1114109|true"
    }, delimiter = '|')
    void ordinaryAndSupplementaryValuesPassThrough(
            String input, int end, String kind, int result, boolean attr) {
        Reading reading = Reader.read(input.codePoints().toArray(), 0, attr);
        assertEquals(kind, reading.kind());
        assertEquals(end, reading.end());
        assertArrayEquals(new int[]{result}, reading.result());
        assertEquals(List.of(), reading.codes());
        assertFalse(reading.mapped());
    }
}

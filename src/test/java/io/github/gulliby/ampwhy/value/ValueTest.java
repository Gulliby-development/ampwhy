package io.github.gulliby.ampwhy.value;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ValueTest {
    @Test
    void replacesInvalidValues() {
        Value.Result zero = Value.of(0);
        assertArrayEquals(new int[]{0xFFFD}, zero.cps());
        assertEquals("AW005", zero.code());
        assertEquals(false, zero.mapped());

        Value.Result firstAboveMaximum = Value.of(0x110000);
        assertArrayEquals(new int[]{0xFFFD}, firstAboveMaximum.cps());
        assertEquals("AW006", firstAboveMaximum.code());
        assertEquals(false, firstAboveMaximum.mapped());

        Value.Result rangeRulePrecedesNoncharacterBits = Value.of(0x11FFFF);
        assertArrayEquals(new int[]{0xFFFD}, rangeRulePrecedesNoncharacterBits.cps());
        assertEquals("AW006", rangeRulePrecedesNoncharacterBits.code());
        assertEquals(false, rangeRulePrecedesNoncharacterBits.mapped());

        Value.Result firstSurrogate = Value.of(0xD800);
        assertArrayEquals(new int[]{0xFFFD}, firstSurrogate.cps());
        assertEquals("AW007", firstSurrogate.code());
        assertEquals(false, firstSurrogate.mapped());

        Value.Result lastSurrogate = Value.of(0xDFFF);
        assertArrayEquals(new int[]{0xFFFD}, lastSurrogate.cps());
        assertEquals("AW007", lastSurrogate.code());
        assertEquals(false, lastSurrogate.mapped());
    }

    @Test
    void preservesOrdinaryBoundaryNeighbors() {
        Value.Result beforeSurrogates = Value.of(0xD7FF);
        assertArrayEquals(new int[]{0xD7FF}, beforeSurrogates.cps());
        assertEquals(null, beforeSurrogates.code());
        assertEquals(false, beforeSurrogates.mapped());

        Value.Result afterSurrogates = Value.of(0xE000);
        assertArrayEquals(new int[]{0xE000}, afterSurrogates.cps());
        assertEquals(null, afterSurrogates.code());
        assertEquals(false, afterSurrogates.mapped());

        Value.Result beforeNoncharacters = Value.of(0xFDCF);
        assertArrayEquals(new int[]{0xFDCF}, beforeNoncharacters.cps());
        assertEquals(null, beforeNoncharacters.code());
        assertEquals(false, beforeNoncharacters.mapped());

        Value.Result afterNoncharacters = Value.of(0xFDF0);
        assertArrayEquals(new int[]{0xFDF0}, afterNoncharacters.cps());
        assertEquals(null, afterNoncharacters.code());
        assertEquals(false, afterNoncharacters.mapped());
    }

    @Test
    void classifiesNoncharactersAndPlaneBoundaries() {
        Value.Result firstRangeValue = Value.of(0xFDD0);
        assertArrayEquals(new int[]{0xFDD0}, firstRangeValue.cps());
        assertEquals("AW008", firstRangeValue.code());
        assertEquals(false, firstRangeValue.mapped());

        Value.Result lastRangeValue = Value.of(0xFDEF);
        assertArrayEquals(new int[]{0xFDEF}, lastRangeValue.cps());
        assertEquals("AW008", lastRangeValue.code());
        assertEquals(false, lastRangeValue.mapped());

        Value.Result firstPlaneNoncharacter = Value.of(0xFFFE);
        assertArrayEquals(new int[]{0xFFFE}, firstPlaneNoncharacter.cps());
        assertEquals("AW008", firstPlaneNoncharacter.code());
        assertEquals(false, firstPlaneNoncharacter.mapped());

        Value.Result lastPlaneNoncharacter = Value.of(0xFFFF);
        assertArrayEquals(new int[]{0xFFFF}, lastPlaneNoncharacter.cps());
        assertEquals("AW008", lastPlaneNoncharacter.code());
        assertEquals(false, lastPlaneNoncharacter.mapped());

        Value.Result finalPlaneNoncharacter = Value.of(0x10FFFE);
        assertArrayEquals(new int[]{0x10FFFE}, finalPlaneNoncharacter.cps());
        assertEquals("AW008", finalPlaneNoncharacter.code());
        assertEquals(false, finalPlaneNoncharacter.mapped());

        Value.Result maximumNoncharacter = Value.of(0x10FFFF);
        assertArrayEquals(new int[]{0x10FFFF}, maximumNoncharacter.cps());
        assertEquals("AW008", maximumNoncharacter.code());
        assertEquals(false, maximumNoncharacter.mapped());
    }

    @Test
    void classifiesControlsExceptionsAndOrdinaryScalars() {
        Value.Result control = Value.of(1);
        assertArrayEquals(new int[]{1}, control.cps());
        assertEquals("AW009", control.code());
        assertEquals(false, control.mapped());

        Value.Result tab = Value.of(9);
        assertArrayEquals(new int[]{9}, tab.cps());
        assertEquals(null, tab.code());
        assertEquals(false, tab.mapped());

        Value.Result lineFeed = Value.of(10);
        assertArrayEquals(new int[]{10}, lineFeed.cps());
        assertEquals(null, lineFeed.code());
        assertEquals(false, lineFeed.mapped());

        Value.Result verticalTab = Value.of(11);
        assertArrayEquals(new int[]{11}, verticalTab.cps());
        assertEquals("AW009", verticalTab.code());
        assertEquals(false, verticalTab.mapped());

        Value.Result formFeed = Value.of(12);
        assertArrayEquals(new int[]{12}, formFeed.cps());
        assertEquals(null, formFeed.code());
        assertEquals(false, formFeed.mapped());

        Value.Result carriageReturn = Value.of(13);
        assertArrayEquals(new int[]{13}, carriageReturn.cps());
        assertEquals("AW009", carriageReturn.code());
        assertEquals(false, carriageReturn.mapped());

        Value.Result controlBoundary = Value.of(0x1F);
        assertArrayEquals(new int[]{0x1F}, controlBoundary.cps());
        assertEquals("AW009", controlBoundary.code());
        assertEquals(false, controlBoundary.mapped());

        Value.Result space = Value.of(32);
        assertArrayEquals(new int[]{32}, space.cps());
        assertEquals(null, space.code());
        assertEquals(false, space.mapped());

        Value.Result ordinaryAscii = Value.of(126);
        assertArrayEquals(new int[]{126}, ordinaryAscii.cps());
        assertEquals(null, ordinaryAscii.code());
        assertEquals(false, ordinaryAscii.mapped());

        Value.Result delete = Value.of(0x7F);
        assertArrayEquals(new int[]{0x7F}, delete.cps());
        assertEquals("AW009", delete.code());
        assertEquals(false, delete.mapped());

        Value.Result afterC1Controls = Value.of(0xA0);
        assertArrayEquals(new int[]{0xA0}, afterC1Controls.cps());
        assertEquals(null, afterC1Controls.code());
        assertEquals(false, afterC1Controls.mapped());

        Value.Result supplementaryScalar = Value.of(0x10000);
        assertArrayEquals(new int[]{0x10000}, supplementaryScalar.cps());
        assertEquals(null, supplementaryScalar.code());
        assertEquals(false, supplementaryScalar.mapped());

        Value.Result lastOrdinaryScalar = Value.of(0x10FFFD);
        assertArrayEquals(new int[]{0x10FFFD}, lastOrdinaryScalar.cps());
        assertEquals(null, lastOrdinaryScalar.code());
        assertEquals(false, lastOrdinaryScalar.mapped());
    }
    @Test
    void mapsEveryWindows1252Substitution() {
        Value.Result value80 = Value.of(0x80);
        assertEquals(0x20AC, Cp1252.map(0x80));
        assertArrayEquals(new int[]{0x20AC}, value80.cps());
        assertEquals("AW009", value80.code());
        assertEquals(true, value80.mapped());

        Value.Result value82 = Value.of(0x82);
        assertEquals(0x201A, Cp1252.map(0x82));
        assertArrayEquals(new int[]{0x201A}, value82.cps());
        assertEquals("AW009", value82.code());
        assertEquals(true, value82.mapped());

        Value.Result value83 = Value.of(0x83);
        assertEquals(0x0192, Cp1252.map(0x83));
        assertArrayEquals(new int[]{0x0192}, value83.cps());
        assertEquals("AW009", value83.code());
        assertEquals(true, value83.mapped());

        Value.Result value84 = Value.of(0x84);
        assertEquals(0x201E, Cp1252.map(0x84));
        assertArrayEquals(new int[]{0x201E}, value84.cps());
        assertEquals("AW009", value84.code());
        assertEquals(true, value84.mapped());

        Value.Result value85 = Value.of(0x85);
        assertEquals(0x2026, Cp1252.map(0x85));
        assertArrayEquals(new int[]{0x2026}, value85.cps());
        assertEquals("AW009", value85.code());
        assertEquals(true, value85.mapped());

        Value.Result value86 = Value.of(0x86);
        assertEquals(0x2020, Cp1252.map(0x86));
        assertArrayEquals(new int[]{0x2020}, value86.cps());
        assertEquals("AW009", value86.code());
        assertEquals(true, value86.mapped());

        Value.Result value87 = Value.of(0x87);
        assertEquals(0x2021, Cp1252.map(0x87));
        assertArrayEquals(new int[]{0x2021}, value87.cps());
        assertEquals("AW009", value87.code());
        assertEquals(true, value87.mapped());

        Value.Result value88 = Value.of(0x88);
        assertEquals(0x02C6, Cp1252.map(0x88));
        assertArrayEquals(new int[]{0x02C6}, value88.cps());
        assertEquals("AW009", value88.code());
        assertEquals(true, value88.mapped());

        Value.Result value89 = Value.of(0x89);
        assertEquals(0x2030, Cp1252.map(0x89));
        assertArrayEquals(new int[]{0x2030}, value89.cps());
        assertEquals("AW009", value89.code());
        assertEquals(true, value89.mapped());

        Value.Result value8A = Value.of(0x8A);
        assertEquals(0x0160, Cp1252.map(0x8A));
        assertArrayEquals(new int[]{0x0160}, value8A.cps());
        assertEquals("AW009", value8A.code());
        assertEquals(true, value8A.mapped());

        Value.Result value8B = Value.of(0x8B);
        assertEquals(0x2039, Cp1252.map(0x8B));
        assertArrayEquals(new int[]{0x2039}, value8B.cps());
        assertEquals("AW009", value8B.code());
        assertEquals(true, value8B.mapped());

        Value.Result value8C = Value.of(0x8C);
        assertEquals(0x0152, Cp1252.map(0x8C));
        assertArrayEquals(new int[]{0x0152}, value8C.cps());
        assertEquals("AW009", value8C.code());
        assertEquals(true, value8C.mapped());

        Value.Result value8E = Value.of(0x8E);
        assertEquals(0x017D, Cp1252.map(0x8E));
        assertArrayEquals(new int[]{0x017D}, value8E.cps());
        assertEquals("AW009", value8E.code());
        assertEquals(true, value8E.mapped());

        Value.Result value91 = Value.of(0x91);
        assertEquals(0x2018, Cp1252.map(0x91));
        assertArrayEquals(new int[]{0x2018}, value91.cps());
        assertEquals("AW009", value91.code());
        assertEquals(true, value91.mapped());

        Value.Result value92 = Value.of(0x92);
        assertEquals(0x2019, Cp1252.map(0x92));
        assertArrayEquals(new int[]{0x2019}, value92.cps());
        assertEquals("AW009", value92.code());
        assertEquals(true, value92.mapped());

        Value.Result value93 = Value.of(0x93);
        assertEquals(0x201C, Cp1252.map(0x93));
        assertArrayEquals(new int[]{0x201C}, value93.cps());
        assertEquals("AW009", value93.code());
        assertEquals(true, value93.mapped());

        Value.Result value94 = Value.of(0x94);
        assertEquals(0x201D, Cp1252.map(0x94));
        assertArrayEquals(new int[]{0x201D}, value94.cps());
        assertEquals("AW009", value94.code());
        assertEquals(true, value94.mapped());

        Value.Result value95 = Value.of(0x95);
        assertEquals(0x2022, Cp1252.map(0x95));
        assertArrayEquals(new int[]{0x2022}, value95.cps());
        assertEquals("AW009", value95.code());
        assertEquals(true, value95.mapped());

        Value.Result value96 = Value.of(0x96);
        assertEquals(0x2013, Cp1252.map(0x96));
        assertArrayEquals(new int[]{0x2013}, value96.cps());
        assertEquals("AW009", value96.code());
        assertEquals(true, value96.mapped());

        Value.Result value97 = Value.of(0x97);
        assertEquals(0x2014, Cp1252.map(0x97));
        assertArrayEquals(new int[]{0x2014}, value97.cps());
        assertEquals("AW009", value97.code());
        assertEquals(true, value97.mapped());

        Value.Result value98 = Value.of(0x98);
        assertEquals(0x02DC, Cp1252.map(0x98));
        assertArrayEquals(new int[]{0x02DC}, value98.cps());
        assertEquals("AW009", value98.code());
        assertEquals(true, value98.mapped());

        Value.Result value99 = Value.of(0x99);
        assertEquals(0x2122, Cp1252.map(0x99));
        assertArrayEquals(new int[]{0x2122}, value99.cps());
        assertEquals("AW009", value99.code());
        assertEquals(true, value99.mapped());

        Value.Result value9A = Value.of(0x9A);
        assertEquals(0x0161, Cp1252.map(0x9A));
        assertArrayEquals(new int[]{0x0161}, value9A.cps());
        assertEquals("AW009", value9A.code());
        assertEquals(true, value9A.mapped());

        Value.Result value9B = Value.of(0x9B);
        assertEquals(0x203A, Cp1252.map(0x9B));
        assertArrayEquals(new int[]{0x203A}, value9B.cps());
        assertEquals("AW009", value9B.code());
        assertEquals(true, value9B.mapped());

        Value.Result value9C = Value.of(0x9C);
        assertEquals(0x0153, Cp1252.map(0x9C));
        assertArrayEquals(new int[]{0x0153}, value9C.cps());
        assertEquals("AW009", value9C.code());
        assertEquals(true, value9C.mapped());

        Value.Result value9E = Value.of(0x9E);
        assertEquals(0x017E, Cp1252.map(0x9E));
        assertArrayEquals(new int[]{0x017E}, value9E.cps());
        assertEquals("AW009", value9E.code());
        assertEquals(true, value9E.mapped());

        Value.Result value9F = Value.of(0x9F);
        assertEquals(0x0178, Cp1252.map(0x9F));
        assertArrayEquals(new int[]{0x0178}, value9F.cps());
        assertEquals("AW009", value9F.code());
        assertEquals(true, value9F.mapped());
    }

    @Test
    void preservesUnmappedWindows1252Controls() {
        assertEquals(-1, Cp1252.map(0x81));
        Value.Result value81 = Value.of(0x81);
        assertArrayEquals(new int[]{0x81}, value81.cps());
        assertEquals("AW009", value81.code());
        assertEquals(false, value81.mapped());

        assertEquals(-1, Cp1252.map(0x8D));
        Value.Result value8D = Value.of(0x8D);
        assertArrayEquals(new int[]{0x8D}, value8D.cps());
        assertEquals("AW009", value8D.code());
        assertEquals(false, value8D.mapped());

        assertEquals(-1, Cp1252.map(0x8F));
        Value.Result value8F = Value.of(0x8F);
        assertArrayEquals(new int[]{0x8F}, value8F.cps());
        assertEquals("AW009", value8F.code());
        assertEquals(false, value8F.mapped());

        assertEquals(-1, Cp1252.map(0x90));
        Value.Result value90 = Value.of(0x90);
        assertArrayEquals(new int[]{0x90}, value90.cps());
        assertEquals("AW009", value90.code());
        assertEquals(false, value90.mapped());

        assertEquals(-1, Cp1252.map(0x9D));
        Value.Result value9D = Value.of(0x9D);
        assertArrayEquals(new int[]{0x9D}, value9D.cps());
        assertEquals("AW009", value9D.code());
        assertEquals(false, value9D.mapped());
    }

    @Test
    void returnsNoMappingOutsideWindows1252Table() {
        assertEquals(-1, Cp1252.map(0x7F));
        assertEquals(-1, Cp1252.map(0xA0));
    }

    @Test
    void preservesNegativeNumbersBeforeNoncharacterClassification() {
        Value.Result negativeOne = Value.of(-1);
        assertArrayEquals(new int[]{-1}, negativeOne.cps());
        assertEquals(null, negativeOne.code());
        assertEquals(false, negativeOne.mapped());

        Value.Result negativeTwo = Value.of(-2);
        assertArrayEquals(new int[]{-2}, negativeTwo.cps());
        assertEquals(null, negativeTwo.code());
        assertEquals(false, negativeTwo.mapped());
    }
}

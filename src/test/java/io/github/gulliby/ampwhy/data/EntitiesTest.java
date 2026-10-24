package io.github.gulliby.ampwhy.data;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

class EntitiesTest {
    @ParameterizedTest
    @CsvSource({
            "AMP,true,38",
            "GT,true,62",
            "LT,true,60",
            "QUOT,true,34",
            "acute,true,180",
            "amp,true,38",
            "ap,false,8776",
            "apos,false,39",
            "cent,true,162",
            "copy,true,169",
            "copysr,false,8471",
            "deg,true,176",
            "divide,true,247",
            "eacute,true,233",
            "euro,false,8364",
            "gt,true,62",
            "gtcc,false,10919",
            "hearts,false,9829",
            "hellip,false,8230",
            "laquo,true,171",
            "larr,false,8592",
            "ldquo,false,8220",
            "lsquo,false,8216",
            "lt,true,60",
            "ltri,false,9667",
            "mdash,false,8212",
            "micro,true,181",
            "nbsp,true,160",
            "ndash,false,8211",
            "not,true,172",
            "notin,false,8713",
            "notni,false,8716",
            "ouml,true,246",
            "para,true,182",
            "parallel,false,8741",
            "plusmn,true,177",
            "pound,true,163",
            "quot,true,34",
            "raquo,true,187",
            "rarr,false,8594",
            "reg,true,174",
            "rdquo,false,8221",
            "rsquo,false,8217",
            "sect,true,167",
            "shy,true,173",
            "szlig,true,223",
            "times,true,215",
            "timesb,false,8864",
            "trade,false,8482",
            "yen,true,165"
    })
    void singleCodePointEntriesHaveExactMetadata(String name, boolean legacy, int cp) {
        Entities.Entity entity = Entities.find(name);
        assertEquals(name, entity.name());
        assertEquals(legacy, entity.legacy());
        assertArrayEquals(new int[]{cp}, entity.cps());
    }

    @Test
    void nvltHasItsExactOrderedPair() {
        Entities.Entity entity = Entities.find("nvlt");
        assertEquals("nvlt", entity.name());
        assertEquals(false, entity.legacy());
        assertArrayEquals(new int[]{0x003C, 0x20D2}, entity.cps());
    }

    @Test
    void allReturnsTheCompleteSortedTable() {
        assertEquals(List.of(
                "AMP", "GT", "LT", "QUOT", "acute", "amp", "ap", "apos",
                "cent", "copy", "copysr", "deg", "divide", "eacute", "euro",
                "gt", "gtcc", "hearts", "hellip", "laquo", "larr", "ldquo",
                "lsquo", "lt", "ltri", "mdash", "micro", "nbsp", "ndash",
                "not", "notin", "notni", "nvlt", "ouml", "para", "parallel",
                "plusmn", "pound", "quot", "raquo", "rarr", "rdquo", "reg",
                "rsquo", "sect", "shy", "szlig", "times", "timesb", "trade",
                "yen"
        ), Entities.all().stream().map(Entities.Entity::name).toList());
        assertEquals(51, Entities.all().size());
    }

    @Test
    void allReturnsFreshLists() {
        assertNotSame(Entities.all(), Entities.all());
    }

    @Test
    void findIsExactAndCaseSensitive() {
        assertEquals("AMP", Entities.find("AMP").name());
        assertEquals("amp", Entities.find("amp").name());
        assertNull(Entities.find("Amp"));
        assertNull(Entities.find("unknown"));
        assertNull(Entities.find(""));
        assertNull(Entities.find("amp;"));
    }
}

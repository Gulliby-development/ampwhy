package io.github.gulliby.ampwhy.check;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.gulliby.ampwhy.base.Inputs;
import io.github.gulliby.ampwhy.base.Output;
import java.util.List;
import org.junit.jupiter.api.Test;

class CheckerTest {
    @Test
    void reportsNameAndSyntaxProblemsExactly() {
        assertEquals(new Output("f:1:1: AW001 missing semicolon after \"&amp\"\n"
                + "1 problem in 1 file\n", 1),
                Checker.check(List.of(new Inputs.Source("f", "&amp")), false));
        assertEquals(new Output("f:1:1: AW002 unknown name \"&zzz;\"\n"
                + "1 problem in 1 file\n", 1),
                Checker.check(List.of(new Inputs.Source("f", "&zzz;")), false));
        assertEquals(new Output("f:1:1: AW003 no digits in \"&#x\"\n"
                + "1 problem in 1 file\n", 1),
                Checker.check(List.of(new Inputs.Source("f", "&#x;")), false));
        assertEquals(new Output("f:1:1: AW004 missing semicolon after \"&#65\"\n"
                + "1 problem in 1 file\n", 1),
                Checker.check(List.of(new Inputs.Source("f", "&#65")), false));
    }

    @Test
    void reportsNullAndOutOfRangeProblemsExactly() {
        assertEquals(new Output("f:1:1: AW005 \"&#0;\" is the null character, read as U+FFFD\n"
                + "1 problem in 1 file\n", 1),
                Checker.check(List.of(new Inputs.Source("f", "&#0;")), false));
        assertEquals(new Output("f:1:1: AW006 \"&#x110000;\" is above U+10FFFF, read as U+FFFD\n"
                + "1 problem in 1 file\n", 1),
                Checker.check(List.of(new Inputs.Source("f", "&#x110000;")), false));
        assertEquals(new Output("f:1:1: AW007 \"&#xD800;\" is a surrogate, read as U+FFFD\n"
                + "1 problem in 1 file\n", 1),
                Checker.check(List.of(new Inputs.Source("f", "&#xD800;")), false));
        assertEquals(new Output("f:1:1: AW008 \"&#xFDD0;\" is a noncharacter, kept as U+FDD0\n"
                + "1 problem in 1 file\n", 1),
                Checker.check(List.of(new Inputs.Source("f", "&#xFDD0;")), false));
    }

    @Test
    void reportsMappedUnmappedAndCarriageReturnControlsExactly() {
        assertEquals(new Output("f:1:1: AW009 \"&#x80;\" is a control character, read as U+20AC\n"
                + "1 problem in 1 file\n", 1),
                Checker.check(List.of(new Inputs.Source("f", "&#x80;")), false));
        assertEquals(new Output("f:1:1: AW009 \"&#x81;\" is a control character, kept as U+0081\n"
                + "1 problem in 1 file\n", 1),
                Checker.check(List.of(new Inputs.Source("f", "&#x81;")), false));
        assertEquals(new Output("f:1:1: AW009 \"&#13;\" is a control character, kept as U+000D\n"
                + "1 problem in 1 file\n", 1),
                Checker.check(List.of(new Inputs.Source("f", "&#13;")), false));
    }

    @Test
    void reportsMultipleCodesAndCleanBeforeAffectedSources() {
        assertEquals(new Output("f:1:1: AW004 missing semicolon after \"&#0\"\nf:1:1: AW005 \"&#0\" is the null character, read as U+FFFD\n2 problems in 1 file\n", 1),
                Checker.check(List.of(new Inputs.Source("f", "&#0")), false), "[Source(\"f\",\"&#0\")]");
        assertEquals(new Output("b.html:1:2: AW001 missing semicolon after \"&copy\"\nb.html:1:9: AW004 missing semicolon after \"&#65\"\n2 problems in 1 file\n", 1),
                Checker.check(List.of(
                        new Inputs.Source("a.html", "&lt;b&gt; &amp;"),
                        new Inputs.Source("b.html", "x&copy y&#65")), false), "[Source(\"a.html\",\"&lt;b&gt; &amp;\"),Source(\"b.html\",\"x&copy y&#65\")]");
    }

    @Test
    void preservesSourceOrderAndCountsRepeatedDisplayNamesSeparately() {
        assertEquals(new Output("z:1:1: AW001 missing semicolon after \"&amp\"\na:1:1: AW004 missing semicolon after \"&#65\"\n2 problems in 2 files\n", 1),
                Checker.check(List.of(
                        new Inputs.Source("z", "&amp"),
                        new Inputs.Source("a", "&#65")), false), "[Source(\"z\",\"&amp\"),Source(\"a\",\"&#65\")]");
        assertEquals(new Output("f:1:1: AW001 missing semicolon after \"&amp\"\nf:1:1: AW001 missing semicolon after \"&amp\"\n2 problems in 2 files\n", 1),
                Checker.check(List.of(
                        new Inputs.Source("f", "&amp"),
                        new Inputs.Source("f", "&amp")), false), "[Source(\"f\",\"&amp\"),Source(\"f\",\"&amp\")]");
    }

    @Test
    void reportsCodePointColumnsAndLfOnlyLines() {
        assertEquals(new Output("f:1:4: AW001 missing semicolon after \"&amp\"\nf:2:1: AW004 missing semicolon after \"&#65\"\n2 problems in 1 file\n", 1),
                Checker.check(List.of(new Inputs.Source("f", "é😀\r&amp\n&#65")), false));
    }

    @Test
    void summarizesValidOccurrencesAndUnknownNamesAsClean() {
        assertEquals(new Output("ok, 4 references in 1 file\n", 0),
                Checker.check(List.of(
                        new Inputs.Source("a.html", "a &lt; b &amp;&amp; c &#x1F600;")), false));
        assertEquals(new Output("ok, 1 reference in 1 file\n", 0),
                Checker.check(List.of(new Inputs.Source("f", "&zzz")), false));
    }

    @Test
    void summarizesAttributeKeptOccurrencesAsClean() {
        assertEquals(new Output("ok, 2 references in 1 file\n", 0),
                Checker.check(List.of(new Inputs.Source("f", "&ampx &notit;")), true));
    }

    @Test
    void summarizesNoOccurrencesAcrossMultipleAndEmptySources() {
        assertEquals(new Output("ok, 0 references in 2 files\n", 0),
                Checker.check(List.of(
                        new Inputs.Source("f", "& x"),
                        new Inputs.Source("g", "")), false));
        assertEquals(new Output("ok, 0 references in 0 files\n", 0),
                Checker.check(List.of(), false));
    }

    @Test
    void distinguishesTextModeFromAttributeException() {
        assertEquals(new Output("f:1:1: AW001 missing semicolon after \"&amp\"\n1 problem in 1 file\n", 1),
                Checker.check(List.of(new Inputs.Source("f", "&ampx")), false));
        assertEquals(new Output("ok, 1 reference in 1 file\n", 0),
                Checker.check(List.of(new Inputs.Source("f", "&ampx")), true));
    }
}

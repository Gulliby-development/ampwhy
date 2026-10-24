package io.github.gulliby.ampwhy.codec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.gulliby.ampwhy.base.Text;
import org.junit.jupiter.api.Test;

class CodecTest {
    @Test
    void emptyDecodeAndEmptyNumericEncode() {
        assertEquals("", Decoder.decode(Text.cps(""), false));
        assertEquals("", Encoder.encode(Text.cps(""), "numeric", false));
    }

    @Test
    void decodeExpandsMultiCodePointWithoutRecursiveParsing() {
        assertEquals("😀<\u20D2&lt;", Decoder.decode(Text.cps("😀&nvlt;&amp;lt;"), false));
    }

    @Test
    void decodePreservesUntouchedTextAndNullResultReadings() {
        assertEquals("\uFEFFx\r\n\u0000&zzz; &#x; ©=2 ¬it; &",
                Decoder.decode(Text.cps("\uFEFFx\r\n\u0000&zzz; &#x; &copy=2 &notit; &amp;"), false));
    }

    @Test
    void decodeKeepsAttributeLegacyNamesWhenRequired() {
        assertEquals("&copy=2 &notit; &x &",
                Decoder.decode(Text.cps("&copy=2 &notit; &amp;x &amp"), true));
    }

    @Test
    void canonicalNamesExcludeAliasesAndMultiCodePointEntries() {
        assertEquals("amp", Namer.nameFor(0x26));
        assertEquals("lt", Namer.nameFor(0x3C));
        assertEquals("quot", Namer.nameFor(0x22));
        assertEquals("apos", Namer.nameFor(0x27));
        assertNull(Namer.nameFor(0x20D2));
        assertNull(Namer.nameFor(0x1F600));
    }

    @Test
    void minimalEscapesTextAndAttributeQuotes() {
        assertEquals("&amp;amp;&lt;&gt;\"'é😀", Encoder.encode(Text.cps("&amp;<>\"'é😀"), "minimal", false));
        assertEquals("&amp;amp;&lt;&gt;&quot;'é😀",
                Encoder.encode(Text.cps("&amp;<>\"'é😀"), "minimal", true));
    }

    @Test
    void minimalPreservesControlsAndBom() {
        assertEquals("\uFEFF\u0000\t\n\f\r\u007F",
                Encoder.encode(Text.cps("\uFEFF\u0000\t\n\f\r\u007F"), "minimal", false));
    }

    @Test
    void namedEscapesCanonicalNamesAndPreservesUnnamedText() {
        assertEquals("&eacute;&nbsp;&lt;&gt;&quot;&apos;&amp;&euro;ć😀",
                Encoder.encode(Text.cps("é <>\"'&€ć😀"), "named", false));
        assertEquals("&quot;&apos;", Encoder.encode(Text.cps("\"'"), "named", true));
        assertEquals("\uFEFF\u0000\t\n\f\r\u007F",
                Encoder.encode(Text.cps("\uFEFF\u0000\t\n\f\r\u007F"), "named", false));
    }

    @Test
    void numericUsesNamesBeforeEscapingAndDoesNotParseReferences() {
        assertEquals("&eacute;&euro;&#x107;&#x1F600;&quot;&apos;&amp;amp;",
                Encoder.encode(Text.cps("é€ć😀\"'&amp;"), "numeric", false));
    }

    @Test
    void numericEscapesControlsAndValuesAboveAsciiBoundary() {
        assertEquals("&#x0;\t\n&#xB;&#xC;&#xD;&#x1F; ~&#x7F;&#x80;&#xFEFF;",
                Encoder.encode(Text.cps("\u0000\t\n\u000B\f\r\u001F ~\u007F\u0080\uFEFF"),
                        "numeric", true));
    }
}

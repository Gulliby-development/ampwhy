<p align="center"><img src="docs/logo.svg" width="96" alt="" /></p>

# ampwhy

[![CI](https://github.com/Gulliby-development/ampwhy/actions/workflows/ci.yml/badge.svg)](https://github.com/Gulliby-development/ampwhy/actions/workflows/ci.yml) [![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

ampwhy is an offline command-line tool for people who write, review or scrape HTML and XML text and want to know why a character reference means what it means (`&amp`, `&notit;`, `&#x80;`, `&#65`). It reads references the way a browser's HTML tokenizer does, in text and in attribute values, and makes no network calls.

## Why

A page that "works" in one browser can still carry references the HTML standard calls errors: a missing semicolon, an unknown name, a number in the Windows-1252 range. `ampwhy decode` shows what a browser makes of them, `explain` shows how each ampersand was read, `check` lists the errors with line and column, `encode` writes text back with references, and `names` prints the table of names it knows.

## Install

Requirements: Java 21 and Maven. From the repository root, build and run the tests:

```sh
mvn -B verify
```

Run the tool (the examples below call this `ampwhy`):

```sh
java -cp target/classes io.github.gulliby.ampwhy.Main
```

`mvn -B verify` also builds `target/ampwhy-0.1.0.jar`, which starts the same program: `java -jar target/ampwhy-0.1.0.jar`.

## Usage

```
usage: ampwhy decode [--attr] [--strict] FILE
       ampwhy explain [--attr] FILE
       ampwhy check [--attr] [--summary] FILE...
       ampwhy encode [--mode minimal|named|numeric] [--attr] FILE
       ampwhy names [PREFIX]
```

`FILE` is a path, or `-` for standard input. Input must be strict UTF-8. Exit code 0 is success, 1 a negative answer (`check` found problems, `decode --strict` met a problem, `names` found no name), 2 any error (standard output is empty and standard error holds one line).

Create a sample file (in a POSIX shell):

```sh
printf 'Fish &amp chips &zzz; &#x; &#65 &#0;\n&#x80; &#x110000; &copy 2024\n' > page.html
```

`decode` replaces every reading that has a result and copies the rest:

```
$ ampwhy decode page.html
Fish & chips &zzz; &#x; A �
€ � © 2024
```

`explain` shows one row per ampersand: where it is (line:column, counted in code points), what was consumed, how it was read, the result and the problem codes:

```
$ ampwhy explain page.html
AT    SOURCE      HOW      RESULT  CODES
1:6   &amp        legacy   U+0026  AW001
1:17  &zzz        unknown  text    AW002
1:23  &#x         none     text    AW003
1:28  &#65        dec      U+0041  AW004
1:33  &#0;        dec      U+FFFD  AW005
2:1   &#x80;      hex      U+20AC  AW009
2:8   &#x110000;  hex      U+FFFD  AW006
2:19  &copy       legacy   U+00A9  AW001
```

`check` prints one line per problem and exits 1 when there is any:

```
$ ampwhy check page.html
page.html:1:6: AW001 missing semicolon after "&amp"
page.html:1:17: AW002 unknown name "&zzz;"
page.html:1:23: AW003 no digits in "&#x"
page.html:1:28: AW004 missing semicolon after "&#65"
page.html:1:33: AW005 "&#0;" is the null character, read as U+FFFD
page.html:2:1: AW009 "&#x80;" is a control character, read as U+20AC
page.html:2:8: AW006 "&#x110000;" is above U+10FFFF, read as U+FFFD
page.html:2:19: AW001 missing semicolon after "&copy"
8 problems in 1 file
```

With `--attr` the text is read as an attribute value, where a legacy name without a semicolon that is followed by `=`, a letter or a digit is left alone:

```sh
printf '<a href="?a=1&copy=2&amp;b=3&notit=4">x &notin;</a>\n' > q.html
```

```
$ ampwhy explain --attr q.html
AT    SOURCE   HOW   RESULT  CODES
1:14  &copy    kept  text    -
1:21  &amp;    name  U+0026  -
1:29  &not     kept  text    -
1:41  &notin;  name  U+2209  -
```

`encode` writes text back with references. Mode `minimal` (default) escapes `&`, `<` and `>` (and `"` with `--attr`), `named` adds every code point that has a name, `numeric` writes the rest as `&#xHEX;`:

```sh
printf 'caf\303\251 <b>"1 < 2" & it'"'"'s\n' > e.txt
```

```
$ ampwhy encode --mode named e.txt
caf&eacute; &lt;b&gt;&quot;1 &lt; 2&quot; &amp; it&apos;s
```

`names` prints the table of the 51 names it knows, optionally only those that start with PREFIX:

```
$ ampwhy names not
NAME   FORM    CODE POINTS
not    legacy  U+00AC
notin  plain   U+2209
notni  plain   U+220C
```

An error exits 2 with one line on standard error:

```
$ ampwhy check nofile.txt
ampwhy: nofile.txt: cannot read the file
```

### Options

| Option | Command | Description |
|---|---|---|
| `--attr` | decode, explain, check, encode | read the text as an attribute value |
| `--summary` | check | print only the last line of the report (the problem count or the `ok` line); the exit code is unchanged |
| `--strict` | decode | exit 1 when any reading has a problem; the text is still written |
| `--mode minimal\|named\|numeric` or `--mode=named` | encode | how much to escape (default `minimal`) |

## References

The tool knows exactly 51 case-sensitive names. 29 of them are legacy names (`amp`, `lt`, `copy`, `not`, `para`, ...) that may also be written without the semicolon; the others need it. After an ampersand, a letter, a digit or `#` starts a reference; anything else is plain text. Among the names of the table that are a prefix of the run of letters and digits, the one that consumes the most wins (`&notit;` reads `&not` as U+00AC, `&parallel` reads `&para`). In attribute mode a legacy name without a semicolon is kept as text when the next code point is `=`, a letter or a digit.

Numbers (`&#65;`, `&#x41;`) grow with each digit and saturate above U+10FFFF. A missing semicolon is AW004. Then the first rule that fits wins: 0 is U+FFFD (AW005), above U+10FFFF is U+FFFD (AW006), a surrogate is U+FFFD (AW007), a noncharacter keeps its code point (AW008), a control character (0 to 1F and 7F to 9F except TAB, LF, FF and space) is AW009, and from 0x80 to 0x9F it is replaced by the Windows-1252 (code page 1252) letter where there is one (`&#x80;` reads as the euro sign U+20AC; 81, 8D, 8F, 90 and 9D have none and keep their code point). The other problem codes are AW001 (missing semicolon after a legacy name), AW002 (unknown name followed by a semicolon) and AW003 (no digits after `&#` or `&#x`).

Positions are counted in code points. Lines end at LF only; a BOM and CR are ordinary characters.

## Development

```sh
mvn -B verify
```

compiles the code and runs every test (JUnit Jupiter 5, the only dependency, in test scope). Runtime code uses the Java 21 standard library only. CI runs this on Linux, Windows and macOS.

## License

MIT, see [LICENSE](LICENSE).

---

Built with BAKED

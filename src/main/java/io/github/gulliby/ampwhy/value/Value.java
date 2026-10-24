package io.github.gulliby.ampwhy.value;

public final class Value {
    private Value() {
    }

    public static Result of(int number) {
        if (number < 0) {
            return new Result(new int[]{number}, null, false);
        }
        if (number == 0) {
            return new Result(new int[]{0xFFFD}, "AW005", false);
        }
        if (number >= 0x110000) {
            return new Result(new int[]{0xFFFD}, "AW006", false);
        }
        if (number >= 0xD800 && number <= 0xDFFF) {
            return new Result(new int[]{0xFFFD}, "AW007", false);
        }
        if ((number >= 0xFDD0 && number <= 0xFDEF)
                || (number & 0xFFFF) == 0xFFFE
                || (number & 0xFFFF) == 0xFFFF) {
            return new Result(new int[]{number}, "AW008", false);
        }
        if ((number <= 0x1F && number != 9 && number != 10 && number != 12)
                || (number >= 0x7F && number <= 0x9F)) {
            int mapped = Cp1252.map(number);
            return new Result(new int[]{mapped == -1 ? number : mapped}, "AW009", mapped != -1);
        }
        return new Result(new int[]{number}, null, false);
    }

    public record Result(int[] cps, String code, boolean mapped) {
    }
}

package io.github.gulliby.ampwhy.value;

public final class Cp1252 {
    private Cp1252() {
    }

    public static int map(int number) {
        return switch (number) {
            case 0x80 -> 0x20AC;
            case 0x82 -> 0x201A;
            case 0x83 -> 0x0192;
            case 0x84 -> 0x201E;
            case 0x85 -> 0x2026;
            case 0x86 -> 0x2020;
            case 0x87 -> 0x2021;
            case 0x88 -> 0x02C6;
            case 0x89 -> 0x2030;
            case 0x8A -> 0x0160;
            case 0x8B -> 0x2039;
            case 0x8C -> 0x0152;
            case 0x8E -> 0x017D;
            case 0x91 -> 0x2018;
            case 0x92 -> 0x2019;
            case 0x93 -> 0x201C;
            case 0x94 -> 0x201D;
            case 0x95 -> 0x2022;
            case 0x96 -> 0x2013;
            case 0x97 -> 0x2014;
            case 0x98 -> 0x02DC;
            case 0x99 -> 0x2122;
            case 0x9A -> 0x0161;
            case 0x9B -> 0x203A;
            case 0x9C -> 0x0153;
            case 0x9E -> 0x017E;
            case 0x9F -> 0x0178;
            default -> -1;
        };
    }
}

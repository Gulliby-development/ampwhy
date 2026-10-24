package io.github.gulliby.ampwhy.codec;

import io.github.gulliby.ampwhy.data.Entities;

public final class Namer {
    private Namer() {
    }

    public static String nameFor(int cp) {
        String best = null;
        for (Entities.Entity entity : Entities.all()) {
            int[] cps = entity.cps();
            if (cps.length != 1 || cps[0] != cp || !hasLowercaseLetter(entity.name())) {
                continue;
            }
            String name = entity.name();
            if (best == null || name.length() < best.length()
                    || name.length() == best.length() && name.compareTo(best) < 0) {
                best = name;
            }
        }
        return best;
    }

    private static boolean hasLowercaseLetter(String name) {
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (ch >= 'a' && ch <= 'z') {
                return true;
            }
        }
        return false;
    }
}

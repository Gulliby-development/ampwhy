package io.github.gulliby.ampwhy.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** The offline set of named character references supported by ampwhy. */
public final class Entities {
    private Entities() {
    }

    /** One named character reference and the code points it represents. */
    public record Entity(String name, boolean legacy, int[] cps) {
        public Entity {
            cps = cps.clone();
        }

        @Override
        public int[] cps() {
            return cps.clone();
        }
    }

    private static final List<Entity> TABLE = List.of(
            new Entity("AMP", true, new int[]{0x0026}),
            new Entity("GT", true, new int[]{0x003E}),
            new Entity("LT", true, new int[]{0x003C}),
            new Entity("QUOT", true, new int[]{0x0022}),
            new Entity("acute", true, new int[]{0x00B4}),
            new Entity("amp", true, new int[]{0x0026}),
            new Entity("ap", false, new int[]{0x2248}),
            new Entity("apos", false, new int[]{0x0027}),
            new Entity("cent", true, new int[]{0x00A2}),
            new Entity("copy", true, new int[]{0x00A9}),
            new Entity("copysr", false, new int[]{0x2117}),
            new Entity("deg", true, new int[]{0x00B0}),
            new Entity("divide", true, new int[]{0x00F7}),
            new Entity("eacute", true, new int[]{0x00E9}),
            new Entity("euro", false, new int[]{0x20AC}),
            new Entity("gt", true, new int[]{0x003E}),
            new Entity("gtcc", false, new int[]{0x2AA7}),
            new Entity("hearts", false, new int[]{0x2665}),
            new Entity("hellip", false, new int[]{0x2026}),
            new Entity("laquo", true, new int[]{0x00AB}),
            new Entity("larr", false, new int[]{0x2190}),
            new Entity("ldquo", false, new int[]{0x201C}),
            new Entity("lsquo", false, new int[]{0x2018}),
            new Entity("lt", true, new int[]{0x003C}),
            new Entity("ltri", false, new int[]{0x25C3}),
            new Entity("mdash", false, new int[]{0x2014}),
            new Entity("micro", true, new int[]{0x00B5}),
            new Entity("nbsp", true, new int[]{0x00A0}),
            new Entity("ndash", false, new int[]{0x2013}),
            new Entity("not", true, new int[]{0x00AC}),
            new Entity("notin", false, new int[]{0x2209}),
            new Entity("notni", false, new int[]{0x220C}),
            new Entity("nvlt", false, new int[]{0x003C, 0x20D2}),
            new Entity("ouml", true, new int[]{0x00F6}),
            new Entity("para", true, new int[]{0x00B6}),
            new Entity("parallel", false, new int[]{0x2225}),
            new Entity("plusmn", true, new int[]{0x00B1}),
            new Entity("pound", true, new int[]{0x00A3}),
            new Entity("quot", true, new int[]{0x0022}),
            new Entity("raquo", true, new int[]{0x00BB}),
            new Entity("rarr", false, new int[]{0x2192}),
            new Entity("reg", true, new int[]{0x00AE}),
            new Entity("rdquo", false, new int[]{0x201D}),
            new Entity("rsquo", false, new int[]{0x2019}),
            new Entity("sect", true, new int[]{0x00A7}),
            new Entity("shy", true, new int[]{0x00AD}),
            new Entity("szlig", true, new int[]{0x00DF}),
            new Entity("times", true, new int[]{0x00D7}),
            new Entity("timesb", false, new int[]{0x22A0}),
            new Entity("trade", false, new int[]{0x2122}),
            new Entity("yen", true, new int[]{0x00A5})
    );

    /** Returns a fresh immutable list of all entities, sorted by name. */
    public static List<Entity> all() {
        ArrayList<Entity> sorted = new ArrayList<>(TABLE);
        sorted.sort(Comparator.comparing(Entity::name));
        return Collections.unmodifiableList(sorted);
    }

    /** Returns the exact, case-sensitive name match, or null when absent. */
    public static Entity find(String name) {
        for (Entity entity : TABLE) {
            if (entity.name().equals(name)) {
                return entity;
            }
        }
        return null;
    }
}

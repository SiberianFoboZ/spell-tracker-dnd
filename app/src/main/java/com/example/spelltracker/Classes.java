package com.example.spelltracker;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Реестр D&D-классов приложения: id, отображаемое имя, файл со списком
 * заклинаний в assets/ и коэффициент участия в расчёте эффективного
 * уровня заклинателя.
 *
 * <p>Коэффициенты:
 * <ul>
 *   <li>{@code 1.0} — полный заклинатель (Бард, Волшебник, Друид, Жрец, Чародей);</li>
 *   <li>{@code 0.5} — половинный заклинатель (Паладин, Следопыт, Изобретатель);
 *       уровень делится на 2. Для Паладина и Следопыта — округление вниз
 *       ({@code roundUp=false}), для Изобретателя — округление вверх
 *       ({@code roundUp=true}, как в PHB);</li>
 *   <li>{@code 0.0} — не участвует в расчёте (Колдун использует pact magic).</li>
 * </ul>
 * </p>
 *
 * <p>Этот реестр — единственный источник правды для {@link ClassFilter}
 * (имена и списки заклинаний) и {@link SpellStorage} (расчёт уровня).</p>
 */
public final class Classes {

    public static final class Info {
        public final String id;
        public final String name;
        public final String assetFile;
        public final double factor;
        public final boolean roundUp;

        public Info(String id, String name, String assetFile, double factor, boolean roundUp) {
            this.id = id;
            this.name = name;
            this.assetFile = assetFile;
            this.factor = factor;
            this.roundUp = roundUp;
        }
    }

    public static final Info[] ALL = {
        new Info("bard",       "Бард",         "бард.json",         1.0, false),
        new Info("wizard",     "Волшебник",    "волшебник.json",    1.0, false),
        new Info("druid",      "Друид",        "друид.json",        1.0, false),
        new Info("cleric",     "Жрец",         "жрец.json",         1.0, false),
        new Info("warlock",    "Колдун",       "колдун.json",       0.0, false),
        new Info("paladin",    "Паладин",      "паладин.json",      0.5, false),
        new Info("ranger",     "Следопыт",     "следопыт.json",     0.5, false),
        new Info("sorcerer",   "Чародей",      "чародей.json",      1.0, false),
        new Info("artificer",  "Изобретатель", "изобретатель.json", 0.5, true),
    };

    private static final Map<String, Info> BY_ID;
    static {
        Map<String, Info> m = new LinkedHashMap<>();
        for (Info info : ALL) m.put(info.id, info);
        BY_ID = Collections.unmodifiableMap(m);
    }

    public static Map<String, Info> allById() {
        return BY_ID;
    }

    private Classes() {}
}

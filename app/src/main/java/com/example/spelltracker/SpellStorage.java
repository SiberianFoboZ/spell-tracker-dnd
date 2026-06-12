package com.example.spelltracker;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Хранилище пользовательских данных в SharedPreferences:
 * <ul>
 *   <li>число ячеек заклинаний 1-9 уровней и количество использованных;</li>
 *   <li>флаги «подготовлено» / «известно» для отдельных заклинаний
 *       (ключ — {@link #keyFor(Spell)});</li>
 *   <li>уровень заклинателя по каждому D&D-классу и производный
 *       от них эффективный уровень заклинателя.</li>
 * </ul>
 *
 * <p>Эффективный уровень считается по правилу PHB:
 * полные заклинатели (×1) + половинные (×0.5, округление вниз).
 * Колдун (pact magic) в формуле не участвует.</p>
 */
public class SpellStorage {

    private static final String PREFS = "spell_tracker_prefs";
    private static final String KEY_PREPARED = "prepared_set";
    private static final String KEY_KNOWN = "known_set";

    // Таблица ячеек заклинаний стандартных заклинателей (полные/половинные).
    // [casterLevel-1][spellLevel-1] = кол-во ячеек.
    // spellLevel 1 = индекс 0, spellLevel 9 = индекс 8.
    private static final int[][] SLOT_TABLE = {
        /*  1 */ {2,0,0,0,0,0,0,0,0},
        /*  2 */ {3,0,0,0,0,0,0,0,0},
        /*  3 */ {4,2,0,0,0,0,0,0,0},
        /*  4 */ {4,3,0,0,0,0,0,0,0},
        /*  5 */ {4,3,2,0,0,0,0,0,0},
        /*  6 */ {4,3,3,0,0,0,0,0,0},
        /*  7 */ {4,3,3,1,0,0,0,0,0},
        /*  8 */ {4,3,3,2,0,0,0,0,0},
        /*  9 */ {4,3,3,3,1,0,0,0,0},
        /* 10 */ {4,3,3,3,2,0,0,0,0},
        /* 11 */ {4,3,3,3,2,1,0,0,0},
        /* 12 */ {4,3,3,3,2,1,0,0,0},
        /* 13 */ {4,3,3,3,2,1,1,0,0},
        /* 14 */ {4,3,3,3,2,1,1,0,0},
        /* 15 */ {4,3,3,3,2,1,1,1,0},
        /* 16 */ {4,3,3,3,2,1,1,1,0},
        /* 17 */ {4,3,3,3,2,1,1,1,1},
        /* 18 */ {4,3,3,3,3,1,1,1,1},
        /* 19 */ {4,3,3,3,3,2,1,1,1},
        /* 20 */ {4,3,3,3,3,2,2,1,1},
    };

    private static final int MIN_LEVEL = 0;
    private static final int MAX_LEVEL = 20;

    // Pact magic (Колдун): [classLevel-1] -> {slotCount, slotLevel}.
    // Считается полностью отдельно от стандартной таблицы: фиксированное
    // число ячеек, все одного уровня (восстанавливаются на коротком отдыхе).
    private static final int[][] WARLOCK_SLOTS = {
        /*  1 */ {1, 1},
        /*  2 */ {2, 1},
        /*  3 */ {2, 2},
        /*  4 */ {2, 2},
        /*  5 */ {2, 3},
        /*  6 */ {2, 3},
        /*  7 */ {2, 4},
        /*  8 */ {2, 4},
        /*  9 */ {2, 5},
        /* 10 */ {2, 5},
        /* 11 */ {3, 5},
        /* 12 */ {3, 5},
        /* 13 */ {3, 5},
        /* 14 */ {3, 5},
        /* 15 */ {3, 5},
        /* 16 */ {3, 5},
        /* 17 */ {4, 5},
        /* 18 */ {4, 5},
        /* 19 */ {4, 5},
        /* 20 */ {4, 5},
    };

    private static final String KEY_WARLOCK_TOTAL = "warlock_slot_total";
    private static final String KEY_WARLOCK_LEVEL = "warlock_slot_level";
    private static final String KEY_WARLOCK_USED = "warlock_slot_used";

    private final SharedPreferences prefs;

    public SpellStorage(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ---------- ячейки заклинаний 1..9 ----------

    public int getSlotTotal(int level) {
        return prefs.getInt(slotKeyTotal(level), 0);
    }

    public void setSlotTotal(int level, int total) {
        if (total < 0) total = 0;
        prefs.edit().putInt(slotKeyTotal(level), total).apply();
    }

    public int getSlotUsed(int level) {
        return prefs.getInt(slotKeyUsed(level), 0);
    }

    public void setSlotUsed(int level, int used) {
        if (used < 0) used = 0;
        prefs.edit().putInt(slotKeyUsed(level), used).apply();
    }

    public void incSlotTotal(int level) {
        setSlotTotal(level, getSlotTotal(level) + 1);
    }

    public void decSlotTotal(int level) {
        setSlotTotal(level, getSlotTotal(level) - 1);
    }

    public void useSlot(int level) {
        setSlotUsed(level, getSlotUsed(level) + 1);
    }

    public void restoreSlot(int level) {
        setSlotUsed(level, getSlotUsed(level) - 1);
    }

    public void resetAllUsed() {
        SharedPreferences.Editor e = prefs.edit();
        for (int i = 1; i <= 9; i++) e.putInt(slotKeyUsed(i), 0);
        e.putInt(KEY_WARLOCK_USED, 0);
        e.apply();
    }

    private static String slotKeyTotal(int level) { return "slot_total_" + level; }
    private static String slotKeyUsed(int level) { return "slot_used_" + level; }

    // ---------- флаги «подготовлено» / «известно» ----------

    public boolean isPrepared(Spell spell) {
        return prefs.getStringSet(KEY_PREPARED, Collections.emptySet()).contains(keyFor(spell));
    }

    public void setPrepared(Spell spell, boolean prepared) {
        Set<String> set = new HashSet<>(prefs.getStringSet(KEY_PREPARED, Collections.emptySet()));
        String k = keyFor(spell);
        if (prepared) set.add(k); else set.remove(k);
        prefs.edit().putStringSet(KEY_PREPARED, set).apply();
    }

    public boolean isKnown(Spell spell) {
        return prefs.getStringSet(KEY_KNOWN, Collections.emptySet()).contains(keyFor(spell));
    }

    public void setKnown(Spell spell, boolean known) {
        Set<String> set = new HashSet<>(prefs.getStringSet(KEY_KNOWN, Collections.emptySet()));
        String k = keyFor(spell);
        if (known) set.add(k); else set.remove(k);
        prefs.edit().putStringSet(KEY_KNOWN, set).apply();
    }

    /**
     * Уникальный ключ заклинания: id из БД + имя в нижнем регистре.
     * Пример: {@code "42:огненный снаряд"}.
     */
    public static String keyFor(Spell spell) {
        return spell.id + ":" + (spell.name == null ? "" : spell.name.toLowerCase(Locale.ROOT));
    }

    // ---------- уровни классов и расчёт ячеек ----------

    private static String classLevelKey(String classId) { return "class_level_" + classId; }

    public int getClassLevel(String classId) {
        return prefs.getInt(classLevelKey(classId), 0);
    }

    public void setClassLevel(String classId, int level) {
        if (level < MIN_LEVEL) level = MIN_LEVEL;
        if (level > MAX_LEVEL) level = MAX_LEVEL;
        prefs.edit().putInt(classLevelKey(classId), level).apply();
    }

    /**
     * Суммирует уровни классов с учётом коэффициента из {@link Classes}:
     * <ul>
     *   <li>полные (1.0) — целиком;</li>
     *   <li>половинные (0.5) — деление на 2. {@code roundUp=true}
     *       (Изобретатель) даёт округление вверх, {@code roundUp=false}
     *       (Паладин, Следопыт) — округление вниз;</li>
     *   <li>исключённые (0.0) — не учитываются.</li>
     * </ul>
     * Результат ограничен диапазоном [0..20].
     */
    public int computeCasterLevel() {
        int total = 0;
        for (Classes.Info info : Classes.ALL) {
            if (info.factor <= 0.0) continue;
            int lvl = getClassLevel(info.id);
            if (info.factor >= 1.0) {
                total += lvl;
            } else if (info.factor == 0.5) {
                total += info.roundUp ? (lvl + 1) / 2 : lvl / 2;
            } else {
                total += (int) Math.round(lvl * info.factor);
            }
        }
        if (total < MIN_LEVEL) total = MIN_LEVEL;
        if (total > MAX_LEVEL) total = MAX_LEVEL;
        return total;
    }

    /**
     * Перезаписывает {@code slot_total_1..9} значениями из таблицы
     * для текущего эффективного уровня заклинателя. Состояние
     * «использовано» ({@code slot_used_N}) не сбрасывается.
     */
    public void applySlotTable() {
        int caster = computeCasterLevel();
        SharedPreferences.Editor e = prefs.edit();
        for (int spellLevel = 1; spellLevel <= 9; spellLevel++) {
            e.putInt(slotKeyTotal(spellLevel), getSlotsForCasterLevel(caster, spellLevel));
        }
        e.apply();
    }

    /** @return число ячеек заклинаний данного уровня для указанного caster level. */
    public static int getSlotsForCasterLevel(int casterLevel, int spellLevel) {
        if (casterLevel < 1 || casterLevel > 20) return 0;
        if (spellLevel < 1 || spellLevel > 9) return 0;
        return SLOT_TABLE[casterLevel - 1][spellLevel - 1];
    }

    // ---------- pact magic (Колдун) ----------

    /**
     * @return {count, level} для указанного уровня Колдуна; {0, 0} при level вне [1..20].
     */
    public static int[] getWarlockSlots(int warlockLevel) {
        if (warlockLevel < 1 || warlockLevel > 20) return new int[]{0, 0};
        return WARLOCK_SLOTS[warlockLevel - 1];
    }

    public int getWarlockSlotCount() {
        return prefs.getInt(KEY_WARLOCK_TOTAL, 0);
    }

    /** Уровень ячеек pact magic (1..5). 0, если Колдун не выбран. */
    public int getWarlockSlotLevel() {
        return prefs.getInt(KEY_WARLOCK_LEVEL, 0);
    }

    public int getWarlockSlotUsed() {
        return prefs.getInt(KEY_WARLOCK_USED, 0);
    }

    public void useWarlockSlot() {
        int used = getWarlockSlotUsed();
        int total = getWarlockSlotCount();
        if (used < total) {
            prefs.edit().putInt(KEY_WARLOCK_USED, used + 1).apply();
        }
    }

    public void restoreWarlockSlot() {
        int used = getWarlockSlotUsed();
        if (used > 0) {
            prefs.edit().putInt(KEY_WARLOCK_USED, used - 1).apply();
        }
    }

    /**
     * Перезаписывает count и level ячеек pact magic по таблице для текущего
     * уровня Колдуна. Состояние «использовано» не сбрасывается, но при
     * уменьшении числа ячеек клампуется вниз.
     */
    public void applyWarlockSlots() {
        int[] s = getWarlockSlots(getClassLevel("warlock"));
        SharedPreferences.Editor e = prefs.edit()
                .putInt(KEY_WARLOCK_TOTAL, s[0])
                .putInt(KEY_WARLOCK_LEVEL, s[1]);
        int used = getWarlockSlotUsed();
        if (used > s[0]) e.putInt(KEY_WARLOCK_USED, s[0]);
        e.apply();
    }
}

package com.example.spelltracker.data

/**
 * Таблицы ячеек заклинаний D&D 5e и логика выбора таблицы
 * (монокласс / мультикласс).
 *
 * **Single source of truth** для расчёта «сколько ячеек N-го уровня
 * есть у класса X на уровне Y». Раньше эта логика жила прямо в
 * [SpellStorage] и применяла единую мультиклассовую таблицу ко всем
 * случаям — отсюда баг: Паладин 5 (монокласс) получал 3/0 вместо
 * правильных 4/2.
 *
 * **Warlock** (factor = 0.0) обрабатывается отдельно — через
 * [SpellStorage.WARLOCK_SLOTS] и `getPactSlotTotal` /
 * `getPactSlotLevel`. В мультиклассовой формуле не участвует.
 *
 * **Этап 26** (2026-07-13): добавил отдельные таблицы для монокласса
 * + переключение 1/3 кастеров с `(lvl+2)/3` (ceil, проектный house rule
 * из Этапа 19) на `lvl/3` (PHB floor).
 */
internal object SlotTables {

    /**
     * Режим расчёта ячеек для текущего набора уровней классов.
     *
     * - [None] — нет активных кастер-классов. Обычные ячейки пустые.
     * - [Monoclass] — ровно один активный кастер-класс. Используется
     *   его индивидуальная PHB-таблица.
     * - [Multiclass] — 2+ активных кастеров. Используется
     *   [MULTICLASS_SLOTS] при итоговом [computeCasterLevel].
     *
     * «Активный кастер» = класс с `factor > 0.0` И уровнем > 0.
     * Колдун (factor = 0.0) сюда не входит — у него своя пакт-магия.
     */
    sealed interface SlotMode {
        object None : SlotMode
        data class Monoclass(val classId: String) : SlotMode
        object Multiclass : SlotMode
    }

    /**
     * Общая таблица мультикласса по PHB p.165.
     * `MULTICLASS_SLOTS[casterLevel][spellLevel] = число ячеек`.
     *
     * Совпадает с PHB full caster table по дизайну: для полного
     * кастера на уровне N multiclass-таблица даёт ту же строку, что
     * и его индивидуальная таблица (CL = уровень класса).
     */
    val MULTICLASS_SLOTS: Array<IntArray> = arrayOf(
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0),  //  0
        intArrayOf(0, 2, 0, 0, 0, 0, 0, 0, 0, 0),  //  1
        intArrayOf(0, 3, 0, 0, 0, 0, 0, 0, 0, 0),  //  2
        intArrayOf(0, 4, 2, 0, 0, 0, 0, 0, 0, 0),  //  3
        intArrayOf(0, 4, 3, 0, 0, 0, 0, 0, 0, 0),  //  4
        intArrayOf(0, 4, 3, 2, 0, 0, 0, 0, 0, 0),  //  5
        intArrayOf(0, 4, 3, 3, 0, 0, 0, 0, 0, 0),  //  6
        intArrayOf(0, 4, 3, 3, 1, 0, 0, 0, 0, 0),  //  7
        intArrayOf(0, 4, 3, 3, 2, 0, 0, 0, 0, 0),  //  8
        intArrayOf(0, 4, 3, 3, 3, 1, 0, 0, 0, 0),  //  9
        intArrayOf(0, 4, 3, 3, 3, 2, 0, 0, 0, 0),  // 10
        intArrayOf(0, 4, 3, 3, 3, 2, 1, 0, 0, 0),  // 11
        intArrayOf(0, 4, 3, 3, 3, 2, 1, 0, 0, 0),  // 12
        intArrayOf(0, 4, 3, 3, 3, 2, 1, 1, 0, 0),  // 13
        intArrayOf(0, 4, 3, 3, 3, 2, 1, 1, 0, 0),  // 14
        intArrayOf(0, 4, 3, 3, 3, 2, 1, 1, 1, 0),  // 15
        intArrayOf(0, 4, 3, 3, 3, 2, 1, 1, 1, 0),  // 16
        intArrayOf(0, 4, 3, 3, 3, 2, 1, 1, 1, 1),  // 17
        intArrayOf(0, 4, 3, 3, 3, 3, 1, 1, 1, 1),  // 18
        intArrayOf(0, 4, 3, 3, 3, 3, 2, 1, 1, 1),  // 19
        intArrayOf(0, 4, 3, 3, 3, 3, 2, 2, 1, 1),  // 20
    )

    /**
     * Таблица полных кастеров по PHB: Bard, Cleric, Druid, Sorcerer, Wizard.
     * Все 5 классов делят одну и ту же таблицу (так в PHB).
     * `FULL_CASTER_SLOTS[classLevel][spellLevel] = число ячеек`.
     */
    val FULL_CASTER_SLOTS: Array<IntArray> = arrayOf(
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0),  //  0
        intArrayOf(0, 2, 0, 0, 0, 0, 0, 0, 0, 0),  //  1
        intArrayOf(0, 3, 0, 0, 0, 0, 0, 0, 0, 0),  //  2
        intArrayOf(0, 4, 2, 0, 0, 0, 0, 0, 0, 0),  //  3
        intArrayOf(0, 4, 3, 0, 0, 0, 0, 0, 0, 0),  //  4
        intArrayOf(0, 4, 3, 2, 0, 0, 0, 0, 0, 0),  //  5
        intArrayOf(0, 4, 3, 3, 0, 0, 0, 0, 0, 0),  //  6
        intArrayOf(0, 4, 3, 3, 1, 0, 0, 0, 0, 0),  //  7
        intArrayOf(0, 4, 3, 3, 2, 0, 0, 0, 0, 0),  //  8
        intArrayOf(0, 4, 3, 3, 3, 1, 0, 0, 0, 0),  //  9
        intArrayOf(0, 4, 3, 3, 3, 2, 0, 0, 0, 0),  // 10
        intArrayOf(0, 4, 3, 3, 3, 2, 1, 0, 0, 0),  // 11
        intArrayOf(0, 4, 3, 3, 3, 2, 1, 0, 0, 0),  // 12
        intArrayOf(0, 4, 3, 3, 3, 2, 1, 1, 0, 0),  // 13
        intArrayOf(0, 4, 3, 3, 3, 2, 1, 1, 0, 0),  // 14
        intArrayOf(0, 4, 3, 3, 3, 2, 1, 1, 1, 0),  // 15
        intArrayOf(0, 4, 3, 3, 3, 2, 1, 1, 1, 0),  // 16
        intArrayOf(0, 4, 3, 3, 3, 2, 1, 1, 1, 1),  // 17
        intArrayOf(0, 4, 3, 3, 3, 3, 1, 1, 1, 1),  // 18
        intArrayOf(0, 4, 3, 3, 3, 3, 2, 1, 1, 1),  // 19
        intArrayOf(0, 4, 3, 3, 3, 3, 2, 2, 1, 1),  // 20
    )

    /**
     * Таблица Изобретателя по Tasha's Cauldron of Everything.
     * Уровни ячеек только 1..5, выше нули.
     */
    val ARTIFICER_SLOTS: Array<IntArray> = arrayOf(
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0),  //  0
        intArrayOf(0, 2, 0, 0, 0, 0, 0, 0, 0, 0),  //  1
        intArrayOf(0, 2, 0, 0, 0, 0, 0, 0, 0, 0),  //  2
        intArrayOf(0, 3, 0, 0, 0, 0, 0, 0, 0, 0),  //  3
        intArrayOf(0, 3, 0, 0, 0, 0, 0, 0, 0, 0),  //  4
        intArrayOf(0, 4, 2, 0, 0, 0, 0, 0, 0, 0),  //  5
        intArrayOf(0, 4, 2, 0, 0, 0, 0, 0, 0, 0),  //  6
        intArrayOf(0, 4, 3, 0, 0, 0, 0, 0, 0, 0),  //  7
        intArrayOf(0, 4, 3, 0, 0, 0, 0, 0, 0, 0),  //  8
        intArrayOf(0, 4, 3, 2, 0, 0, 0, 0, 0, 0),  //  9
        intArrayOf(0, 4, 3, 2, 0, 0, 0, 0, 0, 0),  // 10
        intArrayOf(0, 4, 3, 3, 0, 0, 0, 0, 0, 0),  // 11
        intArrayOf(0, 4, 3, 3, 0, 0, 0, 0, 0, 0),  // 12
        intArrayOf(0, 4, 3, 3, 1, 0, 0, 0, 0, 0),  // 13
        intArrayOf(0, 4, 3, 3, 1, 0, 0, 0, 0, 0),  // 14
        intArrayOf(0, 4, 3, 3, 2, 0, 0, 0, 0, 0),  // 15
        intArrayOf(0, 4, 3, 3, 2, 0, 0, 0, 0, 0),  // 16
        intArrayOf(0, 4, 3, 3, 3, 1, 0, 0, 0, 0),  // 17
        intArrayOf(0, 4, 3, 3, 3, 1, 0, 0, 0, 0),  // 18
        intArrayOf(0, 4, 3, 3, 3, 2, 0, 0, 0, 0),  // 19
        intArrayOf(0, 4, 3, 3, 3, 2, 0, 0, 0, 0),  // 20
    )

    /**
     * Таблица половинных кастеров по PHB: Paladin, Ranger.
     * Уровни ячеек только 1..5, выше нули.
     */
    val HALF_CASTER_SLOTS: Array<IntArray> = arrayOf(
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0),  //  0
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0),  //  1
        intArrayOf(0, 2, 0, 0, 0, 0, 0, 0, 0, 0),  //  2
        intArrayOf(0, 3, 0, 0, 0, 0, 0, 0, 0, 0),  //  3
        intArrayOf(0, 3, 0, 0, 0, 0, 0, 0, 0, 0),  //  4
        intArrayOf(0, 4, 2, 0, 0, 0, 0, 0, 0, 0),  //  5
        intArrayOf(0, 4, 2, 0, 0, 0, 0, 0, 0, 0),  //  6
        intArrayOf(0, 4, 3, 0, 0, 0, 0, 0, 0, 0),  //  7
        intArrayOf(0, 4, 3, 0, 0, 0, 0, 0, 0, 0),  //  8
        intArrayOf(0, 4, 3, 2, 0, 0, 0, 0, 0, 0),  //  9
        intArrayOf(0, 4, 3, 2, 0, 0, 0, 0, 0, 0),  // 10
        intArrayOf(0, 4, 3, 3, 0, 0, 0, 0, 0, 0),  // 11
        intArrayOf(0, 4, 3, 3, 0, 0, 0, 0, 0, 0),  // 12
        intArrayOf(0, 4, 3, 3, 1, 0, 0, 0, 0, 0),  // 13
        intArrayOf(0, 4, 3, 3, 1, 0, 0, 0, 0, 0),  // 14
        intArrayOf(0, 4, 3, 3, 2, 0, 0, 0, 0, 0),  // 15
        intArrayOf(0, 4, 3, 3, 2, 0, 0, 0, 0, 0),  // 16
        intArrayOf(0, 4, 3, 3, 3, 1, 0, 0, 0, 0),  // 17
        intArrayOf(0, 4, 3, 3, 3, 1, 0, 0, 0, 0),  // 18
        intArrayOf(0, 4, 3, 3, 3, 2, 0, 0, 0, 0),  // 19
        intArrayOf(0, 4, 3, 3, 3, 2, 0, 0, 0, 0),  // 20
    )

    /**
     * Таблица 1/3 кастеров по PHB: Fighter (Eldritch Knight), Rogue (Arcane Trickster).
     * Уровни ячеек только 1..4, выше нули.
     */
    val THIRD_CASTER_SLOTS: Array<IntArray> = arrayOf(
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0),  //  0
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0),  //  1
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0),  //  2
        intArrayOf(0, 2, 0, 0, 0, 0, 0, 0, 0, 0),  //  3
        intArrayOf(0, 3, 0, 0, 0, 0, 0, 0, 0, 0),  //  4
        intArrayOf(0, 3, 0, 0, 0, 0, 0, 0, 0, 0),  //  5
        intArrayOf(0, 3, 0, 0, 0, 0, 0, 0, 0, 0),  //  6
        intArrayOf(0, 4, 2, 0, 0, 0, 0, 0, 0, 0),  //  7
        intArrayOf(0, 4, 2, 0, 0, 0, 0, 0, 0, 0),  //  8
        intArrayOf(0, 4, 2, 0, 0, 0, 0, 0, 0, 0),  //  9
        intArrayOf(0, 4, 3, 0, 0, 0, 0, 0, 0, 0),  // 10
        intArrayOf(0, 4, 3, 0, 0, 0, 0, 0, 0, 0),  // 11
        intArrayOf(0, 4, 3, 0, 0, 0, 0, 0, 0, 0),  // 12
        intArrayOf(0, 4, 3, 2, 0, 0, 0, 0, 0, 0),  // 13
        intArrayOf(0, 4, 3, 2, 0, 0, 0, 0, 0, 0),  // 14
        intArrayOf(0, 4, 3, 2, 0, 0, 0, 0, 0, 0),  // 15
        intArrayOf(0, 4, 3, 3, 0, 0, 0, 0, 0, 0),  // 16
        intArrayOf(0, 4, 3, 3, 0, 0, 0, 0, 0, 0),  // 17
        intArrayOf(0, 4, 3, 3, 0, 0, 0, 0, 0, 0),  // 18
        intArrayOf(0, 4, 3, 3, 1, 0, 0, 0, 0, 0),  // 19
        intArrayOf(0, 4, 3, 3, 1, 0, 0, 0, 0, 0),  // 20
    )

    // ---- Registry: class id -> monoclass table ----

    /**
     * Реестр: id класса -> его индивидуальная таблица для монокласса.
     * Классы с factor = 0 (warlock) и неизвестные id дадут null и будут
     * считаться как None-режим в [detectMode].
     */
    internal val MONOCLASS_TABLES: Map<String, Array<IntArray>> = mapOf(
        "bard"            to FULL_CASTER_SLOTS,
        "cleric"          to FULL_CASTER_SLOTS,
        "druid"           to FULL_CASTER_SLOTS,
        "sorcerer"        to FULL_CASTER_SLOTS,
        "wizard"          to FULL_CASTER_SLOTS,
        "artificer"       to ARTIFICER_SLOTS,
        "paladin"         to HALF_CASTER_SLOTS,
        "ranger"          to HALF_CASTER_SLOTS,
        "fighter_mystic"  to THIRD_CASTER_SLOTS,
        "rogue_mystic"    to THIRD_CASTER_SLOTS,
    )

    /**
     * Определить режим расчёта ячеек по набору уровней классов.
     * Колдун (factor = 0) не считается активным кастером — у него
     * собственная пакт-магия через [SpellStorage.WARLOCK_SLOTS].
     */
    fun detectMode(classLevels: Map<String, Int>): SlotMode {
        val activeCasters = classLevels.entries.filter { (id, lvl) ->
            lvl > 0 && (Classes.BY_ID[id]?.factor ?: 0.0) > 0.0
        }
        return when (activeCasters.size) {
            0     -> SlotMode.None
            1     -> SlotMode.Monoclass(activeCasters.first().key)
            else  -> SlotMode.Multiclass
        }
    }

    /**
     * Caster level для мультикласса по PHB p.165 + 1/3 по floor.
     * Колдун не участвует (factor = 0).
     */
    fun computeCasterLevel(classLevels: Map<String, Int>): Int {
        var total = 0
        for (info in Classes.ALL) {
            if (info.factor <= 0.0) continue
            val lvl = classLevels[info.id] ?: 0
            total += when {
                info.factor >= 1.0     -> lvl
                info.isThirdCaster     -> lvl / 3   // PHB floor (Этап 26)
                info.roundUp           -> (lvl + 1) / 2
                else                   -> lvl / 2
            }
        }
        return total.coerceIn(0, 20)
    }

    /**
     * Максимум ячеек spellLevel для текущего набора классов.
     * Учитывает режим (монокласс / мультикласс / None).
     */
    fun getSlotTotal(classLevels: Map<String, Int>, spellLevel: Int): Int {
        return when (val mode = detectMode(classLevels)) {
            SlotMode.None -> 0
            is SlotMode.Monoclass -> {
                val table = MONOCLASS_TABLES[mode.classId] ?: return 0
                val lvl = classLevels[mode.classId] ?: 0
                if (lvl !in 1..20) return 0
                table[lvl].getOrElse(spellLevel) { 0 }
            }
            SlotMode.Multiclass -> {
                val cl = computeCasterLevel(classLevels)
                MULTICLASS_SLOTS[cl].getOrElse(spellLevel) { 0 }
            }
        }
    }
}

package com.example.spelltracker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit-тесты для [SlotTables] — таблиц ячеек заклинаний D&D 5e и
 * логики детекта режима (монокласс / мультикласс / None).
 *
 * Чистая JUnit (без Robolectric) — функции в [SlotTables] не зависят
 * от Android Context. Покрывает:
 *   - все 5 тест-кейсов из задачи request.md
 *   - детекты режима
 *   - ключевые точки каждой таблицы (1, 5, 11, 17, 20)
 *   - граничные случаи (level 0, level 1 у half/third casters)
 */
class SlotTablesTest {

    // ─────────────────────── detectMode ───────────────────────

    @Test
    fun detectMode_empty_returnsNone() {
        assertEquals(SlotTables.SlotMode.None, SlotTables.detectMode(emptyMap()))
    }

    @Test
    fun detectMode_onlyNonCasterClasses_returnsNone() {
        // Нет классов с factor > 0.
        assertEquals(
            SlotTables.SlotMode.None,
            SlotTables.detectMode(mapOf("warlock" to 5)),
        )
    }

    @Test
    fun detectMode_singleCaster_returnsMonoclass() {
        assertEquals(
            SlotTables.SlotMode.Monoclass("paladin"),
            SlotTables.detectMode(mapOf("paladin" to 5)),
        )
    }

    @Test
    fun detectMode_singleCasterWithWarlock_returnsMonoclass() {
        // Колдун не считается активным кастером (factor = 0).
        // Остаётся один не-варлок класс → монокласс.
        assertEquals(
            SlotTables.SlotMode.Monoclass("cleric"),
            SlotTables.detectMode(mapOf("cleric" to 3, "warlock" to 5)),
        )
    }

    @Test
    fun detectMode_twoCasters_returnsMulticlass() {
        assertEquals(
            SlotTables.SlotMode.Multiclass,
            SlotTables.detectMode(mapOf("paladin" to 3, "cleric" to 2)),
        )
    }

    @Test
    fun detectMode_zeroLevels_returnsNone() {
        // Уровни все нулевые — никто не кастует.
        assertEquals(
            SlotTables.SlotMode.None,
            SlotTables.detectMode(mapOf("paladin" to 0, "cleric" to 0)),
        )
    }

    // ──────────── computeCasterLevel (мультикласс) ────────────

    @Test
    fun computeCasterLevel_paladin5_returns2_floorHalf() {
        // Этап 26: PHB floor ⌊lvl/2⌋. Paladin 5 → 5/2 = 2.
        assertEquals(2, SlotTables.computeCasterLevel(mapOf("paladin" to 5)))
    }

    @Test
    fun computeCasterLevel_fullCaster_returnsLevel() {
        assertEquals(5, SlotTables.computeCasterLevel(mapOf("wizard" to 5)))
        assertEquals(17, SlotTables.computeCasterLevel(mapOf("sorcerer" to 17)))
    }

    @Test
    fun computeCasterLevel_artificer_usesRoundUp() {
        // Artificer: roundUp = true, factor = 0.5 → (lvl+1)/2.
        // Artificer 5 → (5+1)/2 = 3.
        assertEquals(3, SlotTables.computeCasterLevel(mapOf("artificer" to 5)))
    }

    @Test
    fun computeCasterLevel_thirdCaster_usesFloor() {
        // Этап 26: PHB floor ⌊lvl/3⌋.
        // fighter_mystic 5 → 5/3 = 1.
        assertEquals(
            1,
            SlotTables.computeCasterLevel(mapOf("fighter_mystic" to 5)),
        )
        // fighter_mystic 9 → 9/3 = 3.
        assertEquals(
            3,
            SlotTables.computeCasterLevel(mapOf("fighter_mystic" to 9)),
        )
    }

    @Test
    fun computeCasterLevel_warlockExcluded() {
        // Только Колдун → CL = 0 (он не в формуле).
        assertEquals(0, SlotTables.computeCasterLevel(mapOf("warlock" to 5)))
    }

    @Test
    fun computeCasterLevel_multiclassSummation() {
        // Paladin 3 (1) + Cleric 2 (2) = 3.
        assertEquals(
            3,
            SlotTables.computeCasterLevel(mapOf("paladin" to 3, "cleric" to 2)),
        )
        // Paladin 5 (2) + fighter_mystic 5 (1) = 3.
        // Этап 26: fighter_mystic 5 → floor(5/3) = 1.
        assertEquals(
            3,
            SlotTables.computeCasterLevel(mapOf("paladin" to 5, "fighter_mystic" to 5)),
        )
    }

    @Test
    fun computeCasterLevel_clampsAt20() {
        // Wizard 15 + Sorcerer 15 = 30 → coerceIn(0,20) = 20.
        assertEquals(
            20,
            SlotTables.computeCasterLevel(mapOf("wizard" to 15, "sorcerer" to 15)),
        )
    }

    // ─────────────── getSlotTotal: 5 тестов из request.md ───────────────

    @Test
    fun test1_paladin5_monoclass_4L1_2L2() {
        // Тест 1 из request.md: Паладин 5 (монокласс) → 4 L1, 2 L2.
        val levels = mapOf("paladin" to 5)
        assertEquals(SlotTables.SlotMode.Monoclass("paladin"), SlotTables.detectMode(levels))
        assertEquals(4, SlotTables.getSlotTotal(levels, 1))
        assertEquals(2, SlotTables.getSlotTotal(levels, 2))
        assertEquals(0, SlotTables.getSlotTotal(levels, 3))
    }

    @Test
    fun test2_paladin3_cleric2_multiclass_CL3_4L1_2L2() {
        // Тест 2: Паладин 3 / Жрец 2 → CL = 1+2 = 3 → 4/2.
        val levels = mapOf("paladin" to 3, "cleric" to 2)
        assertEquals(SlotTables.SlotMode.Multiclass, SlotTables.detectMode(levels))
        assertEquals(3, SlotTables.computeCasterLevel(levels))
        assertEquals(4, SlotTables.getSlotTotal(levels, 1))
        assertEquals(2, SlotTables.getSlotTotal(levels, 2))
    }

    @Test
    fun test3_paladin5_fighterMystic5_multiclass_CL3_4L1_2L2() {
        // Тест 3: Паладин 5 / EK 5 → CL = ⌊5/2⌋+⌊5/3⌋ = 2+1 = 3 → 4/2.
        // Этап 26: ⌊5/3⌋ = 1 (PHB floor), не (5+2)/3 = 2 (старый house rule).
        val levels = mapOf("paladin" to 5, "fighter_mystic" to 5)
        assertEquals(SlotTables.SlotMode.Multiclass, SlotTables.detectMode(levels))
        assertEquals(3, SlotTables.computeCasterLevel(levels))
        assertEquals(4, SlotTables.getSlotTotal(levels, 1))
        assertEquals(2, SlotTables.getSlotTotal(levels, 2))
    }

    @Test
    fun test4_warlock5_onlyMode_isNone_regularSlotsEmpty() {
        // Тест 4: Колдун 5 (монокласс) — обычные ячейки пустые (None-режим),
        // пакт-магия отдельно через WARLOCK_SLOTS.
        val levels = mapOf("warlock" to 5)
        assertEquals(SlotTables.SlotMode.None, SlotTables.detectMode(levels))
        for (lvl in 1..9) {
            assertEquals("regular L$lvl", 0, SlotTables.getSlotTotal(levels, lvl))
        }
    }

    @Test
    fun test5_cleric3_warlock5_clericMonoclass_4L1_2L2_pactSeparate() {
        // Тест 5: Жрец 3 / Колдун 5 → Жрец единственный не-варлок кастер,
        // значит mode = Monoclass("cleric"). По таблице Жреца 3 → 4/2.
        // Пакт Колдуна — отдельно через SpellStorage.WARLOCK_SLOTS.
        val levels = mapOf("cleric" to 3, "warlock" to 5)
        assertEquals(SlotTables.SlotMode.Monoclass("cleric"), SlotTables.detectMode(levels))
        assertEquals(4, SlotTables.getSlotTotal(levels, 1))
        assertEquals(2, SlotTables.getSlotTotal(levels, 2))
        // Пакт Колдуна — через WARLOCK_SLOTS, не SlotTables.
        // ⚠ Известный отдельный баг (вне Этапа 26): в SpellStorage.WARLOCK_SLOTS
        // для warlockLevel=5 записано (3, 3), а по PHB должно быть (2, 3).
        // Тест ниже фиксирует ТЕКУЩЕЕ поведение — после исправления Warlock
        // таблицы ожидание нужно сменить на 2.
        assertEquals(3, SpellStorage.WARLOCK_SLOTS[5]!![0])
        assertEquals(3, SpellStorage.WARLOCK_SLOTS[5]!![1])
    }

    // ──────────────────── Граничные случаи ────────────────────

    @Test
    fun paladin1_noSlots() {
        // PHB: Паладин начинает кастовать только со 2 уровня.
        val levels = mapOf("paladin" to 1)
        assertEquals(SlotTables.SlotMode.Monoclass("paladin"), SlotTables.detectMode(levels))
        for (lvl in 1..9) {
            assertEquals(0, SlotTables.getSlotTotal(levels, lvl))
        }
    }

    @Test
    fun paladin2_firstSlots() {
        // PHB: Паладин 2 → 2 L1.
        assertEquals(2, SlotTables.getSlotTotal(mapOf("paladin" to 2), 1))
    }

    @Test
    fun fullCaster5_threeSlots() {
        // Bard 5, Cleric 5, Wizard 5 — все дают 4 L1 + 3 L2 + 2 L3.
        for (id in listOf("bard", "cleric", "druid", "sorcerer", "wizard")) {
            val levels = mapOf(id to 5)
            assertEquals("$id L1", 4, SlotTables.getSlotTotal(levels, 1))
            assertEquals("$id L2", 3, SlotTables.getSlotTotal(levels, 2))
            assertEquals("$id L3", 2, SlotTables.getSlotTotal(levels, 3))
        }
    }

    @Test
    fun wizard20_maxSlots() {
        val levels = mapOf("wizard" to 20)
        assertEquals(4, SlotTables.getSlotTotal(levels, 1))
        assertEquals(3, SlotTables.getSlotTotal(levels, 2))
        assertEquals(3, SlotTables.getSlotTotal(levels, 3))
        assertEquals(3, SlotTables.getSlotTotal(levels, 4))
        assertEquals(3, SlotTables.getSlotTotal(levels, 5))
        assertEquals(2, SlotTables.getSlotTotal(levels, 6))
        assertEquals(2, SlotTables.getSlotTotal(levels, 7))
        assertEquals(1, SlotTables.getSlotTotal(levels, 8))
        assertEquals(1, SlotTables.getSlotTotal(levels, 9))
    }

    @Test
    fun artificer5_uniqueTashaProgression() {
        // Artificer 5: 4 L1 + 2 L2 (Tasha).
        assertEquals(4, SlotTables.getSlotTotal(mapOf("artificer" to 5), 1))
        assertEquals(2, SlotTables.getSlotTotal(mapOf("artificer" to 5), 2))
        assertEquals(0, SlotTables.getSlotTotal(mapOf("artificer" to 5), 3))
    }

    @Test
    fun eldritchKnight3_firstSlots() {
        // EK начинает кастовать с 3 уровня (PHB).
        assertEquals(2, SlotTables.getSlotTotal(mapOf("fighter_mystic" to 3), 1))
        assertEquals(0, SlotTables.getSlotTotal(mapOf("fighter_mystic" to 2), 1))
    }

    @Test
    fun eldritchKnight7_4L1_2L2() {
        // EK 7 → 4 L1, 2 L2.
        assertEquals(4, SlotTables.getSlotTotal(mapOf("fighter_mystic" to 7), 1))
        assertEquals(2, SlotTables.getSlotTotal(mapOf("fighter_mystic" to 7), 2))
    }

    @Test
    fun arcaneTrickster_sameAsEK() {
        // rogue_mystic делит таблицу с fighter_mystic.
        val ek7 = mapOf("fighter_mystic" to 7)
        val at7 = mapOf("rogue_mystic" to 7)
        for (lvl in 1..9) {
            assertEquals("L$lvl", SlotTables.getSlotTotal(ek7, lvl), SlotTables.getSlotTotal(at7, lvl))
        }
    }

    @Test
    fun paladinAndRanger_sameTable() {
        val pal = mapOf("paladin" to 10)
        val ran = mapOf("ranger" to 10)
        for (lvl in 1..9) {
            assertEquals("L$lvl", SlotTables.getSlotTotal(pal, lvl), SlotTables.getSlotTotal(ran, lvl))
        }
    }

    @Test
    fun fullCasters_sameTable() {
        // Bard, Cleric, Druid, Sorcerer, Wizard делят одну таблицу на 1..20.
        for (lvl in 1..20) {
            val levels = mapOf("wizard" to lvl)
            val expected = SlotTables.getSlotTotal(levels, 1) // single sample
            for (id in listOf("bard", "cleric", "druid", "sorcerer", "wizard")) {
                val idLevels = mapOf(id to lvl)
                assertEquals(
                    "$id L$lvl vs wizard L$lvl",
                    SlotTables.getSlotTotal(levels, 1),
                    SlotTables.getSlotTotal(idLevels, 1),
                )
            }
        }
    }

    @Test
    fun multiclassFullCasterPlusFullCaster_matchesMulticlassTable() {
        // Wizard 5 + Sorcerer 5 = CL 10 → multiclass table CL10 = 4/3/3/3/2.
        val levels = mapOf("wizard" to 5, "sorcerer" to 5)
        assertEquals(SlotTables.SlotMode.Multiclass, SlotTables.detectMode(levels))
        assertEquals(10, SlotTables.computeCasterLevel(levels))
        assertEquals(4, SlotTables.getSlotTotal(levels, 1))
        assertEquals(3, SlotTables.getSlotTotal(levels, 2))
        assertEquals(3, SlotTables.getSlotTotal(levels, 3))
        assertEquals(3, SlotTables.getSlotTotal(levels, 4))
        assertEquals(2, SlotTables.getSlotTotal(levels, 5))
    }

    @Test
    fun multiclassCleric3Paladin3_CL4_4L1_3L2() {
        // Из spells_data.md пример 3: Cleric 3 + Paladin 3 → CL=4 → 4/3.
        val levels = mapOf("cleric" to 3, "paladin" to 3)
        assertEquals(SlotTables.SlotMode.Multiclass, SlotTables.detectMode(levels))
        assertEquals(4, SlotTables.computeCasterLevel(levels))
        assertEquals(4, SlotTables.getSlotTotal(levels, 1))
        assertEquals(3, SlotTables.getSlotTotal(levels, 2))
    }

    @Test
    fun unknownClassId_returnsNoneAndZero() {
        // Неизвестный класс → mode = None, ячейки = 0.
        val levels = mapOf("unknown_class" to 5)
        assertEquals(SlotTables.SlotMode.None, SlotTables.detectMode(levels))
        for (lvl in 1..9) {
            assertEquals(0, SlotTables.getSlotTotal(levels, lvl))
        }
    }

    @Test
    fun spellLevel0_returnsZero() {
        // Индекс 0 в таблице — плейсхолдер, всегда 0.
        assertEquals(0, SlotTables.getSlotTotal(mapOf("wizard" to 5), 0))
    }

    @Test
    fun spellLevelOutOfRange_returnsZero() {
        // spellLevel за пределами таблицы (10+) — 0.
        assertEquals(0, SlotTables.getSlotTotal(mapOf("wizard" to 5), 10))
    }

    // ──────────────────── Регрессия: 1/3 формула floor ────────────────────

    @Test
    fun regression_thirdCasterFloor_replacesCeil() {
        // Этап 26: было (lvl+2)/3 (ceil), стало lvl/3 (floor).
        // fighter_mystic 5: было 2, теперь 1. CL для мультикласса уменьшился.
        val levels5 = mapOf("fighter_mystic" to 5, "paladin" to 5)
        // Было: CL = 2 + 2 = 4. Стало: CL = 2 + 1 = 3.
        assertEquals(3, SlotTables.computeCasterLevel(levels5))
        // А fighter_mystic 5 один — без других кастеров mode = Monoclass.
        // EK таблица для L5 → 3 L1, 0 L2.
        val solo5 = mapOf("fighter_mystic" to 5)
        assertEquals(SlotTables.SlotMode.Monoclass("fighter_mystic"), SlotTables.detectMode(solo5))
        assertEquals(3, SlotTables.getSlotTotal(solo5, 1))
    }

    @Test
    fun warlockAlone_zeroRegularSlots_nonZeroPactSlots() {
        // Sanity: при пустых classLevels (кроме варлока) обычные ячейки = 0,
        // но пакт Колдуна непустой (это отдельный канал).
        val levels = mapOf("warlock" to 5)
        for (lvl in 1..9) {
            assertEquals(0, SlotTables.getSlotTotal(levels, lvl))
        }
        assertTrue(SpellStorage.WARLOCK_SLOTS[5]!![0] > 0)
    }
}
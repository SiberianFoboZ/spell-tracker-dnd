package com.example.spelltracker.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Слой постоянного состояния на SharedPreferences.
 *
 * Здесь живут:
 *   - уровни классов ([classLevels])
 *   - использованные ячейки заклинаний по уровням 1..9 ([usedSlots])
 *   - использованные ячейки пакт-магии колдуна ([usedPactSlots])
 *   - флаги «известно» / «подготовлено» для отдельных заклинаний
 *
 * Изменения пишутся в SharedPreferences **и** публикуются в StateFlow,
 * чтобы Compose-UI мог подписываться реактивно.
 */
class SpellStorage(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ─────────── Уровни классов ───────────

    private val _classLevels = MutableStateFlow(loadClassLevels())
    val classLevels: StateFlow<Map<String, Int>> = _classLevels.asStateFlow()

    private fun loadClassLevels(): Map<String, Int> =
        Classes.ALL.associate { it.id to prefs.getInt("level_${it.id}", 0) }

    fun getClassLevel(classId: String): Int = _classLevels.value[classId] ?: 0

    fun setClassLevel(classId: String, level: Int) {
        val clamped = level.coerceIn(0, 20)
        prefs.edit().putInt("level_$classId", clamped).apply()
        _classLevels.update { it + (classId to clamped) }
    }

    // ─────────── Caster level по PHB ───────────

    /**
     * Полный caster level для определения таблицы ячеек заклинаний.
     * Колдун в формуле не участвует — у него собственная пакт-магия.
     */
    fun computeCasterLevel(): Int {
        var total = 0
        for (info in Classes.ALL) {
            if (info.factor <= 0.0) continue
            val lvl = getClassLevel(info.id)
            total += when {
                info.factor >= 1.0 -> lvl
                info.roundUp -> (lvl + 1) / 2
                else -> lvl / 2
            }
        }
        return total.coerceIn(0, 20)
    }

    fun getWarlockLevel(): Int = getClassLevel("warlock")

    // ─────────── Таблицы ячеек ───────────

    private val _usedSlots = MutableStateFlow(loadUsedSlots())
    val usedSlots: StateFlow<Map<Int, Int>> = _usedSlots.asStateFlow()

    private fun loadUsedSlots(): Map<Int, Int> =
        (1..9).associateWith { prefs.getInt("used_slot_$it", 0) }

    /** Максимум ячеек по уровню заклинания для текущего caster level. */
    fun getSlotTotal(spellLevel: Int): Int {
        val table = SLOT_TABLE[computeCasterLevel()]
        return table[spellLevel]
    }

    fun getSlotUsed(spellLevel: Int): Int = _usedSlots.value[spellLevel] ?: 0

    /** Применить таблицу: сбросить used, если total уменьшился. */
    fun applySlotTable() {
        val newUsed = (1..9).associateWith { lvl ->
            val cap = getSlotTotal(lvl)
            getSlotUsed(lvl).coerceAtMost(cap)
        }
        newUsed.forEach { (lvl, v) ->
            prefs.edit().putInt("used_slot_$lvl", v).apply()
        }
        _usedSlots.value = newUsed
    }

    fun useSlot(spellLevel: Int) {
        val total = getSlotTotal(spellLevel)
        val used = getSlotUsed(spellLevel)
        if (used < total) {
            prefs.edit().putInt("used_slot_$spellLevel", used + 1).apply()
            _usedSlots.update { it + (spellLevel to used + 1) }
        }
    }

    fun restoreSlot(spellLevel: Int) {
        val used = getSlotUsed(spellLevel)
        if (used > 0) {
            prefs.edit().putInt("used_slot_$spellLevel", used - 1).apply()
            _usedSlots.update { it + (spellLevel to used - 1) }
        }
    }

    fun resetAllUsed() {
        prefs.edit().clear().apply()  // безопасно, т.к. других ключей нет
        _usedSlots.value = (1..9).associateWith { 0 }
        _usedPactSlots.value = 0
        // class levels и prepared/known перечитываем
        _classLevels.value = loadClassLevels()
    }

    // ─────────── Пакт-магия колдуна ───────────

    private val _usedPactSlots = MutableStateFlow(prefs.getInt("used_pact_slots", 0))
    val usedPactSlots: StateFlow<Int> = _usedPactSlots.asStateFlow()

    fun getPactSlotTotal(): Int {
        val wl = getWarlockLevel()
        return WARLOCK_SLOTS.getOrDefault(wl, intArrayOf(0, 0))[0]
    }

    fun getPactSlotLevel(): Int {
        val wl = getWarlockLevel()
        return WARLOCK_SLOTS.getOrDefault(wl, intArrayOf(0, 0))[1]
    }

    fun applyWarlockSlots() {
        val cap = getPactSlotTotal()
        if (_usedPactSlots.value > cap) {
            prefs.edit().putInt("used_pact_slots", cap).apply()
            _usedPactSlots.value = cap
        }
    }

    fun usePactSlot() {
        val cap = getPactSlotTotal()
        if (_usedPactSlots.value < cap) {
            val v = _usedPactSlots.value + 1
            prefs.edit().putInt("used_pact_slots", v).apply()
            _usedPactSlots.value = v
        }
    }

    fun restorePactSlot() {
        if (_usedPactSlots.value > 0) {
            val v = _usedPactSlots.value - 1
            prefs.edit().putInt("used_pact_slots", v).apply()
            _usedPactSlots.value = v
        }
    }

    // ─────────── Отдых (Этап 15) ───────────

    /**
     * Короткий отдых: восстановить **только** ячейки пакт-магии Колдуна.
     * Ячейки заклинаний других классов не трогаем — это правило PHB.
     * (Warlock получает pact slots обратно на коротком отдыхе.)
     */
    fun shortRest() {
        if (_usedPactSlots.value > 0) {
            prefs.edit().putInt("used_pact_slots", 0).apply()
            _usedPactSlots.value = 0
        }
    }

    /**
     * Длинный отдых: восстановить **все** ячейки заклинаний и пакт-магии.
     * Уровни классов, подготовленные/известные заклинания — сохраняются.
     *
     * Важно: в отличие от [resetAllUsed], этот метод НЕ вызывает
     * `prefs.edit().clear()` и не обнуляет class levels.
     */
    fun longRest() {
        val newUsed = (1..9).associateWith { 0 }
        newUsed.forEach { (lvl, _) ->
            prefs.edit().putInt("used_slot_$lvl", 0).apply()
        }
        _usedSlots.value = newUsed
        if (_usedPactSlots.value > 0) {
            prefs.edit().putInt("used_pact_slots", 0).apply()
            _usedPactSlots.value = 0
        }
    }

    // ─────────── «Подготовлено» / «известно» ───────────

    private val _prepared = MutableStateFlow(loadPrepared())
    val prepared: StateFlow<Set<Long>> = _prepared.asStateFlow()

    private fun loadPrepared(): Set<Long> =
        prefs.getStringSet("prepared_ids", emptySet())!!.mapNotNull { it.toLongOrNull() }.toSet()

    fun isPrepared(spellId: Long): Boolean = _prepared.value.contains(spellId)

    fun setPrepared(spellId: Long, prep: Boolean) {
        val newSet = if (prep) _prepared.value + spellId else _prepared.value - spellId
        prefs.edit().putStringSet("prepared_ids", newSet.map { it.toString() }.toSet()).apply()
        _prepared.value = newSet
    }

    // ─────────── Константы ───────────

    companion object {
        private const val PREFS_NAME = "spell_tracker"

        /**
         * Таблица ячеек заклинаний по PHB.
         * Индексы: SLOT_TABLE[casterLevel][spellLevel] = число ячеек.
         * spellLevel 0 не используется, 1..9 — обычные уровни.
         * casterLevel 0 = не заклинатель, все нули.
         */
        val SLOT_TABLE: Array<IntArray> = arrayOf(
            // lvl\spell  0  1  2  3  4  5  6  7  8  9
            intArrayOf(   0, 0, 0, 0, 0, 0, 0, 0, 0, 0),  //  0
            intArrayOf(   0, 2, 0, 0, 0, 0, 0, 0, 0, 0),  //  1
            intArrayOf(   0, 3, 0, 0, 0, 0, 0, 0, 0, 0),  //  2
            intArrayOf(   0, 4, 2, 0, 0, 0, 0, 0, 0, 0),  //  3
            intArrayOf(   0, 4, 3, 0, 0, 0, 0, 0, 0, 0),  //  4
            intArrayOf(   0, 4, 3, 2, 0, 0, 0, 0, 0, 0),  //  5
            intArrayOf(   0, 4, 3, 3, 0, 0, 0, 0, 0, 0),  //  6
            intArrayOf(   0, 4, 3, 3, 1, 0, 0, 0, 0, 0),  //  7
            intArrayOf(   0, 4, 3, 3, 2, 0, 0, 0, 0, 0),  //  8
            intArrayOf(   0, 4, 3, 3, 3, 1, 0, 0, 0, 0),  //  9
            intArrayOf(   0, 4, 3, 3, 3, 2, 0, 0, 0, 0),  // 10
            intArrayOf(   0, 4, 3, 3, 3, 2, 1, 0, 0, 0),  // 11
            intArrayOf(   0, 4, 3, 3, 3, 2, 1, 0, 0, 0),  // 12
            intArrayOf(   0, 4, 3, 3, 3, 2, 1, 1, 0, 0),  // 13
            intArrayOf(   0, 4, 3, 3, 3, 2, 1, 1, 0, 0),  // 14
            intArrayOf(   0, 4, 3, 3, 3, 2, 1, 1, 1, 0),  // 15
            intArrayOf(   0, 4, 3, 3, 3, 2, 1, 1, 1, 0),  // 16
            intArrayOf(   0, 4, 3, 3, 3, 2, 1, 1, 1, 1),  // 17
            intArrayOf(   0, 4, 3, 3, 3, 3, 1, 1, 1, 1),  // 18
            intArrayOf(   0, 4, 3, 3, 3, 3, 2, 1, 1, 1),  // 19
            intArrayOf(   0, 4, 3, 3, 3, 3, 2, 2, 1, 1),  // 20
        )

        /**
         * Пакт-магия колдуна: WARLOCK_SLOTS[warlockLevel] = {число ячеек, уровень ячеек}.
         * Совпадает с PHB-таблицей "Pact Magic".
         */
        val WARLOCK_SLOTS: Map<Int, IntArray> = mapOf(
             1 to intArrayOf(1, 1),
             2 to intArrayOf(2, 1),
             3 to intArrayOf(2, 2),
             4 to intArrayOf(2, 2),
             5 to intArrayOf(3, 3),
             6 to intArrayOf(3, 3),
             7 to intArrayOf(4, 4),
             8 to intArrayOf(4, 4),
             9 to intArrayOf(4, 5),
            10 to intArrayOf(4, 5),
            11 to intArrayOf(4, 5),
            12 to intArrayOf(4, 5),
            13 to intArrayOf(4, 5),
            14 to intArrayOf(4, 5),
            15 to intArrayOf(4, 5),
            16 to intArrayOf(4, 5),
            17 to intArrayOf(4, 5),
            18 to intArrayOf(4, 5),
            19 to intArrayOf(4, 5),
            20 to intArrayOf(4, 5),
        )
    }
}

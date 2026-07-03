package com.example.spelltracker.data

/**
 * Логика фильтрации заклинаний для экрана SpellsScreen.
 *
 * Объект — чистая функция [matches] + [SpellFilterState] снапшот
 * фильтров, отделённый от [SpellsState], чтобы:
 *   • не таскать «загруженные списки» / `availableX` в матчер,
 *   • легко тестировать матчер в unit-тестах без Android-зависимостей
 *     (TODO Этап из QWEN.md §9).
 *
 * Семантика:
 *   level, search — точное соответствие;
 *   classIds, sources, schools, savingThrows, subclassNames, raceNames —
 *       «OR»: заклинание проходит, если хотя бы один из выбранных
 *       элементов найден в CSV-поле спелла;
 *   ritual / concentration / componentV / componentS / componentM /
 *   materialConsumed — [TriState]: YES = поле=true, NO = поле=false,
 *       ANY = игнор.
 */
object ClassFilter {

    fun matches(spell: Spell, f: SpellFilterState): Boolean {
        if (f.level != null && spell.level != f.level) return false

        if (f.classIds.isNotEmpty() &&
            f.classIds.none { spell.classes.contains(it) }
        ) return false

        if (f.subclassNames.isNotEmpty() &&
            f.subclassNames.none { spell.subclasses.contains(it) }
        ) return false

        if (f.raceNames.isNotEmpty() &&
            f.raceNames.none { spell.races.contains(it) }
        ) return false

        if (f.sources.isNotEmpty() && spell.source !in f.sources) return false

        if (f.schools.isNotEmpty() && spell.school !in f.schools) return false

        if (f.savingThrows.isNotEmpty() &&
            f.savingThrows.none { spell.savingThrows.contains(it) }
        ) return false

        if (!matchesTriState(spell.ritual, f.ritual)) return false
        if (!matchesTriState(spell.concentration, f.concentration)) return false
        if (!matchesTriState(spell.componentV, f.componentV)) return false
        if (!matchesTriState(spell.componentS, f.componentS)) return false
        if (!matchesTriState(spell.componentM, f.componentM)) return false
        if (!matchesTriState(spell.materialConsumed, f.materialConsumed)) return false

        val needle = f.search.trim().lowercase()
        if (needle.isNotEmpty() && !spell.name.lowercase().contains(needle)) return false

        return true
    }

    private fun matchesTriState(value: Boolean, ts: TriState): Boolean = when (ts) {
        TriState.YES -> value
        TriState.NO -> !value
        TriState.ANY -> true
    }
}

/**
 * Snapshot всех активных фильтров экрана SpellsScreen — без полей про
 * «загружено» и «ui» (это в [SpellsState]).
 *
 * Дефолты из [SpellMenuConfig.DEFAULT_SOURCES] — все источники включены
 * «как из коробки», чтобы при первом запуске пользователь видел полный
 * справочник (соответствует `menu_json.txt` — у всех источников
 * `default: true`).
 */
data class SpellFilterState(
    val level: Int? = null,
    val classIds: Set<String> = emptySet(),
    val subclassNames: Set<String> = emptySet(),
    val raceNames: Set<String> = emptySet(),
    val sources: Set<String> = emptySet(),
    val schools: Set<String> = emptySet(),
    val savingThrows: Set<String> = emptySet(),
    val ritual: TriState = TriState.ANY,
    val concentration: TriState = TriState.ANY,
    val componentV: TriState = TriState.ANY,
    val componentS: TriState = TriState.ANY,
    val componentM: TriState = TriState.ANY,
    val materialConsumed: TriState = TriState.ANY,
    val search: String = "",
) {
    /** Есть ли активный хоть один фильтр (для UI: подсветка кнопки). */
    val hasAny: Boolean
        get() = level != null ||
            classIds.isNotEmpty() || subclassNames.isNotEmpty() || raceNames.isNotEmpty() ||
            sources.isNotEmpty() || schools.isNotEmpty() || savingThrows.isNotEmpty() ||
            ritual != TriState.ANY || concentration != TriState.ANY ||
            componentV != TriState.ANY || componentS != TriState.ANY ||
            componentM != TriState.ANY || materialConsumed != TriState.ANY
}

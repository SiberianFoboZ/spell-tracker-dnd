package com.example.spelltracker.data

/**
 * Логика фильтрации заклинаний для экрана SpellsScreen.
 *
 * Объект — чистая функция [matches] + [SpellFilterState] снапшот
 * фильтров, отделённый от [SpellsState], чтобы:
 *   • не таскать «загруженные списки» / `availableX` в матчер,
 *   • легко тестировать матчер в unit-тестах без Android-зависимостей.
 *
 * Семантика:
 *   level, search — точное соответствие;
 *   classIds, sources, schools, savingThrows, subclassNames —
 *       «OR»: заклинание проходит, если хотя бы один из выбранных
 *       элементов найден в CSV-поле спелла;
 *   ritual / concentration — [TriState]: YES = поле=true, NO = поле=false,
 *       ANY = игнор.
 *   requiredComponents — AND: для каждого выбранного компонента спелл
 *       обязан его иметь (эмулирует PHB-нотацию «В, С» = оба требуются).
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

        if (f.sources.isNotEmpty() && spell.source !in f.sources) return false

        if (f.schools.isNotEmpty() && spell.school !in f.schools) return false

        if (f.savingThrows.isNotEmpty() &&
            f.savingThrows.none { spell.savingThrows.contains(it) }
        ) return false

        if (!matchesTriState(spell.ritual, f.ritual)) return false
        if (!matchesTriState(spell.concentration, f.concentration)) return false

        // Компоненты — AND: каждый выбранный флаг обязан быть у спелла.
        if (f.requiredComponents.isNotEmpty()) {
            for (flag in f.requiredComponents) {
                if (!ComponentFlag.spellHas(flag, spell)) return false
            }
        }

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
 * Snapshot активных фильтров экрана SpellsScreen — без полей про
 * «загружено» и «ui» (это в [SpellsState]).
 *
 * Расы вынесены из фильтра (в [Spec] их можно показать в детальном,
 * но как отдельная ось фильтра они перегружали экран).
 *
 * Компоненты теперь ОДИН `Set<ComponentFlag>` вместо четырёх
 * независимых TriState — выбранный набор означает «спелл обязан иметь все».
 */
data class SpellFilterState(
    val level: Int? = null,
    val classIds: Set<String> = emptySet(),
    val subclassNames: Set<String> = emptySet(),
    val sources: Set<String> = emptySet(),
    val schools: Set<String> = emptySet(),
    val savingThrows: Set<String> = emptySet(),
    val ritual: TriState = TriState.ANY,
    val concentration: TriState = TriState.ANY,
    val requiredComponents: Set<ComponentFlag> = emptySet(),
    val search: String = "",
) {
    /** Есть ли активный хоть один фильтр (для UI: подсветка кнопки). */
    val hasAny: Boolean
        get() = level != null ||
            classIds.isNotEmpty() || subclassNames.isNotEmpty() ||
            sources.isNotEmpty() || schools.isNotEmpty() || savingThrows.isNotEmpty() ||
            ritual != TriState.ANY || concentration != TriState.ANY ||
            requiredComponents.isNotEmpty()
}

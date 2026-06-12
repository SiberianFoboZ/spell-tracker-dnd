package com.example.spelltracker.data

/**
 * Логика фильтрации заклинаний для экрана SpellsScreen.
 *
 * Объект содержит единственный метод [matches], который проверяет,
 * подходит ли заклинание под текущий набор фильтров. Сам список
 * заклинаний и текущие значения фильтров передаются снаружи, чтобы
 * ViewModel могла комбинировать StateFlow-и.
 */
object ClassFilter {

    /**
     * @param spell заклинание
     * @param level выбранный уровень (null = любой; 0 = только заговоры)
     * @param classIds выбранные id классов (пусто = любой)
     * @param search строка поиска (пусто = без фильтра)
     */
    fun matches(
        spell: Spell,
        level: Int?,
        classIds: Set<String>,
        search: String,
    ): Boolean {
        if (level != null && spell.level != level) return false
        if (classIds.isNotEmpty() && classIds.none { spell.classes.contains(it) }) return false
        val needle = search.trim().lowercase()
        if (needle.isNotEmpty() && !spell.name.lowercase().contains(needle)) return false
        return true
    }

    /**
     * Подсчитать, какие уровни заклинаний вообще встречаются в списке.
     * Полезно для отрисовки чипов уровней (0..9).
     */
    fun availableLevels(spells: List<Spell>): Set<Int> = spells.map { it.level }.toSet()
}

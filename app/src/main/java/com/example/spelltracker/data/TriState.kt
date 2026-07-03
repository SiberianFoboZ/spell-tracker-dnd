package com.example.spelltracker.data

/**
 * Трёхзначный фильтр: «да», «нет», «любое».
 *
 * Используется в фильтрах заклинаний для булевых полей
 * (ритуал, концентрация, наличие вербального/соматического/материального
 * компонента, расходуемость материала). «Любое» — пустое состояние,
 * ничего не фильтрует. Совпадает с menu_json.txt keys: yes / no / any.
 */
enum class TriState {
    YES, NO, ANY;

    /** Удобный хелпер для UI — следующее состояние по тапу. */
    fun next(): TriState = when (this) {
        ANY -> YES
        YES -> NO
        NO -> ANY
    }

    companion object {
        /** Распарсить строку «да»/«нет»/«любое» — на случай выкатки из preferences. */
        fun parse(s: String): TriState = when (s.lowercase()) {
            "yes", "да", "true" -> YES
            "no", "нет", "false" -> NO
            else -> ANY
        }
    }
}

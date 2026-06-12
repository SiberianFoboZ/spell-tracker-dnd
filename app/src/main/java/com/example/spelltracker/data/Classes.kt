package com.example.spelltracker.data

/**
 * Метаданные классов D&D 5e.
 *
 * Источник истины о том, какие классы вообще есть в приложении, как они
 * называются по-русски, откуда грузить JSON-файл с заклинаниями и какой
 * у них множитель для подсчёта caster level по правилам мультикласса
 * из Player's Handbook.
 *
 *   factor = 1.0   — полный заклинатель (бард, волшебник, друид, жрец, чародей)
 *   factor = 0.5   — полузаклинатель (паладин, следопыт, изобретатель)
 *   factor = 0.0   — не заклинатель, но всё равно в сетке (колдун обрабатывается отдельно)
 *   roundUp = true — для полузаклинателя округлять (lvl+1)/2
 *                    (нужно только изобретателю)
 */
object Classes {

    data class Info(
        val id: String,
        val name: String,
        val assetFile: String,
        val factor: Double,
        val roundUp: Boolean,
    )

    val ALL: List<Info> = listOf(
        Info("bard",       "Бард",         "бард.json",         1.0, false),
        Info("wizard",     "Волшебник",    "волшебник.json",    1.0, false),
        Info("druid",      "Друид",        "друид.json",        1.0, false),
        Info("cleric",     "Жрец",         "жрец.json",         1.0, false),
        Info("warlock",    "Колдун",       "колдун.json",       0.0, false),
        Info("paladin",    "Паладин",      "паладин.json",      0.5, false),
        Info("ranger",     "Следопыт",     "следопыт.json",     0.5, false),
        Info("sorcerer",   "Чародей",      "чародей.json",      1.0, false),
        Info("artificer",  "Изобретатель", "изобретатель.json", 0.5, true),
    )

    val BY_ID: Map<String, Info> = ALL.associateBy { it.id }

    /** Множество id классов, которые умеют колдовать (full/half caster). */
    val CASTER_IDS: Set<String> = ALL.filter { it.factor > 0.0 }.map { it.id }.toSet()
}

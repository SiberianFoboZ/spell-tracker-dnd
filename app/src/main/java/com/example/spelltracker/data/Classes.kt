package com.example.spelltracker.data

/**
 * Метаданные классов D&D 5e.
 *
 * Источник истины о том, какие классы вообще есть в приложении и как
 * они участвуют в подсчёте caster level по правилам мультикласса
 * из Player's Handbook:
 *
 *   factor = 1.0         — полный заклинатель (бард, волшебник, друид, жрец, чародей)
 *   factor = 0.5         — полузаклинатель (паладин, следопыт, изобретатель)
 *   factor = 0.0         — не заклинатель, но всё равно в сетке (колдун)
 *   roundUp = true       — для полузаклинателя округлять (lvl+1)/2
 *                          (нужно только изобретателю)
 *   isThirdCaster = true — Этап 19: 1/3 кастер (Eldritch Knight / Arcane
 *                          Trickster), caster level = ceil(lvl / 3).
 *                          Перебивает правило roundUp для дробных кастеров.
 *
 * Локализация имени класса: см. `com.example.spelltracker.ClassNames` —
 * `Info.nameRes()` возвращает `@StringRes Int`, UI резолвит через
 * `stringResource(...)`.
 *
 * Файл `assetFile` сохранён для справки (раньше загружали по одному
 * JSON на класс), но сейчас справочник заклинаний общий — один
 * `spells_normalized.json` в assets, см. [SpellParser.loadFromAssets].
 */
object Classes {

    data class Info(
        val id: String,
        @Suppress("unused") val assetFile: String,
        val factor: Double,
        val roundUp: Boolean,
        val isThirdCaster: Boolean = false,
    )

    val ALL: List<Info> = listOf(
        Info("bard",           "бард.json",         1.0, false),
        Info("wizard",         "волшебник.json",    1.0, false),
        Info("druid",          "друид.json",        1.0, false),
        Info("cleric",         "жрец.json",         1.0, false),
        Info("warlock",        "колдун.json",       0.0, false),
        Info("paladin",        "паладин.json",      0.5, false),
        Info("ranger",         "следопыт.json",     0.5, false),
        Info("sorcerer",       "чародей.json",      1.0, false),
        Info("artificer",      "изобретатель.json", 0.5, true),
        // Этап 19: архетипы-третьекастеры (1/3) — Воин/Eldritch Knight
        // и Плут/Arcane Trickster. По правилам пользователя caster level
        // считается как ceil(lvl / 3) — формула `(lvl + 2) / 3`. Это
        // **отличается** от стандартного PHB (там roundDown), но
        // соответствует тому, как игрок ведёт партию.
        Info("fighter_mystic", "fighter_mystic.json", 1.0 / 3.0, true, isThirdCaster = true),
        Info("rogue_mystic",   "rogue_mystic.json",   1.0 / 3.0, true, isThirdCaster = true),
    )

    val BY_ID: Map<String, Info> = ALL.associateBy { it.id }

    /**
     * Множество id классов, которые умеют колдовать (full/half/third caster).
     * Используется там, где нужно отличать «магов» от «не-магов» —
     * например, чтобы убрать Колдуна (factor=0) из списка заклинателей.
     */
    val CASTER_IDS: Set<String> = ALL.filter { it.factor > 0.0 }.map { it.id }.toSet()
}
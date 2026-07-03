package com.example.spelltracker

import androidx.annotation.StringRes
import com.example.spelltracker.data.Classes

/**
 * Централизованная карта `class_id → @StringRes Int` для локализации
 * имён классов из `Classes.kt`.
 *
 * UI читает имя через:
 * ```
 * val name = stringResource(info.nameRes())
 * ```
 * либо напрямую:
 * ```
 * val name = stringResource(ClassNames.byId.getValue(info.id))
 * ```
 *
 * Хранится в отдельном объекте на уровне пакета (а не внутри
 * `data/Classes`), чтобы data-слой оставался чистым Kotlin без
 * зависимости от Android-ресурсов.
 */
object ClassNames {
    private val map: Map<String, Int> = mapOf(
        "bard"           to R.string.class_bard,
        "wizard"         to R.string.class_wizard,
        "druid"          to R.string.class_druid,
        "cleric"         to R.string.class_cleric,
        "warlock"        to R.string.class_warlock,
        "paladin"        to R.string.class_paladin,
        "ranger"         to R.string.class_ranger,
        "sorcerer"       to R.string.class_sorcerer,
        "artificer"      to R.string.class_artificer,
        "fighter_mystic" to R.string.class_fighter_mystic,
        "rogue_mystic"   to R.string.class_rogue_mystic,
    )

    /**
     * Безопасный lookup: возвращает `@StringRes Int` имени класса
     * или [R.string.class_unknown] (на случай, если id не зарегистрирован).
     */
    @StringRes
    fun resFor(classId: String): Int =
        map[classId] ?: R.string.class_unknown
}

/**
 * Удобный экстеншен для [Classes.Info] — возвращает `@StringRes Int`
 * имени класса в текущей локали.
 */
@StringRes
fun Classes.Info.nameRes(): Int = ClassNames.resFor(id)
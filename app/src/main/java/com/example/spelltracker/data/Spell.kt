package com.example.spelltracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Заклинание D&D 5e — нормализованная запись (version 4).
 *
 * Поля один-к-одному соответствуют ключам в
 * `app/build/generated/assets/spells_normalized.json`, которое генерируется
 * build-time Gradle-таской `generateSpellsDb`. Один wide-table подход:
 * справочник всего ~800 строк, джойны не нужны.
 *
 * Multi-value поля (subclasses / races / classes / savingThrows) — CSV-строки.
 * Для поиска «содержит X» достаточно `csv.contains("X")`.
 *
 * Эволюция схемы:
 *   v3 (предыдущая) — 11 полей, данные из `assets/spells_normalized.json`
 *   (при старом pipeline'е: CSV + class_*.json, id = name.hashCode()).
 *   v4 (текущая) — 24 поля, данные из `assets/spells_normalized.json`,
 *     id = стабильный numeric из source-системы. Миграция destructive
 *     через `fallbackToDestructiveMigration` — справочник приходит из
 *     assets, потеря не страшна. См. Этап N в QWEN.md.
 */
@Entity(tableName = "spells")
data class Spell(
    @PrimaryKey val id: Long,
    val name: String,
    val nameEng: String,
    val source: String,
    val sourceGroup: String,
    val level: Int,
    val school: String,
    val ritual: Boolean,
    val concentration: Boolean,
    val timecast: String,
    val distance: String,
    val duration: String,
    val componentV: Boolean,
    val componentS: Boolean,
    val componentM: Boolean,
    val materialConsumed: Boolean,
    val materialDesc: String,
    val descriptionHtml: String,
    val upperLevel: String,
    val url: String,
    val classes: String,
    /** Имена подклассов, CSV. Парный список к [subclassParents]. */
    val subclasses: String,
    /**
     * English id класса-родителя для каждого подкласса в [subclasses], CSV.
     * Нужен runtime-фильтру «показать подклассы выбранных классов».
     * Пример: subclasses="Домен Войны,Клятва Мести", subclassParents="cleric,paladin".
     */
    val subclassParents: String,
    val races: String,
    val savingThrows: String,
)

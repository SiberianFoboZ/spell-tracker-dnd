package com.example.spelltracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Заклинание D&D 5e.
 *
 * Хранится в Room-таблице `spells`. Поле [classes] — запятая-разделённая
 * строка с id классов (например, "wizard,sorcerer") — нужно для фильтра
 * «по классу» в списке заклинаний.
 */
@Entity(tableName = "spells")
data class Spell(
    @PrimaryKey val id: Long,
    val name: String,
    val level: Int,                 // 0 = заговор, 1..9 = уровни заклинаний
    val school: String,
    val castingTime: String,
    val range: String,
    val components: String,         // "В, С, М (огрызок свечи)"
    val duration: String,
    val description: String,
    val higherLevel: String,        // пустая строка, если нет
    val classes: String,            // CSV id классов
)

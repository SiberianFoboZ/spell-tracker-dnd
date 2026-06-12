package com.example.spelltracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Room DAO для таблицы spells. Все методы синхронные — вызывающий
 * код отвечает за переключение на Dispatchers.IO (см. SpellRepository).
 */
@Dao
interface SpellDao {

    @Query("SELECT COUNT(*) FROM spells")
    fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(spells: List<Spell>)

    @Query("SELECT * FROM spells ORDER BY level, name")
    fun getAll(): List<Spell>

    @Query("SELECT * FROM spells WHERE id = :id LIMIT 1")
    fun getById(id: Long): Spell?

    @Query("SELECT * FROM spells WHERE level = :level ORDER BY name")
    fun getByLevel(level: Int): List<Spell>
}

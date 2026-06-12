package com.example.spelltracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room-база со справочником заклинаний. Версия 1, экспорт схемы выключен.
 *
 * База хранится в стандартном каталоге приложения и при первом запуске
 * заполняется из JSON-файлов в assets через SpellRepository.
 */
@Database(entities = [Spell::class], version = 1, exportSchema = false)
abstract class SpellDatabase : RoomDatabase() {

    abstract fun spellDao(): SpellDao

    companion object {
        @Volatile private var INSTANCE: SpellDatabase? = null

        fun get(context: Context): SpellDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                SpellDatabase::class.java,
                "spells.db"
            ).build().also { INSTANCE = it }
        }
    }
}

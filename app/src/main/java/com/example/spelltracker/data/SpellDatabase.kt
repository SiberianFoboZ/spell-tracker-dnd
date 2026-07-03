package com.example.spelltracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room-база со справочником заклинаний.
 *
 * Версия 5 — добавили [Spell.subclassParents] (CSV English id родительских
 * классов для каждого подкласса). Миграция destructive: данные приходят из
 * assets/spells_normalized.json (см. generateSpellsDb в app/build.gradle.kts).
 *
 * `fallbackToDestructiveMigration` уничтожит старую БД при изменении
 * схемы; данные перечитаются из assets при следующем старте. Потеря
 * не страшна — это справочник.
 */
@Database(entities = [Spell::class], version = 5, exportSchema = false)
abstract class SpellDatabase : RoomDatabase() {

    abstract fun spellDao(): SpellDao

    companion object {
        @Volatile private var INSTANCE: SpellDatabase? = null

        fun get(context: Context): SpellDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                SpellDatabase::class.java,
                "spells.db"
            )
                // Если на устройстве лежит spells.db от старой версии —
                // Room не сможет валидировать identity hash и упадёт.
                // Поскольку данные восстанавливаются из assets, безопаснее
                // дропнуть старую БД и пересоздать.
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
        }
    }
}

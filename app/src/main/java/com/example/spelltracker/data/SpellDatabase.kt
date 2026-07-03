package com.example.spelltracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room-база со справочником заклинаний.
 *
 * Версия 4 — расширенная запись заклинания (24 поля) под
 * `assets/spells_normalized.json`, который собирается build-time
 * Gradle-таской `generateSpellsDb` (см. app/build.gradle.kts).
 *
 * `fallbackToDestructiveMigration` уничтожит старую БД при изменении
 * схемы; данные перечитаются из assets при следующем старте. Потеря
 * не страшна — это справочник.
 */
@Database(entities = [Spell::class], version = 4, exportSchema = false)
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

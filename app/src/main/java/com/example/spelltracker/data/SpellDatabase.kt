package com.example.spelltracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room-база со справочником заклинаний.
 *
 * Версия 3 (поднята с 2, чтобы вынудить реимпорт из assets/spells.csv).
 * У пользователей, обновивших приложение с v2.0.0, в `spells.db` мог
 * остаться урезанный набор данных — без заклинаний 6-9 уровней
 * (БД залилась при первой установке, а CSV мог пополниться позже,
 * при этом `fallbackToDestructiveMigration` срабатывает ТОЛЬКО при
 * смене схемы, а не при смене данных). `fallbackToDestructiveMigration`
 * уничтожит старую БД, и при следующем старте заклинания перечитаются
 * из assets/spells.csv. Потеря данных не страшна — это справочник.
 */
// Этап N: schema v4 — расширенная запись заклинания.
// Старая v3 (11 полей) перестала подходить под новый
// `spells_normalized.json`. Migration destructive: справочник
// приходит из assets/spells_normalized.json (см. generateSpellsDb
// в app/build.gradle.kts), переустановка данных безопасна.
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
                // Поскольку данные восстанавливаются из assets/spells.csv,
                // безопаснее дропнуть старую БД и пересоздать.
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
        }
    }
}

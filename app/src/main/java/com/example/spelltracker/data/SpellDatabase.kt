package com.example.spelltracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room-база со справочником заклинаний.
 *
 * Версия 2 (поднята после перехода на KSP — старая БД, оставшаяся
 * на устройстве от v1.x, имеет другой identity hash, и Room отказывается
 * её открывать).
 *
 * На случай любых будущих несовместимостей схемы при апдейтах
 * используется [fallbackToDestructiveMigration] — при несовпадении
 * identity hash Room просто пересоздаст БД. Потеря данных не страшна,
 * потому что заклинания — это справочник, который репозиторий
 * перезаливает из assets/spells.csv при первом запуске
 * (см. SpellRepository.ensureInitialized).
 */
@Database(entities = [Spell::class], version = 2, exportSchema = false)
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

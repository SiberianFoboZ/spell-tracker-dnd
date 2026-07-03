package com.example.spelltracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room-база со справочником заклинаний.
 *
 * Версия 5 — добавили [Spell.subclassParents] (CSV English id родительских
 * классов для каждого подкласса). Данные приходят из
 * `assets/spells_normalized.json` (см. generateSpellsDb в app/build.gradle.kts).
 *
 * Миграции:
 *   - При плановых изменениях схемы добавляем новый `Migration(prev, next)`
 *     в [MIGRATIONS] и поднимаем `version`. Room выполнит его в транзакции
 *     при апгрейде с предыдущей версии.
 *   - `fallbackToDestructiveMigration()` оставлен как **safety-net**: если
 *     на устройстве лежит БД версии, для которой мы не написали миграцию,
 *     Room дропнет её и пересоздаст. Потеря не страшна — это справочник,
 *     данные приходят из assets. Пользовательские `prepared` id при этом
 *     чистятся отдельным проходом через [SpellRepository.pruneOrphanedPrepared].
 */
@Database(entities = [Spell::class], version = 5, exportSchema = false)
abstract class SpellDatabase : RoomDatabase() {

    abstract fun spellDao(): SpellDao

    companion object {
        @Volatile private var INSTANCE: SpellDatabase? = null

        /**
         * Зарегистрированные миграции.
         *
         * Сейчас список пуст (текущая версия — 5, от 5 идти некуда).
         * При первом изменении схемы добавляем сюда:
         *   - MIGRATION_5_6 — alter table, добавить колонку, и т.п.
         *   - поднять `version` до 6
         *   - опционально удалить `.fallbackToDestructiveMigration()`,
         *     если уверены, что все пути покрыты миграциями
         *
         * Каждая миграция — обычная `Migration(prev, next)` с `migrate(db)`,
         * в котором пишем SQL напрямую. Пример:
         * ```
         * val MIGRATION_5_6 = object : Migration(5, 6) {
         *     override fun migrate(db: SupportSQLiteDatabase) {
         *         db.execSQL("ALTER TABLE spells ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
         *     }
         * }
         * ```
         */
        private val MIGRATIONS: Array<Migration> = arrayOf(
            // MIGRATION_5_6, // ← раскомментировать при первом изменении схемы
        )

        fun get(context: Context): SpellDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                SpellDatabase::class.java,
                "spells.db",
            )
                .addMigrations(*MIGRATIONS)
                // Safety-net: если на устройстве версия, для которой нет
                // миграции (например, debug-сборка с экспериментальной
                // схемой), Room дропнет БД и пересоздаст. Данные
                // восстановятся из assets при следующем старте.
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
        }
    }
}
package com.example.spelltracker;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * DAO для таблицы {@code spell}.
 *
 * <p>Все методы выполняются в вызывающем потоке — оборачивайте вызовы в
 * фоновый {@link java.util.concurrent.Executor} или асинхронную задачу.
 * В проекте этим занимается {@link SpellRepository}.</p>
 */
@Dao
public interface SpellDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(Spell spell);

    @Query("SELECT * FROM spell ORDER BY level ASC, name COLLATE NOCASE ASC")
    List<Spell> getAll();

    @Query("SELECT * FROM spell WHERE level = :level ORDER BY name COLLATE NOCASE ASC")
    List<Spell> getByLevel(int level);

    @Query("SELECT * FROM spell WHERE id = :id LIMIT 1")
    Spell getById(long id);

    @Query("SELECT COUNT(*) FROM spell")
    int count();
}

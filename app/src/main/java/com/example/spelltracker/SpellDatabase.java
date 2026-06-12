package com.example.spelltracker;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * Singleton-обёртка над Room-базой {@code spells.db}.
 * Версия схемы: 1.
 */
@Database(entities = {Spell.class}, version = 1, exportSchema = false)
public abstract class SpellDatabase extends RoomDatabase {

    private static final String DB_NAME = "spells.db";
    private static volatile SpellDatabase INSTANCE;

    public abstract SpellDao spellDao();

    public static SpellDatabase get(Context context) {
        if (INSTANCE == null) {
            synchronized (SpellDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    SpellDatabase.class,
                                    DB_NAME)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}

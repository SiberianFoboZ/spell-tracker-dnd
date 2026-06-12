package com.example.spelltracker;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Модель заклинания D&D.
 *
 * <p>Является сущностью Room (таблица {@code spell}). При первом запуске
 * приложения данные импортируются из CSV в локальную БД, после чего
 * заклинания читаются исключительно из БД — это позволяет избежать
 * {@code TransactionTooLargeException} при передаче большого списка
 * заклинаний (1+ МБ) в аргументах фрагмента через {@link android.os.Parcel}.</p>
 */
@Entity(
        tableName = "spell",
        indices = {
                @Index("level"),
                @Index(value = "name")
        }
)
public class Spell {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    @ColumnInfo(name = "name")
    public String name = "";

    @ColumnInfo(name = "school")
    public String school;

    @ColumnInfo(name = "level")
    public int level;

    @ColumnInfo(name = "casting_time")
    public String castingTime;

    @ColumnInfo(name = "range_text")
    public String range;

    @ColumnInfo(name = "components")
    public String components;

    @ColumnInfo(name = "duration")
    public String duration;

    @ColumnInfo(name = "description")
    public String description;

    @ColumnInfo(name = "higher_level")
    public String higherLevel;

    /** Пустой конструктор для Room. */
    public Spell() {
    }

    public Spell(@NonNull String name, String school, int level,
                 String castingTime, String range, String components,
                 String duration, String description, String higherLevel) {
        this.name = name;
        this.school = school;
        this.level = level;
        this.castingTime = castingTime;
        this.range = range;
        this.components = components;
        this.duration = duration;
        this.description = description;
        this.higherLevel = higherLevel;
    }

    public String levelLabel() {
        return level == 0 ? "Заговор" : level + " ур.";
    }
}

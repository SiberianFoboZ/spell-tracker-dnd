package com.example.spelltracker;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Парсер CSV-файла со списком заклинаний.
 * Ожидаемый формат (разделитель — ';', каждое поле в кавычках):
 *   0  Имя
 *   1  "N уровень, школа"
 *   2  "Время накладывания"
 *   3  Значение (например, "1 действие")
 *   4  "Дистанция"
 *   5  Значение
 *   6  "Компоненты"
 *   7  Значение (например, "В, С, М")
 *   8  "Длительность"
 *   9  Значение
 *   10 ""
 *   11 Описание (с переносами \f)
 *   12 "На более высоком уровне"
 *   13 Текст
 *   14 ""
 *   15 Уровень (число 0..9)
 *   16, 17 "" (могут отсутствовать)
 */
public final class SpellParser {

    private static final String ASSET_FILE = "spells.csv";

    private SpellParser() {}

    /** Загружает все заклинания из assets/spells.csv. */
    public static List<Spell> loadFromAssets(Context context) throws IOException {
        List<String> lines = readAllLines(context);
        List<Spell> result = new ArrayList<>(lines.size());
        for (String line : lines) {
            if (line == null || line.isEmpty()) continue;
            Spell s = parseLine(line);
            if (s != null) result.add(s);
        }
        return result;
    }

    /** Читает CSV-файл, корректно склеивая многострочные записи. */
    private static List<String> readAllLines(Context context) throws IOException {
        List<String> rawLines = new ArrayList<>();
        InputStream is = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        try {
            is = context.getAssets().open(ASSET_FILE);
            isr = new InputStreamReader(is, StandardCharsets.UTF_8);
            br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                rawLines.add(line);
            }
        } finally {
            if (br != null) try { br.close(); } catch (IOException ignored) {}
            else if (isr != null) try { isr.close(); } catch (IOException ignored) {}
            else if (is != null) try { is.close(); } catch (IOException ignored) {}
        }
        return joinMultilineRecords(rawLines);
    }

    /**
     * Склеивает перенесённые строки внутри одной CSV-записи.
     * Запись начинается с символа " в начале строки. Если количество кавычек в строке нечётное,
     * значит запись продолжается на следующей строке.
     */
    private static List<String> joinMultilineRecords(List<String> rawLines) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String raw : rawLines) {
            if (current.length() == 0) {
                current.append(raw);
            } else {
                current.append('\n').append(raw);
            }
            if (countUnescapedQuotes(current.toString()) % 2 == 0) {
                result.add(current.toString());
                current.setLength(0);
            }
        }
        if (current.length() > 0) result.add(current.toString());
        return result;
    }

    private static int countUnescapedQuotes(String s) {
        int count = 0;
        boolean inQuotes = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < s.length() && s.charAt(i + 1) == '"') {
                    i++; // экранированная кавычка ""
                } else {
                    inQuotes = !inQuotes;
                    count++;
                }
            }
        }
        return count;
    }

    /** Разбивает строку CSV на поля (с поддержкой кавычек и ""). */
    static List<String> splitCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cur.append(c);
                }
            } else {
                if (c == ';') {
                    fields.add(cur.toString());
                    cur.setLength(0);
                } else if (c == '"') {
                    inQuotes = true;
                } else {
                    cur.append(c);
                }
            }
        }
        fields.add(cur.toString());
        return fields;
    }

    /** Превращает одну CSV-запись в {@link Spell} или null, если запись пустая/повреждённая. */
    static Spell parseLine(String line) {
        List<String> f = splitCsvLine(line);
        // Нам нужно как минимум 16 колонок (последняя числовая — 15).
        if (f.size() < 16) return null;
        String name = f.get(0).trim();
        if (name.isEmpty()) return null;
        String schoolAndLevel = f.get(1).trim();
        int level = parseLevel(schoolAndLevel, f);
        String school = parseSchool(schoolAndLevel);
        String castingTime = safeGet(f, 3);
        String range = safeGet(f, 5);
        String components = safeGet(f, 7);
        String duration = safeGet(f, 9);
        String description = safeGet(f, 11).replace('\f', '\n').trim();
        String higherLevel = safeGet(f, 13).replace('\f', '\n').trim();
        return new Spell(name, school, level, castingTime, range, components,
                duration, description, higherLevel);
    }

    private static String safeGet(List<String> f, int idx) {
        if (idx >= f.size()) return "";
        return f.get(idx);
    }

    /** Уровень берём из колонки 15, иначе из колонки 1. */
    private static int parseLevel(String schoolAndLevel, List<String> f) {
        try {
            return Integer.parseInt(f.get(15).trim());
        } catch (Exception ignored) { }
        // Фолбэк: ищем "N уровень" в schoolAndLevel
        if (schoolAndLevel != null) {
            int sp = schoolAndLevel.indexOf(' ');
            if (sp > 0) {
                try { return Integer.parseInt(schoolAndLevel.substring(0, sp).trim()); }
                catch (Exception ignored) { }
            }
        }
        return 0;
    }

    /** Школа — часть после запятой, например "3 уровень, воплощение" → "воплощение". */
    private static String parseSchool(String schoolAndLevel) {
        if (schoolAndLevel == null) return "";
        int comma = schoolAndLevel.indexOf(',');
        if (comma < 0) return schoolAndLevel.trim();
        return schoolAndLevel.substring(comma + 1).trim();
    }

    /** Группирует заклинания по уровням (0..9). */
    public static List<List<Spell>> groupByLevel(List<Spell> spells) {
        List<List<Spell>> groups = new ArrayList<>(10);
        for (int i = 0; i < 10; i++) groups.add(new ArrayList<>());
        for (Spell s : spells) {
            int idx = Math.max(0, Math.min(9, s.level));
            groups.get(idx).add(s);
        }
        for (List<Spell> g : groups) Collections.sort(g, (a, b) -> a.name.compareToIgnoreCase(b.name));
        return groups;
    }
}

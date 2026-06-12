package com.example.spelltracker.data

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/**
 * Парсер справочника заклинаний из одного CSV-файла `spells.csv`
 * в assets. Формат файла — портирован из старой Java-версии
 * (`SpellParser.java` из коммита `ce25d90`):
 *
 *   разделитель полей — `;`
 *   каждое поле может быть в кавычках `"..."` (поддерживается `""` как экранирование)
 *   многострочные записи склеиваются по правилу «нечётное число кавычек = запись продолжается»
 *
 * Колонки:
 *   0  Имя
 *   1  "N уровень, школа"
 *   3  Время накладывания
 *   5  Дистанция
 *   7  Компоненты
 *   9  Длительность
 *   11 Описание (\f внутри записи → перенос строки)
 *   13 «На более высоком уровне»
 *   15 Уровень (число 0..9)
 */
object SpellParser {

    private const val ASSET_FILE = "spells.csv"

    /**
     * Загрузить все заклинания из assets/spells.csv.
     * Все заклинания помечаются как принадлежащие ко всем классам
     * (через `classes` = список id через запятую), чтобы фильтр
     * «по классу» в UI работал как «показать всё».
     */
    fun loadFromAssets(context: Context): List<Spell> {
        val raw = readAllLines(context)
        val spells = ArrayList<Spell>(raw.size)
        for (line in raw) {
            if (line.isBlank()) continue
            val s = parseLine(line) ?: continue
            spells.add(s)
        }
        return spells
    }

    // ─────────── Чтение файла ───────────

    private fun readAllLines(context: Context): List<String> {
        val raw = ArrayList<String>()
        BufferedReader(
            InputStreamReader(context.assets.open(ASSET_FILE), StandardCharsets.UTF_8)
        ).use { br ->
            while (true) {
                val line = br.readLine() ?: break
                raw.add(line)
            }
        }
        return joinMultilineRecords(raw)
    }

    /**
     * Склеивает перенесённые строки внутри одной CSV-записи.
     * Запись начинается с символа `"` в начале строки. Если количество
     * неэкранированных кавычек в накопленном буфере нечётное — запись
     * продолжается на следующей строке.
     */
    private fun joinMultilineRecords(rawLines: List<String>): List<String> {
        val result = ArrayList<String>()
        val current = StringBuilder()
        for (raw in rawLines) {
            if (current.isEmpty()) {
                current.append(raw)
            } else {
                current.append('\n').append(raw)
            }
            if (countUnescapedQuotes(current.toString()) % 2 == 0) {
                result.add(current.toString())
                current.setLength(0)
            }
        }
        if (current.isNotEmpty()) result.add(current.toString())
        return result
    }

    private fun countUnescapedQuotes(s: String): Int {
        var count = 0
        var inQuotes = false
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '"') {
                if (inQuotes && i + 1 < s.length && s[i + 1] == '"') {
                    i++ // экранированная кавычка ""
                } else {
                    inQuotes = !inQuotes
                    count++
                }
            }
            i++
        }
        return count
    }

    // ─────────── Разбор одной записи ───────────

    internal fun splitCsvLine(line: String): List<String> {
        val fields = ArrayList<String>()
        val cur = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length && line[i + 1] == '"') {
                        cur.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    cur.append(c)
                }
            } else {
                when (c) {
                    ';' -> {
                        fields.add(cur.toString())
                        cur.setLength(0)
                    }
                    '"' -> inQuotes = true
                    else -> cur.append(c)
                }
            }
            i++
        }
        fields.add(cur.toString())
        return fields
    }

    private fun parseLine(line: String): Spell? {
        val f = splitCsvLine(line)
        if (f.size < 16) return null
        val name = f[0].trim()
        if (name.isEmpty()) return null

        val schoolAndLevel = f[1].trim()
        val level = parseLevel(schoolAndLevel, f)
        val school = parseSchool(schoolAndLevel)

        // id — стабильный хэш имени, чтобы Room не падал на дубликатах
        // и фильтр «избранное» работал между запусками.
        val id = name.hashCode().toLong() and 0x7FFFFFFF

        // classes — все 9 классов, чтобы фильтр «по классу» показывал
        // все заклинания при любом выборе (справочник общий).
        val classes = Classes.ALL.joinToString(",") { it.id }

        return Spell(
            id = id,
            name = name,
            level = level,
            school = school,
            castingTime = safeGet(f, 3),
            range = safeGet(f, 5),
            components = safeGet(f, 7),
            duration = safeGet(f, 9),
            // \f (form feed, 0x0C) внутри CSV-записи означает перенос строки
            // — портировано из старой Java-версии. В Kotlin нет литерала \f,
            // используем \u000C.
            description = safeGet(f, 11).replace('\u000C', '\n').trim(),
            higherLevel = safeGet(f, 13).replace('\u000C', '\n').trim(),
            classes = classes,
        )
    }

    private fun safeGet(f: List<String>, idx: Int): String =
        if (idx >= f.size) "" else f[idx]

    private fun parseLevel(schoolAndLevel: String, f: List<String>): Int {
        // Сначала пробуем явную колонку 15
        if (f.size > 15) {
            f[15].trim().toIntOrNull()?.let { return it.coerceIn(0, 9) }
        }
        // Фолбэк: «N уровень» в первой колонке
        val sp = schoolAndLevel.indexOf(' ')
        if (sp > 0) {
            schoolAndLevel.substring(0, sp).trim().toIntOrNull()?.let {
                return it.coerceIn(0, 9)
            }
        }
        return 0
    }

    private fun parseSchool(schoolAndLevel: String): String {
        val comma = schoolAndLevel.indexOf(',')
        return if (comma < 0) schoolAndLevel.trim()
        else schoolAndLevel.substring(comma + 1).trim()
    }
}

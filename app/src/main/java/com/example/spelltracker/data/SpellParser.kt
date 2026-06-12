package com.example.spelltracker.data

import android.content.Context
import org.json.JSONArray
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
 *
 * Помимо CSV, при инициализации подтягиваются per-class JSON-файлы
 * `class_<id>.json` (bard, cleric, druid, paladin, ranger, sorcerer,
 * warlock, wizard, artificer) — списки заклинаний каждого класса.
 * Используются для правильного заполнения поля [Spell.classes].
 *
 * Если заклинание не нашлось ни в одном class_*.json — пишем ему
 * пустую строку классов, а НЕ «все классы»: иначе 258 из 524
 * заклинаний (расхождения в переводах между CSV и JSON — например,
 * «аура живучести» в CSV vs «аура жизнестойкости» в JSON, или
 * «антипатия/симпатия» одной записью) уходили бы в «все классы»,
 * и фильтр `it.classes.contains(id)` для ЛЮБОГО выбранного класса
 * давал бы `true` — заклинание «протекало» в чужие классы.
 * С пустой строкой оно не попадает в фильтр по конкретному классу,
 * но остаётся в «Все классы» через short-circuit `isEmpty()`.
 */
object SpellParser {

    private const val ASSET_FILE = "spells.csv"
    private const val CLASS_FILE_PREFIX = "class_"
    private const val CLASS_FILE_SUFFIX = ".json"

    /** Все id классов, для которых есть per-class JSON. */
    private val CLASS_IDS = listOf(
        "bard", "cleric", "druid", "paladin", "ranger",
        "sorcerer", "warlock", "wizard", "artificer",
    )

    /**
     * Загрузить все заклинания из assets/spells.csv и прикрепить к каждому
     * список классов из per-class JSON. Если заклинание не нашлось ни в
     * одном class_*.json — пишем пустую строку классов (см. KDoc файла,
     * почему «все классы» ломает фильтр).
     */
    fun loadFromAssets(context: Context): List<Spell> {
        val classMap = loadClassMap(context)
        val raw = readAllLines(context)
        val spells = ArrayList<Spell>(raw.size)
        for (line in raw) {
            if (line.isBlank()) continue
            val s = parseLine(line, classMap) ?: continue
            spells.add(s)
        }
        return spells
    }

    /**
     * Считывает все class_<id>.json и строит словарь spell_name (lower) →
     * список class_id. Если файлы не нашлись — возвращает пустую карту
     * (тогда все заклинания попадут в «все классы»).
     */
    private fun loadClassMap(context: Context): Map<String, List<String>> {
        val result = HashMap<String, MutableList<String>>()
        for (id in CLASS_IDS) {
            val path = "$CLASS_FILE_PREFIX$id$CLASS_FILE_SUFFIX"
            val names = try {
                readJsonStringArray(context, path)
            } catch (e: Exception) {
                continue  // файла нет — пропускаем этот класс
            }
            for (name in names) {
                val key = name.trim().lowercase()
                if (key.isEmpty()) continue
                result.getOrPut(key) { ArrayList() }.add(id)
            }
        }
        return result
    }

    private fun readJsonStringArray(context: Context, path: String): List<String> {
        BufferedReader(
            InputStreamReader(context.assets.open(path), StandardCharsets.UTF_8)
        ).use { br ->
            val text = br.readText()
            val arr = JSONArray(text)
            return (0 until arr.length()).map { arr.getString(it) }
        }
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

    private fun parseLine(line: String, classMap: Map<String, List<String>>): Spell? {
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

        // classes — ищем заклинание в per-class JSON-ах (по имени в
        // нижнем регистре). Если нашлось — пишем только те классы, к
        // которым оно реально относится. Если не нашлось — пишем
        // пустую строку: фильтр по конкретному классу такое заклинание
        // не захватит, а в «Все классы» оно попадёт через short-circuit
        // `selectedClassIds.isEmpty()` в SpellsViewModel.applyFilters.
        // (Раньше здесь стоял `Classes.ALL.joinToString(",")`, но
        // тогда 258 нераспознанных заклинаний «протекали» в ЛЮБОЙ
        // класс — `it.classes.contains("wizard")` для «all classes»
        // всегда true.)
        val classIds = classMap[name.lowercase()]
        val classes = classIds?.joinToString(",") ?: ""

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

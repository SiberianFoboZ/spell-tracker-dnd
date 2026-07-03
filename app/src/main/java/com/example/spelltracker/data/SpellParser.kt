package com.example.spelltracker.data

import android.content.Context
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/**
 * Парсер справочника заклинаний из ОДНОГО bundled-файла
 * `spells_normalized.json` в assets.
 *
 * Файл — артефакт build-time Gradle-таски `generateSpellsDb`
 * (см. app/build.gradle.kts): читает 843 per-spell JSON из
 * `spells_data/`, нормализует поля и склеивает в один массив.
 *
 * Формат файла — JSON-массив объектов, поля 1:1 совпадают с [Spell].
 * Никакой class-mapping, никаких regex, никаких дериваций — всё уже
 * сделано на build-time.
 *
 * Скорость: 843 XML-парсинга с device заменены на 1 JSON.parse() —
 * пользователь не видит «loading» после первой установки.
 */
object SpellParser {

    private const val ASSET_FILE = "spells_normalized.json"

    /**
     * Загрузить все заклинания из assets/spells_normalized.json.
     *
     * @return список заклинаний в порядке source-файлов (alpha по имени файла).
     *         Может быть пустым, если assets не содержит файл (например,
     *         не запускался `generateSpellsDb`).
     */
    fun loadFromAssets(context: Context): List<Spell> {
        val raw = readAll(context)
        if (raw.isBlank()) return emptyList()
        val arr = JSONArray(raw)
        val out = ArrayList<Spell>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(
                Spell(
                    id = o.optLong("id"),
                    name = o.optString("name"),
                    nameEng = o.optString("nameEng", o.optString("name")),
                    source = o.optString("source"),
                    sourceGroup = o.optString("sourceGroup"),
                    level = o.optInt("level", 0).coerceIn(0, 9),
                    school = o.optString("school"),
                    ritual = o.optBoolean("ritual", false),
                    concentration = o.optBoolean("concentration", false),
                    timecast = o.optString("timecast"),
                    distance = o.optString("distance"),
                    duration = o.optString("duration"),
                    componentV = o.optBoolean("componentV", false),
                    componentS = o.optBoolean("componentS", false),
                    componentM = o.optBoolean("componentM", false),
                    materialConsumed = o.optBoolean("materialConsumed", false),
                    materialDesc = o.optString("materialDesc"),
                    descriptionHtml = o.optString("descriptionHtml"),
                    upperLevel = o.optString("upperLevel"),
                    url = o.optString("url"),
                    classes = o.optString("classes"),
                    subclasses = o.optString("subclasses"),
                    races = o.optString("races"),
                    savingThrows = o.optString("savingThrows"),
                )
            )
        }
        return out
    }

    private fun readAll(context: Context): String =
        BufferedReader(
            InputStreamReader(context.assets.open(ASSET_FILE), StandardCharsets.UTF_8)
        ).use { it.readText() }
}

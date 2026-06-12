package com.example.spelltracker.data

import android.content.Context
import org.json.JSONObject

/**
 * Парсер JSON-файлов из assets/spells/.
 *
 * Каждый файл соответствует одному классу (см. [Classes.Info.assetFile])
 * и имеет формат:
 * ```
 * {
 *   "class": "wizard",
 *   "spells": [
 *     {
 *       "id": 101,
 *       "name": "Огненный снаряд",
 *       "level": 3,
 *       "school": "Воплощение",
 *       "casting_time": "1 действие",
 *       "range": "150 футов",
 *       "components": "В, С, М (крошечный шарик из гуано и сера)",
 *       "duration": "Мгновенная",
 *       "description": "...",
 *       "higher_level": "..."
 *     },
 *     ...
 *   ]
 * }
 * ```
 */
object SpellParser {

    fun loadFromAsset(context: Context, classId: String, assetFile: String): List<Spell> {
        val raw = context.assets.open("spells/$assetFile")
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }

        val root = JSONObject(raw)
        val arr = root.getJSONArray("spells")
        val result = ArrayList<Spell>(arr.length())

        for (i in 0 until arr.length()) {
            val s = arr.getJSONObject(i)
            result.add(
                Spell(
                    id = s.getLong("id"),
                    name = s.getString("name"),
                    level = s.getInt("level"),
                    school = s.optString("school", ""),
                    castingTime = s.optString("casting_time", ""),
                    range = s.optString("range", ""),
                    components = s.optString("components", ""),
                    duration = s.optString("duration", ""),
                    description = s.optString("description", ""),
                    higherLevel = s.optString("higher_level", ""),
                    classes = classId,
                )
            )
        }
        return result
    }
}

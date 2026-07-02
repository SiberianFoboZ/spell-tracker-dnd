package com.example.spelltracker.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Полное состояние одного персонажа (Этап 22 — мульти-персонажи).
 *
 * Включает все данные, которые до Этапа 22 жили «плоско» в
 * [SpellStorage]: уровни классов, использованные ячейки (обычные /
 * пакт / арканумы), пользовательские ячейки и подготовленные
 * заклинания.
 *
 * Хранится как JSON-блоб под ключом `char_data_${id}` в
 * SharedPreferences. При переключении активного персонажа текущий
 * снимок сериализуется и сохраняется, новый — десериализуется и
 * заливается в StateFlow'ы [SpellStorage].
 */
data class CharacterData(
    val classLevels: Map<String, Int> = emptyMap(),
    val usedSlots: Map<Int, Int> = emptyMap(),
    val usedPactSlots: Int = 0,
    val usedArcanums: Map<Int, Boolean> = emptyMap(),
    val customSlots: List<CustomSlot> = emptyList(),
    val prepared: Set<Long> = emptySet(),
)

/**
 * Метаданные персонажа (Этап 22).
 *
 * Только id и имя; сами данные ([CharacterData]) живут отдельным
 * блобом под `char_data_${id}`, чтобы при переключении не гонять
 * весь список ради одного активного.
 */
data class Character(
    val id: Long,
    val name: String,
)

// ─────────── Сериализация CharacterData ───────────

internal fun characterDataToJson(data: CharacterData): String {
    val obj = JSONObject()
    obj.put("classLevels", JSONObject().apply {
        data.classLevels.forEach { (k, v) -> put(k, v) }
    })
    obj.put("usedSlots", JSONObject().apply {
        data.usedSlots.forEach { (k, v) -> put(k.toString(), v) }
    })
    obj.put("usedPactSlots", data.usedPactSlots)
    obj.put("usedArcanums", JSONObject().apply {
        data.usedArcanums.forEach { (k, v) -> put(k.toString(), v) }
    })
    obj.put("customSlots", customSlotsToJson(data.customSlots))
    obj.put("prepared", JSONArray().apply {
        data.prepared.forEach { put(it) }
    })
    return obj.toString()
}

internal fun characterDataFromJson(json: String?): CharacterData {
    if (json.isNullOrEmpty()) return CharacterData()
    return try {
        val obj = JSONObject(json)
        val classLevels = mutableMapOf<String, Int>()
        obj.optJSONObject("classLevels")?.let { cls ->
            cls.keys().forEach { k -> classLevels[k] = cls.optInt(k) }
        }
        val usedSlots = mutableMapOf<Int, Int>()
        obj.optJSONObject("usedSlots")?.let { us ->
            us.keys().forEach { k ->
                k.toIntOrNull()?.let { lvl -> usedSlots[lvl] = us.optInt(k) }
            }
        }
        val usedArcanums = mutableMapOf<Int, Boolean>()
        obj.optJSONObject("usedArcanums")?.let { ar ->
            ar.keys().forEach { k ->
                k.toIntOrNull()?.let { lvl -> usedArcanums[lvl] = ar.optBoolean(k) }
            }
        }
        val prepared = mutableSetOf<Long>()
        obj.optJSONArray("prepared")?.let { arr ->
            for (i in 0 until arr.length()) {
                prepared.add(arr.optLong(i))
            }
        }
        CharacterData(
            classLevels = classLevels,
            usedSlots = usedSlots,
            usedPactSlots = obj.optInt("usedPactSlots", 0),
            usedArcanums = usedArcanums,
            customSlots = customSlotsFromJson(obj.optString("customSlots", "")),
            prepared = prepared,
        )
    } catch (e: Exception) {
        CharacterData()
    }
}

// ─────────── Сериализация списка персонажей ───────────

internal fun charactersListToJson(chars: List<Character>): String {
    val arr = JSONArray()
    for (c in chars) {
        val obj = JSONObject()
        obj.put("id", c.id)
        obj.put("name", c.name)
        arr.put(obj)
    }
    return arr.toString()
}

internal fun charactersListFromJson(json: String?): List<Character> {
    if (json.isNullOrEmpty()) return emptyList()
    return try {
        val arr = JSONArray(json)
        val result = mutableListOf<Character>()
        for (i in 0 until arr.length()) {
            try {
                val obj = arr.getJSONObject(i)
                result.add(
                    Character(
                        id = obj.getLong("id"),
                        name = obj.getString("name"),
                    )
                )
            } catch (e: Exception) {
                // битая запись — пропускаем
            }
        }
        result
    } catch (e: Exception) {
        emptyList()
    }
}
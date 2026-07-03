package com.example.spelltracker.data

import androidx.annotation.StringRes
import com.example.spelltracker.R
import org.json.JSONArray
import org.json.JSONObject

/**
 * Тип кубика для пользовательской ячейки (Этап 20).
 *
 * В UI отображается текстом (`label`) в бейдже ячейки **вместо**
 * римского номера уровня заклинания — отсюда и просьба пользователя
 * «тип кубика отображается текстом в замен уровня ячейки». Сам кубик
 * нигде в коде не «бросается» — это просто метка в духе D&D 5e
 * (4d6, 2d8 и т.п.), которая указывает на «объём» использования.
 *
 * Этап 21: добавлен [STAR] — «ультимативная способность без кубика».
 * Используется для финальных/уникальных способностей, у которых
 * нет объёма (бросок кубика не важен), только факт «использовано /
 * доступно». В бейдже рендерится звездочкой `★`, чип в форме
 * подхватывается автоматически через `DieType.entries`.
 *
 * `label` — универсальная математическая нотация (d4, d6, ★), не
 * локализуется.
 */
enum class DieType(val label: String) {
    D4("d4"),
    D6("d6"),
    D8("d8"),
    D10("d10"),
    D12("d12"),
    STAR("★"),
}

/**
 * Тип восстановления пользовательской ячейки (Этап 20).
 *  - [SHORT]: восстанавливается на **коротком** отдыхе
 *    (по аналогии с пакт-магией Колдуна)
 *  - [LONG]:  восстанавливается только на **длинном** отдыхе
 *    (по аналогии с арканумами)
 */
enum class RestType(@field:StringRes val displayNameRes: Int) {
    SHORT(R.string.rest_type_SHORT),
    LONG(R.string.rest_type_LONG),
}

/**
 * Пользовательская ячейка (Этап 20) — кастомный ресурс с произвольным
 * названием, количеством использований (1..20), типом кубика (d4..d12)
 * и типом восстановления. Примеры: «Дыхание дракона» 3d6 (long rest),
 * «Ярость» 4d4 (long rest), «Скрытая атака» 1d6 (short rest).
 *
 * Хранится в SharedPreferences как JSON-массив (ключ
 * `custom_slots_json`, см. [SpellStorage]).
 *
 * Поле [id] стабильно между запусками и используется для:
 *   - поиска конкретной ячейки в [SpellStorage.customSlots]
 *   - аргумента навигации на экран редактирования (`customslot/{id}`)
 *   - сравнения в UI (нужен `==`, поэтому [Long], а не autoValue)
 */
data class CustomSlot(
    val id: Long,
    val title: String,
    val total: Int,         // 1..20
    val used: Int,          // 0..total
    val die: DieType,
    val restType: RestType,
) {
    val isAllSpent: Boolean get() = used >= total
    val isEmpty: Boolean get() = used == 0
}

/** Сериализация списка в JSON-строку для SharedPreferences. */
internal fun customSlotsToJson(slots: List<CustomSlot>): String {
    val arr = JSONArray()
    for (slot in slots) {
        val obj = JSONObject()
        obj.put("id", slot.id)
        obj.put("title", slot.title)
        obj.put("total", slot.total)
        obj.put("used", slot.used)
        obj.put("die", slot.die.name)
        obj.put("restType", slot.restType.name)
        arr.put(obj)
    }
    return arr.toString()
}

/**
 * Десериализация из JSON-строки. Устойчива к повреждённым записям:
 * на одной «битой» записи останавливаемся (`mapNotNull`), но не валим
 * весь список. Также жёстко клампим `total` к 1..20 и `used` к 0..total —
 * на случай если вручную правили prefs, или мигрировали со старой версии.
 */
internal fun customSlotsFromJson(json: String?): List<CustomSlot> {
    if (json.isNullOrEmpty()) return emptyList()
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).mapNotNull { i ->
            try {
                val obj = arr.getJSONObject(i)
                val total = obj.getInt("total").coerceIn(1, 20)
                CustomSlot(
                    id = obj.getLong("id"),
                    title = obj.getString("title"),
                    total = total,
                    used = obj.getInt("used").coerceIn(0, total),
                    die = DieType.valueOf(obj.getString("die")),
                    restType = RestType.valueOf(obj.getString("restType")),
                )
            } catch (e: Exception) {
                null
            }
        }
    } catch (e: Exception) {
        emptyList()
    }
}

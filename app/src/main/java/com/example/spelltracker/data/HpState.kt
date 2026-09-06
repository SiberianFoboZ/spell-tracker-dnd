package com.example.spelltracker.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Тип кубика для Hit Dice (PHB).
 *
 * Универсальная математическая нотация (d6, d8, d10, d12) — совпадает
 * с [DieType] (для CustomSlot) без варианта STAR (для Hit Dice не нужна
 * «ультимативная способность без кубика»). Отдельный enum, чтобы:
 *   1. UI Hit Dice не показывал «★» в списке выбора
 *   2. `die.maxValue` / `die.avgValue` — численные поля, привязанные
 *      к размеру кубика (1..12), а не к лейблу
 *
 * Поля:
 *   - [maxValue] — верхняя граница (6/8/10/12)
 *   - [avgValue] — округлённое среднее по PHB (4/5/6/7); используется
 *     при восстановлении HP за кубик без явного броска
 */
enum class HitDie(val maxValue: Int, val avgValue: Int) {
    D6(6, 4),
    D8(8, 5),
    D10(10, 6),
    D12(12, 7),
}

/**
 * Hit Dice — пул «костей здоровья» по PHB.
 *
 * Конструкция [PHB p.196]: «у вас есть количество Hit Dice, равное
 * общему уровню персонажа». На коротком отдыхе персонаж может потратить
 * один или несколько кубиков; каждый кубик даёт `max(1, die + CON mod)`
 * восстановления HP. На длинном — spent уменьшается на `ceil(total / 2)`
 * (т.е. восстанавливается как минимум половина).
 *
 * Хранение в [CharacterData] как отдельный JSON-объект `hitDice`:
 *   { "total": Int, "spent": Int, "die": "D8", "conMod": Int }
 *
 * Не привязан к кастерам (см. запрос пользователя: Hit Dice могут
 * пригодиться немаг.классам тоже).
 */
data class HitDiceState(
    val total: Int = 0,
    val spent: Int = 0,
    val die: HitDie = HitDie.D8,
    val conMod: Int = 0,
) {
    /** Доступно кубиков для траты = total - spent. */
    val available: Int get() = (total - spent).coerceAtLeast(0)

    /**
     * Сколько кубиков было бы восстановлено длинным отдыхом по строгому
     * PHB (`ceil(total / 2)`). Сама логика [SpellStorage.longRest]
     * сейчас использует `spent = 0` (по выбору пользователя), но это
     * свойство оставлено для будущих вариантов и отображения в UI.
     */
    val longRestRecover: Int get() = (total + 1) / 2
}

/**
 * Полное состояние HP/Hit Dice одного персонажа.
 *
 * Хранится в [CharacterData] отдельным JSON-объектом `hp`:
 *   { "maxHp": Int, "currentHp": Int, "tempHp": Int }
 *
 * Миграция не нужна: у всех новых персонажей [maxHp] = 0, у
 * существующих (уже созданных до Этапа HP) — тоже 0, пользователь
 * вручную задаст max при первом открытии экрана. Это безопаснее,
 * чем фантазировать о starting HP за каждый класс.
 */
data class HpState(
    val maxHp: Int = 0,
    val currentHp: Int = 0,
    val tempHp: Int = 0,
) {
    /** Эффективные HP с учётом temp (temp поглощает урон первым). */
    val effectiveHp: Int get() = currentHp + tempHp
}

/**
 * Сериализация [HpState] + [HitDiceState] в JSON-объект, который
 * пишется в [CharacterData] под ключом `hp`.
 *
 * Структура:
 * {
 *   "maxHp":     Int,
 *   "currentHp": Int,
 *   "tempHp":    Int,
 *   "hitDice": {
 *     "total":  Int,
 *     "spent":  Int,
 *     "die":    "D6|D8|D10|D12",
 *     "conMod": Int
 *   }
 * }
 */
internal fun hpStateToJson(state: HpAndHitDice): JSONObject = JSONObject().apply {
    put("maxHp", state.hp.maxHp)
    put("currentHp", state.hp.currentHp)
    put("tempHp", state.hp.tempHp)
    put("hitDice", JSONObject().apply {
        put("total", state.hitDice.total)
        put("spent", state.hitDice.spent)
        put("die", state.hitDice.die.name)
        put("conMod", state.hitDice.conMod)
    })
}

/** Контейнер для совместной передачи HP и Hit Dice. */
data class HpAndHitDice(
    val hp: HpState,
    val hitDice: HitDiceState,
)

/**
 * Десериализация HP-блока из [CharacterData]. Устойчива к отсутствию
 * полей (новые поля добавляются → существующие JSON-блобы остаются
 * читаемыми). Битые значения тихо клампятся.
 */
internal fun hpStateFromJson(json: JSONObject?): HpAndHitDice {
    if (json == null) return HpAndHitDice(HpState(), HitDiceState())
    val hp = HpState(
        maxHp     = json.optInt("maxHp", 0).coerceAtLeast(0),
        currentHp = json.optInt("currentHp", 0).coerceAtLeast(0),
        tempHp    = json.optInt("tempHp", 0).coerceAtLeast(0),
    )
    val hitDiceObj = json.optJSONObject("hitDice")
    val hitDice = if (hitDiceObj == null) {
        HitDiceState()
    } else {
        val die = runCatching { HitDie.valueOf(hitDiceObj.optString("die", "D8")) }
            .getOrDefault(HitDie.D8)
        val total = hitDiceObj.optInt("total", 0).coerceIn(0, 200)
        val spent = hitDiceObj.optInt("spent", 0).coerceIn(0, total)
        HitDiceState(
            total = total,
            spent = spent,
            die = die,
            conMod = hitDiceObj.optInt("conMod", 0).coerceIn(-10, 10),
        )
    }
    return HpAndHitDice(hp, hitDice)
}
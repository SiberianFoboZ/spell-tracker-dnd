package com.example.spelltracker.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Слой постоянного состояния на SharedPreferences.
 *
 * Здесь живут:
 *   - уровни классов ([classLevels])
 *   - использованные ячейки заклинаний по уровням 1..9 ([usedSlots])
 *   - использованные ячейки пакт-магии колдуна ([usedPactSlots])
 *   - флаги «известно» / «подготовлено» для отдельных заклинаний
 *
 * Изменения пишутся в активный [Character] и публикуются в StateFlow,
 * чтобы Compose-UI мог подписываться реактивно.
 *
 * **Этап 21: синглтон.** Раньше `HomeViewModel` и `EditCustomSlotViewModel`
 * создавали каждый свой экземпляр [SpellStorage], из-за чего
 * [customSlots] в одном VM обновлялся, а в другом — нет (отсюда
 * «после сохранения ничего не меняется, только после рестарта»).
 * Теперь все VM делят один экземпляр через [Companion.get] — все
 * StateFlow реактивно синхронизированы между экранами.
 *
 * **Этап 22: мульти-персонажи.** Появились [characters] и
 * [activeCharacterId]. Все StateFlow'ы ([classLevels], [usedSlots] и т.п.)
 * теперь отражают данные **активного** персонажа. При вызове
 * [setActiveCharacter] текущий снимок сериализуется в
 * `char_data_${oldId}`, новый — загружается в те же StateFlow'ы.
 *
 * Конструктор приватный — единственный способ получить экземпляр:
 * `SpellStorage.get(context.applicationContext)`.
 */
class SpellStorage private constructor(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ─────────── Состояние активного персонажа (Этап 22) ───────────
    //
    // Все MutableStateFlow ниже отражают данные АКТИВНОГО персонажа.
    // При переключении ([setActiveCharacter]) они перезаписываются
    // данными нового персонажа; при любой мутации вызывается
    // [persistCurrentCharacter], который сериализует текущий снимок
    // обратно в blob активного персонажа.

    private val _classLevels = MutableStateFlow<Map<String, Int>>(emptyMap())
    val classLevels: StateFlow<Map<String, Int>> = _classLevels.asStateFlow()

    private val _usedSlots = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val usedSlots: StateFlow<Map<Int, Int>> = _usedSlots.asStateFlow()

    private val _usedPactSlots = MutableStateFlow(0)
    val usedPactSlots: StateFlow<Int> = _usedPactSlots.asStateFlow()

    private val _usedArcanums = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    val usedArcanums: StateFlow<Map<Int, Boolean>> = _usedArcanums.asStateFlow()

    private val _customSlots = MutableStateFlow<List<CustomSlot>>(emptyList())
    val customSlots: StateFlow<List<CustomSlot>> = _customSlots.asStateFlow()

    private val _prepared = MutableStateFlow<Set<Long>>(emptySet())
    val prepared: StateFlow<Set<Long>> = _prepared.asStateFlow()

    // ─────────── HP и Hit Dice (Этап HP) ───────────
    //
    // Хранится как один агрегированный снимок [HpAndHitDice], потому что
    // HP и Hit Dice живут вместе в JSON-блобе CharacterData. Реактивность
    // обеспечивается одним MutableStateFlow вместо двух — обновляются
    // всегда атомарно.
    private val _hpAndHitDice = MutableStateFlow(HpAndHitDice(HpState(), HitDiceState()))
    val hpAndHitDice: StateFlow<HpAndHitDice> = _hpAndHitDice.asStateFlow()

    // ─────────── Персонажи (Этап 22) ───────────

    private val _characters = MutableStateFlow<List<Character>>(emptyList())
    val characters: StateFlow<List<Character>> = _characters.asStateFlow()

    private val _activeCharacterId = MutableStateFlow<Long?>(null)
    val activeCharacterId: StateFlow<Long?> = _activeCharacterId.asStateFlow()

    init {
        migrateToCharactersIfNeeded()
        _characters.value = loadCharactersList()
        _activeCharacterId.value = loadActiveCharacterId()
        loadActiveCharacterData()
    }

    // ─────────── Caster level по PHB ───────────

    /**
     * Полный caster level для определения таблицы ячеек заклинаний
     * в мультиклассе. Колдун в формуле не участвует — у него собственная
     * пакт-магия.
     *
     * Делегирует в [SlotTables.computeCasterLevel] — формула PHB p.165:
     *   - full caster (factor ≥ 1.0)       → +lvl
     *   - 1/3 caster (Этап 26: floor)      → +lvl / 3
     *   - half caster roundUp (artificer)  → +(lvl+1)/2
     *   - half caster                      → +lvl/2
     *
     * **Этап 26** (2026-07-13): 1/3 формула изменена с `(lvl+2)/3` (ceil,
     * проектный house rule из Этапа 19) на `lvl/3` (PHB floor) — по
     * просьбе пользователя. Также добавлены отдельные PHB-таблицы
     * для монокласса, см. [SlotTables.MONOCLASS_TABLES].
     */
    fun computeCasterLevel(): Int =
        SlotTables.computeCasterLevel(_classLevels.value)

    fun getWarlockLevel(): Int = _classLevels.value["warlock"] ?: 0

    /**
     * Максимум ячеек N-го уровня для текущего набора классов.
     *
     * Учитывает режим ([SlotTables.SlotMode]):
     *   - [SlotTables.SlotMode.Monoclass]  — индивидуальная PHB-таблица класса
     *   - [SlotTables.SlotMode.Multiclass] — мультикласс-таблица при итоговом CL
     *   - [SlotTables.SlotMode.None]       — 0
     */
    fun getSlotTotal(spellLevel: Int): Int =
        SlotTables.getSlotTotal(_classLevels.value, spellLevel)

    fun getSlotUsed(spellLevel: Int): Int = _usedSlots.value[spellLevel] ?: 0

    fun getClassLevel(classId: String): Int = _classLevels.value[classId] ?: 0

    // ─────────── Мутации (каждая в конце вызывает persistCurrentCharacter) ───────────

    fun setClassLevel(classId: String, level: Int) {
        val clamped = level.coerceIn(0, 20)
        _classLevels.update { it + (classId to clamped) }
        persistCurrentCharacter()
    }

    fun useSlot(spellLevel: Int) {
        val total = getSlotTotal(spellLevel)
        val used = getSlotUsed(spellLevel)
        if (used < total) {
            _usedSlots.update { it + (spellLevel to used + 1) }
            persistCurrentCharacter()
        }
    }

    fun restoreSlot(spellLevel: Int) {
        val used = getSlotUsed(spellLevel)
        if (used > 0) {
            _usedSlots.update { it + (spellLevel to used - 1) }
            persistCurrentCharacter()
        }
    }

    /** Применить таблицу: сбросить used, если total уменьшился. */
    fun applySlotTable() {
        val newUsed = (1..9).associateWith { lvl ->
            val cap = getSlotTotal(lvl)
            getSlotUsed(lvl).coerceAtMost(cap)
        }
        _usedSlots.value = newUsed
        persistCurrentCharacter()
    }

    /** Полный сброс активного персонажа (классы → 0, все ячейки → 0). */
    fun resetAllUsed() {
        _classLevels.value = Classes.ALL.associate { it.id to 0 }
        _usedSlots.value = (1..9).associateWith { 0 }
        _usedPactSlots.value = 0
        _usedArcanums.value = ARCANUM_LEVELS.associateWith { false }
        _customSlots.value = emptyList()
        _prepared.value = emptySet()
        // Этап HP: debug-only сброс возвращает HP к max, temp = 0,
        // Hit Dice — все доступны. Сам maxHp/conMod/die — сохраняются
        // (это настройки, а не «потраченные» ресурсы).
        _hpAndHitDice.update { current ->
            current.copy(
                hp = current.hp.copy(currentHp = current.hp.maxHp, tempHp = 0),
                hitDice = current.hitDice.copy(spent = 0),
            )
        }
        persistCurrentCharacter()
    }

    // ─────────── Пакт-магия колдуна ───────────

    fun usePactSlot() {
        val total = getPactSlotTotal()
        if (_usedPactSlots.value < total) {
            _usedPactSlots.update { it + 1 }
            persistCurrentCharacter()
        }
    }

    fun restorePactSlot() {
        if (_usedPactSlots.value > 0) {
            _usedPactSlots.update { it - 1 }
            persistCurrentCharacter()
        }
    }

    fun getPactSlotTotal(): Int {
        val wl = getWarlockLevel()
        val entry = WARLOCK_SLOTS[wl] ?: return 0
        return entry[0]
    }

    fun getPactSlotLevel(): Int {
        val wl = getWarlockLevel()
        val entry = WARLOCK_SLOTS[wl] ?: return 0
        return entry[1]
    }

    fun applyWarlockSlots() {
        // При изменении уровня Колдуна `used` клампится до нового cap.
        val cap = getPactSlotTotal()
        if (_usedPactSlots.value > cap) {
            _usedPactSlots.value = cap
            persistCurrentCharacter()
        }
    }

    // ─────────── Арканумы (Этап 17) ───────────

    fun setArcanumUsed(level: Int, used: Boolean) {
        if (level !in ARCANUM_LEVELS) return
        _usedArcanums.update { it + (level to used) }
        persistCurrentCharacter()
    }

    fun getArcanumUsed(level: Int): Boolean = _usedArcanums.value[level] ?: false

    // ─────────── Отдых (Этап 15, Этап 17, Этап 24) ───────────

    /**
     * Короткий отдых:
     *   - восстановить ячейки пакт-магии Колдуна
     *   - восстановить кастомные ячейки с [RestType.SHORT]
     *
     * Арканумы и кастомные ячейки с [RestType.LONG] **не трогаем** —
     * правило PHB и явный выбор пользователя (тип восстановления
     * задаётся в форме кастомной ячейки, см. [CustomSlotForm]).
     */
    fun shortRest() {
        _usedPactSlots.value = 0
        _customSlots.update { list ->
            list.map { slot ->
                if (slot.restType == RestType.SHORT) slot.copy(used = 0) else slot
            }
        }
        persistCurrentCharacter()
    }

    /**
     * Длинный отдых:
     *   - восстановить обычные ячейки 1..9
     *   - восстановить пакт-магию
     *   - восстановить арканумы Колдуна
     *   - восстановить ВСЕ кастомные ячейки (RestType.SHORT и LONG).
     *
     * Этап 24 v2: long rest теперь восстанавливает и short-rest слоты
     * тоже — иначе пользователь не понимал, почему «после длинного
     * отдыха способность всё равно недоступна». Long rest логически
     * является надмножеством short rest, поэтому сбрасывает всё.
     * Если нужен более редкий ресурс — пользователь использует
     * RestType.LONG и не отдыхает часто.
     */
    fun longRest() {
        _usedSlots.value = (1..9).associateWith { 0 }
        _usedPactSlots.value = 0
        _usedArcanums.value = ARCANUM_LEVELS.associateWith { false }
        _customSlots.update { list ->
            // Long rest — восстанавливаем ВСЕ кастомные ячейки.
            list.map { it.copy(used = 0) }
        }
        // Этап HP: long rest восстанавливает HP по правилам PHB:
        // current = max, temp HP = 0, spent Hit Dice = 0 (ВСЕ кубики доступны).
        // По просьбе пользователя — «длинный отдых восстановит все кости
        // кубов». Это расходится со строгим PHB (где spent -= ceil(total/2)),
        // но удобнее для соло/домашней партии. HP-логика — PHB-faithful.
        _hpAndHitDice.update { current ->
            current.copy(
                hp = current.hp.copy(
                    currentHp = current.hp.maxHp,
                    tempHp = 0,
                ),
                hitDice = current.hitDice.copy(spent = 0),
            )
        }
        persistCurrentCharacter()
    }

    // ─────────── Prepared / known заклинания ───────────

    fun setPrepared(spellId: Long, prep: Boolean) {
        val newSet = if (prep) _prepared.value + spellId else _prepared.value - spellId
        _prepared.value = newSet
        persistCurrentCharacter()
    }

    /** Проверить, подготовлено ли заклинание (для UI-кнопок). */
    fun isPrepared(spellId: Long): Boolean = _prepared.value.contains(spellId)

    /**
     * Удалить из [prepared] все id, которых нет в [validIds].
     * Используется после destructive-миграции БД (v3→v4) — старые id
     * из SharedPreferences (были основаны на name.hashCode()) могут больше
     * не существовать в v4-таблице (id = numeric из source-системы).
     * Без этой чистки «сироты» висят в БД-несуществующем состоянии и
     * показываются в бейдже «N подготовленных», но не подсвечиваются ни
     * в одном ряду списка.
     *
     * Сохранение в SharedPreferences происходит в конце (через [persistCurrentCharacter]).
     */
    fun pruneOrphanedPrepared(validIds: Set<Long>) {
        val current = _prepared.value
        val orphans = current - validIds
        if (orphans.isEmpty()) return
        _prepared.value = current - orphans
        persistCurrentCharacter()
    }

    // ─────────── Пользовательские ячейки (Этап 20) ───────────

    fun getCustomSlotById(id: Long): CustomSlot? =
        _customSlots.value.find { it.id == id }

    fun addCustomSlot(slot: CustomSlot) {
        _customSlots.update { it + slot }
        persistCurrentCharacter()
    }

    /**
     * Обновить ячейку по id. Если id не найден — no-op (без crash).
     * `used` принудительно клампится в 0..total, чтобы ручное изменение
     * total в экране редактирования не оставляло «использований» больше,
     * чем позволяет новая ёмкость.
     */
    fun updateCustomSlot(slot: CustomSlot) {
        val normalized = slot.copy(used = slot.used.coerceIn(0, slot.total))
        val exists = _customSlots.value.any { it.id == normalized.id }
        if (!exists) return
        _customSlots.update { current ->
            current.map { if (it.id == normalized.id) normalized else it }
        }
        persistCurrentCharacter()
    }

    /** Удалить ячейку по id. no-op, если id не найден. */
    fun deleteCustomSlot(id: Long) {
        if (_customSlots.value.none { it.id == id }) return
        _customSlots.update { it.filter { slot -> slot.id != id } }
        persistCurrentCharacter()
    }

    fun useCustomSlot(id: Long) {
        val current = _customSlots.value
        val target = current.find { it.id == id } ?: return
        if (target.used >= target.total) return
        _customSlots.update { list ->
            list.map { slot ->
                if (slot.id == id) slot.copy(used = slot.used + 1) else slot
            }
        }
        persistCurrentCharacter()
    }

    // ─────────── HP и Hit Dice (Этап HP) ───────────
    //
    // Все мутации ниже идут атомарно через [_hpAndHitDice.update], потому
    // что HP и Hit Dice — один блоб в [CharacterData]. После update
    // обязательно вызываем [persistCurrentCharacter].

    /** Установить max HP. currentHp клампится в 0..newMax. */
    fun setMaxHp(value: Int) {
        val clamped = value.coerceIn(0, 9999)
        _hpAndHitDice.update { current ->
            current.copy(hp = current.hp.copy(
                maxHp = clamped,
                currentHp = current.hp.currentHp.coerceIn(0, clamped),
            ))
        }
        persistCurrentCharacter()
    }

    /** Установить current HP напрямую (из модалки). Клампится в 0..maxHp. */
    fun setCurrentHp(value: Int) {
        val max = _hpAndHitDice.value.hp.maxHp
        _hpAndHitDice.update { current ->
            current.copy(hp = current.hp.copy(currentHp = value.coerceIn(0, max)))
        }
        persistCurrentCharacter()
    }

    /**
     * Прибавить дельту к current HP (от кнопок ±1 / ±5).
     * Клампится в 0..maxHp.
     */
    fun adjustCurrentHp(delta: Int) {
        val snap = _hpAndHitDice.value.hp
        val newVal = (snap.currentHp + delta).coerceIn(0, snap.maxHp)
        if (newVal == snap.currentHp) return
        _hpAndHitDice.update { it.copy(hp = it.hp.copy(currentHp = newVal)) }
        persistCurrentCharacter()
    }

    /**
     * Установить temp HP (из модалки). temp HP поглощает урон первым;
     * если новый temp > старого, заменяем; если меньше — вычитаем разницу
     * из currentHp. Так работают правила PHB по stacking temporary HP.
     */
    fun setTempHp(value: Int) {
        val clamped = value.coerceIn(0, 9999)
        val snap = _hpAndHitDice.value.hp
        val newCurrent = if (clamped >= snap.tempHp) {
            snap.currentHp
        } else {
            (snap.currentHp - (snap.tempHp - clamped)).coerceAtLeast(0)
        }
        _hpAndHitDice.update { it.copy(hp = it.hp.copy(
            tempHp = clamped,
            currentHp = newCurrent,
        )) }
        persistCurrentCharacter()
    }

    /** Прибавить дельту к temp HP (от кнопок ±). Клампится в 0..9999. */
    fun adjustTempHp(delta: Int) {
        val snap = _hpAndHitDice.value.hp
        val newVal = (snap.tempHp + delta).coerceIn(0, 9999)
        if (newVal == snap.tempHp) return
        // Соблюдаем правило PHB: новый temp не может увеличить currentHp.
        val newCurrent = if (newVal >= snap.tempHp) {
            snap.currentHp
        } else {
            (snap.currentHp - (snap.tempHp - newVal)).coerceAtLeast(0)
        }
        _hpAndHitDice.update { it.copy(hp = it.hp.copy(
            tempHp = newVal,
            currentHp = newCurrent,
        )) }
        persistCurrentCharacter()
    }

    // ─────────── Hit Dice ───────────

    /** Задать Hit Dice одной транзакцией: total/spent/die/conMod. */
    fun updateHitDice(state: HitDiceState) {
        _hpAndHitDice.update { it.copy(hitDice = state.copy(
            spent = state.spent.coerceIn(0, state.total),
        )) }
        persistCurrentCharacter()
    }

    /** Прибавить дельту к total Hit Dice (от кнопок ± в настройках HD). */
    fun adjustHitDiceTotal(delta: Int) {
        val snap = _hpAndHitDice.value.hitDice
        val newTotal = (snap.total + delta).coerceIn(0, 200)
        val newSpent = snap.spent.coerceAtMost(newTotal)
        _hpAndHitDice.update { it.copy(hitDice = it.hitDice.copy(
            total = newTotal,
            spent = newSpent,
        )) }
        persistCurrentCharacter()
    }

    /** Прибавить дельту к conMod (от кнопок ± в настройках HD). */
    fun adjustHitDiceConMod(delta: Int) {
        val snap = _hpAndHitDice.value.hitDice
        val newCon = (snap.conMod + delta).coerceIn(-10, 10)
        if (newCon == snap.conMod) return
        _hpAndHitDice.update { it.copy(hitDice = it.hitDice.copy(conMod = newCon)) }
        persistCurrentCharacter()
    }

    /**
     * Потратить [count] Hit Dice на коротком отдыхе (PHB).
     *
     * Хилинг считается **по PHB**: каждый кубик бросается отдельно,
     * результаты складываются, плюс `conMod` за каждый кубик.
     *
     *   heal = sum(rolls) + conMod * count
     *
     * Например, d8 CON=2 при бросках [3, 5, 7]:
     *   heal = (3 + 5 + 7) + 2 * 3 = 15 + 6 = 21 HP.
     *
     * Если [rolls] не заданы (например, UI просит «бросить и применить»
     * в один шаг), вызывающий код сам бросает [count] чисел через
     * [com.example.spelltracker.util.Xoroshiro128Plus] и передаёт их.
     *
     * @param rolls список значений на кубиках (длина == count).
     *              Если null или неверной длины — метод возвращает 0
     *              и не списывает кубики.
     * @return сколько HP фактически восстановлено (для Snackbar).
     */
    fun spendHitDice(count: Int, rolls: List<Int>? = null): Int {
        val snap = _hpAndHitDice.value
        val hd = snap.hitDice
        if (count <= 0 || hd.available < count) return 0
        if (rolls == null || rolls.size != count) return 0
        val perDieCon = hd.conMod * count
        val rolledSum = rolls.sum()
        val totalHeal = (rolledSum + perDieCon).coerceAtLeast(count) // минимум 1 HP за каждый кубик
        val maxHealable = (snap.hp.maxHp - snap.hp.currentHp).coerceAtLeast(0)
        val actualHeal = totalHeal.coerceAtMost(maxHealable)
        _hpAndHitDice.update { current ->
            current.copy(
                hp = current.hp.copy(currentHp = current.hp.currentHp + actualHeal),
                hitDice = current.hitDice.copy(spent = current.hitDice.spent + count),
            )
        }
        persistCurrentCharacter()
        return actualHeal
    }

    // ─────────── Управление персонажами (Этап 22) ───────────

    /**
     * Переключиться на другого персонажа. Сохраняет текущий снимок
     * в blob предыдущего, загружает blob нового в StateFlow'ы.
     * Если id совпадает с активным — no-op.
     */
    fun setActiveCharacter(id: Long) {
        if (id == _activeCharacterId.value) return
        if (_characters.value.none { it.id == id }) return  // неизвестный id
        persistCurrentCharacter()
        _activeCharacterId.value = id
        prefs.edit().putLong(KEY_ACTIVE_CHARACTER_ID, id).apply()
        loadActiveCharacterData()
    }

    /**
     * Создать нового персонажа с указанным именем. Не переключает
     * на него — вызывающий код решает, активировать ли сразу.
     * Возвращает созданный [Character].
     *
     * **Локализация пустого имени** — на стороне вызывающего
     * (например, [com.example.spelltracker.ui.characters.CharactersViewModel]):
     * VM резолвит `R.string.characters_default_name` и подставляет
     * его до вызова storage. Сам storage хранит то, что получил.
     */
    fun addCharacter(name: String): Character {
        // id = нанотаймстамп — почти гарантированно уникален между
        // быстрыми последовательными созданиями
        val newId = System.nanoTime()
        val newChar = Character(id = newId, name = name)
        val updated = _characters.value + newChar
        _characters.value = updated
        prefs.edit()
            .putString(KEY_CHARACTERS_JSON, charactersListToJson(updated))
            .putString(charDataKey(newId), characterDataToJson(CharacterData()))
            .apply()
        return newChar
    }

    /**
     * Удалить персонажа. Нельзя удалить последнего — оставляем как
     * минимум одного. Если удалён активный — переключаемся на первого
     * из оставшихся.
     */
    fun deleteCharacter(id: Long) {
        if (_characters.value.size <= 1) return  // последнего удалять нельзя
        if (_characters.value.none { it.id == id }) return
        val updated = _characters.value.filter { it.id != id }
        _characters.value = updated
        prefs.edit()
            .putString(KEY_CHARACTERS_JSON, charactersListToJson(updated))
            .remove(charDataKey(id))
            .apply()
        if (_activeCharacterId.value == id) {
            setActiveCharacter(updated.first().id)
        }
    }

    fun renameCharacter(id: Long, newName: String) {
        if (_characters.value.none { it.id == id }) return
        val updated = _characters.value.map {
            if (it.id == id) it.copy(name = newName.trim()) else it
        }
        _characters.value = updated
        prefs.edit().putString(KEY_CHARACTERS_JSON, charactersListToJson(updated)).apply()
    }

    // ─────────── Внутреннее: миграция и persist ───────────

    /**
     * Первичная миграция Этапа 22: если флаг [KEY_MIGRATED_V22] ещё
     * не выставлен, читаем «плоские» ключи из старого формата
     * (level_*, used_slot_*, used_pact_slots, arcanum_*, custom_slots_json,
     * prepared_ids) и сохраняем их как данные одного персонажа
     * с пустым именем. UI подставит локализованный fallback
     * (`R.string.characters_default_name`) при отображении.
     */
    private fun migrateToCharactersIfNeeded() {
        if (prefs.getBoolean(KEY_MIGRATED_V22, false)) return

        val classLevels = Classes.ALL.associate { it.id to
            prefs.getInt("level_${it.id}", 0) }
        val usedSlots = (1..9).associateWith { prefs.getInt("used_slot_$it", 0) }
        val usedArcanums = ARCANUM_LEVELS.associateWith {
            prefs.getBoolean("arcanum_$it", false)
        }
        val customSlots = customSlotsFromJson(prefs.getString("custom_slots_json", null))
        val prepared = (prefs.getStringSet("prepared_ids", emptySet()) ?: emptySet())
            .mapNotNull { it.toLongOrNull() }
            .toSet()

        val data = CharacterData(
            classLevels = classLevels,
            usedSlots = usedSlots,
            usedPactSlots = prefs.getInt("used_pact_slots", 0),
            usedArcanums = usedArcanums,
            customSlots = customSlots,
            prepared = prepared,
        )

        // Имя оставляем пустым — UI подставит локализованный
        // `R.string.characters_default_name` через свой helper
        // (см. CharactersScreen.characterDisplayName). Это позволяет
        // data-слою оставаться без зависимости от Android-ресурсов
        // и одинаково работать при любой активной локали.
        val defaultChar = Character(id = 1L, name = "")
        prefs.edit()
            .putString(KEY_CHARACTERS_JSON, charactersListToJson(listOf(defaultChar)))
            .putLong(KEY_ACTIVE_CHARACTER_ID, defaultChar.id)
            .putString(charDataKey(defaultChar.id), characterDataToJson(data))
            .putBoolean(KEY_MIGRATED_V22, true)
            .apply()
    }

    /** Загрузить данные активного персонажа из его blob во все StateFlow'ы. */
    private fun loadActiveCharacterData() {
        val id = _activeCharacterId.value ?: return
        val json = prefs.getString(charDataKey(id), null)
        val data = characterDataFromJson(json)
        _classLevels.value = data.classLevels
        _usedSlots.value = data.usedSlots
        _usedPactSlots.value = data.usedPactSlots
        _usedArcanums.value = data.usedArcanums
        _customSlots.value = data.customSlots
        _prepared.value = data.prepared
        // Этап HP: HP/Hit Dice загружаются атомарно из общего блоба.
        _hpAndHitDice.value = data.hp
    }

    /** Сериализовать текущие StateFlow'ы в blob активного персонажа. */
    private fun persistCurrentCharacter() {
        val id = _activeCharacterId.value ?: return
        val data = CharacterData(
            classLevels = _classLevels.value,
            usedSlots = _usedSlots.value,
            usedPactSlots = _usedPactSlots.value,
            usedArcanums = _usedArcanums.value,
            customSlots = _customSlots.value,
            prepared = _prepared.value,
            hp = _hpAndHitDice.value,
        )
        prefs.edit().putString(charDataKey(id), characterDataToJson(data)).apply()
    }

    private fun loadCharactersList(): List<Character> =
        charactersListFromJson(prefs.getString(KEY_CHARACTERS_JSON, null))

    private fun loadActiveCharacterId(): Long? =
        if (prefs.contains(KEY_ACTIVE_CHARACTER_ID))
            prefs.getLong(KEY_ACTIVE_CHARACTER_ID, -1L).takeIf { it > 0 }
        else null

    private fun charDataKey(id: Long): String = "${KEY_CHAR_DATA_PREFIX}$id"

    // ─────────── Константы ───────────

    companion object {
        private const val PREFS_NAME = "spell_tracker"
        private const val KEY_CUSTOM_SLOTS_JSON = "custom_slots_json"

        // Этап 22: ключи для мульти-персонажей
        private const val KEY_CHARACTERS_JSON = "characters_v22_json"
        private const val KEY_ACTIVE_CHARACTER_ID = "active_character_id_v22"
        private const val KEY_CHAR_DATA_PREFIX = "char_data_v22_"
        private const val KEY_MIGRATED_V22 = "migrated_to_characters_v22"

        /**
         * Единственный экземпляр [SpellStorage] на весь процесс.
         * Создаётся лениво при первом обращении, потокобезопасно
         * (double-checked locking + `@Volatile`).
         *
         * Контекст берётся через `applicationContext`, чтобы случайно
         * не удержать ссылку на Activity/Fragment.
         */
        @Volatile
        private var INSTANCE: SpellStorage? = null

        /**
         * Возвращает общий для приложения экземпляр [SpellStorage].
         * Все ViewModel (`HomeViewModel`, `EditCustomSlotViewModel`, …)
         * подписаны на **одни и те же** StateFlow, поэтому изменения
         * видны реактивно на всех экранах без рестарта.
         */
        fun get(context: Context): SpellStorage {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SpellStorage(context.applicationContext).also { INSTANCE = it }
            }
        }

        /**
         * Пакт-магия колдуна: WARLOCK_SLOTS[warlockLevel] = {число ячеек, уровень ячеек}.
         * Совпадает с PHB-таблицей "Pact Magic".
         */
        val WARLOCK_SLOTS: Map<Int, IntArray> = mapOf(
             1 to intArrayOf(1, 1),
             2 to intArrayOf(2, 1),
             3 to intArrayOf(2, 2),
             4 to intArrayOf(2, 2),
             5 to intArrayOf(3, 3),
             6 to intArrayOf(3, 3),
             7 to intArrayOf(4, 4),
             8 to intArrayOf(4, 4),
             9 to intArrayOf(4, 5),
            10 to intArrayOf(4, 5),
            11 to intArrayOf(4, 5),
            12 to intArrayOf(4, 5),
            13 to intArrayOf(4, 5),
            14 to intArrayOf(4, 5),
            15 to intArrayOf(4, 5),
            16 to intArrayOf(4, 5),
            17 to intArrayOf(4, 5),
            18 to intArrayOf(4, 5),
            19 to intArrayOf(4, 5),
            20 to intArrayOf(4, 5),
        )

        /**
         * Уровни арканумов Колдуна (Этап 17).
         * По одному аркануму на каждый из уровней VI, VII, VIII, IX.
         * Доступ открывается на 11 уровне Колдуна.
         */
        val ARCANUM_LEVELS: IntArray = intArrayOf(6, 7, 8, 9)
    }
}
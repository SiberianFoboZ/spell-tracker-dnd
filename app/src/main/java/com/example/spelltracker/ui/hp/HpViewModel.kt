package com.example.spelltracker.ui.hp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.spelltracker.data.HpAndHitDice
import com.example.spelltracker.data.SpellStorage
import com.example.spelltracker.util.Xoroshiro128Plus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Одноразовое событие от [HpViewModel] к UI (Этап HP).
 *
 * Семантика совпадает с [com.example.spelltracker.ui.home.HomeEvent]:
 * [SharedFlow] с `replay = 0` — Snackbar получат только подписанные
 * в момент emit.
 *
 * Разделение [HitDiceSpent] / [HitDiceSpentNone] нужно, чтобы UI
 * мог показать поясняющее сообщение, когда heal был нулевым
 * (например, у персонажа уже max HP — диалог «Потратить» всё равно
 * списал кубик, но HP не восстановил).
 */
sealed interface HpEvent {
    /** Длинный отдых выполнен — HP = max, temp = 0, Hit Dice восстановлены. */
    object LongRest : HpEvent

    /**
     * Потрачено [count] кубиков, восстановлено [healed] HP.
     *
     * Поля нужны UI для Snackbar и превью:
     *   - [healed] фактически добавленные HP (≤ totalHeal из-за maxHp-клампа).
     *   - [count] количество потраченных кубиков.
     *   - [rolls] список значений на каждом кубике (длина == count).
     *   - [conTotal] суммарный бонус Телосложения = conMod * count.
     *   - [dieSize] верхняя граница кубика (6/8/10/12) — для лейбла «d8: 3, 5, 7».
     */
    data class HitDiceSpent(
        val healed: Int,
        val count: Int,
        val rolls: List<Int>,
        val conTotal: Int,
        val dieSize: Int,
    ) : HpEvent

    /**
     * Попытка потратить Hit Dice, когда maxHp = 0 (настройка не задана).
     * UI должен показать [com.example.spelltracker.R.string.hit_dice_no_max_hp].
     */
    object HitDiceBlockedNoMaxHp : HpEvent
}

/**
 * Снимок состояния экрана HP/Hit Dice для [HpScreen].
 *
 * Состав:
 *   - [hp]: max/current/temp HP (см. [com.example.spelltracker.data.HpState])
 *   - [hitDice]: total/spent/die/conMod (см. [com.example.spelltracker.data.HitDiceState])
 *
 * Иммутабельный data class — Compose читает его через [StateFlow.collectAsState],
 * VM пересобирает снимок при ЛЮБОМ изменении `storage.hpAndHitDice`.
 */
data class HpState(
    val hp: com.example.spelltracker.data.HpState = com.example.spelltracker.data.HpState(),
    val hitDice: com.example.spelltracker.data.HitDiceState =
        com.example.spelltracker.data.HitDiceState(),
) {
    /** Удобный флаг: max HP ещё не задан (первый запуск). */
    val isMaxHpNotSet: Boolean get() = hp.maxHp == 0

    /** Удобный флаг: можно ли вообще тратить Hit Dice. */
    val canSpendHitDice: Boolean
        get() = hitDice.available > 0 && hp.maxHp > 0 && hp.currentHp < hp.maxHp
}

/**
 * ViewModel для экрана HP / Hit Dice (Этап HP).
 *
 * Хранит снимок в [state], обновляемый реактивно из
 * [SpellStorage.hpAndHitDice]. Эмитит [HpEvent] в [events]
 * для отображения Snackbar.
 *
 * Намеренно НЕ вызывает [SpellStorage.shortRest]/[longRest] напрямую —
 * эти мутации уже используются из HomeViewModel и должны срабатывать
 * глобально (shortRest сбрасывает пакт-магию Колдуна + custom slots,
 * которых у HP-экрана нет). Здесь только узкоспециализированные
 * мутации: setMaxHp, setCurrentHp, setTempHp, spendHitDice.
 *
 * Длинный отдых с HP-стороны проксируется через [longRest] →
 * [SpellStorage.longRest] (там же восстановление HD по половине).
 */
class HpViewModel(application: Application) : AndroidViewModel(application) {

    private val storage = SpellStorage.get(application)

    private val _state = MutableStateFlow(snapshot())
    val state: StateFlow<HpState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<HpEvent>(
        replay = 0,
        extraBufferCapacity = 4,
    )
    val events: SharedFlow<HpEvent> = _events.asSharedFlow()

    init {
        // Одна реактивная подписка: любое изменение HP/Hit Dice
        // → пересборка снимка.
        storage.hpAndHitDice
            .onEach { _state.value = snapshot() }
            .launchIn(viewModelScope)
    }

    private fun snapshot(): HpState = HpState(
        hp = storage.hpAndHitDice.value.hp,
        hitDice = storage.hpAndHitDice.value.hitDice,
    )

    // ─────────── Мутации HP (делегаты в storage) ───────────

    fun setMaxHp(value: Int) = storage.setMaxHp(value)
    fun setCurrentHp(value: Int) = storage.setCurrentHp(value)
    fun adjustCurrentHp(delta: Int) = storage.adjustCurrentHp(delta)
    fun setTempHp(value: Int) = storage.setTempHp(value)
    fun adjustTempHp(delta: Int) = storage.adjustTempHp(delta)

    // ─────────── Мутации Hit Dice ───────────

    fun updateHitDice(state: com.example.spelltracker.data.HitDiceState) =
        storage.updateHitDice(state)

    fun adjustHitDiceTotal(delta: Int) = storage.adjustHitDiceTotal(delta)
    fun adjustHitDiceConMod(delta: Int) = storage.adjustHitDiceConMod(delta)

    /**
     * Потратить [count] Hit Dice на коротком отдыхе (PHB).
     *
     * Если [rolls] не заданы — бросает [count] кубиков сам через
     * [Xoroshiro128Plus] (по одному на кубик, согласно PHB). Хилинг
     * считается в [SpellStorage.spendHitDice] как `sum(rolls) +
     * conMod * count`.
     *
     * Эмитит [HpEvent.HitDiceSpent] (даже если heal = 0 — кубики же
     * потрачены) или [HpEvent.HitDiceBlockedNoMaxHp], если max HP не
     * задан.
     */
    fun spendHitDice(count: Int, rolls: List<Int>? = null) {
        if (state.value.hp.maxHp == 0) {
            viewModelScope.launch { _events.emit(HpEvent.HitDiceBlockedNoMaxHp) }
            return
        }
        val hd = state.value.hitDice
        val finalRolls = rolls ?: List(count) {
            Xoroshiro128Plus.instance.nextInt(1, hd.die.maxValue + 1)
        }
        val healed = storage.spendHitDice(count, finalRolls)
        val conTotal = hd.conMod * count
        viewModelScope.launch {
            _events.emit(HpEvent.HitDiceSpent(
                healed = healed,
                count = count,
                rolls = finalRolls,
                conTotal = conTotal,
                dieSize = hd.die.maxValue,
            ))
        }
    }

    // ─────────── Отдых ───────────

    /**
     * Длинный отдых из HP-экрана — проксирует в [SpellStorage.longRest],
     * который помимо HP восстановит также ячейки заклинаний / пакт /
     * арканумы / custom slots. Намеренно единая точка отдыха для всех
     * ресурсов, чтобы пользователь не держал в голове «откуда отдыхать».
     */
    fun longRest() {
        storage.longRest()
        viewModelScope.launch { _events.emit(HpEvent.LongRest) }
    }

    /** Полный сброс (debug-only): maxHp остаётся, current → max, temp → 0, spent → 0. */
    fun resetAll() = storage.resetAllUsed()
}
package com.example.spelltracker.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.spelltracker.data.Classes
import com.example.spelltracker.data.SpellStorage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Снимок состояния для [HomeScreen].
 *
 * Compose не должен напрямую читать [SpellStorage] — он работает
 * с этим иммутабельным снимком, который обновляется реактивно.
 *
 * Этап 16: ячейки заклинаний и пакт-магия Колдуна объединены в
 * единый список [allSlots] для унифицированной отрисовки.
 */
data class HomeState(
    val classLevels: Map<String, Int> = emptyMap(),
    val casterLevel: Int = 0,
    val regularSlots: List<SlotInfo> = emptyList(),
    /** null, если warlockLevel == 0 (или у Колдуна нет ячеек на его уровне). */
    val warlockSlot: SlotInfo? = null,
    val warlockLevel: Int = 0,
    val pactSlotLevel: Int = 0,
) {
    /**
     * Все ячейки в порядке отрисовки: обычные уровни 1..9, затем
     * (если есть) — пакт-магия Колдуна. Используется в `SpellSlotsSection`.
     */
    val allSlots: List<SlotInfo>
        get() = regularSlots + listOfNotNull(warlockSlot)
}

/**
 * Описание одной строки «ячейки заклинания N-го уровня».
 *
 * [isWarlock] = true для пакт-магии Колдуна — в UI она отрисовывается
 * идентично обычным ячейкам (те же блоки), но помечается подписью
 * «Колдун» под бейджем уровня и восстанавливается отдельно
 * на коротком отдыхе.
 */
data class SlotInfo(
    val level: Int,        // 1..9 для обычных; spell level Колдуна для пакт-магии
    val total: Int,
    val used: Int,
    val isWarlock: Boolean = false,
)

/**
 * Одноразовое событие из ViewModel в UI (Этап 15).
 *
 * Используется для показа Snackbar после короткого/длинного отдыха.
 * SharedFlow с `replay=0` — событие получат только те, кто подписан
 * в момент emit (а не подписавшиеся позже).
 */
sealed interface HomeEvent {
    object ShortRest : HomeEvent
    object LongRest  : HomeEvent
}

/**
 * ViewModel для главного экрана. Хранит [SpellStorage] в Application-скоупе
 * и пересчитывает [HomeState] при любом изменении class levels / used slots.
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val storage = SpellStorage(application)

    private val _state = MutableStateFlow(snapshot())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<HomeEvent>(
        replay = 0,
        extraBufferCapacity = 4,
    )
    /** Поток одноразовых событий (показать Snackbar и т.п.). */
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    init {
        // Любое изменение уровней классов ИЛИ использованных ячеек
        // (обычных и пакт) вызывает пересборку снимка.
        combine(
            storage.classLevels,
            storage.usedSlots,
            storage.usedPactSlots,
        ) { _, _, _ -> snapshot() }
            .onEach { _state.value = it }
            .launchIn(viewModelScope)
    }

    private fun snapshot(): HomeState {
        val casterLevel = storage.computeCasterLevel()
        val regular = (1..9)
            .map {
                SlotInfo(
                    level = it,
                    total = storage.getSlotTotal(it),
                    used = storage.getSlotUsed(it),
                    isWarlock = false,
                )
            }
            .filter { it.total > 0 }
        val wl = storage.getWarlockLevel()
        val pactTotal = storage.getPactSlotTotal()
        val warlockSlot = if (wl > 0 && pactTotal > 0) {
            SlotInfo(
                level = storage.getPactSlotLevel(),
                total = pactTotal,
                used = storage.usedPactSlots.value,
                isWarlock = true,
            )
        } else null
        return HomeState(
            classLevels = storage.classLevels.value,
            casterLevel = casterLevel,
            regularSlots = regular,
            warlockSlot = warlockSlot,
            warlockLevel = wl,
            pactSlotLevel = storage.getPactSlotLevel(),
        )
    }

    fun setClassLevel(classId: String, level: Int) {
        storage.setClassLevel(classId, level)
        storage.applySlotTable()
        storage.applyWarlockSlots()
    }

    // ─────────── Тап по строке уровня (Этап 16) ───────────

    /**
     * Обработка тапа по строке уровня ячеек.
     * «Гасит» первую доступную ячейку слева направо.
     * Если все ячейки уровня потрачены — no-op (UI блокирует клик
     * через `clickable(enabled = ...)`).
     */
    fun onRowClick(slot: SlotInfo) {
        if (slot.used >= slot.total) return
        if (slot.isWarlock) storage.usePactSlot() else storage.useSlot(slot.level)
    }

    // ─────────── Кнопки отдыха (Этап 15) ───────────

    /** Короткий отдых: восстановить ячейки пакт-магии Колдуна + Snackbar. */
    fun shortRest() {
        storage.shortRest()
        viewModelScope.launch { _events.emit(HomeEvent.ShortRest) }
    }

    /** Длинный отдых: восстановить все ячейки + Snackbar. */
    fun longRest() {
        storage.longRest()
        viewModelScope.launch { _events.emit(HomeEvent.LongRest) }
    }

    /** Полный сброс всего (включая class levels) — для отладки. */
    fun resetAllUsed() = storage.resetAllUsed()

    /** Доступ к списку классов — для рендера 3×3 сетки. */
    fun classes(): List<Classes.Info> = Classes.ALL
}

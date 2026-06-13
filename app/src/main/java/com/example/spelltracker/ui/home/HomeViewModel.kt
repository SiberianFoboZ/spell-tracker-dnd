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
 */
data class HomeState(
    val classLevels: Map<String, Int> = emptyMap(),
    val casterLevel: Int = 0,
    val slots: List<SlotInfo> = emptyList(),
    val warlockLevel: Int = 0,
    val pactSlots: Int = 0,
    val pactSlotLevel: Int = 0,
    val pactUsed: Int = 0,
)

/** Описание одной строки «ячейки заклинания N-го уровня» на главном экране. */
data class SlotInfo(
    val level: Int,        // 1..9
    val total: Int,
    val used: Int,
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
    /** Поток одноразовых событий (показать Snackbar, диалог и т.п.). */
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    init {
        // Любое изменение уровней классов ИЛИ использованных ячеек
        // вызывает пересборку снимка.
        combine(storage.classLevels, storage.usedSlots, storage.usedPactSlots) { _, _, _ ->
            snapshot()
        }.onEach { _state.value = it }.launchIn(viewModelScope)
    }

    private fun snapshot(): HomeState {
        val casterLevel = storage.computeCasterLevel()
        val slots = (1..9)
            .map { lvl -> SlotInfo(lvl, storage.getSlotTotal(lvl), storage.getSlotUsed(lvl)) }
            .filter { it.total > 0 }
        return HomeState(
            classLevels = storage.classLevels.value,
            casterLevel = casterLevel,
            slots = slots,
            warlockLevel = storage.getWarlockLevel(),
            pactSlots = storage.getPactSlotTotal(),
            pactSlotLevel = storage.getPactSlotLevel(),
            pactUsed = storage.usedPactSlots.value,
        )
    }

    fun setClassLevel(classId: String, level: Int) {
        storage.setClassLevel(classId, level)
        storage.applySlotTable()
        storage.applyWarlockSlots()
    }

    fun useSlot(level: Int)    = storage.useSlot(level)
    fun restoreSlot(level: Int) = storage.restoreSlot(level)

    fun usePactSlot()    = storage.usePactSlot()
    fun restorePactSlot() = storage.restorePactSlot()

    // ─────────── Отдых (Этап 15) ───────────

    /**
     * Короткий отдых: восстановить ячейки пакт-магии Колдуна и
     * отправить событие [HomeEvent.ShortRest] для показа Snackbar.
     */
    fun shortRest() {
        storage.shortRest()
        viewModelScope.launch { _events.emit(HomeEvent.ShortRest) }
    }

    /**
     * Длинный отдых: восстановить все ячейки заклинаний и пакт-магии
     * и отправить событие [HomeEvent.LongRest] для показа Snackbar.
     */
    fun longRest() {
        storage.longRest()
        viewModelScope.launch { _events.emit(HomeEvent.LongRest) }
    }

    /** Полный сброс всего (включая class levels) — для отладки / на крайний случай. */
    fun resetAllUsed() = storage.resetAllUsed()

    /** Доступ к списку классов — для рендера 3×3 сетки. */
    fun classes(): List<Classes.Info> = Classes.ALL
}

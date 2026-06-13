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
 *
 * Этап 17: добавлены [arcanums] — арканумы Колдуна (VI..IX), по одному
 * на каждый уровень. Доступ завит от [warlockLevel].
 */
data class HomeState(
    val classLevels: Map<String, Int> = emptyMap(),
    val casterLevel: Int = 0,
    val regularSlots: List<SlotInfo> = emptyList(),
    /** null, если warlockLevel == 0 (или у Колдуна нет ячеек на его уровне). */
    val warlockSlot: SlotInfo? = null,
    val warlockLevel: Int = 0,
    val pactSlotLevel: Int = 0,
    /**
     * Список арканумов Колдуна. Содержит только уровни, доступные
     * на текущем [warlockLevel] (или пуст, если warlockLevel < 11).
     * Каждый арканум — ровно один блок, который можно «потратить».
     */
    val arcanums: List<ArcanumInfo> = emptyList(),
) {
    /**
     * Все ячейки в порядке отрисовки: обычные уровни 1..9, затем
     * (если есть) — пакт-магия Колдуна. Используется в `SpellSlotsSection`.
     */
    val allSlots: List<SlotInfo>
        get() = regularSlots + listOfNotNull(warlockSlot)

    /**
     * Максимальное число арканумов для текущего warlock level
     * (0 при warlockLevel < 11).
     */
    val arcanumsCapacity: Int get() = arcanums.size

    /** Есть ли хоть один потраченный арканум. */
    val hasAnyArcanumUsed: Boolean
        get() = arcanums.any { it.used }
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
 * Арканум Колдуна (Этап 17).
 *
 * У Колдуна ровно по одному аркануму каждого уровня из доступных
 * (6, 7, 8, 9). [used] = true означает «потрачен». Восстановление —
 * только длинный отдых, клик по строке может только **потратить**.
 */
data class ArcanumInfo(
    val level: Int,        // 6..9
    val used: Boolean,
)

/**
 * Одноразовое событие из ViewModel в UI (Этап 15, Этап 17).
 *
 * Используется для показа Snackbar после короткого/длинного отдыха.
 * SharedFlow с `replay=0` — событие получат только те, кто подписан
 * в момент emit (а не подписавшиеся позже).
 */
sealed interface HomeEvent {
    object ShortRest : HomeEvent
    object LongRest  : HomeEvent
    /**
     * Короткий отдых не восстановил арканумы, потому что они
     * восстанавливаются только на длинном. Показываем Snackbar-предупреждение.
     */
    object ArcanumShortRestBlocked : HomeEvent
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
        // (обычных, пакт и арканумов) вызывает пересборку снимка.
        combine(
            storage.classLevels,
            storage.usedSlots,
            storage.usedPactSlots,
            storage.usedArcanums,
        ) { _, _, _, _ -> snapshot() }
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
        // Этап 17: арканумы. Доступ завит от warlockLevel.
        //   < 11         → 0
        //   11..12       → 1 (VI)
        //   13..14       → 2 (VI, VII)
        //   15..16       → 3 (VI, VII, VIII)
        //   17+          → 4 (VI, VII, VIII, IX)
        val arcanumCount = when {
            wl < 11 -> 0
            wl < 13 -> 1
            wl < 15 -> 2
            wl < 17 -> 3
            else    -> 4
        }
        val arcanums = (0 until arcanumCount).map { idx ->
            val lvl = SpellStorage.ARCANUM_LEVELS[idx]
            ArcanumInfo(
                level = lvl,
                used = storage.getArcanumUsed(lvl),
            )
        }
        return HomeState(
            classLevels = storage.classLevels.value,
            casterLevel = casterLevel,
            regularSlots = regular,
            warlockSlot = warlockSlot,
            warlockLevel = wl,
            pactSlotLevel = storage.getPactSlotLevel(),
            arcanums = arcanums,
        )
    }

    fun setClassLevel(classId: String, level: Int) {
        storage.setClassLevel(classId, level)
        storage.applySlotTable()
        storage.applyWarlockSlots()
    }

    // ─────────── Тап по строке уровня (Этап 16, Этап 17) ───────────

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

    /**
     * Обработка тапа по строке арканума (Этап 17).
     * Арканум можно **только потратить** кликом. Восстановление —
     * исключительно длинный отдых. Если арканум уже потрачен —
     * no-op (UI блокирует клик).
     */
    fun onArcanumClick(arcanum: ArcanumInfo) {
        if (arcanum.used) return
        storage.setArcanumUsed(arcanum.level, true)
    }

    // ─────────── Кнопки отдыха (Этап 15, Этап 17) ───────────

    /**
     * Короткий отдых: восстановить ячейки пакт-магии Колдуна.
     * Арканумы **не трогаем** — это правило PHB.
     *
     * Перед сбросом pact slots проверяем, есть ли потраченные арканумы.
     * Если да — UI покажет Snackbar «Арканумы восстанавливаются только
     * после длинного отдыха» (через [HomeEvent.ArcanumShortRestBlocked]).
     */
    fun shortRest() {
        storage.shortRest()
        viewModelScope.launch { _events.emit(HomeEvent.ShortRest) }
        // Если при коротком отдыхе есть потраченные арканумы —
        // дополнительно уведомляем UI, чтобы он показал поясняющий Snackbar.
        if (state.value.hasAnyArcanumUsed) {
            viewModelScope.launch { _events.emit(HomeEvent.ArcanumShortRestBlocked) }
        }
    }

    /** Длинный отдых: восстановить все ячейки, пакт-магию и все арканумы + Snackbar. */
    fun longRest() {
        storage.longRest()
        viewModelScope.launch { _events.emit(HomeEvent.LongRest) }
    }

    /** Полный сброс всего (включая class levels) — для отладки. */
    fun resetAllUsed() = storage.resetAllUsed()

    /** Доступ к списку классов — для рендера 3×3 сетки. */
    fun classes(): List<Classes.Info> = Classes.ALL
}

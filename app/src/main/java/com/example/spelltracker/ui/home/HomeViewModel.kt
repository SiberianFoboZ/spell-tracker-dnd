package com.example.spelltracker.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.spelltracker.data.Classes
import com.example.spelltracker.data.CustomSlot
import com.example.spelltracker.data.SlotTables
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
 * Этап 18: пакт-магия Колдуна **отделена** от обычных ячеек и
 * живёт в собственной секции «МАГИЯ ДОГОВОРА». Ячейки других классов
 * (включая Колдуна как «обычного» полно-кастера в мультиклассе) — в
 * [regularSlots], пакт-слоты — в [pactSlot] (или null, если Warlock
 * не выбран / нет ячеек на его уровне).
 *
 * Этап 17: [arcanums] — арканумы Колдуна (VI..IX), по одному
 * на каждый уровень. Доступ завит от [warlockLevel].
 */
data class HomeState(
    val classLevels: Map<String, Int> = emptyMap(),
    /**
     * Число, отображаемое в [EffectiveLevelPanel] (большая золотая цифра).
     *
     * **Этап 26** — семантика зависит от режима:
     *   - [spellMode] = [SpellMode.Monoclass] → **фактический уровень класса**
     *     (например, Паладин 5 → 5), **без** мультиклассовой формулы
     *   - [spellMode] = [SpellMode.Multiclass] → caster level по PHB p.165
     *     (например, Паладин 3 / Жрец 2 → 3)
     *   - [spellMode] = [SpellMode.None] → 0 (некому показывать)
     *
     * До Этапа 26 число всегда было результатом `computeCasterLevel()`
     * (multiclass-формула) — отсюда был баг: Паладин 5 показывал 2
     * вместо 5. Теперь UI говорит правду в обоих режимах.
     */
    val casterLevel: Int = 0,
    /**
     * **Этап 26**: режим расчёта ячеек заклинаний. Определяет,
     * какие таблицы применяются (см. [SlotTables.detectMode] и
     * [SlotTables.getSlotTotal]), и как интерпретируется [casterLevel]
     * для UI-индикатора в [EffectiveLevelPanel].
     */
    val spellMode: SpellMode = SpellMode.None,
    /** Ячейки заклинаний всех классов, кроме пакт-магии Колдуна. */
    val regularSlots: List<SlotInfo> = emptyList(),
    /**
     * Пакт-магия Колдуна: `null`, если warlockLevel == 0 (Колдун не выбран)
     * или cap == 0 на текущем уровне. В обоих случаях секция «МАГИЯ
     * ДОГОВОРА» просто не рисуется.
     */
    val pactSlot: SlotInfo? = null,
    val warlockLevel: Int = 0,
    val pactSlotLevel: Int = 0,
    /**
     * Список арканумов Колдуна. Содержит только уровни, доступные
     * на текущем [warlockLevel] (или пуст, если warlockLevel < 11).
     * Каждый арканум — ровно один блок, который можно «потратить».
     */
    val arcanums: List<ArcanumInfo> = emptyList(),
    /**
     * Этап 20: пользовательские ячейки (универсальный конструктор).
     * Сортируются в порядке добавления (id ASC) — пользователь видит
     * ячейки в том порядке, в котором их создал, и легко находит
     * «верхние» / «нижние» без перетасовки при изменении любого поля.
     */
    val customSlots: List<CustomSlot> = emptyList(),
) {
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
 * **Этап 26**: режим расчёта ячеек заклинаний (UI-представление).
 *
 * Дубликат [SlotTables.SlotMode] на уровне UI-слоя, потому что
 * [SlotTables.SlotMode] помечен `internal` (виден только в data-модуле),
 * а HomeState — публичное API экрана. Семантика совпадает 1-в-1.
 *
 *   - [None] — нет активных кастер-классов
 *   - [Monoclass] — ровно один активный кастер, используется его
 *     индивидуальная PHB-таблица
 *   - [Multiclass] — 2+ активных кастеров, мультиклассовая формула
 */
sealed interface SpellMode {
    object None : SpellMode
    data class Monoclass(val classId: String) : SpellMode
    object Multiclass : SpellMode
}

/**
 * Описание одной строки «ячейки заклинания N-го уровня».
 *
 * Этап 18: пакт-магия Колдуна отрисовывается отдельной строкой
 * через собственный [HomeState.pactSlot], поэтому `isWarlock`
 * больше **не нужен** — оставлен устаревшим полем по умолчанию
 * `false` для бинарной совместимости со старыми вызывающими
 * (на случай, если что-то в UI ещё на него опирается; безопасно
 * удалить в одном из следующих релизов).
 */
data class SlotInfo(
    val level: Int,        // 1..9 для обычных; spell level Колдуна для пакт-магии
    val total: Int,
    val used: Int,
    @Deprecated("Пакт-магия теперь в отдельном HomeState.pactSlot")
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

    private val storage = SpellStorage.get(application)

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
        // (обычных, пакт, арканумов, **пользовательских** — Этап 20)
        // вызывает пересборку снимка.
        combine(
            storage.classLevels,
            storage.usedSlots,
            storage.usedPactSlots,
            storage.usedArcanums,
            storage.customSlots,
        ) { _, _, _, _, _ -> snapshot() }
            .onEach { _state.value = it }
            .launchIn(viewModelScope)
    }

    private fun snapshot(): HomeState {
        // Этап 26: режим расчёта ячеек и значение casterLevel зависят
        // от набора уровней. Один источник истины — [SlotTables.detectMode].
        val levels = storage.classLevels.value
        val mode = SlotTables.detectMode(levels)
        val spellMode: SpellMode = when (mode) {
            SlotTables.SlotMode.None -> SpellMode.None
            is SlotTables.SlotMode.Monoclass -> SpellMode.Monoclass(mode.classId)
            SlotTables.SlotMode.Multiclass -> SpellMode.Multiclass
        }
        // Число для UI: для монокласса — фактический уровень класса
        // (без мультиклассовой формулы!), для мультикласса — CL по PHB.
        val displayedCasterLevel = when (mode) {
            SlotTables.SlotMode.None -> 0
            is SlotTables.SlotMode.Monoclass -> levels[mode.classId] ?: 0
            SlotTables.SlotMode.Multiclass -> storage.computeCasterLevel()
        }
        val regular = (1..9)
            .map {
                SlotInfo(
                    level = it,
                    total = storage.getSlotTotal(it),
                    used = storage.getSlotUsed(it),
                )
            }
            .filter { it.total > 0 }
        // Этап 18: пакт-слот Колдуна — отдельная запись, не часть regularSlots.
        val wl = storage.getWarlockLevel()
        val pactTotal = storage.getPactSlotTotal()
        val pactLevel = storage.getPactSlotLevel()
        val pact = if (wl > 0 && pactTotal > 0) {
            SlotInfo(
                level = pactLevel,
                total = pactTotal,
                used = storage.usedPactSlots.value,
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
            casterLevel = displayedCasterLevel,
            spellMode = spellMode,
            regularSlots = regular,
            pactSlot = pact,
            warlockLevel = wl,
            pactSlotLevel = pactLevel,
            arcanums = arcanums,
            // Этап 20: пользовательские ячейки в порядке создания.
            // `id` монотонно растёт (System.currentTimeMillis при создании),
            // так что простая сортировка по id даёт стабильный порядок.
            customSlots = storage.customSlots.value.sortedBy { it.id },
        )
    }

    fun setClassLevel(classId: String, level: Int) {
        storage.setClassLevel(classId, level)
        storage.applySlotTable()
        storage.applyWarlockSlots()
    }

    // ─────────── Тап по строке уровня (Этап 18) ───────────

    /**
     * Тап по строке обычной ячейки (любой класс, кроме пакт-магии Колдуна).
     * «Гасит» первую доступную ячейку слева направо.
     * Если все ячейки уровня потрачены — no-op (UI блокирует клик
     * через `clickable(enabled = ...)`).
     */
    fun onRegularRowClick(slot: SlotInfo) {
        if (slot.used >= slot.total) return
        storage.useSlot(slot.level)
    }

    /**
     * Тап по строке пакт-магии Колдуна (секция «МАГИЯ ДОГОВОРА»).
     * По правилам PHB у Колдуна все ячейки одного уровня, и
     * восстанавливаются они на **коротком** отдыхе. Семантика та же —
     * «гасим» первую доступную ячейку слева.
     */
    fun onPactRowClick(slot: SlotInfo) {
        if (slot.used >= slot.total) return
        storage.usePactSlot()
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

    // ─────────── Пользовательские ячейки (Этап 20) ───────────

    /**
     * Тап по строке пользовательской ячейки — «гасит» одну ячейку
     * (по аналогии с [onRegularRowClick] и [onPactRowClick]).
     * Если все ячейки потрачены — no-op (UI блокирует клик).
     */
    fun onCustomRowClick(slot: CustomSlot) {
        if (slot.isAllSpent) return
        storage.useCustomSlot(slot.id)
    }

    /**
     * Добавить новую пользовательскую ячейку. Вызывается из
     * `AddCustomSlotSheet` (bottom sheet) после заполнения формы.
     * id генерируется во внешнем коде, чтобы не зависеть от системного
     * времени внутри VM (тестируемость + предсказуемость).
     */
    fun addCustomSlot(slot: CustomSlot) {
        storage.addCustomSlot(slot)
    }

    /**
     * Обновить пользовательскую ячейку. Вызывается из
     * `EditCustomSlotScreen` при сохранении формы.
     */
    fun updateCustomSlot(slot: CustomSlot) {
        storage.updateCustomSlot(slot)
    }

    /**
     * Удалить пользовательскую ячейку. Вызывается из
     * `EditCustomSlotScreen` при нажатии «Удалить» (после подтверждения).
     */
    fun deleteCustomSlot(id: Long) {
        storage.deleteCustomSlot(id)
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

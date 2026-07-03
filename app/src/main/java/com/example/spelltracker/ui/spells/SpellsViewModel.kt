package com.example.spelltracker.ui.spells

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.spelltracker.data.ClassFilter
import com.example.spelltracker.data.Classes
import com.example.spelltracker.data.ComponentFlag
import com.example.spelltracker.data.Spell
import com.example.spelltracker.data.SpellFilterState
import com.example.spelltracker.data.SpellMenuConfig
import com.example.spelltracker.data.SpellRepository
import com.example.spelltracker.data.SpellStorage
import com.example.spelltracker.data.TriState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

/**
 * Состояние экрана списка заклинаний.
 *
 * Поля разделены на три зоны:
 *   • «загружено» — то, что пришло из БД / хранилища: allSpells, preparedIds,
 *     available* (уникальные значения для осей фильтра);
 *   • «выбрано» — filters ([SpellFilterState]);
 *   • «ui» — флаги UI (isLoading, showPreparedOnly, showFiltersSheet).
 *
 * Производные поля (visibleSpells) считаются в combine-блоке.
 *
 * Расы (races) удалены из фильтра — перегружали BottomSheet, и запрос
 * «раса X» не давал осмысленной фильтрации.
 */
data class SpellsState(
    // Загружено
    val isLoading: Boolean = true,
    val allSpells: List<Spell> = emptyList(),
    val classes: List<Classes.Info> = Classes.ALL,
    val availableLevels: Set<Int> = emptySet(),
    val availableSubclasses: Set<String> = emptySet(),
    val availableSchools: Set<String> = emptySet(),
    val availableSources: Set<String> = emptySet(),
    val availableSavingThrows: Set<String> = emptySet(),
    val preparedIds: Set<Long> = emptySet(),
    val preparedCount: Int = 0,

    // Фильтры (по умолчанию всё пустое = все спеллы показаны)
    val filters: SpellFilterState = SpellFilterState(),

    // UI
    val showPreparedOnly: Boolean = false,
    val showFiltersSheet: Boolean = false,

    // Производное
    val visibleSpells: List<Spell> = emptyList(),
)

/**
 * VM экрана заклинаний. Один источник истины — `state` ([StateFlow]).
 *
 * Подписки:
 *   • `repo.initialized.onEach { ... }` — после заливки данных однажды
 *     заполняет `allSpells` и `available*` (только при первом запуске).
 *   • `combine(_state, storage.prepared)` — реактивно пересчитывает
 *     `visibleSpells` при изменении фильтров или `prepared` (вкл/выкл
 *     закладки). Statefold-у equality не зацикливается.
 */
class SpellsViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = SpellRepository(application)
    private val storage = SpellStorage.get(application)

    private val _state = MutableStateFlow(SpellsState())
    val state: StateFlow<SpellsState> = _state.asStateFlow()

    init {
        repo.ensureInitialized()

        // Жёсткая ссылка на снимок allSpells нужна, чтобы lazy-поле
        // [subclassToParents] могло его использовать без гонки с combine-блоком.
        // Сохраняем в [allSpellsSnapshot] сразу после получения из БД.
        repo.initialized
            .onEach { ready ->
                if (!ready || _state.value.allSpells.isNotEmpty()) return@onEach
                val all = repo.getAll()
                allSpellsSnapshot = all
                _state.update {
                    it.copy(
                        isLoading = false,
                        allSpells = all,
                        availableLevels = all.mapTo(HashSet()) { it.level },
                        availableSubclasses = all.flatMapTo(HashSet()) { splitCsv(it.subclasses) },
                        availableSchools = all.mapTo(HashSet()) { it.school }
                            .filter(String::isNotBlank).toSet(),
                        availableSources = all.mapTo(HashSet()) { it.source }
                            .filter(String::isNotBlank).toSet(),
                        availableSavingThrows = all.flatMapTo(HashSet()) { splitCsv(it.savingThrows) },
                    )
                }
            }
            .launchIn(viewModelScope)

        combine(_state, storage.prepared) { s, prep ->
            val visible = if (s.allSpells.isEmpty()) emptyList()
                else applyFilters(s.allSpells, s.filters, s.showPreparedOnly, prep)
            s.copy(
                preparedIds = prep,
                preparedCount = prep.size,
                visibleSpells = visible,
            )
        }.onEach { _state.value = it }.launchIn(viewModelScope)
    }

    private fun applyFilters(
        all: List<Spell>,
        f: SpellFilterState,
        showPreparedOnly: Boolean,
        preparedIds: Set<Long>,
    ): List<Spell> = all
        .asSequence()
        .filter { !showPreparedOnly || preparedIds.contains(it.id) }
        .filter { ClassFilter.matches(it, f) }
        .toList()

    private fun splitCsv(s: String): List<String> =
        if (s.isBlank()) emptyList() else s.split(',').map(String::trim).filter(String::isNotEmpty)

    /**
     * Маппинг «имя подкласса → множество parent class English id».
     * Считается один раз при загрузке данных. Нужен чтобы в UI показывать
     * только подклассы выбранных классов (или скрывать секцию, если класс
     * не выбран вовсе).
     */
    private val subclassToParents: Map<String, Set<String>> by lazy {
        val map = mutableMapOf<String, MutableSet<String>>()
        for (spell in allSpellsSnapshot) {
            val names = splitCsv(spell.subclasses)
            val parents = splitCsv(spell.subclassParents)
            for ((name, parent) in names.zip(parents)) {
                map.getOrPut(name) { mutableSetOf() }.add(parent)
            }
        }
        map.mapValues { it.value.toSet() }
    }

    /** Снимок allSpells для инициализации lazy-полей выше. */
    private var allSpellsSnapshot: List<Spell> = emptyList()

    /**
     * Подклассы, которые сейчас имеет смысл показывать в фильтре:
     *   • если [SpellFilterState.classIds] пуст → пустое множество
     *     (UI прячет секцию «Подкласс» — нечего выбирать без класса);
     *   • если классы выбраны → только те подклассы, чьи parent
     *     классы пересекаются с выбранными.
     *
     * Считается реактивно из текущего [state.filters.classIds].
     */
    val displayedSubclasses: Set<String>
        get() {
            val selected = state.value.filters.classIds
            if (selected.isEmpty()) return emptySet()
            return subclassToParents.entries
                .filter { (_, parents) -> parents.any { it in selected } }
                .map { it.key }
                .toSet()
        }

    // ─── Мутаторы фильтров ───

    fun setLevel(level: Int?) =
        mutate { it.copy(filters = it.filters.copy(level = level)) }

    fun toggleClass(classId: String) =
        mutate { it.copy(filters = it.filters.copy(classIds = toggleId(it.filters.classIds, classId))) }

    fun toggleSubclass(name: String) =
        mutate { it.copy(filters = it.filters.copy(subclassNames = toggleId(it.filters.subclassNames, name))) }

    fun toggleSource(key: String) =
        mutate { it.copy(filters = it.filters.copy(sources = toggleId(it.filters.sources, key))) }

    fun setAllSources(on: Boolean) =
        mutate { it.copy(filters = it.filters.copy(sources = if (on) it.availableSources else emptySet())) }

    fun toggleSchool(key: String) =
        mutate { it.copy(filters = it.filters.copy(schools = toggleId(it.filters.schools, key))) }

    fun toggleSavingThrow(key: String) =
        mutate { it.copy(filters = it.filters.copy(savingThrows = toggleId(it.filters.savingThrows, key))) }

    fun setRitual(ts: TriState) =
        mutate { it.copy(filters = it.filters.copy(ritual = ts)) }

    fun setConcentration(ts: TriState) =
        mutate { it.copy(filters = it.filters.copy(concentration = ts)) }

    /** Multi-select компонента: добавляет или убирает [flag] из required set. */
    fun toggleComponent(flag: ComponentFlag) =
        mutate { it.copy(filters = it.filters.copy(requiredComponents = toggleId(it.filters.requiredComponents, flag))) }

    fun setSearch(q: String) =
        mutate { it.copy(filters = it.filters.copy(search = q)) }

    fun togglePreparedOnly() =
        mutate { it.copy(showPreparedOnly = !it.showPreparedOnly) }

    fun setShowFiltersSheet(v: Boolean) =
        mutate { it.copy(showFiltersSheet = v) }

    fun togglePrepared(spellId: Long) {
        storage.setPrepared(spellId, !storage.isPrepared(spellId))
    }

    /** Вызывается чипом «Все» в секции «Класс». */
    fun clearClassFilter() =
        mutate { it.copy(filters = it.filters.copy(classIds = emptySet())) }

    /** Вызывается чипом «Все» в секции «Уровень». */
    fun clearLevelFilter() =
        mutate { it.copy(filters = it.filters.copy(level = null)) }

    /** Сбросить ВСЕ фильтры — вызывается кнопкой «Сбросить» в BottomSheet. */
    fun resetFilters() =
        mutate { it.copy(filters = SpellFilterState()) }

    private inline fun mutate(transform: (SpellsState) -> SpellsState) {
        _state.update(transform)
    }

    private fun toggleId(set: Set<String>, v: String): Set<String> =
        if (v in set) set - v else set + v

    private fun toggleId(set: Set<ComponentFlag>, v: ComponentFlag): Set<ComponentFlag> =
        if (v in set) set - v else set + v
}

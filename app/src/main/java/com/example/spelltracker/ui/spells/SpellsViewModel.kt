package com.example.spelltracker.ui.spells

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.spelltracker.data.Classes
import com.example.spelltracker.data.Spell
import com.example.spelltracker.data.SpellRepository
import com.example.spelltracker.data.SpellStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Режим фильтра: «по классу» или «по уровню заклинания».
 * Два независимых набора чипов на экране.
 */
enum class FilterMode { BY_CLASS, BY_LEVEL }

/**
 * Состояние экрана списка заклинаний.
 */
data class SpellsState(
    val isLoading: Boolean = true,
    val allSpells: List<Spell> = emptyList(),
    val visibleSpells: List<Spell> = emptyList(),
    val classes: List<Classes.Info> = Classes.ALL,
    val availableLevels: Set<Int> = emptySet(),
    val selectedClassIds: Set<String> = emptySet(),
    val selectedLevel: Int? = null,    // null = «Все»
    val search: String = "",
    val mode: FilterMode = FilterMode.BY_CLASS,
    val preparedIds: Set<Long> = emptySet(),
    val showPreparedOnly: Boolean = false,  // вкл. «только подготовленные»
    val preparedCount: Int = 0,             // счётчик в TopAppBar
)

/**
 * VM для экрана заклинаний. Загружает весь справочник через
 * [SpellRepository] (инициализируется при первом запуске) и применяет
 * фильтры в памяти — справочник маленький, накладных расходов нет.
 */
class SpellsViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = SpellRepository(application)
    private val storage = SpellStorage(application)

    private val _state = MutableStateFlow(SpellsState())
    val state: StateFlow<SpellsState> = _state.asStateFlow()

    init {
        repo.ensureInitialized()
        viewModelScope.launch {
            // Ждём, пока БД наполнится, и загружаем один раз.
            repo.initialized.collect { ready ->
                if (ready && _state.value.allSpells.isEmpty()) {
                    val all = repo.getAll()
                    val levels = all.map { it.level }.toSet()
                    _state.value = _state.value.copy(
                        isLoading = false,
                        allSpells = all,
                        availableLevels = levels,
                        visibleSpells = applyFilters(all, _state.value),
                    )
                }
            }
        }
        // Реактивно обновляем видимый список при изменении фильтров/поиска
        combine(_state, storage.prepared) { s, prep ->
            s.copy(
                preparedIds = prep,
                preparedCount = prep.size,
                visibleSpells = applyFilters(s.allSpells, s),
            )
        }.onEach { _state.value = it }.launchIn(viewModelScope)
    }

    private fun applyFilters(all: List<Spell>, s: SpellsState): List<Spell> {
        val needle = s.search.trim().lowercase()
        return all
            .asSequence()
            .filter { s.showPreparedOnly.not() || s.preparedIds.contains(it.id) }
            .filter { s.selectedLevel == null || it.level == s.selectedLevel }
            .filter { s.selectedClassIds.isEmpty() || s.selectedClassIds.any { id -> it.classes.contains(id) } }
            .filter { needle.isEmpty() || it.name.lowercase().contains(needle) }
            .toList()
    }

    fun setMode(mode: FilterMode) {
        _state.value = _state.value.copy(
            mode = mode,
            selectedLevel = if (mode == FilterMode.BY_LEVEL) _state.value.selectedLevel else null,
            // выходим из «только подготовленные» при смене режима фильтра
            showPreparedOnly = false,
        )
    }

    fun setLevel(level: Int?) {
        _state.value = _state.value.copy(selectedLevel = level)
    }

    fun toggleClass(classId: String) {
        val current = _state.value.selectedClassIds
        val next = if (classId in current) current - classId else current + classId
        _state.value = _state.value.copy(selectedClassIds = next)
    }

    fun setSearch(q: String) {
        _state.value = _state.value.copy(search = q)
    }

    fun togglePreparedOnly() {
        _state.value = _state.value.copy(showPreparedOnly = !_state.value.showPreparedOnly)
    }

    fun togglePrepared(spellId: Long) {
        storage.setPrepared(spellId, !storage.isPrepared(spellId))
    }
}

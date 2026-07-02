package com.example.spelltracker.ui.detail

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.spelltracker.data.Spell
import com.example.spelltracker.data.SpellRepository
import com.example.spelltracker.data.SpellStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Состояние экрана детальной карточки заклинания.
 */
data class SpellDetailState(
    val isLoading: Boolean = true,
    val spell: Spell? = null,
    val isPrepared: Boolean = false,
)

/**
 * VM детальной карточки. Принимает id заклинания в конструкторе —
 * создаётся через [Factory] в NavController.
 */
class SpellDetailViewModel(
    application: Application,
    private val spellId: Long,
) : ViewModel() {

    private val repo = SpellRepository(application)
    private val storage = SpellStorage.get(application)

    private val _state = MutableStateFlow(SpellDetailState())
    val state: StateFlow<SpellDetailState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.ensureInitialized()
            repo.initialized.collect { ready ->
                if (!ready) return@collect
                val spell = repo.getById(spellId)
                _state.value = SpellDetailState(
                    isLoading = false,
                    spell = spell,
                    isPrepared = storage.isPrepared(spellId),
                )
                return@collect
            }
        }
        storage.prepared
            .onEach { ids ->
                val cur = _state.value
                if (cur.spell != null) {
                    _state.value = cur.copy(isPrepared = ids.contains(cur.spell.id))
                }
            }
            .launchIn(viewModelScope)
    }

    fun togglePrepared() {
        storage.setPrepared(spellId, !storage.isPrepared(spellId))
    }

    /**
     * Фабрика: передаёт spellId в конструктор VM. Используется в
     * AppNavigation при переходе на маршрут "spell/{id}".
     */
    class Factory(
        private val application: Application,
        private val spellId: Long,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SpellDetailViewModel(application, spellId) as T
        }
    }
}

package com.example.spelltracker.ui.customslot

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.spelltracker.data.CustomSlot
import com.example.spelltracker.data.SpellStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Состояние экрана редактирования пользовательской ячейки (Этап 20).
 *
 *  - [slot]      — текущее редактируемое значение (загружается из
 *                  [SpellStorage] при создании VM и обновляется через
 *                  [EditCustomSlotViewModel.update] по мере правки формы)
 *  - [isFound]   — `false`, если ячейка с заданным id не найдена
 *                  (например, удалили в другой сессии, а роут ещё
 *                  хранит старый id). В этом случае экран показывает
 *                  «Ячейка не найдена» вместо формы.
 */
data class EditCustomSlotState(
    val slot: CustomSlot? = null,
    val isFound: Boolean = false,
)

/**
 * VM экрана редактирования пользовательской ячейки (Этап 20).
 *
 * Создаётся через [Factory] с id ячейки, полученным из
 * `navArgument("id")` в маршруте `customslot/{id}`.
 *
 * При инициализации подтягивает текущее значение из [SpellStorage].
 * Если ячейка не найдена — [isFound] = false, форма не показывается.
 *
 * Изменения через [update] — чистые, без записи в storage, чтобы
 * пользователь мог отменить (нажать back) без побочных эффектов.
 * Запись происходит только при явном [save] / [delete].
 */
class EditCustomSlotViewModel(
    application: Application,
    private val slotId: Long,
) : AndroidViewModel(application) {

    private val storage = SpellStorage.get(application)

    private val _state = MutableStateFlow(EditCustomSlotState())
    val state: StateFlow<EditCustomSlotState> = _state.asStateFlow()

    init {
        val existing = storage.getCustomSlotById(slotId)
        _state.value = EditCustomSlotState(
            slot = existing,
            isFound = existing != null,
        )
    }

    /**
     * Применить трансформацию к текущему слоту. Если слот не загружен
     * (isFound = false) — no-op. Используется формой:
     * `viewModel.update { slot -> slot.copy(title = newTitle) }`.
     */
    fun update(transform: (CustomSlot) -> CustomSlot) {
        val cur = _state.value.slot ?: return
        _state.value = _state.value.copy(slot = transform(cur))
    }

    /**
     * Сохранить изменения. Записывает в [SpellStorage] и **вызывающий
     * код** (экран) должен сам сделать `onBack()`. Не возвращаем
     * boolean: `false`-ветка (пустой title) уже отключена в UI.
     */
    fun save() {
        val slot = _state.value.slot ?: return
        if (slot.title.isBlank()) return
        storage.updateCustomSlot(slot)
    }

    /**
     * Удалить ячейку. Вызывающий код (экран) после [delete] должен
     * сам сделать `onBack()` — здесь мы не знаем, нужно ли закрывать
     * экран (теоретически можно остаться на пустой форме, но UX
     * у нас однозначный — после удаления возвращаемся).
     */
    fun delete() {
        val slot = _state.value.slot ?: return
        storage.deleteCustomSlot(slot.id)
    }

    /**
     * Фабрика для Compose Navigation: пробрасывает [Application] и
     * [slotId] в конструктор VM. Идиома та же, что в
     * `SpellDetailViewModel.Factory`.
     */
    class Factory(
        private val application: Application,
        private val slotId: Long,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EditCustomSlotViewModel(application, slotId) as T
        }
    }
}

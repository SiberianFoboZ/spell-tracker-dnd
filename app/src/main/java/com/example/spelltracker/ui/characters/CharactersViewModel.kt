package com.example.spelltracker.ui.characters

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.spelltracker.data.Character
import com.example.spelltracker.data.SpellStorage
import kotlinx.coroutines.flow.StateFlow

/**
 * VM экрана «Персонажи» (Этап 22 — мульти-персонажи).
 *
 * Не хранит собственного state — прокидывает напрямую [SpellStorage]'s
 * [SpellStorage.characters] и [SpellStorage.activeCharacterId]. Это
 * гарантирует мгновенную реактивность: добавление/удаление/переключение
 * сразу видны на всех экранах.
 */
class CharactersViewModel(application: Application) : AndroidViewModel(application) {

    private val storage = SpellStorage.get(application)

    /** Список всех персонажей (реактивно). */
    val characters: StateFlow<List<Character>> = storage.characters

    /** id активного персонажа (реактивно). */
    val activeCharacterId: StateFlow<Long?> = storage.activeCharacterId

    /** Создать нового персонажа. Возвращает созданный [Character]. */
    fun addCharacter(name: String): Character = storage.addCharacter(name)

    /** Переключиться на персонажа. Экран сам сделает `onBack()`. */
    fun setActive(id: Long) = storage.setActiveCharacter(id)

    /** Удалить персонажа. Нельзя удалить последнего. */
    fun deleteCharacter(id: Long) = storage.deleteCharacter(id)

    /** Переименовать персонажа. Пустое имя → «Без имени». */
    fun renameCharacter(id: Long, newName: String) =
        storage.renameCharacter(id, newName)

    class Factory(
        private val application: Application,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CharactersViewModel(application) as T
        }
    }
}
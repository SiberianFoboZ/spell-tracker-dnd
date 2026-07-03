package com.example.spelltracker.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Единая точка доступа к Room-базе со справочником заклинаний.
 *
 * Приложение запускает [ensureInitialized] при старте, и если БД пуста —
 * парсит `spells_normalized.json` из assets и заливает все заклинания в таблицу
 * `spells`. Данные собраны build-time Gradle-таской `generateSpellsDb` из
 * per-spell JSON в `spells_data/` (см. app/build.gradle.kts).
 * `spells`. После этого [initialized] становится true, и ViewModel-ы
 * могут реагировать.
 */
class SpellRepository(context: Context) {

    private val appContext = context.applicationContext
    private val db = SpellDatabase.get(appContext)
    private val dao = db.spellDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _initialized = MutableStateFlow(false)
    val initialized: StateFlow<Boolean> = _initialized.asStateFlow()

    fun ensureInitialized() {
        scope.launch {
            if (dao.count() == 0) {
                val allSpells = SpellParser.loadFromAssets(appContext)
                if (allSpells.isNotEmpty()) {
                    dao.insertAll(allSpells)
                }
            }
            _initialized.value = true
        }
    }

    suspend fun getAll(): List<Spell> = withContext(Dispatchers.IO) { dao.getAll() }

    suspend fun getById(id: Long): Spell? = withContext(Dispatchers.IO) { dao.getById(id) }

    /**
     * Загрузить все заклинания и отфильтровать по UI-параметрам.
     * Фильтрация делается в памяти — справочник маленький, JSON-ы
     * кешируются Room-ом, так что это дёшево.
     */
    suspend fun filter(
        level: Int?,            // null = любой
        classIds: Set<String>,  // пусто = любой
        search: String,         // "" = без поиска по имени
    ): List<Spell> = withContext(Dispatchers.IO) {
        val all = dao.getAll()
        val needle = search.trim().lowercase()
        all.asSequence()
            .filter { level == null || it.level == level }
            .filter { classIds.isEmpty() || classIds.any { id -> it.classes.contains(id) } }
            .filter { needle.isEmpty() || it.name.lowercase().contains(needle) }
            .toList()
    }
}

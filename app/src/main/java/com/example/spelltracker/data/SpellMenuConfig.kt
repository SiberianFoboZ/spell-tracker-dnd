package com.example.spelltracker.data

/**
 * Константы для фильтр-меню заклинаний.
 *
 * Значения ключей — те же, что в JSON `source.shortName`, `school`,
 * `savingThrow` в исходных файлах каталога `spells_data/` (один JSON на
 * заклинание) и в сгенерированном `spells_normalized.json`. Хранятся здесь,
 * чтобы фильтр-UI и фильтр-логика ссылались на единый источник правды.
 *
 * Источник — `menu_json.txt`, который автор приложения держит руками.
 * При обновлении источника — обновить также и этот объект.
 */
object SpellMenuConfig {

    /** Книга-источник. */
    data class Source(val key: String, val label: String, val tooltip: String)

    /** Группа книг-источника (категория в меню). */
    data class SourceGroup(val key: String, val name: String, val sources: List<Source>)

    // ─── Источники (6 групп, 33 книги) ───

    val SOURCE_GROUPS: List<SourceGroup> = listOf(
        SourceGroup("OFFICAL", "Базовые", listOf(
            Source("PHB",  "PHB",  "Книга игрока"),
            Source("XGE",  "XGE",  "Руководство Занатара обо всем"),
            Source("TCE",  "TCE",  "Котел Таши со всякой всячиной"),
            Source("FTD",  "FTD",  "Сокровищница драконов Фицбана"),
            Source("BMT",  "BMT",  "Книга многих вещей"),
        )),
        SourceGroup("MODULE", "Приключения", listOf(
            Source("LLK",    "LLK",    "Потерянная лаборатория Квалиша"),
            Source("AI",     "AI",     "Корпорация приобретений"),
            Source("IDRotf", "IDRotf", "Долина Ледяного Ветра: Иней Морозной девы"),
        )),
        SourceGroup("SETTING", "Сеттинги", listOf(
            Source("SCAG", "SCAG", "Путеводитель приключенца по Побережью меча"),
            Source("GGR",  "GGR",  "Справочник гильдмастера по Равнике"),
            Source("SCC",  "SCC",  "Стриксхейвен: Учебная программа хаоса"),
            Source("AAG",  "AAG",  "Руководство астрального приключенца"),
            Source("SatO", "SatO", "Сигил и Внешние Земли"),
        )),
        SourceGroup("TEST", "Unearthed Arcana", listOf(
            Source("UAMM",      "UAMM",      "Modern Magic"),
            Source("UATOBM",    "UATOBM",    "Древняя черная магия"),
            Source("UASS",      "UASS",      "Стартовые заклинания"),
            Source("UACDW",     "UACDW",     "Жрец, Друид, Волшебник"),
            Source("UAFRW",     "UAFRW",     "Воин, Следопыт, Волшебник"),
            Source("UA20POR",   "UA20POR",   "Пересмотр псионических способностей"),
            Source("UASMT",     "UASMT",     "Заклинания и магические тату"),
            Source("UA21DO",    "UA21DO",    "Драконьи варианты"),
            Source("UA22WotM",  "UA22WotM",  "Чудеса Мультивселенной"),
        )),
        SourceGroup("THIRD_PARTY", "3rd party", listOf(
            Source("MHH",   "MHH",   "Midgard Hero Handbook"),
            Source("ODL",   "ODL",   "Odyssey of the Dragonlords"),
            Source("DMf5E", "DMf5E", "Deep Magic for 5e"),
            Source("EGtW",  "EGtW",  "Explorer's Guide to Wildemount"),
            Source("GHtPG", "GHtPG", "Ghoulhaven Player's Guide"),
            Source("TDCS",  "TDCS",  "Tal'Dorei Campaign Setting"),
            Source("VSoS",  "VSoS",  "Valda's Spire of Secrets"),
            Source("DoDk",  "DoDk",  "Drakkenheim"),
        )),
        SourceGroup("CUSTOM", "Homebrew", listOf(
            Source("ICB", "ICB", "Книга класса Механист"),
            Source("LH",  "LH",  "Laserllama"),
            Source("PG",  "PG",  "Player's Guide: Proliferating Chaos"),
        )),
    )

    /** Все ключи источников в один Set — для дефолтного состояния фильтра. */
    val DEFAULT_SOURCES: Set<String> =
        SOURCE_GROUPS.flatMap { it.sources }.map { it.key }.toSet()

    // ─── Школы магии ───

    data class School(val key: String, val label: String)

    val SCHOOLS: List<School> = listOf(
        School("ABJURATION",    "Ограждение"),
        School("CONJURATION",   "Вызов"),
        School("DIVINATION",    "Прорицание"),
        School("ENCHANTMENT",   "Очарование"),
        School("EVOCATION",     "Воплощение"),
        School("ILLUSION",      "Иллюзия"),
        School("NECROMANCY",    "Некромантия"),
        School("TRANSMUTATION", "Преобразование"),
    )

    // ─── Спасброски ───

    /**
     * В нормализованном JSON `savingThrows` хранится текстом из HTML
     * («Мудрости», «Ловкости», «Силы» и т.п.), поэтому ключи — на русском.
     * Для UI — отображение через [SAVING_THROW_LABELS].
     */
    val SAVING_THROWS: List<Pair<String, String>> = listOf(
        "Сила"          to "Сила",
        "Ловкость"      to "Ловкость",
        "Телосложение"  to "Телосложение",
        "Интеллект"     to "Интеллект",
        "Мудрости"      to "Мудрость",  // в данных — родительный падеж
        "Харизмы"       to "Харизма",  // в данных — родительный падеж
    )

    // ─── Компоненты ───

    /** Подписи для три-стейт фильтров по компонентам (для UI). */
    const val COMPONENT_V_LABEL  = "Вербальный"
    const val COMPONENT_S_LABEL  = "Соматический"
    const val COMPONENT_M_LABEL  = "Материальный"
    const val COMPONENT_RC_LABEL = "Расходуемый"

    /** Подписи для три-стейт переключателя. */
    const val TRI_ANY_LABEL = "Любое"
    const val TRI_YES_LABEL = "Да"
    const val TRI_NO_LABEL  = "Нет"
}

package com.example.spelltracker.data

import androidx.annotation.StringRes
import com.example.spelltracker.R

/**
 * Константы для фильтр-меню заклинаний.
 *
 * Ключи (`key`) — стабильные идентификаторы, совпадают с JSON
 * `source.shortName`, `school`, `savingThrow` в исходных файлах каталога
 * `spells_data/` (один JSON на заклинание) и в сгенерированном
 * `spells_normalized.json`. Хранятся здесь, чтобы фильтр-UI и
 * фильтр-логика ссылались на единый источник правды.
 *
 * Локализованные подписи — через `*Res: Int` (`@StringRes`), резолвятся
 * в UI через `stringResource(...)`. Источник — `menu_json.txt`, который
 * автор приложения держит руками; при обновлении — обновить также и
 * этот объект + соответствующие ключи в `res/values{,-en}/strings.xml`.
 */
object SpellMenuConfig {

    /** Книга-источник. */
    data class Source(
        val key: String,
        @field:StringRes val labelRes: Int,
        @field:StringRes val tooltipRes: Int,
    )

    /** Группа книг-источника (категория в меню). */
    data class SourceGroup(
        val key: String,
        @field:StringRes val nameRes: Int,
        val sources: List<Source>,
    )

    // ─── Источники (6 групп, 33 книги) ───

    val SOURCE_GROUPS: List<SourceGroup> = listOf(
        SourceGroup("OFFICAL", R.string.source_group_OFFICAL, listOf(
            Source("PHB", R.string.source_PHB_label, R.string.source_PHB_tooltip),
            Source("XGE", R.string.source_XGE_label, R.string.source_XGE_tooltip),
            Source("TCE", R.string.source_TCE_label, R.string.source_TCE_tooltip),
            Source("FTD", R.string.source_FTD_label, R.string.source_FTD_tooltip),
            Source("BMT", R.string.source_BMT_label, R.string.source_BMT_tooltip),
        )),
        SourceGroup("MODULE", R.string.source_group_MODULE, listOf(
            Source("LLK",    R.string.source_LLK_label,    R.string.source_LLK_tooltip),
            Source("AI",     R.string.source_AI_label,     R.string.source_AI_tooltip),
            Source("IDRotf", R.string.source_IDRotf_label, R.string.source_IDRotf_tooltip),
        )),
        SourceGroup("SETTING", R.string.source_group_SETTING, listOf(
            Source("SCAG", R.string.source_SCAG_label, R.string.source_SCAG_tooltip),
            Source("GGR",  R.string.source_GGR_label,  R.string.source_GGR_tooltip),
            Source("SCC",  R.string.source_SCC_label,  R.string.source_SCC_tooltip),
            Source("AAG",  R.string.source_AAG_label,  R.string.source_AAG_tooltip),
            Source("SatO", R.string.source_SatO_label, R.string.source_SatO_tooltip),
        )),
        SourceGroup("TEST", R.string.source_group_TEST, listOf(
            Source("UAMM",     R.string.source_UAMM_label,     R.string.source_UAMM_tooltip),
            Source("UATOBM",   R.string.source_UATOBM_label,   R.string.source_UATOBM_tooltip),
            Source("UASS",     R.string.source_UASS_label,     R.string.source_UASS_tooltip),
            Source("UACDW",    R.string.source_UACDW_label,    R.string.source_UACDW_tooltip),
            Source("UAFRW",    R.string.source_UAFRW_label,    R.string.source_UAFRW_tooltip),
            Source("UA20POR",  R.string.source_UA20POR_label,  R.string.source_UA20POR_tooltip),
            Source("UASMT",    R.string.source_UASMT_label,    R.string.source_UASMT_tooltip),
            Source("UA21DO",   R.string.source_UA21DO_label,   R.string.source_UA21DO_tooltip),
            Source("UA22WotM", R.string.source_UA22WotM_label, R.string.source_UA22WotM_tooltip),
        )),
        SourceGroup("THIRD_PARTY", R.string.source_group_THIRD_PARTY, listOf(
            Source("MHH",   R.string.source_MHH_label,   R.string.source_MHH_tooltip),
            Source("ODL",   R.string.source_ODL_label,   R.string.source_ODL_tooltip),
            Source("DMf5E", R.string.source_DMf5E_label, R.string.source_DMf5E_tooltip),
            Source("EGtW",  R.string.source_EGtW_label,  R.string.source_EGtW_tooltip),
            Source("GHtPG", R.string.source_GHtPG_label, R.string.source_GHtPG_tooltip),
            Source("TDCS",  R.string.source_TDCS_label,  R.string.source_TDCS_tooltip),
            Source("VSoS",  R.string.source_VSoS_label,  R.string.source_VSoS_tooltip),
            Source("DoDk",  R.string.source_DoDk_label,  R.string.source_DoDk_tooltip),
        )),
        SourceGroup("CUSTOM", R.string.source_group_CUSTOM, listOf(
            Source("ICB", R.string.source_ICB_label, R.string.source_ICB_tooltip),
            Source("LH",  R.string.source_LH_label,  R.string.source_LH_tooltip),
            Source("PG",  R.string.source_PG_label,  R.string.source_PG_tooltip),
        )),
    )

    /** Все ключи источников в один Set — для дефолтного состояния фильтра. */
    val DEFAULT_SOURCES: Set<String> =
        SOURCE_GROUPS.flatMap { it.sources }.map { it.key }.toSet()

    // ─── Школы магии ───

    data class School(val key: String, @field:StringRes val labelRes: Int)

    val SCHOOLS: List<School> = listOf(
        School("ABJURATION",    R.string.school_ABJURATION),
        School("CONJURATION",   R.string.school_CONJURATION),
        School("DIVINATION",    R.string.school_DIVINATION),
        School("ENCHANTMENT",   R.string.school_ENCHANTMENT),
        School("EVOCATION",     R.string.school_EVOCATION),
        School("ILLUSION",      R.string.school_ILLUSION),
        School("NECROMANCY",    R.string.school_NECROMANCY),
        School("TRANSMUTATION", R.string.school_TRANSMUTATION),
    )

    // ─── Спасброски ───

    /**
     * В нормализованном JSON `savingThrows` хранится текстом из HTML
     * («Мудрости», «Ловкости», «Силы» и т.п. — родительный падеж),
     * поэтому `key` — на русском (родительный падеж). Для UI —
     * локализованное отображение через `labelRes` (именительный падеж).
     */
    data class SavingThrow(val key: String, @field:StringRes val labelRes: Int)

    val SAVING_THROWS: List<SavingThrow> = listOf(
        SavingThrow("Сила",         R.string.saving_throw_STR),
        SavingThrow("Ловкость",     R.string.saving_throw_DEX),
        SavingThrow("Телосложение", R.string.saving_throw_CON),
        SavingThrow("Интеллект",    R.string.saving_throw_INT),
        SavingThrow("Мудрости",     R.string.saving_throw_WIS),
        SavingThrow("Харизмы",      R.string.saving_throw_CHA),
    )

    // ─── Tri-state фильтр (Любое/Да/Нет) ───
    //
    // НЕ `const val`: инициализатор `R.string.xxx` — это ссылка на
    // сгенерированное поле R-класса, а **не** compile-time constant
    // в строгом смысле Kotlin. Если оставить `const`, Kotlin пытается
    // инлайнить значение в места вызова, но dex-код геттера
    // `getTRI_ANY_LABEL()` оказывается битым → `NoSuchMethodError`
    // при первом рендере TriStateRow (Ritual / Concentration в
    // FiltersBottomSheet). `val` + `@field:StringRes` даёт обычный
    // getter, который читает R-string в рантайме.
    @field:StringRes val TRI_ANY_LABEL: Int = R.string.tri_state_ANY
    @field:StringRes val TRI_YES_LABEL: Int = R.string.tri_state_YES
    @field:StringRes val TRI_NO_LABEL:  Int = R.string.tri_state_NO
}

/**
 * Флаги компонентов заклинания. Используются как multi-select в фильтре:
 *   • пустой набор — компонент не фильтруется,
 *   • выбранные флаги означают «спелл обязан иметь все выбранные»
 *     (PHB-нотация «В, С, М» → выбраны [V, S, M]).
 *
 * [spellHas] мапит флаг на конкретное булево поле [Spell].
 *
 * Метки (`labelRes`) — локализованные однобуквенные/короткие коды
 * (В/С/М + «Расх» для расходуемого). В английской локали — V/S/M +
 * «Cons» соответственно.
 */
enum class ComponentFlag(@field:StringRes val labelRes: Int, val isConsumed: Boolean = false) {
    V(R.string.component_V_label),                  // вербальный
    S(R.string.component_S_label),                  // соматический
    M(R.string.component_M_label),                  // материальный
    RC(R.string.component_RC_label, isConsumed = true);  // расходуемый

    companion object {
        fun spellHas(flag: ComponentFlag, spell: Spell): Boolean = when (flag) {
            V  -> spell.componentV
            S  -> spell.componentS
            M  -> spell.componentM
            RC -> spell.materialConsumed
        }
    }
}
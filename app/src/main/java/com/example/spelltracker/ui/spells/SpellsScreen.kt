package com.example.spelltracker.ui.spells

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spelltracker.data.ComponentFlag
import com.example.spelltracker.data.Spell
import com.example.spelltracker.data.SpellMenuConfig
import com.example.spelltracker.data.TriState
import com.example.spelltracker.ui.theme.AppColors

/**
 * Экран списка заклинаний (v2.1 — фильтры переработаны).
 *
 * Что изменилось в v2.1 (после UX-теста):
 *   • Расы убраны из фильтра (перегружали BottomSheet, не давали
 *     осмысленного сужения списка).
 *   • Компоненты теперь ОДИН multi-select row (В / С / М / Расх) —
 *     4 отдельных TriState-строки слились в один с AND-семантикой.
 *   • Подклассы — компактный checkbox-list (вместо clunky FlowRow из
 *     110+ чипов). Каждая строка: чекбокс + название, всё плотно.
 *
 * ВАЖНО: внутри @Composable циклы по коллекциям — `for ... in ...`.
 * `forEach { }` лямбда — НЕ @Composable, вызов Composable внутри неё
 * вызовет ошибку компиляции.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SpellsScreen(
    viewModel: SpellsViewModel,
    onBack: () -> Unit,
    onOpenSpell: (Long) -> Unit,
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    val n = state.visibleSpells.size
                    Text(
                        text = "Заклинания ($n)",
                        color = AppColors.TextWhite,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "Назад",
                            tint = AppColors.TextWhite,
                        )
                    }
                },
                actions = {
                    PreparedToggleAction(
                        showPreparedOnly = state.showPreparedOnly,
                        preparedCount = state.preparedCount,
                        onToggle = viewModel::togglePreparedOnly,
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = AppColors.BgPurpleDeep,
                ),
            )
        },
        containerColor = AppColors.BgDark,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(AppColors.BgDark),
        ) {
            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppColors.Gold)
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    SearchField(
                        value = state.filters.search,
                        onChange = viewModel::setSearch,
                    )
                    FilterButton(
                        activeCount = activeFilterCount(state),
                        onClick = { viewModel.setShowFiltersSheet(true) },
                    )
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(
                            start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.visibleSpells, key = { it.id }) { spell ->
                            SpellRow(
                                spell = spell,
                                isPrepared = state.preparedIds.contains(spell.id),
                                onToggle = { viewModel.togglePrepared(spell.id) },
                                onClick = { onOpenSpell(spell.id) },
                            )
                        }
                        if (state.visibleSpells.isEmpty()) {
                            item {
                                val msg = when {
                                    state.showPreparedOnly && state.preparedCount == 0 ->
                                        "Отметьте заклинания галочкой в общем списке, чтобы они появились здесь."
                                    state.showPreparedOnly ->
                                        "Нет подготовленных заклинаний под выбранные фильтры."
                                    else ->
                                        "Ничего не найдено по выбранным фильтрам."
                                }
                                Text(
                                    msg,
                                    color = AppColors.TextGrey,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(16.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.showFiltersSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.setShowFiltersSheet(false) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            containerColor = AppColors.BgDark,
            contentColor = AppColors.TextWhite,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            scrimColor = Color.Black.copy(alpha = 0.55f),
        ) {
            FiltersBottomSheet(
                state = state,
                vm = viewModel,
                onClose = { viewModel.setShowFiltersSheet(false) },
            )
        }
    }
}

private fun activeFilterCount(s: SpellsState): Int {
    var n = 0
    with(s.filters) {
        if (level != null) n++
        if (classIds.isNotEmpty()) n++
        if (subclassNames.isNotEmpty()) n++
        if (sources.isNotEmpty()) n++
        if (schools.isNotEmpty()) n++
        if (savingThrows.isNotEmpty()) n++
        if (ritual != TriState.ANY) n++
        if (concentration != TriState.ANY) n++
        if (requiredComponents.isNotEmpty()) n++
    }
    if (s.showPreparedOnly) n++
    return n
}

// =============================================================
// Top-bar bookmark
// =============================================================

@Composable
private fun PreparedToggleAction(
    showPreparedOnly: Boolean,
    preparedCount: Int,
    onToggle: () -> Unit,
) {
    val tint = if (showPreparedOnly) AppColors.Gold else AppColors.TextWhite
    IconButton(onClick = onToggle) {
        BadgedBox(
            badge = {
                if (preparedCount > 0) {
                    Badge(
                        containerColor = AppColors.Gold,
                        contentColor = AppColors.BgDark,
                    ) { Text(preparedCount.toString()) }
                }
            }
        ) {
            Icon(
                imageVector = if (showPreparedOnly) Icons.Filled.Bookmark
                else Icons.Filled.BookmarkBorder,
                contentDescription = if (showPreparedOnly) "Показать все" else "Только подготовленные",
                tint = tint,
            )
        }
    }
}

// =============================================================
// Кнопка «Фильтры»
// =============================================================

@Composable
private fun FilterButton(activeCount: Int, onClick: () -> Unit) {
    val hasAny = activeCount > 0
    val borderColor = if (hasAny) AppColors.GoldDeep else AppColors.Outline
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(AppColors.CardBg)
                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.FilterList,
                contentDescription = null,
                tint = AppColors.Gold,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.size(10.dp))
            Text(
                text = "Фильтры",
                color = AppColors.TextWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            if (hasAny) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(AppColors.Gold),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        activeCount.toString(),
                        color = AppColors.BgDark,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.size(8.dp))
            }
            Text("▾", color = AppColors.TextGrey, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// =============================================================
// Bottom Sheet content
// =============================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FiltersBottomSheet(
    state: SpellsState,
    vm: SpellsViewModel,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        // Заголовок
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Фильтры",
                color = AppColors.TextWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, "Закрыть", tint = AppColors.TextWhite)
            }
        }

        // ─── Источник ───
        SectionTitle("Источник")
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            val allSelected = state.filters.sources.isEmpty()
            FilterChip(
                text = "Все источники",
                selected = allSelected,
                onClick = { vm.setAllSources(true) },
            )
            Spacer(Modifier.size(8.dp))
            for (group in SpellMenuConfig.SOURCE_GROUPS) {
                Text(
                    text = group.name,
                    color = AppColors.TextGrey,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    for (src in group.sources) {
                        val available = state.availableSources.contains(src.key)
                        if (!available) continue
                        FilterChip(
                            text = src.label,
                            selected = state.filters.sources.contains(src.key),
                            onClick = { vm.toggleSource(src.key) },
                        )
                    }
                }
            }
        }

        Divider()

        // ─── Класс ───
        SectionTitle("Класс")
        FlowRow(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FilterChip(
                text = "Все",
                selected = state.filters.classIds.isEmpty(),
                onClick = { vm.clearClassFilter() },
            )
            for (info in state.classes) {
                val hasAnySpell = state.allSpells.any { it.classes.contains(info.id) }
                if (!hasAnySpell) continue
                FilterChip(
                    text = info.name,
                    selected = state.filters.classIds.contains(info.id),
                    onClick = { vm.toggleClass(info.id) },
                )
            }
        }

        Divider()

        // ─── Уровень ───
        SectionTitle("Уровень")
        FlowRow(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FilterChip(
                text = "Все",
                selected = state.filters.level == null,
                onClick = { vm.clearLevelFilter() },
            )
            for (lvl in 0..9) {
                if (!state.availableLevels.contains(lvl)) continue
                FilterChip(
                    text = levelLabel(lvl),
                    selected = state.filters.level == lvl,
                    onClick = { vm.setLevel(lvl) },
                )
            }
        }

        Divider()

        // ─── Школа ───
        SectionTitle("Школа")
        FlowRow(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FilterChip(
                text = "Все",
                selected = state.filters.schools.isEmpty(),
                onClick = {
                    if (state.filters.schools.isNotEmpty()) {
                        for (s in SpellMenuConfig.SCHOOLS) vm.toggleSchool(s.key)
                    }
                },
            )
            for (s in SpellMenuConfig.SCHOOLS) {
                if (!state.availableSchools.contains(s.key)) continue
                FilterChip(
                    text = s.label,
                    selected = state.filters.schools.contains(s.key),
                    onClick = { vm.toggleSchool(s.key) },
                )
            }
        }

        Divider()

        // ─── Подкласс (показывается только когда выбран хотя бы один класс) ───
        val displayedSubs = vm.displayedSubclasses
        if (displayedSubs.isNotEmpty()) {
            SectionTitle("Подкласс (${displayedSubs.size})")
            // "Все" — отдельной строкой над списком
            CheckListRow(
                label = "Все подклассы",
                selected = state.filters.subclassNames.isEmpty(),
                onToggle = {
                    if (state.filters.subclassNames.isNotEmpty()) {
                        for (name in state.filters.subclassNames) vm.toggleSubclass(name)
                    }
                },
            )
            HorizontalDivider(color = AppColors.Outline.copy(alpha = 0.3f))
            val sortedSubs = displayedSubs.sorted()
            for (name in sortedSubs) {
                CheckListRow(
                    label = name,
                    selected = state.filters.subclassNames.contains(name),
                    onToggle = { vm.toggleSubclass(name) },
                )
            }
            Divider()
        }

        // ─── Ритуал / Концентрация (3-state) ───
        TriStateRow("Ритуал", state.filters.ritual, vm::setRitual)
        Divider()
        TriStateRow("Концентрация", state.filters.concentration, vm::setConcentration)
        Divider()

        // ─── Компоненты (multi-select, AND-семантика) ───
        SectionTitle("Компоненты (выбраны — обязательны у спелла)")
        FlowRow(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            for (flag in ComponentFlag.values()) {
                FilterChip(
                    text = flag.label,
                    selected = state.filters.requiredComponents.contains(flag),
                    onClick = { vm.toggleComponent(flag) },
                )
            }
        }
        Divider()

        // ─── Спасбросок ───
        if (state.availableSavingThrows.isNotEmpty()) {
            SectionTitle("Спасбросок")
            FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FilterChip(
                    text = "Любой",
                    selected = state.filters.savingThrows.isEmpty(),
                    onClick = {
                        if (state.filters.savingThrows.isNotEmpty()) {
                            for (st in state.filters.savingThrows) vm.toggleSavingThrow(st)
                        }
                    },
                )
                for (st in state.availableSavingThrows.sorted()) {
                    FilterChip(
                        text = st,
                        selected = state.filters.savingThrows.contains(st),
                        onClick = { vm.toggleSavingThrow(st) },
                    )
                }
            }
            Divider()
        }

        // ─── Сбросить ───
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.CardBg)
                    .border(1.dp, AppColors.Outline, RoundedCornerShape(12.dp))
                    .clickable(onClick = { vm.resetFilters() })
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    tint = AppColors.Gold,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.size(10.dp))
                Text(
                    "Сбросить фильтры",
                    color = AppColors.TextWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

// =============================================================
// Reusable UI atoms
// =============================================================

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = AppColors.TextGrey,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun Divider() {
    Spacer(Modifier.size(8.dp))
    HorizontalDivider(color = AppColors.Outline)
}

@Composable
private fun FilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) AppColors.Gold else AppColors.CardBg
    val fg = if (selected) AppColors.BgDark else AppColors.TextWhite
    val border = if (selected) AppColors.Gold else AppColors.Outline
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Плотная строка чекбокс + лейбл. Используется для подклассов (110+ имён)
 * и других длинных вертикальных списков — в разы компактнее FlowRow-чипов.
 */
@Composable
private fun CheckListRow(label: String, selected: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = selected,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = AppColors.Gold,
                uncheckedColor = AppColors.Outline,
                checkmarkColor = AppColors.BgDark,
            ),
        )
        Text(
            label,
            color = AppColors.TextWhite,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f).padding(start = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TriStateRow(label: String, value: TriState, onChange: (TriState) -> Unit) {
    Column {
        SectionTitle(label)
        FlowRow(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FilterChip(text = SpellMenuConfig.TRI_ANY_LABEL, selected = value == TriState.ANY) { onChange(TriState.ANY) }
            FilterChip(text = SpellMenuConfig.TRI_YES_LABEL, selected = value == TriState.YES) { onChange(TriState.YES) }
            FilterChip(text = SpellMenuConfig.TRI_NO_LABEL,  selected = value == TriState.NO)  { onChange(TriState.NO) }
        }
    }
}

// =============================================================
// Search bar + spell row + level badge
// =============================================================

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text("Поиск по названию", color = AppColors.TextGrey) },
        leadingIcon = { Icon(Icons.Filled.Search, null, tint = AppColors.Gold) },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = AppColors.TextWhite,
            unfocusedTextColor = AppColors.TextWhite,
            focusedBorderColor = AppColors.PurpleLight,
            unfocusedBorderColor = AppColors.Outline,
            cursorColor = AppColors.Gold,
        ),
    )
}

@Composable
private fun SpellRow(
    spell: Spell,
    isPrepared: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.CardBg)
            .border(1.dp, AppColors.Outline, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = isPrepared,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = AppColors.Gold,
                uncheckedColor = AppColors.Outline,
                checkmarkColor = AppColors.BgDark,
            ),
        )
        Spacer(Modifier.size(6.dp))
        LevelBadge(spell.level)
        Spacer(Modifier.size(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                spell.name,
                color = AppColors.TextWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val subtitle = subtitleFor(spell)
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    color = AppColors.TextGrey,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (isPrepared) {
            Icon(Icons.Filled.Check, null, tint = AppColors.Gold, modifier = Modifier.size(18.dp))
        }
    }
}

private fun subtitleFor(spell: Spell): String {
    val schoolLabel = SpellMenuConfig.SCHOOLS
        .firstOrNull { it.key == spell.school }?.label ?: spell.school
    val flag = if (spell.ritual) " · ритуал" else ""
    return "$schoolLabel$flag"
}

@Composable
private fun LevelBadge(level: Int) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(AppColors.PurpleDeep),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (level == 0) "0" else romanLevel(level),
            color = AppColors.Gold,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

// =============================================================
// helpers
// =============================================================

private fun levelLabel(level: Int): String = when (level) {
    0 -> "Заговор"
    else -> romanLevel(level)
}

private fun romanLevel(n: Int): String = when (n) {
    1 -> "I"; 2 -> "II"; 3 -> "III"; 4 -> "IV"
    5 -> "V"; 6 -> "VI"; 7 -> "VII"; 8 -> "VIII"; 9 -> "IX"
    else -> n.toString()
}

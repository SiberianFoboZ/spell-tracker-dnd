package com.example.spelltracker.ui.spells

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
import com.example.spelltracker.data.Classes
import com.example.spelltracker.data.Spell
import com.example.spelltracker.ui.theme.AppColors

/**
 * Экран списка заклинаний.
 *
 *  ┌──────────────────────────────────┐
 *  │ ←  Заклинания                    │  ← TopAppBar
 *  │ [🔍 Поиск по названию        ]   │
 *  │ Класс | Уровень                  │  ← переключатель режима
 *  │ [Все] [Бард] [Волшебник] …        │  ← чипы фильтра
 *  │ ────────────────────────────────  │
 *  │ ☑ Заговор 1 — Огненный снаряд    │  ← список
 *  │ ☐ Заговор 0 — Свет               │
 *  └──────────────────────────────────┘
 */
@OptIn(ExperimentalMaterial3Api::class)
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
                title = { Text("Заклинания", color = AppColors.TextWhite, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = AppColors.TextWhite)
                    }
                },
                actions = {
                    // Кнопка «Только подготовленные» со счётчиком.
                    // Активна, когда выбран этот режим; подсвечивается золотом.
                    val active = state.showPreparedOnly
                    val tint = if (active) AppColors.Gold else AppColors.TextWhite
                    IconButton(onClick = { viewModel.togglePreparedOnly() }) {
                        BadgedBox(
                            badge = {
                                if (state.preparedCount > 0) {
                                    androidx.compose.material3.Badge(
                                        containerColor = AppColors.Purple,
                                        contentColor = AppColors.TextWhite,
                                    ) { Text(state.preparedCount.toString()) }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (active) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                contentDescription = if (active) "Показать все" else "Только подготовленные",
                                tint = tint,
                            )
                        }
                    }
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
                .background(AppColors.BgDark)
        ) {
            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppColors.Gold)
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    SearchField(
                        value = state.search,
                        onChange = viewModel::setSearch,
                    )
                    ModeSwitcher(
                        mode = state.mode,
                        onMode = viewModel::setMode,
                    )
                    FilterChipsRow(state, viewModel)
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
                                        "Нет подготовленных заклинаний, подходящих под фильтры."
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
}

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
private fun ModeSwitcher(mode: FilterMode, onMode: (FilterMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ModePill("Класс",  mode == FilterMode.BY_CLASS)  { onMode(FilterMode.BY_CLASS) }
        ModePill("Уровень", mode == FilterMode.BY_LEVEL) { onMode(FilterMode.BY_LEVEL) }
    }
}

@Composable
private fun ModePill(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) AppColors.Purple else Color.Transparent
    val border = if (selected) AppColors.PurpleLight else AppColors.Outline
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            color = if (selected) AppColors.TextWhite else AppColors.TextGrey,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun FilterChipsRow(state: SpellsState, vm: SpellsViewModel) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (state.mode == FilterMode.BY_CLASS) {
            item {
                ClassChip(
                    label = "Все",
                    selected = state.selectedClassIds.isEmpty(),
                    onClick = { vm.toggleClass("") },   // сигнал «сбросить»
                )
            }
            items(state.classes, key = { it.id }) { info ->
                ClassChip(
                    label = info.name,
                    selected = state.selectedClassIds.contains(info.id),
                    onClick = { vm.toggleClass(info.id) },
                )
            }
        } else {
            // режим BY_LEVEL: чипы уровней 0..9 (по доступности)
            item {
                LevelChip(
                    label = "Все",
                    level = null,
                    selected = state.selectedLevel == null,
                    available = true,
                    onClick = { vm.setLevel(null) },
                )
            }
            val ordered = (0..9).filter { it in state.availableLevels }
            items(ordered) { lvl ->
                LevelChip(
                    label = spellLevelLabel(lvl),
                    level = lvl,
                    selected = state.selectedLevel == lvl,
                    available = true,
                    onClick = { vm.setLevel(lvl) },
                )
            }
        }
    }
}

@Composable
private fun ClassChip(label: String, selected: Boolean, onClick: () -> Unit) {
    // Используем FilterChip из Material 3
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 12.sp) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = AppColors.CardBg,
            labelColor = AppColors.TextGrey,
            selectedContainerColor = AppColors.Purple,
            selectedLabelColor = AppColors.TextWhite,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = AppColors.Outline,
            selectedBorderColor = AppColors.PurpleLight,
        ),
    )
}

@Composable
private fun LevelChip(
    label: String,
    level: Int?,
    selected: Boolean,
    available: Boolean,
    onClick: () -> Unit,
) {
    val bg = when {
        selected -> AppColors.Gold
        available -> AppColors.CardBg
        else -> AppColors.BgDark
    }
    val fg = when {
        selected -> AppColors.BgDark
        available -> AppColors.TextGrey
        else -> AppColors.TextGreyDark
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable(enabled = available, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(label, color = fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun spellLevelLabel(level: Int): String = when (level) {
    0 -> "Заговор"
    else -> romanLevel(level)
}

private fun romanLevel(n: Int): String = when (n) {
    1 -> "I"; 2 -> "II"; 3 -> "III"; 4 -> "IV"
    5 -> "V"; 6 -> "VI"; 7 -> "VII"; 8 -> "VIII"; 9 -> "IX"
    else -> n.toString()
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
            if (spell.school.isNotBlank()) {
                Text(
                    spell.school,
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

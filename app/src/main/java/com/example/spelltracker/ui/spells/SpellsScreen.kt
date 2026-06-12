package com.example.spelltracker.ui.spells

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.launch

/**
 * Экран списка заклинаний с Material 3 Bottom Sheet для фильтров.
 *
 * Главная кнопка "Класс" открывает [FilterBottomSheetContent] —
 * Bottom Sheet с чекбоксами классов, чипами уровней и кнопкой
 * "Сбросить фильтры". Закладка "Только подготовленные" осталась
 * в тулбаре как отдельный быстрый toggle.
 *
 *  ┌──────────────────────────────────┐
 *  │ ←  Заклинания              🔖   │  ← TopAppBar
 *  │ [🔍 Поиск по названию        ]   │
 *  │ [≡ Фильтр ▾]                     │  ← одна кнопка-фильтр
 *  │ ────────────────────────────────  │
 *  │ ☑ Заговор 1 — Огненный снаряд    │  ← список
 *  │ ☐ Заговор 0 — Свет               │
 *  └──────────────────────────────────┘
 *
 *  Bottom Sheet при нажатии на "≡ Фильтр ▾":
 *  ┌──────────────────────────────────┐
 *  │ ─── (drag handle) ───            │
 *  │ Фильтры                     ✕   │
 *  │ ─────────────────────────────    │
 *  │ Класс                            │
 *  │ ☑ Все классы                     │
 *  │ ☐ Бард                           │
 *  │ ☐ Волшебник                      │
 *  │ ☐ Друид                          │
 *  │ ☐ Жрец                           │
 *  │ ...                              │
 *  │ ─────────────────────────────    │
 *  │ Уровень                          │
 *  │ [Все] [Заговор] [I] [II] [III]…  │
 *  │ ─────────────────────────────    │
 *  │ [↻ Сбросить фильтры]             │
 *  └──────────────────────────────────┘
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SpellsScreen(
    viewModel: SpellsViewModel,
    onBack: () -> Unit,
    onOpenSpell: (Long) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Заклинания",
                        color = AppColors.TextWhite,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
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
                        value = state.search,
                        onChange = viewModel::setSearch,
                    )
                    FilterButton(
                        selectedClassCount = state.selectedClassIds.size,
                        hasLevel = state.selectedLevel != null,
                        onClick = { showFilterSheet = true },
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

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            containerColor = AppColors.BgPurpleDeep,
            contentColor = AppColors.TextWhite,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            scrimColor = Color.Black.copy(alpha = 0.55f),
        ) {
            FilterBottomSheetContent(
                state = state,
                onToggleClass = viewModel::toggleClass,
                onClearClasses = viewModel::clearClassFilter,
                onSelectLevel = viewModel::setLevel,
                onClearLevel = viewModel::clearLevelFilter,
                onReset = viewModel::resetFilters,
                onClose = {
                    scope.launch {
                        sheetState.hide()
                        showFilterSheet = false
                    }
                },
            )
        }
    }
}

// =============================================================
// Top-bar bookmark (иконка закладки)
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
                imageVector = if (showPreparedOnly) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                contentDescription = if (showPreparedOnly) "Показать все" else "Только подготовленные",
                tint = tint,
            )
        }
    }
}

// =============================================================
// Top-level filter button (открывает Bottom Sheet)
// =============================================================

@Composable
private fun FilterButton(
    selectedClassCount: Int,
    hasLevel: Boolean,
    onClick: () -> Unit,
) {
    val hasAnyFilter = selectedClassCount > 0 || hasLevel
    // Активная кнопка фильтра подсвечивается золотом: более насыщенная
    // обводка (GoldDeep) + всегда золотая иконка. Неактивная — обычный
    // outline и всё равно золотая иконка (брендовый цвет).
    val borderColor = if (hasAnyFilter) AppColors.GoldDeep else AppColors.Outline
    val iconTint = AppColors.Gold
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
                tint = iconTint,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.size(10.dp))
            Text(
                text = "Класс",
                color = AppColors.TextWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            // Бейдж с числом выбранных классов (только если > 1).
            if (selectedClassCount > 1) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(AppColors.Gold),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        selectedClassCount.toString(),
                        color = AppColors.BgDark,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.size(8.dp))
            }
            Text(
                "▾",
                color = AppColors.TextGrey,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

// =============================================================
// Bottom Sheet content: фильтры классов и уровней
// =============================================================

@Composable
private fun FilterBottomSheetContent(
    state: SpellsState,
    onToggleClass: (String) -> Unit,
    onClearClasses: () -> Unit,
    onSelectLevel: (Int?) -> Unit,
    onClearLevel: () -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit,
) {
    val allChecked = state.selectedClassIds.isEmpty()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        // Заголовок + кнопка закрыть
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Фильтры",
                color = AppColors.TextWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Закрыть",
                    tint = AppColors.TextGrey,
                )
            }
        }

        HorizontalDivider(color = AppColors.Outline)

        // Секция: Класс
        SectionTitle("Класс")
        // Чекбокс "Все классы" — клик очищает выбор конкретных классов.
        ClassCheckRow(
            label = "Все классы",
            selected = allChecked,
            onCheckedChange = { wantChecked ->
                if (wantChecked && !allChecked) onClearClasses()
                // Когда уже выбрано "Все" — клик игнорируется (снимать
                // нечего; снять можно только выбрав хотя бы один класс).
            },
        )
        state.classes.forEach { info ->
            val checked = state.selectedClassIds.contains(info.id)
            ClassCheckRow(
                label = info.name,
                selected = checked,
                onCheckedChange = { _ -> onToggleClass(info.id) },
            )
        }

        HorizontalDivider(
            color = AppColors.Outline,
            modifier = Modifier.padding(top = 8.dp),
        )

        // Секция: Уровень
        SectionTitle("Уровень")
        LevelChipsFlow(
            availableLevels = state.availableLevels,
            selected = state.selectedLevel,
            onSelect = { lvl ->
                if (lvl == null) onClearLevel() else onSelectLevel(lvl)
            },
        )

        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = AppColors.Outline)

        // Кнопка "Сбросить фильтры" внизу
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.CardBg)
                    .border(1.dp, AppColors.Outline, RoundedCornerShape(12.dp))
                    .clickable(onClick = onReset)
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
private fun ClassCheckRow(
    label: String,
    selected: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!selected) }
            .padding(start = 8.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = selected,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = AppColors.Gold,
                uncheckedColor = AppColors.Outline,
                checkmarkColor = AppColors.BgDark,
            ),
        )
        Spacer(Modifier.size(8.dp))
        Text(
            label,
            color = AppColors.TextWhite,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun LevelChipsFlow(
    @Suppress("UNUSED_PARAMETER") availableLevels: Set<Int>,
    selected: Int?,
    onSelect: (Int?) -> Unit,
) {
    // Горизонтальный скролл: все 11 чипов (Все + Заговор + I-IX) всегда
    // отображаются, даже если не помещаются в одну строку. Узкие экраны
    // получают плавный horizontal-scroll, широкие — всё на одном ряду.
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
    ) {
        item(key = "all") {
            LevelChipMini(
                label = "Все",
                selected = selected == null,
                onClick = { onSelect(null) },
            )
        }
        // Показываем все 0-9 независимо от наличия данных в БД: после
        // реимпорта spells.db версией 3 там есть заклинания 0-9. Если в
        // каком-то уровне данных нет, фильтр просто даст пустой результат
        // и пользователь увидит «Ничего не найдено».
        // Показываем все 0-9 (10 чипов). Данные для уровней 6-9 уже
        // есть в assets/spells.csv (122 заклинания суммарно), и version=3
        // в SpellDatabase гарантирует реимпорт при первом запуске.
        (0..9).forEach { lvl ->
            item(key = "lv-$lvl") {
                LevelChipMini(
                    label = spellLevelLabel(lvl),
                    selected = selected == lvl,
                    onClick = { onSelect(lvl) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LevelChipMini(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    // Material 3 FilterChip с фиолетовой палитрой — единый стиль для
    // «Все», «Заговор» и I-IX. Не выбран: тёмный фон + серый текст.
    // Выбран: фиолетовая заливка + светлый текст + фиолетовая рамка.
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        },
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

// =============================================================
// Поле поиска
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

// =============================================================
// Строка заклинания в списке
// =============================================================

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

// =============================================================
// Утилиты
// =============================================================

private fun spellLevelLabel(level: Int): String = when (level) {
    0 -> "Заговор"
    else -> romanLevel(level)
}

private fun romanLevel(n: Int): String = when (n) {
    1 -> "I"; 2 -> "II"; 3 -> "III"; 4 -> "IV"
    5 -> "V"; 6 -> "VI"; 7 -> "VII"; 8 -> "VIII"; 9 -> "IX"
    else -> n.toString()
}

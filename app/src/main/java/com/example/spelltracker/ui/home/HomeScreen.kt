package com.example.spelltracker.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spelltracker.data.Classes
import com.example.spelltracker.ui.theme.AppColors
import kotlinx.coroutines.flow.collectLatest

/**
 * Главный экран (Spell Tracker). Содержит:
 *   - Заголовок
 *   - Панель эффективного caster level (большая золотая цифра)
 *   - 3×3 сетку карточек классов с полем ввода уровня
 *   - Секцию пакт-магии (показывается, если warlock > 0) — кнопки +/-
 *   - Секцию «Ячейки заклинаний» — кликабельные золотые блоки (Этап 15)
 *   - Нижнюю панель: «Короткий отдых» (Outlined) + «Длинный отдых» (Filled)
 *   - FAB → переход на экран «Заклинания»
 *
 * Состояния диалогов/уведомлений:
 *   - [restDialog]: подтверждение короткого/длинного отдыха (AlertDialog)
 *   - [snackbarHostState]: «Ячейки Колдуна восстановлены» / «Все ячейки
 *     восстановлены» (Material 3 Snackbar, single-flight через collectLatest)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenSpells: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var restDialog by remember { mutableStateOf<RestKind?>(null) }

    // Snackbar события от VM. collectLatest отменяет предыдущий
    // showSnackbar при поступлении нового — гарантирует, что всегда
    // виден самый свежий результат.
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            val msg = when (event) {
                HomeEvent.ShortRest -> "Ячейки Колдуна восстановлены"
                HomeEvent.LongRest  -> "Все ячейки восстановлены"
            }
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = AppColors.CardBgLighter,
                    contentColor   = AppColors.Gold,
                )
            }
        },
        bottomBar = {
            RestButtonsBar(
                onShortRest = { restDialog = RestKind.Short },
                onLongRest  = { restDialog = RestKind.Long },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenSpells,
                containerColor = AppColors.Gold,
                contentColor   = AppColors.BgDark,
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(
                    Icons.Filled.AutoStories,
                    contentDescription = "Открыть список заклинаний",
                )
            }
        },
        containerColor = Color.Transparent,    // фон рисуем сами радиальным градиентом
        contentColor   = AppColors.TextWhite,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(AppColors.ScreenGradient)
                .windowInsetsPadding(WindowInsets.statusBars),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp,
                        top = 8.dp, bottom = 88.dp,   // bottom space под FAB
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item { Header() }
                    item { EffectiveLevelPanel(state.casterLevel) }
                    item { ClassesGrid(viewModel) }

                    if (state.warlockLevel > 0) {
                        item {
                            PactMagicRow(
                                state = state,
                                onUse = viewModel::usePactSlot,
                                onRestore = viewModel::restorePactSlot,
                            )
                        }
                    }

                    item { SpellSlotsSection(state, viewModel) }
                }
            }
        }
    }

    // ─────────── Диалог подтверждения отдыха ───────────
    restDialog?.let { kind ->
        val isShort = kind == RestKind.Short
        AlertDialog(
            onDismissRequest = { restDialog = null },
            title = { Text(if (isShort) "Короткий отдых?" else "Длинный отдых?") },
            text = {
                Text(
                    if (isShort)
                        "Восстановить ячейки пакт-магии Колдуна. Ячейки других классов останутся потраченными."
                    else
                        "Восстановить все ячейки заклинаний и пакт-магии. Классы сохранятся."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (isShort) viewModel.shortRest() else viewModel.longRest()
                    restDialog = null
                }) {
                    Text(
                        if (isShort) "Короткий отдых" else "Длинный отдых",
                        color = AppColors.Gold,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { restDialog = null }) {
                    Text("Отмена", color = AppColors.TextGrey)
                }
            },
            containerColor   = AppColors.CardBg,
            titleContentColor = AppColors.TextWhite,
            textContentColor  = AppColors.TextGrey,
        )
    }
}

/** Тип подтверждаемого отдыха (для диалога). */
private enum class RestKind { Short, Long }

// =============================================================
// Заголовок
// =============================================================

@Composable
private fun Header() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Spell Tracker",
            color = AppColors.TextWhite,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Мультикласс по правилам PHB",
            color = AppColors.TextGrey,
            fontSize = 13.sp,
        )
    }
}

// =============================================================
// Панель caster level
// =============================================================

@Composable
private fun EffectiveLevelPanel(casterLevel: Int) {
    val animated by animateIntAsState(
        targetValue = casterLevel,
        animationSpec = tween(durationMillis = 350),
        label = "casterLevel",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(AppColors.Purple, AppColors.PurpleDeep)))
            .padding(vertical = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "УРОВЕНЬ ЗАКЛИНАТЕЛЯ",
                color = AppColors.Cream,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = animated.toString(),
                color = AppColors.Gold,
                fontSize = 64.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

// =============================================================
// Сетка классов (3×3) — без изменений из v2.1.x
// =============================================================

@Composable
private fun ClassesGrid(viewModel: HomeViewModel) {
    Column {
        SectionTitle("Классы")
        Spacer(Modifier.height(10.dp))
        val rows = viewModel.classes().chunked(3)
        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { info ->
                    ClassCard(
                        info = info,
                        level = viewModel.state.value.classLevels[info.id] ?: 0,
                        onLevelChange = { viewModel.setClassLevel(info.id, it) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ClassCard(
    info: Classes.Info,
    level: Int,
    onLevelChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasCasting = info.factor > 0.0
    val isMulticlass = level > 0 && hasCasting
    val bg = if (isMulticlass) AppColors.CardBgLighter else AppColors.CardBg
    Box(
        modifier = modifier
            .heightIn(min = 92.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(
                width = if (isMulticlass) 1.5.dp else 1.dp,
                color = if (isMulticlass) AppColors.PurpleLight else AppColors.Outline,
                shape = RoundedCornerShape(14.dp),
            )
            .padding(horizontal = 8.dp, vertical = 10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = info.name,
                color = AppColors.TextWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (hasCasting) "×${formatFactor(info)}" else "пзак",
                color = if (hasCasting) AppColors.Gold else AppColors.TextGreyDark,
                fontSize = 10.sp,
            )
            LevelInput(
                level = level,
                onLevelChange = onLevelChange,
            )
        }
    }
}

@Composable
private fun LevelInput(
    level: Int,
    onLevelChange: (Int) -> Unit,
) {
    var text by remember { mutableStateOf(level.toString()) }
    LaunchedEffect(level) {
        if (text.toIntOrNull() != level) text = level.toString()
    }
    BasicTextField(
        value = text,
        onValueChange = { raw ->
            val filtered = raw.filter { it.isDigit() }.take(2)
            text = filtered
            val v = filtered.toIntOrNull() ?: 0
            onLevelChange(v.coerceIn(0, 20))
        },
        singleLine = true,
        textStyle = TextStyle(
            color = if (level > 0) AppColors.Gold else AppColors.TextGrey,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        ),
        cursorBrush = SolidColor(AppColors.Gold),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp),
        decorationBox = { inner ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .background(
                        color = AppColors.BgDark.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) { inner() }
        }
    )
}

private fun formatFactor(info: Classes.Info): String = when {
    info.factor >= 1.0 -> "1.0"
    info.roundUp -> "0.5↑"
    else -> "0.5"
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        color = AppColors.PurpleLight,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
    )
}

// =============================================================
// Пакт-магия (Колдун) — кнопки +/-, как в v2.1.x
// =============================================================

@Composable
private fun PactMagicRow(
    state: HomeState,
    onUse: () -> Unit,
    onRestore: () -> Unit,
) {
    Column {
        SectionTitle("Пакт-магия")
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(AppColors.BgDark)
                .border(1.dp, AppColors.GoldDeep, RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Колдун: ${state.warlockLevel}",
                        color = AppColors.TextGrey, fontSize = 12.sp,
                    )
                    Text(
                        "${state.pactUsed} / ${state.pactSlots} ячеек ${state.pactSlotLevel}-го ур.",
                        color = AppColors.Gold,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                StepButton(icon = Icons.Filled.Remove, enabled = state.pactUsed > 0, onClick = onRestore)
                Spacer(Modifier.width(8.dp))
                StepButton(icon = Icons.Filled.Add, enabled = state.pactUsed < state.pactSlots, onClick = onUse)
            }
        }
    }
}

@Composable
private fun StepButton(
    icon: ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (enabled) AppColors.GoldDeep else AppColors.Outline),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(onClick = onClick, enabled = enabled) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) AppColors.BgDark else AppColors.TextGreyDark,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// =============================================================
// Ячейки заклинаний (Этап 15) — кликабельные золотые блоки
// =============================================================

@Composable
private fun SpellSlotsSection(state: HomeState, viewModel: HomeViewModel) {
    Column {
        SectionTitle("Ячейки заклинаний")
        Spacer(Modifier.height(10.dp))
        if (state.slots.isEmpty()) {
            Text(
                "Нет доступных ячеек — укажите уровень хотя бы одного заклинателя.",
                color = AppColors.TextGrey,
                fontSize = 12.sp,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                state.slots.forEach { slot ->
                    SpellSlotRow(
                        level = slot.level,
                        total = slot.total,
                        used = slot.used,
                        onUse = { viewModel.useSlot(slot.level) },
                        onRestore = { viewModel.restoreSlot(slot.level) },
                    )
                }
            }
        }
    }
}

/**
 * Строка одной ступени заклинаний (уровень I..IX).
 *
 * Состоит из бэйджа с римским номером и [FlowRow] из
 * [SpellSlotBlock] — по одному на каждую ячейку. FlowRow нужен
 * для адаптивности: на совсем узких экранах блоки переносятся
 * на следующую строку, а не вылезают за край.
 *
 * Цвет блока:
 *   - i < (total - used)   → gold (доступная ячейка)
 *   - i >= (total - used)  → серый (потраченная)
 *
 * Тап по gold-блоку → [onUse] (useSlot); тап по серому → [onRestore].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpellSlotRow(
    level: Int,
    total: Int,
    used: Int,
    onUse: () -> Unit,
    onRestore: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.CardBg)
            .border(1.dp, AppColors.Outline, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Бэйдж уровня (I, II, ...)
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(AppColors.PurpleDeep),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = romanLevel(level),
                color = AppColors.Gold,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(14.dp))
        // Ряд кликабельных блоков
        FlowRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(total) { i ->
                val isUsedSlot = i >= (total - used)
                SpellSlotBlock(
                    used = isUsedSlot,
                    onClick = {
                        if (isUsedSlot) onRestore() else onUse()
                    },
                )
            }
        }
    }
}

/**
 * Один кликабельный блок ячейки заклинания (Этап 15).
 *
 * Визуально:
 *   - Активная ячейка  : золотой фон + золотая рамка + лёгкая тень (3dp)
 *   - Потраченная       : серый (SlotUsed) фон + тёмная рамка, без тени
 *
 * Анимации:
 *   - `animateColorAsState`   — плавная смена цвета фона и рамки
 *   - `animateDpAsState`      — плавное появление/исчезновение тени
 *   - `animateFloatAsState`   — scale 1.0 ↔ 0.92 на нажатие (через
 *                               `collectIsPressedAsState`, без `indication`,
 *                               чтобы не было ripple поверх анимации)
 */
@Composable
private fun SpellSlotBlock(
    used: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val bg by animateColorAsState(
        targetValue = if (used) AppColors.SlotUsed else AppColors.Gold,
        animationSpec = tween(200),
        label = "slotBg",
    )
    val edge by animateColorAsState(
        targetValue = if (used) AppColors.SlotUsedEdge else AppColors.GoldDeep,
        animationSpec = tween(200),
        label = "slotEdge",
    )
    val elevation by animateDpAsState(
        targetValue = if (used) 0.dp else 3.dp,
        animationSpec = tween(200),
        label = "slotElev",
    )
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = tween(120),
        label = "slotScale",
    )

    Box(
        modifier = Modifier
            .size(48.dp)
            .scale(scale)
            .shadow(
                elevation = elevation,
                shape = RoundedCornerShape(8.dp),
                ambientColor = if (used) Color.Transparent else AppColors.Gold.copy(alpha = 0.4f),
                spotColor    = if (used) Color.Transparent else AppColors.Gold.copy(alpha = 0.4f),
            )
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, edge, RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
            ) { onClick() },
    )
}

// =============================================================
// Нижняя панель: две кнопки отдыха (Этап 15)
// =============================================================

@Composable
private fun RestButtonsBar(
    onShortRest: () -> Unit,
    onLongRest: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.BgPurpleDeep)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Короткий отдых — Outlined (рамка золотом)
        OutlinedButton(
            onClick = onShortRest,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.5.dp, AppColors.GoldDeep),
        ) {
            Icon(
                Icons.Filled.LocalCafe,
                contentDescription = null,
                tint = AppColors.Gold,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Короткий отдых",
                color = AppColors.Gold,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                fontSize = 14.sp,
            )
        }
        // Длинный отдых — Filled (залитая золотом)
        Button(
            onClick = onLongRest,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.Gold,
                contentColor   = AppColors.BgDark,
            ),
        ) {
            Icon(
                Icons.Filled.Bed,
                contentDescription = null,
                tint = AppColors.BgDark,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Длинный отдых",
                color = AppColors.BgDark,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                fontSize = 14.sp,
            )
        }
    }
}

// =============================================================
// Утилиты
// =============================================================

private fun romanLevel(n: Int): String = when (n) {
    1 -> "I"; 2 -> "II"; 3 -> "III"; 4 -> "IV"
    5 -> "V"; 6 -> "VI"; 7 -> "VII"; 8 -> "VIII"; 9 -> "IX"
    else -> n.toString()
}

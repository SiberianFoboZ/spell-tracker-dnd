package com.example.spelltracker.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
 *   - Секцию «Ячейки заклинаний» — кликабельные строки уровней (Этап 16)
 *       * Обычные классы: I..IX
 *       * Пакт-магия Колдуна: встроена в общий список (с подписью «Колдун»)
 *   - Нижнюю панель: «Короткий отдых» (Outlined) + «Длинный отдых» (Filled)
 *   - FAB → переход на экран «Заклинания»
 *
 * Логика строк (Этап 16):
 *   - Тап по **всей строке** уровня тратит первую доступную ячейку
 *     (золотой блок становится серым по порядку слева направо).
 *   - Тап по отдельному серому блоку **запрещён** — он не реактивен.
 *   - Восстановление — только через кнопки отдыха в нижней панели.
 *
 * Состояния:
 *   - [restDialog]: подтверждение короткого/длинного отдыха (AlertDialog)
 *   - [snackbarHostState]: «Ячейки Колдуна восстановлены» /
 *     «Все ячейки восстановлены» (Material 3 Snackbar)
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
                HomeEvent.ShortRest               -> "Ячейки Колдуна восстановлены"
                HomeEvent.LongRest                -> "Все ячейки восстановлены"
                HomeEvent.ArcanumShortRestBlocked ->
                    "Арканумы восстанавливаются только после длинного отдыха"
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
                    // Этап 16: пакт-магия Колдуна встроена в общий список
                    // ниже — отдельной секции «Пакт-магия» больше нет.
                    // Этап 18: пакт-магия Колдуна вынесена в отдельную
                    // секцию «МАГИЯ ДОГОВОРА» (идёт первой, если
                    // Колдун выбран). Ячейки других классов — ниже.
                    if (state.pactSlot != null) {
                        item { PactMagicSection(state, viewModel) }
                    }
                    item { SpellSlotsSection(state, viewModel) }
                    // Этап 17: арканумы Колдуна (VI..IX) — сразу после ячеек.
                    // Секция условно рендерит строки по warlockLevel:
                    //   < 11         → «Доступно с 11 уровня»
                    //   11+          → 1..4 строки (по уровню)
                    item { ArcanumsSection(state, viewModel) }
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
                        "Восстановить ячейки пакт-магии Колдуна. Ячейки других классов и арканумы останутся потраченными."
                    else
                        "Восстановить все ячейки заклинаний, пакт-магии и арканумы. Классы сохранятся."
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
// Ячейки заклинаний (Этап 16) — кликабельные строки уровней
// (включая унифицированную пакт-магию Колдуна)
// =============================================================

// =============================================================
// Пакт-магия Колдуна (Этап 18)
// =============================================================
//
// «МАГИЯ ДОГОВОРА» — отдельная секция в HomeScreen, идёт **перед**
// «Ячейки заклинаний». Видна только если [HomeState.pactSlot] != null,
// т.е. warlockLevel > 0 и cap ячеек на этом уровне > 0.
//
// Визуально идентична обычной строке ячеек (бейдж + блоки), но:
//   - бейдж чуть светлее (BgMid) — маркер «это пакт»
//   - под бейджем подпись «Колдун»
//   - тап по строке → onPactRowClick() → storage.usePactSlot()
//   - восстанавливается на **коротком** отдыхе
//
// Намеренно НЕ используем SpellSlotRow: у пакт-ряда своя подпись и
// своё событие клика. Геометрия/анимация скопированы.

@Composable
private fun PactMagicSection(state: HomeState, viewModel: HomeViewModel) {
    val pact = state.pactSlot ?: return
    Column {
        SectionTitle("Магия договора")
        Spacer(Modifier.height(10.dp))
        PactMagicRow(
            slot = pact,
            onRowClick = { viewModel.onPactRowClick(pact) },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PactMagicRow(
    slot: SlotInfo,
    onRowClick: () -> Unit,
) {
    val isAllSpent = slot.used >= slot.total
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.CardBg)
            .border(
                width = 1.dp,
                color = if (isAllSpent) AppColors.TextGreyDark else AppColors.Outline,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(enabled = !isAllSpent) { onRowClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Бейдж уровня + подпись «Колдун»
            Column(
                modifier = Modifier.width(56.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppColors.BgMid),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = romanLevel(slot.level),
                        color = AppColors.Gold,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Колдун",
                    color = AppColors.TextGrey,
                    fontSize = 9.sp,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.width(12.dp))
            FlowRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                repeat(slot.total) { i ->
                    val isUsedSlot = i >= (slot.total - slot.used)
                    SpellSlotBlock(used = isUsedSlot)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "${slot.used} / ${slot.total}",
            color = if (isAllSpent) AppColors.TextGreyDark else AppColors.TextGrey,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.End),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun SpellSlotsSection(state: HomeState, viewModel: HomeViewModel) {
    Column {
        SectionTitle("Ячейки заклинаний")
        Spacer(Modifier.height(10.dp))
        if (state.regularSlots.isEmpty()) {
            Text(
                "Нет доступных ячеек — укажите уровень хотя бы одного заклинателя.",
                color = AppColors.TextGrey,
                fontSize = 12.sp,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                state.regularSlots.forEach { slot ->
                    SpellSlotRow(
                        slot = slot,
                        onRowClick = { viewModel.onRegularRowClick(slot) },
                    )
                }
            }
        }
    }
}

/**
 * Строка одной ступени заклинаний (Этап 16).
 *
 * Содержит:
 *   - Бейдж с римским номером уровня (для Колдуна — чуть светлее
 *     оттенок фона + подпись «Колдун» под бейджем).
 *   - Ряд визуальных блоков [SpellSlotBlock] в [FlowRow]
 *     (обёрнут для адаптивности на узких экранах).
 *   - Счётчик «X / Y» под блоками (правый край).
 *
 * Кликабельна **вся строка** целиком. Тап гасит первую доступную
 * (золотую) ячейку слева направо. Когда все ячейки потрачены —
 * `clickable(enabled = false)`, рипл отключается.
 *
 * Цвет блока:
 *   - i < (total - used)   → gold (доступная)
 *   - i >= (total - used)  → серый (потраченная)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpellSlotRow(
    slot: SlotInfo,
    onRowClick: () -> Unit,
) {
    val isAllSpent = slot.used >= slot.total
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.CardBg)
            .border(
                width = 1.dp,
                color = if (isAllSpent) AppColors.TextGreyDark else AppColors.Outline,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(enabled = !isAllSpent) { onRowClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        // Верх: бейдж уровня + ряд блоков
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Бейдж уровня + опциональная подпись «Колдун»
            Column(
                modifier = Modifier.width(56.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            // Пакт-магия чуть светлее — отличаем от обычных
                            AppColors.PurpleDeep
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = romanLevel(slot.level),
                        color = AppColors.Gold,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            // Ряд визуальных блоков
            FlowRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                repeat(slot.total) { i ->
                    val isUsedSlot = i >= (slot.total - slot.used)
                    SpellSlotBlock(used = isUsedSlot)
                }
            }
        }
        // Низ: счётчик X / Y, выровнен вправо
        Spacer(Modifier.height(6.dp))
        Text(
            text = "${slot.used} / ${slot.total}",
            color = if (isAllSpent) AppColors.TextGreyDark else AppColors.TextGrey,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.End),
            textAlign = TextAlign.End,
        )
    }
}

/**
 * Один визуальный блок ячейки заклинания (Этап 16).
 *
 * В отличие от Этапа 15, блок **не** обрабатывает клики —
 * кликабельна вся [SpellSlotRow]. Блок чисто визуальный.
 *
 * Анимации:
 *   - `animateColorAsState`   — плавная смена цвета фона/рамки (300 мс)
 *   - `animateDpAsState`      — плавное появление/снятие тени (300 мс)
 *   - `Animatable` + `LaunchedEffect(used)` — короткая «вспышка»
 *     scale 1.0 → 0.92 → 1.0 при «зажигании» (только при spent=true)
 *
 * Восстановление (например, при длинном отдыхе) даёт обратный
 * плавный переход цвета/тени, но без «вспышки» — это «затухание».
 */
@Composable
private fun SpellSlotBlock(used: Boolean) {
    val scale = remember { Animatable(1f) }
    // «Вспышка» scale при смене used: false → true.
    // При обратном переходе (rest) — без анимации, чтобы блок
    // плавно «зажёгся» только через color/elevation.
    LaunchedEffect(used) {
        if (used) {
            scale.animateTo(0.92f, animationSpec = tween(140))
            scale.animateTo(1f, animationSpec = tween(180))
        } else {
            scale.snapTo(1f)
        }
    }

    val bg by animateColorAsState(
        targetValue = if (used) AppColors.SlotUsed else AppColors.Gold,
        animationSpec = tween(300),
        label = "slotBg",
    )
    val edge by animateColorAsState(
        targetValue = if (used) AppColors.SlotUsedEdge else AppColors.GoldDeep,
        animationSpec = tween(300),
        label = "slotEdge",
    )
    val elevation by animateDpAsState(
        targetValue = if (used) 0.dp else 3.dp,
        animationSpec = tween(300),
        label = "slotElev",
    )

    Box(
        modifier = Modifier
            .size(48.dp)
            .scale(scale.value)
            .shadow(
                elevation = elevation,
                shape = RoundedCornerShape(8.dp),
                ambientColor = if (used) Color.Transparent else AppColors.Gold.copy(alpha = 0.4f),
                spotColor    = if (used) Color.Transparent else AppColors.Gold.copy(alpha = 0.4f),
            )
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, edge, RoundedCornerShape(8.dp)),
    )
}

// =============================================================
// Арканумы Колдуна (Этап 17)
// =============================================================
//
// Каждый арканум — это **ровно один** золотой блок (как в обычных
// ячейках, но без счётчика «X / Y», потому что X всегда 0..1).
// По правилам PHB у Колдуна по одному аркануму уровней VI, VII, VIII,
// IX; доступ к ним открывается с 11 уровня.
//
// Секция видна всегда, но если warlockLevel < 11 — показываем
// поясняющий текст «Доступно с 11 уровня» вместо строк.
// Если warlockLevel >= 11 — рисуем 1..4 строки в зависимости от
// уровня (11..12 → VI; 13..14 → VI, VII; 15..16 → VI, VII, VIII;
// 17+ → все четыре).
//
// Восстановление — ТОЛЬКО длинный отдых. Клик по строке только
// «гасит» доступный арканум, восстановить кликом нельзя.

@Composable
private fun ArcanumsSection(state: HomeState, viewModel: HomeViewModel) {
    if (state.warlockLevel == 0) return
    Column {
        SectionTitle("Арканумы")
        Spacer(Modifier.height(10.dp))
        when {
            state.warlockLevel < 11 -> {
                Text(
                    "Доступно с 11 уровня Колдуна",
                    color = AppColors.TextGrey,
                    fontSize = 12.sp,
                )
            }
            state.arcanums.isEmpty() -> {
                Text(
                    "Нет доступных арканумов",
                    color = AppColors.TextGrey,
                    fontSize = 12.sp,
                )
            }
            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.arcanums.forEach { arcanum ->
                        ArcanumRow(
                            arcanum = arcanum,
                            onRowClick = { viewModel.onArcanumClick(arcanum) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Строка одного арканума (Этап 17). По стилю повторяет [SpellSlotRow],
 * но упрощена: всегда один блок, без счётчика «X / Y» (он избыточен
 * при total=1). Кликабельна только пока арканум не потрачен.
 */
@Composable
private fun ArcanumRow(
    arcanum: ArcanumInfo,
    onRowClick: () -> Unit,
) {
    val isSpent = arcanum.used
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.CardBg)
            .border(
                width = 1.dp,
                color = if (isSpent) AppColors.TextGreyDark else AppColors.Outline,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(enabled = !isSpent) { onRowClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Бейдж уровня арканума (тот же стиль, что у обычных ячеек).
        Column(
            modifier = Modifier.width(56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppColors.PurpleDeep),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = romanLevel(arcanum.level),
                    color = AppColors.Gold,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        // Один визуальный блок.
        SpellSlotBlock(used = isSpent)
    }
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
            border = androidx.compose.foundation.BorderStroke(1.5.dp, AppColors.GoldDeep),
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

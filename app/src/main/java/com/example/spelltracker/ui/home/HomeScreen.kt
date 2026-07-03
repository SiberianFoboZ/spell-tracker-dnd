package com.example.spelltracker.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spelltracker.R
import com.example.spelltracker.data.Classes
import com.example.spelltracker.nameRes
import com.example.spelltracker.ui.theme.AppColors
import kotlinx.coroutines.delay
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
 *   - [classesExpanded] (Этап 20): сворачивание секции «Классы».
 *     По умолчанию **false** — секция свёрнута, чтобы разгрузить
 *     главный экран (просьба пользователя «поле классы должно
 *     скрываться под slidebar»). Сохраняется через
 *     `rememberSaveable` между пересозданиями Activity.
 *   - [showAddCustomSlotSheet] (Этап 20): модалка для добавления
 *     пользовательской ячейки (см. [AddCustomSlotSheet]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenSpells: () -> Unit,
    onEditCustomSlot: (Long) -> Unit,
    onOpenCharacters: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var restDialog by remember { mutableStateOf<RestKind?>(null) }
    // Этап 22: свайп влево → открыть экран «Персонажи».
    // Аккумулируем горизонтальное смещение, при превышении порога —
    // вызываем onOpenCharacters. Свайп не мешает вертикальному
    // скроллу LazyColumn: detectHorizontalDragGestures отменяется,
    // если палец сначала идёт по вертикали.
    var accumulatedDragX by remember { mutableStateOf(0f) }
    val swipeThresholdPx = with(LocalDensity.current) { 80.dp.toPx() }
    // Этап 20: сворачивание «Классы» (по умолчанию свёрнуто).
    var classesExpanded by rememberSaveable { mutableStateOf(false) }
    // Этап 20: модалка добавления пользовательской ячейки.
    var showAddCustomSlotSheet by remember { mutableStateOf(false) }

    // Snackbar события от VM. collectLatest отменяет предыдущий
    // showSnackbar при поступлении нового — гарантирует, что всегда
    // виден самый свежий результат.
    //
    // Строки резолвим ЗДЕСЬ (в Composable-контексте) — внутри лямбды
    // LaunchedEffect вызывать stringResource нельзя.
    val shortRestMsg    = stringResource(R.string.rest_short_done)
    val longRestMsg     = stringResource(R.string.rest_long_done)
    val arcanumRestMsg  = stringResource(R.string.arcanum_blocked_short_rest)
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            val msg = when (event) {
                HomeEvent.ShortRest               -> shortRestMsg
                HomeEvent.LongRest                -> longRestMsg
                HomeEvent.ArcanumShortRestBlocked -> arcanumRestMsg
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
        topBar = {
            // Тонкая панель с одной action-кнопкой — переход в Настройки.
            // Заголовка на Home нет: «Spell Tracker» + подзаголовок
            // уже отрисованы [Header] в самом контенте, дублировать
            // в TopAppBar смысла нет.
            //
            // Фон — сплошной [AppColors.BgPurpleDeep], НЕ прозрачный.
            // Раньше был Color.Transparent, и над прозрачным Scaffold'ом
            // (containerColor=Transparent) проступал тёмный фон окна —
            // получалось «чёрное поле» с иконкой-глобусом, плохо
            // читаемое. Теперь TopAppBar визуально сливается с фоном
            // верхней части градиента.
            CenterAlignedTopAppBar(
                title = {},
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.settings_open_content_description),
                            tint = AppColors.TextWhite,
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = AppColors.BgPurpleDeep,
                    titleContentColor = AppColors.TextWhite,
                    actionIconContentColor = AppColors.TextWhite,
                ),
            )
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
                    contentDescription = stringResource(R.string.home_fab_open_spells_content_description),
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
                        .fillMaxWidth()
                        // Этап 22: свайп влево — навигация на «Персонажи».
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragStart = { accumulatedDragX = 0f },
                                onDragEnd = {
                                    if (accumulatedDragX < -swipeThresholdPx) {
                                        onOpenCharacters()
                                    }
                                    accumulatedDragX = 0f
                                },
                                onDragCancel = { accumulatedDragX = 0f },
                            ) { _, dragAmount ->
                                accumulatedDragX += dragAmount
                            }
                        },
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp,
                        top = 8.dp, bottom = 88.dp,   // bottom space под FAB
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item { Header() }
                    item { EffectiveLevelPanel(state.casterLevel) }
                    // Этап 20: секция «Классы» свёрнута по умолчанию,
                    // чтобы разгрузить главный экран. Заголовок кликабельный
                    // — раскрывает/скрывает сетку классов с анимацией.
                    item {
                        CollapsibleClassesSection(
                            viewModel = viewModel,
                            expanded = classesExpanded,
                            onToggle = { classesExpanded = !classesExpanded },
                            // Этап 20 v2: «+» в сетке классов открывает
                            // bottom sheet добавления пользовательской ячейки.
                            onAddCustomSlot = { showAddCustomSlotSheet = true },
                        )
                    }
                    if (state.pactSlot != null) {
                        item { PactMagicSection(state, viewModel) }
                    }
                    item { SpellSlotsSection(state, viewModel) }
                    // Этап 17: арканумы Колдуна (VI..IX) — сразу после ячеек.
                    // Секция условно рендерит строки по warlockLevel:
                    //   < 11         → «Доступно с 11 уровня»
                    //   11+          → 1..4 строки (по уровню)
                    item { ArcanumsSection(state, viewModel) }
                    // Этап 20: «Пользовательские ячейки» (универсальный
                    // конструктор). Идёт последним — это отдельный тип
                    // ресурса, не привязан к PHB. Кнопка добавления
                    // перенесена в [AddClassButton] внутри сетки классов
                    // (Этап 20 v2).
                    item {
                        CustomSlotsSection(
                            slots = state.customSlots,
                            onRowClick = { slot -> viewModel.onCustomRowClick(slot) },
                            onRowLongPress = { slot -> onEditCustomSlot(slot.id) },
                        )
                    }
                }
            }
        }
    }

    // ─────────── Диалог подтверждения отдыха ───────────
    restDialog?.let { kind ->
        val isShort = kind == RestKind.Short
        AlertDialog(
            onDismissRequest = { restDialog = null },
            title = {
                Text(
                    stringResource(
                        if (isShort) R.string.rest_short_confirm_title
                        else R.string.rest_long_confirm_title,
                    )
                )
            },
            text = {
                Text(
                    stringResource(
                        if (isShort) R.string.rest_short_confirm_body
                        else R.string.rest_long_confirm_body,
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (isShort) viewModel.shortRest() else viewModel.longRest()
                    restDialog = null
                }) {
                    Text(
                        stringResource(
                            if (isShort) R.string.rest_short_title
                            else R.string.rest_long_title,
                        ),
                        color = AppColors.Gold,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { restDialog = null }) {
                    Text(stringResource(R.string.common_cancel), color = AppColors.TextGrey)
                }
            },
            containerColor   = AppColors.CardBg,
            titleContentColor = AppColors.TextWhite,
            textContentColor  = AppColors.TextGrey,
        )
    }

    // ─────────── Этап 20: модалка добавления пользовательской ячейки ───────────
    if (showAddCustomSlotSheet) {
        AddCustomSlotSheet(
            onDismiss = { showAddCustomSlotSheet = false },
            onSave = { draft ->
                // id генерируем здесь, чтобы не зависеть от системного
                // времени внутри UI-слоя. draft приходит из локального
                // state в AddCustomSlotSheet (там id = 0L как заглушка).
                val withId = draft.copy(id = System.currentTimeMillis())
                viewModel.addCustomSlot(withId)
                showAddCustomSlotSheet = false
            },
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
            text = stringResource(R.string.app_name),
            color = AppColors.TextWhite,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.app_subtitle),
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
                text = stringResource(R.string.home_effective_caster_level_caption),
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
// Сетка классов (Этап 19: добавлены 2 третьекастера + кнопка «+»)
// =============================================================
//
// Было: 3×3 = 9 ячеек с фиксированным набором классов (Этап 13).
// Стало: 11 классов (9 базовых + Воин/мистический рыцарь +
// Плут/мистический ловкач) + кнопка «+» = 12 ячеек, 4×3.
//
// Логика: список ячеек `cells: List<GridCell>` собирается из
// `viewModel.classes()` + `GridCell.AddButton` и раскладывается
// `chunked(3)` по рядам. Кнопка всегда оказывается **последней**
// ячейкой сетки, потому что это последний элемент списка — независимо
// от того, сколько классов в ALL на текущий момент.

/** Ячейка сетки классов: либо карточка существующего класса, либо кнопка «+». */
private sealed interface GridCell {
    data class Class(val info: Classes.Info) : GridCell
    /** Этап 19: кнопка добавления нового класса (функционал появится в Этапе 20). */
    data object AddButton : GridCell
}

@Composable
private fun ClassesGrid(
    viewModel: HomeViewModel,
    onAddCustomSlot: () -> Unit,
) {
    // Этап 20 v2: SectionTitle("Классы") удалён — он дублировал
    // заголовок CollapsibleClassesSection. Spacer(10.dp) тоже не нужен,
    // его рисует родительская обёртка (AnimatedVisibility-блок).
    Column {
        // Этап 19: добавляем GridCell.AddButton в конец — это и есть
        // «последняя ячейка» сетки. В Этапе 20 v2 эта кнопка открывает
        // AddCustomSlotSheet (см. [AddClassButton]).
        val cells: List<GridCell> = viewModel.classes().map { GridCell.Class(it) } +
            GridCell.AddButton
        cells.chunked(3).forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { cell ->
                    when (cell) {
                        is GridCell.Class -> ClassCard(
                            info = cell.info,
                            level = viewModel.state.value.classLevels[cell.info.id] ?: 0,
                            onLevelChange = { viewModel.setClassLevel(cell.info.id, it) },
                            modifier = Modifier.weight(1f),
                        )
                        GridCell.AddButton -> AddClassButton(
                            modifier = Modifier.weight(1f),
                            onClick = onAddCustomSlot,
                        )
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/**
 * Этап 24 v3: общий конструктор карточки сетки классов.
 *
 * Единая геометрия для [ClassCard] и [AddClassButton]:
 *   - `Box` 92.dp мин. высота, скруглённые углы 14.dp, фон CardBg
 *   - внутри `Column`: заголовок (13.sp SemiBold) + подзаголовок (10.sp)
 *     + `middleContent` (произвольный — LevelInput для классов, иконка
 *     для кнопки добавления)
 *   - опциональный `onClick` делает карточку кликабельной
 *   - `borderColor`/`borderWidth` позволяют подсветить активные
 *     мультиклассы (PurpleLight + 1.5.dp)
 *
 * Так [AddClassButton] визуально встаёт в один ритм с [ClassCard] —
 * одинаковая высота, одинаковое расположение заголовка/подзаголовка,
 * одинаковый внутренний отступ. Раньше AddClassButton был Box с
 * Column(horizontalAlignment = CenterHorizontally), и иконка с
 * текстом «custom» сидели в центре — теперь текст «Custom» это
 * полноценный заголовок сверху, как у остальных.
 */
@Composable
private fun GridCard(
    title: String,
    subtitle: String,
    titleColor: androidx.compose.ui.graphics.Color,
    subtitleColor: androidx.compose.ui.graphics.Color,
    onClick: (() -> Unit)?,
    borderColor: androidx.compose.ui.graphics.Color,
    borderWidth: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    middleContent: @Composable () -> Unit = {},
) {
    Box(
        modifier = modifier
            .heightIn(min = 92.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.CardBg)
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(14.dp),
            )
            .let { if (onClick != null) it.clickable { onClick() } else it }
            .padding(horizontal = 8.dp, vertical = 10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = title,
                color = titleColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                color = subtitleColor,
                fontSize = 10.sp,
                maxLines = 1,
            )
            middleContent()
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
    GridCard(
        title = stringResource(info.nameRes()),
        subtitle = if (hasCasting) "×${stringResource(factorRes(info))}" else stringResource(R.string.home_class_non_caster),
        titleColor = AppColors.TextWhite,
        subtitleColor = if (hasCasting) AppColors.Gold else AppColors.TextGreyDark,
        onClick = null,
        borderColor = if (isMulticlass) AppColors.PurpleLight else AppColors.Outline,
        borderWidth = if (isMulticlass) 1.5.dp else 1.dp,
        // Этап 24 v3: фон CardBgLighter для мультиклассов применяем
        // ПОВЕРХ GridCard'овского CardBg, чтобы подсветить активные классы.
        modifier = if (isMulticlass) modifier.background(AppColors.CardBgLighter) else modifier,
        middleContent = { LevelInput(level, onLevelChange) },
    )
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

/**
 * Кнопка «Добавить класс» (Этап 19) — последняя ячейка сетки.
 *
 * Внешне повторяет [ClassCard] (мин. высота 92.dp, скруглённые углы,
 * фон CardBg, рамка Outline), но вместо имени класса и поля ввода
 * уровня — крупная золотая иконка «+» по центру. Занимает последнюю
 * ячейку сетки: при 11 классах это ячейка 4×3 (ряд 4, колонка 3);
 * при изменении числа классов кнопка автоматически окажется в конце
 * `chunked(3)`, потому что добавляется в конец списка ячеек.
 *
 * Этап 20 v2: кнопка больше не no-op — открывает [AddCustomSlotSheet]
 * (см. [CollapsibleClassesSection], который передаёт [onClick]).
 * Визуально: золотая «+» в бейдже + подпись «custom» под ней (компактно,
 * 10.sp серым), чтобы пользователь видел, что это «кастомная» ячейка,
 * а не «добавить новый класс». Геометрия та же (92.dp мин. высота,
 * скруглённые углы 14.dp, фон CardBg, рамка Outline), чтобы не ломать
 * общий ритм сетки.
 */
/**
 * Кнопка «Добавить пользовательскую ячейку» (Этап 20 v2 → Этап 24 v3).
 *
 * Занимает последнюю ячейку сетки [ClassesGrid] (3×3 → 10 ячеек: 9
 * классов + 1 кнопка). Визуально повторяет [ClassCard] 1-в-1 благодаря
 * общему конструктору [GridCard]:
 *   - Заголовок «Custom» (13.sp, gold) — как у других классов
 *   - Подзаголовок «+ ячейка» (10.sp, grey) — объясняет действие
 *   - В middleContent — крупная золотая иконка «+» (36.dp)
 *   - Кликабельна — открывает [AddCustomSlotSheet] (Этап 20 v2).
 *
 * Раньше текст «custom» был мелкой подписью 10.sp ПОД иконкой +
 * иконка была 28.dp — из-за этого кнопка визуально казалась меньше
 * других карточек в сетке. Теперь с единым конструктором и
 * полноценным заголовком кнопка выглядит как «ещё одна карточка
 * класса», а не как «отдельная маленькая кнопочка».
 */
@Composable
private fun AddClassButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    GridCard(
        title = stringResource(R.string.home_class_add_custom_title),
        subtitle = stringResource(R.string.home_class_add_custom_subtitle),
        titleColor = AppColors.Gold,
        subtitleColor = AppColors.TextGrey,
        onClick = onClick,
        borderColor = AppColors.Outline,
        borderWidth = 1.dp,
        modifier = modifier,
        middleContent = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.home_class_add_custom_content_description),
                    tint = AppColors.Gold,
                    modifier = Modifier.size(36.dp),
                )
            }
        },
    )
}

/**
 * Возвращает `@StringRes Int` для множителя caster level,
 * отображаемого в подзаголовке карточки класса (формат «×0.5»).
 *
 * Порядок проверок важен:
 *   1. `factor >= 1.0` — полный заклинатель, без стрелки
 *   2. `isThirdCaster` — 1/3 кастер (Eldritch Knight / Arcane Trickster),
 *      ДО проверки roundUp, иначе обычная ветка roundUp дала бы «0.5↑»
 *   3. `roundUp` — полузаклинатель с округлением вверх (Изобретатель)
 *   4. else — обычный полузаклинатель (Паладин, Следопыт)
 */
@androidx.annotation.StringRes
private fun factorRes(info: Classes.Info): Int = when {
    info.factor >= 1.0          -> R.string.home_class_factor_one
    info.isThirdCaster          -> R.string.home_class_factor_third_up
    info.roundUp                -> R.string.home_class_factor_half_up
    else                        -> R.string.home_class_factor_half
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

@Composable
private fun SectionTitle(@androidx.annotation.StringRes textRes: Int) {
    Text(
        text = stringResource(textRes).uppercase(),
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
        SectionTitle(R.string.home_section_pact_magic)
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
                    text = stringResource(R.string.home_pact_magic_class_label),
                    color = AppColors.TextGrey,
                    fontSize = 9.sp,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.width(12.dp))
            SlotCells(
                used = slot.used,
                total = slot.total,
                modifier = Modifier.weight(1f),
            )
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
        SectionTitle(R.string.home_section_spell_slots)
        Spacer(Modifier.height(10.dp))
        if (state.regularSlots.isEmpty()) {
            Text(
                stringResource(R.string.home_spell_slots_empty),
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
 *   - Ряд визуальных блоков [SlotCells] (рендеринг по правилам
 *     Этапа 21: `total in 1..5` → 1 ряд 48.dp, `6..10` → 2 ряда по
 *     38.dp, `11..20` → числовой диапазон «used / total»).
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
            SlotCells(
                used = slot.used,
                total = slot.total,
                modifier = Modifier.weight(1f),
            )
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
 * Один визуальный блок ячейки вынесен в [SlotCell] (Этап 21).
 *
 * Прежний приватный `SpellSlotBlock` заменён на публичный [SlotCell]
 * с параметром `sizeDp`: один и тот же блок работает и в обычном
 * ряду (48.dp), и в уменьшенном (38.dp для `total in 6..10`), и как
 * одиночный блок арканума.
 */

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
        SectionTitle(R.string.home_section_arcanums)
        Spacer(Modifier.height(10.dp))
        when {
            state.warlockLevel < 11 -> {
                Text(
                    stringResource(R.string.home_arcanum_locked),
                    color = AppColors.TextGrey,
                    fontSize = 12.sp,
                )
            }
            state.arcanums.isEmpty() -> {
                Text(
                    stringResource(R.string.home_arcanum_empty),
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
        // Один визуальный блок (арканум — всегда ровно 1 ячейка).
        SlotCell(used = isSpent, modifier = Modifier.size(48.dp))
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
                stringResource(R.string.rest_short_title),
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
                stringResource(R.string.rest_long_title),
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

// =============================================================
// Этап 20: сворачиваемая секция «Классы» + новый раздел
// «Пользовательские ячейки» (универсальный конструктор)
// =============================================================

/** Таймаут удержания для перехода в режим редактирования (1.5 секунды). */
private const val LONG_PRESS_TIMEOUT_MS: Long = 1500L

/**
 * Секция «Классы» (Этап 20: сворачиваемая).
 *
 * Заголовок кликабельный — `onToggle` переключает `expanded`. Тело
 * (сетка классов) появляется/исчезает через [AnimatedVisibility]
 * с плавной анимацией. Свернуто по умолчанию (см. `classesExpanded`
 * в [HomeScreen]).
 *
 * Сетка классов ([ClassesGrid]) — тот же компонент, что и раньше;
 * мы только обернули его в collapsible-обёртку.
 *
 * `onAddCustomSlot` (Этап 20 v2): проброс клика по «+» в сетке
 * (см. [AddClassButton]) — открывает [AddCustomSlotSheet].
 */
@Composable
private fun CollapsibleClassesSection(
    viewModel: HomeViewModel,
    expanded: Boolean,
    onToggle: () -> Unit,
    onAddCustomSlot: () -> Unit,
) {
    Column {
        // Кликабельный заголовок с шевроном
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onToggle)
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionTitle(R.string.home_section_classes)
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = if (expanded) Icons.Filled.KeyboardArrowUp
                              else Icons.Filled.KeyboardArrowDown,
                contentDescription = stringResource(
                    if (expanded) R.string.home_section_classes_collapse
                    else R.string.home_section_classes_expand,
                ),
                tint = AppColors.PurpleLight,
                modifier = Modifier.size(20.dp),
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column {
                Spacer(Modifier.height(10.dp))
                ClassesGrid(viewModel, onAddCustomSlot)
            }
        }
    }
}

/**
 * Секция «Пользовательские ячейки» (Этап 20, ревизия v2).
 *
 * - Заголовок (без `+` — он перенесён в сетку классов, см.
 *   [AddClassButton] в [ClassesGrid])
 * - Список [CustomSlotRow]
 * - Пустое состояние с подсказкой про «разверните Классы»,
 *   потому что кнопка `+` теперь находится там
 *
 * Каждая строка:
 *   - тап → потратить одну ячейку ([onRowClick])
 *   - удержание 3с → редактирование ([onRowLongPress], обычно
 *     `nav.navigate("customslot/$id")` в вызывающем коде)
 */
@Composable
private fun CustomSlotsSection(
    slots: List<com.example.spelltracker.data.CustomSlot>,
    onRowClick: (com.example.spelltracker.data.CustomSlot) -> Unit,
    onRowLongPress: (com.example.spelltracker.data.CustomSlot) -> Unit,
) {
    Column {
        // Этап 20 v2: «+» перенесён в [AddClassButton] внутри сетки
        // классов. Заголовок секции — простой [SectionTitle].
        SectionTitle(R.string.home_section_custom_slots)
        Spacer(Modifier.height(10.dp))
        if (slots.isEmpty()) {
            // Пустое состояние — подсказка, что кнопка добавления
            // теперь в сетке классов (нужно развернуть «Классы»).
            Text(
                stringResource(R.string.home_custom_slots_empty),
                color = AppColors.TextGrey,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                slots.forEach { slot ->
                    CustomSlotRow(
                        slot = slot,
                        onRowClick = { onRowClick(slot) },
                        onLongPress = { onRowLongPress(slot) },
                    )
                }
            }
        }
    }
}

/**
 * Строка одной пользовательской ячейки (Этап 20, фикс long-press v2).
 *
 * Визуально похожа на [SpellSlotRow], но:
 *   - **заголовок** сверху (название: «Дыхание дракона» и т.п.)
 *   - **бейдж** содержит тип кубика (`d6`) вместо римского номера
 *     уровня заклинания (отсюда просьба пользователя «тип кубика
 *     отображается текстом в замен уровня ячейки»)
 *   - **счётчик** `X / Y` справа внизу
 *
 * Жесты:
 *   - короткий тап → [onRowClick] (потратить ячейку, если есть куда)
 *   - удержание 3с → [onLongPress] (открыть экран редактирования)
 *
 * Реализация long press 3с: используем `MutableInteractionSource` +
 * `LaunchedEffect(isPressed)`. Когда палец прижат (`isPressed = true`),
 * запускается корутина, которая ждёт 3с и вызывает [onLongPress].
 * Если палец отпущен раньше — `isPressed` сбрасывается в false,
 * корутина отменяется, [onLongPress] не вызывается. `clickable`
 * при этом успевает вызвать [onRowClick] (стандартное поведение
 * `onClick` — отпускание пальца внутри clickable).
 *
 * **Фикс long-press v2 (отзыв пользователя):** до фикса
 * `clickable.onClick` срабатывал **и после** успешного long press —
 * пользователь удерживал 3с, переходил в режим редактирования, а при
 * отпускании пальца ячейка всё равно тратилась. Лечится флагом
 * [longPressHandled]:
 *   - В LaunchedEffect сбрасываем флаг в начале каждого нажатия и
 *     ставим `true` после `delay(3000)` + `onLongPress()`.
 *   - В `clickable.onClick` сначала запоминаем значение флага, потом
 *     сбрасываем (для следующего нажатия), и вызываем [onRowClick]
 *     только если флаг был `false` на момент релиза.
 *
 * Сброс делается **в двух местах** (LaunchedEffect + clickable.onClick)
 * намеренно: гарантирует, что даже если пользователь прервёт
 * взаимодействие (например, длинный press + закрытие экрана
 * редактирования), следующее короткое нажатие снова тратит ячейку.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun CustomSlotRow(
    slot: com.example.spelltracker.data.CustomSlot,
    onRowClick: () -> Unit,
    onLongPress: () -> Unit,
) {
    val isAllSpent = slot.isAllSpent
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
            // Этап 24 v2: combinedClickable — тап и long-press работают
            // НЕЗАВИСИМО. Тап → тратим ячейку (если есть что), long-press
            // → ВСЕГДА открывает модалку (даже когда всё потрачено).
            .combinedClickable(
                onClick = { if (!isAllSpent) onRowClick() },
                onLongClick = onLongPress,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        // Заголовок — крупно (16sp), как у остальных рядов ячеек.
        // Раньше был 14sp — слишком мелкий по сравнению с бейджем.
        Text(
            text = slot.title.ifBlank { stringResource(R.string.home_custom_slot_no_title) },
            color = AppColors.TextWhite,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        // Ряд: бейдж (56dp) + ячейки — структура 1-в-1 как у PactMagicRow,
        // чтобы все ряды выглядели единообразно.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
                        text = slot.die.label,
                        color = AppColors.Gold,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(2.dp))
                // Тип восстановления: К (короткий) / Д (длинный) —
                // маленькая подпись под бейджем, как «Колдун» у PactMagicRow.
                Text(
                    text = stringResource(
                        when (slot.restType) {
                            com.example.spelltracker.data.RestType.SHORT -> R.string.home_custom_slot_rest_short
                            com.example.spelltracker.data.RestType.LONG  -> R.string.home_custom_slot_rest_long
                        }
                    ),
                    color = AppColors.TextGrey,
                    fontSize = 9.sp,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.width(12.dp))
            SlotCells(
                used = slot.used,
                total = slot.total,
                modifier = Modifier.weight(1f),
            )
        }
        // Счётчик X / Y (правый край) — только при total ≤ 10.
        // Для 11..20 числовой диапазон уже показан в SlotCells.
        if (slot.total <= 10) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${slot.used} / ${slot.total}",
                color = if (isAllSpent) AppColors.TextGreyDark else AppColors.TextGrey,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
            )
        }
    }
}
